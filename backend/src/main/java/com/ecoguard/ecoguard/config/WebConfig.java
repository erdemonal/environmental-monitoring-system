package com.ecoguard.ecoguard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for CORS and interceptors.
 * <p>
 * Configures Cross-Origin Resource Sharing (CORS) to allow requests from the
 * frontend application and registers the authentication interceptor for API endpoints.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /**
     * Constructs a new WebConfig with required dependencies.
     *
     * @param authInterceptor the authentication interceptor to register
     */
    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * Configures CORS mappings to allow frontend requests.
     * <p>
     * Allows all HTTP methods and headers from localhost:5173 (Vite dev server)
     * with credentials enabled.
     *
     * @param registry the CORS registry to configure
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") 
                .allowedOrigins("http://localhost:5173") 
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") 
                .allowedHeaders("*") 
                .allowCredentials(true); 
    }

    /**
     * Registers the authentication interceptor for API endpoints.
     * <p>
     * Applies authentication to all /api/** paths except /api/auth/login.
     *
     * @param registry the interceptor registry to configure
     */
    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
    }
}