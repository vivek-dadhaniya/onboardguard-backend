package com.onboardguard.auth.mapper;

import com.onboardguard.auth.dto.request.CreateOfficerDto;
import com.onboardguard.auth.dto.request.RegisterCandidateDto;
import com.onboardguard.auth.dto.response.CandidateLoginResponseDto;
import com.onboardguard.auth.dto.response.StaffLoginResponseDto;
import com.onboardguard.auth.entity.AppUser;
import com.onboardguard.shared.security.CustomUserDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthMapper {

    // --- toEntity ---

    @Mapping(target = "email",        source = "dto.email")
    @Mapping(target = "fullName",     source = "dto.fullName")
    @Mapping(target = "phone",        source = "dto.phone")
    @Mapping(target = "passwordHash", source = "encodedPassword")
    @Mapping(target = "role",         constant = "ROLE_CANDIDATE")
    @Mapping(target = "active",       constant = "true")
    @Mapping(target = "locked",       constant = "false")
    AppUser toEntity(RegisterCandidateDto dto, String encodedPassword);

    @Mapping(target = "email",         source = "dto.email")
    @Mapping(target = "fullName",      source = "dto.fullName")
    @Mapping(target = "phone",         source = "dto.phone")
    @Mapping(target = "passwordHash",  source = "encodedPassword")
    @Mapping(target = "role",          source = "dto.role")
    @Mapping(target = "active",        constant = "true")
    @Mapping(target = "locked",        constant = "false")
    @Mapping(target = "createdByUser", source = "createdBy")
    AppUser toEntity(CreateOfficerDto dto, String encodedPassword, AppUser createdBy);

    // --- toDto (Register flow: AppUser entity → CandidateLoginResponseDto) ---
    // Used by registerCandidate() — entity is freshly saved, no principal yet
    @Mapping(target = "token",           source = "token")
    @Mapping(target = "expiresInSeconds",source = "expiresInSeconds")
    @Mapping(target = "email",           source = "user.email")
    @Mapping(target = "fullName",        source = "user.fullName")
    CandidateLoginResponseDto toCandidateDto(AppUser user, String token, long expiresInSeconds);

    // --- toDto (Login flow: CustomUserDetails principal → CandidateLoginResponseDto) ---
    // Used by loginCandidate() — principal already loaded from Redis/DB
    @Mapping(target = "token",           source = "token")
    @Mapping(target = "expiresInSeconds",source = "expiresInSeconds")
    @Mapping(target = "email",           source = "principal.email")
    @Mapping(target = "fullName",        source = "principal.fullName")
    CandidateLoginResponseDto toCandidateDto(CustomUserDetails principal, String token, long expiresInSeconds);

    // --- toDto (Staff login: CustomUserDetails principal → StaffLoginResponseDto) ---
    @Mapping(target = "token",           source = "token")
    @Mapping(target = "roleCode",        source = "roleCode")
    @Mapping(target = "expiresInSeconds",source = "expiresInSeconds")
    @Mapping(target = "email",           source = "principal.email")
    @Mapping(target = "fullName",        source = "principal.fullName")
    StaffLoginResponseDto toStaffDto(CustomUserDetails principal, String token, String roleCode, long expiresInSeconds);
}