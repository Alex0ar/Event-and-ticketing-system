package com.ticketflow.ticketflow.reservation;

import com.ticketflow.ticketflow.IntegrationTest;
import com.ticketflow.ticketflow.event.domain.Event;
import com.ticketflow.ticketflow.event.domain.EventStatus;
import com.ticketflow.ticketflow.event.domain.TicketTier;
import com.ticketflow.ticketflow.event.domain.Venue;
import com.ticketflow.ticketflow.event.repository.EventRepository;
import com.ticketflow.ticketflow.event.repository.TicketTierRepository;
import com.ticketflow.ticketflow.event.repository.VenueRepository;
import com.ticketflow.ticketflow.reservation.domain.Reservation;
import com.ticketflow.ticketflow.reservation.domain.ReservationItem;
import com.ticketflow.ticketflow.reservation.domain.ReservationStatus;
import com.ticketflow.ticketflow.reservation.repository.ReservationItemRepository;
import com.ticketflow.ticketflow.reservation.repository.ReservationRepository;
import com.ticketflow.ticketflow.reservation.service.ReservationExpiryJob;
import com.ticketflow.ticketflow.user.domain.Role;
import com.ticketflow.ticketflow.user.domain.User;
import com.ticketflow.ticketflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ReservationExpiryTest extends IntegrationTest {
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TicketTierRepository tierRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private ReservationItemRepository reservationItemRepository;
    @Autowired
    private ReservationExpiryJob reservationExpiryJob;

    Long organizerId;

    @BeforeEach
    void setup() {
        tierRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Long createUser() {
        User org = new User();
        org.setEmail("org@test.com");
        org.setPasswordHash("x");
        org.setFullName("Org");
        org.setRoles(Set.of(Role.ORGANIZER));
        return userRepository.save(org).getId();
    }

    private Long publishedEventWithTiers(String title, String category, Long venueId, int tierCount) {
        Event e = new Event();
        e.setTitle(title);
        e.setCategory(category);
        e.setOrganizerId(organizerId);
        e.setVenueId(venueId);
        e.setStartsAt(Instant.now().plus(30, ChronoUnit.DAYS));
        e.setEndsAt(Instant.now().plus(31, ChronoUnit.DAYS));
        e.setSalesStartsAt(Instant.now().plus(20, ChronoUnit.DAYS));
        e.setSalesEndsAt(Instant.now().plus(25, ChronoUnit.DAYS));
        e.setStatus(EventStatus.PUBLISHED);
        Long eventId = eventRepository.save(e).getId();

        for (int i = 0; i < tierCount; i++) {
            TicketTier tier = new TicketTier();
            tier.setEventId(eventId);
            tier.setName(String.format("tier-%d", i));
            tier.setPrice(new BigDecimal("50.00"));
            tier.setPriceCurrency("EUR");
            tier.setTotalQuantity(10);
            tier.setMaxPerOrder(4);
            tierRepository.save(tier);
        }
        return eventId;
    }

    private Long venue(String city) {
        Venue v = new Venue();
        v.setCity(city);
        v.setName("Venue - " + city);
        v.setAddress("addr");
        v.setCountry("DE");
        v.setCapacity(10000);
        v.setOrganizerId(organizerId);
        return venueRepository.save(v).getId();
    }

    private ReservationItem createReservationItem(Long reservationId, Long tierId) {
        Optional<TicketTier> tier = tierRepository.findById(tierId);
        ReservationItem item = new ReservationItem();
        item.setReservationId(reservationId);
        item.setUnitPriceAmount(new BigDecimal(String.valueOf(tier.get().getPrice())));
        item.setTierId(tierId);
        item.setQuantity(1);
        item.setCreatedAt(Instant.now().minus(11, ChronoUnit.MINUTES));
        return reservationItemRepository.save(item);
    }

    private Reservation createReservation() {
        Long venueId = venue("Berlin");
        Long eventId = publishedEventWithTiers("Rock in Berlin", "CONCERT", venueId, 1);
        Reservation r = new Reservation();
        r.setStatus(ReservationStatus.PENDING);
        r.setEventId(eventId);
        r.setUserId(organizerId);
        r.setCreatedAt(Instant.now().minus(11, ChronoUnit.MINUTES));
        r.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        Reservation saved = reservationRepository.save(r);
        createReservationItem(saved.getId(), tierRepository.findByEventId(eventId).getFirst().getId());
        return saved;
    }

    @Test
    void check_exprired_reservations() {
        setup();
        organizerId = createUser();
        Reservation reservation = createReservation();
        reservationExpiryJob.findExpiredReservations();
        int expiredReservations = reservationRepository.findByStatus(ReservationStatus.EXPIRED).size();
        boolean stillReserved = tierRepository.existsByReservedQuantityGreaterThan(0);
        assertThat(expiredReservations).isEqualTo(1);
        assertThat(stillReserved).isFalse();
    }
}
