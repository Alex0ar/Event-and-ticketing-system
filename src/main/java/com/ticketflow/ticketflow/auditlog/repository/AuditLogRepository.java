package com.ticketflow.ticketflow.auditlog.repository;

import com.ticketflow.ticketflow.auditlog.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
