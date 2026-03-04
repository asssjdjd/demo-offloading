package com.example.repository;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.example.model.Item;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;

import jakarta.annotation.PostConstruct;

/**
 * ItemRepository - Xử lý dữ liệu Item với In-Memory Storage
 * 
 * Load items.json từ classpath khi khởi động và lưu trong memory.
 * Tất cả CRUD operations thực hiện trên in-memory cache.
 * 
 * Note: Data sẽ reset khi restart service (phù hợp cho demo).
 */
@Repository
public class ItemRepository {

    private static final String JSON_FILE_PATH = "items.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    // Gson với custom LocalDateTime adapter để tránh Java module access issues
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, 
                (JsonDeserializer<LocalDateTime>) (json, type, context) -> 
                    LocalDateTime.parse(json.getAsString(), DATE_FORMATTER))
            .registerTypeAdapter(LocalDateTime.class,
                (JsonSerializer<LocalDateTime>) (src, type, context) ->
                    context.serialize(src.format(DATE_FORMATTER)))
            .create();
    
    // In-memory cache cho items
    private List<Item> itemsCache = new ArrayList<>();
    private boolean initialized = false;

    /**
     * Initialize repository - Load items từ JSON vào memory
     */
    @PostConstruct
    public void init() {
        System.out.println("Initializing ItemRepository...");
        itemsCache = loadItemsFromJson();
        initialized = true;
        System.out.println("✅ Loaded " + itemsCache.size() + " items from items.json");
    }

    /**
     * Load items từ JSON file trong classpath
     */
    private List<Item> loadItemsFromJson() {
        try {
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream(JSON_FILE_PATH);
            
            if (inputStream == null) {
                System.err.println("❌ Cannot find " + JSON_FILE_PATH + " in classpath");
                return new ArrayList<>();
            }
            
            String content = new String(inputStream.readAllBytes());
            JsonObject jsonObject = gson.fromJson(content, JsonObject.class);
            JsonArray itemsArray = jsonObject.getAsJsonArray("items");
            
            List<Item> items = new ArrayList<>();
            for (int i = 0; i < itemsArray.size(); i++) {
                Item item = gson.fromJson(itemsArray.get(i), Item.class);
                items.add(item);
            }
            
            inputStream.close();
            return items;
        } catch (IOException e) {
            System.err.println("❌ Error reading items.json: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Lấy tất cả các item từ cache
     */
    public List<Item> findAll() {
        return new ArrayList<>(itemsCache); // Return copy để avoid modification
    }

    /**
     * Lấy item theo ID từ cache
     */
    public Optional<Item> findById(Integer id) {
        return itemsCache.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
    }

    /**
     * Tạo item mới và thêm vào cache
     */
    public Item save(Item item) {
        try {
            // Tạo ID mới (auto-increment)
            Integer maxId = itemsCache.stream()
                    .map(Item::getId)
                    .max(Integer::compareTo)
                    .orElse(0);
            item.setId(maxId + 1);
            
            // Set timestamps
            if (item.getCreatedAt() == null) {
                item.setCreatedAt(LocalDateTime.now());
            }
            item.setUpdatedAt(LocalDateTime.now());
            
            // Add to cache
            itemsCache.add(item);
            
            System.out.println("✅ Created item with ID: " + item.getId());
            return item;
        } catch (Exception e) {
            System.err.println("❌ Error saving item: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cập nhật item trong cache
     */
    public Optional<Item> update(Integer id, Item updatedItem) {
        try {
            Optional<Item> existingItem = itemsCache.stream()
                    .filter(item -> item.getId().equals(id))
                    .findFirst();
            
            if (existingItem.isPresent()) {
                Item item = existingItem.get();
                
                // Cập nhật các trường nếu có giá trị mới
                if (updatedItem.getName() != null) {
                    item.setName(updatedItem.getName());
                }
                if (updatedItem.getDescription() != null) {
                    item.setDescription(updatedItem.getDescription());
                }
                if (updatedItem.getPrice() != null) {
                    item.setPrice(updatedItem.getPrice());
                }
                if (updatedItem.getQuantity() != null) {
                    item.setQuantity(updatedItem.getQuantity());
                }
                if (updatedItem.getCategory() != null) {
                    item.setCategory(updatedItem.getCategory());
                }
                if (updatedItem.getStatus() != null) {
                    item.setStatus(updatedItem.getStatus());
                }
                
                item.setUpdatedAt(LocalDateTime.now());
                
                System.out.println("✅ Updated item with ID: " + id);
                return Optional.of(item);
            }
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("❌ Error updating item: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Xóa item từ cache theo ID
     */
    public boolean deleteById(Integer id) {
        try {
            boolean removed = itemsCache.removeIf(item -> item.getId().equals(id));
            
            if (removed) {
                System.out.println("✅ Deleted item with ID: " + id);
            }
            
            return removed;
        } catch (Exception e) {
            System.err.println("❌ Error deleting item: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tìm item theo category từ cache
     */
    public List<Item> findByCategory(String category) {
        return itemsCache.stream()
                .filter(item -> item.getCategory() != null && 
                        item.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    /**
     * Tìm item theo phạm vi giá từ cache
     */
    public List<Item> findByPriceRange(Double minPrice, Double maxPrice) {
        return itemsCache.stream()
                .filter(item -> item.getPrice() != null &&
                        item.getPrice() >= minPrice &&
                        item.getPrice() <= maxPrice)
                .collect(Collectors.toList());
    }

    /**
     * Tìm item theo tên (partial match) từ cache
     */
    public List<Item> findByNameContaining(String name) {
        return itemsCache.stream()
                .filter(item -> item.getName() != null &&
                        item.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra SKU đã tồn tại hay chưa trong cache
     */
    public boolean existBySku(String sku) {
        return itemsCache.stream()
                .anyMatch(item -> item.getSku() != null &&
                        item.getSku().equalsIgnoreCase(sku));
    }

    /**
     * Kiểm tra SKU đã tồn tại (không tính item hiện tại) trong cache
     */
    public boolean existsBySkuExcludingId(String sku, Integer excludeId) {
        return itemsCache.stream()
                .filter(item -> !item.getId().equals(excludeId))
                .anyMatch(item -> item.getSku() != null &&
                        item.getSku().equalsIgnoreCase(sku));
    }

    /**
     * Lấy tổng số item trong cache
     */
    public long count() {
        return itemsCache.size();
    }
}
