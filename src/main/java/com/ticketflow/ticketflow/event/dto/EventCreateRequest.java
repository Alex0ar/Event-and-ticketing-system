package com.ticketflow.ticketflow.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record EventCreateRequest(
        @NotNull Long venueId,
        @NotBlank String title,
        String description,
        @NotBlank String category,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @NotNull Instant salesStartsAt,
        @NotNull Instant salesEndsAt,
        String bannerUrl
) {
}
