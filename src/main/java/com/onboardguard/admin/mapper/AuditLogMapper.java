package com.onboardguard.admin.mapper;

import com.onboardguard.admin.dto.AuditLogDto;
import com.onboardguard.admin.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogMapper {

    AuditLogDto toDto(AuditLog entity);

}