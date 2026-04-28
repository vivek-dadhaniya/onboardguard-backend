package com.onboardguard.shared.security;

import com.onboardguard.auth.entity.AppUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecretKey;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey));
    }

    // Generate token from Spring Authentication (Login)
    public String generateToken(Authentication authentication) {

        String email = authentication.getName();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);
        String jti = UUID.randomUUID().toString();  // unique ID — required for blacklist

        // Store permission strings (not role names) so authorities match @PreAuthorize
        List<String> permissions = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .id(jti)
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .claim("permissions", permissions)
                .signWith(key)
                .compact();
    }

    // Generate token from User entity (Registration)
    public String generateTokenForUser(AppUser user) {
        Date   now    = new Date();
        Date   expiry = new Date(now.getTime() + jwtExpirationMs);
        String jti    = UUID.randomUUID().toString();

        // Same permission strings as generateToken() — consistency is critical
        List<String> permissions = new ArrayList<>(
                RolePermissions.getPermissions(user.getRole())
        );

        return Jwts.builder()
                .id(jti)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiry)
                .claim("permissions", permissions)
                .signWith(key)
                .compact();
    }

    //  EXTRACT EMAIL — used by JwtAuthenticationFilter
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // Extract jti — called by TokenBlacklistService on logout
    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    // Calculate remaining validity seconds — used for Redis TTL on logout
    public long getRemainingSeconds(String token) {
        Date expiry = parseClaims(token).getExpiration();
        long remainingMs = expiry.getTime() - System.currentTimeMillis();
        return Math.max(0L, remainingMs / 1000L);
    }


    //  VALIDATE — returns boolean, never throws
    //  Filter chain must never break on invalid tokens
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims empty: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
