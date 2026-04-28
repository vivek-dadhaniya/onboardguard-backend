package com.onboardguard.officer.controller;

import com.onboardguard.candidate.dto.response.DocumentResponseDto;
import com.onboardguard.officer.dto.CandidateQueueItemDto;
import com.onboardguard.officer.dto.CandidateVerificationDashboardDto;
import com.onboardguard.officer.dto.RejectDocumentRequestDto;
import com.onboardguard.officer.service.DocumentVerificationService;
import com.onboardguard.shared.common.dto.ApiResponse;
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
@RequestMapping("/api/v1/officer/documents")
@RequiredArgsConstructor
//@PreAuthorize("hasAnyRole('ROLE_OFFICER_L1', 'ROLE_SUPER_ADMIN')") // L1 is typically the Maker for KYC docs
public class DocumentVerificationController {

    private final DocumentVerificationService documentVerificationService;
    private final SecurityUtils securityUtils;

    // ══════════════════════════════════════════════════════════════
    // 1. QUEUE & CLAIM ENDPOINTS
    // ══════════════════════════════════════════════════════════════

    /**
     * POST: Auto-assigns the oldest waiting candidate to the officer (FIFO Push Model).
     * Instantly returns the full dashboard data so the UI can route to the verification screen.
     */
    @PostMapping("/candidates/assign-next")
    public ResponseEntity<ApiResponse<CandidateVerificationDashboardDto>> assignNextCandidate() {
        Long currentOfficerId = securityUtils.getCurrentUserPrincipal().getUserId();

        CandidateVerificationDashboardDto dashboardData = documentVerificationService.claimNextAvailableCandidate(currentOfficerId);

        return ResponseEntity.ok(ApiResponse.success(
                "Candidate successfully assigned from the queue.",
                dashboardData
        ));
    }

    /**
     * POST: Manually claim a specific candidate from the UI grid (Pull Model).
     * Locks the candidate to prevent collision with other officers.
     */
    @PostMapping("/candidates/{candidateId}/claim")
    public ResponseEntity<ApiResponse<Void>> claimCandidate(@PathVariable Long candidateId) {
        Long currentOfficerId = securityUtils.getCurrentUserPrincipal().getUserId();

        documentVerificationService.claimCandidateForVerification(candidateId, currentOfficerId);

        return ResponseEntity.ok(ApiResponse.success(
                "Candidate claimed successfully. Ready for review.",
                null
        ));
    }

    // ══════════════════════════════════════════════════════════════
    // 2. DASHBOARD & VERIFICATION ENDPOINTS
    // ══════════════════════════════════════════════════════════════

    /**
     * GET: Pulls the complete candidate profile (data + documents) for the Officer Dashboard.
     */
    @GetMapping("/candidates/{candidateId}")
    public ResponseEntity<ApiResponse<CandidateVerificationDashboardDto>> getCandidateVerificationDetails(
            @PathVariable Long candidateId) {

        CandidateVerificationDashboardDto dashboardData = documentVerificationService.getCandidateVerificationDetails(candidateId);

        return ResponseEntity.ok(ApiResponse.success(
                "Candidate verification details retrieved successfully.",
                dashboardData
        ));
    }

    /**
     * POST: Approve a legally valid document.
     */
    @PostMapping("/{documentId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveDocument(@PathVariable Long documentId) {

        Long currentOfficerId = securityUtils.getCurrentUserPrincipal().getUserId();
        documentVerificationService.approveDocument(documentId, currentOfficerId);

        return ResponseEntity.ok(ApiResponse.success("Document verified successfully.", null));
    }

    /**
     * POST: Reject an invalid/blurry document and trigger an email to the candidate.
     */
    @PostMapping("/{documentId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectDocument(
            @PathVariable Long documentId,
            @Valid @RequestBody RejectDocumentRequestDto request) {

        Long currentOfficerId = securityUtils.getCurrentUserPrincipal().getUserId();
        documentVerificationService.rejectDocument(documentId, request.reason(), currentOfficerId);

        return ResponseEntity.ok(ApiResponse.success("Document rejected. Candidate will be notified to re-upload.", null));
    }

    // ══════════════════════════════════════════════════════════════
    // QUEUE VIEW ENDPOINT
    // ══════════════════════════════════════════════════════════════

    /**
     * GET: Returns the queue of all candidates waiting for document verification.
     * Used to populate the Officer's "Pending Verifications" data grid.
     */
    @GetMapping("/candidates/pending")
    public ResponseEntity<ApiResponse<List<CandidateQueueItemDto>>> getPendingCandidatesQueue() {

        List<CandidateQueueItemDto> queue = documentVerificationService.getPendingCandidatesQueue();

        return ResponseEntity.ok(ApiResponse.success(
                "Pending candidate queue retrieved successfully.",
                queue
        ));
    }

}