package com.ticketflow.ticketflow.auditlog.domain;

import com.ticketflow.ticketflow.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
