package com.ticketflow.ticketflow.ticket.repository;

import com.ticketflow.ticketflow.ticket.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByOwnerUserId(Long userId);
}
