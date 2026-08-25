package com.ticketflow.ticketflow.event.service;

import com.ticketflow.ticketflow.common.error.ForbidenException;
import com.ticketflow.ticketflow.common.error.NotFoundException;
import com.ticketflow.ticketflow.common.error.UnauthorizedException;
import com.ticketflow.ticketflow.event.domain.Venue;
import com.ticketflow.ticketflow.event.dto.VenueRequest;
import com.ticketflow.ticketflow.event.dto.VenueResponse;
import com.ticketflow.ticketflow.event.repository.VenueRepository;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

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
        venue.setOrganizerId(currentUser.currentUserId());
        return toResponse(venueRepository.save(venue));
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> listMine() {
        return venueRepository.findByOrganizerId(currentUser.currentUserId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VenueResponse getOwned(Long id) {
        return toResponse(loadOwned(id));
    }

    @Transactional
    public VenueResponse update(Long id, VenueRequest request) {
        Venue venue = loadOwned(id);
        venue.setName(request.name());
        venue.setAddress(request.address());
        venue.setCity(request.city());
        venue.setCountry(request.country());
        venue.setCapacity(request.capacity());
        venue.setOrganizerId(currentUser.currentUserId());
        return toResponse(venueRepository.save(venue));
    }

    @Transactional
    public void delete(Long venueId) {
        venueRepository.delete(loadOwned(venueId));
    }

    private Venue loadOwned(Long venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new NotFoundException("Venue not found"));
        if(!venue.getOrganizerId().equals(currentUser.currentUserId())) {
            throw new ForbidenException("You do not own this Venue");
        }
        return venue;
    }

    private VenueResponse toResponse(Venue venue) {
        return new VenueResponse(venue.getId(), venue.getName(), venue.getAddress(), venue.getCity(), venue.getCountry(), venue.getCapacity());
    }
}
