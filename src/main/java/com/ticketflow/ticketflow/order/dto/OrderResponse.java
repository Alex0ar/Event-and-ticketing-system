package com.ticketflow.ticketflow.order.dto;

import com.ticketflow.ticketflow.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        Long userId,
        Long reservationId,
        OrderStatus status,
        BigDecimal total,
        String currency,
        Long eventId,
        String eventTitle,
        Instant eventStartsAt,
        Instant createdAt
) {
}
