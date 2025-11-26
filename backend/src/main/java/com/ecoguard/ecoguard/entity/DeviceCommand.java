package com.ecoguard.ecoguard.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "DEVICE_COMMANDS")
@Data
public class DeviceCommand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "command_id")
    private Long id;

    @Column(name = "device_key", nullable = false, length = 100)
    private String deviceKey;

    @Column(name = "command_type", nullable = false, length = 50)
    private String commandType;

    @Column(name = "parameters", length = 500)
    private String parameters;

    @Column(name = "executed", nullable = false)
    private Boolean executed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

