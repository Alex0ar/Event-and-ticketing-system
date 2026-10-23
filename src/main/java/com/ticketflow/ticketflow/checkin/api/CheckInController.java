package com.ticketflow.ticketflow.checkin.api;

import com.ticketflow.ticketflow.checkin.dto.CheckInRequest;
import com.ticketflow.ticketflow.checkin.dto.CheckInResponse;
import com.ticketflow.ticketflow.checkin.service.CheckInService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkin")
@PreAuthorize("hasRole('GATE_STAFF')")
public class CheckInController {
    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping
    public CheckInResponse checkIn(@Valid @RequestBody CheckInRequest request) {
        return checkInService.checkIn(request.ticketCode().trim());
    }
}
