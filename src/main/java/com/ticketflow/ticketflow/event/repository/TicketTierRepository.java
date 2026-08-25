package com.ticketflow.ticketflow.event.repository;

import com.ticketflow.ticketflow.event.domain.TicketTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TicketTierRepository extends JpaRepository<TicketTier, Long> {
    List<TicketTier> findByEventId(Long eventId);
    List<TicketTier> findByEventIdIn(Collection<Long> eventIds);
}
