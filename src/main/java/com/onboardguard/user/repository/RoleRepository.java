package com.onboardguard.user.repository;

import com.onboardguard.shared.common.enums.RoleCode;
import com.onboardguard.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByRoleCode(RoleCode roleCode);

    boolean existsByRoleCode(RoleCode roleCode);
}
