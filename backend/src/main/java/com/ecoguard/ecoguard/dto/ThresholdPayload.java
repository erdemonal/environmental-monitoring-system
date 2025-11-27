package com.ecoguard.ecoguard.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object for threshold update requests.
 * <p>
 * Used as the request body for PUT /api/admin/thresholds/{id} endpoint.
 * All fields are optional; only provided fields will be updated.
 *
 * @author EcoGuard 
 * @since 1.0
 */
public class ThresholdPayload {
    /**
     * Default constructor.
     */
    public ThresholdPayload() {
    }

    private String metricType;
    private BigDecimal minValue;
    private BigDecimal maxValue;

    /**
     * Gets the metric type for this threshold update.
     *
     * @return the metric type string, or null if not provided
     */
    public String getMetricType() {
        return metricType;
    }

    /**
     * Sets the metric type for this threshold update.
     *
     * @param metricType the metric type to set
     */
    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    /**
     * Gets the minimum value for this threshold update.
     *
     * @return the minimum value, or null if not provided
     */
    public BigDecimal getMinValue() {
        return minValue;
    }

    /**
     * Sets the minimum value for this threshold update.
     *
     * @param minValue the minimum value to set
     */
    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }

    /**
     * Gets the maximum value for this threshold update.
     *
     * @return the maximum value, or null if not provided
     */
    public BigDecimal getMaxValue() {
        return maxValue;
    }

    /**
     * Sets the maximum value for this threshold update.
     *
     * @param maxValue the maximum value to set
     */
    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }
}

