package com.ecoguard.ecoguard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for ThresholdService.
 */
class ThresholdServiceTest {

    private ThresholdService thresholdService;

    @BeforeEach
    void setUp() {
        thresholdService = new ThresholdService();
    }

    @Test
    void testThresholdService_CanBeInstantiated() {
        assertNotNull(thresholdService);
    }
}

