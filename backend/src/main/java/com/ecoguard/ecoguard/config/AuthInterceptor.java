package com.ecoguard.ecoguard.config;

import com.ecoguard.ecoguard.entity.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor for authentication and authorization.
 * <p>
 * Intercepts all HTTP requests and validates authentication tokens. Handles three
 * types of endpoints:
 * <ul>
 *   <li>Public endpoints (/api/auth/login) - no authentication required</li>
 *   <li>Device endpoints (/api/device) - requires X-Device-Key header</li>
 *   <li>User/Admin endpoints - requires Bearer token and role-based authorization</li>
 * </ul>
 * Sets the authenticated session as a request attribute for use in controllers.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /**
     * Request attribute name for storing the authenticated session.
     */
    public static final String ATTR_SESSION = "authSession";

    private final AuthTokenService tokenService;

    /**
     * Constructs a new AuthInterceptor with required dependencies.
     *
     * @param tokenService service for token validation
     */
    public AuthInterceptor(AuthTokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * Intercepts requests before handler execution to perform authentication and authorization.
     * <p>
     * Validates tokens, checks role-based access, and sets session attribute on success.
     * Returns false to stop request processing if authentication/authorization fails.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param handler the target handler
     * @return true if request should proceed, false to stop processing
     */
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

