# Spring Item Service

**Microservice quản lý sản phẩm (Items) trong hệ thống Demo Offloading**

## 📌 Mô Tả Service

Item Service là một microservice độc lập chịu trách nhiệm:

- ✅ Quản lý danh sách sản phẩm (Items)
- ✅ Cung cấp thông tin chi tiết về sản phẩm
- ✅ Hỗ trợ CRUD operations cơ bản
- ✅ Trả dữ liệu dưới dạng JSON

**Port mặc định**: `8082`

---

## 🏗️ Cấu Trúc Dự Án

```
spring-item-service/
├── pom.xml
├── README.md                           # Tài liệu này
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/
│   │   │       ├── Main.java           # Entry point Spring Boot
│   │   │       ├── controller/
│   │   │       │   └── ItemController.java       # REST Controller
│   │   │       ├── service/
│   │   │       │   └── ItemService.java          # Business Logic
│   │   │       ├── model/
│   │   │       │   └── Item.java                 # Entity/DTO
│   │   │       ├── repository/
│   │   │       │   └── ItemRepository.java       # Data Access
│   │   │       └── config/
│   │   │           └── ApplicationConfig.java    # Configuration
│   │   └── resources/
│   │       ├── application.yml         # Spring Boot Configuration
│   │       └── items.json              # Sample Data
│   └── test/
│       └── java/
│           └── ItemServiceTests.java   # Unit Tests
└── target/
    └── spring-item-service-0.0.1-SNAPSHOT.jar
```

---

## 📡 API Endpoints

### **Base URL**

```
http://localhost:8082/api/items
```

### **1️⃣ Lấy Danh Sách Tất Cả Sản Phẩm**

```http
GET /api/items
```

**Response (200 OK)**:

```json
[
  {
    "id": 1,
    "name": "Laptop Dell XPS 13",
    "description": "Lightweight and powerful laptop",
    "price": 999.99,
    "quantity": 50,
    "category": "Electronics",
    "sku": "DELL-XPS-13",
    "status": "ACTIVE",
    "createdAt": "2025-01-15T10:00:00",
    "updatedAt": "2025-03-04T15:30:00"
  },
  {
    "id": 2,
    "name": "Wireless Mouse",
    "description": "USB wireless mouse with 1 year battery life",
    "price": 29.99,
    "quantity": 200,
    "category": "Accessories",
    "sku": "MOUSE-WIRELESS-001",
    "status": "ACTIVE",
    "createdAt": "2025-01-20T10:00:00",
    "updatedAt": "2025-03-04T15:30:00"
  }
]
```

---

### **2️⃣ Lấy Chi Tiết Sản Phẩm Theo ID**

```http
GET /api/items/{id}
```

**Parameters**:
| Tham số | Loại | Bắt buộc | Mô Tả |
|---------|------|----------|-------|
| `id` | Integer | ✅ | ID của sản phẩm |

**Response (200 OK)**:

```json
{
  "id": 1,
  "name": "Laptop Dell XPS 13",
  "description": "Lightweight and powerful laptop",
  "price": 999.99,
  "quantity": 50,
  "category": "Electronics",
  "sku": "DELL-XPS-13",
  "status": "ACTIVE",
  "createdAt": "2025-01-15T10:00:00",
  "updatedAt": "2025-03-04T15:30:00"
}
```

**Response (404 Not Found)**:

```json
{
  "error": "Item not found",
  "message": "Item with ID 999 does not exist",
  "timestamp": "2025-03-04T15:30:00"
}
```

---

### **3️⃣ Tạo Sản Phẩm Mới**

```http
POST /api/items
Content-Type: application/json
```

**Request Body**:

```json
{
  "name": "Mechanical Keyboard",
  "description": "RGB Mechanical Gaming Keyboard",
  "price": 149.99,
  "quantity": 100,
  "category": "Accessories",
  "sku": "KB-MECH-RGB-001",
  "status": "ACTIVE"
}
```

**Response (201 Created)**:

```json
{
  "id": 3,
  "name": "Mechanical Keyboard",
  "description": "RGB Mechanical Gaming Keyboard",
  "price": 149.99,
  "quantity": 100,
  "category": "Accessories",
  "sku": "KB-MECH-RGB-001",
  "status": "ACTIVE",
  "createdAt": "2025-03-04T16:00:00",
  "updatedAt": "2025-03-04T16:00:00"
}
```

