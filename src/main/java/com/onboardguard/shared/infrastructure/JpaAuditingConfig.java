package com.onboardguard.shared.infrastructure;

import com.onboardguard.shared.security.CustomUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Enables Spring Data JPA Auditing.
 *
 * The AuditorAware bean tells Spring who is making a change,
 * which gets stored in created_by / updated_by on BaseEntity.
 *
 * Returns:
 *   - Authenticated user's email (for API-triggered changes)
 *   - "SYSTEM" (for Flyway seeds, batch jobs, or unauthenticated contexts)
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated()
                    || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of("SYSTEM");
            }

            if (auth.getPrincipal() instanceof CustomUserDetails principal) {
                return Optional.of(principal.getEmail());
            }

            // Fallback: use whatever name Spring Security has
            return Optional.of(auth.getName());
        };
    }
}