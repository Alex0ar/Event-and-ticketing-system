package com.ticketflow.ticketflow.ticket.service;

import com.ticketflow.ticketflow.common.error.ConflictException;
import com.ticketflow.ticketflow.common.error.ForbidenException;
import com.ticketflow.ticketflow.common.error.NotFoundException;
import com.ticketflow.ticketflow.event.domain.Event;
import com.ticketflow.ticketflow.event.domain.TicketTier;
import com.ticketflow.ticketflow.event.domain.Venue;
import com.ticketflow.ticketflow.event.repository.EventRepository;
import com.ticketflow.ticketflow.event.repository.TicketTierRepository;
import com.ticketflow.ticketflow.event.repository.VenueRepository;
import com.ticketflow.ticketflow.reservation.domain.ReservationItem;
import com.ticketflow.ticketflow.reservation.repository.ReservationItemRepository;
import com.ticketflow.ticketflow.reservation.repository.ReservationRepository;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import com.ticketflow.ticketflow.ticket.domain.Ticket;
import com.ticketflow.ticketflow.ticket.domain.TicketStatus;
import com.ticketflow.ticketflow.ticket.dto.TicketResponse;
import com.ticketflow.ticketflow.ticket.repository.TicketRepository;
import com.ticketflow.ticketflow.user.domain.User;
import com.ticketflow.ticketflow.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TicketService {
    private final CurrentUserProvider currentUser;
    private final TicketRepository ticketRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketTierRepository tierRepository;
    private final VenueRepository venueRepository;

    public TicketService(CurrentUserProvider currentUser, TicketRepository ticketRepository,
                         ReservationRepository reservationRepository, ReservationItemRepository reservationItemRepository,
                         UserRepository userRepository, EventRepository eventRepository,
                         TicketTierRepository tierRepository, VenueRepository venueRepository) {
        this.currentUser = currentUser;
        this.ticketRepository = ticketRepository;
        this.reservationItemRepository = reservationItemRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.tierRepository = tierRepository;
        this.venueRepository = venueRepository;
    }

    /** Issues one ticket per reserved seat. Called inside the payment transaction. */
    public void createAndSaveTicket(Long orderId, Long reservationId, Long ownerUserId) {
        Long eventId = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found")).getEventId();
        List<ReservationItem> items = reservationItemRepository.findByReservationId(reservationId);
        for (ReservationItem item : items) {
            for (int i = 0; i < item.getQuantity(); i++) {
                Ticket ticket = new Ticket();
                ticket.setEventId(eventId);
                ticket.setOrderId(orderId);
                ticket.setTierId(item.getTierId());
                ticket.setStatus(TicketStatus.VALID);
                ticket.setOwnerUserId(ownerUserId);
                ticket.setUuidCode(UUID.randomUUID().toString());
                ticketRepository.save(ticket);
            }
        }
    }

    public Ticket getOwnedByCode(String code) {
        Ticket ticket = ticketRepository.findByUuidCode(code)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
        if (!ticket.getOwnerUserId().equals(currentUser.currentUserId())) {
            throw new ForbidenException("Ticket doesn't belong to you");
        }
        return ticket;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listOwnedTickets() {
        User owner = userRepository.findById(currentUser.currentUserId())
                .orElseThrow(() -> new NotFoundException("Owner not found"));
        List<Ticket> tickets = ticketRepository.findByOwnerUserIdOrderByCreatedAtDesc(owner.getId());
        if (tickets.isEmpty()) {
            return List.of();
        }
        // Batch-load everything the cards display: 3 queries regardless of ticket count.
        Map<Long, Event> events = byId(eventRepository.findAllById(ids(tickets, Ticket::getEventId)), Event::getId);
        Map<Long, TicketTier> tiers = byId(tierRepository.findAllById(ids(tickets, Ticket::getTierId)), TicketTier::getId);
        Map<Long, Venue> venues = byId(venueRepository.findAllById(
                events.values().stream().map(Event::getVenueId).distinct().toList()), Venue::getId);

        return tickets.stream().map(t -> {
            Event e = events.get(t.getEventId());
            TicketTier tier = tiers.get(t.getTierId());
            Venue v = e == null ? null : venues.get(e.getVenueId());
            return new TicketResponse(owner.getFullName(), t.getStatus(), t.getUuidCode(), t.getOwnerUserId(),
                    t.getTierId(), tier == null ? null : tier.getName(), t.getOrderId(), t.getEventId(),
                    e == null ? null : e.getTitle(), e == null ? null : e.getStartsAt(),
                    v == null ? null : v.getName(), v == null ? null : v.getCity(), t.getCheckedInAt());
        }).toList();
    }

    /** Customer refund: only VALID tickets can be refunded. Ownership is checked at order level. */
    @Transactional
    public List<Ticket> refundTickets(Long orderId) {
        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.REFUNDED) {
                throw new ConflictException("Ticket has already been refunded");
            }
            if (ticket.getStatus() != TicketStatus.VALID) {
                throw new ConflictException("Ticket not valid, so can't be refunded");
            }
            ticket.setStatus(TicketStatus.REFUNDED);
        }
        return tickets;
    }

    /** Event cancellation: every ticket of the order becomes VOID regardless of state. */
    @Transactional
    public void voidTickets(Long orderId) {
        ticketRepository.findByOrderId(orderId).forEach(t -> t.setStatus(TicketStatus.VOID));
    }

    // --- helpers ---

    private static <T> List<Long> ids(List<T> items, Function<T, Long> id) {
        return items.stream().map(id).distinct().toList();
    }

    private static <T> Map<Long, T> byId(List<T> items, Function<T, Long> id) {
        return items.stream().collect(Collectors.toMap(id, Function.identity()));
    }
}
