package com.ecoguard.ecoguard.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Represents a user in the EcoGuard system.
 * <p>
 * Users can have different roles (Admin or User) and are used for authentication
 * and authorization throughout the application. Each user has a username, password hash,
 * and optional device token for push notifications.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Entity
@Table(name = "USERS")
@Data
public class User {
    /**
     * Default constructor.
     */
    public User() {
    }

    /**
     * Unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    /**
     * Unique username for authentication.
     * Must be unique across all users and cannot be null.
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Hashed password for secure authentication.
     * Stored as a BCrypt hash, never stored in plain text.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * User role determining access level.
     * Can be either ADMIN or USER.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    /**
     * Device token for push notifications (FCM/APNs).
     * Optional field, can be null if user hasn't registered a device.
     */
    @Column(name = "device_token", length = 255)
    private String deviceToken;

    /**
     * Timestamp when the user account was created.
     * Automatically set on first persistence.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * JPA lifecycle callback to set creation timestamp before persisting.
     * Automatically sets {@code createdAt} to current time if not already set.
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

