package com.ticketflow.ticketflow.order.api;

import com.ticketflow.ticketflow.order.dto.OrderResponse;
import com.ticketflow.ticketflow.order.service.OrderService;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {
    private final OrderService orderService;
    private final CurrentUserProvider currentUser;

    public OrderController(OrderService orderService, CurrentUserProvider currentUser) {
        this.orderService = orderService;
        this.currentUser = currentUser;
    }

    @PostMapping("/initiate")
    public ResponseEntity<OrderResponse> initiateOrder() {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder());
    }

    @GetMapping("/listOwnedOrders")
    public List<OrderResponse> listOwnedOrders() {
        return orderService.listOwnedOrders();
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<OrderResponse> pay(@PathVariable Long id, @RequestHeader("Idempotency-key") String idempotencyKey) {
        return ResponseEntity.ok(orderService.pay(id, idempotencyKey));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return ResponseEntity.ok().build();
    }
}
