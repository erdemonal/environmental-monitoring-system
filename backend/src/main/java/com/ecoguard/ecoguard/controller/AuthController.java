package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.config.AuthInterceptor;
import com.ecoguard.ecoguard.config.AuthTokenService;
import com.ecoguard.ecoguard.entity.User;
import com.ecoguard.ecoguard.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for user authentication and session management.
 * <p>
 * Handles user login, token generation, and device token registration
 * for push notifications. All endpoints are publicly accessible (no authentication required).
 *
 * @author EcoGuard 
 * @since 1.0
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final AuthTokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a new AuthController with required dependencies.
     *
     * @param userRepository repository for user data access
     * @param tokenService service for JWT token generation and validation
     * @param passwordEncoder encoder for password hashing and verification
     */
    public AuthController(UserRepository userRepository, AuthTokenService tokenService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates a user and returns a session token.
     * <p>
     * Validates the provided username and password against stored credentials.
     * If authentication succeeds, returns a JWT token along with user information.
     * The token must be included in subsequent requests via the Authorization header.
     *
     * @param body request body containing "username" and "password" fields
     * @return ResponseEntity containing token, username, and role on success,
     *         or 401 Unauthorized with error message on failure
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }
        String token = tokenService.createSession(user.getUsername(), user.getRole());
        Map<String, Object> resp = new HashMap<>();
        resp.put("token", token);
        resp.put("username", user.getUsername());
        resp.put("role", user.getRole());
        return ResponseEntity.ok(resp);
    }

    /**
     * Updates the device token for push notifications.
     * <p>
     * Allows authenticated users to register their device token (FCM/APNs)
     * for receiving push notifications when alerts are generated.
     * Requires a valid authentication token in the request.
     *
     * @param body request body containing "token" field with the device token
     * @param request HTTP request containing the authentication session
     * @return ResponseEntity with success message, or error response if validation fails
     */
    @PutMapping("/device-token")
    public ResponseEntity<?> updateDeviceToken(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String deviceToken = body.get("token");
        if (deviceToken == null || deviceToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Device token required"));
        }
        AuthTokenService.Session session = (AuthTokenService.Session) request.getAttribute(AuthInterceptor.ATTR_SESSION);
        if (session == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        Optional<User> userOpt = userRepository.findByUsername(session.username());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }
        User user = userOpt.get();
        user.setDeviceToken(deviceToken);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Device token updated"));
    }
}
