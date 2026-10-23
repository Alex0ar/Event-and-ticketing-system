package com.ticketflow.ticketflow.event.dto;

/** The venue fields the catalog shows next to an event. */
public record VenueSummary(Long id, String name, String city, String country) {
}
