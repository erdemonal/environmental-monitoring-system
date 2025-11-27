package com.ecoguard.ecoguard.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object for threshold data sent to ESP32 device.
 * <p>
 * Used as the response format for GET /api/device/thresholds endpoint.
 * Contains threshold information in a format suitable for device processing.
 *
 * @param metricType the metric type name (TEMP, HUMIDITY, CO2, or LIGHT)
 * @param minValue the minimum acceptable value for this metric
 * @param maxValue the maximum acceptable value for this metric
 *
 * @author EcoGuard 
 * @since 1.0
 */
public record ThresholdDeviceResponse(
        String metricType,
        BigDecimal minValue,
        BigDecimal maxValue
) {}


