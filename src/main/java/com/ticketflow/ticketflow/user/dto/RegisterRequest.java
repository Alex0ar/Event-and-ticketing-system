package com.ticketflow.ticketflow.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email
        String email,

        @NotBlank
        @Size(min = 8, message = "password must be at least 8 characters")
        @Pattern(regexp = ".*[A-Za-z].*", message = "password must contain a letter")
        @Pattern(regexp = ".*\\d.*", message = "password must contain a digit")
        String password,

        @NotBlank
        String fullName
) {
}
