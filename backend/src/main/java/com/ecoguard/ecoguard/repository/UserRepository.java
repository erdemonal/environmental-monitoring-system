package com.ecoguard.ecoguard.repository;

import com.ecoguard.ecoguard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for user persistence operations.
 * <p>
 * Provides methods for querying users by username.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return Optional containing the user if found, or empty if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * Retrieves all users that have a registered device token.
     *
     * @return list of users with non-null device tokens
     */
    java.util.List<User> findByDeviceTokenIsNotNull();
}

