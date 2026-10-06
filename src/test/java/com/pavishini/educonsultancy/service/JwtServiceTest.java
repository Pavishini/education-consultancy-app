package com.pavishini.educonsultancy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-jwt-secret-key-for-education-consultancy-1234567890",
            3_600_000L
    );

    @Test
    void generateToken_shouldCreateValidTokenForUser() {
        String token = jwtService.generateToken("student@example.com", "ROLE_STUDENT");

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("student@example.com", jwtService.extractUsername(token));
        assertEquals("ROLE_STUDENT", jwtService.extractRole(token));
    }

    @Test
    void isTokenValid_shouldRejectExpiredToken() {
        String expiredToken = jwtService.generateToken("student@example.com", "ROLE_STUDENT", Date.from(Instant.now().minusSeconds(60)));

        assertFalse(jwtService.isTokenValid(expiredToken));
        assertThrows(Exception.class, () -> jwtService.extractUsername(expiredToken));
    }

    @Test
    void isTokenValid_shouldRejectTamperedToken() {
        String token = jwtService.generateToken("student@example.com", "ROLE_STUDENT");
        String tamperedToken = token.substring(0, token.length() - 1) + "A";

        assertFalse(jwtService.isTokenValid(tamperedToken));
        assertThrows(Exception.class, () -> jwtService.extractUsername(tamperedToken));
    }
}
