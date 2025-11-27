package com.ecoguard.ecoguard.dto;

import lombok.Data;

/**
 * Data Transfer Object for device command responses.
 * <p>
 * Used as the response format when the ESP32 device fetches pending commands.
 * Contains the command ID, type, and optional parameters.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Data
public class DeviceCommandResponse {
    /**
     * Default constructor.
     */
    public DeviceCommandResponse() {
    }

    private Long id;
    private String commandType;
    private String parameters;
}

