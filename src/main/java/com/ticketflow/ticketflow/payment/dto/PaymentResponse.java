package com.ticketflow.ticketflow.payment.dto;

import com.ticketflow.ticketflow.payment.domain.PaymentStatus;

import java.math.BigDecimal;

public record PaymentResponse(
        Long id,
        Long orderId,
        PaymentStatus status,
        BigDecimal amount,
        String currency
) {
}
