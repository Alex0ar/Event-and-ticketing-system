package com.ticketflow.ticketflow.event.repository;

import com.ticketflow.ticketflow.event.domain.TicketTier;
import com.ticketflow.ticketflow.event.dto.TierStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface TicketTierRepository extends JpaRepository<TicketTier, Long> {
    List<TicketTier> findByEventId(Long eventId);
    List<TicketTier> findByEventIdIn(Collection<Long> eventIds);

    @Modifying
    @Transactional
    @Query ("""
        update TicketTier t
            set t.reservedQuantity = t.reservedQuantity + :qty
                where t.id = :id and (t.totalQuantity - t.reservedQuantity - t.soldQuantity) >= :qty
    """)
    int reserveQuantity(@Param("id") Long tierId, @Param("qty") int qty);

    @Modifying
    @Transactional
    @Query("""
        update TicketTier t
            set t.reservedQuantity = t.reservedQuantity - :qty
                where t.id = :id and t.reservedQuantity >= :qty
    """)
    int releaseQuantity(@Param("id") Long tierId, @Param("qty") int qty);

    boolean existsByReservedQuantityGreaterThan(int value);

    /** Per-tier sales figures for the organizer dashboard, aggregated in the database. */
    @Query("""
        select new com.ticketflow.ticketflow.event.dto.TierStats(
            t.id, t.name, t.price, t.priceCurrency, t.totalQuantity, t.soldQuantity, t.reservedQuantity,
            t.price * t.soldQuantity,
            (select count(k) from Ticket k
                where k.tierId = t.id and k.status = com.ticketflow.ticketflow.ticket.domain.TicketStatus.USED))
        from TicketTier t
        where t.eventId = :eventId
        order by t.price
    """)
    List<TierStats> statsForEvent(@Param("eventId") Long eventId);

    @Modifying
    @Transactional
    @Query ("""
        UPDATE TicketTier t
            SET t.reservedQuantity = t.reservedQuantity - :qty, t.soldQuantity = t.soldQuantity + :qty
                WHERE t.id = :id AND t.reservedQuantity >= :qty
    """)
    int reservedToSold(@Param("id") Long tierId, @Param("qty") int qty);

    /** Refund path: a paid seat goes back to the pool (sold -> available). */
    @Modifying
    @Transactional
    @Query("""
        UPDATE TicketTier t
            SET t.soldQuantity = t.soldQuantity - :qty
                WHERE t.id = :id AND t.soldQuantity >= :qty
    """)
    int soldToAvailable(@Param("id") Long tierId, @Param("qty") int qty);
}
