package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.entity.Alert;
import com.ecoguard.ecoguard.repository.AlertRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for admin access to alert data.
 * <p>
 * Provides the same endpoints as AlertController but requires ADMIN role.
 * Allows administrators to view all alerts in the system.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@RestController
@RequestMapping("/api/admin/alerts")
public class AdminAlertController {

    private final AlertRepository alertRepository;

    /**
     * Constructs a new AdminAlertController with required dependencies.
     *
     * @param alertRepository repository for alert data access
     */
    public AdminAlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * Retrieves all alerts from the database.
     *
     * @return list of all alert records
     */
    @GetMapping
    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    /**
     * Retrieves an alert by its unique identifier.
     *
     * @param id the alert ID to look up
     * @return ResponseEntity containing the alert if found, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Alert> getAlertById(@PathVariable Long id) {
        return alertRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

