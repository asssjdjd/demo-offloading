package com.example.dto.response;

import lombok.Data;

@Data
public class OrderResponse {
    private Long orderId;
    private String status;
    private String message;

    public OrderResponse(Long orderId, String status, String message) {
        this.orderId = orderId;
        this.status = status;
        this.message = message;
    }
}
