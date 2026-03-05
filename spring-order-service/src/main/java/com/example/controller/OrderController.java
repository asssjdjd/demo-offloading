package com.example.controller;

import com.example.dto.request.OrderRequest;
import com.example.dto.request.OrderStatusUpdateRequest;
import com.example.dto.response.OrderResponse;
import com.example.dto.response.OrderSummaryResponse;
import com.example.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * API Tạo đơn hàng
     * OrderRequest bao gồm các thông tin của đơn hàng không cần gọi đến các service khác
     * userId,ItemId,Total,...
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 1. API Lấy danh sách
    @GetMapping
    public ResponseEntity<List<OrderSummaryResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // 2. API Xem chi tiết
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(orderService.getOrderById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // 3. API Cập nhật trạng thái
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody OrderStatusUpdateRequest request) {
        try {
            orderService.updateOrderStatus(id, request.getStatus());
            return ResponseEntity.ok("Cập nhật trạng thái thành công.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
