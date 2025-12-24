package com.ecoguard.ecoguard.config;

import com.ecoguard.ecoguard.entity.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthInterceptor.
 */
@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock
    private AuthTokenService tokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object handler;

    @InjectMocks
    private AuthInterceptor authInterceptor;

    @Test
    void testPreHandle_LoginEndpoint_ReturnsTrue() {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");

        boolean result = authInterceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(request, never()).getHeader(anyString());
    }

    @Test
    void testPreHandle_OptionsRequest_ReturnsTrue() {
        when(request.getRequestURI()).thenReturn("/api/user/alerts");
        when(request.getMethod()).thenReturn("OPTIONS");

        boolean result = authInterceptor.preHandle(request, response, handler);

        assertTrue(result);
    }

    @Test
    void testPreHandle_DeviceEndpoint_ValidDeviceKey_ReturnsTrue() {
        when(request.getRequestURI()).thenReturn("/api/device/sensor-data");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Device-Key")).thenReturn("demo-device-key");

        boolean result = authInterceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void testPreHandle_DeviceEndpoint_ValidQueryParam_ReturnsTrue() {
        when(request.getRequestURI()).thenReturn("/api/device/sensor-data");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Device-Key")).thenReturn(null);
        when(request.getParameter("key")).thenReturn("demo-device-key");

        boolean result = authInterceptor.preHandle(request, response, handler);

        assertTrue(result);
    }

    @Test
    void testPreHandle_DeviceEndpoint_InvalidDeviceKey_ReturnsFalse() {
        when(request.getRequestURI()).thenReturn("/api/device/sensor-data");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Device-Key")).thenReturn("invalid-key");
        when(request.getParameter("key")).thenReturn(null);

        boolean result = authInterceptor.preHandle(request, response, handler);

        assertFalse(result);
        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void testPreHandle_UserEndpoint_ValidToken_ReturnsTrue() {
        AuthTokenService.Session session = new AuthTokenService.Session("testuser", Role.USER, Instant.now());
        
        when(request.getRequestURI()).thenReturn("/api/user/alerts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenService.getSession("valid-token")).thenReturn(session);

        boolean result = authInterceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(request, times(1)).setAttribute(AuthInterceptor.ATTR_SESSION, session);
    }

    @Test
    void testPreHandle_UserEndpoint_InvalidToken_ReturnsFalse() {
        when(request.getRequestURI()).thenReturn("/api/user/alerts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(tokenService.getSession("invalid-token")).thenReturn(null);

        boolean result = authInterceptor.preHandle(request, response, handler);

        assertFalse(result);
        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void testPreHandle_UserEndpoint_MissingBearer_ReturnsFalse() {
        when(request.getRequestURI()).thenReturn("/api/user/alerts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat");

        boolean result = authInterceptor.preHandle(request, response, handler);

        assertFalse(result);
        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void testPreHandle_UserEndpoint_NoAuthorizationHeader_ReturnsFalse() {
        when(request.getRequestURI()).thenReturn("/api/user/alerts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);

        boolean result = authInterceptor.preHandle(request, response, handler);

        assertFalse(result);
        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void testPreHandle_AdminEndpoint_AdminRole_ReturnsTrue() {
        AuthTokenService.Session session = new AuthTokenService.Session("admin", Role.ADMIN, Instant.now());
        
        when(request.getRequestURI()).thenReturn("/api/admin/thresholds");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(tokenService.getSession("admin-token")).thenReturn(session);

        boolean result = authInterceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(request, times(1)).setAttribute(AuthInterceptor.ATTR_SESSION, session);
    }

    @Test
    void testPreHandle_AdminEndpoint_UserRole_ReturnsFalse() {
        AuthTokenService.Session session = new AuthTokenService.Session("user", Role.USER, Instant.now());
        
        when(request.getRequestURI()).thenReturn("/api/admin/thresholds");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer user-token");
        when(tokenService.getSession("user-token")).thenReturn(session);

        boolean result = authInterceptor.preHandle(request, response, handler);

        assertFalse(result);
        verify(response, times(1)).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void testPreHandle_UserEndpoint_AdminRole_ReturnsFalse() {
        AuthTokenService.Session session = new AuthTokenService.Session("admin", Role.ADMIN, Instant.now());
        
        when(request.getRequestURI()).thenReturn("/api/user/alerts");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(tokenService.getSession("admin-token")).thenReturn(session);

        boolean result = authInterceptor.preHandle(request, response, handler);

        assertFalse(result);
        verify(response, times(1)).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}

