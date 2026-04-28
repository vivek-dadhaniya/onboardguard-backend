package com.onboardguard.auth.controller;

import com.onboardguard.auth.dto.request.LoginRequestDto;
import com.onboardguard.auth.dto.request.RegisterCandidateDto;
import com.onboardguard.auth.dto.response.CandidateLoginResponseDto;
import com.onboardguard.auth.dto.response.StaffLoginResponseDto;
import com.onboardguard.auth.service.AuthService;
import com.onboardguard.shared.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login/candidate")
    public ResponseEntity<ApiResponse<CandidateLoginResponseDto>> loginCandidate(
            @Valid @RequestBody LoginRequestDto dto) {
        CandidateLoginResponseDto response = authService.loginCandidate(dto);
        return ResponseEntity.ok(ApiResponse.success("Candidate login successful", response));
    }

    @PostMapping("/login/staff")
    public ResponseEntity<ApiResponse<StaffLoginResponseDto>> loginStaff(
            @Valid @RequestBody LoginRequestDto dto) {
        StaffLoginResponseDto response = authService.loginStaff(dto);
        return ResponseEntity.ok(ApiResponse.success("Staff login successful", response));
    }

    @PostMapping("/register/candidate")
    public ResponseEntity<ApiResponse<CandidateLoginResponseDto>> registerCandidate(
            @Valid @RequestBody RegisterCandidateDto dto) {
        CandidateLoginResponseDto response = authService.registerCandidate(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Candidate registered successfully", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}