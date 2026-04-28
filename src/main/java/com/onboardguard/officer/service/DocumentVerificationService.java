package com.onboardguard.officer.service;

import com.onboardguard.candidate.dto.response.DocumentResponseDto;
import com.onboardguard.officer.dto.CandidateQueueItemDto;
import com.onboardguard.officer.dto.CandidateVerificationDashboardDto;

import java.util.List;

public interface DocumentVerificationService {

    List<DocumentResponseDto> getCandidateDocumentsForReview(Long candidateId);

    List<CandidateQueueItemDto> getPendingCandidatesQueue();

    void claimCandidateForVerification(Long candidateId, Long officerId);

    CandidateVerificationDashboardDto claimNextAvailableCandidate(Long officerId);

    CandidateVerificationDashboardDto getCandidateVerificationDetails(Long candidateId);

    void approveDocument(Long documentId, Long officerId);

    void rejectDocument(Long documentId, String reason, Long officerId);
}