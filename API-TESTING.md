# 🧪 API Testing Guide - Gateway Offloading Pattern

Hướng dẫn test tất cả API của hệ thống sau khi chạy `docker-compose up`.

> **Base URLs:**
> - Kong Gateway: `http://localhost:8000` (qua gateway)
> - User Service (direct): `http://localhost:8080`
> - Order Service (direct): `http://localhost:8081`
> - Item Service (direct): `http://localhost:8082`

---

## 📋 Mục lục

1. [Health Check - Kiểm tra services](#1-health-check---kiểm-tra-services)
2. [Item Service APIs](#2-item-service-apis)
3. [User Service APIs](#3-user-service-apis)
4. [Order Service APIs](#4-order-service-apis)
5. [Monitoring](#5-monitoring)

---

## 1. Health Check - Kiểm tra services

### 1.1. Kong Gateway
```bash
curl -i http://localhost:8001/status
```

### 1.2. User Service
```bash
curl -i http://localhost:8080/actuator/health
```

### 1.3. Order Service
```bash
curl -i http://localhost:8081/actuator/health
```

### 1.4. Item Service
```bash
curl -i http://localhost:8082/actuator/health
```

### 1.5. Kiểm tra tất cả cùng lúc (PowerShell)
```powershell
@("http://localhost:8001/status", "http://localhost:8080/actuator/health", "http://localhost:8081/actuator/health", "http://localhost:8082/actuator/health") | ForEach-Object {
    Write-Host "`n--- $_" -ForegroundColor Cyan
    try { (Invoke-RestMethod $_) | ConvertTo-Json -Depth 3 } catch { Write-Host "FAIL: $_" -ForegroundColor Red }
}
```

---

## 2. Item Service APIs

### 2.1. Lấy danh sách tất cả sản phẩm
```bash
curl -i http://localhost:8082/api/items
```

**Expected:** `200 OK` - Trả về danh sách items (JSON array)

---

### 2.2. Lấy chi tiết sản phẩm theo ID
```bash
curl -i http://localhost:8082/api/items/1
```

**Expected:** `200 OK` - Trả về thông tin item, hoặc `404` nếu không tồn tại

---

### 2.3. Tạo sản phẩm mới
```bash
curl -i -X POST http://localhost:8082/api/items \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Dell XPS 15",
    "description": "Laptop cao cấp cho developer",
    "price": 35000000,
    "quantity": 50,
    "category": "Electronics",
    "sku": "DELL-XPS-15-001",
    "status": "ACTIVE"
  }'
```

**PowerShell:**
```powershell
$body = @{
    name = "Laptop Dell XPS 15"
    description = "Laptop cao cap cho developer"
    price = 35000000
    quantity = 50
    category = "Electronics"
    sku = "DELL-XPS-15-001"
    status = "ACTIVE"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/items" -Method POST -Body $body -ContentType "application/json"
```

**Expected:** `201 Created` - Trả về item vừa tạo

---

### 2.4. Cập nhật sản phẩm
```bash
curl -i -X PUT http://localhost:8082/api/items/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Dell XPS 15 (Updated)",
    "description": "Laptop cao cấp - phiên bản mới",
    "price": 32000000,
    "quantity": 45,
    "category": "Electronics",
    "sku": "DELL-XPS-15-001",
    "status": "ACTIVE"
  }'
```

**PowerShell:**
```powershell
$body = @{
    name = "Laptop Dell XPS 15 (Updated)"
    description = "Laptop cao cap - phien ban moi"
    price = 32000000
    quantity = 45
    category = "Electronics"
    sku = "DELL-XPS-15-001"
    status = "ACTIVE"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/items/1" -Method PUT -Body $body -ContentType "application/json"
```

**Expected:** `200 OK` - Trả về item đã cập nhật

---

### 2.5. Xóa sản phẩm
```bash
curl -i -X DELETE http://localhost:8082/api/items/1
```

**Expected:** `204 No Content`

---

### 2.6. Tìm kiếm theo Category
```bash
curl -i "http://localhost:8082/api/items/search/category?category=Electronics"
```

**Expected:** `200 OK` - Danh sách items theo category

---

### 2.7. Tìm kiếm theo khoảng giá
```bash
curl -i "http://localhost:8082/api/items/search/price?minPrice=10000&maxPrice=50000000"
```

**Expected:** `200 OK` - Danh sách items trong khoảng giá

---

### 2.8. Tìm kiếm theo tên
```bash
curl -i "http://localhost:8082/api/items/search/name?name=Laptop"
```

**Expected:** `200 OK` - Danh sách items có tên chứa keyword

---

### 2.9. Thống kê sản phẩm
```bash
curl -i http://localhost:8082/api/items/stats
```

**Expected:** `200 OK` - `{"totalItems": ..., "timestamp": ...}`

---

### 2.10. Swagger UI (Item Service)
Mở trình duyệt:
```
http://localhost:8082/swagger-ui.html
```

---

## 3. User Service APIs

### 3.1. Auth - Health Check (Public - không cần JWT)
```bash
curl -i -X POST http://localhost:8000/api/v1/auth/health
```

**Expected:** `200 OK`

---

### 3.2. Auth - Info (Public)
```bash
curl -i http://localhost:8000/api/v1/auth/info
```

---

### 3.3. Lấy danh sách users (Protected - cần JWT)

> ⚠️ Các route `/api/v1/users` và `/api/v1/admin` yêu cầu JWT token qua Kong Gateway.

Bước 1: Tạo JWT token (dùng [jwt.io](https://jwt.io) hoặc script):

**Payload cho regular user:**
```json
{
  "iss": "user-issuer",
  "exp": 1900000000,
  "sub": "user-001"
}
```
**Secret:** `royUGbSvQAOa39UfYdxp6XJhvKTXXrbwYW0RwJdslLf` (Algorithm: HS256)

**Payload cho admin:**
```json
{
  "iss": "admin-issuer",
  "exp": 1900000000,
  "sub": "admin-001"
}
```
**Secret:** `0fnG4qhwGcijk529oNlvG3lFFgUF2dpGZY7IiOXVSlb` (Algorithm: HS256)

Bước 2: Gọi API với token:
```bash
# Thay <JWT_TOKEN> bằng token đã tạo
curl -i http://localhost:8000/api/v1/users \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Expected:** `200 OK` - Danh sách users

---

### 3.4. Lấy thông tin user hiện tại
```bash
curl -i http://localhost:8000/api/v1/users/me \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

---

### 3.5. Lấy user theo ID
```bash
curl -i http://localhost:8000/api/v1/users/1 \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

---

### 3.6. Tạo user mới
```bash
curl -i -X POST http://localhost:8000/api/v1/users \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test@123"
  }'
```

---

### 3.7. Cập nhật user
```bash
curl -i -X PUT http://localhost:8000/api/v1/users/1 \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "updateduser",
    "email": "updated@example.com"
  }'
```

---

### 3.8. Deactivate / Activate user
```bash
# Deactivate
curl -i -X PATCH http://localhost:8000/api/v1/users/1/deactivate \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Activate
curl -i -X PATCH http://localhost:8000/api/v1/users/1/activate \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

---

### 3.9. Admin APIs (cần Admin JWT token)

```bash
# Lấy danh sách tất cả users (admin only)
curl -i http://localhost:8000/api/v1/admin/users \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"

# Deactivate user (admin only)
curl -i -X PATCH http://localhost:8000/api/v1/admin/users/1/deactivate \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"

# Activate user (admin only)
curl -i -X PATCH http://localhost:8000/api/v1/admin/users/1/activate \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"
```

---

## 4. Order Service APIs

### 4.1. Tạo đơn hàng
```bash
curl -i -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "items": [
      {
        "itemId": 1,
        "quantity": 2,
        "price": 35000000
      },
      {
        "itemId": 2,
        "quantity": 1,
        "price": 15000000
      }
    ]
  }'
```

**PowerShell:**
```powershell
$body = @{
    userId = 1
    items = @(
        @{ itemId = 1; quantity = 2; price = 35000000 },
        @{ itemId = 2; quantity = 1; price = 15000000 }
    )
} | ConvertTo-Json -Depth 3

Invoke-RestMethod -Uri "http://localhost:8081/api/orders" -Method POST -Body $body -ContentType "application/json"
```

**Expected:** `201 Created`

---

### 4.2. Lấy danh sách đơn hàng
```bash
curl -i http://localhost:8081/api/orders
```

**Expected:** `200 OK` - Danh sách tất cả orders

---

### 4.3. Lấy chi tiết đơn hàng
```bash
curl -i http://localhost:8081/api/orders/1
```

**Expected:** `200 OK` hoặc `404` nếu không tồn tại

---

### 4.4. Cập nhật trạng thái đơn hàng
```bash
curl -i -X PATCH http://localhost:8081/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "CONFIRMED"}'
```

**PowerShell:**
```powershell
$body = @{ status = "CONFIRMED" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/api/orders/1/status" -Method PATCH -Body $body -ContentType "application/json"
```

**Expected:** `200 OK` - "Cập nhật trạng thái thành công."

---

## 5. Monitoring

### 5.1. Prometheus
```
http://localhost:9090
```

### 5.2. Grafana
```
http://localhost:3000
```
- **Username:** `admin`
- **Password:** `admin123`

### 5.3. Kibana (ELK)
```
http://localhost:5601
```

### 5.4. Kong Admin API
```bash
# Xem tất cả services
curl -i http://localhost:8001/services

# Xem tất cả routes
curl -i http://localhost:8001/routes

# Xem tất cả plugins
curl -i http://localhost:8001/plugins
```

### 5.5. Prometheus Metrics từ các service
```bash
# Item Service metrics
curl -s http://localhost:8082/actuator/prometheus | head -50

# User Service metrics
curl -s http://localhost:8080/actuator/prometheus | head -50

# Order Service metrics
curl -s http://localhost:8081/actuator/prometheus | head -50
```

---

## 🚀 Quick Test Script (PowerShell)

Chạy script sau để test nhanh tất cả services:

```powershell
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "  GATEWAY OFFLOADING - API QUICK TEST" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow

$tests = @(
    @{ Name = "Kong Gateway";       URL = "http://localhost:8001/status" },
    @{ Name = "User Service Health"; URL = "http://localhost:8080/actuator/health" },
    @{ Name = "Order Service Health"; URL = "http://localhost:8081/actuator/health" },
    @{ Name = "Item Service Health"; URL = "http://localhost:8082/actuator/health" },
    @{ Name = "Item List";           URL = "http://localhost:8082/api/items" },
    @{ Name = "Item Stats";          URL = "http://localhost:8082/api/items/stats" },
    @{ Name = "Order List";          URL = "http://localhost:8081/api/orders" },
    @{ Name = "Prometheus";          URL = "http://localhost:9090/-/healthy" },
    @{ Name = "Grafana";             URL = "http://localhost:3000/api/health" }
)

foreach ($test in $tests) {
    Write-Host "`n[$($test.Name)]" -ForegroundColor Cyan -NoNewline
    try {
        $response = Invoke-WebRequest -Uri $test.URL -TimeoutSec 5 -ErrorAction Stop
        Write-Host " ✅ $($response.StatusCode)" -ForegroundColor Green
    } catch {
        Write-Host " ❌ FAIL" -ForegroundColor Red
    }
}

Write-Host "`n========================================" -ForegroundColor Yellow
Write-Host "  TEST COMPLETE" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
```

---

## 📝 Ghi chú

- **Item Service** không yêu cầu JWT (truy cập trực tiếp port 8082)
- **User Service** & **Admin routes** yêu cầu JWT qua Kong Gateway (port 8000)
- **Order Service** hiện truy cập trực tiếp qua port 8081
- Tạo JWT token tại [jwt.io](https://jwt.io) với secret và payload tương ứng
