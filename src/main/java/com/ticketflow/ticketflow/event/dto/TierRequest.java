package com.ticketflow.ticketflow.event.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TierRequest(
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin(value = "0.00")BigDecimal priceAmount,
        @NotBlank @Size(min = 3, max = 3) String priceCurrency,
        @NotNull @Min(1) Integer totalQuantity,
        @NotNull @Min(1) Integer maxPerOrder
) {
}
