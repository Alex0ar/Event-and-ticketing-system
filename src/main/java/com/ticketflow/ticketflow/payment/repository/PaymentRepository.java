package com.ticketflow.ticketflow.payment.repository;

import com.ticketflow.ticketflow.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}
