package com.ticketflow.ticketflow.ticket.repository;

import com.ticketflow.ticketflow.ticket.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByOwnerUserId(Long userId);
    Optional<Ticket> findByUuidCode(String code);

    @Modifying
    @Query("""
        update Ticket t
            set t.status = 'USED', t.checkedInAt = :now, t.checkedInBy = :staffId
                where t.uuidCode = :payload and t.status = 'VALID'
    """)
    int validateTicket(@Param("now") Instant now, @Param("staffId") Long staffId, @Param("payload") String payload);
}
