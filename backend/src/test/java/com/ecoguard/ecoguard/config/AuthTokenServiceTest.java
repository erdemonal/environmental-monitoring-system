package com.ecoguard.ecoguard.config;

import com.ecoguard.ecoguard.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthTokenService.
 */
class AuthTokenServiceTest {

    private AuthTokenService authTokenService;

    @BeforeEach
    void setUp() {
        authTokenService = new AuthTokenService();
    }

    @Test
    void testCreateSession_ReturnsToken() {
        String username = "testuser";
        Role role = Role.USER;

        String token = authTokenService.createSession(username, role);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testCreateSession_StoresSession() {
        String username = "testuser";
        Role role = Role.USER;

        String token = authTokenService.createSession(username, role);
        AuthTokenService.Session session = authTokenService.getSession(token);

        assertNotNull(session);
        assertEquals(username, session.username());
        assertEquals(role, session.role());
        assertNotNull(session.issuedAt());
    }

    @Test
    void testGetSession_WithValidToken() {
        String username = "admin";
        Role role = Role.ADMIN;
        String token = authTokenService.createSession(username, role);

        AuthTokenService.Session session = authTokenService.getSession(token);

        assertNotNull(session);
        assertEquals(username, session.username());
        assertEquals(role, session.role());
    }

    @Test
    void testGetSession_WithInvalidToken() {
        AuthTokenService.Session session = authTokenService.getSession("invalid-token");

        assertNull(session);
    }

    @Test
    void testInvalidate_RemovesSession() {
        String token = authTokenService.createSession("testuser", Role.USER);

        authTokenService.invalidate(token);

        assertNull(authTokenService.getSession(token));
    }

    @Test
    void testInvalidate_WithInvalidToken() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            authTokenService.invalidate("invalid-token");
        });
    }

    @Test
    void testMultipleSessions() {
        String token1 = authTokenService.createSession("user1", Role.USER);
        String token2 = authTokenService.createSession("user2", Role.ADMIN);

        assertNotEquals(token1, token2);
        assertEquals("user1", authTokenService.getSession(token1).username());
        assertEquals("user2", authTokenService.getSession(token2).username());
    }

    @Test
    void testSessionRecord() {
        String username = "testuser";
        Role role = Role.USER;
        Instant issuedAt = Instant.now();

        AuthTokenService.Session session = new AuthTokenService.Session(username, role, issuedAt);

        assertEquals(username, session.username());
        assertEquals(role, session.role());
        assertEquals(issuedAt, session.issuedAt());
    }
}

