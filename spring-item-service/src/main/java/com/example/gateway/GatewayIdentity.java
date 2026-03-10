package com.example.gateway;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class GatewayIdentity {

    private String userId;
    private String userRole;
    private String username;

    public GatewayIdentity() {
    }

    public GatewayIdentity(String userId, String userRole, String username) {
        this.userId = userId;
        this.userRole = userRole;
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}