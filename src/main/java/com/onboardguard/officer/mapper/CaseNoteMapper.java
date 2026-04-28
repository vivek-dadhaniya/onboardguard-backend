package com.onboardguard.officer.mapper;

import com.onboardguard.officer.dto.CaseNoteDto;
import com.onboardguard.officer.entity.CaseNote;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CaseNoteMapper {

    CaseNoteDto toDto(CaseNote caseNote);

}