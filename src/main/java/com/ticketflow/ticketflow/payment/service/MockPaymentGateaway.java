package com.ticketflow.ticketflow.payment.service;

import com.ticketflow.ticketflow.payment.domain.Payment;
import com.ticketflow.ticketflow.payment.domain.PaymentStatus;
import com.ticketflow.ticketflow.payment.dto.PaymentResponse;
import com.ticketflow.ticketflow.payment.repository.PaymentRepository;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MockPaymentGateaway implements PaymentGateaway {
    private final CurrentUserProvider currentUser;
    private final PaymentRepository paymentRepository;

    public MockPaymentGateaway(CurrentUserProvider currentUser, PaymentRepository paymentRepository) {
        this.currentUser = currentUser;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PaymentResponse charge(long orderId, String idempotencyKey, BigDecimal amount, String currency) {
        Payment p = new Payment();
        p.setUserId(currentUser.currentUserId());
        p.setOrderId(orderId);
        p.setIdempotencyKey(idempotencyKey);
        p.setAmount(amount);
        p.setCurrency(currency);
        p.setStatus(PaymentStatus.SUCCESSED);
        return toResponse(paymentRepository.save(p));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getStatus(), payment.getAmount(), payment.getCurrency());
    }
}
