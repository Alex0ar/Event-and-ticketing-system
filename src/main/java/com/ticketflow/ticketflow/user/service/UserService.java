package com.ticketflow.ticketflow.user.service;

import com.ticketflow.ticketflow.common.error.ConflictException;
import com.ticketflow.ticketflow.common.error.NotFoundException;
import com.ticketflow.ticketflow.common.error.UnauthorizedException;
import com.ticketflow.ticketflow.security.JwtService;
import com.ticketflow.ticketflow.security.RefreshTokenService;
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
    private final RefreshTokenService refreshTokenService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
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

    @Transactional
    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return toResponse(user);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid password");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user.getId());
        return new TokenResponse(accessToken, refreshToken, "Bearer");
    }

    public TokenResponse refresh(String refreshToken) {
        Long userId = refreshTokenService.consume(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        String newAccess = jwtService.generateAccessToken(user);
        String newRefresh = refreshTokenService.issue(user.getId());
        return new TokenResponse(newAccess, newRefresh, "Bearer");
    }

    public void logout(Long userId) {
        refreshTokenService.revokeAll(userId);
    }

    public void logoutByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        refreshTokenService.revokeAll(user.getId());
    }

    @Transactional
    public UserResponse grantRole(Long userId, Role role) {
        User user  = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.getRoles().add(role);
        User saved =  userRepository.save(user);
        return toResponse(saved);
    }

}
