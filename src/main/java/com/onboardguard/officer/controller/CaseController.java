package com.onboardguard.officer.controller;

import com.onboardguard.officer.dto.*;
import com.onboardguard.officer.service.CaseNoteService;
import com.onboardguard.officer.service.CaseService;
import com.onboardguard.shared.common.dto.ApiResponse;
import com.onboardguard.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/officer/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;
    private final CaseNoteService caseNoteService;
    private final SecurityUtils securityUtils;

    /**
     * GET: Fetch the full details, timeline, and notes of a Case.
     * Both L1 and L2 officers need to read cases.
     */
    @GetMapping("/{caseId}")
//    @PreAuthorize("hasAnyRole('ROLE_OFFICER_L1', 'ROLE_OFFICER_L2', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CaseDetailDto>> getCaseDetails(@PathVariable Long caseId) {
        CaseDetailDto caseDetails = caseService.getCaseDetails(caseId);
        return ResponseEntity.ok(ApiResponse.success("Case details retrieved successfully.", caseDetails));
    }

    /**
     * POST: Append a note to the investigation timeline.
     */
    @PostMapping("/{caseId}/notes")
//    @PreAuthorize("hasAnyRole('ROLE_OFFICER_L1', 'ROLE_OFFICER_L2')")
    public ResponseEntity<ApiResponse<CaseNoteDto>> addInvestigationNote(
            @PathVariable Long caseId,
            @Valid @RequestBody NoteRequest request) {

        Long currentOfficerId = securityUtils.getCurrentUserPrincipal().getUserId();
        CaseNoteDto note = caseNoteService.addInvestigationNote(caseId, request.content(), currentOfficerId);

        return ResponseEntity.ok(ApiResponse.success("Note added successfully.", note));
    }

    /**
     * POST: Escalate the case to the L2 queue.
     * STRICT ROLE: Only L1 Officers can escalate cases.
     */
    @PostMapping("/{caseId}/escalate")
//    @PreAuthorize("hasRole('ROLE_OFFICER_L1')")
    public ResponseEntity<ApiResponse<Void>> escalateCase(
            @PathVariable Long caseId,
            @Valid @RequestBody EscalateCaseDto dto) {

        Long currentOfficerId = securityUtils.getCurrentUserPrincipal().getUserId();
        caseService.escalateCase(caseId, dto, currentOfficerId);

        return ResponseEntity.ok(ApiResponse.success("Case escalated to L2 successfully.", null));
    }

    /**
     * POST: Claim an escalated case from the L2 queue.
     * STRICT ROLE: Only L2 Officers can claim escalated cases.
     */
    @PostMapping("/{caseId}/claim")
//    @PreAuthorize("hasRole('ROLE_OFFICER_L2')")
    public ResponseEntity<ApiResponse<Void>> claimEscalatedCase(@PathVariable Long caseId) {
        Long currentOfficerId = securityUtils.getCurrentUserPrincipal().getUserId();
        caseService.claimEscalatedCase(caseId, currentOfficerId);

        return ResponseEntity.ok(ApiResponse.success("Escalated case claimed successfully.", null));
    }

    /**
     * POST: Make the final Cleared/Rejected decision.
     * STRICT ROLE: Only L2 Officers can resolve cases.
     */
    @PostMapping("/{caseId}/resolve")
//    @PreAuthorize("hasRole('ROLE_OFFICER_L2')")
    public ResponseEntity<ApiResponse<Void>> resolveCase(
            @PathVariable Long caseId,
            @Valid @RequestBody ResolveCaseDto dto) {

        Long currentOfficerId = securityUtils.getCurrentUserPrincipal().getUserId();
        caseService.resolveCase(caseId, dto, currentOfficerId);

        return ResponseEntity.ok(ApiResponse.success("Case resolved successfully.", null));
    }
}