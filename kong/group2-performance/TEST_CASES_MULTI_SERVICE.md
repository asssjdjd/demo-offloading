# Group 2 (Performance and Reliability) - Generic Test Cases for All Services

**Version**: 2.0 - Multi-Service
**Target Services**: User-Service, Order-Service, Item-Service
**Generated**: 2026-03-07

---

## Overview

This document defines comprehensive test cases for Kong Group 2 Performance & Reliability features applicable to **all three microservices**:

- **Load Balancing** with Active Health Checks
- **Response Caching** (Proxy Cache)
- **Circuit Breaker** - Fail-fast mechanism

Each service has its own set of endpoints, but the same Group 2 features apply universally.

---

## Service Endpoints Reference

### User Service

- **Base Path**: `/api/v1/users`
- **Main Endpoints**: GET, POST, PUT, DELETE, PATCH
- **Health Check**: `/actuator/health`
- **Port**: 8080

### Order Service

- **Base Path**: `/api/orders`
- **Main Endpoints**: GET, POST, PATCH
- **Health Check**: `/actuator/health`
- **Port**: 8081

### Item Service

- **Base Path**: `/api/items`
- **Main Endpoints**: GET, POST, PUT, DELETE
- **Health Check**: `/actuator/health`
- **Port**: 8082

---

## 1. Load Balancing with Health Checks

### Prerequisites

For **each service**:

- Kong running with `<service>-upstream` configured
- Service instances available (e.g., `<service>:8080`)
- Health endpoint available at `/actuator/health` returning 200 OK

### Test Case 1.1: Round-Robin Load Distribution

**Objective**: Verify requests are distributed equally across healthy targets

**Services Tested**: User-Service, Order-Service, Item-Service

**Setup**:

- Scale each service to 2 instances (e.g., `user-service:8080`, `user-service-2:8080`)
- Both instances healthy

**Test Steps** (for each service):

```bash
# Example: User Service
SERVICE_PATH="/api/v1/users"
TOKEN="<VALID_JWT_TOKEN>"

# Send 10 sequential GET requests
for i in {1..10}; do
  curl -H "Authorization: Bearer $TOKEN" \
       http://localhost:8000${SERVICE_PATH}
done
```

Repeat for:

- Order-Service: `/api/orders`
- Item-Service: `/api/items`

**Expected Result**:

- ✅ Requests distributed evenly (50/50 or close distribution)
- ✅ Both instances handle incoming traffic

**Failure Indicators**:

- ❌ All requests go to one instance
- ❌ Uneven distribution (e.g., 8/2 split)

---

### Test Case 1.2: Health Check - Healthy Target Detection

**Objective**: Verify active health checks detect healthy targets

**Services Tested**: All three services

**Setup**:

- Both instances started and responding to `/actuator/health` with 200 OK
- Health check interval: 5 seconds
- Success threshold: 2 (consecutive)

**Test Steps** (for each service):

```bash
# Example: Check Order Service upstream health
curl -s http://localhost:8001/upstreams/order-service-upstream/health/

# Check User Service upstream health
curl -s http://localhost:8001/upstreams/user-service-upstream/health/

# Check Item Service upstream health
curl -s http://localhost:8001/upstreams/item-service-upstream/health/
```

**Expected Result**:

- ✅ All targets show `healthy: true`
- ✅ Both instances per service are healthy

**Failure Indicators**:

- ❌ Targets missing from health endpoint response
- ❌ Targets showing `healthy: false` despite running service
- ❌ Inconsistent health status

---

### Test Case 1.3: Health Check - Unhealthy Target Detection

**Objective**: Verify active health checks detect and exclude unhealthy targets

**Services Tested**: All three services

**Setup**:

- Start with 2 healthy instances per service
- Failure threshold: 3 (consecutive HTTP failures)
- Failure interval: 5 seconds

**Test Steps** (for User-Service, then repeat for others):

