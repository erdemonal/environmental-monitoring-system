package com.ecoguard.ecoguard.dto;

import lombok.Data;

@Data
public class DeviceCommandPayload {
    private String deviceKey;
    private String commandType;
    private String parameters;
}

