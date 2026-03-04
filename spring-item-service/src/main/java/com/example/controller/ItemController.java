package com.example.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.model.Item;
import com.example.service.ItemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * ItemController - REST Controller để xử lý Item APIs
 * 
 * Base URL: /api/items
 */
@RestController
@RequestMapping("/api/items")
@Tag(name = "Item Service", description = "APIs quản lý sản phẩm (Items)")
public class ItemController {

    @Autowired
    private ItemService itemService;

    /**
     * Lấy danh sách tất cả sản phẩm
     * GET /api/items
     */
    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả sản phẩm", 
               description = "Trả về danh sách tất cả các sản phẩm trong hệ thống")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
        @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    public ResponseEntity<?> getAllItems() {
        try {
            List<Item> items = itemService.getAllItems();
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi khi lấy danh sách sản phẩm", e.getMessage()));
        }
    }

    /**
     * Lấy chi tiết sản phẩm theo ID
     * GET /api/items/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết sản phẩm", 
               description = "Lấy thông tin chi tiết của một sản phẩm cụ thể")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thành công"),
        @ApiResponse(responseCode = "404", description = "Sản phẩm không tìm thấy"),
        @ApiResponse(responseCode = "400", description = "ID không hợp lệ")
    })
    public ResponseEntity<?> getItemById(
            @PathVariable @Parameter(description = "ID của sản phẩm") Integer id) {
        try {
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("ID không hợp lệ", "ID phải là số dương"));
            }
            
            Optional<Item> item = itemService.getItemById(id);
            if (item.isPresent()) {
                return ResponseEntity.ok(item.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Sản phẩm không tìm thấy", 
                              "Item với ID " + id + " không tồn tại"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi server", e.getMessage()));
        }
    }

    /**
     * Tạo sản phẩm mới
     * POST /api/items
     */
    @PostMapping
    @Operation(summary = "Tạo sản phẩm mới", 
               description = "Tạo một sản phẩm mới trong hệ thống")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tạo thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "409", description = "Xung đột (SKU đã tồn tại)")
    })
    public ResponseEntity<?> createItem(
            @RequestBody @Parameter(description = "Thông tin sản phẩm cần tạo") Item item) {
        try {
            Item createdItem = itemService.createItem(item);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
        } catch (Exception e) {
            if (e.getMessage().contains("SKU")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(createErrorResponse("SKU đã tồn tại", e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Dữ liệu không hợp lệ", e.getMessage()));
        }
    }

    /**
     * Cập nhật sản phẩm
     * PUT /api/items/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật sản phẩm", 
               description = "Cập nhật thông tin của một sản phẩm cụ thể")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
        @ApiResponse(responseCode = "404", description = "Sản phẩm không tìm thấy"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "409", description = "Xung đột (SKU đã tồn tại)")
    })
    public ResponseEntity<?> updateItem(
            @PathVariable @Parameter(description = "ID của sản phẩm") Integer id,
            @RequestBody @Parameter(description = "Thông tin cần cập nhật") Item itemDetails) {
        try {
            Optional<Item> updatedItem = itemService.updateItem(id, itemDetails);
            if (updatedItem.isPresent()) {
                return ResponseEntity.ok(updatedItem.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Sản phẩm không tìm thấy", 
                              "Item với ID " + id + " không tồn tại"));
            }
        } catch (Exception e) {
            if (e.getMessage().contains("SKU")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(createErrorResponse("SKU đã tồn tại", e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Dữ liệu không hợp lệ", e.getMessage()));
        }
    }

    /**
     * Xóa sản phẩm
     * DELETE /api/items/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa sản phẩm", 
               description = "Xóa một sản phẩm khỏi hệ thống")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Xóa thành công"),
        @ApiResponse(responseCode = "404", description = "Sản phẩm không tìm thấy"),
        @ApiResponse(responseCode = "400", description = "ID không hợp lệ")
    })
    public ResponseEntity<?> deleteItem(
            @PathVariable @Parameter(description = "ID của sản phẩm") Integer id) {
        try {
            boolean deleted = itemService.deleteItem(id);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Sản phẩm không tìm thấy", 
                              "Item với ID " + id + " không tồn tại"));
            }
        } catch (Exception e) {
            if (e.getMessage().contains("không tồn tại")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Sản phẩm không tìm thấy", e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Lỗi", e.getMessage()));
        }
    }

    /**
     * Tìm kiếm sản phẩm theo category
     * GET /api/items/search/category?category={category}
     */
    @GetMapping("/search/category")
    @Operation(summary = "Tìm kiếm sản phẩm theo category", 
               description = "Tìm tất cả sản phẩm thuộc một category cụ thể")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tìm kiếm thành công"),
        @ApiResponse(responseCode = "400", description = "Tham số không hợp lệ")
    })
    public ResponseEntity<?> searchByCategory(
            @RequestParam @Parameter(description = "Tên category") String category) {
        try {
            List<Item> items = itemService.searchByCategory(category);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Tham số không hợp lệ", e.getMessage()));
        }
    }

    /**
     * Tìm kiếm sản phẩm theo phạm vi giá
     * GET /api/items/search/price?minPrice={minPrice}&maxPrice={maxPrice}
     */
    @GetMapping("/search/price")
    @Operation(summary = "Tìm kiếm sản phẩm theo giá", 
               description = "Tìm tất cả sản phẩm trong phạm vi giá cụ thể")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tìm kiếm thành công"),
        @ApiResponse(responseCode = "400", description = "Tham số không hợp lệ")
    })
    public ResponseEntity<?> searchByPrice(
            @RequestParam @Parameter(description = "Giá tối thiểu") Double minPrice,
            @RequestParam @Parameter(description = "Giá tối đa") Double maxPrice) {
        try {
            List<Item> items = itemService.searchByPriceRange(minPrice, maxPrice);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Tham số không hợp lệ", e.getMessage()));
        }
    }

    /**
     * Tìm kiếm sản phẩm theo tên
     * GET /api/items/search/name?name={name}
     */
    @GetMapping("/search/name")
    @Operation(summary = "Tìm kiếm sản phẩm theo tên", 
               description = "Tìm sản phẩm có tên chứa từ khóa cụ thể")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tìm kiếm thành công"),
        @ApiResponse(responseCode = "400", description = "Tham số không hợp lệ")
    })
    public ResponseEntity<?> searchByName(
            @RequestParam @Parameter(description = "Từ khóa tìm kiếm") String name) {
        try {
            List<Item> items = itemService.searchByName(name);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Tham số không hợp lệ", e.getMessage()));
        }
    }

    /**
     * Lấy số liệu thống kê
     * GET /api/items/stats
     */
    @GetMapping("/stats")
    @Operation(summary = "Lấy thống kê", 
               description = "Lấy các thống kê về sản phẩm")
    @ApiResponse(responseCode = "200", description = "Lấy thành công")
    public ResponseEntity<?> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalItems", itemService.getTotalItems());
            stats.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi server", e.getMessage()));
        }
    }

    /**
     * Helper method để tạo error response
     */
    private Map<String, Object> createErrorResponse(String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }
}