```bash
# 1. Verify both targets are healthy
curl -s http://localhost:8001/upstreams/user-service-upstream/health/

# 2. Stop one user-service instance
docker-compose pause user-service-2  # or kill it

# 3. Wait 15 seconds (3 cycles × 5s = 15s for 3 failed checks)
sleep 15

# 4. Check health status again
curl -s http://localhost:8001/upstreams/user-service-upstream/health/

# 5. Send 5 requests - should all route to remaining instance
for i in {1..5}; do
  curl -H "Authorization: Bearer $TOKEN" \
       http://localhost:8000/api/v1/users
done
```

Repeat for Order-Service and Item-Service.

**Expected Result**:

- ✅ Stopped instance marked as `healthy: false`
- ✅ All subsequent requests go to surviving instance only
- ✅ No 502/503 errors once marked unhealthy

**Failure Indicators**:

- ❌ Failed instance still marked healthy
- ❌ Some requests still route to dead instance (503/timeout errors)
- ❌ Failed instance takes longer than 15s to mark unhealthy

---

### Test Case 1.4: Recovery - Unhealthy → Healthy Transition

**Objective**: Verify recovered target is returned to pool

**Services Tested**: All three services

**Setup**:

- From test 1.3: one instance stopped and marked unhealthy
- Recovery threshold: 2 (consecutive) successes over 5s intervals

**Test Steps** (for User-Service):

```bash
# 1. Restart the stopped instance
docker-compose unpause user-service-2

# 2. Verify it responds to /actuator/health
curl http://user-service-2:8080/actuator/health

# 3. Wait 10 seconds (2 cycles × 5s for 2 successes)
sleep 10

# 4. Check health status
curl -s http://localhost:8001/upstreams/user-service-upstream/health/

# 5. Send 10 requests - should distribute across both instances
for i in {1..10}; do
  curl -H "Authorization: Bearer $TOKEN" \
       http://localhost:8000/api/v1/users
done
```

Repeat for Order-Service and Item-Service.

**Expected Result**:

- ✅ Recovered instance marked as `healthy: true`
- ✅ Load distribution resumes (round-robin across both)
- ✅ No errors during recovery

**Failure Indicators**:

- ❌ Recovery instance not added back despite being healthy
- ❌ Takes longer than 10s to recover
- ❌ Requests still avoid recovered instance

---

## 2. Response Caching (Proxy Cache)

### Prerequisites

For **each service**:

- Kong proxy-cache plugin enabled on the service's protected route
- Cache TTL: 30 seconds
- Memory strategy for testing
- Valid JWT token for authentication

### Test Case 2.1: Cache Hit - Identical Request Within TTL

**Objective**: Verify identical GET requests return cached response

**Services Tested**: All three services

**Setup**:

- All services ready
- Valid JWT tokens available

**Test Steps** (for User-Service, then repeat for others):

```bash
# User-Service
TOKEN="<VALID_JWT_TOKEN>"

# 1. First GET request
time curl -i -H "Authorization: Bearer $TOKEN" \
          http://localhost:8000/api/v1/users

# 2. Immediately send identical request again
time curl -i -H "Authorization: Bearer $TOKEN" \
          http://localhost:8000/api/v1/users

# For Order-Service
curl -i http://localhost:8000/api/orders

# For Item-Service
curl -i http://localhost:8000/api/items
```

**Expected Result**:

- ✅ Second response faster than first (time comparison)
- ✅ X-Cache-Status: HIT
- ✅ Response body identical to first request

**Failure Indicators**:

- ❌ X-Cache-Status: MISS on second request
- ❌ No significant time difference
- ❌ Cache header missing

---

### Test Case 2.2: Cache Invalidation After TTL Expiration

**Objective**: Verify cache expires after 30 second TTL

**Services Tested**: All three services

**Setup**:

- TTL: 30 seconds

**Test Steps** (example for User-Service):

```bash
# 1. Send GET request to User-Service
T1=$(date +%s%N | cut -b1-13)
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8000/api/v1/users
T2=$(date +%s%N | cut -b1-13)
echo "Time for first request: $((T2-T1))ms"

# 2. Wait 35 seconds (exceeds 30s TTL)
echo "Waiting 35 seconds for TTL to expire..."
sleep 35

# 3. Send same request again
T3=$(date +%s%N | cut -b1-13)
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8000/api/v1/users
T4=$(date +%s%N | cut -b1-13)
echo "Time for request after TTL: $((T4-T3))ms"

# 4. Check X-Cache-Status header - should indicate MISS
```

