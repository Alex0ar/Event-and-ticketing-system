package com.ticketflow.ticketflow.event.dto;

import com.ticketflow.ticketflow.event.domain.EventStatus;

import java.time.Instant;
import java.util.List;

public record EventResponse(
        Long id,
        Long venueId,
        VenueSummary venue,
        String title,
        String description,
        String category,
        Instant startsAt,
        Instant endsAt,
        Instant salesStartsAt,
        Instant salesEndsAt,
        EventStatus status,
        String bannerUrl,
        List<TierResponse> tiers
) {
}
