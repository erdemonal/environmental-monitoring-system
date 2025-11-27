package com.ecoguard.ecoguard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration class for password encoding.
 * <p>
 * Provides a BCrypt password encoder bean for secure password hashing
 * throughout the application.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Configuration
public class PasswordConfig {
    /**
     * Default constructor.
     */
    public PasswordConfig() {
    }

    /**
     * Creates a BCrypt password encoder bean.
     * <p>
     * BCrypt is a strong, adaptive hashing algorithm suitable for password storage.
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

