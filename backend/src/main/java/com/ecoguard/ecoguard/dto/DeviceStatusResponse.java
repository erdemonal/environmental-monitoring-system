package com.ecoguard.ecoguard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DeviceStatusResponse(
        LocalDateTime lastReadingTime,
        String lastIpAddress,
        BigDecimal temperature,
        BigDecimal humidity,
        Integer co2,
        Integer lightLevel,
        LocalDateTime lastAlertTime,
        String lastAlertType,
        String lastAdminMessage
) {}


