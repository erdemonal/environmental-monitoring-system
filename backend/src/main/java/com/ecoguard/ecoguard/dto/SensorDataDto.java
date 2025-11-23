package com.ecoguard.ecoguard.dto;

import lombok.Data;

@Data
public class SensorDataDto {
    private double temperature;
    private double humidity;
    private double co2;
}