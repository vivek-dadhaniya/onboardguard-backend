package com.onboardguard.officer.mapper;

import com.onboardguard.officer.dto.AlertDetailDto;
import com.onboardguard.officer.entity.Alert;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AlertMapper {

    AlertDetailDto toDto(Alert alert);

}