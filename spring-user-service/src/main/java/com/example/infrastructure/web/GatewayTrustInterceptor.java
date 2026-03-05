package com.example.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.example.application.gateway.GatewayHeaderExtractor;

/**
 * Gateway Trust Interceptor
 * 
 * Ensures that incoming requests were routed through the trusted Kong gateway.
 * This is a lightweight security check - NOT full JWT validation.
 * 
 * The interceptor checks for the presence of the X-Gateway-Auth header
 * that Kong injects after successful authentication.
 * 
 * If the header is missing or invalid, the request is rejected with 401.
 * This prevents direct access to the service bypassing Kong.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayTrustInterceptor implements HandlerInterceptor {

    private final GatewayHeaderExtractor headerExtractor;

    @Value("${gateway.offloading.enabled:true}")
    private boolean gatewayOffloadingEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // Skip check if gateway offloading is disabled (e.g., local development)
        if (!gatewayOffloadingEnabled) {
            log.debug("Gateway offloading is disabled, allowing request through");
            return true;
        }

        // Skip health check endpoints
        String requestUri = request.getRequestURI();
        if (requestUri.startsWith("/actuator")) {
            return true;
        }

        // Verify the request came through the trusted gateway
        if (!headerExtractor.isTrustedGatewayRequest(request)) {
            log.warn("Untrusted request blocked: {} {} from {}",
                    request.getMethod(), requestUri, request.getRemoteAddr());

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                    "status": 401,
                    "error": "Unauthorized",
                    "message": "Request must be routed through the API Gateway"
                }
                """);
            return false;
        }

        log.debug("Gateway trust verified for: {} {}", request.getMethod(), requestUri);
        return true;
    }
}
