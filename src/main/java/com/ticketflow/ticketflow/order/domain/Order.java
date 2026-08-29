package com.ticketflow.ticketflow.order.domain;

import com.ticketflow.ticketflow.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.engine.jdbc.Size;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    @Column(nullable = false)
    Long userId;
    @Column(nullable = false)
    Long reservationId;
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal total;
    @Column(nullable = false, length = 3)
    String currency;
    @Column(nullable = false)
    OrderStatus status = OrderStatus.PENDING_PAYMENT;
}
