package com.ticketflow.ticketflow.order.repository;

import com.ticketflow.ticketflow.order.domain.Order;
import com.ticketflow.ticketflow.order.dto.OrderResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<OrderResponse> findAllByUserId(Long userId);
}
