package com.ticketflow.ticketflow.payment.service;

import com.ticketflow.ticketflow.common.error.NotFoundException;
import com.ticketflow.ticketflow.order.domain.Order;
import com.ticketflow.ticketflow.payment.domain.Payment;
import com.ticketflow.ticketflow.payment.domain.PaymentStatus;
import com.ticketflow.ticketflow.payment.dto.PaymentResponse;
import com.ticketflow.ticketflow.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Stand-in for a real payment provider. Charges succeed unless the current HTTP request
 * carries {@code X-Mock-Payment-Outcome: DECLINE}, which lets the demo UI and tests
 * exercise the decline-and-retry path without touching the {@link PaymentGateaway} contract.
 */
@Service
public class MockPaymentGateaway implements PaymentGateaway {
    public static final String OUTCOME_HEADER = "X-Mock-Payment-Outcome";

    private final PaymentRepository paymentRepository;

    public MockPaymentGateaway(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Payment initiatePayment(Order o, String idempotencyKey) {
        Payment p = new Payment();
        p.setOrderId(o.getId());
        p.setIdempotencyKey(idempotencyKey);
        p.setAmount(o.getTotal());
        p.setUserId(o.getUserId());
        p.setCurrency(o.getCurrency());
        p.setStatus(PaymentStatus.INITIATED);
        return p;
    }

    @Override
    public PaymentResponse charge(Long paymentId) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        p.setStatus(declineRequested() ? PaymentStatus.FAILED : PaymentStatus.SUCCESSED);
        return toResponse(paymentRepository.save(p));
    }

    @Override
    public PaymentResponse refund(Long orderId) {
        Payment p = paymentRepository.findFirstByOrderIdAndStatus(orderId, PaymentStatus.SUCCESSED)
                .orElseThrow(() -> new NotFoundException("No successful payment to refund"));
        p.setStatus(PaymentStatus.REFUNDED);
        return toResponse(paymentRepository.save(p));
    }

    private boolean declineRequested() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return "DECLINE".equalsIgnoreCase(attrs.getRequest().getHeader(OUTCOME_HEADER));
        }
        return false;
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getStatus(), payment.getAmount(), payment.getCurrency());
    }
}
