package com.ecoguard.ecoguard.dto;

import lombok.Data;

/**
 * Data Transfer Object for sensor data (simplified format).
 * <p>
 * Contains only temperature, humidity, and CO2 values.
 * Used for simplified data transfer in certain contexts.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Data
public class SensorDataDto {
    /**
     * Default constructor.
     */
    public SensorDataDto() {
    }

    private double temperature;
    private double humidity;
    private double co2;
}