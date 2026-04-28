package com.onboardguard.admin.service;

import com.onboardguard.admin.dto.DashboardReportDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

public interface ReportService {

    DashboardReportDto generateDashboard();
}
