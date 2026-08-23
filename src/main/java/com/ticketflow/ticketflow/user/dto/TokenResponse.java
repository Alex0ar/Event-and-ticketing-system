package com.ticketflow.ticketflow.user.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
