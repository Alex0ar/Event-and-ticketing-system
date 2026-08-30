package com.ticketflow.ticketflow.ticket.domain;

import com.ticketflow.ticketflow.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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
    private TicketStatus status;
}
