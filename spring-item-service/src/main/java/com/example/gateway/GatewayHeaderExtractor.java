package com.example.gateway;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import org.springframework.stereotype.Component;

@Component
@Builder
public class GatewayHeaderExtractor {
    public GatewayIdentity extract(HttpServletRequest request) {
        return GatewayIdentity.builder()
                .userId(request.getHeader("X-User-Id"))
                .userRole(request.getHeader("X-User-Role"))
                .username(request.getHeader("X-User-Username"))
                .build();
    }
}
