package com.onboardguard.admin.service.impl;

import com.onboardguard.admin.dto.AuditLogDto;
import com.onboardguard.admin.entity.AuditLog;
import com.onboardguard.admin.mapper.AuditLogMapper;
import com.onboardguard.admin.repository.AuditLogRepository;
import com.onboardguard.admin.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.onboardguard.shared.common.events.BusinessLogEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    /**
     * 1. THE LISTENER: Asynchronously catches events fired from Watchlist, Candidate, or Officer modules.
     */
    @Async
    @EventListener
    @Transactional
    @Override
    public void handleBusinessLogEvent(BusinessLogEvent event) {
        log.info("Recording Business Audit Log for {} ID: {}", event.entityType(), event.entityId());

        AuditLog auditLog = AuditLog.builder()
                .entityType(event.entityType())
                .entityId(event.entityId())
                .action(event.action())
                .oldStatus(event.oldStatus())
                .newStatus(event.newStatus())
                .performedBy(event.performedBy())
                .actorRole(event.actorRole())
                .remarks(event.remarks())
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    /**
     * 2. THE UI PROVIDER: Returns the timeline for a specific candidate/entry.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> getEntityHistory(String entityType, Long entityId) {
        return auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(auditLogMapper::toDto)
                .toList();
    }
}
