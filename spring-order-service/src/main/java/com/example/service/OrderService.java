package com.example.service;

import com.example.dto.OrderItemDto;
import com.example.dto.request.OrderItemRequest;
import com.example.dto.request.OrderRequest;
import com.example.dto.response.OrderDetailResponse;
import com.example.dto.response.OrderResponse;
import com.example.dto.response.OrderSummaryResponse;
import com.example.model.Order;
import com.example.model.OrderItem;
import com.example.model.OrderStatus;
import com.example.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
//    private final RestTemplate restTemplate;
//
//    private final String ITEM_SERVICE_URL = "http://localhost:8081/api/items";
//    private final String USER_SERVICE_URL = "http://localhost:8082/api/users";

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // Khởi tạo
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for(OrderItemRequest itemReq : request.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setItemId(itemReq.getItemId());
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setPrice(itemReq.getPrice());

            orderItem.setOrder(order);
            // Tính tổng tiền
            BigDecimal itemTotal = itemReq.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalPrice = totalPrice.add(itemTotal);
            totalAmount = totalAmount.add(BigDecimal.valueOf(orderItem.getQuantity()));
            orderItems.add(orderItem);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalPrice(totalPrice);

        Order savedOrder = orderRepository.save(order);
        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getStatus().name(),
                "Tạo đơn hàng thành công"
        );
    }


    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getAllOrders() {
        return orderRepository.findAll().stream().map(order -> {
            OrderSummaryResponse summary = new OrderSummaryResponse();
            summary.setId(order.getId());
            summary.setUserId(order.getUserId());
            summary.setTotalAmount(order.getTotalAmount());
            summary.setStatus(order.getStatus().name());
            summary.setCreatedAt(order.getCreatedAt());
            return summary;
        }).collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));

        OrderDetailResponse response = new OrderDetailResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus().name());
        response.setCreatedAt(order.getCreatedAt());

        // Map danh sách items
        List<OrderItemDto> itemDtos = order.getItems().stream().map(item -> {
            OrderItemDto itemDto = new OrderItemDto();
            itemDto.setItemId(item.getItemId());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setPrice(item.getPrice());
            return itemDto;
        }).collect(Collectors.toList());

        response.setItems(itemDtos);
        return response;
    }

    // 3. Cập nhật trạng thái
    @Transactional
    public void updateOrderStatus(Long id, String newStatus) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));

        try {
            OrderStatus statusEnum = OrderStatus.valueOf(newStatus.toUpperCase());
            order.setStatus(statusEnum);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + newStatus);
        }
    }


}
