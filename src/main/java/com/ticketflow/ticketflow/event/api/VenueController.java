package com.ticketflow.ticketflow.event.api;

import com.ticketflow.ticketflow.event.dto.VenueRequest;
import com.ticketflow.ticketflow.event.dto.VenueResponse;
import com.ticketflow.ticketflow.event.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/venues")
@PreAuthorize("hasRole('ORGANIZER')")
public class VenueController {
    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    public ResponseEntity<VenueResponse> create(@Valid @RequestBody VenueRequest venueRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venueService.create(venueRequest));
    }

    @GetMapping
    public List<VenueResponse> listMine() {
        return venueService.listMine();
    }

    @GetMapping("/{id}")
    public VenueResponse get(@PathVariable Long id) {
        return venueService.getOwned(id);
    }

    @PostMapping("/{id}")
    public VenueResponse update(@PathVariable Long id, @Valid @RequestBody VenueRequest venueRequest) {
        return venueService.update(id, venueRequest);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        venueService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
