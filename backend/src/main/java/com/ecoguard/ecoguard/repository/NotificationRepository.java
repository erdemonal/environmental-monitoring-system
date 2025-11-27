package com.ecoguard.ecoguard.repository;

import com.ecoguard.ecoguard.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for notification persistence operations.
 * <p>
 * Provides standard JPA repository methods for managing notification records.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
}

