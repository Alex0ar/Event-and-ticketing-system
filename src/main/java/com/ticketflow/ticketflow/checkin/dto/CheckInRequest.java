package com.ticketflow.ticketflow.checkin.dto;

import jakarta.validation.constraints.NotBlank;

/** The ticket code decoded from the QR by the scanner (or typed by gate staff). */
public record CheckInRequest(@NotBlank String ticketCode) {
}
