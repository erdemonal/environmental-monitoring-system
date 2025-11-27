package com.ecoguard.ecoguard.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for device configuration responses.
 * <p>
 * Contains device network configuration including WiFi credentials and backend connection info.
 * Currently deprecated in favor of hardcoded configuration.
 *
 * @param deviceKey the device identifier
 * @param wifiSsid the WiFi network SSID
 * @param wifiPassword the WiFi network password
 * @param backendHost the backend server hostname or IP
 * @param backendPort the backend server port
 * @param updatedAt timestamp when the configuration was last updated
 *
 * @author EcoGuard
 * @since 1.0
 */
public record DeviceConfigResponse(
        String deviceKey,
        String wifiSsid,
        String wifiPassword,
        String backendHost,
        Integer backendPort,
        LocalDateTime updatedAt
) {}

