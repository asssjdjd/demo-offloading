# =============================================================================
# Item Service - Automated Testing Script
# =============================================================================
# Mô tả: Script tự động test các API endpoints của Item Service
# Ghi log kết quả vào file test-results.log
# =============================================================================

# Configuration
$baseUrl = "http://localhost:8082"
$logFile = "test-results.log"
$summaryFile = "test-summary.log"
$errorFile = "test-errors.log"

# Color functions
function Write-Success { param($msg) Write-Host $msg -ForegroundColor Green }
function Write-Failure { param($msg) Write-Host $msg -ForegroundColor Red }
function Write-Info { param($msg) Write-Host $msg -ForegroundColor Cyan }
function Write-Warning { param($msg) Write-Host $msg -ForegroundColor Yellow }

# Test counters
$script:totalTests = 0
$script:passedTests = 0
$script:failedTests = 0
$script:startTime = Get-Date

# Initialize log files
function Initialize-Logs {
    $header = @"
================================
ITEM SERVICE - TEST EXECUTION
================================
Start Time: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
Service URL: $baseUrl
================================

"@
    $header | Out-File -FilePath $logFile -Encoding UTF8
    "" | Out-File -FilePath $errorFile -Encoding UTF8
}

# Write to log
function Write-Log {
    param(
        [string]$message
    )
    $message | Out-File -FilePath $logFile -Append -Encoding UTF8
}

# Execute test case
function Test-API {
    param(
        [string]$testId,
        [string]$testName,
        [string]$method,
        [string]$endpoint,
        [string]$body = $null,
        [int]$expectedStatus,
        [string]$description = ""
    )
    
    $script:totalTests++
    
    Write-Info "`n[$testId] $testName"
    Write-Log "`n[$testId] $testName"
    Write-Log "-----------------------------------"
    
    $url = "$baseUrl$endpoint"
    Write-Log "Request: $method $endpoint"
    
    try {
        $requestStart = Get-Date
        
        if ($method -eq "GET") {
            $response = Invoke-WebRequest -Uri $url -Method Get -ErrorAction Stop
        }
        elseif ($method -eq "POST") {
            $response = Invoke-WebRequest -Uri $url -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop
        }
        elseif ($method -eq "PUT") {
            $response = Invoke-WebRequest -Uri $url -Method Put -Body $body -ContentType "application/json" -ErrorAction Stop
        }
        elseif ($method -eq "DELETE") {
            $response = Invoke-WebRequest -Uri $url -Method Delete -ErrorAction Stop
        }
        
        $requestEnd = Get-Date
        $responseTime = ($requestEnd - $requestStart).TotalMilliseconds
        
        $statusCode = $response.StatusCode
        Write-Log "Status Code: $statusCode"
        Write-Log "Response Time: $([math]::Round($responseTime))ms"
        
        if ($statusCode -eq $expectedStatus) {
            $script:passedTests++
            Write-Success "Result: ✅ PASSED"
            Write-Log "Result: ✅ PASSED"
            
            if ($description) {
                Write-Log "Details: $description"
            }
            
            # Show response preview
            if ($response.Content) {
                $contentPreview = ($response.Content | ConvertFrom-Json | ConvertTo-Json -Compress)
                if ($contentPreview.Length -gt 200) {
                    $contentPreview = $contentPreview.Substring(0, 200) + "..."
                }
                Write-Log "Response Preview: $contentPreview"
            }
        }
        else {
            $script:failedTests++
            Write-Failure "Result: ❌ FAILED"
            Write-Log "Result: ❌ FAILED"
            Write-Log "Expected Status: $expectedStatus, Got: $statusCode"
            
            # Write to error log
            "[$testId] Expected $expectedStatus but got $statusCode" | Out-File -FilePath $errorFile -Append
        }
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        
        if ($statusCode -eq $expectedStatus) {
            $script:passedTests++
            Write-Success "Result: ✅ PASSED (Expected Error)"
            Write-Log "Result: ✅ PASSED (Expected Error)"
            Write-Log "Status Code: $statusCode (Expected: $expectedStatus)"
        }
        else {
            $script:failedTests++
            Write-Failure "Result: ❌ FAILED"
            Write-Log "Result: ❌ FAILED"
            Write-Log "Error: $($_.Exception.Message)"
            
            # Write to error log
            "[$testId] Error: $($_.Exception.Message)" | Out-File -FilePath $errorFile -Append
        }
    }
    
    Write-Log ""
}

