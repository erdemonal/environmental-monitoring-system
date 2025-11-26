package com.ecoguard.ecoguard.dto;

import java.math.BigDecimal;

public record ThresholdDeviceResponse(
        String metricType,
        BigDecimal minValue,
        BigDecimal maxValue
) {}


