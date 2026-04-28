package com.onboardguard.auth.repository;

import com.onboardguard.auth.entity.AppUser;
import com.onboardguard.shared.common.enums.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // Look-up
    Optional<AppUser> findByEmail(String email);

    // Existence checks (used during registration / officer creation)
    boolean existsByEmail(String email);

    // Role-based queries (used by Admin / Super Admin panels)
    List<AppUser> findByRole(RoleCode role);
    List<AppUser> findByRoleAndActive(RoleCode role, boolean active);

    // Returns all staff users (everyone except candidates).
    @Query("SELECT u FROM AppUser u WHERE u.role != :candidateRole ORDER BY u.fullName")
    List<AppUser> findAllStaff(@Param("candidateRole") RoleCode candidateRole);


    /**
     * Uses executeUpdate() instead of getResultList()
     * Returns number of rows affected
     * Caller must also call userDetailsService.evictCache(email) after this.
     */
    @Modifying
    @Query("UPDATE AppUser u SET u.active = false, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    int deactivateById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE AppUser u SET u.active = true, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    int activateById(@Param("id") Long id);

    @Modifying
    @Query("""
        UPDATE AppUser u
        SET u.locked = false,
            u.updatedAt = CURRENT_TIMESTAMP
        WHERE u.id = :id
        """)
    int unlockById(@Param("id") Long id);
}