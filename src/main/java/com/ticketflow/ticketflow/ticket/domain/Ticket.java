package com.ticketflow.ticketflow.ticket.domain;

import com.ticketflow.ticketflow.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "tickets")
public class Ticket extends BaseEntity {
    @Column(nullable = false)
    private Long orderId;
    @Column(nullable = false)
    private Long tierId;
    @Column(nullable = false)
    private Long eventId;
    @Column(nullable = false)
    private Long ownerUserId;
    @Column(nullable = false)
    private String uuidCode;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TicketStatus status;
    private Instant checkedInAt;
    private Long checkedInBy;
}
