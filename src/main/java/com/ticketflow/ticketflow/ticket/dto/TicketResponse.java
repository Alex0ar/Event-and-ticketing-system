package com.ticketflow.ticketflow.ticket.dto;

import com.ticketflow.ticketflow.ticket.domain.TicketStatus;

public record TicketResponse(
        String ownerName,
        TicketStatus status,
        String uuidCode,
        Long ownerId,
        Long tierId
) {
}
