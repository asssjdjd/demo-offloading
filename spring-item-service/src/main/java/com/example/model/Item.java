package com.example.model;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;

/**
 * Item Entity - Đại diện cho một sản phẩm trong hệ thống
 */
public class Item {

    /**
     * ID duy nhất của sản phẩm
     */
    @SerializedName("id")
    private Integer id;

    /**
     * Tên sản phẩm (bắt buộc)
     */
    @SerializedName("name")
    private String name;

    /**
     * Mô tả chi tiết về sản phẩm
     */
    @SerializedName("description")
    private String description;

    /**
     * Giá bán của sản phẩm (bắt buộc)
     */
    @SerializedName("price")
    private Double price;

    /**
     * Số lượng sản phẩm có sẵn
     */
    @SerializedName("quantity")
    private Integer quantity;

    /**
     * Danh mục/Loại sản phẩm
     */
    @SerializedName("category")
    private String category;

    /**
     * SKU (Stock Keeping Unit) - Mã định danh sản phẩm duy nhất
     */
    @SerializedName("sku")
    private String sku;

    /**
     * Trạng thái: ACTIVE, INACTIVE, DISCONTINUED
     */
    @SerializedName("status")
    private String status;

    /**
     * Thời gian tạo (auto-generated)
     */
    @SerializedName("createdAt")
    private LocalDateTime createdAt;

    /**
     * Thời gian cập nhật cuối cùng (auto-generated)
     */
    @SerializedName("updatedAt")
    private LocalDateTime updatedAt;

    // ============== Constructors ==============

    /**
     * Constructor rỗng (mặc định)
     */
    public Item() {
    }

    /**
     * Constructor đầy đủ
     */
    public Item(Integer id, String name, String description, Double price,
                Integer quantity, String category, String sku, String status,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
        this.sku = sku;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ============== Getters and Setters ==============

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ============== toString ==============

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", category='" + category + '\'' +
                ", sku='" + sku + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
