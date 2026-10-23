package com.ticketflow.ticketflow.event.dto;

import com.ticketflow.ticketflow.event.domain.EventStatus;

import java.math.BigDecimal;
import java.util.List;

public record EventStatsResponse(
        Long eventId,
        String title,
        EventStatus status,
        String currency,
        int capacity,
        int sold,
        int reserved,
        int available,
        BigDecimal revenue,
        long checkedIn,
        List<TierStats> tiers,
        List<DailySales> dailySales
) {
    public record DailySales(String day, long tickets) {}
}
