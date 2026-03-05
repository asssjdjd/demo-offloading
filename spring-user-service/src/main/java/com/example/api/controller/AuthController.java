package com.example.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Auth Controller - Public endpoints (no Kong authentication required).
 * 
 * In a real-world scenario, this would handle login, registration, etc.
 * The JWT token would be issued here and subsequent requests would be
 * validated by Kong.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * POST /api/v1/auth/health
     * Public health check for the auth endpoint.
     */
    @PostMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "user-service",
                "message", "Auth endpoint is accessible (public route - no JWT required)"
        ));
    }

    /**
     * GET /api/v1/auth/info
     * Returns info about the authentication mechanism.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> authInfo() {
        return ResponseEntity.ok(Map.of(
                "authentication", "JWT via Kong API Gateway",
                "pattern", "Gateway Offloading",
                "description", "Kong handles JWT validation, ACL authorization, and header injection. " +
                        "Backend service reads identity from X-User-* headers.",
                "headers", Map.of(
                        "X-User-Id", "User's unique identifier (from JWT sub claim)",
                        "X-User-Role", "User's role (from JWT role claim)",
                        "X-User-Username", "Username (from JWT username claim)",
                        "X-User-Email", "Email (from JWT email claim)",
                        "X-Gateway-Auth", "Gateway trust verification header"
                )
        ));
    }
}
