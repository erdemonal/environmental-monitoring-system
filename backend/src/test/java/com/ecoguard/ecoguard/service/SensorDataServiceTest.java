package com.ecoguard.ecoguard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for SensorDataService.
 */
class SensorDataServiceTest {

    private SensorDataService sensorDataService;

    @BeforeEach
    void setUp() {
        sensorDataService = new SensorDataService();
    }

    @Test
    void testSensorDataService_CanBeInstantiated() {
        assertNotNull(sensorDataService);
    }
}

