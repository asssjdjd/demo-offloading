package com.example.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDto {
    private Long itemId;
    private Integer quantity;
    private BigDecimal price;
}
