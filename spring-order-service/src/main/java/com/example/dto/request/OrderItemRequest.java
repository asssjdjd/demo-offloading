package com.example.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemRequest {
    private Long itemId;
    private Integer quantity;
    private BigDecimal price;
}