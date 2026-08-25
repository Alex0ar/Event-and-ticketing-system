package com.ticketflow.ticketflow.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VenueRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String country,
        @NotNull @Min(1) Integer capacity
) {
}
