package com.ticketflow.ticketflow.ticket.service;

import com.ticketflow.ticketflow.reservation.domain.ReservationItem;
import com.ticketflow.ticketflow.reservation.repository.ReservationItemRepository;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import com.ticketflow.ticketflow.ticket.domain.Ticket;
import com.ticketflow.ticketflow.ticket.domain.TicketStatus;
import com.ticketflow.ticketflow.ticket.dto.TicketResponse;
import com.ticketflow.ticketflow.ticket.repository.TicketRepository;
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

    public TicketService(CurrentUserProvider currentUser, TicketRepository ticketRepository, ReservationItemRepository reservationItemRepository, ReservationItemRepository reservationRepository) {
        this.currentUser = currentUser;
        this.ticketRepository = ticketRepository;
        this.reservationItemRepository = reservationItemRepository;
        this.reservationRepository = reservationRepository;
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

    public List<TicketResponse> listOwnedTickets() {
        List<Ticket> tickets = ticketRepository.findByOwnerUserId(currentUser.currentUserId());
        List<TicketResponse> responses = tickets.stream().map(this::toResponse).toList();  // DE VAZUT DACA ESTE CORECT
        return responses;
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse()   // DE TERMINAT DE FACUT
    }
}
