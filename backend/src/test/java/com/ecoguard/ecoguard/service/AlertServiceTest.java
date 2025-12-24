package com.ecoguard.ecoguard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for AlertService.
 */
class AlertServiceTest {

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService();
    }

    @Test
    void testAlertService_CanBeInstantiated() {
        assertNotNull(alertService);
    }
}

