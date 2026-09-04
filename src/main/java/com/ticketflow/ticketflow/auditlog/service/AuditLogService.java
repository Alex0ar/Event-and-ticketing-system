package com.ticketflow.ticketflow.auditlog.service;

import com.ticketflow.ticketflow.auditlog.domain.AuditLog;
import com.ticketflow.ticketflow.auditlog.domain.AuditLogAction;
import com.ticketflow.ticketflow.auditlog.repository.AuditLogRepository;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class AuditLogService {
    private final CurrentUserProvider currentUser;
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(CurrentUserProvider currentUser, AuditLogRepository auditLogRepository) {
        this.currentUser = currentUser;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog record(AuditLogAction action, String entityType, Long entityId, Map<String, Object> payloadJson) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setActorUserId(currentUser.currentUserId());
        auditLog.setEntityId(entityId);
        auditLog.setEntityType(entityType);
        auditLog.setPayloadJson(payloadJson);
        auditLog.setCreatedAt(Instant.now());
        return auditLogRepository.save(auditLog);
    }
}
