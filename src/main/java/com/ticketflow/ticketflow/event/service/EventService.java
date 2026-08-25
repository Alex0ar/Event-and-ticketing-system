package com.ticketflow.ticketflow.event.service;

import com.ticketflow.ticketflow.common.dto.PageResponse;
import com.ticketflow.ticketflow.common.error.ConflictException;
import com.ticketflow.ticketflow.common.error.ForbidenException;
import com.ticketflow.ticketflow.common.error.NotFoundException;
import com.ticketflow.ticketflow.config.CacheConfig;
import com.ticketflow.ticketflow.event.domain.Event;
import com.ticketflow.ticketflow.event.domain.EventStatus;
import com.ticketflow.ticketflow.event.domain.TicketTier;
import com.ticketflow.ticketflow.event.domain.Venue;
import com.ticketflow.ticketflow.event.dto.*;
import com.ticketflow.ticketflow.event.repository.EventRepository;
import com.ticketflow.ticketflow.event.repository.EventSpecification;
import com.ticketflow.ticketflow.event.repository.TicketTierRepository;
import com.ticketflow.ticketflow.event.repository.VenueRepository;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventService {
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final TicketTierRepository tierRepository;
    private final CurrentUserProvider currentUser;

    public EventService(EventRepository eventRepository, VenueRepository venueRepository, TicketTierRepository tierRepository, CurrentUserProvider currentUser) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.tierRepository = tierRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public EventResponse createDraft (EventCreateRequest request) {
        Long organizerId = currentUser.currentUserId();
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new NotFoundException("Venue not found"));
        if (!venue.getOrganizerId().equals(organizerId)) {
            throw new ForbidenException("Venue does not belong to this organizer");
        }
        Event event = new Event();
        event.setVenueId(venue.getId());
        event.setOrganizerId(organizerId);
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setCategory(request.category());
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        event.setSalesStartsAt(request.salesStartsAt());
        event.setSalesEndsAt(request.salesEndsAt());
        event.setBannerUrl(request.bannerUrl());
        event.setStatus(EventStatus.DRAFT);

        return toResponse(eventRepository.save(event), List.of());
    }

    @Transactional
    public TierResponse addTier (Long eventId, TierRequest request) {
        Event event = loadOwned(eventId);
        if (!event.getStatus().equals(EventStatus.DRAFT)) {
            throw new ConflictException("Tiers can only be added on draft events");
        }
        TicketTier tier = new TicketTier();
        tier.setEventId(eventId);
        tier.setName(request.name());
        tier.setDescription(request.description());
        tier.setPrice(request.priceAmount());
        tier.setPriceCurrency(request.priceCurrency());
        tier.setTotalQuantity(request.totalQuantity());
        tier.setMaxPerOrder(request.maxPerOrder());

        return toTierResponse(tierRepository.save(tier));
    }

    @Transactional(readOnly = true)
    public EventResponse getOwned(Long eventId) {
        Event event = loadOwned(eventId);
        return toResponse(event, tierRepository.findByEventId(eventId));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listMine () {
        return eventRepository.findByOrganizerId(currentUser.currentUserId()).stream()
                .map(e -> toResponse(e, tierRepository.findByEventId(e.getId())))
                .toList();
    }

    @CacheEvict(value = CacheConfig.EVENT_SEARCH_CAHCE, allEntries = true)
    @Transactional
    public EventResponse publish(Long eventId) {
        Event event = loadOwned(eventId);
        if (!event.getStatus().equals(EventStatus.DRAFT)) {
            throw new ConflictException("only draft events can be published");
        }
        List<TicketTier> tiers = tierRepository.findByEventId(eventId);
        if (tiers.isEmpty()) {
            throw new ConflictException("An event needs at least one ticket tier before publishing");
        }
        if (!event.getSalesStartsAt().isBefore(event.getSalesEndsAt())) {
            throw new ConflictException("salesStartsAt must be before salesEndsAt");
        }
        if (event.getSalesEndsAt().isAfter(event.getStartsAt())) {
            throw new ConflictException("salesEndsAt must be before startsAt");
        }
        if (!event.getStartsAt().isBefore(event.getEndsAt())) {
            throw new ConflictException("startsAt must be before endsAt");
        }
        Venue venue = venueRepository.findById(event.getVenueId())
                .orElseThrow(() -> new NotFoundException("Venue not found"));
        int totalTierQuantity = tiers.stream().mapToInt(TicketTier::getTotalQuantity).sum();
        if (totalTierQuantity > venue.getCapacity()) {
            throw new ConflictException("Total tier quantity (" + totalTierQuantity + ") exceeds venue capacity (" + venue.getCapacity() + ")");
        }
        event.setStatus(EventStatus.PUBLISHED);
        return toResponse(eventRepository.save(event), tiers);
    }

    @Cacheable(value = CacheConfig.EVENT_SEARCH_CAHCE, key = "#filter.toString() + '|' + #pageable.toString()")
    @Transactional(readOnly = true)
    public PageResponse<EventResponse> search(EventSearchFilter filter, Pageable pageable) {
        Page<Event> events = eventRepository.findAll(EventSpecification.fromFilter(filter), pageable);
        List<Long> eventIds = events.getContent().stream().map(Event::getId).toList();
        Map<Long, List<TicketTier>> tiersByEvent = eventIds.isEmpty()
                ? Map.of()
                : tierRepository.findByEventIdIn(eventIds).stream()
                  .collect(Collectors.groupingBy(TicketTier::getEventId));
        List<EventResponse> content = events.getContent().stream()
                .map(e -> toResponse(e, tiersByEvent.getOrDefault(e.getId(), List.of()))).toList();
        return PageResponse.from(new PageImpl<>(content, pageable, events.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public EventResponse getPublic(Long eventId) {
        Event event = loadOwned(eventId);
        if (!event.getStatus().equals(EventStatus.PUBLISHED) &&  !event.getStatus().equals(EventStatus.SOLD_OUT)) {
            throw new ConflictException("Event has no status PUBLISHED or SOLD_OUT");
        }
        return toResponse(event, tierRepository.findByEventId(eventId));
    }

    // --- helper ---

    Event loadOwned(long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getOrganizerId().equals(currentUser.currentUserId())) {
            throw new ForbidenException("Event does not belong to this organizer");
        }
        return event;
    }

    private EventResponse toResponse(Event e, List<TicketTier> tiers) {
        List<TierResponse> tierResponses = tiers.stream()
                .map(this::toTierResponse).toList();
        return new EventResponse(e.getId(), e.getVenueId(), e.getTitle(), e.getDescription(), e.getCategory(), e.getStartsAt(), e.getEndsAt(), e.getSalesStartsAt(), e.getSalesEndsAt(), e.getStatus(), e.getBannerUrl(), tierResponses);
    }

    private TierResponse toTierResponse(TicketTier t) {
        int available = t.getTotalQuantity() - t.getReservedQuantity() - t.getSoldQuantity();
        return new TierResponse(t.getId(), t.getName(), t.getDescription(), t.getPrice(), t.getPriceCurrency(), t.getTotalQuantity(), available, t.getMaxPerOrder());
    }
}
