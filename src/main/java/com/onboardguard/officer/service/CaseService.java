package com.onboardguard.officer.service;

import com.onboardguard.officer.dto.CaseDetailDto;
import com.onboardguard.officer.dto.EscalateCaseDto;
import com.onboardguard.officer.dto.ResolveCaseDto;
import org.springframework.transaction.annotation.Transactional;

public interface CaseService {

    CaseDetailDto getCaseDetails(Long caseId);

    void escalateCase(Long caseId, EscalateCaseDto dto, Long l1OfficerId);

    void claimEscalatedCase(Long caseId, Long l2OfficerId);

    void resolveCase(Long caseId, ResolveCaseDto dto, Long l2OfficerId);
}
