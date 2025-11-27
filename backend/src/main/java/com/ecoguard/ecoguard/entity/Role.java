package com.ecoguard.ecoguard.entity;

/**
 * User role enumeration for access control.
 * <p>
 * Defines the two access levels in the EcoGuard system:
 * <ul>
 *   <li>ADMIN - Full access to all features including threshold management and device control</li>
 *   <li>USER - Read-only access to view sensor data and alerts</li>
 * </ul>
 *
 * @author EcoGuard 
 * @since 1.0
 */
public enum Role {
    /**
     * Administrator role with full system access.
     */
    ADMIN,
    
    /**
     * Standard user role with read-only access.
     */
    USER
}

