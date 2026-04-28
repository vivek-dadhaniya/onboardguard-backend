package com.onboardguard.candidate.controller;

import com.onboardguard.candidate.dto.request.PersonalDetailsRequestDto;
import com.onboardguard.candidate.dto.request.ProfessionalDetailsRequestDto;
import com.onboardguard.candidate.dto.response.CandidateStatusResponseDto;
import com.onboardguard.candidate.service.impl.CandidateServiceImpl;
import com.onboardguard.shared.common.dto.ApiResponse;
import com.onboardguard.shared.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/candidates/profile")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateServiceImpl candidateService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<CandidateStatusResponseDto>> getProfileStatus() {
        CandidateStatusResponseDto status = candidateService.getStatus();
        return ResponseEntity.ok(ApiResponse.success("Status fetched successfully", status));
    }

    @PostMapping("/personal")
    public ResponseEntity<ApiResponse<Void>> updatePersonalDetails(
            @Valid @RequestBody PersonalDetailsRequestDto dto) {
        candidateService.savePersonalDetails(dto);
        return ResponseEntity.ok(ApiResponse.success("Personal details saved successfully", null));
    }

    @PostMapping("/professional")
    public ResponseEntity<ApiResponse<Void>> updateProfessionalDetails(
            @Valid @RequestBody ProfessionalDetailsRequestDto dto) {
        candidateService.saveProfessionalDetails(dto);
        return ResponseEntity.ok(ApiResponse.success("Professional details saved successfully", null));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<Void>> submitProfile() {
        candidateService.submitProfile();
        return ResponseEntity.ok(ApiResponse.success("Profile submitted successfully for verification", null));
    }
}