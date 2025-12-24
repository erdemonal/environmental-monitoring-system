package com.ecoguard.ecoguard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for PushNotificationService.
 */
@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    private PushNotificationService pushNotificationService;

    @BeforeEach
    void setUp() {
        pushNotificationService = new PushNotificationService();
    }

    @Test
    void testSendPushNotification_WithValidToken() {
        String deviceToken = "test-token-123";
        String title = "Test Alert";
        String body = "This is a test notification";

        // Should not throw exception even if Firebase is not initialized in test
        assertDoesNotThrow(() -> {
            pushNotificationService.sendPushNotification(deviceToken, title, body);
        });
    }

    @Test
    void testSendPushNotification_WithNullToken() {
        // Should handle null token gracefully
        assertDoesNotThrow(() -> {
            pushNotificationService.sendPushNotification(null, "Title", "Body");
        });
    }

    @Test
    void testSendPushNotification_WithBlankToken() {
        // Should handle blank token gracefully
        assertDoesNotThrow(() -> {
            pushNotificationService.sendPushNotification("", "Title", "Body");
        });
        assertDoesNotThrow(() -> {
            pushNotificationService.sendPushNotification("   ", "Title", "Body");
        });
    }
}

