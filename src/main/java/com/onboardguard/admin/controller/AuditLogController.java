package com.onboardguard.admin.controller;

import com.onboardguard.admin.dto.AuditLogDto;
import com.onboardguard.admin.service.AuditLogService;
import com.onboardguard.shared.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Fetches the clean, human-readable timeline for the UI Frontend.
     * Example Call: GET /api/v1/admin/audit-logs/timeline?entityType=CANDIDATE&entityId=105
     */
    @GetMapping("/timeline")
//    @PreAuthorize("hasAnyRole('ROLE_OFFICER_L1', 'ROLE_OFFICER_L2', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getTimeline(
            @RequestParam String entityType,
            @RequestParam Long entityId) {

        List<AuditLogDto> timeline = auditLogService.getEntityHistory(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success("Timeline fetched successfully", timeline));
    }
}