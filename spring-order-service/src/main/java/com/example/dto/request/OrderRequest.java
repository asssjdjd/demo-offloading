package com.example.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    private Long userId;
    private List<OrderItemRequest> items;
}
