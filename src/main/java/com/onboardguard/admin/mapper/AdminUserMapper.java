package com.onboardguard.admin.mapper;

import com.onboardguard.admin.dto.UserResponseDto;
import com.onboardguard.auth.entity.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * componentModel = "spring" allows you to inject this via @RequiredArgsConstructor
 * unmappedTargetPolicy = IGNORE prevents compiler warnings for fields we intentionally skip.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminUserMapper {

    // MapStruct will automatically map matching fields and IGNORE the passwordHash!
    UserResponseDto toDto(AppUser user);

}