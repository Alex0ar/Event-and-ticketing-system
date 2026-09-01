package com.ticketflow.ticketflow.checkin.api;

import com.ticketflow.ticketflow.checkin.service.CheckInService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkIn")
public class CheckInController {
    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping("/{qr}")
    public ResponseEntity<String> checkIn(@PathVariable byte[] qr) {
        
    }
}
