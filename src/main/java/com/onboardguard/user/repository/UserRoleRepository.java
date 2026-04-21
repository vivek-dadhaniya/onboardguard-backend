package com.onboardguard.user.repository;

import com.onboardguard.user.entity.UserRole;
import com.onboardguard.user.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByIdUserId(UUID userId);

    List<UserRole> findByIdRoleId(UUID roleId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.id.userId = :userId AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP)")
    List<UserRole> findActiveRolesByUserId(@Param("userId") UUID userId);
}
