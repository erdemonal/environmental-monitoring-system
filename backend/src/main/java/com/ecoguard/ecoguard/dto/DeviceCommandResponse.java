package com.ecoguard.ecoguard.dto;

import lombok.Data;

@Data
public class DeviceCommandResponse {
    private Long id;
    private String commandType;
    private String parameters;
}

