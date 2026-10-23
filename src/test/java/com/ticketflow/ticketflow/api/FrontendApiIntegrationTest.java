package com.ticketflow.ticketflow.api;

import com.ticketflow.ticketflow.IntegrationTest;
import com.ticketflow.ticketflow.event.domain.EventStatus;
import com.ticketflow.ticketflow.event.repository.EventRepository;
import com.ticketflow.ticketflow.event.repository.TicketTierRepository;
import com.ticketflow.ticketflow.fixtures.TicketCreationFixtures;
import com.ticketflow.ticketflow.order.dto.OrderResponse;
import com.ticketflow.ticketflow.ticket.domain.Ticket;
import com.ticketflow.ticketflow.ticket.repository.TicketRepository;
import com.ticketflow.ticketflow.user.domain.Role;
import com.ticketflow.ticketflow.user.domain.User;
import com.ticketflow.ticketflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Covers the endpoints and behaviours the web frontend depends on. */
@AutoConfigureMockMvc
class FrontendApiIntegrationTest extends IntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired TicketCreationFixtures fixtures;
    @Autowired UserRepository userRepository;
    @Autowired EventRepository eventRepository;
    @Autowired TicketTierRepository tierRepository;
    @Autowired TicketRepository ticketRepository;

    User organizer;
    User customer;
    User gate;
    Long eventId;

    @BeforeEach
    void setUp() {
        organizer = fixtures.createOrganizer();
        customer = fixtures.createCustomer();
        gate = fixtures.createGateStaff();
        eventId = runsAs(organizer, () -> fixtures.publishedEventWithTiers("Rock Night", "CONCERT",
                fixtures.createVenue("Berlin"), 1));
    }

    private RequestPostProcessor as(User u) {
        return user(u.getEmail()).roles(u.getRoles().stream().map(Role::name).toArray(String[]::new));
    }

    private Ticket paidTicket() {
        OrderResponse order = runsAs(customer, () -> fixtures.createRezervationAndPayOrder(eventId));
        return ticketRepository.findByOrderId(order.id()).getFirst();
    }

    private Long tierId() {
        return tierRepository.findByEventId(eventId).getFirst().getId();
    }

    // --- public catalog ---

    @Test
    void publicEventDetail_isAnonymous_andIncludesVenue() throws Exception {
        mockMvc.perform(get("/api/v1/events/{id}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.venue.city").value("Berlin"))
                .andExpect(jsonPath("$.tiers[0].available").value(10));
    }

    @Test
    void draftEvent_isHiddenFromPublic() throws Exception {
        var event = eventRepository.findById(eventId).orElseThrow();
        event.setStatus(EventStatus.DRAFT);
        eventRepository.save(event);

        mockMvc.perform(get("/api/v1/events/{id}", eventId)).andExpect(status().isNotFound());
    }

    // --- checkout ---

    @Test
    void currentReservation_is204WhenNone_and200AfterReserving() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/current").with(as(customer)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/reservations").with(as(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":%d,\"items\":[{\"tierId\":%d,\"quantity\":2}]}".formatted(eventId, tierId())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/reservations/current").with(as(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void declinedPayment_isIdempotent_andANewKeyCanSucceed() throws Exception {
        mockMvc.perform(post("/api/v1/reservations").with(as(customer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventId\":%d,\"items\":[{\"tierId\":%d,\"quantity\":3}]}".formatted(eventId, tierId())));
        String orderJson = mockMvc.perform(post("/api/v1/orders/initiate").with(as(customer)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long orderId = Long.parseLong(orderJson.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(post("/api/v1/orders/{id}/pay", orderId).with(as(customer))
                        .header("Idempotency-Key", "attempt-1").header("X-Mock-Payment-Outcome", "DECLINE"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.errorCode").value("PAYMENT_DECLINED"));
        // Same key, no decline header: still the original outcome.
        mockMvc.perform(post("/api/v1/orders/{id}/pay", orderId).with(as(customer))
                        .header("Idempotency-Key", "attempt-1"))
                .andExpect(status().isPaymentRequired());

        mockMvc.perform(post("/api/v1/orders/{id}/pay", orderId).with(as(customer))
                        .header("Idempotency-Key", "attempt-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.eventTitle").value("Rock Night"));
        mockMvc.perform(post("/api/v1/orders/{id}/pay", orderId).with(as(customer))
                        .header("Idempotency-Key", "attempt-2"))
                .andExpect(status().isOk());

        assertThat(ticketRepository.findByOrderId(orderId)).hasSize(3);
        assertThat(tierRepository.findById(tierId()).orElseThrow().getSoldQuantity()).isEqualTo(3);
    }

    @Test
    void customerRefund_returnsSeatsToThePool() throws Exception {
        Ticket ticket = paidTicket();
        assertThat(tierRepository.findById(tierId()).orElseThrow().getSoldQuantity()).isEqualTo(2);

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", ticket.getOrderId()).with(as(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        assertThat(tierRepository.findById(tierId()).orElseThrow().getSoldQuantity()).isZero();
    }

    // --- tickets & gate ---

    @Test
    void ticketQr_isOnlyForTheOwner() throws Exception {
        Ticket ticket = paidTicket();
        User other = new User();
        other.setEmail("other@test.com");
        other.setPasswordHash("x");
        other.setFullName("Other");
        other.setRoles(Set.of(Role.CUSTOMER));
        userRepository.save(other);

        mockMvc.perform(get("/api/v1/tickets/{code}/qr", ticket.getUuidCode()).with(as(customer)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
        mockMvc.perform(get("/api/v1/tickets/{code}/qr", ticket.getUuidCode()).with(as(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkIn_succeedsOnce_thenReportsAlreadyUsed() throws Exception {
        Ticket ticket = paidTicket();
        String body = "{\"ticketCode\":\"%s\"}".formatted(ticket.getUuidCode());

        mockMvc.perform(post("/api/v1/checkin").with(as(gate)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holderName").value("Customer"))
                .andExpect(jsonPath("$.eventTitle").value("Rock Night"));
        mockMvc.perform(post("/api/v1/checkin").with(as(gate)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TICKET_ALREADY_USED"));
    }

    @Test
    void checkIn_rejectsNonGateStaff_andUnknownCodes() throws Exception {
        Ticket ticket = paidTicket();

        mockMvc.perform(post("/api/v1/checkin").with(as(customer)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketCode\":\"%s\"}".formatted(ticket.getUuidCode())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").exists());
        mockMvc.perform(post("/api/v1/checkin").with(as(gate)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketCode\":\"no-such-ticket\"}"))
                .andExpect(status().isNotFound());
    }

    // --- organizer ---

    @Test
    void stats_areComputedForTheOwner_andForbiddenForOthers() throws Exception {
        paidTicket();
        User rival = new User();
        rival.setEmail("rival@test.com");
        rival.setPasswordHash("x");
        rival.setFullName("Rival");
        rival.setRoles(Set.of(Role.ORGANIZER));
        userRepository.save(rival);

        mockMvc.perform(get("/api/v1/organizer/events/{id}/stats", eventId).with(as(organizer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sold").value(2))
                .andExpect(jsonPath("$.revenue").value(100.0))
                .andExpect(jsonPath("$.tiers[0].sold").value(2))
                .andExpect(jsonPath("$.dailySales[0].tickets").value(2));
        mockMvc.perform(get("/api/v1/organizer/events/{id}/stats", eventId).with(as(rival)))
                .andExpect(status().isForbidden());
    }

    // --- cross-cutting ---

    @Test
    void unauthenticatedRequests_getJsonApiError() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me"));
    }

    @Test
    void corsPreflight_allowsTheConfiguredOrigin_only() throws Exception {
        mockMvc.perform(options("/api/v1/orders/1/pay")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,idempotency-key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mockMvc.perform(options("/api/v1/orders/1/pay")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
