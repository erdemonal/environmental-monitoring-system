package com.ecoguard.ecoguard.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "THRESHOLDS")
@Data
public class Threshold {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "threshold_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "metric_type", nullable = false, length = 10)
    private MetricType metricType;

    @Column(name = "min_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal maxValue;

    @Column(name = "disabled", nullable = false)
    private Boolean disabled = false;

}