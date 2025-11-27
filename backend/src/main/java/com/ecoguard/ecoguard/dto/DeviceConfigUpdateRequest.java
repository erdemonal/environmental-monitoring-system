package com.ecoguard.ecoguard.dto;

/**
 * Data Transfer Object for device configuration update requests.
 * <p>
 * Used to update device network settings remotely.
 * Currently deprecated in favor of hardcoded configuration.
 *
 * @param wifiSsid the WiFi network SSID to set
 * @param wifiPassword the WiFi network password to set
 * @param backendHost the backend server hostname or IP to set
 * @param backendPort the backend server port to set
 *
 * @author EcoGuard
 * @since 1.0
 */
public record DeviceConfigUpdateRequest(
        String wifiSsid,
        String wifiPassword,
        String backendHost,
        Integer backendPort
) {}

