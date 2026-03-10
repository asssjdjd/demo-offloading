package com.example.controller;

import com.example.dto.request.OrderRequest;
import com.example.dto.request.OrderStatusUpdateRequest;
import com.example.dto.response.OrderResponse;
import com.example.dto.response.OrderSummaryResponse;
import com.example.gateway.GatewayHeaderExtractor;
import com.example.gateway.GatewayIdentity;
import com.example.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final GatewayHeaderExtractor headerExtractor;

    /**
     * API Tạo đơn hàng
     * OrderRequest bao gồm các thông tin của đơn hàng không cần gọi đến các service khác
     * userId,ItemId,Total,...
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request,HttpServletRequest httpRequest) {
        GatewayIdentity identity = headerExtractor.extract(httpRequest);

        // 2. Kiểm tra xác thực (Authentication)
        if (identity.getUserId() == null) {
            log.warn("POST /api/orders - Unauthorized attempt to create order");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 3. Ghi log truy vết
        log.info("POST /api/orders - creating order requested by userId={}, role={}",
                identity.getUserId(), identity.getUserRole());

        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 1. API Lấy danh sách
    @GetMapping
    public ResponseEntity<List<OrderSummaryResponse>> getAllOrders(HttpServletRequest request) {
        GatewayIdentity identity = headerExtractor.extract(request);

        // xác thực
        if (identity.getUserId() == null) {
            log.warn("GET /api/orders - Unauthorized access attempt without User ID");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("GET /api/orders - requested by userId={}, role={}",
                identity.getUserId(), identity.getUserRole());
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // 2. API Xem chi tiết
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id,HttpServletRequest httpRequest) {
        GatewayIdentity identity = headerExtractor.extract(httpRequest);

        // Xác thực
        if (identity.getUserId() == null) {
            log.warn("GET /api/orders/{} - Unauthorized access attempt", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("GET /api/orders/{} - requested by userId={}, role={}",
                id, identity.getUserId(), identity.getUserRole());

        try {
            // TRUYỀN IDENTITY XUỐNG SERVICE ĐỂ PHÂN QUYỀN
            return ResponseEntity.ok(orderService.getOrderById(id));
        } catch (RuntimeException e) {
            // Phân biệt lỗi 403 (Cấm) và 404 (Không tìm thấy)
            if (e.getMessage().contains("Quyền")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 3. API Cập nhật trạng thái
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody OrderStatusUpdateRequest request,
            HttpServletRequest httpRequest) {

        GatewayIdentity identity = headerExtractor.extract(httpRequest);

        if (identity.getUserId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (identity.getUserRole() == null || !identity.getUserRole().toUpperCase().contains("ADMIN")) {
            log.warn("PATCH /api/orders/{}/status - Forbidden: User {} attempted to update status",
                    id, identity.getUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Lỗi Quyền: Chỉ Admin mới có quyền cập nhật trạng thái đơn hàng.");
        }

        log.info("PATCH /api/orders/{}/status - requested by ADMIN userId={}", id, identity.getUserId());

        try {
            orderService.updateOrderStatus(id, request.getStatus());
            return ResponseEntity.ok("Cập nhật trạng thái thành công.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
