package com.ecoguard.ecoguard.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Represents a threshold configuration for environmental metrics.
 * <p>
 * Thresholds define the acceptable range (min/max) for each metric type
 * (Temperature, Humidity, CO2, Light). When sensor readings exceed these
 * thresholds, alerts are generated and the device LED changes color.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Entity
@Table(name = "THRESHOLDS")
@Data
public class Threshold {
    /**
     * Default constructor.
     */
    public Threshold() {
    }

    /**
     * Unique identifier for the threshold configuration.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "threshold_id")
    private Long id;

    /**
     * The metric type this threshold applies to.
     * Can be TEMP, HUMIDITY, CO2, or LIGHT.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "metric_type", nullable = false, length = 10)
    private MetricType metricType;

    /**
     * Minimum acceptable value for this metric.
     * Values below this will trigger a threshold breach alert.
     */
    @Column(name = "min_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal minValue;

    /**
     * Maximum acceptable value for this metric.
     * Values above this will trigger a threshold breach alert.
     */
    @Column(name = "max_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal maxValue;

}