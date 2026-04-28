package com.onboardguard.officer.mapper;

import com.onboardguard.candidate.dto.response.DocumentResponseDto;
import com.onboardguard.candidate.entity.Candidate;
import com.onboardguard.candidate.entity.CandidatePersonalDetail;
import com.onboardguard.candidate.entity.CandidateProfessionalDetail;
import com.onboardguard.officer.dto.CandidateQueueItemDto;
import com.onboardguard.officer.dto.CandidateVerificationDashboardDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OfficerCandidateMapper {

    // 1. The Main Mapping Method
    @Mapping(source = "candidate.id", target = "candidateId")
    @Mapping(source = "candidate.user.email", target = "email")
    // MapStruct Magic: One entity feeds two different UI objects!
    @Mapping(source = "candidate.personalDetail", target = "personalInfo")
    @Mapping(source = "candidate.personalDetail", target = "addressInfo")
    @Mapping(source = "candidate.professionalDetail", target = "professionalInfo")
    @Mapping(source = "documents", target = "documents")
    CandidateVerificationDashboardDto toDashboardDto(Candidate candidate, List<DocumentResponseDto> documents);

    // 2. Personal Info Mapping
    // No @Mapping needed! firstName, lastName, panNumber perfectly match the entity.
    CandidateVerificationDashboardDto.PersonalInfoDto toPersonalInfoDto(CandidatePersonalDetail detail);

    // 3. Address Info Mapping (Splitting out the address fields)
    @Mapping(source = "addressCity", target = "city")
    @Mapping(source = "addressState", target = "state")
    @Mapping(source = "addressPincode", target = "pincode")
    @Mapping(source = "addressCountry", target = "country")
    CandidateVerificationDashboardDto.AddressInfoDto toAddressInfoDto(CandidatePersonalDetail detail);

    // 4. Professional Info Mapping
    // No @Mapping needed! currentOrganization and totalExperienceYears match perfectly.
    CandidateVerificationDashboardDto.ProfessionalInfoDto toProfessionalInfoDto(CandidateProfessionalDetail detail);

    // Translates the entity into a lightweight row for the UI Grid
    @Mapping(source = "id", target = "candidateId")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "personalDetail.firstName", target = "firstName")
    @Mapping(source = "personalDetail.lastName", target = "lastName")
    CandidateQueueItemDto toQueueItemDto(Candidate candidate);
}