# Group 2 Performance & Reliability Test Suite - Multi-Service (User + Order + Item)
param(
    [string]$OutputFile = "TEST_RESULTS.md"
)

# =============================================================================
# CONFIGURATION
# =============================================================================
$ADMIN_TOKEN = "eyJhbGciOiAiSFMyNTYiLCAidHlwIjogIkpXVCJ9.eyJpc3MiOiAiYWRtaW4taXNzdWVyIiwgImV4cCI6IDE3NzI5NzkzOTAsICJpYXQiOiAxNzcyODkyOTkwfQ.7BKYDNvFpCm51bYUuyPeT0oGKs1_wCBmp0STI6ufsug"
$USER_TOKEN  = "eyJhbGciOiAiSFMyNTYiLCAidHlwIjogIkpXVCJ9.eyJpc3MiOiAidXNlci1pc3N1ZXIiLCAiZXhwIjogMTc3Mjk3OTM5MCwgImlhdCI6IDE3NzI4OTI5OTB9.BxQHSEd-6PcC6HqI4w4T6WH865-jwXuC5JeDKQHMMzc"

$KONG_PROXY = "http://localhost:8000"
$KONG_ADMIN = "http://localhost:8001"

$results     = @()
$testsPassed = 0
$testsFailed = 0

function Add-TestResult {
    param([string]$TestName, [bool]$Passed, [string]$Message, [object]$Details)
    if ($Passed) { $script:testsPassed++ } else { $script:testsFailed++ }
    $script:results += @{
        Name    = $TestName
        Status  = if ($Passed) { "[PASS]" } else { "[FAIL]" }
        Message = $Message
        Details = $Details
    }
}

function Invoke-KongGet {
    param([string]$Url, [hashtable]$Headers = @{})
    $req = [System.Net.HttpWebRequest]::Create($Url)
    $req.Method  = "GET"
    $req.Timeout = 10000
    foreach ($k in $Headers.Keys) { $req.Headers.Add($k, $Headers[$k]) }
    try {
        $resp = $req.GetResponse()
        $code = [int]$resp.StatusCode
        $hdrs = $resp.Headers
        $body = (New-Object System.IO.StreamReader $resp.GetResponseStream()).ReadToEnd()
        $resp.Close()
        return @{ Code = $code; Headers = $hdrs; Body = $body; Error = $null }
    } catch [System.Net.WebException] {
        $we = $_.Exception
        if ($we.Response) {
            $code = [int]$we.Response.StatusCode
            $hdrs = $we.Response.Headers
            $we.Response.Close()
            return @{ Code = $code; Headers = $hdrs; Body = $null; Error = $we.Message }
        }
        return @{ Code = 0; Headers = $null; Body = $null; Error = $we.Message }
    }
}

Write-Host "============================================================"
Write-Host "  GROUP 2 - MULTI-SERVICE PERFORMANCE AND RELIABILITY TESTS"
Write-Host "  Services: user-service | order-service | item-service"
Write-Host "============================================================"
Write-Host ""

# ---------------------------------------------------------------------------
# SECTION 1: KONG GATEWAY INFRASTRUCTURE
# ---------------------------------------------------------------------------
Write-Host "--- SECTION 1: KONG GATEWAY INFRASTRUCTURE ---"
Write-Host ""

# TEST 1: Kong Admin API
Write-Host "TEST 1: Kong Admin API Health"
try {
    $r    = Invoke-WebRequest -Uri "$KONG_ADMIN" -UseBasicParsing -ErrorAction Stop
    $data = $r.Content | ConvertFrom-Json
    Add-TestResult -TestName "T1: Kong Admin API" -Passed $true -Message "Admin API accessible (v$($data.version))" -Details "Status $($r.StatusCode)"
    Write-Host "  [PASS] Kong $($data.version) running"
} catch {
    Add-TestResult -TestName "T1: Kong Admin API" -Passed $false -Message "Admin API failed" -Details $_.Exception.Message
    Write-Host "  [FAIL] $($_.Exception.Message)"
}

