package com.ticketflow.ticketflow.event.dto;

import java.math.BigDecimal;

public record TierResponse(
        Long id,
        String name,
        String description,
        BigDecimal priceAmount,
        String priceCurrency,
        Integer totalQuantity,
        Integer available,
        Integer maxPerOrder
) {
}
