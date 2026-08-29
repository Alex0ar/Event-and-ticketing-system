package com.ticketflow.ticketflow.payment.repository;

import com.ticketflow.ticketflow.payment.domain.Payment;
import com.ticketflow.ticketflow.payment.dto.PaymentResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
    PaymentResponse findByOrderId(Long orderId);
}
