package com.example.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.application.dto.CreateUserRequest;
import com.example.application.dto.UpdateUserRequest;
import com.example.application.dto.UserResponse;
import com.example.application.gateway.GatewayHeaderExtractor;
import com.example.application.gateway.GatewayIdentity;
import com.example.application.service.UserService;

import java.util.List;
import java.util.UUID;

/**
 * User REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final GatewayHeaderExtractor headerExtractor;

    /**
     * GET /api/v1/users/me
     * 
     * Returns the profile of the currently authenticated user.
     * Identity is extracted from Kong-injected headers, NOT from JWT parsing.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(HttpServletRequest request) {

        GatewayIdentity identity = headerExtractor.extract(request);
        log.info("GET /users/me - userId={}, role={}",
                identity.getUserId(), identity.getUserRole());

        UserResponse user = userService.getCurrentUser(identity);
        return ResponseEntity.ok(user);
    }

    /**
     * GET /api/v1/users/{id}
     * 
     * Get a specific user by ID. 
     * Accessible by any authenticated user (Kong ACL: user-group, admin-group).
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable UUID id,
            HttpServletRequest request) {

        GatewayIdentity identity = headerExtractor.extract(request);
        log.info("GET /users/{} - requested by userId={}", id, identity.getUserId());

        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * GET /api/v1/users
     * 
     * Get all users. Accessible by any authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(HttpServletRequest request) {
        GatewayIdentity identity = headerExtractor.extract(request);
        log.info("GET /users - requested by userId={}, role={}",
                identity.getUserId(), identity.getUserRole());

        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * POST /api/v1/users
     * 
     * Create a new user. Typically admin-only in production.
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {

        GatewayIdentity identity = headerExtractor.extract(httpRequest);
        log.info("POST /users - creating user '{}', requested by userId={}",
                request.getUsername(), identity.getUserId());

        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * PUT /api/v1/users/{id}
     * 
     * Update user profile.
     * Business rule: Users can only update their own profile. Admins can update anyone.
     * This authorization logic uses the role from Kong headers (X-User-Role).
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest) {

        // Read identity from headers — Kong already validated the token
        GatewayIdentity identity = headerExtractor.extract(httpRequest);
        log.info("PUT /users/{} - requested by userId={}, role={}",
                id, identity.getUserId(), identity.getUserRole());

        UserResponse user = userService.updateUser(id, request, identity);
        return ResponseEntity.ok(user);
    }

    /**
     * PATCH /api/v1/users/{id}/deactivate
     * 
     * Deactivate a user account. Admin only (enforced by Kong ACL + business logic).
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable UUID id,
            HttpServletRequest request) {

        GatewayIdentity identity = headerExtractor.extract(request);
        log.info("PATCH /users/{}/deactivate - requested by admin userId={}",
                id, identity.getUserId());

        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/v1/users/{id}/activate
     * 
     * Activate a user account. Admin only.
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(
            @PathVariable UUID id,
            HttpServletRequest request) {

        GatewayIdentity identity = headerExtractor.extract(request);
        log.info("PATCH /users/{}/activate - requested by admin userId={}",
                id, identity.getUserId());

        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }
}
