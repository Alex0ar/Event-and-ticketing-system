package com.ticketflow.ticketflow.reservation.service;

import com.ticketflow.ticketflow.common.error.ConflictException;
import com.ticketflow.ticketflow.common.error.ForbidenException;
import com.ticketflow.ticketflow.common.error.NotFoundException;
import com.ticketflow.ticketflow.event.domain.Event;
import com.ticketflow.ticketflow.event.domain.EventStatus;
import com.ticketflow.ticketflow.event.domain.TicketTier;
import com.ticketflow.ticketflow.event.repository.EventRepository;
import com.ticketflow.ticketflow.event.repository.TicketTierRepository;
import com.ticketflow.ticketflow.reservation.domain.Reservation;
import com.ticketflow.ticketflow.reservation.domain.ReservationItem;
import com.ticketflow.ticketflow.reservation.domain.ReservationStatus;
import com.ticketflow.ticketflow.reservation.dto.ReservationRequest;
import com.ticketflow.ticketflow.reservation.dto.ReservationResponse;
import com.ticketflow.ticketflow.reservation.repository.ReservationItemRepository;
import com.ticketflow.ticketflow.reservation.repository.ReservationRepository;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.ConfigurationException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReservationService {
    private static final Duration HOLD_DURATION = Duration.ofMinutes(10);

    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository itemRepository;
    private final TicketTierRepository tierRepository;
    private final EventRepository eventRepository;
    private final CurrentUserProvider currentUser;
    private final ReservationItemRepository reservationItemRepository;


    public ReservationService(ReservationRepository reservationRepository, ReservationItemRepository itemRepository, TicketTierRepository tierRepository, EventRepository eventRepository, CurrentUserProvider currentUser, ReservationItemRepository reservationItemRepository) {
        this.reservationRepository = reservationRepository;
        this.itemRepository = itemRepository;
        this.tierRepository = tierRepository;
        this.eventRepository = eventRepository;
        this.currentUser = currentUser;
        this.reservationItemRepository = reservationItemRepository;
    }

    @Transactional
    public ReservationResponse reserve(ReservationRequest requeset) {
        Long userId = currentUser.currentUserId();
        if (reservationRepository.existsByUserIdAndStatus(userId, ReservationStatus.PENDING)) {
            throw new ConflictException("You already have a pending reservation");
        }
        Event event = eventRepository.findById(requeset.eventId())
                .orElseThrow(() -> new NotFoundException("Event not found"));
        Instant now = Instant.now();
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ConflictException("Event not on sale!");
        }
        if(now.isBefore(event.getSalesStartsAt()) || now.isAfter(event.getSalesEndsAt())) {
            throw new ConflictException("Event in not within its sales window!");
        }
        Reservation r = new Reservation();
        r.setUserId(userId);
        r.setEventId(requeset.eventId());
        r.setStatus(ReservationStatus.PENDING);
        r.setExpiresAt(now.plus(HOLD_DURATION));
        Reservation saved = reservationRepository.save(r);
        List<ReservationItem> items = new ArrayList<>();
        for (ReservationRequest.Item item : requeset.items()) {
            TicketTier tier = tierRepository.findById(item.tierId())
                    .orElseThrow(() -> new NotFoundException("Ticket tier not found"));
            if (!tier.getEventId().equals(event.getId())) {
                throw new ConflictException("Tier does not belong to this event");
            }
            if (item.quantity() > tier.getMaxPerOrder()) {
                throw new ConflictException("Maximum number of orders exceeded");
            }

            int updated = tierRepository.reserveQuantity(tier.getId(), item.quantity());
            if (updated == 0) {
                throw new ConflictException("Not enought tickets for tier '" + tier.getName() + "'");
            }
            ReservationItem ri = new ReservationItem();
            ri.setReservationId(saved.getId());
            ri.setTierId(tier.getId());
            ri.setQuantity(item.quantity());
            ri.setUnitPriceAmount(tier.getPrice());
            items.add(itemRepository.save(ri));
        }
        return toResponse(saved, items);
    }

    @Transactional
    public void release(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        if (!reservation.getUserId().equals(currentUser.currentUserId())) {
            throw new ForbidenException("Not your reservation");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ConflictException("Only pending reservations can be released");
        }

        returnInventory(reservationId);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    public BigDecimal getReservationTotalCost(Reservation reservation) {
        List<ReservationItem> items = reservationItemRepository.findByReservationId(reservation.getId());
        BigDecimal total = BigDecimal.ZERO;
        for (ReservationItem item : items) {
            total = total.add(item.getUnitPriceAmount().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }

    public Reservation confirmReservation(Long ReservationId) {
        Reservation r =  reservationRepository.findById(ReservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        List<ReservationItem> items = reservationItemRepository.findByReservationId(ReservationId);
        for  (ReservationItem item : items) {
            tierRepository.reservedToSold(item.getTierId(), item.getQuantity());
        }
        r.setStatus(ReservationStatus.CONFIRMED);
        return reservationRepository.save(r);
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        if (!r.getUserId().equals(currentUser.currentUserId())) {
            throw new  ForbidenException("This reservation doesn't belong to you");
        }
        returnInventory(reservationId);
        r.setStatus(ReservationStatus.REFUNDED);
        return r;
    }

    // --- helper ---

    void returnInventory(Long reservationId) {
        for (ReservationItem item : itemRepository.findByReservationId(reservationId)) {
            tierRepository.releaseQuantity(item.getTierId(), item.getQuantity());
        }
    }

    private ReservationResponse toResponse(Reservation r, List<ReservationItem> items) {
        Map<Long, TicketTier> tiers = tierRepository.findAllById(items.stream().map(ReservationItem::getTierId).toList())
                .stream().collect(Collectors.toMap(TicketTier::getId, ticket -> ticket));
        BigDecimal total = BigDecimal.ZERO;
        List<ReservationResponse.Line> lines = new ArrayList<>();
        String currency = null;
        for (ReservationItem item : items) {
            TicketTier tier = tiers.get(item.getTierId());
            BigDecimal lineTotal = item.getUnitPriceAmount().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineTotal);
            currency = tier.getPriceCurrency();
            lines.add(new ReservationResponse.Line(
                    item.getTierId(), tier.getName(), item.getQuantity(),
                    item.getUnitPriceAmount(), lineTotal));
        }

        long secondsLeft = Math.max(0, Duration.between(Instant.now(), r.getExpiresAt()).getSeconds());
        return new ReservationResponse(r.getId(), r.getEventId(), r.getStatus(),
                r.getExpiresAt(), secondsLeft, total, currency, lines);
    }
}
