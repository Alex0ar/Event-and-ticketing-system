package com.ticketflow.ticketflow.api;

import com.ticketflow.ticketflow.IntegrationTest;
import com.ticketflow.ticketflow.config.DemoDataSeeder;
import com.ticketflow.ticketflow.event.repository.TicketTierRepository;
import com.ticketflow.ticketflow.ticket.repository.TicketRepository;
import com.ticketflow.ticketflow.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("demo")
@AutoConfigureMockMvc
class DemoDataSeederTest extends IntegrationTest {
    @Autowired DemoDataSeeder seeder;
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired TicketTierRepository tierRepository;
    @Autowired TicketRepository ticketRepository;

    @Test
    void seedsAUsableDemo_andIsIdempotent() throws Exception {
        seeder.run(); // the base class wiped whatever ran at startup

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}"
                                .formatted(DemoDataSeeder.CUSTOMER_EMAIL, DemoDataSeeder.DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
        mockMvc.perform(get("/api/v1/events").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(7)); // the draft gala stays hidden

        // Seeded sales never break the inventory invariant.
        tierRepository.findAll().forEach(t ->
                assertThat(t.getReservedQuantity() + t.getSoldQuantity()).isLessThanOrEqualTo(t.getTotalQuantity()));
        long sold = tierRepository.findAll().stream().mapToLong(t -> t.getSoldQuantity()).sum();
        assertThat(ticketRepository.count()).isEqualTo(sold);

        long users = userRepository.count();
        seeder.run();
        assertThat(userRepository.count()).isEqualTo(users);
    }
}
