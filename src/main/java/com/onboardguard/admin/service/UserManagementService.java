package com.onboardguard.admin.service;

import com.onboardguard.auth.entity.AppUser;
import com.onboardguard.shared.common.enums.RoleCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface UserManagementService {

    void toggleUserStatus(Long targetUserId, Boolean isActive, AppUser currentUser, RoleCode currentUserRole);

    Page<AppUser> getAllUsers(Pageable pageable);
}
