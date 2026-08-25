package com.ticketflow.ticketflow.event.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record EventSearchFilter(
        String query,
        String city,
        String category,
        Instant startsAfter,
        Instant startsBefore,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
