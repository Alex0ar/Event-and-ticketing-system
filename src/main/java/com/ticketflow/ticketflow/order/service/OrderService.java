package com.ticketflow.ticketflow.order.service;

import com.ticketflow.ticketflow.common.error.*;
import com.ticketflow.ticketflow.event.domain.Event;
import com.ticketflow.ticketflow.event.domain.EventStatus;
import com.ticketflow.ticketflow.event.repository.EventRepository;
import com.ticketflow.ticketflow.order.domain.Order;
import com.ticketflow.ticketflow.order.domain.OrderStatus;
import com.ticketflow.ticketflow.order.dto.OrderResponse;
import com.ticketflow.ticketflow.order.repository.OrderRepository;
import com.ticketflow.ticketflow.payment.domain.Payment;
import com.ticketflow.ticketflow.payment.domain.PaymentStatus;
import com.ticketflow.ticketflow.payment.repository.PaymentRepository;
import com.ticketflow.ticketflow.payment.service.PaymentGateaway;
import com.ticketflow.ticketflow.reservation.domain.Reservation;
import com.ticketflow.ticketflow.reservation.domain.ReservationStatus;
import com.ticketflow.ticketflow.reservation.repository.ReservationRepository;
import com.ticketflow.ticketflow.reservation.service.ReservationService;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import com.ticketflow.ticketflow.ticket.domain.Ticket;
import com.ticketflow.ticketflow.ticket.repository.TicketRepository;
import com.ticketflow.ticketflow.ticket.service.TicketService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class OrderService {
    private final CurrentUserProvider currentUser;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateaway paymentGateaway;
    private final TicketService ticketService;
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;

    public OrderService(CurrentUserProvider currentUser, ReservationRepository reservationRepository, ReservationService reservationService, OrderRepository orderRepository, PaymentRepository paymentRepository, PaymentGateaway paymentGateaway, TicketService ticketService, EventRepository eventRepository, TicketRepository ticketRepository) {
        this.currentUser = currentUser;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateaway = paymentGateaway;
        this.ticketService = ticketService;
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
    }

    public OrderResponse createOrder () {
        Order o = new Order();
        Reservation r = reservationRepository.findByUserIdAndStatus(currentUser.currentUserId(), ReservationStatus.PENDING);
        o.setUserId(currentUser.currentUserId());
        o.setReservationId(r.getId());
        o.setTotal(reservationService.getReservationTotalCost(r));
        o.setCurrency("EUR");
        return toResponse(orderRepository.save(o));
    }

    public OrderResponse pay(Long orderId, String idempotencyKey) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!o.getUserId().equals(currentUser.currentUserId())) {
            throw new ConflictException("This order doesn't belong to you");
        }
        Payment p = paymentGateaway.initiatePayment(o, idempotencyKey);
        Payment savedPayment;
        try {
            savedPayment = paymentRepository.saveAndFlush(p);
        } catch (DataIntegrityViolationException e) {
            Payment existing = paymentRepository.findByUserIdAndIdempotencyKey(currentUser.currentUserId(), idempotencyKey)
                    .orElseThrow(() -> e);
            return handleExistingPayment(existing);
        }
        o.setStatus(OrderStatus.PAID);
        reservationService.confirmReservation(o.getReservationId());
        paymentGateaway.charge(savedPayment.getId());
        OrderResponse savedOrder = toResponse(orderRepository.save(o));
        ticketService.createAndSaveTicket(o.getId(), o.getReservationId());
        return savedOrder;
    }

    public List<OrderResponse> listOwnedOrders() {
        return orderRepository.findAllByUserId(currentUser.currentUserId());
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        Ticket ticket = ticketRepository.getById(o.getId());
        Event event = eventRepository.findById(ticket.getEventId())
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new ConflictException("The event has been cancelled");
        }
        Instant eventStartsAt = event.getStartsAt();
        if (Instant.now().isAfter(eventStartsAt.minus(24, ChronoUnit.HOURS))) {
            throw new ConflictException("Can't cancel this order because event is in less the 24 hours");
        }
        if (!o.getUserId().equals(currentUser.currentUserId())) {
            throw new ForbidenException("This order doesn't belong to you");
        }
        if (!o.getStatus().equals(OrderStatus.PAID)) {
            throw new ConflictException("Order was not paid to be cancelled");
        }
        reservationService.cancelReservation(o.getReservationId());
        ticketService.cancelTickets(orderId);
        paymentGateaway.refund(orderId);
        o.setStatus(OrderStatus.REFUNDED);
        return toResponse(orderRepository.save(o));
    }

    // --- helper ---

    private OrderResponse handleExistingPayment(Payment existingPayment) {
        return switch (existingPayment.getStatus()) {
            case FAILED -> throw new FailedPaymentException("Payment failed");
            case INITIATED -> throw new PaymentInProgres("Payment is already initiated");
            case REFUNDED -> throw new FailedPaymentException("Payment is already refunded");
            case SUCCESSED -> {
                Order order = orderRepository.findById(existingPayment.getOrderId())
                        .orElseThrow(() -> new NotFoundException("Order not found"));
                yield toResponse(order);
            }
        };
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(order.getId(), order.getUserId(), order.getReservationId(), order.getStatus(), order.getTotal(), order.getCurrency());
    }


}
