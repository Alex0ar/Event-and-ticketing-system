package com.ticketflow.ticketflow.user.api;

import com.ticketflow.ticketflow.user.domain.Role;
import com.ticketflow.ticketflow.user.dto.UserResponse;
import com.ticketflow.ticketflow.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        String email = authentication.getName();
        return userService.getByEmail(email);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        String email = authentication.getName();
        userService.logoutByEmail(email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse grantRole(@PathVariable Long id, @PathVariable Role role) {
        return userService.grantRole(id, role);
    }
}
