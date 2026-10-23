package com.ticketflow.ticketflow.order.service;

import com.ticketflow.ticketflow.auditlog.domain.AuditLogAction;
import com.ticketflow.ticketflow.auditlog.domain.AuditReason;
import com.ticketflow.ticketflow.auditlog.service.AuditLogService;
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
import com.ticketflow.ticketflow.payment.dto.PaymentResponse;
import com.ticketflow.ticketflow.payment.repository.PaymentRepository;
import com.ticketflow.ticketflow.payment.service.PaymentGateaway;
import com.ticketflow.ticketflow.reservation.domain.Reservation;
import com.ticketflow.ticketflow.reservation.domain.ReservationStatus;
import com.ticketflow.ticketflow.reservation.repository.ReservationRepository;
import com.ticketflow.ticketflow.reservation.service.ReservationService;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import com.ticketflow.ticketflow.ticket.service.TicketService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final AuditLogService auditLogService;

    public OrderService(CurrentUserProvider currentUser, ReservationRepository reservationRepository,
                        ReservationService reservationService, OrderRepository orderRepository,
                        PaymentRepository paymentRepository, PaymentGateaway paymentGateaway,
                        TicketService ticketService, EventRepository eventRepository,
                        AuditLogService auditLogService) {
        this.currentUser = currentUser;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateaway = paymentGateaway;
        this.ticketService = ticketService;
        this.eventRepository = eventRepository;
        this.auditLogService = auditLogService;
    }

    /** Creates the order for the caller's active hold, or returns it if it already exists (one order per reservation). */
    @Transactional
    public OrderResponse createOrder() {
        Long userId = currentUser.currentUserId();
        Reservation r = reservationRepository.findByUserIdAndStatus(userId, ReservationStatus.PENDING);
        if (r == null) {
            throw new NotFoundException("You have no active reservation");
        }
        if (r.getExpiresAt().isBefore(Instant.now())) {
            throw new ApplicationException(HttpStatus.CONFLICT, "RESERVATION_EXPIRED", "Your reservation has expired");
        }
        Order existing = orderRepository.findByReservationId(r.getId()).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }
        Order o = new Order();
        o.setUserId(userId);
        o.setReservationId(r.getId());
        o.setTotal(reservationService.getReservationTotalCost(r));
        o.setCurrency("EUR");
        return toResponse(orderRepository.save(o));
    }

    /**
     * Charges the order. Idempotent per Idempotency-Key: a repeated key returns the first outcome
     * instead of charging again. A decline keeps its FAILED payment row (no rollback) so a retry
     * with the same key sees the same decline; a new key may try again while the hold lasts.
     */
    @Transactional(noRollbackFor = PaymentDeclinedException.class)
    public OrderResponse pay(Long orderId, String idempotencyKey) {
        Long userId = currentUser.currentUserId();
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!o.getUserId().equals(userId)) {
            throw new ForbidenException("This order doesn't belong to you");
        }
        var previous = paymentRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (previous.isPresent()) {
            return handleExistingPayment(previous.get());
        }
        if (o.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ApplicationException(HttpStatus.CONFLICT, "ORDER_NOT_PAYABLE", "Order is already " + o.getStatus());
        }
        Reservation r = reservationRepository.findById(o.getReservationId())
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        if (r.getStatus() != ReservationStatus.PENDING || r.getExpiresAt().isBefore(Instant.now())) {
            throw new ApplicationException(HttpStatus.CONFLICT, "RESERVATION_EXPIRED", "Your reservation has expired");
        }

        Payment savedPayment;
        try {
            savedPayment = paymentRepository.saveAndFlush(paymentGateaway.initiatePayment(o, idempotencyKey));
        } catch (DataIntegrityViolationException e) {
            // A concurrent request with the same key won the insert.
            throw new PaymentInProgres("Payment is already in progress");
        }
        PaymentResponse charge = paymentGateaway.charge(savedPayment.getId());
        if (charge.status() != PaymentStatus.SUCCESSED) {
            throw new PaymentDeclinedException("Payment was declined");
        }

        reservationService.confirmReservation(o.getReservationId());
        o.setStatus(OrderStatus.PAID);
        Order saved = orderRepository.save(o);
        ticketService.createAndSaveTicket(o.getId(), o.getReservationId(), o.getUserId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOwnedOrders() {
        List<Order> orders = orderRepository.findAllByUserIdOrderByCreatedAtDesc(currentUser.currentUserId());
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, Reservation> reservations = reservationRepository
                .findAllById(orders.stream().map(Order::getReservationId).toList()).stream()
                .collect(Collectors.toMap(Reservation::getId, Function.identity()));
        Map<Long, Event> events = eventRepository
                .findAllById(reservations.values().stream().map(Reservation::getEventId).distinct().toList()).stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));
        return orders.stream().map(o -> {
            Reservation r = reservations.get(o.getReservationId());
            return toResponse(o, r == null ? null : events.get(r.getEventId()));
        }).toList();
    }

    /** Customer-initiated cancellation of a paid order, allowed until 24h before the event. */
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!o.getUserId().equals(currentUser.currentUserId())) {
            throw new ForbidenException("This order doesn't belong to you");
        }
        if (o.getStatus() != OrderStatus.PAID) {
            throw new ConflictException("Only paid orders can be cancelled");
        }
        Event event = eventFor(o);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new ConflictException("The event has been cancelled");
        }
        if (Instant.now().isAfter(event.getStartsAt().minus(24, ChronoUnit.HOURS))) {
            throw new ConflictException("Orders can only be cancelled until 24 hours before the event");
        }
        reservationService.refundReservation(o.getReservationId());
        ticketService.refundTickets(orderId);
        return refund(o, AuditReason.CUSTOMER_REQUEST);
    }

    /** Organizer cancelled the event: refund regardless of owner or timing, and void the tickets. */
    @Transactional
    public void refundForCancelledEvent(Order o) {
        reservationService.refundReservation(o.getReservationId());
        ticketService.voidTickets(o.getId());
        refund(o, AuditReason.EVENT_CANCELLED);
    }

    // --- helpers ---

    private OrderResponse refund(Order o, AuditReason reason) {
        paymentGateaway.refund(o.getId());
        o.setStatus(OrderStatus.REFUNDED);
        auditLogService.record(AuditLogAction.ORDER_REFUNDED, "order", o.getId(),
                Map.of("reason", reason, "total_price", o.getTotal(), "currency", o.getCurrency()));
        return toResponse(orderRepository.save(o));
    }

    private Event eventFor(Order o) {
        Reservation r = reservationRepository.findById(o.getReservationId())
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        return eventRepository.findById(r.getEventId())
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    private OrderResponse handleExistingPayment(Payment existingPayment) {
        return switch (existingPayment.getStatus()) {
            case FAILED -> throw new PaymentDeclinedException("Payment was declined");
            case INITIATED -> throw new PaymentInProgres("Payment is already in progress");
            case REFUNDED -> throw new FailedPaymentException("Payment was refunded");
            case SUCCESSED -> toResponse(orderRepository.findById(existingPayment.getOrderId())
                    .orElseThrow(() -> new NotFoundException("Order not found")));
        };
    }

    private OrderResponse toResponse(Order order) {
        return toResponse(order, eventFor(order));
    }

    private OrderResponse toResponse(Order order, Event event) {
        return new OrderResponse(order.getId(), order.getUserId(), order.getReservationId(), order.getStatus(),
                order.getTotal(), order.getCurrency(),
                event == null ? null : event.getId(), event == null ? null : event.getTitle(),
                event == null ? null : event.getStartsAt(), order.getCreatedAt());
    }
}
