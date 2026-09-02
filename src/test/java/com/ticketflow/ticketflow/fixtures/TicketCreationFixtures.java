package com.ticketflow.ticketflow.fixtures;

import com.ticketflow.ticketflow.event.domain.Event;
import com.ticketflow.ticketflow.event.domain.EventStatus;
import com.ticketflow.ticketflow.event.domain.TicketTier;
import com.ticketflow.ticketflow.event.domain.Venue;
import com.ticketflow.ticketflow.event.repository.EventRepository;
import com.ticketflow.ticketflow.event.repository.TicketTierRepository;
import com.ticketflow.ticketflow.event.repository.VenueRepository;
import com.ticketflow.ticketflow.order.domain.OrderStatus;
import com.ticketflow.ticketflow.order.dto.OrderResponse;
import com.ticketflow.ticketflow.order.service.OrderService;
import com.ticketflow.ticketflow.payment.repository.PaymentRepository;
import com.ticketflow.ticketflow.payment.service.PaymentGateaway;
import com.ticketflow.ticketflow.reservation.domain.Reservation;
import com.ticketflow.ticketflow.reservation.domain.ReservationItem;
import com.ticketflow.ticketflow.reservation.domain.ReservationStatus;
import com.ticketflow.ticketflow.reservation.repository.ReservationItemRepository;
import com.ticketflow.ticketflow.reservation.repository.ReservationRepository;
import com.ticketflow.ticketflow.reservation.service.ReservationExpiryJob;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import com.ticketflow.ticketflow.ticket.service.TicketService;
import com.ticketflow.ticketflow.user.domain.Role;
import com.ticketflow.ticketflow.user.domain.User;
import com.ticketflow.ticketflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Component
public class TicketCreationFixtures {
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
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentGateaway paymentGateaway;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private CurrentUserProvider currentUser;
    @Autowired
    private TicketService ticketService;


    public User createOrganizer() {
        User org = new User();
        org.setEmail("org@test.com");
        org.setPasswordHash("x");
        org.setFullName("Org");
        org.setRoles(Set.of(Role.ORGANIZER));
        return userRepository.save(org);
    }

    public User createCustomer() {
        User org = new User();
        org.setEmail("customer@test.com");
        org.setPasswordHash("y");
        org.setFullName("Customer");
        org.setRoles(Set.of(Role.CUSTOMER));
        return userRepository.save(org);
    }

    public User createGateStaff() {
        User org = new User();
        org.setEmail("gateStaff@test.com");
        org.setPasswordHash("y");
        org.setFullName("Gate Staff");
        org.setRoles(Set.of(Role.GATE_STAFF));
        return userRepository.save(org);
    }

    public OrderResponse createRezervationAndPayOrder(Long eventId) {
        Reservation reservation = createReservation(eventId);
        OrderResponse orderResponse = orderService.createOrder();
        return orderService.pay(orderResponse.id(), "aaa111bbb222");
    }

    public Reservation createReservation(Long eventId) {
        Reservation r = new Reservation();
        r.setStatus(ReservationStatus.PENDING);
        r.setEventId(eventId);
        r.setUserId(currentUser.currentUserId());
        r.setCreatedAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        r.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        Reservation saved = reservationRepository.save(r);
        createReservationItem(saved.getId(), tierRepository.findByEventId(eventId).getFirst().getId());
        return saved;
    }

    public Long publishedEventWithTiers(String title, String category, Long venueId, int tierCount) {
        Event e = new Event();
        e.setTitle(title);
        e.setCategory(category);
        e.setOrganizerId(currentUser.currentUserId());
        e.setVenueId(venueId);
        e.setStartsAt(Instant.now().plus(30, ChronoUnit.DAYS));
        e.setEndsAt(Instant.now().plus(31, ChronoUnit.DAYS));
        e.setSalesStartsAt(Instant.now().minus(1, ChronoUnit.DAYS));
        e.setSalesEndsAt(Instant.now().plus(10, ChronoUnit.DAYS));
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

    public Long createVenue(String city) {
        Venue v = new Venue();
        v.setCity(city);
        v.setName("Venue - " + city);
        v.setAddress("addr");
        v.setCountry("DE");
        v.setCapacity(10000);
        v.setOrganizerId(currentUser.currentUserId());
        return venueRepository.save(v).getId();
    }

    private ReservationItem createReservationItem(Long reservationId, Long tierId) {
        Optional<TicketTier> tier = tierRepository.findById(tierId);
        ReservationItem item = new ReservationItem();
        item.setReservationId(reservationId);
        item.setUnitPriceAmount(new BigDecimal(String.valueOf(tier.get().getPrice())));
        item.setTierId(tierId);
        item.setQuantity(2);
        item.setCreatedAt(Instant.now().minus(11, ChronoUnit.MINUTES));
        return reservationItemRepository.save(item);
    }



}