Repeat for Order-Service and Item-Service.

**Expected Result**:

- ✅ After 35 seconds, X-Cache-Status: MISS
- ✅ Fetched fresh from backend
- ✅ Response time may be higher (fresh fetch)

**Failure Indicators**:

- ❌ X-Cache-Status: HIT after 35 seconds
- ❌ TTL not respected
- ❌ Response still served from cache

---

### Test Case 2.3: Cache Bypass for Non-GET Methods

**Objective**: Verify caching only applies to GET requests

**Services Tested**: All three services

**Setup**:

- POST/PUT/DELETE support enabled on routes
- Caching configured for GET only

**Test Steps** (example for User-Service):

```bash
# 1. Send POST request to User-Service
curl -X POST -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"name":"Test User"}' \
     http://localhost:8000/api/v1/users

# 2. Send identical POST again
curl -X POST -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"name":"Test User"}' \
     http://localhost:8000/api/v1/users

# Same for Order-Service POST
curl -X POST http://localhost:8000/api/orders \
     -H "Content-Type: application/json" \
     -d '{"userId":1,"items":[...]}'

# Same for Item-Service POST
curl -X POST http://localhost:8000/api/items \
     -H "Content-Type: application/json" \
     -d '{"name":"Test Item"}'
```

**Expected Result**:

- ✅ X-Cache-Status: BYPASS or MISS (not cached)
- ✅ Both requests reach backend
- ✅ POST requests not served from cache

**Failure Indicators**:

- ❌ X-Cache-Status: HIT on POST
- ❌ Second POST doesn't reach backend
- ❌ POST requests cached incorrectly

---

### Test Case 2.4: Cache Key Consideration - Authorization Header Variance

**Objective**: Verify cache respects Authorization header variance

**Services Tested**: All three services (where applicable)

**Setup**:

- Two different JWT tokens (different users/roles)
- `vary_headers: [Authorization]` configured

**Test Steps**:

```bash
TOKEN_A="<JWT_TOKEN_USER_A>"
TOKEN_B="<JWT_TOKEN_USER_B>"

# User-Service: Request with Token A
curl -H "Authorization: Bearer $TOKEN_A" \
     http://localhost:8000/api/v1/users

# User-Service: Request with Token B (different user)
curl -H "Authorization: Bearer $TOKEN_B" \
     http://localhost:8000/api/v1/users

# User-Service: Request with Token A again (should be from cache, user A data)
curl -H "Authorization: Bearer $TOKEN_A" \
     http://localhost:8000/api/v1/users
```

Repeat for Order-Service and Item-Service with appropriate tokens.

**Expected Result**:

- ✅ Token A request returns user A data
- ✅ Token B request returns different user B data
- ✅ Token A request (3rd) returns user A data from cache
- ✅ Data is NOT mixed/corrupted between users

**Failure Indicators**:

- ❌ Token B request returns Token A data
- ❌ Cache not respecting Authorization header
- ❌ Security issue: data leakage between tokens

---

## 3. Circuit Breaker

### Prerequisites

For **each service**:

- Circuit-breaker plugin enabled
- Failure threshold: 5 consecutive failures
- Success threshold: 2 consecutive successes (for HALF-OPEN)
- Timeout (OPEN → HALF-OPEN): 30 seconds
- Fallback status: 503
- Fallback body: `{"message":"Service temporarily unavailable"}`

### Test Case 3.1: Circuit Closed - Normal Operation

**Objective**: Verify circuit breaker doesn't interfere during normal operation

**Services Tested**: All three services

**Setup**:

- All services healthy
- All requests should succeed

**Test Steps**:

```bash
# User-Service: Send 10 GET requests
for i in {1..10}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
           -H "Authorization: Bearer $TOKEN" \
           http://localhost:8000/api/v1/users)
  CB_STATE=$(curl -s -H "Authorization: Bearer $TOKEN" \
             http://localhost:8000/api/v1/users \
             -w "\n%{header_x_circuitbreaker_state}")
  echo "Request $i - Status: $STATUS, CB State: $CB_STATE"
done

# Order-Service: Send 10 GET requests
for i in {1..10}; do
  curl -i http://localhost:8000/api/orders
done

# Item-Service: Send 10 GET requests
for i in {1..10}; do
  curl -i http://localhost:8000/api/items
done
```

