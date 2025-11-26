package com.ecoguard.ecoguard.config;

import com.ecoguard.ecoguard.entity.Role;
import com.ecoguard.ecoguard.entity.MetricType;
import com.ecoguard.ecoguard.entity.Threshold;
import com.ecoguard.ecoguard.entity.User;
import com.ecoguard.ecoguard.repository.ThresholdRepository;
import com.ecoguard.ecoguard.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

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