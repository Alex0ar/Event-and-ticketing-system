package com.ticketflow.ticketflow.event.service;

import com.ticketflow.ticketflow.event.domain.Event;
import com.ticketflow.ticketflow.event.dto.EventStatsResponse;
import com.ticketflow.ticketflow.event.dto.TierStats;
import com.ticketflow.ticketflow.event.repository.TicketTierRepository;
import com.ticketflow.ticketflow.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class EventStatsService {
    private final EventService eventService;
    private final TicketTierRepository tierRepository;
    private final TicketRepository ticketRepository;

    public EventStatsService(EventService eventService, TicketTierRepository tierRepository, TicketRepository ticketRepository) {
        this.eventService = eventService;
        this.tierRepository = tierRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public EventStatsResponse statsFor(Long eventId) {
        Event event = eventService.loadOwned(eventId);
        List<TierStats> tiers = tierRepository.statsForEvent(eventId);
        // Totals below only add up the handful of per-tier rows the database already aggregated.
        int capacity = tiers.stream().mapToInt(TierStats::total).sum();
        int sold = tiers.stream().mapToInt(TierStats::sold).sum();
        int reserved = tiers.stream().mapToInt(TierStats::reserved).sum();
        BigDecimal revenue = tiers.stream().map(TierStats::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        long checkedIn = tiers.stream().mapToLong(TierStats::checkedIn).sum();
        String currency = tiers.isEmpty() ? "EUR" : tiers.getFirst().currency();
        List<EventStatsResponse.DailySales> daily = ticketRepository.dailySales(eventId).stream()
                .map(r -> new EventStatsResponse.DailySales(r.getDay(), r.getTickets()))
                .toList();
        return new EventStatsResponse(event.getId(), event.getTitle(), event.getStatus(), currency,
                capacity, sold, reserved, capacity - sold - reserved, revenue, checkedIn, tiers, daily);
    }
}