**Validation Errors (400 Bad Request)**:

```json
{
  "errors": {
    "name": "Name is required",
    "price": "Price must be greater than 0",
    "sku": "SKU must be unique"
  },
  "timestamp": "2025-03-04T16:00:00"
}
```

---

### **4️⃣ Cập Nhật Sản Phẩm**

```http
PUT /api/items/{id}
Content-Type: application/json
```

**Parameters**:
| Tham số | Loại | Bắt buộc | Mô Tả |
|---------|------|----------|-------|
| `id` | Integer | ✅ | ID của sản phẩm |

**Request Body**:

```json
{
  "name": "Mechanical Keyboard RGB",
  "description": "Premium RGB Mechanical Gaming Keyboard",
  "price": 179.99,
  "quantity": 95,
  "status": "ACTIVE"
}
```

**Response (200 OK)**:

```json
{
  "id": 3,
  "name": "Mechanical Keyboard RGB",
  "description": "Premium RGB Mechanical Gaming Keyboard",
  "price": 179.99,
  "quantity": 95,
  "category": "Accessories",
  "sku": "KB-MECH-RGB-001",
  "status": "ACTIVE",
  "createdAt": "2025-03-04T16:00:00",
  "updatedAt": "2025-03-04T16:30:00"
}
```

---

### **5️⃣ Xóa Sản Phẩm**

```http
DELETE /api/items/{id}
```

**Parameters**:
| Tham số | Loại | Bắt buộc | Mô Tả |
|---------|------|----------|-------|
| `id` | Integer | ✅ | ID của sản phẩm |

**Response (204 No Content)**:

```
(No content)
```

**Response (404 Not Found)**:

```json
{
  "error": "Item not found",
  "message": "Cannot delete: Item with ID 999 does not exist",
  "timestamp": "2025-03-04T16:30:00"
}
```

---

### **6️⃣ Tìm Kiếm Sản Phẩm Theo Category**

```http
GET /api/items/search/category?category=Electronics
```

**Parameters**:
| Tham số | Loại | Bắt buộc | Mô Tả |
|---------|------|----------|-------|
| `category` | String | ✅ | Loại sản phẩm |

**Response (200 OK)**:

```json
[
  {
    "id": 1,
    "name": "Laptop Dell XPS 13",
    "category": "Electronics",
    "price": 999.99,
    "quantity": 50
  }
]
```

---

### **7️⃣ Lấy Sản Phẩm Theo Phạm Vi Giá**

```http
GET /api/items/search/price?minPrice=50&maxPrice=500
```

**Parameters**:
| Tham số | Loại | Bắt buộc | Mô Tả |
|---------|------|----------|-------|
| `minPrice` | Double | ✅ | Giá tối thiểu |
| `maxPrice` | Double | ✅ | Giá tối đa |

**Response (200 OK)**:

```json
[
  {
    "id": 2,
    "name": "Wireless Mouse",
    "price": 29.99,
    "quantity": 200
  },
  {
    "id": 3,
    "name": "Mechanical Keyboard RGB",
    "price": 179.99,
    "quantity": 95
  }
]
```

---

## 📊 Data Model - Item Entity

```json
{
  "id": "number (unique identifier)",
  "name": "string (required, max 255)",
  "description": "string (optional, max 1000)",
  "price": "number (required, > 0)",
  "quantity": "number (required, >= 0)",
  "category": "string (required)",
  "sku": "string (unique, required)",
  "status": "enum: ACTIVE | INACTIVE | DISCONTINUED",
  "createdAt": "timestamp (auto-generated)",
  "updatedAt": "timestamp (auto-generated)"
}
```

---

## 🗄️ Sample Data (items.json)

