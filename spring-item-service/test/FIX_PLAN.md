# Fix Plan - Item Service Test Failures

**Ngày tạo**: March 4, 2026  
**Trạng thái**: In Progress

---

## 📊 Test Results Summary

| Metric           | Value |
| ---------------- | ----- |
| **Total Tests**  | 19    |
| **Passed**       | 16 ✅ |
| **Failed**       | 3 ❌  |
| **Success Rate** | 84.2% |

---

## ❌ Failed Test Cases Analysis

### **TC-002: Lấy chi tiết sản phẩm (Valid ID)**

**Endpoint**: `GET /api/items/1`

**Expected**: HTTP 200 với item data  
**Actual**: HTTP 404 Not Found

**Root Cause**:

- Item với ID=1 không tồn tại trong database
- items.json không được load đúng cách
- Response từ TC-001 cho thấy database rỗng: `{"value":[],"Count":0}`

**Impact**: HIGH - Ảnh hưởng đến CRUD operations cơ bản

---

### **TC-007: Tạo sản phẩm (Duplicate SKU)**

**Endpoint**: `POST /api/items`  
**Body**: Item với SKU="DELL-XPS-13" (đã tồn tại trong sample data)

**Expected**: HTTP 409 Conflict  
**Actual**: HTTP 201 Created

**Root Cause**:

- Validation SKU trùng lặp không hoạt động
- Method `existBySku()` trong ItemRepository không tìm thấy SKU vì database rỗng
- Hoặc logic kiểm tra SKU có vấn đề

**Impact**: MEDIUM - Cho phép tạo items với SKU trùng lặp

---

### **TC-009: Cập nhật sản phẩm (Valid Data)**

**Endpoint**: `PUT /api/items/1`

**Expected**: HTTP 200 với updated item  
**Actual**: HTTP 404 Not Found

**Root Cause**:

- Item với ID=1 không tồn tại (giống TC-002)
- Liên quan đến vấn đề load items.json

**Impact**: HIGH - Không thể test update functionality

---

## 🔍 Root Cause Analysis

### **Primary Issue: items.json Not Loaded**

**Problem**:

```java
private static final String JSON_FILE_PATH = "src/main/resources/items.json";
```

Đường dẫn này chỉ hoạt động khi chạy từ IDE hoặc trong development environment. Khi chạy JAR file (`java -jar`), đường dẫn này không đúng vì:

1. Resources được đóng gói trong JAR file
2. Cần sử dụng ClassLoader hoặc ResourceLoader để load file từ classpath

**Evidence**:

- TC-001 trả về empty array `[]`
- TC-018 (stats) trả về `totalItems: 0`
- Tất cả search queries trả về empty results

### **Secondary Issue: File I/O khi chạy từ JAR**

Khi application chạy từ JAR file:

- File trong `src/main/resources/` được đóng gói vào JAR
- Không thể dùng `Files.readAllBytes(Paths.get(...))` với relative path
- Phải dùng `ClassPathResource` hoặc `getResourceAsStream()`

---

## 🛠️ Fix Strategy

### **Fix #1: Sửa ItemRepository để load items.json từ classpath**

**Current Code**:

```java
private static final String JSON_FILE_PATH = "src/main/resources/items.json";

public List<Item> findAll() {
    String content = new String(Files.readAllBytes(Paths.get(JSON_FILE_PATH)));
    // ...
}
```

**Fixed Code**:

```java
private static final String JSON_FILE_PATH = "items.json";

public List<Item> findAll() {
    try {
        // Load từ classpath thay vì file system
        InputStream inputStream = getClass().getClassLoader()
            .getResourceAsStream(JSON_FILE_PATH);

        if (inputStream == null) {
            System.err.println("Cannot find " + JSON_FILE_PATH);
            return new ArrayList<>();
        }

        String content = new String(inputStream.readAllBytes());
        // ... parse JSON
    }
    // ...
}
```

**Changes**:

1. ✅ Đổi path từ `src/main/resources/items.json` → `items.json`
2. ✅ Sử dụng `ClassLoader.getResourceAsStream()` thay vì `Files.readAllBytes()`
3. ✅ Handle null case khi file không tìm thấy

**Expected Outcome**: items.json sẽ được load thành công khi chạy từ JAR

---

### **Fix #2: Cân nhắc về Write Operations**

**Problem**:

- Khi load từ classpath (trong JAR), không thể write trực tiếp vào file
- Methods `save()`, `update()`, `delete()` sẽ không hoạt động với JAR file

**Options**:

#### **Option A: In-Memory Storage (Recommended cho demo)**

- Load items.json vào memory khi khởi động
- Lưu trữ data trong memory (List/Map)
- Tất cả operations (CRUD) chỉ thực hiện trong memory
- Data reset khi restart service

**Pros**:

- ✅ Đơn giản, phù hợp cho demo
- ✅ Hoạt động với JAR file
- ✅ Nhanh

**Cons**:

- ❌ Data không persist
- ❌ Reset khi restart

#### **Option B: Write to External File**

- Load từ classpath khi first run
- Write operations ghi vào file ngoài JAR (e.g., `./data/items.json`)
- Subsequent reads từ external file nếu tồn tại

**Pros**:

- ✅ Data persist

**Cons**:

- ❌ Phức tạp hơn
- ❌ Cần handle file paths

#### **Option C: Use Real Database (H2/PostgreSQL)**

- Migrate sang database thực
- JPA/Hibernate entities

**Pros**:

