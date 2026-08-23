package com.ticketflow.ticketflow.event.dto;

public record VenueResponse(
        Long id,
        String name,
        String address,
        String city,
        String country,
        Integer capacity
) {
}