```json
{
  "items": [
    {
      "id": 1,
      "name": "Laptop Dell XPS 13",
      "description": "Lightweight and powerful laptop",
      "price": 999.99,
      "quantity": 50,
      "category": "Electronics",
      "sku": "DELL-XPS-13",
      "status": "ACTIVE",
      "createdAt": "2025-01-15T10:00:00",
      "updatedAt": "2025-03-04T15:30:00"
    },
    {
      "id": 2,
      "name": "Wireless Mouse",
      "description": "USB wireless mouse with 1 year battery life",
      "price": 29.99,
      "quantity": 200,
      "category": "Accessories",
      "sku": "MOUSE-WIRELESS-001",
      "status": "ACTIVE",
      "createdAt": "2025-01-20T10:00:00",
      "updatedAt": "2025-03-04T15:30:00"
    },
    {
      "id": 3,
      "name": "USB-C Hub",
      "description": "7-in-1 USB-C Hub with HDMI, USB 3.0, SD Card Reader",
      "price": 79.99,
      "quantity": 75,
      "category": "Accessories",
      "sku": "HUB-USB-C-7IN1",
      "status": "ACTIVE",
      "createdAt": "2025-02-01T10:00:00",
      "updatedAt": "2025-03-04T15:30:00"
    },
    {
      "id": 4,
      "name": "Monitor LG 27\"",
      "description": "4K UHD Monitor with HDR support",
      "price": 399.99,
      "quantity": 30,
      "category": "Electronics",
      "sku": "MONITOR-LG-27-4K",
      "status": "ACTIVE",
      "createdAt": "2025-02-10T10:00:00",
      "updatedAt": "2025-03-04T15:30:00"
    },
    {
      "id": 5,
      "name": "External SSD 1TB",
      "description": "USB 3.1 External SSD, Fast and Portable",
      "price": 129.99,
      "quantity": 100,
      "category": "Storage",
      "sku": "SSD-EXT-1TB",
      "status": "ACTIVE",
      "createdAt": "2025-02-15T10:00:00",
      "updatedAt": "2025-03-04T15:30:00"
    }
  ]
}
```

---

## 🚀 Hướng Dẫn Chạy Service

### **Yêu Cầu**

- Java 21+
- Maven 3.8+
- Spring Boot 4.0.3

### **Bước 1: Build Project**

```bash
# Từ thư mục gốc
mvn clean package

# Hoặc chỉ build Item Service
cd spring-item-service
mvn clean package
```

### **Bước 2: Chạy Service**

```bash
# Cách 1: Sử dụng Maven
mvn spring-boot:run

# Cách 2: Chạy JAR file
java -jar target/spring-item-service-0.0.1-SNAPSHOT.jar

# Cách 3: Chạy trong IDE (IntelliJ/Eclipse)
# Click Run trên lớp Main.java
```

### **Bước 3: Kiểm Tra Service**

```bash
# Service sẽ khởi động tại http://localhost:8082

# Kiểm tra health
curl http://localhost:8082/actuator/health

# Lấy danh sách item
curl http://localhost:8082/api/items

# Swagger UI
http://localhost:8082/swagger-ui.html
```

---

## 🧪 Ví Dụ Kiểm Thử (cURL)

### **1. Lấy tất cả sản phẩm**

```bash
curl -X GET http://localhost:8082/api/items \
  -H "Content-Type: application/json"
```

### **2. Lấy sản phẩm cụ thể**

```bash
curl -X GET http://localhost:8082/api/items/1 \
  -H "Content-Type: application/json"
```

### **3. Tạo sản phẩm mới**

```bash
curl -X POST http://localhost:8082/api/items \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mechanical Keyboard",
    "description": "RGB Mechanical Gaming Keyboard",
    "price": 149.99,
    "quantity": 100,
    "category": "Accessories",
    "sku": "KB-MECH-RGB-001",
    "status": "ACTIVE"
  }'
```

### **4. Cập nhật sản phẩm**

```bash
curl -X PUT http://localhost:8082/api/items/3 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mechanical Keyboard RGB",
    "price": 179.99,
    "quantity": 95
  }'
```

### **5. Xóa sản phẩm**

```bash
curl -X DELETE http://localhost:8082/api/items/3 \
  -H "Content-Type: application/json"
```

### **6. Tìm kiếm theo category**

```bash
curl -X GET "http://localhost:8082/api/items/search/category?category=Electronics" \
  -H "Content-Type: application/json"
```

### **7. Tìm kiếm theo phạm vi giá**

```bash
curl -X GET "http://localhost:8082/api/items/search/price?minPrice=50&maxPrice=500" \
  -H "Content-Type: application/json"
```

---

## 🔒 Offloading Pattern Áp Dụng

### **Tại Kong API Gateway**

Item Service sẽ được bảo vệ bởi các chức năng offloading từ Kong:

