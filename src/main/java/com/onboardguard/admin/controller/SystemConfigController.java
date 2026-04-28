package com.onboardguard.admin.controller;

import com.onboardguard.admin.dto.SystemConfigResponseDto;
import com.onboardguard.admin.dto.UpdateSystemConfigDto;
import com.onboardguard.admin.service.SystemConfigAdminService;
import com.onboardguard.shared.common.dto.ApiResponse;
import com.onboardguard.shared.common.enums.RoleCode;
import com.onboardguard.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/system-configs")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigAdminService systemConfigAdminService;
    private final SecurityUtils securityUtils;

    /**
     * MAKER ACTION: Request an update to a system configuration.
     * Endpoint: PUT /api/v1/admin/system-configs/{configId}
     */
    @PutMapping("/{configId}")
//    @PreAuthorize("hasRole('ROLE_ADMIN')") // Super Admins usually approve, Admins request
    public ResponseEntity<ApiResponse<String>> requestConfigUpdate(
            @PathVariable Long configId,
            @Valid @RequestBody UpdateSystemConfigDto updateDto) {

        // 1. Effortlessly extract the Maker's details using your SecurityUtils
        Long currentUserId = securityUtils.getCurrentUserPrincipal().getUserId();
        RoleCode currentUserRole = securityUtils.getCurrentUserPrincipal().getRole();

        log.info("Admin ID {} is submitting an update request for config ID {}", currentUserId, configId);

        // 2. Pass to the Interceptor Service (Maker flow)
        systemConfigAdminService.requestConfigUpdate(configId, updateDto, currentUserId, currentUserRole);

        // 3. Return a clear success message to the Angular UI
        return ResponseEntity.ok(ApiResponse.success("Configuration update request submitted successfully and is pending Super Admin approval.", null));
    }

    /**
     * GET ALL CONFIGURATIONS: Returns the configuration grid for the UI.
     * Endpoint: GET /api/v1/admin/system-configs
     */
    @GetMapping
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SystemConfigResponseDto>>> getAllConfigs() {

        log.info("Admin {} is viewing the system configuration grid",
                securityUtils.getCurrentUserPrincipal().getEmail());

        List<SystemConfigResponseDto> configs = systemConfigAdminService.getAllConfigs();

        return ResponseEntity.ok(ApiResponse.success( "Fetched all the configurations", configs));
    }
}