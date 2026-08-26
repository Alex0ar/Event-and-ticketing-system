package com.ticketflow.ticketflow.reservation.dto;

import com.ticketflow.ticketflow.reservation.domain.ReservationStatus;
import com.ticketflow.ticketflow.reservation.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReservationResponse (
        Long id,
        Long eventId,
        ReservationStatus status,
        Instant expiresAt,
        long secondsUntilExpiry,
        BigDecimal total,
        String currency,
        List<Line> items
) {
    public record Line(
            Long tierId,
            String tierName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}
}
