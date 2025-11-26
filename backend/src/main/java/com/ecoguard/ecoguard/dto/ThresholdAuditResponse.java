package com.ecoguard.ecoguard.dto;

import com.ecoguard.ecoguard.entity.MetricType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ThresholdAuditResponse(
        Long id,
        Long thresholdId,
        MetricType metricType,
        BigDecimal minValue,
        BigDecimal maxValue,
        String updatedBy,
        LocalDateTime updatedAt
) {}


