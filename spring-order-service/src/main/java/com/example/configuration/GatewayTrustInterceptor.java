package com.example.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

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

    private final String trustedHeader = "X-Gateway-Auth";
    private final String trustedValue = "kong-verified";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String gatewayAuth = request.getHeader(trustedHeader);
        // Nếu không có header do Kong đóng mộc, đá văng ngay lập tức
        if (gatewayAuth == null || !gatewayAuth.equals(trustedValue)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        return true;
    }
}
