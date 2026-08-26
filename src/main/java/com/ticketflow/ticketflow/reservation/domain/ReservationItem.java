package com.ticketflow.ticketflow.reservation.domain;

import com.ticketflow.ticketflow.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "reservation_items")
public class ReservationItem extends BaseEntity {
    @Column(nullable = false)
    private Long reservationId;

    @Column(nullable = false)
    private Long tierId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceAmount;
}
