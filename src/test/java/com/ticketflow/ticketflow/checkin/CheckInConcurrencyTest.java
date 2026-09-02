package com.ticketflow.ticketflow.checkin;

import com.ticketflow.ticketflow.IntegrationTest;
import com.ticketflow.ticketflow.common.error.NotFoundException;
import com.ticketflow.ticketflow.fixtures.TicketCreationFixtures;
import com.ticketflow.ticketflow.order.dto.OrderResponse;
import com.ticketflow.ticketflow.ticket.domain.Ticket;
import com.ticketflow.ticketflow.ticket.domain.TicketStatus;
import com.ticketflow.ticketflow.ticket.repository.TicketRepository;
import com.ticketflow.ticketflow.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CheckInConcurrencyTest extends IntegrationTest {
    @Autowired TicketCreationFixtures ticketCreationFixtures;
    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void check_in_concurrency_test() throws InterruptedException {
        User organizer = ticketCreationFixtures.createOrganizer();
        User customer = ticketCreationFixtures.createCustomer();
        User gateStaff = ticketCreationFixtures.createGateStaff();
        int requestsNumber = 200;
        var startGate = new CountDownLatch(1);
        var doneGate = new CountDownLatch(requestsNumber);
        var successes = new AtomicInteger(0);
        var failures = new ConcurrentLinkedQueue<Throwable>();

        Long eventId = runsAs(organizer, () -> {
            Long venueId = ticketCreationFixtures.createVenue("Berlin");
            return ticketCreationFixtures.publishedEventWithTiers("Rock in Bucharest", "Bucharest", venueId, 1);
        });

        Ticket ticket = runsAs(customer, () -> {
            OrderResponse orderResponse = ticketCreationFixtures.createRezervationAndPayOrder(eventId);
            return ticketRepository.findByOrderId(orderResponse.id()).get(0);
        });

        ExecutorService pool = Executors.newFixedThreadPool(32);
        for (int i = 0; i < requestsNumber; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    runsAs(gateStaff, () -> {
                        int success = ticketRepository.ticketCheckIn(Instant.now(), gateStaff.getId(), ticket.getUuidCode());
                        if (success == 1) {
                            successes.incrementAndGet();
                        }
                        return null;
                    });
                } catch (Throwable ex) {
                    failures.add(ex);
                } finally {
                    doneGate.countDown();
                }
            });
        }
        startGate.countDown();
        doneGate.await();

        for (int i = 0; i < failures.size(); i++) {
            System.out.println(failures.peek().getMessage());
        }

        assertThat(successes.get()).isEqualTo(1);
        Ticket after = ticketRepository.findById(ticket.getId()).orElseThrow(() -> new NotFoundException("Ticket not found"));
        assertThat(after.getCheckedInAt()).isNotNull();
        assertThat(after.getCheckedInBy()).isEqualTo(gateStaff.getId());
        assertThat(after.getStatus()).isEqualTo(TicketStatus.USED);
    }
}
