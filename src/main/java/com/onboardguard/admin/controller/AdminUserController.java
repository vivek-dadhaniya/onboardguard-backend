package com.onboardguard.admin.controller;

import com.onboardguard.admin.dto.UserResponseDto;
import com.onboardguard.admin.mapper.AdminUserMapper;
import com.onboardguard.admin.service.UserManagementService;
import com.onboardguard.auth.dto.request.CreateOfficerDto;
import com.onboardguard.auth.entity.AppUser;
import com.onboardguard.auth.service.AuthService;
import com.onboardguard.shared.common.dto.ApiResponse;
import com.onboardguard.shared.common.enums.RoleCode;
import com.onboardguard.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserManagementService userManagementService;
    private final AuthService authService; // Injected for Officer Provisioning
    private final SecurityUtils securityUtils;
    private final AdminUserMapper adminUserMapper;

    /**
     * 1. CREATE OFFICER: Provision a new L1 or L2 Officer.
     * Endpoint: POST /api/v1/admin/users/officers
     */
    @PostMapping("/officers")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> createOfficer(@Valid @RequestBody CreateOfficerDto dto) {

        // Fetch the currently logged-in Admin safely from the Security Context
        AppUser currentUser = securityUtils.getCurrentUser();

        log.info("Admin {} is provisioning a new {}", currentUser.getEmail(), dto.role());

        // Delegate business logic to the Auth module
        authService.createOfficer(dto, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Officer provisioned successfully. Temporary credentials have been emailed." , null));
    }

    /**
     * 2. GET ALL USERS: Fetch paginated list of users for the Admin Grid.
     * Endpoint: GET /api/v1/admin/users?page=0&size=20
     */
    @GetMapping
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponseDto>>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        log.info("Admin {} is fetching the user grid", securityUtils.getCurrentUserPrincipal().getEmail());

        Page<AppUser> users = userManagementService.getAllUsers(pageable);

        return ResponseEntity.ok(ApiResponse.success("Users Fetched Successfully..." , users.map(adminUserMapper::toDto)));
    }

    /**
     * 3. TOGGLE STATUS: Activate or deactivate a specific user.
     * Endpoint: PATCH /api/v1/admin/users/{userId}/status?isActive=true
     */
    @PatchMapping("/{userId}/status")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> toggleUserStatus(
            @PathVariable Long userId,
            @RequestParam Boolean isActive) {

        // Effortlessly fetch current user and their role
        AppUser currentUser = securityUtils.getCurrentUser();
        RoleCode currentUserRole = securityUtils.getCurrentUserPrincipal().getRole();

        log.info("Admin {} is attempting to set user {} active status to {}",
                currentUser.getEmail(), userId, isActive);

        userManagementService.toggleUserStatus(userId, isActive, currentUser, currentUserRole);

        String message = isActive ? "User successfully activated." : "User successfully deactivated.";
        return ResponseEntity.ok(ApiResponse.success(message , null));
    }
}