**Expected Result**:

- ✅ All requests return 200
- ✅ X-CircuitBreaker-State: CLOSED
- ✅ No fallback responses

**Failure Indicators**:

- ❌ Any request returns 503
- ❌ X-CircuitBreaker-State something other than CLOSED
- ❌ Fallback response received

---

### Test Case 3.2: Circuit Transitions to OPEN After Failure Threshold

**Objective**: Verify circuit opens after 5 consecutive failures

**Services Tested**: All three services

**Setup**:

- Failure threshold: 5
- Service responding with errors (e.g., 500, 503, timeout)

**Test Steps** (example for User-Service):

```bash
# 1. Stop user-service to trigger failures
docker-compose stop user-service

# 2. Send 5 requests expecting failures
for i in {1..5}; do
  curl -v -H "Authorization: Bearer $TOKEN" \
       http://localhost:8000/api/v1/users 2>&1 | grep "< HTTP"
done

# 3. Check headers on request #6 (should be fallback 503)
curl -v -H "Authorization: Bearer $TOKEN" \
     http://localhost:8000/api/v1/users

# 4. Check if body contains fallback message
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8000/api/v1/users
```

Repeat for Order-Service and Item-Service.

**Expected Result**:

- ✅ Requests 1-5: Get actual backend errors or timeouts
- ✅ Request 6: Returned 503 immediately with fallback body
- ✅ X-CircuitBreaker-State: OPEN
- ✅ No backend request on request 6 (fast-fail)

**Failure Indicators**:

- ❌ After 5 failures, request 6 still reaches backend
- ❌ X-CircuitBreaker-State still CLOSED
- ❌ Fallback response not returned

---

### Test Case 3.3: Circuit Transitions to HALF-OPEN After Timeout

**Objective**: Verify circuit tries recovery after open timeout (30 seconds)

**Services Tested**: All three services

**Setup**:

- From Test 3.2: Circuit is in OPEN state
- Timeout: 30 seconds
- Service now recovered

**Test Steps** (example for User-Service):

```bash
# 1. Confirm circuit is OPEN
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8000/api/v1/users

# 2. Restart user-service
docker-compose start user-service
sleep 5  # Give it time to start

# 3. Wait 30 seconds for circuit timeout
echo "Waiting 30 seconds for OPEN timeout..."
sleep 30

# 4. Send test request
curl -v -H "Authorization: Bearer $TOKEN" \
     http://localhost:8000/api/v1/users
```

Repeat for Order-Service and Item-Service.

**Expected Result**:

- ✅ X-CircuitBreaker-State: HALF-OPEN
- ✅ Request is forwarded to backend (not fallback)
- ✅ Receives 200 response from healthy backend

**Failure Indicators**:

- ❌ X-CircuitBreaker-State still OPEN
- ❌ Before 30 seconds, circuit transitions
- ❌ After 30 seconds, still returns 503 fallback

---

### Test Case 3.4: Circuit Transitions Back to CLOSED on Recovery

**Objective**: Verify circuit closes after 2 consecutive successes in HALF-OPEN state

**Services Tested**: All three services

**Setup**:

- From Test 3.3: Circuit is HALF-OPEN
- Service is healthy
- Success threshold: 2

**Test Steps**:

```bash
# User-Service: From HALF-OPEN state, send 2 successful requests
for i in {1..2}; do
  curl -H "Authorization: Bearer $TOKEN" \
       http://localhost:8000/api/v1/users
  sleep 1
done

# Send 3rd request and check state
curl -v -H "Authorization: Bearer $TOKEN" \
     http://localhost:8000/api/v1/users
```

Repeat for Order-Service and Item-Service.

**Expected Result**:

- ✅ Requests 1-2 return 200 from backend
- ✅ After 2 successes, X-CircuitBreaker-State: CLOSED
- ✅ Request 3 also returns 200 (normal operation resumed)

**Failure Indicators**:

