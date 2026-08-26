package com.ticketflow.ticketflow.reservation.repository;

import com.ticketflow.ticketflow.reservation.domain.Reservation;
import com.ticketflow.ticketflow.reservation.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByUserIdAndStatus(Long userId, ReservationStatus status);
    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);
}
