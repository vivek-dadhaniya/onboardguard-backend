package com.onboardguard.auth.service.impl;

import com.onboardguard.auth.dto.request.CreateOfficerDto;
import com.onboardguard.auth.dto.request.LoginRequestDto;
import com.onboardguard.auth.dto.request.RegisterCandidateDto;
import com.onboardguard.auth.dto.response.CandidateLoginResponseDto;
import com.onboardguard.auth.dto.response.StaffLoginResponseDto;
import com.onboardguard.auth.entity.AppUser;
import com.onboardguard.auth.mapper.AuthMapper;
import com.onboardguard.auth.repository.AppUserRepository;
import com.onboardguard.auth.service.AuthService;
import com.onboardguard.auth.service.OfficerCredentialGenerator;
import com.onboardguard.shared.common.enums.RoleCode;
import com.onboardguard.shared.common.events.CandidateRegisteredEvent;
import com.onboardguard.shared.common.events.OfficerCreatedEvent;
import com.onboardguard.shared.common.exception.BadRequestException;
import com.onboardguard.shared.security.CustomUserDetails;
import com.onboardguard.shared.security.CustomUserDetailsService;
import com.onboardguard.shared.security.JwtTokenProvider;
import com.onboardguard.shared.security.SecurityConstants;
import com.onboardguard.shared.security.SecurityUtils;
import com.onboardguard.shared.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService blacklistService;
    private final OfficerCredentialGenerator credentialGenerator;
    private final AuthMapper authMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityUtils securityUtils;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Override
    public CandidateLoginResponseDto loginCandidate(LoginRequestDto dto) {
        Authentication auth = doAuthenticate(dto.email(), dto.password());
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();

        if (!principal.isCandidate()) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(auth);
        updateLastLogin(dto.email());
        log.info("Candidate login: email={}", dto.email());

        return authMapper.toCandidateDto(principal, token, jwtExpirationMs / 1000);
    }

    @Override
    public StaffLoginResponseDto loginStaff(LoginRequestDto dto) {
        Authentication auth = doAuthenticate(dto.email(), dto.password());
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();

        if (principal.isCandidate()) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(auth);
        updateLastLogin(dto.email());
        log.info("Staff login: email={} role={}", dto.email(), principal.getRole().name());

        return authMapper.toStaffDto(principal, token, principal.getRole().name(), jwtExpirationMs / 1000);
    }

    @Override
    @Transactional
    public CandidateLoginResponseDto registerCandidate(RegisterCandidateDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        AppUser user = authMapper.toEntity(dto, passwordEncoder.encode(dto.password()));
        AppUser saved = userRepository.save(user);

        String token = jwtTokenProvider.generateTokenForUser(saved);
        updateLastLogin(saved.getEmail()); // Ensure last login is set on registration
        eventPublisher.publishEvent(new CandidateRegisteredEvent(saved.getEmail(), saved.getFullName()));

        log.info("Candidate registered: email={}", saved.getEmail());

        return authMapper.toCandidateDto(saved, token, jwtExpirationMs / 1000);
    }

    @Transactional
    @Override
    public void createOfficer(CreateOfficerDto dto, AppUser createdBy) {

        if (userRepository.existsByEmail(dto.email())) {
            throw new BadRequestException("An account with this email already exists.");
        }

        if (dto.role() != RoleCode.ROLE_OFFICER_L1 && dto.role() != RoleCode.ROLE_OFFICER_L2) {
            log.warn("Security Alert: User Id {} attempted to create an unauthorized role: {}", createdBy.getId(), dto.role());
            throw new SecurityException("This API is strictly limited to provisioning L1 and L2 Officers.");
        }

        // 1. Generate secure password
        String rawPassword = credentialGenerator.generatePassword();

        // 2. Create the Officer entity using Mapper
        AppUser officer = authMapper.toEntity(dto, passwordEncoder.encode(rawPassword), createdBy);

        userRepository.save(officer);

        // 3. Fire event to trigger 'officer-welcome.html' async email
        eventPublisher.publishEvent(
                new OfficerCreatedEvent(
                        officer.getEmail(),
                        officer.getFullName(),
                        rawPassword,
                        officer.getRole().name(),
                        createdBy.getEmail()
                )
        );
        log.info("Officer created: email={} by={}", officer.getEmail(), createdBy.getEmail());
    }

    @Override
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            throw new BadRequestException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
        String email = jwtTokenProvider.getUsername(token);

        blacklistService.blacklist(token);
        userDetailsService.evictCache(email);

        log.info("Logout successful: email={}", email);
    }

    private Authentication doAuthenticate(String email, String password) {
        try {
            return authManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        } catch (DisabledException e) {
            throw new DisabledException("Account is deactivated");
        } catch (LockedException e) {
            throw new LockedException("Account is locked");
        }
    }

    private void updateLastLogin(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            u.setLastLoginAt(Instant.now());
            userRepository.save(u);
            userDetailsService.evictCache(email);
        });
    }
}