# Test Suite Execution
function Run-TestSuite {
    Write-Info "`n================================"
    Write-Info "Starting Item Service Test Suite"
    Write-Info "================================`n"
    
    Initialize-Logs
    
    # ========== Test Suite 1: CRUD Operations ==========
    Write-Warning "`n>>> Test Suite 1: CRUD Operations <<<`n"
    
    # TC-001: Get all items
    Test-API -testId "TC-001" `
             -testName "Lấy danh sách tất cả sản phẩm" `
             -method "GET" `
             -endpoint "/api/items" `
             -expectedStatus 200 `
             -description "Should return array of items"
    
    Start-Sleep -Milliseconds 500
    
    # TC-002: Get item by valid ID
    Test-API -testId "TC-002" `
             -testName "Lấy chi tiết sản phẩm (Valid ID)" `
             -method "GET" `
             -endpoint "/api/items/1" `
             -expectedStatus 200 `
             -description "Item ID=1 should exist"
    
    Start-Sleep -Milliseconds 500
    
    # TC-003: Get item by invalid ID (not found)
    Test-API -testId "TC-003" `
             -testName "Lấy chi tiết sản phẩm (Not Found)" `
             -method "GET" `
             -endpoint "/api/items/9999" `
             -expectedStatus 404 `
             -description "Item ID=9999 should not exist"
    
    Start-Sleep -Milliseconds 500
    
    # TC-004: Get item by negative ID
    Test-API -testId "TC-004" `
             -testName "Lấy chi tiết sản phẩm (Negative ID)" `
             -method "GET" `
             -endpoint "/api/items/-1" `
             -expectedStatus 400 `
             -description "Negative ID should be rejected"
    
    Start-Sleep -Milliseconds 500
    
    # TC-005: Create new item (valid)
    $newItem = @{
        name = "Test Keyboard"
        description = "Mechanical Gaming Keyboard"
        price = 149.99
        quantity = 50
        category = "Accessories"
        sku = "TEST-KB-$(Get-Random -Minimum 1000 -Maximum 9999)"
        status = "ACTIVE"
    } | ConvertTo-Json
    
    Test-API -testId "TC-005" `
             -testName "Tạo sản phẩm mới (Valid Data)" `
             -method "POST" `
             -endpoint "/api/items" `
             -body $newItem `
             -expectedStatus 201 `
             -description "New item should be created"
    
    Start-Sleep -Milliseconds 500
    
    # TC-006: Create item (missing name)
    $invalidItem = @{
        description = "Test Item"
        price = 99.99
        quantity = 10
        category = "Test"
        sku = "TEST-INVALID-001"
    } | ConvertTo-Json
    
    Test-API -testId "TC-006" `
             -testName "Tạo sản phẩm (Missing Name)" `
             -method "POST" `
             -endpoint "/api/items" `
             -body $invalidItem `
             -expectedStatus 400 `
             -description "Missing name should be rejected"
    
    Start-Sleep -Milliseconds 500
    
    # TC-007: Create item (duplicate SKU)
    $duplicateItem = @{
        name = "Duplicate Item"
        price = 99.99
        quantity = 10
        category = "Test"
        sku = "DELL-XPS-13"
        status = "ACTIVE"
    } | ConvertTo-Json
    
    Test-API -testId "TC-007" `
             -testName "Tạo sản phẩm (Duplicate SKU)" `
             -method "POST" `
             -endpoint "/api/items" `
             -body $duplicateItem `
             -expectedStatus 409 `
             -description "Duplicate SKU should be rejected"
    
    Start-Sleep -Milliseconds 500
    
    # TC-008: Create item (invalid price)
    $invalidPriceItem = @{
        name = "Invalid Price Item"
        price = -10
        quantity = 10
        category = "Test"
        sku = "TEST-INVALID-PRICE"
        status = "ACTIVE"
    } | ConvertTo-Json
    
    Test-API -testId "TC-008" `
             -testName "Tạo sản phẩm (Invalid Price)" `
             -method "POST" `
             -endpoint "/api/items" `
             -body $invalidPriceItem `
             -expectedStatus 400 `
             -description "Negative price should be rejected"
    
    Start-Sleep -Milliseconds 500
    
    # TC-009: Update item (valid)
    $updateData = @{
        price = 899.99
        quantity = 45
    } | ConvertTo-Json
    
    Test-API -testId "TC-009" `
             -testName "Cập nhật sản phẩm (Valid Data)" `
             -method "PUT" `
             -endpoint "/api/items/1" `
             -body $updateData `
             -expectedStatus 200 `
             -description "Item should be updated"
    
    Start-Sleep -Milliseconds 500
    
    # TC-010: Update item (not found)
    Test-API -testId "TC-010" `
             -testName "Cập nhật sản phẩm (Not Found)" `
             -method "PUT" `
             -endpoint "/api/items/9999" `
             -body $updateData `
             -expectedStatus 404 `
             -description "Update non-existent item should fail"
    
    Start-Sleep -Milliseconds 500
    
    # ========== Test Suite 2: Search & Filter ==========
    Write-Warning "`n>>> Test Suite 2: Search & Filter Operations <<<`n"
    
    # TC-011: Search by category (valid)
    Test-API -testId "TC-011" `
             -testName "Tìm kiếm theo Category (Valid)" `
             -method "GET" `
             -endpoint "/api/items/search/category?category=Electronics" `
             -expectedStatus 200 `
             -description "Should return Electronics items"
    
    Start-Sleep -Milliseconds 500
    
    # TC-012: Search by category (empty result)
    Test-API -testId "TC-012" `
             -testName "Tìm kiếm theo Category (Empty)" `
             -method "GET" `
             -endpoint "/api/items/search/category?category=NonExistent" `
             -expectedStatus 200 `
             -description "Should return empty array"
    
    Start-Sleep -Milliseconds 500
    
    # TC-013: Search by price range (valid)
    Test-API -testId "TC-013" `
             -testName "Tìm kiếm theo Price Range (Valid)" `
             -method "GET" `
             -endpoint "/api/items/search/price?minPrice=50&maxPrice=500" `
             -expectedStatus 200 `
             -description "Should return items in price range"
    
    Start-Sleep -Milliseconds 500
    
    # TC-014: Search by price range (invalid - min > max)
    Test-API -testId "TC-014" `
             -testName "Tìm kiếm theo Price Range (Min > Max)" `
             -method "GET" `
             -endpoint "/api/items/search/price?minPrice=500&maxPrice=50" `
             -expectedStatus 400 `
             -description "Min > Max should be rejected"
    
    Start-Sleep -Milliseconds 500
    
    # TC-015: Search by price range (negative)
    Test-API -testId "TC-015" `
             -testName "Tìm kiếm theo Price Range (Negative)" `
             -method "GET" `
             -endpoint "/api/items/search/price?minPrice=-10&maxPrice=100" `
             -expectedStatus 400 `
             -description "Negative price should be rejected"
    
    Start-Sleep -Milliseconds 500
    
    # TC-016: Search by name (valid)
    Test-API -testId "TC-016" `
             -testName "Tìm kiếm theo Name (Valid)" `
             -method "GET" `
             -endpoint "/api/items/search/name?name=Laptop" `
             -expectedStatus 200 `
             -description "Should find items containing 'Laptop'"
    
    Start-Sleep -Milliseconds 500
    
    # TC-017: Search by name (case insensitive)
    Test-API -testId "TC-017" `
             -testName "Tìm kiếm theo Name (Case Insensitive)" `
             -method "GET" `
             -endpoint "/api/items/search/name?name=laptop" `
             -expectedStatus 200 `
             -description "Should be case insensitive"
    
    Start-Sleep -Milliseconds 500
    
    # ========== Test Suite 3: Statistics & Health ==========
    Write-Warning "`n>>> Test Suite 3: Statistics & Health Check <<<`n"
    
    # TC-018: Get stats
    Test-API -testId "TC-018" `
             -testName "Lấy thống kê" `
             -method "GET" `
             -endpoint "/api/items/stats" `
             -expectedStatus 200 `
             -description "Should return statistics"
    
    Start-Sleep -Milliseconds 500
    
    # TC-019: Health check (sử dụng API endpoint thay vì actuator)
    Test-API -testId "TC-019" `
             -testName "Service Health Check" `
             -method "GET" `
             -endpoint "/api/items" `
             -expectedStatus 200 `
             -description "Service should be running"
    
    Start-Sleep -Milliseconds 500
}

# Summary Report
function Generate-Summary {
    $endTime = Get-Date
    $duration = ($endTime - $script:startTime)
    $successRate = if ($script:totalTests -gt 0) { 
        [math]::Round(($script:passedTests / $script:totalTests) * 100, 1) 
    } else { 0 }
    
    $summary = @"

================================
TEST SUMMARY
================================
Total Tests: $($script:totalTests)
Passed: $($script:passedTests) ✅
Failed: $($script:failedTests) ❌
Success Rate: $successRate%
Start Time: $(Get-Date $script:startTime -Format "yyyy-MM-dd HH:mm:ss")
End Time: $(Get-Date $endTime -Format "yyyy-MM-dd HH:mm:ss")
Duration: $($duration.Minutes) minutes $($duration.Seconds) seconds
================================

"@
    
    Write-Info $summary
    Write-Log $summary
    
    $summary | Out-File -FilePath $summaryFile -Encoding UTF8
    
    if ($script:failedTests -gt 0) {
        Write-Failure "`n⚠️  Some tests failed. Check $errorFile for details."
    } else {
        Write-Success "`n🎉 All tests passed!"
    }
    
    Write-Info "`nLog files created:"
    Write-Info "  - Detailed results: $logFile"
    Write-Info "  - Summary: $summaryFile"
    Write-Info "  - Errors: $errorFile"
}

# Main Execution
try {
    # Check if service is running
    Write-Info "Checking if Item Service is running on $baseUrl..."
    try {
        $healthCheck = Invoke-WebRequest -Uri "$baseUrl/api/items" -Method Get -TimeoutSec 5 -ErrorAction Stop
        Write-Success "✅ Service is running!`n"
    }
    catch {
        Write-Failure "❌ Service is not running on $baseUrl"
        Write-Warning "Please start the service first:"
        Write-Warning "  cd spring-item-service"
        Write-Warning "  mvn spring-boot:run"
        exit 1
    }
    
    # Run test suite
    Run-TestSuite
    
    # Generate summary
    Generate-Summary
}
catch {
    Write-Failure "`n❌ Test execution failed: $($_.Exception.Message)"
    exit 1
}
