package com.ecoguard.ecoguard.dto;

import lombok.Data;

/**
 * Data Transfer Object for device command creation requests.
 * <p>
 * Used as the request body for POST /api/admin/device/commands endpoint.
 * deviceKey and commandType are required; parameters are optional.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Data
public class DeviceCommandPayload {
    /**
     * Default constructor.
     */
    public DeviceCommandPayload() {
    }

    private String deviceKey;
    private String commandType;
    private String parameters;
}

