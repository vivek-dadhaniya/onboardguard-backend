package com.onboardguard.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onboardguard.auth.entity.AppUser;
import com.onboardguard.auth.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Spring Security UserDetailsService with Redis caching.
 *
 * Flow per request:
 *   1. Check Redis (key: "user:details:{email}") → deserialize UserPrincipal if found
 *   2. Cache miss → load AppUser from DB (single query, no joins)
 *   3. Validate account state (active, locked)
 *   4. Build UserPrincipal (expands role → permissions)
 *   5. Write UserPrincipal to Redis (TTL: 15 min)
 *   6. Return UserPrincipal
 *
 * Redis failures are always non-fatal:
 *   - Read failure  → fall through to DB (auth still works)
 *   - Write failure → log and continue (auth still works, just not cached)
 *
 * Cache eviction must be called on any state change:
 *   - Password change
 *   - Role change
 *   - Deactivation / lock
 *   Call: userDetailsService.evictCache(email)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private static final String   CACHE_PREFIX = "user:details:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    private final AppUserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        String cacheKey = CACHE_PREFIX + email;

        // 1. Redis check - O(1), no DB call
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Cache HIT: {}", email);
                return objectMapper.readValue(cached, CustomUserDetails.class);
            }
        } catch (Exception e) {
            // Redis read failure must NEVER block login
            log.warn("Redis read failed for {}: {}", email, e.getMessage());
        }

        // 2. DB load (single query — no joins needed)
        log.debug("Cache MISS: {} — querying DB", email);
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // 3. Account state checks
        if (!user.isActive()) {
            throw new DisabledException("Account is deactivated: " + email);
        }
        if (user.isLocked()) {
            throw new LockedException("Account is locked: " + email);
        }

        // 4. Build principal (role -> permissions expansion happens here)
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        // 5. Write to Redis
        try {
            String serialized = objectMapper.writeValueAsString(customUserDetails);
            redisTemplate.opsForValue().set(cacheKey, serialized, CACHE_TTL);
            log.debug("Cached principal for: {}", email);
        } catch (Exception e) {
            // Redis write failure must NEVER block login
            log.warn("Redis write failed for {}: {}", email, e.getMessage());
        }

        return customUserDetails;
    }

    /**
     * Removes the cached UserPrincipal from Redis.
     *
     * Call this immediately after any of:
     *   - Password change
     *   - Role reassignment
     *   - Account deactivation or lock
     *   - Any other change that affects what the user can do
     *
     * The next request after eviction will trigger a fresh DB load.
     */
    public void evictCache(String email) {
        try {
            redisTemplate.delete(CACHE_PREFIX + email);
            log.info("Cache evicted for: {}", email);
        } catch (Exception e) {
            log.warn("Cache eviction failed for {}: {}", email, e.getMessage());
        }
    }
}