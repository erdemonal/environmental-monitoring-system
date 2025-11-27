package com.ecoguard.ecoguard.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Represents a notification sent to a user for an alert.
 * <p>
 * This entity tracks which users have been notified about specific alerts.
 * Used for push notification delivery tracking and preventing duplicate notifications.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Entity
@Table(name = "NOTIFICATIONS")
@Data
public class Notification {
    /**
     * Default constructor.
     */
    public Notification() {
    }

    /**
     * Unique identifier for the notification record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    /**
     * The user who received this notification.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The alert that triggered this notification.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    /**
     * Timestamp when the notification was sent.
     * Automatically set to current time on persistence.
     */
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    /**
     * JPA lifecycle callback to set timestamp before persisting.
     * Automatically sets {@code sentAt} to current time if not already set.
     */
    @PrePersist
    public void prePersist() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }
}

