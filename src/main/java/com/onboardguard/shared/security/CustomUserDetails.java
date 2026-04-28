package com.onboardguard.shared.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.onboardguard.auth.entity.AppUser;
import com.onboardguard.shared.common.enums.RoleCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Custom UserDetails that is both:
 *   - A Spring Security principal (carries authorities for @PreAuthorize)
 *   - Jackson-serializable into Redis (so we avoid repeated DB queries)
 *
 * Jackson deserialization requirement:
 *   Fields must NOT be final (Jackson sets them after no-arg construction).
 *   @JsonProperty on each field ensures the JSON key maps correctly.
 *   @JsonIgnore on passwordHash ensures the password is never written to Redis.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomUserDetails implements UserDetails {

    private Long userId;
    private String email;
    private String fullName;
    private RoleCode role;
    private boolean active;
    private boolean locked;

    @JsonProperty("passwordHash")
    private String passwordHash;

    private Set<String> authorities;

    public CustomUserDetails(AppUser user) {
        this.userId     = user.getId();
        this.email      = user.getEmail();
        this.fullName   = user.getFullName();
        this.role       = user.getRole();
        this.active     = user.isActive();
        this.locked     = user.isLocked();
        this.passwordHash = user.getPasswordHash();
        this.authorities = RolePermissions.getPermissions(user.getRole());
    }

    /**
     * No-arg constructor required by Jackson for Redis deserialization.
     * Fields are set via setters during deserialization.
     * Do not call directly in application code.
     */
    public CustomUserDetails() {
        
    }

    // UserDetails contract
     @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;   //email as login ID
    }

    @Override
    public boolean isEnabled() {
        return active && !locked;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // Convenience helpers — no DB call, all from context
    public boolean isCandidate()  { return role == RoleCode.ROLE_CANDIDATE; }
    public boolean isOfficerL1()  { return role == RoleCode.ROLE_OFFICER_L1; }
    public boolean isOfficerL2()  { return role == RoleCode.ROLE_OFFICER_L2; }
    public boolean isAdmin()      { return role == RoleCode.ROLE_ADMIN; }
    public boolean isSuperAdmin() { return role == RoleCode.ROLE_SUPER_ADMIN; }
    public boolean isStaff()      { return role != RoleCode.ROLE_CANDIDATE; }

    public boolean hasPermission(String permissionCode) {
        return authorities != null && authorities.contains(permissionCode);
    }
}
