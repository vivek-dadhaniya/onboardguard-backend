package com.onboardguard.officer.mapper;

import com.onboardguard.officer.dto.CaseDetailDto;
import com.onboardguard.officer.entity.Case;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {CaseNoteMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CaseMapper {

    CaseDetailDto toDto(Case investigationCase);

}