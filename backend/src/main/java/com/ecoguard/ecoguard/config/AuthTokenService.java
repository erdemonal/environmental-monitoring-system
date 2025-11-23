package com.ecoguard.ecoguard.config;

import com.ecoguard.ecoguard.entity.Role;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthTokenService {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public record Session(String username, Role role, Instant issuedAt) {}

    public String createSession(String username, Role role) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(username, role, Instant.now()));
        return token;
    }

    public Session getSession(String token) {
        return sessions.get(token);
    }

    public void invalidate(String token) {
        sessions.remove(token);
    }
}

