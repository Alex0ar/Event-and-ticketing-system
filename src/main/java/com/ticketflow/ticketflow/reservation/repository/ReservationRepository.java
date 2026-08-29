package com.ticketflow.ticketflow.reservation.repository;

import com.ticketflow.ticketflow.reservation.domain.Reservation;
import com.ticketflow.ticketflow.reservation.domain.ReservationStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByUserIdAndStatus(Long userId, ReservationStatus status);
    List<Reservation> findAllByUserId(Long userId);
    Reservation findByUserIdAndStatus(Long userId, ReservationStatus status);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);

    List<Reservation> findByStatus(ReservationStatus status);
}
