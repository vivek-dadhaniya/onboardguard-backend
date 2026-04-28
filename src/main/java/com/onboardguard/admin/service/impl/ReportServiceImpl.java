package com.onboardguard.admin.service.impl;

import com.onboardguard.admin.dto.DashboardReportDto;
import com.onboardguard.admin.repository.ApprovalRequestRepository;
import com.onboardguard.admin.service.ReportService;
import com.onboardguard.candidate.repository.CandidateRepository;
import com.onboardguard.candidate.enums.OnboardingStatus;
import com.onboardguard.shared.common.enums.RequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    // Injecting the required repositories to aggregate data
    private final ApprovalRequestRepository approvalRequestRepository;

     private final CandidateRepository candidateRepository;
    // private final AlertRepository alertRepository;
    // private final CaseRepository caseRepository;

    /**
     * Generates the entire Dashboard.
     * Caches the result in Redis under the "dashboardStats" bucket.
     * The key is a static string 'master' so all admins share the same cached stats.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardStats", key = "'master'")
    public DashboardReportDto generateDashboard() {
        log.info("Cache Miss! Running heavy SQL aggregation queries for the Admin Dashboard...");

        // 1. Gather Candidate Stats
        DashboardReportDto.CandidateStats candidateStats = DashboardReportDto.CandidateStats.builder()
                .totalOnboarded(candidateRepository.count())
                .pendingScreening(candidateRepository.countByOnboardingStatus(OnboardingStatus.SCREENING_IN_PROGRESS))
                .cleared(candidateRepository.countByOnboardingStatus(OnboardingStatus.SCREENING_CLEARED))
                .flagged(candidateRepository.countByOnboardingStatus(OnboardingStatus.FLAGGED))
                .build();

        // 2. Gather Alert Stats
        DashboardReportDto.AlertStats alertStats = DashboardReportDto.AlertStats.builder()
                .totalGenerated(400L)      // alertRepository.count()
                .openAlerts(45L)           // alertRepository.countByStatus("OPEN")
                .dismissedFalsePositives(300L)
                .escalatedToCases(55L)
                .build();

        // 3. Gather Case Stats
        DashboardReportDto.CasePerformanceStats caseStats = DashboardReportDto.CasePerformanceStats.builder()
                .totalOpenCases(15L)
                .totalResolvedCases(40L)
                .averageResolutionTimeHours(24.5) // caseRepository.getAverageResolutionTime()
                .slaBreachedCases(3L)             // caseRepository.countSlaBreached()
                .build();

        // 4. Gather Category Hit Frequency (Mocked map for example)
        Map<String, Long> categoryHits = Map.of(
                "SANCTIONS", 120L,
                "PEP", 45L,
                "ADVERSE_MEDIA", 80L
        );

        // 5. Gather Pending Maker-Checker Approvals (Your Super Admin Inbox)
        long pendingApprovals = approvalRequestRepository.countByStatus(RequestStatus.PENDING);

        log.info("Dashboard aggregation complete. Saving to Redis Cache.");

        // 6. Build the final nested immutable Record
        return DashboardReportDto.builder()
                .candidateStats(candidateStats)
                .alertStats(alertStats)
                .casePerformanceStats(caseStats)
                .categoryHitFrequency(categoryHits)
                .pendingMakerCheckerRequests(pendingApprovals)
                .build();
    }
}