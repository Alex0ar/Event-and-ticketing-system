package com.ticketflow.ticketflow.user.service;

import com.ticketflow.ticketflow.common.error.ConflictException;
import com.ticketflow.ticketflow.common.error.NotFoundException;
import com.ticketflow.ticketflow.common.error.UnauthorizedException;
import com.ticketflow.ticketflow.security.JwtService;
import com.ticketflow.ticketflow.user.domain.Role;
import com.ticketflow.ticketflow.user.domain.User;
import com.ticketflow.ticketflow.user.dto.LoginRequest;
import com.ticketflow.ticketflow.user.dto.RegisterRequest;
import com.ticketflow.ticketflow.user.dto.TokenResponse;
import com.ticketflow.ticketflow.user.dto.UserResponse;
import com.ticketflow.ticketflow.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse regiter(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRoles(Set.of(Role.CUSTOMER));
        User savedUser = userRepository.save(user);
        return toResponse(savedUser);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRoles());
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid password");
        }

        String accessToken = jwtService.generateAccessToken(user);
        return new TokenResponse(accessToken, "Bearer");
    }

    @Transactional
    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return toResponse(user);
    }
}
