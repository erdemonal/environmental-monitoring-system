package com.ecoguard.ecoguard.config;

import com.ecoguard.ecoguard.entity.Role;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing authentication tokens and user sessions.
 * <p>
 * Provides token-based authentication using UUID tokens stored in memory.
 * Each session contains username, role, and issue timestamp. Sessions are
 * stored in a thread-safe ConcurrentHashMap.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Service
public class AuthTokenService {
    /**
     * Default constructor.
     */
    public AuthTokenService() {
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * Represents an authenticated user session.
     *
     * @param username the authenticated username
     * @param role the user's role (ADMIN or USER)
     * @param issuedAt timestamp when the session was created
     */
    public record Session(String username, Role role, Instant issuedAt) {}

    /**
     * Creates a new authentication session and returns a token.
     *
     * @param username the username to create a session for
     * @param role the user's role
     * @return a UUID token string that can be used for authentication
     */
    public String createSession(String username, Role role) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(username, role, Instant.now()));
        return token;
    }

    /**
     * Retrieves a session by its token.
     *
     * @param token the authentication token
     * @return the session if found, or null if token is invalid
     */
    public Session getSession(String token) {
        return sessions.get(token);
    }

    /**
     * Invalidates a session by removing it from the session store.
     *
     * @param token the token to invalidate
     */
    public void invalidate(String token) {
        sessions.remove(token);
    }
}

