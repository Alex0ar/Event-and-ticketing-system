package com.ticketflow.ticketflow.checkin.dto;

import java.time.Instant;

public record CheckInResponse(
        String ticketCode,
        String holderName,
        String eventTitle,
        String tierName,
        Instant checkedInAt
) {
}
