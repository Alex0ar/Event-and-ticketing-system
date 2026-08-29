package com.ticketflow.ticketflow.order.dto;

import com.ticketflow.ticketflow.order.domain.OrderStatus;

import java.math.BigDecimal;

public record OrderResponse(
        Long id,
        Long userId,
        Long reservationId,
        OrderStatus status,
        BigDecimal total,
        String currency
) {
}
