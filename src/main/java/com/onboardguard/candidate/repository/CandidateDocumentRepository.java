package com.onboardguard.candidate.repository;

import com.onboardguard.candidate.entity.CandidateDocument;
import com.onboardguard.candidate.enums.CandidateDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CandidateDocumentRepository extends JpaRepository<CandidateDocument, Long> {
    List<CandidateDocument> findByCandidateId(Long candidateId);
    Optional<CandidateDocument> findByCandidateIdAndCandidateDocumentType(Long candidateId, CandidateDocumentType candidateDocumentType);
}