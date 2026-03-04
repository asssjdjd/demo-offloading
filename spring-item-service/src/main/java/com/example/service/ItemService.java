package com.example.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.model.Item;
import com.example.repository.ItemRepository;

/**
 * ItemService - Xử lý business logic liên quan đến Item
 * 
 * Chịu trách nhiệm:
 * - Validation dữ liệu
 * - Gọi repository để lấy/lưu dữ liệu
 * - Xử lý logic nghiệp vụ
 */
@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    /**
     * Lấy tất cả item
     */
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    /**
     * Lấy item theo ID
     */
    public Optional<Item> getItemById(Integer id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return itemRepository.findById(id);
    }

    /**
     * Tạo item mới với validation
     */
    public Item createItem(Item item) throws Exception {
        // Validation
        validateItem(item);
        
        // Kiểm tra SKU không trùng lặp
        if (itemRepository.existBySku(item.getSku())) {
            throw new Exception("SKU '" + item.getSku() + "' đã tồn tại trong hệ thống");
        }
        
        // Set status mặc định nếu không có
        if (item.getStatus() == null || item.getStatus().isEmpty()) {
            item.setStatus("ACTIVE");
        }
        
        return itemRepository.save(item);
    }

    /**
     * Cập nhật item với validation
     */
    public Optional<Item> updateItem(Integer id, Item itemDetails) throws Exception {
        if (id == null || id <= 0) {
            throw new Exception("ID không hợp lệ");
        }
        
        Optional<Item> existingItem = itemRepository.findById(id);
        if (!existingItem.isPresent()) {
            return Optional.empty();
        }
        
        // Kiểm tra SKU nếu có thay đổi
        if (itemDetails.getSku() != null && !itemDetails.getSku().equals(existingItem.get().getSku())) {
            if (itemRepository.existsBySkuExcludingId(itemDetails.getSku(), id)) {
                throw new Exception("SKU '" + itemDetails.getSku() + "' đã tồn tại trong hệ thống");
            }
        }
        
        return itemRepository.update(id, itemDetails);
    }

    /**
     * Xóa item
     */
    public boolean deleteItem(Integer id) throws Exception {
        if (id == null || id <= 0) {
            throw new Exception("ID không hợp lệ");
        }
        
        if (!itemRepository.findById(id).isPresent()) {
            throw new Exception("Item với ID " + id + " không tồn tại");
        }
        
        return itemRepository.deleteById(id);
    }

    /**
     * Tìm item theo category
     */
    public List<Item> searchByCategory(String category) throws Exception {
        if (category == null || category.trim().isEmpty()) {
            throw new Exception("Category không được để trống");
        }
        return itemRepository.findByCategory(category);
    }

    /**
     * Tìm item theo phạm vi giá
     */
    public List<Item> searchByPriceRange(Double minPrice, Double maxPrice) throws Exception {
        if (minPrice == null || maxPrice == null) {
            throw new Exception("minPrice và maxPrice là bắt buộc");
        }
        
        if (minPrice < 0 || maxPrice < 0) {
            throw new Exception("Giá không được âm");
        }
        
        if (minPrice > maxPrice) {
            throw new Exception("minPrice không được lớn hơn maxPrice");
        }
        
        return itemRepository.findByPriceRange(minPrice, maxPrice);
    }

    /**
     * Tìm item theo tên
     */
    public List<Item> searchByName(String name) throws Exception {
        if (name == null || name.trim().isEmpty()) {
            throw new Exception("Tên không được để trống");
        }
        return itemRepository.findByNameContaining(name);
    }

    /**
     * Validation dữ liệu item
     */
    private void validateItem(Item item) throws Exception {
        // Kiểm tra tên
        if (item.getName() == null || item.getName().trim().isEmpty()) {
            throw new Exception("Tên sản phẩm là bắt buộc");
        }
        if (item.getName().length() > 255) {
            throw new Exception("Tên sản phẩm không được vượt quá 255 ký tự");
        }
        
        // Kiểm tra giá
        if (item.getPrice() == null) {
            throw new Exception("Giá sản phẩm là bắt buộc");
        }
        if (item.getPrice() <= 0) {
            throw new Exception("Giá sản phẩm phải lớn hơn 0");
        }
        
        // Kiểm tra số lượng
        if (item.getQuantity() == null) {
            throw new Exception("Số lượng là bắt buộc");
        }
        if (item.getQuantity() < 0) {
            throw new Exception("Số lượng không được âm");
        }
        
        // Kiểm tra category
        if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
            throw new Exception("Category là bắt buộc");
        }
        
        // Kiểm tra SKU
        if (item.getSku() == null || item.getSku().trim().isEmpty()) {
            throw new Exception("SKU là bắt buộc");
        }
        
        // Kiểm tra mô tả
        if (item.getDescription() != null && item.getDescription().length() > 1000) {
            throw new Exception("Mô tả không được vượt quá 1000 ký tự");
        }
    }

    /**
     * Lấy tổng số item
     */
    public long getTotalItems() {
        return itemRepository.count();
    }

    /**
     * Kiểm tra item có tồn tại không
     */
    public boolean itemExists(Integer id) {
        return itemRepository.findById(id).isPresent();
    }
}
