package com.ticketflow.ticketflow.auditlog.domain;

import com.ticketflow.ticketflow.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {
    @Column(nullable = false)
    private Long actorUserId;
    @Column(nullable = false, length = 64)
    private String action;
    @Column(nullable = false, length = 64)
    private String entityType;
    @Column(nullable = false)
    private Long entityId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payloadJson;
}
