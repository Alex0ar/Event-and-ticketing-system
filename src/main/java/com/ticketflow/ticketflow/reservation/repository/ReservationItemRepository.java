package com.ticketflow.ticketflow.reservation.repository;

import com.ticketflow.ticketflow.reservation.domain.ReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationItemRepository extends JpaRepository<ReservationItem, Long> {
    List<ReservationItem> findByReservationId(Long id);
}
