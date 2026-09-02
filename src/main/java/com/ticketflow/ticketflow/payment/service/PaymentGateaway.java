package com.ticketflow.ticketflow.payment.service;

import com.ticketflow.ticketflow.order.domain.Order;
import com.ticketflow.ticketflow.payment.domain.Payment;
import com.ticketflow.ticketflow.payment.dto.PaymentResponse;

import java.math.BigDecimal;

public interface PaymentGateaway {
    Payment initiatePayment(Order o, String idempotencyKey);
    PaymentResponse charge(Long paymentId);
    PaymentResponse refund(Long OrderId);
}
