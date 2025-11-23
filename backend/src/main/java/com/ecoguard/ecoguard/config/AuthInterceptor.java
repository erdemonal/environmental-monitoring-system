package com.ecoguard.ecoguard.config;

import com.ecoguard.ecoguard.entity.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_SESSION = "authSession";

    private final AuthTokenService tokenService;

    public AuthInterceptor(AuthTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            return true;
        }
        if (path.startsWith("/api/auth/login")) {
            return true;
        }
        if (path.startsWith("/api/device")) {
            String deviceKey = request.getHeader("X-Device-Key");
            String qpKey = request.getParameter("key");
            if ("demo-device-key".equals(deviceKey) || "demo-device-key".equals(qpKey)) {
                return true;
            }
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        String token = header.substring("Bearer ".length());
        AuthTokenService.Session session = tokenService.getSession(token);
        if (session == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        if (path.startsWith("/api/admin") && session.role() != Role.ADMIN) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }
        if (path.startsWith("/api/user") && session.role() != Role.USER) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }
        request.setAttribute(ATTR_SESSION, session);
        return true;
    }
}

