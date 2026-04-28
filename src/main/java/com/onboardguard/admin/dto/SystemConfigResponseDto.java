package com.onboardguard.admin.dto;

import lombok.Builder;

@Builder
public record SystemConfigResponseDto (
     Long id,
     String configKey, // e.g., MAX_LOGIN_ATTEMPTS, EXTERNAL_API_KEY
     String configValue,
     String configType, // e.g., SECURITY, INTEGRATION, THRESHOLD
     String description,
     Boolean isSensitive// If true, frontend should display "********"
){}