package com.ecoguard.ecoguard.entity;

/**
 * Enumeration of environmental metrics monitored by the system.
 * <p>
 * Each metric type corresponds to a sensor on the ESP32 device:
 * <ul>
 *   <li>TEMP - Temperature in degrees Celsius (SCD41 sensor)</li>
 *   <li>HUMIDITY - Relative humidity percentage (SCD41 sensor)</li>
 *   <li>CO2 - Carbon dioxide concentration in ppm (SCD41 sensor)</li>
 *   <li>LIGHT - Light level in lux (TEMT6000 sensor)</li>
 * </ul>
 *
 * @author EcoGuard 
 * @since 1.0
 */
public enum MetricType {
    /**
     * Temperature metric in degrees Celsius.
     */
    TEMP,
    
    /**
     * Humidity metric as a percentage.
     */
    HUMIDITY,
    
    /**
     * CO2 concentration metric in parts per million (ppm).
     */
    CO2,
    
    /**
     * Light level metric in lux.
     */
    LIGHT
}

