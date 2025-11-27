package com.ecoguard.ecoguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for EcoGuard Environmental Monitoring System.
 * <p>
 * EcoGuard is a Spring Boot application that provides a REST API backend
 * for monitoring environmental conditions (temperature, humidity, CO2, light)
 * via an ESP32 device. The system supports user authentication, threshold
 * management, alert generation, and remote device control.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@SpringBootApplication
public class EcoguardApplication {
	/**
	 * Default constructor.
	 */
	public EcoguardApplication() {
	}

	/**
	 * Main entry point for the EcoGuard application.
	 * <p>
	 * Launches the Spring Boot application with default configuration.
	 * The application will start an embedded Tomcat server and initialize
	 * the H2 in-memory database with default users and thresholds.
	 *
	 * @param args command line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(EcoguardApplication.class, args);
	}

}
