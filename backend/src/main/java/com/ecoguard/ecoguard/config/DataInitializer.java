package com.ecoguard.ecoguard.config;

import com.ecoguard.ecoguard.entity.Role;
import com.ecoguard.ecoguard.entity.MetricType;
import com.ecoguard.ecoguard.entity.Threshold;
import com.ecoguard.ecoguard.entity.SensorData;
import com.ecoguard.ecoguard.entity.User;
import com.ecoguard.ecoguard.repository.ThresholdRepository;
import com.ecoguard.ecoguard.repository.SensorDataRepository;
import com.ecoguard.ecoguard.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
	CommandLineRunner seedUsers(UserRepository userRepository) {
		return args -> {
			seedUser(userRepository, "admin", Role.ADMIN);
			seedUser(userRepository, "user", Role.USER);
			seedUser(userRepository, "erdem", Role.USER);
			seedUser(userRepository, "dawood", Role.USER);
			seedUser(userRepository, "shariar", Role.USER);
		};
	}

	@Bean
	CommandLineRunner seedDefaults(ThresholdRepository thresholdRepository, SensorDataRepository sensorDataRepository) {
		return args -> {
			// thresholds
			if (thresholdRepository.findByMetricType(MetricType.TEMP).isEmpty()) {
				Threshold t = new Threshold();
				t.setMetricType(MetricType.TEMP);
				t.setMinValue(new java.math.BigDecimal("10"));
				t.setMaxValue(new java.math.BigDecimal("30"));
				t.setDisabled(false);
				thresholdRepository.save(t);
			}
			if (thresholdRepository.findByMetricType(MetricType.HUMIDITY).isEmpty()) {
				Threshold t = new Threshold();
				t.setMetricType(MetricType.HUMIDITY);
				t.setMinValue(new java.math.BigDecimal("30"));
				t.setMaxValue(new java.math.BigDecimal("70"));
				t.setDisabled(false);
				thresholdRepository.save(t);
			}
			if (thresholdRepository.findByMetricType(MetricType.CO2).isEmpty()) {
				Threshold t = new Threshold();
				t.setMetricType(MetricType.CO2);
				t.setMinValue(new java.math.BigDecimal("400"));
				t.setMaxValue(new java.math.BigDecimal("1200"));
				t.setDisabled(false);
				thresholdRepository.save(t);
			}
			if (thresholdRepository.findByMetricType(MetricType.LIGHT).isEmpty()) {
				Threshold t = new Threshold();
				t.setMetricType(MetricType.LIGHT);
				t.setMinValue(new java.math.BigDecimal("0"));
				t.setMaxValue(new java.math.BigDecimal("1000"));
				t.setDisabled(false);
				thresholdRepository.save(t);
			}

			// one sample sensor reading if none exists
			if (sensorDataRepository.count() == 0) {
				SensorData s = new SensorData();
				s.setTemperature(new java.math.BigDecimal("22.5"));
				s.setHumidity(new java.math.BigDecimal("45.0"));
				s.setCo2Level(650);
				s.setLightLevel(300);
				// timestamp set in @PrePersist
				sensorDataRepository.save(s);
			}
		};
	}

    private void seedUser(UserRepository userRepository, String username, Role role) {
        userRepository.findByUsername(username).ifPresentOrElse(
                u -> {},
                () -> {
                    User u = new User();
                    u.setUsername(username);
                    u.setPasswordHash(username); // demo-only passwords
                    u.setRole(role);
                    userRepository.save(u);
                }
        );
    }
}