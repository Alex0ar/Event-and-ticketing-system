package com.ticketflow.ticketflow.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.flywaydb.core.extensibility.Tier;

import java.util.List;

public record ReservationRequest(
        @NotNull Long eventId,
        @NotNull @Valid List<Item> items
) {
    public record Item(
            @NotNull Long tierId,
            @NotNull @Min(1) Integer quantity
    ) {}
}
