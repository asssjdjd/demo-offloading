package com.example.application.gateway;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Gateway Header Extractor
 * 
 * Extracts user identity information from HTTP headers injected by Kong.
 * This is the bridge between the Gateway Offloading Pattern and the application logic.
 * 
 * Flow:
 * 1. Client sends request with JWT token to Kong
 * 2. Kong validates JWT, extracts claims, and injects X-User-* headers
 * 3. Kong proxies request to this service with identity headers
 * 4. This extractor reads the headers and creates a GatewayIdentity object
 * 5. Controller/Service uses GatewayIdentity for business logic
 */
@Slf4j
@Component
public class GatewayHeaderExtractor {

    @Value("${gateway.offloading.trusted-header:X-Gateway-Auth}")
    private String trustedHeader;

    @Value("${gateway.offloading.trusted-value:kong-verified}")
    private String trustedValue;

    @Value("${gateway.offloading.headers.user-id:X-User-Id}")
    private String userIdHeader;

    @Value("${gateway.offloading.headers.user-role:X-User-Role}")
    private String userRoleHeader;

    @Value("${gateway.offloading.headers.user-username:X-User-Username}")
    private String usernameHeader;

    @Value("${gateway.offloading.headers.user-email:X-User-Email}")
    private String userEmailHeader;

    /**
     * Extract gateway identity from the current HTTP request headers.
     *
     * @param request the HTTP servlet request
     * @return GatewayIdentity containing the user info from Kong headers
     */
    public GatewayIdentity extract(HttpServletRequest request) {
        GatewayIdentity identity = GatewayIdentity.builder()
                .userId(request.getHeader(userIdHeader))
                .userRole(request.getHeader(userRoleHeader))
                .username(request.getHeader(usernameHeader))
                .userEmail(request.getHeader(userEmailHeader))
                .build();

        log.debug("Extracted gateway identity: userId={}, role={}, username={}",
                identity.getUserId(), identity.getUserRole(), identity.getUsername());

        return identity;
    }

    /**
     * Verify that the request was forwarded by a trusted gateway (Kong).
     * Checks for the presence of the trusted gateway header.
     *
     * @param request the HTTP servlet request
     * @return true if the request came through the trusted gateway
     */
    public boolean isTrustedGatewayRequest(HttpServletRequest request) {
        String headerValue = request.getHeader(trustedHeader);
        return trustedValue.equals(headerValue);
    }
}
