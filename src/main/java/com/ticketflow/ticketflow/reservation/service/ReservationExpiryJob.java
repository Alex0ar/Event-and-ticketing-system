package com.ticketflow.ticketflow.reservation.service;

import com.ticketflow.ticketflow.reservation.domain.Reservation;
import com.ticketflow.ticketflow.reservation.domain.ReservationStatus;
import com.ticketflow.ticketflow.reservation.repository.ReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class ReservationExpiryJob {
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public ReservationExpiryJob(ReservationRepository reservationRepository, ReservationService reservationService) {
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void findExpiredReservations() {
        List<Reservation> expired = reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, Instant.now());
        for (Reservation r : expired) {
            Long reservationId = r.getId();
            reservationService.returnInventory(reservationId);
            r.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(r);
        }
    }
}
