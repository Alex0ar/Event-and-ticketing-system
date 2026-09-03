package com.ticketflow.ticketflow.event.api;

import com.sun.jdi.request.EventRequest;
import com.ticketflow.ticketflow.event.dto.EventCreateRequest;
import com.ticketflow.ticketflow.event.dto.EventResponse;
import com.ticketflow.ticketflow.event.dto.TierRequest;
import com.ticketflow.ticketflow.event.dto.TierResponse;
import com.ticketflow.ticketflow.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizer/events")
@PreAuthorize("hasRole('ORGANIZER')")
public class EventAdminController {
    private final EventService eventService;

    public EventAdminController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createDraft(@Valid @RequestBody EventCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createDraft(request));
    }

    @GetMapping
    public List<EventResponse> listMine() {
        return eventService.listMine();
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return eventService.getOwned(id);
    }

    @PostMapping("/{id}/tiers")
    public ResponseEntity<TierResponse> addTier(@PathVariable Long id, @Valid @RequestBody TierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.addTier(id, request));
    }

    @PostMapping("/{id}/publish")
    public EventResponse publish(@PathVariable Long id) {
        return eventService.publish(id);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<EventResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.cancelEvent(id));
    }
}
