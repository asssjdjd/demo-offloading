package com.example.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.application.dto.UserResponse;
import com.example.application.gateway.GatewayHeaderExtractor;
import com.example.application.gateway.GatewayIdentity;
import com.example.application.service.UserService;
import com.example.domain.exception.ForbiddenAccessException;

import java.util.List;
import java.util.UUID;

/**
 * Admin REST Controller
 * 
 * Routes under /api/v1/admin/* are protected by Kong ACL (admin-group only).
 * Kong ensures only admin tokens can reach these endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final GatewayHeaderExtractor headerExtractor;

    /**
     * GET /api/v1/admin/users
     * 
     * List all users — admin only.
     * Kong ACL already restricts access to admin-group.
     * We add a secondary check here as defense-in-depth.
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listAllUsers(HttpServletRequest request) {
        GatewayIdentity identity = headerExtractor.extract(request);

        // Defense-in-depth: verify admin role even though Kong ACL already checks
        if (!identity.isAdmin()) {
            throw new ForbiddenAccessException("Admin access required");
        }

        log.info("ADMIN GET /admin/users - by admin userId={}", identity.getUserId());

        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * PATCH /api/v1/admin/users/{id}/deactivate
     */
    @PatchMapping("/users/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable UUID id,
            HttpServletRequest request) {

        GatewayIdentity identity = headerExtractor.extract(request);
        if (!identity.isAdmin()) {
            throw new ForbiddenAccessException("Admin access required");
        }

        log.info("ADMIN PATCH /admin/users/{}/deactivate - by admin userId={}", id, identity.getUserId());

        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/v1/admin/users/{id}/activate
     */
    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<Void> activateUser(
            @PathVariable UUID id,
            HttpServletRequest request) {

        GatewayIdentity identity = headerExtractor.extract(request);
        if (!identity.isAdmin()) {
            throw new ForbiddenAccessException("Admin access required");
        }

        log.info("ADMIN PATCH /admin/users/{}/activate - by admin userId={}", id, identity.getUserId());

        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }
}
