package com.ticketflow.ticketflow.event.repository;

import com.ticketflow.ticketflow.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    List<Event> findByOrganizerId(Long organizerId);
}
