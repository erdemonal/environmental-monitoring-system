package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.config.AuthTokenService;
import com.ecoguard.ecoguard.entity.Role;
import com.ecoguard.ecoguard.entity.User;
import com.ecoguard.ecoguard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthController.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthTokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPasswordHash("$2a$10$hashedpassword");
        testUser.setRole(Role.USER);
    }

    @Test
    void testLogin_Success() {
        Map<String, String> body = Map.of(
                "username", "testuser",
                "password", "password123"
        );

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(tokenService.createSession("testuser", Role.USER)).thenReturn("test-token-123");

        ResponseEntity<?> response = authController.login(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("test-token-123", responseBody.get("token"));
        assertEquals("testuser", responseBody.get("username"));
        assertEquals(Role.USER, responseBody.get("role"));
    }

    @Test
    void testLogin_InvalidUsername() {
        Map<String, String> body = Map.of(
                "username", "nonexistent",
                "password", "password123"
        );

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.login(body);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("Invalid credentials", responseBody.get("message"));
    }

    @Test
    void testLogin_InvalidPassword() {
        Map<String, String> body = Map.of(
                "username", "testuser",
                "password", "wrongpassword"
        );

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPasswordHash())).thenReturn(false);

        ResponseEntity<?> response = authController.login(body);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("Invalid credentials", responseBody.get("message"));
    }

    @Test
    void testLogin_MissingFields() {
        Map<String, String> body = Map.of();

        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.login(body);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testUpdateDeviceToken_Success() throws Exception {
        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);
        AuthTokenService.Session session = new AuthTokenService.Session("testuser", Role.USER, java.time.Instant.now());
        
        Map<String, String> body = Map.of("token", "device-token-123");

        when(request.getAttribute("authSession")).thenReturn(session);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        ResponseEntity<?> response = authController.updateDeviceToken(body, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateDeviceToken_MissingToken() {
        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);
        Map<String, String> body = Map.of();

        ResponseEntity<?> response = authController.updateDeviceToken(body, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("Device token required", responseBody.get("message"));
    }

    @Test
    void testUpdateDeviceToken_BlankToken() {
        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);
        Map<String, String> body = Map.of("token", "   ");

        ResponseEntity<?> response = authController.updateDeviceToken(body, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testUpdateDeviceToken_Unauthorized() {
        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);
        Map<String, String> body = Map.of("token", "device-token-123");

        when(request.getAttribute("authSession")).thenReturn(null);

        ResponseEntity<?> response = authController.updateDeviceToken(body, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testUpdateDeviceToken_UserNotFound() {
        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);
        AuthTokenService.Session session = new AuthTokenService.Session("nonexistent", Role.USER, java.time.Instant.now());
        Map<String, String> body = Map.of("token", "device-token-123");

        when(request.getAttribute("authSession")).thenReturn(session);
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.updateDeviceToken(body, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

