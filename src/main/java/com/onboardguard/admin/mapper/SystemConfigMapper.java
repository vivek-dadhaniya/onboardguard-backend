package com.onboardguard.admin.mapper;

import com.onboardguard.admin.dto.SystemConfigResponseDto;
import com.onboardguard.shared.config.entity.SystemConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SystemConfigMapper {

    // Tell MapStruct to use our custom method for the configValue field
    @Mapping(target = "configValue", expression = "java(maskIfSensitive(config))")
    SystemConfigResponseDto toResponseDto(SystemConfig config);

    /**
     * Custom logic to prevent API Keys or Passwords from leaking to the UI.
     */
    default String maskIfSensitive(SystemConfig config) {
        if (config == null) {
            return null;
        }
        return Boolean.TRUE.equals(config.getIsSensitive()) ? "********" : config.getConfigValue();
    }
}