package com.onboardguard.candidate.service;

import com.onboardguard.candidate.dto.request.PersonalDetailsRequestDto;
import com.onboardguard.candidate.dto.request.ProfessionalDetailsRequestDto;
import com.onboardguard.candidate.dto.response.CandidateStatusResponseDto;

public interface CandidateService {

    void savePersonalDetails(PersonalDetailsRequestDto dto);

    void saveProfessionalDetails(ProfessionalDetailsRequestDto dto);

    void submitProfile();

    CandidateStatusResponseDto getStatus();
}
