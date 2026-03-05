package com.example.application.service;

import com.example.application.dto.CreateUserRequest;
import com.example.application.dto.UpdateUserRequest;
import com.example.application.dto.UserResponse;
import com.example.application.gateway.GatewayIdentity;

import java.util.List;
import java.util.UUID;

/**
 * Application Service Interface for User operations.
 * Orchestrates domain logic and infrastructure concerns.
 */
public interface UserService {

    /**
     * Get current user profile from gateway identity.
     */
    UserResponse getCurrentUser(GatewayIdentity identity);

    /**
     * Get user by ID.
     */
    UserResponse getUserById(UUID id);

    /**
     * Get all users (admin only).
     */
    List<UserResponse> getAllUsers();

    /**
     * Create a new user.
     */
    UserResponse createUser(CreateUserRequest request);

    /**
     * Update user profile. Users can only update their own profile.
     * Admins can update any user.
     */
    UserResponse updateUser(UUID id, UpdateUserRequest request, GatewayIdentity identity);

    /**
     * Deactivate a user (admin only).
     */
    void deactivateUser(UUID id);

    /**
     * Activate a user (admin only).
     */
    void activateUser(UUID id);
}
