package com.onboardguard.admin.service;

import com.onboardguard.admin.dto.PendingApprovalDto;
import com.onboardguard.admin.dto.ReviewApprovalRequestDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MakerCheckerService {

    List<PendingApprovalDto> getPendingInbox();

    void processReview(Long requestId, ReviewApprovalRequestDto reviewDto, String checkerEmail);
}
