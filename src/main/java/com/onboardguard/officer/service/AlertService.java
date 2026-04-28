package com.onboardguard.officer.service;

import com.onboardguard.officer.dto.AlertDetailDto;
import org.springframework.transaction.annotation.Transactional;

public interface AlertService {

    AlertDetailDto acknowledgeAlert(Long alertId, Long officerId);

    @Transactional
    AlertDetailDto claimNextAvailableAlert(Long officerId);

    void dismissAlert(Long alertId, Long officerId, String reason);

    Long convertToCase(Long alertId, Long officerId);
}