#### **Nhóm 1: Security & Identity Offloading**

- ✅ **JWT Authentication** - Kong xác thực token trước khi forward đến Item Service
- ✅ **Authorization** - Kong kiểm tra quyền truy cập (admin, customer, guest)
- ✅ **SSL/TLS Termination** - Kong xử lý HTTPS

#### **Nhóm 2: Performance & Reliability Offloading**

- ✅ **Rate Limiting** - Kong giới hạn số request đến Item Service (ví dụ: 1000 req/min)
- ✅ **Response Caching** - Kong cache GET requests trong 5 phút
- ✅ **Load Balancing** - Kong phân phối tải nếu có nhiều instance Item Service

#### **Nhóm 3: Observability & Transformation**

- ✅ **Request Logging** - Kong ghi nhật ký tất cả request
- ✅ **Response Monitoring** - Kong giám sát phản hồi
- ✅ **Request/Response Transformation** - Kong có thể thêm/sửa headers

### **Kong Configuration (declarative mode)**

```yaml
routes:
  - name: item-service-route
    paths:
      - /api/items
    service:
      name: item-service
    plugins:
      - name: jwt
      - name: rate-limiting
        config:
          minute: 1000
      - name: proxy-cache
        config:
          content_type:
            - application/json
      - name: file-log
        config:
          path: /var/log/kong/item-service.log

services:
  - name: item-service
    url: http://localhost:8082
    port: 8082
```

---

## 📋 HTTP Status Codes

| Code | Meaning               | Mô Tả                                            |
| ---- | --------------------- | ------------------------------------------------ |
| 200  | OK                    | Request thành công, lấy dữ liệu                  |
| 201  | Created               | Tạo resource mới thành công                      |
| 204  | No Content            | Xóa/Cập nhật thành công, không có dữ liệu trả về |
| 400  | Bad Request           | Request không hợp lệ (validation error)          |
| 401  | Unauthorized          | Chưa xác thực                                    |
| 403  | Forbidden             | Không có quyền truy cập                          |
| 404  | Not Found             | Resource không tồn tại                           |
| 409  | Conflict              | Conflict (ví dụ: SKU trùng lặp)                  |
| 500  | Internal Server Error | Lỗi server                                       |
| 503  | Service Unavailable   | Service đang bảo trì                             |

---

## 📚 Dependencies

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.3.0</version>
</dependency>

<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<dependency>
  <groupId>com.google.code.gson</groupId>
  <artifactId>gson</artifactId>
</dependency>
```

---

## 🔧 Application Configuration (application.yml)

```yaml
spring:
  application:
    name: item-service
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
  h2:
    console:
      enabled: true

server:
  port: 8082
  servlet:
    context-path: /

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

logging:
  level:
    root: INFO
    com.example: DEBUG
  file:
    name: logs/item-service.log
```

---

## 📊 Architecture Diagram

```
┌──────────────────────┐
│   Kong API Gateway   │
│  (Port 8000)         │
│  - JWT Auth          │
│  - Rate Limiting     │
│  - Caching           │
│  - Logging           │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Item Service        │
│  (Port 8082)         │
│                      │
│  ├─ Controller       │
│  ├─ Service         │
│  ├─ Repository      │
│  └─ Model          │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   Database (JSON)    │
│  items.json          │
└──────────────────────┘
```

---

## 🤝 Tích Hợp Với Các Service Khác

### **Tương Tác Với Order Service**

- Order Service sẽ gọi Item Service để get thông tin sản phẩm khi tạo order
- Endpoint: `GET /api/items/{id}`

### **Tương Tác Với User Service**

- Không có dependency trực tiếp
- User Service có thể lấy danh sách item mà user yêu thích

---

## 📝 Ghi Chú

- Database sử dụng **JSON files** cho demo (có thể thay bằng PostgreSQL/MongoDB)
- Tất cả timestamps theo **ISO 8601** format
- API Response luôn có **consistency structure**
- Support **pagination** cho GET /api/items (optional)
- Support **sorting** và **filtering** (optional)

---

## 📧 Support & Liên Hệ

Để report bug hoặc gửi feedback:

- Tạo Issue trong repository
- Email: dev@example.com

---

**Phiên bản**: 1.0  
**Ngày cập nhật**: March 4, 2026  
**Status**: ✅ Active
