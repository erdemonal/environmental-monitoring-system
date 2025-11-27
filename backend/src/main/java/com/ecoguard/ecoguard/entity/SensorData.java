package com.ecoguard.ecoguard.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a sensor reading from the ESP32 device.
 * <p>
 * This entity stores environmental data collected by the device, including
 * temperature, humidity, CO2 level, and light level. Each reading is timestamped
 * and stored in the database for historical analysis and charting.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Entity
@Table(name = "SENSOR_DATA")
@Data
public class SensorData {
    /**
     * Default constructor.
     */
    public SensorData() {
    }

    /**
     * Unique identifier for the sensor reading.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reading_id")
    private Long id;

    /**
     * Temperature reading in degrees Celsius.
     * Stored with 2 decimal places precision.
     */
    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temperature;

    /**
     * Relative humidity reading as a percentage.
     * Stored with 2 decimal places precision.
     */
    @Column(name = "humidity", precision = 5, scale = 2)
    private BigDecimal humidity;

    /**
     * CO2 concentration in parts per million (ppm).
     * Measured by the SCD41 sensor.
     */
    @Column(name = "co2_level")
    private Integer co2Level;

    /**
     * Light level in lux.
     * Measured by the TEMT6000 light sensor.
     */
    @Column(name = "light_level")
    private Integer lightLevel;

    /**
     * Timestamp when the reading was taken.
     * Automatically set to current time on persistence.
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * JPA lifecycle callback to set timestamp before persisting.
     * Automatically sets {@code timestamp} to current time if not already set.
     */
    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}