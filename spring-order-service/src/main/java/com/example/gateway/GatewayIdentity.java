package com.example.gateway;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GatewayIdentity {
    private String userId;
    private String userRole;
    private String username;
}
