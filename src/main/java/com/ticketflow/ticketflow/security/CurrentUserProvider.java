package com.ticketflow.ticketflow.security;

import com.ticketflow.ticketflow.common.error.UnauthorizedException;
import com.ticketflow.ticketflow.user.domain.User;
import com.ticketflow.ticketflow.user.repository.UserRepository;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null){
            throw new UnauthorizedException("Authentication required");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    public Long currentUserId() {
        return currentUser().getId();
    }
}
