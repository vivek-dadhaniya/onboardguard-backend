package com.onboardguard.admin.service.impl;

import com.onboardguard.admin.service.UserManagementService;
import com.onboardguard.auth.entity.AppUser;
import com.onboardguard.auth.repository.AppUserRepository;
import com.onboardguard.shared.common.enums.RoleCode;
import com.onboardguard.shared.common.events.BusinessLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final AppUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 1. TOGGLE USER STATUS: Activates or Deactivates a user.
     */
    @Override
    @Transactional
    public void toggleUserStatus(Long targetUserId, Boolean isActive, AppUser currentUser, RoleCode currentUserRole) {
        AppUser targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Rule 3: Prevent self-deactivation
        if (targetUser.getEmail().equals(currentUser.getEmail())) {
            throw new SecurityException("You cannot alter your own account status.");
        }

        String oldStatus = targetUser.isActive() ? "ACTIVE" : "INACTIVE";
        String newStatus = isActive ? "ACTIVE" : "INACTIVE";

        targetUser.setActive(isActive);
        userRepository.save(targetUser);

        String action = isActive ? "USER_ACTIVATED" : "USER_DEACTIVATED";

        publishUserAudit(targetUserId, action, oldStatus, newStatus, currentUser.getId(),
                currentUserRole.name(), "Admin altered user access.");
    }

    /**
     * 2. GET ALL USERS: Used to populate the Admin User Grid.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AppUser> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    private void publishUserAudit(Long userId, String action, String oldStatus, String newStatus,
                                  Long performedBy, String actorRole, String remarks) {

        eventPublisher.publishEvent(BusinessLogEvent.builder()
                .entityType("USER")
                .entityId(userId)
                .action(action)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .performedBy(performedBy)
                .actorRole(actorRole)
                .remarks(remarks)
                .build());
    }
}