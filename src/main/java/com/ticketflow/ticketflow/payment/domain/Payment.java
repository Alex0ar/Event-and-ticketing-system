package com.ticketflow.ticketflow.payment.domain;

import com.ticketflow.ticketflow.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {
    @Column(nullable = false)
    Long userId;
    @Column(nullable = false)
    Long orderId;
    @Column(nullable = false)
    String providerRef;
    @Column(nullable = false)
    BigDecimal amount;
    @Column(nullable = false)
    PaymentStatus status = PaymentStatus.INITIATED;
    @Column(nullable = false)
    String idempotencyKey;
    @Column(nullable = false)
    String currency;
}
