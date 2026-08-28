package com.ticketflow.ticketflow.event.repository;

import com.ticketflow.ticketflow.event.domain.TicketTier;
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
}
