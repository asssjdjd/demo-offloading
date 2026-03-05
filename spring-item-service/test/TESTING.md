# Testing Documentation - Item Service

**Tài liệu kiểm thử cho Item Service**

## 📋 Mục Lục

- [Giới thiệu](#giới-thiệu)
- [Test Cases](#test-cases)
- [Automated Testing](#automated-testing)
- [Test Results](#test-results)
- [Log Files](#log-files)

---

## 🎯 Giới Thiệu

File này mô tả các test cases để kiểm thử Item Service, bao gồm:
- ✅ Functional Testing (Kiểm thử chức năng)
- ✅ API Testing (Kiểm thử API endpoints)
- ✅ Validation Testing (Kiểm thử validation)
- ✅ Error Handling Testing (Kiểm thử xử lý lỗi)

**Test Environment**:
- Service URL: `http://localhost:8082`
- Testing Tool: cURL, PowerShell
- Log File: `test-results.log`

---

## 📝 Test Cases

### **Test Suite 1: CRUD Operations**

#### **TC-001: Lấy danh sách tất cả sản phẩm**
**Endpoint**: `GET /api/items`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Kiểm tra khả năng lấy danh sách sản phẩm |
| **Precondition** | Service đang chạy, có dữ liệu trong items.json |
| **Request** | `GET /api/items` |
| **Expected Result** | HTTP 200, trả về mảng JSON chứa danh sách items |
| **Expected Response** | Array of Items |

**Test Steps**:
1. Gửi GET request đến `/api/items`
2. Kiểm tra status code = 200
3. Kiểm tra response là một mảng
4. Kiểm tra mảng chứa ít nhất 1 item
5. Kiểm tra mỗi item có đầy đủ trường: id, name, price, quantity, category, sku, status

**cURL Command**:
```bash
curl -X GET http://localhost:8082/api/items
```

---

#### **TC-002: Lấy chi tiết sản phẩm theo ID (Valid ID)**
**Endpoint**: `GET /api/items/{id}`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Lấy thông tin chi tiết sản phẩm với ID hợp lệ |
| **Precondition** | Item với ID=1 tồn tại trong hệ thống |
| **Request** | `GET /api/items/1` |
| **Expected Result** | HTTP 200, trả về item object |

**Test Steps**:
1. Gửi GET request với ID = 1
2. Kiểm tra status code = 200
3. Kiểm tra response có trường id = 1
4. Kiểm tra các trường bắt buộc có giá trị

**cURL Command**:
```bash
curl -X GET http://localhost:8082/api/items/1
```

---

#### **TC-003: Lấy chi tiết sản phẩm (Invalid ID - Not Found)**
**Endpoint**: `GET /api/items/{id}`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Kiểm tra xử lý khi ID không tồn tại |
| **Precondition** | Item với ID=9999 không tồn tại |
| **Request** | `GET /api/items/9999` |
| **Expected Result** | HTTP 404, error message |

**Test Steps**:
1. Gửi GET request với ID không tồn tại (9999)
2. Kiểm tra status code = 404
3. Kiểm tra response chứa error message

**cURL Command**:
```bash
curl -X GET http://localhost:8082/api/items/9999
```

---

#### **TC-004: Lấy chi tiết sản phẩm (Invalid ID - Negative)**
**Endpoint**: `GET /api/items/{id}`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Kiểm tra validation với ID âm |
| **Request** | `GET /api/items/-1` |
| **Expected Result** | HTTP 400, validation error |

**Test Steps**:
1. Gửi GET request với ID âm (-1)
2. Kiểm tra status code = 400
3. Kiểm tra error message về ID không hợp lệ

**cURL Command**:
```bash
curl -X GET http://localhost:8082/api/items/-1
```

---

#### **TC-005: Tạo sản phẩm mới (Valid Data)**
**Endpoint**: `POST /api/items`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Tạo sản phẩm mới với đầy đủ dữ liệu hợp lệ |
| **Request Body** | Valid Item JSON |
| **Expected Result** | HTTP 201, trả về item đã tạo với ID mới |

**Test Data**:
```json
{
  "name": "Test Keyboard",
  "description": "Mechanical Gaming Keyboard",
  "price": 149.99,
  "quantity": 50,
  "category": "Accessories",
  "sku": "TEST-KB-001",
  "status": "ACTIVE"
}
```

**Test Steps**:
1. Gửi POST request với dữ liệu hợp lệ
2. Kiểm tra status code = 201
3. Kiểm tra response có ID được sinh tự động
4. Kiểm tra createdAt và updatedAt được tạo

**cURL Command**:
```bash
curl -X POST http://localhost:8082/api/items \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Keyboard",
    "description": "Mechanical Gaming Keyboard",
    "price": 149.99,
    "quantity": 50,
    "category": "Accessories",
    "sku": "TEST-KB-001",
    "status": "ACTIVE"
  }'
```

---

#### **TC-006: Tạo sản phẩm (Missing Required Fields)**
**Endpoint**: `POST /api/items`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Kiểm tra validation khi thiếu trường bắt buộc |
| **Request Body** | Item JSON thiếu name |
| **Expected Result** | HTTP 400, validation error |

**Test Data**:
```json
{
  "description": "Test Item",
  "price": 99.99,
  "quantity": 10,
  "category": "Test"
}
```

**Test Steps**:
1. Gửi POST request thiếu trường name
2. Kiểm tra status code = 400
3. Kiểm tra error message về name bắt buộc

**cURL Command**:
```bash
curl -X POST http://localhost:8082/api/items \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Test Item",
    "price": 99.99,
    "quantity": 10,
    "category": "Test"
  }'
```

---

#### **TC-007: Tạo sản phẩm (Duplicate SKU)**
**Endpoint**: `POST /api/items`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Kiểm tra xử lý SKU trùng lặp |
| **Precondition** | SKU "DELL-XPS-13" đã tồn tại |
| **Request Body** | Item với SKU đã tồn tại |
| **Expected Result** | HTTP 409, conflict error |

**Test Data**:
```json
{
  "name": "Duplicate Item",
  "price": 99.99,
  "quantity": 10,
  "category": "Test",
  "sku": "DELL-XPS-13",
  "status": "ACTIVE"
}
```

**Test Steps**:
1. Gửi POST request với SKU đã tồn tại
2. Kiểm tra status code = 409
3. Kiểm tra error message về SKU trùng lặp

---

#### **TC-008: Tạo sản phẩm (Invalid Price)**
**Endpoint**: `POST /api/items`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Kiểm tra validation giá âm hoặc bằng 0 |
| **Request Body** | Item với price = -10 |
| **Expected Result** | HTTP 400, validation error |

**Test Data**:
```json
{
  "name": "Invalid Price Item",
  "price": -10,
  "quantity": 10,
  "category": "Test",
  "sku": "TEST-INVALID-001",
  "status": "ACTIVE"
}
```

---

#### **TC-009: Cập nhật sản phẩm (Valid Data)**
**Endpoint**: `PUT /api/items/{id}`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Cập nhật thông tin sản phẩm |
| **Precondition** | Item với ID=1 tồn tại |
| **Request Body** | Partial update data |
| **Expected Result** | HTTP 200, item đã cập nhật |

**Test Data**:
```json
{
  "price": 899.99,
  "quantity": 45
}
```

**Test Steps**:
1. Gửi PUT request với dữ liệu cập nhật
2. Kiểm tra status code = 200
3. Kiểm tra giá và số lượng đã được cập nhật
4. Kiểm tra updatedAt đã thay đổi

**cURL Command**:
```bash
curl -X PUT http://localhost:8082/api/items/1 \
  -H "Content-Type: application/json" \
  -d '{
    "price": 899.99,
    "quantity": 45
  }'
```

---

#### **TC-010: Cập nhật sản phẩm (Not Found)**
**Endpoint**: `PUT /api/items/{id}`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Kiểm tra cập nhật với ID không tồn tại |
| **Request** | `PUT /api/items/9999` |
| **Expected Result** | HTTP 404, error message |

---

#### **TC-011: Xóa sản phẩm (Valid ID)**
**Endpoint**: `DELETE /api/items/{id}`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Xóa sản phẩm thành công |
| **Precondition** | Đã tạo item test để xóa |
| **Request** | `DELETE /api/items/{test_id}` |
| **Expected Result** | HTTP 204, no content |

**Test Steps**:
1. Tạo item test mới
2. Gửi DELETE request với ID của item test
3. Kiểm tra status code = 204
4. Gửi GET request để xác nhận item đã bị xóa (404)

---

#### **TC-012: Xóa sản phẩm (Not Found)**
**Endpoint**: `DELETE /api/items/{id}`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Kiểm tra xóa với ID không tồn tại |
| **Request** | `DELETE /api/items/9999` |
| **Expected Result** | HTTP 404, error message |

---

### **Test Suite 2: Search & Filter Operations**

#### **TC-013: Tìm kiếm theo Category (Valid)**
**Endpoint**: `GET /api/items/search/category?category=Electronics`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Tìm tất cả sản phẩm trong category Electronics |
| **Request** | Query param: category=Electronics |
| **Expected Result** | HTTP 200, array các items thuộc Electronics |

**Test Steps**:
1. Gửi GET request với category=Electronics
2. Kiểm tra status code = 200
3. Kiểm tra tất cả items trả về có category = "Electronics"
4. Kiểm tra có ít nhất 1 item

**cURL Command**:
```bash
curl -X GET "http://localhost:8082/api/items/search/category?category=Electronics"
```

---

#### **TC-014: Tìm kiếm theo Category (Empty Result)**
**Endpoint**: `GET /api/items/search/category?category=NonExistent`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Tìm với category không tồn tại |
| **Request** | Query param: category=NonExistent |
| **Expected Result** | HTTP 200, empty array |

---

#### **TC-015: Tìm kiếm theo Category (Missing Param)**
**Endpoint**: `GET /api/items/search/category`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Kiểm tra validation khi thiếu category param |
| **Expected Result** | HTTP 400, error message |

---

#### **TC-016: Tìm kiếm theo Price Range (Valid)**
**Endpoint**: `GET /api/items/search/price?minPrice=50&maxPrice=500`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Tìm sản phẩm trong phạm vi giá 50-500 |
| **Request** | Query params: minPrice=50, maxPrice=500 |
| **Expected Result** | HTTP 200, items có giá từ 50 đến 500 |

**Test Steps**:
1. Gửi GET request với minPrice=50, maxPrice=500
2. Kiểm tra status code = 200
3. Kiểm tra tất cả items có 50 <= price <= 500

**cURL Command**:
```bash
curl -X GET "http://localhost:8082/api/items/search/price?minPrice=50&maxPrice=500"
```

---

#### **TC-017: Tìm kiếm theo Price Range (Invalid - Min > Max)**
**Endpoint**: `GET /api/items/search/price?minPrice=500&maxPrice=50`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Kiểm tra validation khi minPrice > maxPrice |
| **Expected Result** | HTTP 400, validation error |

---

#### **TC-018: Tìm kiếm theo Price Range (Negative Values)**
**Endpoint**: `GET /api/items/search/price?minPrice=-10&maxPrice=100`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Kiểm tra validation với giá âm |
| **Expected Result** | HTTP 400, validation error |

---

#### **TC-019: Tìm kiếm theo Name (Valid)**
**Endpoint**: `GET /api/items/search/name?name=Laptop`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Tìm sản phẩm có tên chứa "Laptop" |
| **Request** | Query param: name=Laptop |
| **Expected Result** | HTTP 200, items có tên chứa "Laptop" |

**cURL Command**:
```bash
curl -X GET "http://localhost:8082/api/items/search/name?name=Laptop"
```

---

#### **TC-020: Tìm kiếm theo Name (Case Insensitive)**
**Endpoint**: `GET /api/items/search/name?name=laptop`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Kiểm tra tìm kiếm không phân biệt chữ hoa/thường |
| **Request** | Query param: name=laptop (lowercase) |
| **Expected Result** | HTTP 200, tìm thấy "Laptop" items |

---

### **Test Suite 3: Statistics & Health Check**

#### **TC-021: Lấy thống kê**
**Endpoint**: `GET /api/items/stats`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Lấy thống kê về items |
| **Expected Result** | HTTP 200, object chứa totalItems, timestamp |

**cURL Command**:
```bash
curl -X GET http://localhost:8082/api/items/stats
```

---

#### **TC-022: Health Check**
**Endpoint**: `GET /actuator/health`

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Kiểm tra service health |
| **Expected Result** | HTTP 200, status: UP |

**cURL Command**:
```bash
curl -X GET http://localhost:8082/actuator/health
```

---

## 🤖 Automated Testing

### **Test Script: `test-item-service.ps1`**

Script PowerShell tự động chạy tất cả test cases và ghi log kết quả.

**Features**:
- ✅ Tự động chạy 22 test cases
- ✅ Ghi log chi tiết vào file
- ✅ Báo cáo tổng hợp (Passed/Failed)
- ✅ Màu sắc cho output (Pass = Green, Fail = Red)
- ✅ Timestamp cho mỗi test

---

## 📊 Test Results Format

Log file sẽ có format:

```
================================
ITEM SERVICE - TEST EXECUTION
================================
Start Time: 2026-03-04 16:30:00
Service URL: http://localhost:8082
================================

[TC-001] Lấy danh sách tất cả sản phẩm
-----------------------------------
Request: GET /api/items
Status Code: 200
Response Time: 150ms
Result: ✅ PASSED
Details: Trả về 5 items

[TC-002] Lấy chi tiết sản phẩm (Valid ID)
-----------------------------------
Request: GET /api/items/1
Status Code: 200
Response Time: 50ms
Result: ✅ PASSED
Details: Item ID=1 found

...

================================
TEST SUMMARY
================================
Total Tests: 22
Passed: 20
Failed: 2
Success Rate: 90.9%
End Time: 2026-03-04 16:32:15
Duration: 2 minutes 15 seconds
================================
```

---

## 📁 Log Files

| File | Mô tả |
|------|-------|
| `test-results.log` | Kết quả chi tiết tất cả test cases |
| `test-summary.log` | Tóm tắt kết quả (Pass/Fail count) |
| `test-errors.log` | Chi tiết các test cases failed |

---

## 🔍 Test Coverage

| Area | Test Cases | Coverage |
|------|------------|----------|
| CRUD Operations | TC-001 to TC-012 | 100% |
| Search & Filter | TC-013 to TC-020 | 100% |
| Statistics | TC-021 | 100% |
| Health Check | TC-022 | 100% |
| Validation | Multiple TCs | 100% |
| Error Handling | Multiple TCs | 100% |

---

## ✅ Pass/Fail Criteria

**Test PASSED nếu**:
1. Status code đúng như expected
2. Response structure đúng format
3. Data validation đúng
4. No exceptions hoặc errors

**Test FAILED nếu**:
1. Status code sai
2. Response format không đúng
3. Service không phản hồi (timeout)
4. Exception/Error không mong muốn

---

## 🚀 How to Run Tests

### **Bước 1: Khởi động Service**
```powershell
cd spring-item-service
mvn spring-boot:run
```

### **Bước 2: Chạy Test Script**
```powershell
.\test-item-service.ps1
```

### **Bước 3: Xem Kết Quả**
```powershell
# Xem log chi tiết
cat test-results.log

# Xem log tóm tắt
cat test-summary.log
```

---

## 📝 Test Data Management

**Sample Data** (từ items.json):
- 5 items ban đầu (ID: 1-5)
- Categories: Electronics, Accessories, Storage
- Price range: $29.99 - $999.99

**Test Data** (tạo trong quá trình test):
- Test items với SKU: TEST-*
- Được xóa sau khi test xong

---

## 🔧 Troubleshooting

| Vấn đề | Giải pháp |
|--------|-----------|
| "Connection refused" | Kiểm tra service đang chạy trên port 8082 |
| "404 Not Found" | Kiểm tra URL endpoint đúng |
| "500 Internal Error" | Kiểm tra logs/item-service.log |
| Test timeout | Tăng timeout trong script |

---

**Phiên bản**: 1.0  
**Ngày tạo**: March 4, 2026  
**Tác giả**: Test Team  
**Status**: ✅ Ready for Execution
