package com.ticketflow.ticketflow.user.dto;

import com.ticketflow.ticketflow.user.domain.Role;

import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Set<Role> roles
) {
}
