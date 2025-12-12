package com.ecoguard.ecoguard.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around FirebaseMessaging to send push notifications to a device token.
 */
@Service
public class PushNotificationService {

    /**
     * Sends a basic notification (title + body) to the given device token.
     *
     * @param deviceToken FCM device token registered by the mobile app
     * @param title       notification title
     * @param body        notification body
     */
    public void sendPushNotification(String deviceToken, String title, String body) {
        if (deviceToken == null || deviceToken.isBlank()) {
            return;
        }

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setToken(deviceToken)
                .setNotification(notification)
                .build();

        try {
            System.out.println("[FCM] Sending push to token=" + deviceToken + " title=" + title + " body=" + body);
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("[FCM] Successfully sent message: " + response);
        } catch (Exception e) {
            System.err.println("[FCM] Failed to send push notification to token " + deviceToken + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}

