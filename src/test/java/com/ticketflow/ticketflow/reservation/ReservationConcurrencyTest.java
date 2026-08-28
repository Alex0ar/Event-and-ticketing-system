package com.ticketflow.ticketflow.reservation;

import com.ticketflow.ticketflow.IntegrationTest;
import com.ticketflow.ticketflow.event.domain.Event;
import com.ticketflow.ticketflow.event.domain.EventStatus;
import com.ticketflow.ticketflow.event.domain.TicketTier;
import com.ticketflow.ticketflow.event.domain.Venue;
import com.ticketflow.ticketflow.event.repository.EventRepository;
import com.ticketflow.ticketflow.event.repository.TicketTierRepository;
import com.ticketflow.ticketflow.event.repository.VenueRepository;
import com.ticketflow.ticketflow.event.service.TicketTierService;
import com.ticketflow.ticketflow.reservation.service.ReservationService;
import com.ticketflow.ticketflow.user.domain.Role;
import com.ticketflow.ticketflow.user.domain.User;
import com.ticketflow.ticketflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class ReservationConcurrencyTest extends IntegrationTest {
    @Autowired
    private TicketTierService tierService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TicketTierRepository tierRepository;
    @Autowired
    private VenueRepository venueRepository;

    private static final Logger log = LoggerFactory.getLogger(ReservationConcurrencyTest.class);

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


    @Test
    void concurrent_reservation() throws InterruptedException {
        setup();
        organizerId = createUser();
        Long berlinVenue = venue("Berlin");
        Long eventId = publishedEventWithTiers("Jazz in Berlin", "CONCERT", berlinVenue, 1);
        Long tierId = tierRepository.findByEventId(eventId).get(0).getId();
        int requestsNumber = 200;
        var startGate = new CountDownLatch(1);
        var doneGate = new CountDownLatch(requestsNumber);
        var successes = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(32);
        for (int i = 0; i < requestsNumber; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    int result = tierRepository.reserveQuantity(tierId, 1);
                    if  (result == 1) {
                        successes.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    doneGate.countDown();
                }
            });
        }
        startGate.countDown();
        doneGate.await();
//        Long tierId = tierRepository.findByEventId(eventId).get(0).getId();
//        tierRepository.findById(tierId).ifPresent(tier -> {
//            log.info("DB tier: total={} reserved={} sold={}",
//                    tier.getTotalQuantity(), tier.getReservedQuantity(), tier.getSoldQuantity());
//        });
        assertThat(successes.get()).isEqualTo(10);
        boolean tierAvailabilityCorrect = tierService.checkTierAvailability(tierId);
        assertThat(tierAvailabilityCorrect).isTrue();

    }
}
