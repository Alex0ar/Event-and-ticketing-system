package com.ticketflow.ticketflow.checkin.service;

import com.ticketflow.ticketflow.checkin.dto.CheckInResponse;
import com.ticketflow.ticketflow.common.error.ApplicationException;
import com.ticketflow.ticketflow.common.error.NotFoundException;
import com.ticketflow.ticketflow.event.repository.EventRepository;
import com.ticketflow.ticketflow.event.repository.TicketTierRepository;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import com.ticketflow.ticketflow.ticket.domain.Ticket;
import com.ticketflow.ticketflow.ticket.repository.TicketRepository;
import com.ticketflow.ticketflow.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CheckInService {
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final TicketTierRepository tierRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUser;

    public CheckInService(TicketRepository ticketRepository, EventRepository eventRepository,
                          TicketTierRepository tierRepository, UserRepository userRepository,
                          CurrentUserProvider currentUser) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.tierRepository = tierRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public CheckInResponse checkIn(String ticketCode) {
        Instant now = Instant.now();
        // Atomic guard: only a VALID ticket flips to USED, so concurrent scans succeed exactly once.
        int updated = ticketRepository.ticketCheckIn(now, currentUser.currentUserId(), ticketCode);

        Ticket ticket = ticketRepository.findByUuidCode(ticketCode)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
        if (updated == 0) {
            throw switch (ticket.getStatus()) {
                case USED -> rejected("TICKET_ALREADY_USED", "Ticket was already checked in");
                case REFUNDED -> rejected("TICKET_REFUNDED", "Ticket was refunded");
                case VOID -> rejected("TICKET_VOID", "Ticket is void (event cancelled)");
                case VALID -> rejected("TICKET_NOT_VALID", "Ticket could not be checked in");
            };
        }
        return toResponse(ticket, now);
    }

    private CheckInResponse toResponse(Ticket ticket, Instant checkedInAt) {
        String holder = userRepository.findById(ticket.getOwnerUserId()).map(u -> u.getFullName()).orElse("—");
        String event = eventRepository.findById(ticket.getEventId()).map(e -> e.getTitle()).orElse("—");
        String tier = tierRepository.findById(ticket.getTierId()).map(t -> t.getName()).orElse("—");
        return new CheckInResponse(ticket.getUuidCode(), holder, event, tier, checkedInAt);
    }

    private static ApplicationException rejected(String code, String message) {
        return new ApplicationException(HttpStatus.CONFLICT, code, message);
    }
}
