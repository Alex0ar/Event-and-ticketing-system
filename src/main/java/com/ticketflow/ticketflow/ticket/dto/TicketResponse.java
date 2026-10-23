package com.ticketflow.ticketflow.ticket.dto;

import com.ticketflow.ticketflow.ticket.domain.TicketStatus;

import java.time.Instant;

public record TicketResponse(
        String ownerName,
        TicketStatus status,
        String uuidCode,
        Long ownerId,
        Long tierId,
        String tierName,
        Long orderId,
        Long eventId,
        String eventTitle,
        Instant eventStartsAt,
        String venueName,
        String venueCity,
        Instant checkedInAt
) {
}
