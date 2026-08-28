package com.ticketflow.ticketflow.reservation.api;

import com.ticketflow.ticketflow.reservation.dto.ReservationRequest;
import com.ticketflow.ticketflow.reservation.dto.ReservationResponse;
import com.ticketflow.ticketflow.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
@PreAuthorize("hasRole('CUSTOMER')")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.reserve(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> release(@PathVariable Long id) {
        reservationService.release(id);
        return ResponseEntity.noContent().build();
    }
}
