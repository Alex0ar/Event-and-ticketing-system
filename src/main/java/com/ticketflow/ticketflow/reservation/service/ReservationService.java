package com.ticketflow.ticketflow.reservation.service;

import com.ticketflow.ticketflow.event.repository.EventRepository;
import com.ticketflow.ticketflow.event.repository.TicketTierRepository;
import com.ticketflow.ticketflow.reservation.repository.ReservationItemRepository;
import com.ticketflow.ticketflow.reservation.repository.ReservationRepository;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class ReservationService {
    private static final Duration HOLD_DURATION = Duration.ofMinutes(10);

    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository itemRepository;
    private final TicketTierRepository ticketRepository;
    private final EventRepository eventRepository;
    private final CurrentUserProvider currentUser;


    public ReservationService(ReservationRepository reservationRepository, ReservationItemRepository itemRepository, TicketTierRepository ticketRepository, EventRepository eventRepository, CurrentUserProvider currentUser) {
        this.reservationRepository = reservationRepository;
        this.itemRepository = itemRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.currentUser = currentUser;
    }

    @Transactional
}
