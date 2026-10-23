package com.ticketflow.ticketflow.order.repository;

import com.ticketflow.ticketflow.order.domain.Order;
import com.ticketflow.ticketflow.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Order> findByReservationId(Long reservationId);
    List<Order> findByReservationIdInAndStatus(List<Long> reservationIds, OrderStatus status);
}
