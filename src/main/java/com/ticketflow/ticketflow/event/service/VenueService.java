package com.ticketflow.ticketflow.event.service;

import com.ticketflow.ticketflow.event.domain.Venue;
import com.ticketflow.ticketflow.event.dto.VenueRequest;
import com.ticketflow.ticketflow.event.dto.VenueResponse;
import com.ticketflow.ticketflow.event.repository.VenueRepository;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class VenueService {
    private final VenueRepository venueRepository;
    private final CurrentUserProvider currentUser;


    public VenueService(VenueRepository venueRepository, CurrentUserProvider currentUser) {
        this.venueRepository = venueRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public VenueResponse create(VenueRequest request) {
        Venue venue = new Venue();
        venue.setName(request.name());
        venue.setAddress(request.address());
        venue.setCity(request.city());
        venue.setCountry(request.country());
        venue.setCapacity(request.capacity());
        return toResponse(venueRepository.save(venue));
    }

    // aici mai sunt lucruri care trebuie de adaugat

    @Transactional
    public void delete(Long venueId) {
        venueRepository.delete(loadOwned(venueId));
    }

    private Venue loadOwned(Long venueId) {
        Venue venue = venueRepository.findById(venueId) // cum adica findById daca aceasta functie nu este declarata in VenueRepository

    }

    private VenueResponse toResponse(Venue venue) {
        return new VenueResponse(venue.getId(), venue.getName(), venue.getAddress(), venue.getCity(), venue.getCountry(), venue.getCapacity());
    }
}
