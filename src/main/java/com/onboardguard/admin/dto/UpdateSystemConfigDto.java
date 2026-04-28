package com.onboardguard.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateSystemConfigDto(

        @NotBlank(message = "Configuration value cannot be empty")
        String configValue,

        String description

        // We omit configKey, configType, and isSensitive here because
        // an Admin should not be able to change an API_KEY into a non-sensitive string!
) {}