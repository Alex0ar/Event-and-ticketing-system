package com.ticketflow.ticketflow.generator;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHashTest {
    @Test
    void generate() {
        System.out.println(new BCryptPasswordEncoder().encode("secret123"));
    }
}
