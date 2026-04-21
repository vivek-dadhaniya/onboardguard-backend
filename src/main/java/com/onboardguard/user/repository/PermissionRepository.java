package com.onboardguard.user.repository;

import com.onboardguard.shared.common.enums.PermissionCode;
import com.onboardguard.user.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByPermissionCode(PermissionCode permissionCode);
}
