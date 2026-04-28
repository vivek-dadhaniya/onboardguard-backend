package com.onboardguard.shared.security;

import com.onboardguard.shared.common.exception.LogoutFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Manages the JWT blacklist in Redis.
 *
 * Key pattern:  blacklist:jti:{jti}
 * Value:        "1"  (presence matters, not value)
 * TTL:          Remaining token validity in seconds.
 *               When the token would naturally expire, Redis auto-removes the key.
 *               No cleanup job required.
 *
 * On logout:
 *   blacklist(rawToken) → extracts jti → sets Redis key with TTL
 *
 * On every request (in JwtAuthFilter, before loading user):
 *   isBlacklisted(rawToken) → O(1) Redis EXISTS check
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:jti:";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Adds the token to the blacklist.
     * Called on logout. TTL = remaining seconds until token expires.
     * If token is already expired, no blacklist entry is needed.
     */
    public void blacklist(String rawToken) {
        try {
            String jti = jwtTokenProvider.extractJti(rawToken);
            long remainingSecs = jwtTokenProvider.getRemainingSeconds(rawToken);

            if (remainingSecs > 0) {
                redisTemplate.opsForValue()
                        .set(BLACKLIST_PREFIX + jti, "1", Duration.ofSeconds(remainingSecs));
                log.info("Token blacklisted: jti={} ttl={}s", jti, remainingSecs);
            } else {
                log.debug("Token already expired - blacklist entry not needed: jti={}", jti);
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token: {}", e.getMessage());
            throw new LogoutFailedException("Logout failed - could not invalidate token", e);
        }
    }

    /**
     * Checks if a token is blacklisted.
     * Called on every request — O(1) Redis EXISTS.
     * If Redis is down, returns false (safer to allow than to lock all users out).
     */
    public boolean isBlacklisted(String rawToken) {
        try {
            String  jti    = jwtTokenProvider.extractJti(rawToken);
            Boolean exists = redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Blacklist check failed - allowing request: {}", e.getMessage());
            return false;  // Redis down: fail open (safer than locking everyone out)
        }
    }
}

