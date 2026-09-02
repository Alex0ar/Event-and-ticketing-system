package com.ticketflow.ticketflow.payment.service;

import com.ticketflow.ticketflow.common.error.NotFoundException;
import com.ticketflow.ticketflow.order.domain.Order;
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
    public Payment initiatePayment(Order o, String idempotencyKey) {
        Payment p = new Payment();
        p.setOrderId(o.getId());
        p.setIdempotencyKey(idempotencyKey);
        p.setAmount(o.getTotal());
        p.setUserId(currentUser.currentUserId());
        p.setCurrency(o.getCurrency());
        p.setStatus(PaymentStatus.INITIATED);
        return p;
    }

    @Override
    public PaymentResponse charge(Long paymentId) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        p.setStatus(PaymentStatus.SUCCESSED);
        return toResponse(paymentRepository.save(p));
    }

    @Override
    public PaymentResponse refund(Long orderId) {
        Payment p =  paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        if  (p.getStatus() == PaymentStatus.SUCCESSED) {
            p.setStatus(PaymentStatus.REFUNDED);
        }
        return toResponse(paymentRepository.save(p));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getStatus(), payment.getAmount(), payment.getCurrency());
    }
}
