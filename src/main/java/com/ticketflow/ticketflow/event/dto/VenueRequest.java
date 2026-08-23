package com.ticketflow.ticketflow.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record VenueRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String country,
        @NotBlank @Min(1) Integer capacity
) {
}
