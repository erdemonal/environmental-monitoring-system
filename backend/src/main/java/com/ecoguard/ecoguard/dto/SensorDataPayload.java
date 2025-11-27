package com.ecoguard.ecoguard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for sensor data ingestion from ESP32 device.
 * <p>
 * Used as the request body for POST /api/device/sensor-data endpoint.
 * All fields are optional, but at least one metric must be provided.
 *
 * @author EcoGuard 
 * @since 1.0
 */
public class SensorDataPayload {
    /**
     * Default constructor.
     */
    public SensorDataPayload() {
    }

    private BigDecimal temperature;
    private BigDecimal humidity;
    private Integer co2Level;
    private Integer lightLevel;
    private LocalDateTime timestamp;

    /**
     * Gets the temperature reading in degrees Celsius.
     *
     * @return the temperature value, or null if not provided
     */
    public BigDecimal getTemperature() {
        return temperature;
    }

    /**
     * Sets the temperature reading in degrees Celsius.
     *
     * @param temperature the temperature value to set
     */
    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    /**
     * Gets the humidity reading as a percentage.
     *
     * @return the humidity value, or null if not provided
     */
    public BigDecimal getHumidity() {
        return humidity;
    }

    /**
     * Sets the humidity reading as a percentage.
     *
     * @param humidity the humidity value to set
     */
    public void setHumidity(BigDecimal humidity) {
        this.humidity = humidity;
    }

    /**
     * Gets the CO2 level reading in parts per million (ppm).
     *
     * @return the CO2 level value, or null if not provided
     */
    public Integer getCo2Level() {
        return co2Level;
    }

    /**
     * Sets the CO2 level reading in parts per million (ppm).
     *
     * @param co2Level the CO2 level value to set
     */
    public void setCo2Level(Integer co2Level) {
        this.co2Level = co2Level;
    }

    /**
     * Gets the light level reading in lux.
     *
     * @return the light level value, or null if not provided
     */
    public Integer getLightLevel() {
        return lightLevel;
    }

    /**
     * Sets the light level reading in lux.
     *
     * @param lightLevel the light level value to set
     */
    public void setLightLevel(Integer lightLevel) {
        this.lightLevel = lightLevel;
    }

    /**
     * Gets the timestamp when the reading was taken.
     *
     * @return the timestamp, or null if not provided (will default to current time)
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when the reading was taken.
     *
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

