package com.example.application.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Gateway Identity Context
 * 
 * Represents the authenticated user identity as extracted from HTTP headers
 * injected by Kong API Gateway after successful JWT validation.
 * 
 * This is the core concept of Gateway Offloading Pattern:
 * - Kong validates the JWT token and extracts claims
 * - Kong injects identity info as HTTP headers (X-User-Id, X-User-Role, etc.)
 * - The backend service reads these headers instead of parsing JWT itself
 * - This keeps the service stateless and lightweight
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayIdentity {

    /**
     * User ID extracted from JWT 'sub' claim by Kong.
     * Header: X-User-Id
     */
    private String userId;

    /**
     * User role extracted from JWT 'role' claim by Kong.
     * Header: X-User-Role
     */
    private String userRole;

    /**
     * Username extracted from JWT 'username' claim by Kong.
     * Header: X-User-Username
     */
    private String username;

    /**
     * Email extracted from JWT 'email' claim by Kong.
     * Header: X-User-Email
     */
    private String userEmail;

    /**
     * Check if identity was provided by the gateway.
     */
    public boolean isAuthenticated() {
        return userId != null && !userId.isBlank();
    }

    /**
     * Check if user has admin role.
     */
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(userRole);
    }

    /**
     * Check if user has a specific role.
     */
    public boolean hasRole(String role) {
        return role != null && role.equalsIgnoreCase(userRole);
    }
}
