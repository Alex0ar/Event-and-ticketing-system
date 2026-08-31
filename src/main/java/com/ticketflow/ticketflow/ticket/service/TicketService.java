package com.ticketflow.ticketflow.ticket.service;

import com.ticketflow.ticketflow.common.error.NotFoundException;
import com.ticketflow.ticketflow.reservation.domain.ReservationItem;
import com.ticketflow.ticketflow.reservation.repository.ReservationItemRepository;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import com.ticketflow.ticketflow.ticket.domain.Ticket;
import com.ticketflow.ticketflow.ticket.domain.TicketStatus;
import com.ticketflow.ticketflow.ticket.dto.TicketResponse;
import com.ticketflow.ticketflow.ticket.repository.TicketRepository;
import com.ticketflow.ticketflow.user.domain.User;
import com.ticketflow.ticketflow.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {
    private final CurrentUserProvider currentUser;
    private final TicketRepository ticketRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final ReservationItemRepository reservationRepository;
    private final UserRepository userRepository;

    public TicketService(CurrentUserProvider currentUser, TicketRepository ticketRepository, ReservationItemRepository reservationItemRepository, ReservationItemRepository reservationRepository, UserRepository userRepository) {
        this.currentUser = currentUser;
        this.ticketRepository = ticketRepository;
        this.reservationItemRepository = reservationItemRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
    }

    public void createAndSaveTicket(Long orderId, Long reservationId) {
        Long eventId = reservationRepository.findById(reservationId).get().getId();
        List<ReservationItem> items = reservationItemRepository.findByReservationId(reservationId);
        for  (ReservationItem item : items) {
            for (int i = 0; i < item.getQuantity(); i++) {
                Ticket ticket = new Ticket();
                ticket.setEventId(eventId);
                ticket.setOrderId(orderId);
                ticket.setTierId(item.getTierId());
                ticket.setStatus(TicketStatus.VALID);
                ticket.setOwnerUserId(currentUser.currentUserId());
                ticket.setUuidCode(UUID.randomUUID().toString());
                ticketRepository.save(ticket);
            }
        }
    }

    public Ticket getOwnedByCode (String code) {
        return ticketRepository.findByUuidCode(code)
                .orElseThrow(() -> new NotFoundException("Ticket not found for the introduced code: " + code));
    }

    public List<TicketResponse> listOwnedTickets() {
        User owner = userRepository.findById(currentUser.currentUserId())
                .orElseThrow(() -> new NotFoundException("Owner not found"));
        return ticketRepository.findByOwnerUserId(owner.getId()).stream()
                .map(t -> toResponse(t, owner.getFullName())).toList();
    }

    private TicketResponse toResponse(Ticket ticket, String ownerName) {
        return new TicketResponse(ownerName, ticket.getStatus(), ticket.getUuidCode(), ticket.getOwnerUserId(), ticket.getTierId());
    }
}
