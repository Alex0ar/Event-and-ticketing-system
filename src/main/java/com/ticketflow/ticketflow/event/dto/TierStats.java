package com.ticketflow.ticketflow.event.dto;

import java.math.BigDecimal;

/** One row per tier, built directly by a JPQL constructor expression. */
public record TierStats(
        Long tierId,
        String name,
        BigDecimal price,
        String currency,
        Integer total,
        Integer sold,
        Integer reserved,
        BigDecimal revenue,
        Long checkedIn
) {
}
