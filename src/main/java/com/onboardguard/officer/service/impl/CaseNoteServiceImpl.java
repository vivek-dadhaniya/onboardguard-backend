package com.onboardguard.officer.service.impl;

import com.onboardguard.officer.dto.CaseNoteDto;
import com.onboardguard.officer.entity.Case;
import com.onboardguard.officer.entity.CaseNote;
import com.onboardguard.officer.mapper.CaseNoteMapper;
import com.onboardguard.officer.repository.CaseRepository;
import com.onboardguard.officer.service.CaseNoteService;
import com.onboardguard.shared.common.enums.CaseStatus;
import com.onboardguard.shared.common.enums.NoteType;
import com.onboardguard.shared.common.exception.ResourceNotFoundException;
import com.onboardguard.shared.common.exception.UnauthorizedAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseNoteServiceImpl implements CaseNoteService {

    private final CaseRepository caseRepository;
    private final CaseNoteMapper caseNoteMapper;

    /**
     * Append a manual investigation note to an active Case.
     */
    @Override
    @Transactional
    public CaseNoteDto addInvestigationNote(Long caseId, String content, Long officerId) {

        Case investigationCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));

        // 1. Prevent tampering with resolved cases
        if (investigationCase.getStatus() == CaseStatus.RESOLVED) {
            throw new IllegalStateException("Cannot add notes to a RESOLVED case. The audit trail is permanently locked.");
        }

        // 2. Prevent random officers from dropping notes on cases they don't own
        if (!officerId.equals(investigationCase.getAssignedOfficerId())) {
            throw new UnauthorizedAccessException("You can only add notes to cases that are assigned to you.");
        }

        // 3. Build the strictly append-only note
        CaseNote note = CaseNote.builder()
                .investigationCase(investigationCase)
                .authorId(officerId)
                .content(content)
                .noteType(NoteType.INVESTIGATION)
                .build();

        // 4. Hibernate cascades the save
        investigationCase.getNotes().add(note);
        caseRepository.save(investigationCase);

        log.info("Officer ID {} appended an investigation note to Case ID {}", officerId, caseId);

        return caseNoteMapper.toDto(note);
    }
}