# TEST 2: All 3 Upstreams Configured
Write-Host ""
Write-Host "TEST 2: All 3 Upstreams Configured (user / order / item)"
try {
    $r    = Invoke-WebRequest -Uri "$KONG_ADMIN/upstreams/" -UseBasicParsing -ErrorAction Stop
    $data = $r.Content | ConvertFrom-Json
    $names = $data.data | Select-Object -ExpandProperty name

    $hasUser  = $names -contains "user-service-upstream"
    $hasOrder = $names -contains "order-service-upstream"
    $hasItem  = $names -contains "item-service-upstream"
    $allFound = $hasUser -and $hasOrder -and $hasItem

    Add-TestResult -TestName "T2: Three Upstreams Exist" -Passed $allFound `
        -Message "user=$hasUser order=$hasOrder item=$hasItem" -Details ($names -join ", ")
    $mark = if ($allFound) { "[PASS]" } else { "[FAIL]" }
    Write-Host "  $mark user=$hasUser  order=$hasOrder  item=$hasItem"
} catch {
    Add-TestResult -TestName "T2: Three Upstreams Exist" -Passed $false -Message "Upstream list failed" -Details $_.Exception.Message
    Write-Host "  [FAIL] $($_.Exception.Message)"
}

# TEST 3: Upstream target health - one sub-test per upstream
Write-Host ""
Write-Host "TEST 3: Upstream Target Health"
@(
    @{ Name="user-service-upstream";  Label="user"  },
    @{ Name="order-service-upstream"; Label="order" },
    @{ Name="item-service-upstream";  Label="item"  }
) | ForEach-Object {
    $upName = $_.Name; $label = $_.Label
    try {
        $r    = Invoke-WebRequest -Uri "$KONG_ADMIN/upstreams/$upName/health/" -UseBasicParsing -ErrorAction Stop
        $data = $r.Content | ConvertFrom-Json
        $tgt  = $data.data[0]
        $h    = if ($tgt) { $tgt.health } else { "N/A" }
        $pass = $h -eq "HEALTHY"
        Add-TestResult -TestName "T3: Target Health ($label)" -Passed $pass -Message "$upName -> $h" -Details $tgt
        $mark = if ($pass) { "[PASS]" } else { "[FAIL]" }
        Write-Host "  $mark [$label] $upName -> $h"
    } catch {
        Add-TestResult -TestName "T3: Target Health ($label)" -Passed $false -Message "Health check failed" -Details $_.Exception.Message
        Write-Host "  [FAIL] [$label] $($_.Exception.Message)"
    }
}

# TEST 4: Required plugins
Write-Host ""
Write-Host "TEST 4: Required Plugins Loaded (proxy-cache x3, circuit-breaker x3, jwt)"
try {
    $r       = Invoke-WebRequest -Uri "$KONG_ADMIN/plugins?size=100" -UseBasicParsing -ErrorAction Stop
    $plugins = ($r.Content | ConvertFrom-Json).data

    $pcCount = ($plugins | Where-Object { $_.name -eq "proxy-cache"     }).Count
    $cbCount = ($plugins | Where-Object { $_.name -eq "circuit-breaker" }).Count
    $jwtCnt  = ($plugins | Where-Object { $_.name -eq "jwt"             }).Count

    $pcOk  = $pcCount -ge 3
    $cbOk  = $cbCount -ge 3
    $jwtOk = $jwtCnt  -ge 1

    Add-TestResult -TestName "T4: proxy-cache (x3)"     -Passed $pcOk  -Message "Found $pcCount proxy-cache plugin(s)"     -Details "need >=3"
    Add-TestResult -TestName "T4: circuit-breaker (x3)" -Passed $cbOk  -Message "Found $cbCount circuit-breaker plugin(s)" -Details "need >=3"
    Add-TestResult -TestName "T4: jwt (>=1)"            -Passed $jwtOk -Message "Found $jwtCnt jwt plugin(s)"              -Details "need >=1"

    Write-Host "  $(if($pcOk){'[PASS]'}else{'[FAIL]'}) proxy-cache: $pcCount / 3"
    Write-Host "  $(if($cbOk){'[PASS]'}else{'[FAIL]'}) circuit-breaker: $cbCount / 3"
    Write-Host "  $(if($jwtOk){'[PASS]'}else{'[FAIL]'}) jwt: $jwtCnt"
} catch {
    Add-TestResult -TestName "T4: Plugins Check" -Passed $false -Message "Plugin list failed" -Details $_.Exception.Message
    Write-Host "  [FAIL] $($_.Exception.Message)"
}

# ---------------------------------------------------------------------------
# SECTION 2: JWT AUTHENTICATION (user-service)
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "--- SECTION 2: JWT AUTHENTICATION (user-service) ---"
Write-Host ""

Write-Host "TEST 5: No Token -> 401 Unauthorized"
$r5    = Invoke-KongGet "$KONG_PROXY/api/v1/users"
$pass5 = $r5.Code -eq 401
Add-TestResult -TestName "T5: No JWT -> 401" -Passed $pass5 -Message "Got HTTP $($r5.Code)" -Details $r5.Error
Write-Host "  $(if($pass5){'[PASS]'}else{'[FAIL]'}) Status $($r5.Code)"

Write-Host ""
Write-Host "TEST 6: Admin Token -> 200 OK"
$r6    = Invoke-KongGet "$KONG_PROXY/api/v1/users" @{ "Authorization" = "Bearer $ADMIN_TOKEN" }
$pass6 = $r6.Code -eq 200
Add-TestResult -TestName "T6: Admin JWT -> 200" -Passed $pass6 -Message "Got HTTP $($r6.Code)" -Details "cache=$($r6.Headers['X-Cache-Status'])"
Write-Host "  $(if($pass6){'[PASS]'}else{'[FAIL]'}) Status $($r6.Code)  cache=$($r6.Headers['X-Cache-Status'])"

Write-Host ""
Write-Host "TEST 7: User Token -> 200 OK"
$r7    = Invoke-KongGet "$KONG_PROXY/api/v1/users" @{ "Authorization" = "Bearer $USER_TOKEN" }
$pass7 = $r7.Code -eq 200
Add-TestResult -TestName "T7: User JWT -> 200" -Passed $pass7 -Message "Got HTTP $($r7.Code)" -Details "cache=$($r7.Headers['X-Cache-Status'])"
Write-Host "  $(if($pass7){'[PASS]'}else{'[FAIL]'}) Status $($r7.Code)"

# ---------------------------------------------------------------------------
# SECTION 3: SERVICE REACHABILITY (all 3)
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "--- SECTION 3: SERVICE REACHABILITY ---"
Write-Host ""

Write-Host "TEST 8: Order service accessible (no JWT)"
$r8    = Invoke-KongGet "$KONG_PROXY/api/orders"
$pass8 = $r8.Code -eq 200
Add-TestResult -TestName "T8: Order service -> 200" -Passed $pass8 -Message "GET /api/orders HTTP $($r8.Code)" -Details "cache=$($r8.Headers['X-Cache-Status'])"
Write-Host "  $(if($pass8){'[PASS]'}else{'[FAIL]'}) Status $($r8.Code)"

Write-Host ""
Write-Host "TEST 9: Item service accessible (no JWT)"
$r9    = Invoke-KongGet "$KONG_PROXY/api/items"
$pass9 = $r9.Code -eq 200
Add-TestResult -TestName "T9: Item service -> 200" -Passed $pass9 -Message "GET /api/items HTTP $($r9.Code)" -Details "cache=$($r9.Headers['X-Cache-Status'])"
Write-Host "  $(if($pass9){'[PASS]'}else{'[FAIL]'}) Status $($r9.Code)"

# ---------------------------------------------------------------------------
# SECTION 4: PROXY CACHE (all 3 services)
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "--- SECTION 4: PROXY CACHE (TTL=30s) ---"
Write-Host ""

# user-service cache
Write-Host "TEST 10+11: User-service Cache MISS then HIT"
$sw  = [System.Diagnostics.Stopwatch]::StartNew()
$u10 = Invoke-KongGet "$KONG_PROXY/api/v1/users" @{ "Authorization" = "Bearer $USER_TOKEN" }
$sw.Stop(); $t10 = $sw.ElapsedMilliseconds; $c10 = $u10.Headers['X-Cache-Status']
$pass10 = $u10.Code -eq 200
Add-TestResult -TestName "T10: User cache 1st req" -Passed $pass10 -Message "${t10}ms X-Cache-Status=$c10" -Details "HTTP $($u10.Code)"
Write-Host "  $(if($pass10){'[PASS]'}else{'[FAIL]'}) ${t10}ms  cache=$c10"

Start-Sleep -Milliseconds 400
$sw  = [System.Diagnostics.Stopwatch]::StartNew()
$u11 = Invoke-KongGet "$KONG_PROXY/api/v1/users" @{ "Authorization" = "Bearer $USER_TOKEN" }
$sw.Stop(); $t11 = $sw.ElapsedMilliseconds; $c11 = $u11.Headers['X-Cache-Status']
$pass11 = ($u11.Code -eq 200) -and ($c11 -eq "Hit")
Add-TestResult -TestName "T11: User cache HIT" -Passed $pass11 -Message "${t11}ms X-Cache-Status=$c11 (was ${t10}ms)" -Details "HTTP $($u11.Code)"
Write-Host "  $(if($pass11){'[PASS]'}else{'[FAIL]'}) ${t11}ms  cache=$c11"

# order-service cache
Write-Host ""
Write-Host "TEST 12+13: Order-service Cache MISS then HIT"
$sw  = [System.Diagnostics.Stopwatch]::StartNew()
$o12 = Invoke-KongGet "$KONG_PROXY/api/orders"
$sw.Stop(); $t12 = $sw.ElapsedMilliseconds; $c12 = $o12.Headers['X-Cache-Status']
$pass12 = $o12.Code -eq 200
Add-TestResult -TestName "T12: Order cache 1st req" -Passed $pass12 -Message "${t12}ms X-Cache-Status=$c12" -Details "HTTP $($o12.Code)"
Write-Host "  $(if($pass12){'[PASS]'}else{'[FAIL]'}) ${t12}ms  cache=$c12"

Start-Sleep -Milliseconds 400
$sw  = [System.Diagnostics.Stopwatch]::StartNew()
$o13 = Invoke-KongGet "$KONG_PROXY/api/orders"
$sw.Stop(); $t13 = $sw.ElapsedMilliseconds; $c13 = $o13.Headers['X-Cache-Status']
$pass13 = ($o13.Code -eq 200) -and ($c13 -eq "Hit")
Add-TestResult -TestName "T13: Order cache HIT" -Passed $pass13 -Message "${t13}ms X-Cache-Status=$c13 (was ${t12}ms)" -Details "HTTP $($o13.Code)"
Write-Host "  $(if($pass13){'[PASS]'}else{'[FAIL]'}) ${t13}ms  cache=$c13"

# item-service cache
Write-Host ""
Write-Host "TEST 14+15: Item-service Cache MISS then HIT"
$sw  = [System.Diagnostics.Stopwatch]::StartNew()
$i14 = Invoke-KongGet "$KONG_PROXY/api/items"
$sw.Stop(); $t14 = $sw.ElapsedMilliseconds; $c14 = $i14.Headers['X-Cache-Status']
$pass14 = $i14.Code -eq 200
Add-TestResult -TestName "T14: Item cache 1st req" -Passed $pass14 -Message "${t14}ms X-Cache-Status=$c14" -Details "HTTP $($i14.Code)"
Write-Host "  $(if($pass14){'[PASS]'}else{'[FAIL]'}) ${t14}ms  cache=$c14"

Start-Sleep -Milliseconds 400
$sw  = [System.Diagnostics.Stopwatch]::StartNew()
$i15 = Invoke-KongGet "$KONG_PROXY/api/items"
$sw.Stop(); $t15 = $sw.ElapsedMilliseconds; $c15 = $i15.Headers['X-Cache-Status']
$pass15 = ($i15.Code -eq 200) -and ($c15 -eq "Hit")
Add-TestResult -TestName "T15: Item cache HIT" -Passed $pass15 -Message "${t15}ms X-Cache-Status=$c15 (was ${t14}ms)" -Details "HTTP $($i15.Code)"
Write-Host "  $(if($pass15){'[PASS]'}else{'[FAIL]'}) ${t15}ms  cache=$c15"

# Cache TTL expiry
Write-Host ""
Write-Host "TEST 16: Cache TTL Expiration (waiting 35s for TTL=30s to expire...)"
Start-Sleep -Seconds 35
$sw  = [System.Diagnostics.Stopwatch]::StartNew()
$u16 = Invoke-KongGet "$KONG_PROXY/api/v1/users" @{ "Authorization" = "Bearer $USER_TOKEN" }
$sw.Stop(); $t16 = $sw.ElapsedMilliseconds; $c16 = $u16.Headers['X-Cache-Status']
$pass16 = ($c16 -ne "Hit")
Add-TestResult -TestName "T16: Cache TTL Expired (user)" -Passed $pass16 -Message "${t16}ms X-Cache-Status=$c16" -Details "After 35s TTL expiry"
Write-Host "  $(if($pass16){'[PASS]'}else{'[FAIL]'}) ${t16}ms  cache=$c16"

# ---------------------------------------------------------------------------
# SECTION 5: CIRCUIT BREAKER - CLOSED state (normal)
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "--- SECTION 5: CIRCUIT BREAKER - CLOSED (normal operation) ---"
Write-Host ""

function Test-CBClosed {
    param([string]$Label, [string]$Url, [hashtable]$Headers = @{})
    Write-Host "TEST CB-CLOSED [$Label]"
    $pass = $true; $details = @()
    for ($i = 1; $i -le 3; $i++) {
        $r       = Invoke-KongGet $Url $Headers
        $cbState = if ($r.Headers) { $r.Headers['X-CircuitBreaker-State'] } else { "N/A" }
        if ($r.Code -ne 200) { $pass = $false }
        $details += "Req${i}: HTTP $($r.Code) CB=$cbState"
        Write-Host "    Req ${i}: HTTP $($r.Code)  CB=$cbState"
    }
    Add-TestResult -TestName "T-CB-CLOSED [$Label]" -Passed $pass -Message "3 requests all 200" -Details ($details -join " | ")
    Write-Host "  $(if($pass){'[PASS]'}else{'[FAIL]'})"
}

Test-CBClosed -Label "user"  -Url "$KONG_PROXY/api/v1/users" -Headers @{ "Authorization" = "Bearer $ADMIN_TOKEN" }
Write-Host ""
Test-CBClosed -Label "order" -Url "$KONG_PROXY/api/orders"
Write-Host ""
Test-CBClosed -Label "item"  -Url "$KONG_PROXY/api/items"

# ---------------------------------------------------------------------------
# SECTION 6: CIRCUIT BREAKER - OPEN trigger (order-service)
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "--- SECTION 6: CIRCUIT BREAKER - OPEN (stop order-service) ---"
Write-Host ""
Write-Host "TEST 20: Stopping order-service, sending 8 cache-busted requests..."

docker stop spring-order-service | Out-Null
Start-Sleep -Seconds 3

$cbOpened  = $false
$failCount = 0
for ($i = 1; $i -le 8; $i++) {
    $ts      = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + $i
    $r       = Invoke-KongGet "$KONG_PROXY/api/orders?_cb=$ts"
    $cbState = if ($r.Headers) { $r.Headers['X-CircuitBreaker-State'] } else { "N/A" }
    if ($r.Code -ne 200) { $failCount++ }
    if ($cbState -eq "open" -or $r.Code -eq 503) { $cbOpened = $true }
    Write-Host "    Req ${i} : HTTP $($r.Code)  CB=$cbState"
    Start-Sleep -Milliseconds 600
}

Add-TestResult -TestName "T20: Circuit Breaker OPEN (order)" -Passed $cbOpened `
    -Message "$failCount/8 requests failed; CB opened=$cbOpened" -Details "Stopped order-service, sent 8 cache-busted requests"
Write-Host "  $(if($cbOpened){'[PASS]'}else{'[FAIL] (CB may need more failures)'})"

# Restore - need 35s for service to boot AND circuit open_timeout_ms(30s) to expire
Write-Host ""
Write-Host "  Restarting order-service (waiting 35s for circuit OPEN->HALF-OPEN)..."
docker start spring-order-service | Out-Null
Start-Sleep -Seconds 35

# Make up to 4 attempts - first 2 may still be HALF-OPEN (need 2 successes to CLOSE)
$restored = $false
for ($j = 1; $j -le 4; $j++) {
    $ts = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + $j
    $rCheck   = Invoke-KongGet "$KONG_PROXY/api/orders?_r=$ts"
    $cbState  = if ($rCheck.Headers) { $rCheck.Headers['X-CircuitBreaker-State'] } else { "N/A" }
    Write-Host "    Recovery check ${j}: HTTP $($rCheck.Code)  CB=$cbState"
    if ($rCheck.Code -eq 200) { $restored = $true }
    Start-Sleep -Seconds 2
}
Add-TestResult -TestName "T21: Order service restored" -Passed $restored `
    -Message "POST-recovery HTTP $($rCheck.Code)" -Details "After restart + 35s CB cool-down"
Write-Host "  $(if($restored){'[PASS]'}else{'[FAIL]'}) order-service HTTP $($rCheck.Code) after recovery"

# =============================================================================
# GENERATE MARKDOWN REPORT
# =============================================================================
Write-Host ""
Write-Host "============================================================"
Write-Host "Generating report: $OutputFile"
Write-Host "============================================================"

$total = $testsPassed + $testsFailed
$pct   = if ($total -gt 0) { [math]::Round(($testsPassed / $total) * 100, 1) } else { 0 }
$ts    = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

$md  = "# Group 2 - Multi-Service Performance and Reliability Test Results`n`n"
$md += "**Generated**: $ts  `n"
$md += "**Services Tested**: user-service | order-service | item-service  `n"
$md += "**Kong Version**: 3.9 (DB-less declarative)`n`n"
$md += "## Summary`n`n"
$md += "| Metric | Value |`n|--------|-------|`n"
$md += "| Total Tests | $total |`n"
$md += "| Passed | $testsPassed |`n"
$md += "| Failed | $testsFailed |`n"
$md += "| Success Rate | ${pct}% |`n`n"
$md += "---`n`n## Test Results`n"

foreach ($r in $results) {
    $md += "`n### $($r.Status) - $($r.Name)`n"
    $md += "- **Message**: $($r.Message)`n"
    if ($r.Details) { $md += "- **Details**: $($r.Details)`n" }
}

$md += "`n---`n`n## Coverage Matrix`n`n"
$md += "| Feature | user-service | order-service | item-service |`n"
$md += "|---------|:---:|:---:|:---:|`n"
$md += "| Route reachable | YES | YES | YES |`n"
$md += "| JWT auth required | YES | no | no |`n"
$md += "| Proxy cache MISS-HIT | YES | YES | YES |`n"
$md += "| Cache TTL expiry | YES | - | - |`n"
$md += "| Circuit breaker CLOSED | YES | YES | YES |`n"
$md += "| Circuit breaker OPEN | - | YES | - |`n"
$md += "| Upstream health check | YES | YES | YES |`n"
$md += "`n---`n`n## Kong Configuration`n`n"
$md += "- **Load Balancing**: round-robin on all 3 upstreams`n"
$md += "- **Proxy Cache**: TTL=30s, strategy=memory`n"
$md += "- **Circuit Breaker**: failure_threshold=5, success_threshold=2, timeout=30s`n"
$md += "- **JWT (user-service only)**: HS256, admin-issuer + user-issuer`n"
$md += "`n---`n`n## Issues Found and Fixed`n`n"
$md += "1. **Missing Dockerfiles** - Created Dockerfile for order-service and item-service`n"
$md += "2. **Missing docker-compose entries** - Added order-service (8081) and item-service (8082)`n"
$md += "3. **Unknown database 'orderdb'** - Fixed with createDatabaseIfNotExist=true in JDBC URL`n"
$md += "4. **Healthcheck 404** - Fixed docker-compose healthchecks to use /api/orders and /api/items`n"
$md += "5. **Kong ring-balancer failure** - Fixed Kong upstream http_path from /actuator/health to actual API paths`n"

$md | Out-File -FilePath $OutputFile -Encoding UTF8
Write-Host "[OK] Saved: $OutputFile"

Write-Host ""
Write-Host "============================================================"
Write-Host "  FINAL: $testsPassed / $total passed  ($pct%)"
Write-Host "============================================================"
Write-Host ""

