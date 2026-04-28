package com.onboardguard.admin.service;

import com.onboardguard.admin.dto.SystemConfigResponseDto;
import com.onboardguard.admin.dto.UpdateSystemConfigDto;
import com.onboardguard.shared.common.enums.RoleCode;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SystemConfigAdminService {

    void requestConfigUpdate(Long configId, UpdateSystemConfigDto updateDto,
                             Long currentUserId, RoleCode currentUserRole);

    List<SystemConfigResponseDto> getAllConfigs();
}
