package com.ticketflow.ticketflow.payment.service;

import com.ticketflow.ticketflow.payment.dto.PaymentResponse;

import java.math.BigDecimal;

public interface PaymentGateaway {
    PaymentResponse charge(long orderId, String idempotencyKey, BigDecimal amount, String currency);
}
