package com.ticketflow.ticketflow.event.service;

import com.ticketflow.ticketflow.auditlog.domain.AuditLogAction;
import com.ticketflow.ticketflow.auditlog.domain.AuditReason;
import com.ticketflow.ticketflow.auditlog.service.AuditLogService;
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
import com.ticketflow.ticketflow.order.domain.Order;
import com.ticketflow.ticketflow.order.domain.OrderStatus;
import com.ticketflow.ticketflow.order.repository.OrderRepository;
import com.ticketflow.ticketflow.order.service.OrderService;
import com.ticketflow.ticketflow.reservation.domain.Reservation;
import com.ticketflow.ticketflow.reservation.repository.ReservationRepository;
import com.ticketflow.ticketflow.reservation.service.ReservationService;
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
    private final ReservationRepository reservationRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final AuditLogService auditLogService;
    private final ReservationService reservationService;

    public EventService(EventRepository eventRepository, VenueRepository venueRepository, TicketTierRepository tierRepository, CurrentUserProvider currentUser, ReservationRepository reservationRepository, OrderRepository orderRepository, OrderService orderService, AuditLogService auditLogService, ReservationService reservationService) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.tierRepository = tierRepository;
        this.currentUser = currentUser;
        this.reservationRepository = reservationRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.auditLogService = auditLogService;
        this.reservationService = reservationService;
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
        return toResponses(eventRepository.findByOrganizerId(currentUser.currentUserId()));
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
        return PageResponse.from(new PageImpl<>(toResponses(events.getContent()), pageable, events.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public EventResponse getPublic(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .filter(e -> e.getStatus() == EventStatus.PUBLISHED || e.getStatus() == EventStatus.SOLD_OUT)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        return toResponse(event, tierRepository.findByEventId(eventId));
    }

    @CacheEvict(value = CacheConfig.EVENT_SEARCH_CAHCE, allEntries = true)
    @Transactional
    public EventResponse cancelEvent(Long eventId) {
        Event event = loadOwned(eventId);
        if (event.getStatus().equals(EventStatus.CANCELLED)) {
            throw new ConflictException("Event has been already CANCELLED");
        }
        auditLogService.record(AuditLogAction.EVENT_CANCELATION, "event", event.getId(),
                Map.of("reason", AuditReason.EVENT_CANCELLED));
        reservationService.releasePendingForEvent(eventId);
        List<Long> reservationIds = reservationRepository.findByEventId(eventId).stream()
                .map(Reservation::getId).toList();
        List<Order> paidOrders = reservationIds.isEmpty()
                ? List.of()
                : orderRepository.findByReservationIdInAndStatus(reservationIds, OrderStatus.PAID);
        paidOrders.forEach(orderService::refundForCancelledEvent);
        event.setStatus(EventStatus.CANCELLED);
        return toResponse(eventRepository.save(event), tierRepository.findByEventId(eventId));
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

    /** Maps a page of events with 2 extra queries total (tiers + venues) — no N+1. */
    private List<EventResponse> toResponses(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        Map<Long, List<TicketTier>> tiersByEvent = tierRepository
                .findByEventIdIn(events.stream().map(Event::getId).toList()).stream()
                .collect(Collectors.groupingBy(TicketTier::getEventId));
        Map<Long, Venue> venues = venueRepository
                .findAllById(events.stream().map(Event::getVenueId).distinct().toList()).stream()
                .collect(Collectors.toMap(Venue::getId, v -> v));
        return events.stream()
                .map(e -> toResponse(e, tiersByEvent.getOrDefault(e.getId(), List.of()), venues.get(e.getVenueId())))
                .toList();
    }

    private EventResponse toResponse(Event e, List<TicketTier> tiers) {
        return toResponse(e, tiers, venueRepository.findById(e.getVenueId()).orElse(null));
    }

    private EventResponse toResponse(Event e, List<TicketTier> tiers, Venue venue) {
        List<TierResponse> tierResponses = tiers.stream()
                .sorted(java.util.Comparator.comparing(TicketTier::getPrice))
                .map(this::toTierResponse).toList();
        VenueSummary venueSummary = venue == null ? null
                : new VenueSummary(venue.getId(), venue.getName(), venue.getCity(), venue.getCountry());
        return new EventResponse(e.getId(), e.getVenueId(), venueSummary, e.getTitle(), e.getDescription(),
                e.getCategory(), e.getStartsAt(), e.getEndsAt(), e.getSalesStartsAt(), e.getSalesEndsAt(),
                e.getStatus(), e.getBannerUrl(), tierResponses);
    }

    private TierResponse toTierResponse(TicketTier t) {
        int available = t.getTotalQuantity() - t.getReservedQuantity() - t.getSoldQuantity();
        return new TierResponse(t.getId(), t.getName(), t.getDescription(), t.getPrice(), t.getPriceCurrency(), t.getTotalQuantity(), available, t.getMaxPerOrder());
    }
}
