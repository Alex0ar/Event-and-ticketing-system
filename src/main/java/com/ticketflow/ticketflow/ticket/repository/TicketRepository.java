package com.ticketflow.ticketflow.ticket.repository;

import com.ticketflow.ticketflow.ticket.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
