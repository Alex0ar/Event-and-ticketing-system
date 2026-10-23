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

    /** 200 with the caller's active hold, or 204 if there is none. */
    @GetMapping("/current")
    public ResponseEntity<ReservationResponse> current() {
        return reservationService.getCurrent()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> release(@PathVariable Long id) {
        reservationService.release(id);
        return ResponseEntity.noContent().build();
    }
}
