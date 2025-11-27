package com.ecoguard.ecoguard.config;

import com.ecoguard.ecoguard.entity.*;
import com.ecoguard.ecoguard.repository.ThresholdRepository;
import com.ecoguard.ecoguard.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration class for initializing default data on application startup.
 * <p>
 * Seeds the database with default users and threshold configurations using
 * Spring Boot's CommandLineRunner beans. Runs automatically when the application starts.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Configuration
public class DataInitializer {
    /**
     * Default constructor.
     */
    public DataInitializer() {
    }

    /**
     * Seeds default users into the database.
     * <p>
     * Creates admin and user accounts if they don't exist, or updates passwords
     * if they do exist but passwords don't match.
     *
     * @param userRepository repository for user persistence
     * @param passwordEncoder encoder for password hashing
     * @return CommandLineRunner that executes on application startup
     */
    @Bean
	CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			seedUser(userRepository, passwordEncoder, "admin", "admin", Role.ADMIN);
			seedUser(userRepository, passwordEncoder, "user", "user", Role.USER);
			seedUser(userRepository, passwordEncoder, "erdem", "erdem", Role.USER);
			seedUser(userRepository, passwordEncoder, "dawood", "dawood", Role.USER);
			seedUser(userRepository, passwordEncoder, "shariar", "shariar", Role.USER);
		};
	}

	/**
	 * Seeds default threshold configurations into the database.
	 * <p>
	 * Creates default thresholds for TEMP, HUMIDITY, CO2, and LIGHT metrics
	 * if they don't already exist.
	 *
	 * @param thresholdRepository repository for threshold persistence
	 * @return CommandLineRunner that executes on application startup
	 */
	@Bean
	CommandLineRunner seedDefaults(ThresholdRepository thresholdRepository) {
		return args -> {
			if (thresholdRepository.findByMetricType(MetricType.TEMP).isEmpty()) {
				Threshold t = new Threshold();
				t.setMetricType(MetricType.TEMP);
				t.setMinValue(new java.math.BigDecimal("10"));
				t.setMaxValue(new java.math.BigDecimal("30"));
				thresholdRepository.save(t);
			}
			if (thresholdRepository.findByMetricType(MetricType.HUMIDITY).isEmpty()) {
				Threshold t = new Threshold();
				t.setMetricType(MetricType.HUMIDITY);
				t.setMinValue(new java.math.BigDecimal("30"));
				t.setMaxValue(new java.math.BigDecimal("70"));
				thresholdRepository.save(t);
			}
			if (thresholdRepository.findByMetricType(MetricType.CO2).isEmpty()) {
				Threshold t = new Threshold();
				t.setMetricType(MetricType.CO2);
				t.setMinValue(new java.math.BigDecimal("400"));
				t.setMaxValue(new java.math.BigDecimal("1200"));
				thresholdRepository.save(t);
			}
			if (thresholdRepository.findByMetricType(MetricType.LIGHT).isEmpty()) {
				Threshold t = new Threshold();
				t.setMetricType(MetricType.LIGHT);
				t.setMinValue(new java.math.BigDecimal("0"));
				t.setMaxValue(new java.math.BigDecimal("1000"));
				thresholdRepository.save(t);
			}
		};
	}

    /**
     * Helper method to seed a single user into the database.
     * <p>
     * Creates the user if it doesn't exist, or updates the password if it exists
     * but the password doesn't match.
     *
     * @param userRepository repository for user persistence
     * @param passwordEncoder encoder for password hashing
     * @param username the username to create or update
     * @param password the plain text password to hash and store
     * @param role the user's role (ADMIN or USER)
     */
    private void seedUser(UserRepository userRepository, PasswordEncoder passwordEncoder, String username, String password, Role role) {
        userRepository.findByUsername(username).ifPresentOrElse(
                u -> {
                    if (!passwordEncoder.matches(password, u.getPasswordHash())) {
                        u.setPasswordHash(passwordEncoder.encode(password));
                        userRepository.save(u);
                    }
                },
                () -> {
                    User u = new User();
                    u.setUsername(username);
                    u.setPasswordHash(passwordEncoder.encode(password));
                    u.setRole(role);
                    userRepository.save(u);
                }
        );
    }
}