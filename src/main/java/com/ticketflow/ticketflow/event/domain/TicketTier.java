package com.ticketflow.ticketflow.event.domain;

import com.ticketflow.ticketflow.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "ticket_tiers")
public class TicketTier extends BaseEntity {
    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String priceCurrency;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer reservedQuantity = 0;

    @Column(nullable = false)
    private Integer soldQuantity = 0;

    @Column(nullable = false)
    private Integer maxPerOrder;

    @Version
    private Long version;
}
