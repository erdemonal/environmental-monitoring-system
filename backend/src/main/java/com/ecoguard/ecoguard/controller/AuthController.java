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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final AuthTokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, AuthTokenService tokenService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

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
