package com.ticketflow.ticketflow.config;

import com.ticketflow.ticketflow.event.domain.Event;
import com.ticketflow.ticketflow.event.domain.EventStatus;
import com.ticketflow.ticketflow.event.domain.TicketTier;
import com.ticketflow.ticketflow.event.domain.Venue;
import com.ticketflow.ticketflow.event.repository.EventRepository;
import com.ticketflow.ticketflow.event.repository.TicketTierRepository;
import com.ticketflow.ticketflow.event.repository.VenueRepository;
import com.ticketflow.ticketflow.order.domain.Order;
import com.ticketflow.ticketflow.order.domain.OrderStatus;
import com.ticketflow.ticketflow.order.repository.OrderRepository;
import com.ticketflow.ticketflow.payment.domain.Payment;
import com.ticketflow.ticketflow.payment.domain.PaymentStatus;
import com.ticketflow.ticketflow.payment.repository.PaymentRepository;
import com.ticketflow.ticketflow.reservation.domain.Reservation;
import com.ticketflow.ticketflow.reservation.domain.ReservationItem;
import com.ticketflow.ticketflow.reservation.domain.ReservationStatus;
import com.ticketflow.ticketflow.reservation.repository.ReservationItemRepository;
import com.ticketflow.ticketflow.reservation.repository.ReservationRepository;
import com.ticketflow.ticketflow.ticket.service.TicketService;
import com.ticketflow.ticketflow.user.domain.Role;
import com.ticketflow.ticketflow.user.domain.User;
import com.ticketflow.ticketflow.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Seeds a showcase dataset for the live demo: one account per role, venues, published events
 * with realistic sales history, and a draft event to publish.
 * <p>
 * Active only with the {@code demo} profile. Idempotent by default; with
 * {@code APP_DEMO_RESET_ON_STARTUP=true} it wipes all data and reseeds on every boot, so a
 * free-tier host that sleeps comes back with fresh data and event dates in the future.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {
    public static final String DEMO_PASSWORD = "demo1234";
    public static final String CUSTOMER_EMAIL = "customer@demo.ticketflow";
    public static final String ORGANIZER_EMAIL = "organizer@demo.ticketflow";
    public static final String GATE_EMAIL = "gate@demo.ticketflow";
    public static final String ADMIN_EMAIL = "admin@demo.ticketflow";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final TicketTierRepository tierRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final TicketService ticketService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final boolean resetOnStartup;

    private final Random random = new Random(42);
    private Instant now;

    public DemoDataSeeder(UserRepository userRepository, VenueRepository venueRepository,
                          EventRepository eventRepository, TicketTierRepository tierRepository,
                          ReservationRepository reservationRepository, ReservationItemRepository itemRepository,
                          OrderRepository orderRepository, PaymentRepository paymentRepository,
                          TicketService ticketService, PasswordEncoder passwordEncoder,
                          JdbcTemplate jdbc, TransactionTemplate tx,
                          @Value("${app.demo.reset-on-startup:false}") boolean resetOnStartup) {
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
        this.tierRepository = tierRepository;
        this.reservationRepository = reservationRepository;
        this.itemRepository = itemRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.ticketService = ticketService;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.tx = tx;
        this.resetOnStartup = resetOnStartup;
    }

    @Override
    public void run(String... args) {
        tx.executeWithoutResult(status -> {
            if (resetOnStartup) {
                wipeAllData();
            } else if (userRepository.findByEmail(ORGANIZER_EMAIL).isPresent()) {
                log.info("Demo data already present, skipping seed");
                return;
            }
            seed();
        });
    }

    private void wipeAllData() {
        String tables = String.join(", ", jdbc.queryForList("""
                SELECT tablename FROM pg_tables
                WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
                """, String.class));
        jdbc.execute("TRUNCATE " + tables + " RESTART IDENTITY CASCADE");
        log.info("Demo reset: all tables truncated");
    }

    private void seed() {
        now = Instant.now();

        User organizer = user(ORGANIZER_EMAIL, "Olivia Organizer", Role.ORGANIZER);
        User customer = user(CUSTOMER_EMAIL, "Casey Customer", Role.CUSTOMER);
        user(GATE_EMAIL, "Gabe Gatekeeper", Role.GATE_STAFF);
        user(ADMIN_EMAIL, "Ada Admin", Role.ADMIN);
        List<User> fans = List.of(
                user("maria@demo.ticketflow", "Maria Popescu", Role.CUSTOMER),
                user("lukas@demo.ticketflow", "Lukas Becker", Role.CUSTOMER),
                user("chloe@demo.ticketflow", "Chloé Martin", Role.CUSTOMER),
                user("james@demo.ticketflow", "James O'Neill", Role.CUSTOMER));

        Long org = organizer.getId();
        Venue arena = venue(org, "Arena Nord", "Am Nordbahnhof 1", "Berlin", "Germany", 12_000);
        Venue palace = venue(org, "Sala Palatului", "Strada Ion Câmpineanu 28", "Bucharest", "Romania", 4_000);
        Venue jazzClub = venue(org, "Le Petit Jazz", "12 Rue des Lombards", "Paris", "France", 120);
        Venue stadium = venue(org, "National Arena", "Bulevardul Basarabia 37", "Bucharest", "Romania", 55_000);

        Event synth = published(org, arena, "Neon Nights: Synthwave Live", "CONCERT", 14,
                "Retro-futurist synth acts, a laser show and a crowd dressed for 1984.");
        TicketTier synthStanding = tier(synth, "Standing", "General admission floor", "45.00", 2_000, 6);
        TicketTier synthFront = tier(synth, "Front Row", "First three rows, early entry", "95.00", 300, 4);
        TicketTier synthVip = tier(synth, "VIP Lounge", "Lounge access, drinks included", "180.00", 50, 2);

        Event jazz = published(org, jazzClub, "Jazz at Le Petit", "CONCERT", 9,
                "An intimate night with a trio that plays standards the way they were meant to be played.");
        TicketTier jazzTable = tier(jazz, "Table Seat", "Seated table service", "60.00", 40, 4);
        TicketTier jazzBar = tier(jazz, "Bar Stool", "Close to the band, closer to the bar", "35.00", 60, 2);

        Event summit = published(org, palace, "DevSummit Europe", "CONFERENCE", 30,
                "Two days of talks on distributed systems, platform engineering and developer experience.");
        TicketTier summitEarly = tier(summit, "Early Bird", "Full access, limited batch", "149.00", 500, 3);
        TicketTier summitRegular = tier(summit, "Regular", "Full access", "249.00", 1_500, 5);
        TicketTier summitWorkshop = tier(summit, "Workshop Pass", "Talks plus hands-on workshops", "399.00", 200, 2);

        Event derby = published(org, stadium, "Bucharest Derby", "SPORTS", 21,
                "The city's fiercest rivalry, under the lights.");
        TicketTier derbyUpper = tier(derby, "Upper Tier", "Upper ring seating", "25.00", 20_000, 8);
        TicketTier derbyLower = tier(derby, "Lower Tier", "Lower ring, close to the pitch", "55.00", 15_000, 6);
        TicketTier derbyPremium = tier(derby, "Premium Box", "Covered box with catering", "140.00", 2_000, 4);

        Event hamlet = published(org, palace, "Hamlet — Reimagined", "THEATRE", 45,
                "Shakespeare's tragedy staged in a near-future surveillance state.");
        TicketTier hamletBalcony = tier(hamlet, "Balcony", "Upper balcony", "30.00", 1_200, 6);
        TicketTier hamletStalls = tier(hamlet, "Stalls", "Ground floor", "65.00", 1_800, 6);

        Event standup = published(org, jazzClub, "Stand-up Night: Lost in Translation", "COMEDY", 5,
                "Four comedians, three languages, zero subtitles.");
        TicketTier standupGeneral = tier(standup, "General", "Unreserved seating", "20.00", 100, 4);

        Event festival = published(org, arena, "Summer Echoes Festival", "FESTIVAL", 60,
                "Indie, electronic and a sunrise set nobody will remember properly.");
        TicketTier festivalDay = tier(festival, "Day Pass", "One day, all stages", "70.00", 6_000, 6);
        TicketTier festivalWeekend = tier(festival, "Weekend Pass", "Both days, all stages", "150.00", 4_000, 4);

        Event gala = draft(org, arena, "Winter Lights Gala", "CONCERT", 90,
                "Orchestral winter favourites. Still a draft — log in as the organizer and publish it.");
        tier(gala, "Gala Seat", "Reserved seating", "120.00", 800, 4);

        // Sales history so the organizer dashboard has something to chart.
        sell(fans, synthStanding, 70, 4);
        sell(fans, synthFront, 25, 3);
        sell(fans, synthVip, 6, 2);
        sellExactly(fans, jazzTable, 36);          // nearly sold out: 4 table seats left
        sell(fans, jazzBar, 12, 2);
        sell(fans, summitEarly, 60, 3);
        sell(fans, summitRegular, 40, 3);
        sell(fans, summitWorkshop, 10, 2);
        sell(fans, derbyUpper, 80, 6);
        sell(fans, derbyLower, 50, 4);
        sell(fans, derbyPremium, 8, 2);
        sell(fans, hamletBalcony, 20, 4);
        sell(fans, hamletStalls, 25, 4);
        sell(fans, standupGeneral, 18, 3);
        sell(fans, festivalDay, 30, 4);
        sell(fans, festivalWeekend, 22, 3);

        // The demo customer already owns tickets, so "My tickets" and the gate scanner work immediately.
        paidOrder(customer, synthFront, 2, now.minus(2, ChronoUnit.DAYS));
        paidOrder(customer, summitEarly, 1, now.minus(5, ChronoUnit.DAYS));

        log.info("Demo data seeded: accounts {}, {}, {}, {} (password '{}')",
                CUSTOMER_EMAIL, ORGANIZER_EMAIL, GATE_EMAIL, ADMIN_EMAIL, DEMO_PASSWORD);
    }

    // --- builders ---

    private User user(String email, String name, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        u.setRoles(Set.of(role));
        return userRepository.save(u);
    }

    private Venue venue(Long organizerId, String name, String address, String city, String country, int capacity) {
        Venue v = new Venue();
        v.setOrganizerId(organizerId);
        v.setName(name);
        v.setAddress(address);
        v.setCity(city);
        v.setCountry(country);
        v.setCapacity(capacity);
        return venueRepository.save(v);
    }

    private Event published(Long organizerId, Venue venue, String title, String category, int daysAhead, String description) {
        return event(organizerId, venue, title, category, daysAhead, description, EventStatus.PUBLISHED);
    }

    private Event draft(Long organizerId, Venue venue, String title, String category, int daysAhead, String description) {
        return event(organizerId, venue, title, category, daysAhead, description, EventStatus.DRAFT);
    }

    private Event event(Long organizerId, Venue venue, String title, String category, int daysAhead,
                        String description, EventStatus status) {
        Instant startsAt = now.truncatedTo(ChronoUnit.DAYS).plus(daysAhead, ChronoUnit.DAYS).plus(19, ChronoUnit.HOURS);
        Event e = new Event();
        e.setOrganizerId(organizerId);
        e.setVenueId(venue.getId());
        e.setTitle(title);
        e.setDescription(description);
        e.setCategory(category);
        e.setStartsAt(startsAt);
        e.setEndsAt(startsAt.plus(Duration.ofHours(4)));
        e.setSalesStartsAt(now.minus(20, ChronoUnit.DAYS));
        e.setSalesEndsAt(startsAt.minus(Duration.ofHours(1)));
        e.setStatus(status);
        return eventRepository.save(e);
    }

    private TicketTier tier(Event event, String name, String description, String price, int total, int maxPerOrder) {
        TicketTier t = new TicketTier();
        t.setEventId(event.getId());
        t.setName(name);
        t.setDescription(description);
        t.setPrice(new BigDecimal(price));
        t.setPriceCurrency("EUR");
        t.setTotalQuantity(total);
        t.setMaxPerOrder(maxPerOrder);
        return tierRepository.saveAndFlush(t);
    }

    /** Spreads {@code orders} purchases of 1..maxQty tickets over the last two weeks. */
    private void sell(List<User> buyers, TicketTier tier, int orders, int maxQty) {
        for (int i = 0; i < orders; i++) {
            int qty = 1 + random.nextInt(Math.max(1, Math.min(maxQty, tier.getMaxPerOrder())));
            Instant when = now.minus(random.nextInt(14 * 24), ChronoUnit.HOURS);
            paidOrder(buyers.get(random.nextInt(buyers.size())), tier, qty, when);
        }
    }

    private void sellExactly(List<User> buyers, TicketTier tier, int tickets) {
        int remaining = tickets;
        while (remaining > 0) {
            int qty = Math.min(remaining, tier.getMaxPerOrder());
            paidOrder(buyers.get(random.nextInt(buyers.size())), tier, qty,
                    now.minus(random.nextInt(10 * 24), ChronoUnit.HOURS));
            remaining -= qty;
        }
    }

    /** Runs the same state transitions as checkout: hold -> pay -> reserved-to-sold -> tickets. */
    private void paidOrder(User buyer, TicketTier tier, int qty, Instant when) {
        if (tierRepository.reserveQuantity(tier.getId(), qty) == 0) {
            return; // tier full — skip rather than oversell
        }
        tierRepository.reservedToSold(tier.getId(), qty);

        Reservation r = new Reservation();
        r.setUserId(buyer.getId());
        r.setEventId(tier.getEventId());
        r.setStatus(ReservationStatus.CONFIRMED);
        r.setExpiresAt(when.plus(Duration.ofMinutes(10)));
        r = reservationRepository.save(r);

        ReservationItem item = new ReservationItem();
        item.setReservationId(r.getId());
        item.setTierId(tier.getId());
        item.setQuantity(qty);
        item.setUnitPriceAmount(tier.getPrice());
        itemRepository.save(item);

        BigDecimal total = tier.getPrice().multiply(BigDecimal.valueOf(qty));
        Order o = new Order();
        o.setUserId(buyer.getId());
        o.setReservationId(r.getId());
        o.setTotal(total);
        o.setCurrency(tier.getPriceCurrency());
        o.setStatus(OrderStatus.PAID);
        o = orderRepository.save(o);

        Payment p = new Payment();
        p.setOrderId(o.getId());
        p.setUserId(buyer.getId());
        p.setAmount(total);
        p.setCurrency(tier.getPriceCurrency());
        p.setIdempotencyKey("seed-" + UUID.randomUUID());
        p.setStatus(PaymentStatus.SUCCESSED);
        paymentRepository.save(p);

        ticketService.createAndSaveTicket(o.getId(), r.getId(), buyer.getId());

        // Auditing stamps "now"; backdate so the sales chart shows a history.
        reservationRepository.flush();
        Timestamp ts = Timestamp.from(when);
        jdbc.update("UPDATE reservations SET created_at = ? WHERE id = ?", ts, r.getId());
        jdbc.update("UPDATE orders SET created_at = ? WHERE id = ?", ts, o.getId());
        jdbc.update("UPDATE payments SET created_at = ? WHERE order_id = ?", ts, o.getId());
        jdbc.update("UPDATE tickets SET created_at = ? WHERE order_id = ?", ts, o.getId());
    }
}
