package com.ticketflow.ticketflow.event;

import com.ticketflow.ticketflow.IntegrationTest;
import com.ticketflow.ticketflow.event.domain.Event;
import com.ticketflow.ticketflow.event.domain.EventStatus;
import com.ticketflow.ticketflow.event.domain.TicketTier;
import com.ticketflow.ticketflow.event.domain.Venue;
import com.ticketflow.ticketflow.event.dto.EventSearchFilter;
import com.ticketflow.ticketflow.event.repository.EventRepository;
import com.ticketflow.ticketflow.event.repository.TicketTierRepository;
import com.ticketflow.ticketflow.event.repository.VenueRepository;
import com.ticketflow.ticketflow.event.service.EventService;
import com.ticketflow.ticketflow.user.domain.Role;
import com.ticketflow.ticketflow.user.domain.User;
import com.ticketflow.ticketflow.user.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class EventCatalogIntegrationTest extends IntegrationTest {
    @Autowired
    UserRepository userRepository;
    @Autowired
    EventRepository eventRepository;
    @Autowired
    VenueRepository venueRepository;
    @Autowired
    TicketTierRepository tierRepository;
    @Autowired
    EventService eventService;
    @Autowired
    EntityManagerFactory emf;

    Long organizerId;

    @BeforeEach
    void setup() {
        tierRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();
        userRepository.deleteAll();

        User org = new User();
        org.setEmail("org@test.com");
        org.setPasswordHash("x");
        org.setFullName("Org");
        org.setRoles(Set.of(Role.ORGANIZER));
        organizerId = userRepository.save(org).getId();
    }

    private Long publishedEventWithTiers(String title, String category, Long venueId, int tierCount) {
        Event e =  new Event();
        e.setOrganizerId(organizerId);
        e.setVenueId(venueId);
        e.setTitle(title);
        e.setCategory(category);
        e.setStartsAt(Instant.now().plus(30, ChronoUnit.DAYS));
        e.setEndsAt(Instant.now().plus(31, ChronoUnit.DAYS));
        e.setSalesStartsAt(Instant.now().minus(1, ChronoUnit.DAYS));
        e.setSalesEndsAt(Instant.now().plus(20, ChronoUnit.DAYS));
        e.setStatus(EventStatus.PUBLISHED);
        Long eventId = eventRepository.save(e).getId();

        for (int i = 0; i < tierCount; i++) {
            TicketTier tier = new TicketTier();
            tier.setEventId(eventId);
            tier.setName("tier " + i);
            tier.setPrice(new BigDecimal("50.00"));
            tier.setPriceCurrency("EUR");
            tier.setTotalQuantity(100);
            tier.setMaxPerOrder(4);
            tierRepository.save(tier);
        }
        return eventId;
    }

    private Long venue(String city) {
        Venue v = new Venue();
        v.setName("V-" + city);
        v.setAddress("addr");
        v.setCity(city);
        v.setCountry("DE");
        v.setCapacity(10_000);
        v.setOrganizerId(organizerId);
        return venueRepository.save(v).getId();
    }

    @Test
    void search_filtersByCity() {
        Long berlin = venue("Berlin");
        long paris =  venue("Paris");
        publishedEventWithTiers("Rock in Berlin", "CONCERT", berlin, 1);
        publishedEventWithTiers("Jazz in Paris", "CONCERT", paris, 1);
        var result = eventService.search(
                new EventSearchFilter(null, "Berlin", null, null, null, null, null),
                PageRequest.of(0, 20));
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).title()).isEqualTo("Rock in Berlin");
    }

    @Test
    void search_doesNotNPlusOne() {
        Long v = venue("Berlin");
        for (int i = 0; i < 10; i++) {
            publishedEventWithTiers("Event " + i, "CONCERT", v, 3);
        }
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var result =  eventService.search(
                new EventSearchFilter(null, null, null, null, null, null, null),
                PageRequest.of(0, 20));
        assertThat(result.content()).hasSize(10);
        long queryCount = stats.getPrepareStatementCount();
        assertThat(queryCount).isLessThanOrEqualTo(4);
    }
}
