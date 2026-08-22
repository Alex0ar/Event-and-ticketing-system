package com.ticketflow.ticketflow.user.dto;

public record TokenResponse(
        String AccessToken,
        String tokenType
) {
}
