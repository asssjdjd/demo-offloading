package com.example.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItemDTO {
    private Long id;
    private BigDecimal price;
    private Integer stockQuantity;
}
