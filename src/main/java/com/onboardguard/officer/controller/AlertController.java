package com.onboardguard.officer.controller;

import com.onboardguard.officer.dto.AlertDetailDto;
import com.onboardguard.officer.service.AlertService;
import com.onboardguard.shared.common.dto.ApiResponse;
import com.onboardguard.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/officer/alerts")
@RequiredArgsConstructor
//@PreAuthorize("hasAnyRole('ROLE_OFFICER_L1', 'ROLE_SUPER_ADMIN')")
public class AlertController {

    private final AlertService alertService;
    private final SecurityUtils securityUtils;

    /**
     * POST: Claim an OPEN alert and lock it for review.
     */
    @PostMapping("/{alertId}/acknowledge")
    public ResponseEntity<ApiResponse<AlertDetailDto>> acknowledgeAlert(@PathVariable Long alertId) {
        Long currentOfficerId = securityUtils.getCurrentUserPrincipal().getUserId();

        AlertDetailDto claimedAlert = alertService.acknowledgeAlert(alertId, currentOfficerId);

        return ResponseEntity.ok(ApiResponse.success("Alert successfully claimed and locked.", claimedAlert));
    }

    /**
     * POST: Automatically assigns the oldest OPEN alert to the requesting L1 Officer.
     * This enforces a strict FIFO (First-In, First-Out) queue and prevents cherry-picking.
     */
    @PostMapping("/assign-next")
    public ResponseEntity<ApiResponse<AlertDetailDto>> claimNextAvailableAlert() {

        Long currentOfficerId = securityUtils.getCurrentUserPrincipal().getUserId();

        // If the queue is empty, the service throws ResourceNotFoundException,
        // which your GlobalExceptionHandler will elegantly catch and return to the UI!
        AlertDetailDto nextAlert = alertService.claimNextAvailableAlert(currentOfficerId);

        return ResponseEntity.ok(ApiResponse.success("Alert successfully assigned from the queue.", nextAlert));
    }

    /**
     * POST: Dismiss an alert as a false positive.
     * Note: Using @RequestParam for the reason is perfect for simple string submissions.
     */
    @PostMapping("/{alertId}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismissAlert(
            @PathVariable Long alertId,
            @RequestParam String reason) {

        Long currentOfficerId = securityUtils.getCurrentUserPrincipal().getUserId();

        alertService.dismissAlert(alertId, currentOfficerId, reason);

        return ResponseEntity.ok(ApiResponse.success("Alert dismissed successfully.", null));
    }

    /**
     * POST: Convert a valid alert into a full investigation Case.
     */
    @PostMapping("/{alertId}/convert")
    public ResponseEntity<ApiResponse<Long>> convertToCase(@PathVariable Long alertId) {
        Long currentOfficerId = securityUtils.getCurrentUserPrincipal().getUserId();

        Long newCaseId = alertService.convertToCase(alertId, currentOfficerId);

        return ResponseEntity.ok(ApiResponse.success("Alert converted to Case successfully.", newCaseId));
    }
}