- ✅ Production-ready
- ✅ Full ACID support

**Cons**:

- ❌ Overkill cho demo với JSON
- ❌ Thay đổi architecture

**Decision**: Chọn **Option A** - In-Memory Storage cho demo này

---

### **Fix #3: Refactor ItemRepository với In-Memory Storage**

**Architecture**:

```
ItemRepository
├─ Load items.json once at startup (from classpath)
├─ Store in private List<Item> itemsCache
└─ All CRUD operations on itemsCache
```

**Implementation Plan**:

1. **Add cache field**:

```java
private List<Item> itemsCache = new ArrayList<>();
private boolean initialized = false;
```

2. **Initialize cache từ JSON**:

```java
@PostConstruct
public void init() {
    itemsCache = loadItemsFromJson();
    initialized = true;
}
```

3. **Update all methods** để sử dụng `itemsCache` thay vì đọc file mỗi lần

---

## 📝 Implementation Checklist

### **Phase 1: Fix ItemRepository**

- [ ] Thêm field `List<Item> itemsCache`
- [ ] Tạo method `loadItemsFromJson()` sử dụng ClassLoader
- [ ] Thêm `@PostConstruct init()` method
- [ ] Refactor `findAll()` để return từ cache
- [ ] Refactor `findById()` để search trong cache
- [ ] Refactor `save()` để add vào cache
- [ ] Refactor `update()` để modify cache
- [ ] Refactor `deleteById()` để remove từ cache
- [ ] Refactor search methods (category, price, name)
- [ ] Update `existBySku()` và `existsBySkuExcludingId()`

### **Phase 2: Test & Verify**

- [ ] Rebuild project: `mvn clean package`
- [ ] Restart service: `java -jar target/*.jar`
- [ ] Verify items loaded: `curl http://localhost:8082/api/items`
- [ ] Run test suite: `.\test-item-service.ps1`
- [ ] Verify TC-002 passes (GET /items/1 returns 200)
- [ ] Verify TC-007 passes (Duplicate SKU returns 409)
- [ ] Verify TC-009 passes (PUT /items/1 returns 200)

### **Phase 3: Documentation**

- [ ] Update README.md về in-memory storage
- [ ] Add note về data reset khi restart
- [ ] Update TESTING.md với expected behavior

---

## 🎯 Expected Results After Fix

| Test Case        | Current Status | Expected After Fix      |
| ---------------- | -------------- | ----------------------- |
| TC-002           | ❌ 404         | ✅ 200 (Item found)     |
| TC-007           | ❌ 201         | ✅ 409 (Duplicate SKU)  |
| TC-009           | ❌ 404         | ✅ 200 (Update success) |
| **Success Rate** | 84.2%          | **100%** 🎉             |

---

## 📌 Additional Improvements (Optional)

### **Improvement 1: Add Logging**

```java
private static final Logger logger = LoggerFactory.getLogger(ItemRepository.class);

@PostConstruct
public void init() {
    itemsCache = loadItemsFromJson();
    logger.info("Loaded {} items from items.json", itemsCache.size());
}
```

### **Improvement 2: Thread Safety**

- Nếu cần thread-safe operations, sử dụng `ConcurrentHashMap` hoặc `synchronized`
- Cho demo single-instance không cần thiết

### **Improvement 3: Validation Enhancement**

- Add more validation rules
- Custom Exception classes (ItemNotFoundException, DuplicateSkuException)

---

## ⚠️ Known Limitations After Fix

1. **Data persistence**: Data sẽ reset khi restart service
2. **Concurrency**: Không handle concurrent modifications (OK cho demo)
3. **Scale**: Không phù hợp cho production với large dataset
4. **Backup**: Không có backup mechanism

**Note**: Đây là acceptable trade-offs cho demo purpose. Để production, nên migrate sang real database.

---

## 🚀 Execution Plan

### **Step 1: Backup Current Code**

```bash
git commit -am "Before fixing ItemRepository"
```

### **Step 2: Implement Fixes**

- Modify ItemRepository.java
- Test locally

### **Step 3: Build & Deploy**

```bash
mvn clean package
java -jar target/spring-item-service-0.0.1-SNAPSHOT.jar
```

### **Step 4: Run Tests**

```bash
.\test-item-service.ps1
```

### **Step 5: Verify Results**

- Check test-summary.log
- Ensure 19/19 tests pass

---

## 📊 Timeline

| Task             | Estimated Time | Status         |
| ---------------- | -------------- | -------------- |
| Analysis         | 15 minutes     | ✅ Done        |
| Create Fix Plan  | 10 minutes     | ✅ Done        |
| Implement Fix #1 | 20 minutes     | 🔄 In Progress |
| Test & Debug     | 15 minutes     | ⏳ Pending     |
| Documentation    | 10 minutes     | ⏳ Pending     |
| **Total**        | **70 minutes** |                |

---

## ✅ Success Criteria

Fix được coi là thành công khi:

1. ✅ All 19 test cases pass (100% success rate)
2. ✅ GET /api/items trả về 5 items (từ items.json)
3. ✅ GET /api/items/1 trả về Laptop Dell XPS 13
4. ✅ Duplicate SKU validation hoạt động (409 response)
5. ✅ CRUD operations hoạt động đầy đủ
6. ✅ No errors trong logs
7. ✅ Service khởi động thành công từ JAR file

---

**Status**: 🔄 Ready to implement  
**Priority**: HIGH  
**Assigned**: Development Team  
**Target Completion**: March 4, 2026 - 17:30