- ❌ Circuit remains in HALF-OPEN after successes
- ❌ Success threshold not respected
- ❌ Circuit transitions to OPEN again

---

### Test Case 3.5: Fallback Response Format and Status

**Objective**: Verify fallback response provides correct status and body

**Services Tested**: All three services

**Setup**:

- Circuit in OPEN state
- Fallback status: 503
- Fallback body: `{"message":"Service temporarily unavailable"}`

**Test Steps**:

```bash
# 1. Trigger circuit to OPEN (from Test 3.2)
# 2. Send request while circuit OPEN
curl -i -H "Authorization: Bearer $TOKEN" \
     http://localhost:8000/api/v1/users

# 3. Inspect response headers and body
```

Repeat for all three services.

**Expected Result**:

- ✅ Status: 503
- ✅ Header X-CircuitBreaker-State: OPEN
- ✅ Body: `{"message":"Service temporarily unavailable"}`

**Failure Indicators**:

- ❌ Status is not 503
- ❌ Wrong fallback body
- ❌ Missing headers

---

## 4. Integration Tests

### Test Case 4.1: Multi-Service Load Balancing + Caching

**Objective**: Verify all three services work with load balancing and caching simultaneously

**Setup**:

- 2 instances of each service healthy
- Cache TTL: 30s

**Test Steps**:

```bash
# Test User-Service + Order-Service + Item-Service together
SERVICES=(
  "user_service:/api/v1/users"
  "order_service:/api/orders"
  "item_service:/api/items"
)

for service in "${SERVICES[@]}"; do
  SERVICE_NAME="${service%%:*}"
  SERVICE_PATH="${service#*:}"

  echo "Testing $SERVICE_NAME at $SERVICE_PATH"

  # Send requests and verify cache
  time curl -H "Authorization: Bearer $TOKEN" \
            http://localhost:8000${SERVICE_PATH}

  time curl -H "Authorization: Bearer $TOKEN" \
            http://localhost:8000${SERVICE_PATH}
done
```

**Expected Result**:

- ✅ Caching works across all services
- ✅ Load balancing transparent to services
- ✅ No 502/503 errors

---

## Performance Baselines (Optional)

Reference metrics to validate Group 2 features:

| Metric                   | User-Service         | Order-Service        | Item-Service         |
| ------------------------ | -------------------- | -------------------- | -------------------- |
| Cache Hit Response Time  | ~5-20ms              | ~5-20ms              | ~5-20ms              |
| Healthy Target Detection | <20s                 | <20s                 | <20s                 |
| Circuit Open Delay       | <5s after 5 failures | <5s after 5 failures | <5s after 5 failures |

---

## Test Execution Checklist

- [ ] All services started and healthy
- [ ] Kong reachable at `http://localhost:8000`
- [ ] Admin API accessible at `http://localhost:8001`
- [ ] Valid JWT tokens available for User-Service
- [ ] Load balancing tests for all 3 services (1.1-1.4)
- [ ] Caching tests for all 3 services (2.1-2.4)
- [ ] Circuit breaker tests for all 3 services (3.1-3.5)
- [ ] Integration tests (4.1)
- [ ] No runtime errors in Kong logs
- [ ] All services functioning independently

---

## Notes

- Test environment: Local Docker Compose setup with all 3 services
- Kong upstream names follow pattern: `{service}-upstream`
- Cache strategy is `memory` (ephemeral, suitable for testing)
- Production should use `redis` for cache persistence
- Each service uses different port but same Kong proxy (localhost:8000)
- JWT tokens are service-agnostic but may require appropriate roles/scopes

---

## Service-Specific Commands

### User-Service

```bash
# Get all users
curl -H "Authorization: Bearer $TOKEN" http://localhost:8000/api/v1/users

# Get specific user
curl -H "Authorization: Bearer $TOKEN" http://localhost:8000/api/v1/users/1
```

### Order-Service

```bash
# Get all orders
curl http://localhost:8000/api/orders

# Get specific order
curl http://localhost:8000/api/orders/1
```

### Item-Service

```bash
# Get all items
curl http://localhost:8000/api/items

# Get specific item
curl http://localhost:8000/api/items/1
```

---
