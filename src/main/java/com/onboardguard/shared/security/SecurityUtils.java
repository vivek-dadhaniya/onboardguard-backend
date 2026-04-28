package com.onboardguard.shared.security;

import com.onboardguard.auth.entity.AppUser;
import com.onboardguard.auth.repository.AppUserRepository;
import com.onboardguard.shared.common.exception.ResourceNotFoundException;
import com.onboardguard.shared.common.exception.UnauthorizedAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {

    private final AppUserRepository appUserRepository;

    /**
     * Returns the CustomUserDetails from the SecurityContext.
     * ZERO database calls — reads the principal that was loaded by JwtAuthFilter.
     * Use this for permission checks, role checks, and getting userId/email in controllers.
     */
    public CustomUserDetails getCurrentUserPrincipal() {
        Authentication auth = getValidAuthentication();
        if (!(auth.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new IllegalStateException("Principal in SecurityContext is not CustomUserDetails — check filter configuration");
        }
        return principal;
    }

    /**
     * Returns the full AppUser JPA entity by loading from DB.
     * Use ONLY when you genuinely need entity fields not in CustomUserDetails
     * (e.g., createdBy, lastLoginAt). For everything else, use getCurrentUserPrincipal().
     */
    public AppUser getCurrentUser() {
        String email = getCurrentUserPrincipal().getEmail();

        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Token valid, but user not found in database for email: {}", email);
                    return new ResourceNotFoundException("Authenticated user not found in database.");
                });
    }

    // Used to prevent Horizontal Privilege Escalation (IDOR attacks) - Insecure Direct Object Reference
    public boolean isOwner(Long resourceOwnerId) {
        if (resourceOwnerId == null)    return false;
        return getCurrentUser().getId().equals(resourceOwnerId);
    }

    /**
     * Checks if the current user has a specific permission.
     * Zero DB calls — reads from the pre-loaded CustomUserDetails in SecurityContext.
     * Prefer @PreAuthorize("hasAuthority('PERMISSION')") in controllers.
     * Use this method for dynamic permission checks inside service logic.
     */
    public boolean hasPermission(String permissionCode) {
        return isAuthenticated() && getCurrentUserPrincipal().hasPermission(permissionCode);
    }

    /**
     * Checks if the current user has a specific role.
     * Kept from original SecurityUtils for backward compatibility.
     */
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        // Roles are not directly in authorities (permissions are).
        // Compare against the role field of CustomUserDetails.
        if (auth.getPrincipal() instanceof CustomUserDetails principal) {
            return principal.getRole().name().equals("ROLE_" + role.toUpperCase())
                    || principal.getRole().name().equals(role.toUpperCase());
        }
        return false;
    }

    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    private Authentication getValidAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(auth.getPrincipal())) {
            log.warn("Attempted access without valid authentication");
            throw new UnauthorizedAccessException("No authenticated user found in security context");
        }
        return auth;
    }

}