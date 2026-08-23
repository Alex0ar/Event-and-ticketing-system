package com.ticketflow.ticketflow.event.repository;

import com.ticketflow.ticketflow.event.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    List<Venue> findByOrganizerId(Long organizerId);
}
