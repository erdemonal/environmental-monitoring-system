package com.ecoguard.ecoguard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for device status information.
 * <p>
 * Contains comprehensive device status including latest sensor readings,
 * online/offline status, last alert, and last admin message.
 * Used by the admin panel's Device Status card.
 *
 * @param lastReadingTime timestamp of the most recent sensor reading
 * @param temperature latest temperature reading in degrees Celsius
 * @param humidity latest humidity reading as a percentage
 * @param co2 latest CO2 reading in ppm
 * @param lightLevel latest light reading in lux
 * @param lastAlertTime timestamp of the most recent alert
 * @param lastAlertType type of the most recent alert (THRESHOLD or INTRUDER)
 * @param lastAdminMessage text of the last DISPLAY_MESSAGE command sent
 * @param online true if device is considered online (data received within 20 seconds)
 * @param secondsSinceLastSeen number of seconds since last data was received
 *
 * @author EcoGuard 
 * @since 1.0
 */
public record DeviceStatusResponse(
        LocalDateTime lastReadingTime,
        BigDecimal temperature,
        BigDecimal humidity,
        Integer co2,
        Integer lightLevel,
        LocalDateTime lastAlertTime,
        String lastAlertType,
        String lastAdminMessage,
        boolean online,
        Long secondsSinceLastSeen
) {}


