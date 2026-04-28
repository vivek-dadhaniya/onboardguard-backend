package com.onboardguard.officer.service;

import com.onboardguard.officer.dto.CaseNoteDto;
import org.springframework.transaction.annotation.Transactional;

public interface CaseNoteService {

    CaseNoteDto addInvestigationNote(Long caseId, String content, Long officerId);

}
