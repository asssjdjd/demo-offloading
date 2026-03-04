# 🚀 Hướng Dẫn Chạy Item Service Trên Windows

**Tài liệu hướng dẫn cài đặt và chạy Item Service trên Windows**

---

## 📋 Mục Lục

- [Yêu Cầu Hệ Thống](#yêu-cầu-hệ-thống)
- [Cài Đặt Dependencies](#cài-đặt-dependencies)
- [Build Project](#build-project)
- [Chạy Service](#chạy-service)
- [Test API](#test-api)
- [Chạy Automated Tests](#chạy-automated-tests)
- [Dừng Service](#dừng-service)
- [Troubleshooting](#troubleshooting)
- [Commands Tham Khảo](#commands-tham-khảo)

---

## 💻 Yêu Cầu Hệ Thống

### **Phần Mềm Cần Thiết**

| Software           | Version | Download Link                                       |
| ------------------ | ------- | --------------------------------------------------- |
| **Java JDK**       | 21+     | https://www.oracle.com/java/technologies/downloads/ |
| **Maven**          | 3.8+    | https://maven.apache.org/download.cgi               |
| **PowerShell**     | 5.0+    | Built-in Windows                                    |
| **Git** (Optional) | Latest  | https://git-scm.com/download/win                    |

### **Kiểm Tra Đã Cài Đặt Chưa**

Mở **PowerShell** và chạy các lệnh sau:

```powershell
# Kiểm tra Java
java -version

# Kiểm tra Maven
mvn -version

# Kiểm tra PowerShell
$PSVersionTable.PSVersion
```

**Expected Output**:

```
java version "21.0.x"
Apache Maven 3.x.x
PSVersion: 5.x or higher
```

---

## 📦 Cài Đặt Dependencies

### **Bước 1: Cài Đặt Java JDK 21**

1. Download Java JDK 21 từ Oracle
2. Chạy installer và follow wizard
3. Thiết lập `JAVA_HOME` environment variable:

```powershell
# Mở PowerShell với quyền Administrator
[System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Java\jdk-21', 'Machine')
```

4. Thêm Java vào PATH:

```powershell
$env:Path += ";$env:JAVA_HOME\bin"
```

### **Bước 2: Cài Đặt Maven**

1. Download Maven từ apache.org
2. Giải nén vào `C:\Program Files\Apache\maven`
3. Thiết lập `MAVEN_HOME`:

```powershell
[System.Environment]::SetEnvironmentVariable('MAVEN_HOME', 'C:\Program Files\Apache\maven', 'Machine')
$env:Path += ";$env:MAVEN_HOME\bin"
```

### **Bước 3: Restart PowerShell**

Sau khi cài đặt, **đóng và mở lại PowerShell** để load environment variables mới.

---

## 🏗️ Build Project

### **Bước 1: Di Chuyển Vào Thư Mục Project**

```powershell
cd d:\4Y2S\SOA\demo_present\demo-offloading\spring-item-service
```

### **Bước 2: Clean & Build Project**

```powershell
# Build project (bỏ qua tests)
mvn clean package -DskipTests
```

**Output mong đợi**:

```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

**JAR file được tạo tại**: `target\spring-item-service-0.0.1-SNAPSHOT.jar`

### **Bước 3: Verify Build**

```powershell
# Kiểm tra JAR file đã được tạo
Test-Path "target\spring-item-service-0.0.1-SNAPSHOT.jar"
```

**Expected**: `True`

---

## 🚀 Chạy Service

### **Cách 1: Chạy JAR File (Recommended)**

```powershell
# Di chuyển vào thư mục project
cd d:\4Y2S\SOA\demo_present\demo-offloading\spring-item-service

# Chạy JAR file
java -jar target\spring-item-service-0.0.1-SNAPSHOT.jar
```

### **Cách 2: Chạy Với Maven (Development Mode)**

```powershell
# Từ thư mục gốc (demo-offloading)
cd d:\4Y2S\SOA\demo_present\demo-offloading

# Chạy với Maven
mvn -pl spring-item-service spring-boot:run
```

### **Verify Service Đang Chạy**

Service sẽ khởi động trên **port 8082**. Xem console output:

```
====================================
✅ Item Service started successfully!
📍 URL: http://localhost:8082
📚 Swagger UI: http://localhost:8082/swagger-ui.html
====================================
```

**Kiểm tra trong terminal khác**:

```powershell
# Test API endpoint
curl.exe http://localhost:8082/api/items
```

**Expected**: Trả về danh sách 5 items (JSON format)

---

## 🧪 Test API

### **Test Nhanh Với cURL**

Mở **PowerShell terminal mới** (giữ service đang chạy ở terminal cũ):

#### **1. Lấy Tất Cả Items**

```powershell
curl.exe http://localhost:8082/api/items
```

#### **2. Lấy Item Theo ID**

```powershell
curl.exe http://localhost:8082/api/items/1
```

#### **3. Tạo Item Mới**

```powershell
curl.exe -X POST http://localhost:8082/api/items `
  -H "Content-Type: application/json" `
  -d '{\"name\":\"Test Item\",\"price\":99.99,\"quantity\":10,\"category\":\"Test\",\"sku\":\"TEST-001\",\"status\":\"ACTIVE\"}'
```

#### **4. Tìm Kiếm Theo Category**

```powershell
curl.exe "http://localhost:8082/api/items/search/category?category=Electronics"
```

#### **5. Tìm Kiếm Theo Price Range**

```powershell
curl.exe "http://localhost:8082/api/items/search/price?minPrice=50&maxPrice=500"
```

#### **6. Lấy Thống Kê**

```powershell
curl.exe http://localhost:8082/api/items/stats
```

### **Test Với Swagger UI**

Mở browser và truy cập:

```
http://localhost:8082/swagger-ui.html
```

Swagger UI cung cấp interactive documentation để test API trực tiếp.

---

## 🤖 Chạy Automated Tests

### **Bước 1: Đảm Bảo Service Đang Chạy**

```powershell
# Kiểm tra service
curl.exe http://localhost:8082/api/items/stats
```

### **Bước 2: Chạy Test Script**

Mở **PowerShell terminal mới**:

```powershell
# Di chuyển vào thư mục project
cd d:\4Y2S\SOA\demo_present\demo-offloading\spring-item-service

# Chạy test script
.\test-item-service.ps1
```

### **Xem Kết Quả**

Test script sẽ chạy 19 test cases và hiển thị kết quả:

```
================================
TEST SUMMARY
================================
Total Tests: 19
Passed: 19 ✅
Failed: 0 ❌
Success Rate: 100%
================================
```

### **Xem Log Files**

```powershell
# Xem log chi tiết
Get-Content test-results.log

# Xem log tóm tắt
Get-Content test-summary.log

# Xem log errors (nếu có)
Get-Content test-errors.log
```

---

## 🛑 Dừng Service

### **Cách 1: Trong Terminal Đang Chạy Service**

Nhấn `Ctrl + C` trong terminal đang chạy service.

### **Cách 2: Kill Process By Port**

```powershell
# Tìm process đang chạy trên port 8082
Get-NetTCPConnection -LocalPort 8082 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess

# Kill process (thay <PID> bằng process ID từ lệnh trên)
Stop-Process -Id <PID> -Force
```

### **Cách 3: Tìm và Kill Java Process**

```powershell
# Tìm tất cả Java processes
Get-Process -Name java

# Kill java process cụ thể
Stop-Process -Name java -Force
```

---

## ⚠️ Troubleshooting

### **Vấn Đề 1: Port 8082 Đã Được Sử Dụng**

**Error**:

```
Address already in use: bind
```

**Solution**:

```powershell
# Tìm và kill process đang dùng port 8082
$process = Get-NetTCPConnection -LocalPort 8082 -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess
Stop-Process -Id $process -Force

# Hoặc đổi port trong application.yml
# server.port: 8082 → 8083
```

---

### **Vấn Đề 2: JAVA_HOME Không Được Thiết Lập**

**Error**:

```
'java' is not recognized as an internal or external command
```

**Solution**:

```powershell
# Kiểm tra JAVA_HOME
$env:JAVA_HOME

# Nếu rỗng, thiết lập JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path += ";$env:JAVA_HOME\bin"

# Verify
java -version
```

---

### **Vấn Đề 3: Maven Build Failed**

**Error**:

```
[ERROR] Failed to execute goal
```

**Solution**:

```powershell
# Clean Maven cache
Remove-Item -Path "$env:USERPROFILE\.m2\repository" -Recurse -Force

# Rebuild
mvn clean install -U
```

---

### **Vấn Đề 4: Items.json Không Được Load**

**Error**: API trả về empty array `[]`

**Solution**:

```powershell
# Verify items.json tồn tại
Test-Path "src\main\resources\items.json"

# Rebuild project
mvn clean package -DskipTests

# Restart service
```

---

### **Vấn Đề 5: Test Script Execution Policy Error**

**Error**:

```
cannot be loaded because running scripts is disabled on this system
```

**Solution**:

```powershell
# Mở PowerShell với quyền Administrator
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Hoặc bypass cho script cụ thể
PowerShell -ExecutionPolicy Bypass -File .\test-item-service.ps1
```

---

### **Vấn Đề 6: Curl Command Not Found**

**Solution**:

```powershell
# Windows 10/11 đã có curl.exe built-in
# Nếu không có, sử dụng Invoke-WebRequest:

Invoke-WebRequest -Uri "http://localhost:8082/api/items" -Method Get
```

---

## 📚 Commands Tham Khảo

### **Maven Commands**

```powershell
# Clean project
mvn clean

# Compile
mvn compile

# Package (tạo JAR)
mvn package

# Package (skip tests)
mvn package -DskipTests

# Run tests
mvn test

# Clean và package
mvn clean package

# Build toàn bộ multi-module project
mvn clean install
```

### **Java Commands**

```powershell
# Chạy JAR file
java -jar target\spring-item-service-0.0.1-SNAPSHOT.jar

# Chạy với custom port
java -jar target\spring-item-service-0.0.1-SNAPSHOT.jar --server.port=8083

# Chạy với profile cụ thể
java -jar target\spring-item-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# Chạy với memory settings
java -Xms512m -Xmx1024m -jar target\spring-item-service-0.0.1-SNAPSHOT.jar
```

### **PowerShell Shortcuts**

```powershell
# Di chuyển vào thư mục nhanh (tạo alias)
$itemServicePath = "d:\4Y2S\SOA\demo_present\demo-offloading\spring-item-service"
function goto-item { cd $itemServicePath }

# Sử dụng:
goto-item
```

### **Batch Script để Start Service**

Tạo file `start-item-service.bat`:

```batch
@echo off
echo Starting Item Service...
cd /d d:\4Y2S\SOA\demo_present\demo-offloading\spring-item-service
java -jar target\spring-item-service-0.0.1-SNAPSHOT.jar
pause
```

Double-click file này để start service.

---

## 🔄 Quick Start (TL;DR)

Nếu đã cài đặt Java và Maven:

```powershell
# 1. Di chuyển vào thư mục
cd d:\4Y2S\SOA\demo_present\demo-offloading\spring-item-service

# 2. Build
mvn clean package -DskipTests

# 3. Chạy
java -jar target\spring-item-service-0.0.1-SNAPSHOT.jar

# 4. Test (terminal mới)
curl.exe http://localhost:8082/api/items

# 5. Run tests (terminal mới)
.\test-item-service.ps1
```

---

## 📊 Service Information

| Property          | Value                                 |
| ----------------- | ------------------------------------- |
| **Service Name**  | Item Service                          |
| **Port**          | 8082                                  |
| **Base URL**      | http://localhost:8082                 |
| **API Base Path** | /api/items                            |
| **Swagger UI**    | http://localhost:8082/swagger-ui.html |
| **Health Check**  | http://localhost:8082/api/items/stats |

---

## 🗂️ Project Structure

```
spring-item-service/
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── Main.java                    # Entry point
│   │   │   ├── controller/
│   │   │   │   └── ItemController.java       # REST endpoints
│   │   │   ├── service/
│   │   │   │   └── ItemService.java          # Business logic
│   │   │   ├── model/
│   │   │   │   └── Item.java                 # Entity
│   │   │   └── repository/
│   │   │       └── ItemRepository.java       # Data access
│   │   └── resources/
│   │       ├── application.yml               # Configuration
│   │       └── items.json                    # Sample data
│   └── test/                                 # Unit tests
├── target/
│   └── spring-item-service-0.0.1-SNAPSHOT.jar  # Compiled JAR
├── pom.xml                                   # Maven config
├── README.md                                 # Documentation
├── TESTING.md                                # Test documentation
├── WINDOWS_SETUP.md                          # This file
├── FIX_PLAN.md                               # Bug fix plan
└── test-item-service.ps1                     # Test script
```

---

## 🌐 API Endpoints Summary

| Method | Endpoint                                            | Description       |
| ------ | --------------------------------------------------- | ----------------- |
| GET    | `/api/items`                                        | Lấy tất cả items  |
| GET    | `/api/items/{id}`                                   | Lấy item theo ID  |
| POST   | `/api/items`                                        | Tạo item mới      |
| PUT    | `/api/items/{id}`                                   | Cập nhật item     |
| DELETE | `/api/items/{id}`                                   | Xóa item          |
| GET    | `/api/items/search/category?category={x}`           | Tìm theo category |
| GET    | `/api/items/search/price?minPrice={x}&maxPrice={y}` | Tìm theo giá      |
| GET    | `/api/items/search/name?name={x}`                   | Tìm theo tên      |
| GET    | `/api/items/stats`                                  | Lấy thống kê      |

---

## 💡 Tips & Best Practices

### **Development Workflow**

1. **Code changes**: Edit code trong IDE
2. **Rebuild**: `mvn clean package -DskipTests`
3. **Restart**: Stop service (Ctrl+C) và start lại
4. **Test**: Chạy test script hoặc test thủ công

### **Performance Tips**

```powershell
# Skip unnecessary rebuilds
mvn compile  # Thay vì mvn package nếu chỉ test code changes

# Enable hot reload (Spring DevTools)
# Đã có trong pom.xml dependencies
```

### **Logging**

```powershell
# Xem logs
Get-Content logs\item-service.log -Tail 50

# Follow logs (real-time)
Get-Content logs\item-service.log -Wait
```

---

## 📞 Support & Documentation

- **Main README**: [README.md](README.md)
- **Testing Guide**: [TESTING.md](TESTING.md)
- **Bug Fixes**: [FIX_PLAN.md](FIX_PLAN.md)
- **API Docs**: http://localhost:8082/swagger-ui.html (khi service chạy)

---

## ✅ Checklist

Trước khi bắt đầu development:

- [ ] Java 21+ đã cài đặt và JAVA_HOME được thiết lập
- [ ] Maven 3.8+ đã cài đặt và MAVEN_HOME được thiết lập
- [ ] PowerShell 5.0+ available
- [ ] Port 8082 available (không bị conflict)
- [ ] items.json có trong src/main/resources/
- [ ] Build thành công: `mvn clean package -DskipTests`
- [ ] Service start thành công
- [ ] API test thành công: `curl.exe http://localhost:8082/api/items`
- [ ] Test suite pass 100%: `.\test-item-service.ps1`

---

**Phiên bản**: 1.0  
**Ngày cập nhật**: March 4, 2026  
**Platform**: Windows 10/11  
**Status**: ✅ Production Ready
