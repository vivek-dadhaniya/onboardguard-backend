package com.onboardguard.candidate.service.impl;

import com.onboardguard.auth.entity.AppUser;
import com.onboardguard.auth.repository.AppUserRepository;
import com.onboardguard.candidate.dto.request.PersonalDetailsRequestDto;
import com.onboardguard.candidate.dto.request.ProfessionalDetailsRequestDto;
import com.onboardguard.candidate.dto.response.CandidateStatusResponseDto;
import com.onboardguard.candidate.entity.Candidate;
import com.onboardguard.candidate.entity.CandidateProfessionalDetail;
import com.onboardguard.candidate.enums.CandidateType;
import com.onboardguard.candidate.enums.OnboardingStatus;
import com.onboardguard.candidate.mapper.CandidateMapper;
import com.onboardguard.candidate.repository.CandidateRepository;
import com.onboardguard.candidate.service.CandidateService;
import com.onboardguard.shared.common.exception.BadRequestException;
import com.onboardguard.shared.common.exception.ResourceNotFoundException;
import com.onboardguard.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final AppUserRepository userRepository;
    private final CandidateMapper candidateMapper;
    private final SecurityUtils securityUtils;

    private Candidate getOrCreateCandidate() {
        Long userId = securityUtils.getCurrentUserPrincipal().getUserId();
        return candidateRepository.findByUserId(userId)
                .orElseGet(() -> {
                    try {
                        AppUser user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

                        Candidate candidate = candidateMapper.toEntity(user);
                        return candidateRepository.save(candidate);

                    } catch (Exception ex) {
                        return candidateRepository.findByUserId(userId)
                                .orElseThrow(() -> new RuntimeException("Concurrent candidate creation failed", ex));
                    }
                });
    }

    private void ensureProfileNotSubmitted(Candidate candidate) {
        if (candidate.getOnboardingStatus() == OnboardingStatus.FORM_SUBMITTED ||
                candidate.getFormSubmittedAt() != null) {
            throw new BadRequestException("Profile already submitted. Modification not allowed.");
        }
    }

    @Override
    @PreAuthorize("hasAuthority('CANDIDATE_FORM_SUBMIT')")
    public void savePersonalDetails(PersonalDetailsRequestDto dto) {
        Candidate candidate = getOrCreateCandidate();
        ensureProfileNotSubmitted(candidate);

        if (candidate.getPersonalDetail() == null) {
            candidate.setPersonalDetail(candidateMapper.toPersonalDetailEntity(dto, candidate));
        } else {
            candidateMapper.updatePersonalDetailFromDto(dto, candidate.getPersonalDetail());
        }

        // Safe normalized name generation
        String normalizedName = (
                (dto.firstName() != null ? dto.firstName() : "") + " " +
                        (dto.middleName() != null ? dto.middleName() + " " : "") +
                        (dto.lastName() != null ? dto.lastName() : "")
        ).toUpperCase().trim();

        if (normalizedName.isEmpty()) {
            throw new BadRequestException("At least one name field (first, middle, last) must be provided");
        }

        candidate.getPersonalDetail().setFullNameNormalized(normalizedName);

        if (candidate.getOnboardingStatus().ordinal() < OnboardingStatus.PERSONAL_SAVED.ordinal()) {
            candidate.setOnboardingStatus(OnboardingStatus.PERSONAL_SAVED);
        }

        candidateRepository.save(candidate);
    }

    @Override
    @PreAuthorize("hasAuthority('CANDIDATE_FORM_SUBMIT')")
    public void saveProfessionalDetails(ProfessionalDetailsRequestDto dto) {
        Candidate candidate = getOrCreateCandidate();
        ensureProfileNotSubmitted(candidate);

        if (candidate.getProfessionalDetail() == null) {
            candidate.setProfessionalDetail(candidateMapper.initProfessionalDetail(candidate));
        }

        candidateMapper.updateProfessionalDetailFromDto(dto, candidate.getProfessionalDetail());

        if (candidate.getOnboardingStatus().ordinal() < OnboardingStatus.PROFESSIONAL_SAVED.ordinal()) {
            candidate.setOnboardingStatus(OnboardingStatus.PROFESSIONAL_SAVED);
        }

        candidateRepository.save(candidate);
    }

    @Override
    @PreAuthorize("hasAuthority('CANDIDATE_FORM_SUBMIT')")
    public void submitProfile() {
        Long userId = securityUtils.getCurrentUserPrincipal().getUserId();
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Profile not initialized."));

        ensureProfileNotSubmitted(candidate);

        if (candidate.getPersonalDetail() == null || candidate.getProfessionalDetail() == null) {
            throw new BadRequestException("Complete all sections before submitting.");
        }

        candidate.setOnboardingStatus(OnboardingStatus.FORM_SUBMITTED);
        candidate.setFormSubmittedAt(Instant.now());

        candidateRepository.save(candidate);
    }

    @Override
    @PreAuthorize("hasAuthority('CANDIDATE_STATUS_VIEW_OWN')")
    public CandidateStatusResponseDto getStatus() {
        Candidate candidate = getOrCreateCandidate();
        return candidateMapper.toStatusDto(candidate);
    }
}