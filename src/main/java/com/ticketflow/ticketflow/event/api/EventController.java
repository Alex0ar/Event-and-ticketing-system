package com.ticketflow.ticketflow.event.api;

import com.ticketflow.ticketflow.common.dto.PageResponse;
import com.ticketflow.ticketflow.event.dto.EventResponse;
import com.ticketflow.ticketflow.event.dto.EventSearchFilter;
import com.ticketflow.ticketflow.event.service.EventService;
import org.hibernate.engine.jdbc.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {
    private final EventService eventService;
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public PageResponse<EventResponse> search(EventSearchFilter filter, @PageableDefault(size = 20, sort = "startsAt") Pageable pageable) {
        return eventService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return eventService.getPublic(id);
    }
}
