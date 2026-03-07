# Group 2 (Performance and Reliability) - Test Cases

## Overview

This document defines comprehensive test cases for Kong Group 2 Performance & Reliability features:

- **Load Balancing** with Active Health Checks
- **Response Caching** (Proxy Cache)
- **Circuit Breaker** - Fail-fast mechanism

---

## 1. Load Balancing with Health Checks

### Prerequisites

- Kong running with `user-service-upstream` configured
- User service instances available at `user-service:8080`
- Health endpoint available at `/actuator/health` returning 200 OK

### Test Case 1.1: Round-Robin Load Distribution

**Objective**: Verify requests are distributed equally across healthy targets

**Setup**:

- Scale user-service to 2 instances (e.g., `user-service:8080`, `user-service-2:8080`)
- Both instances healthy

**Test Steps**:

1. Send 10 sequential GET requests to `/api/v1/users` with valid JWT
2. Log which instance handles each request (check `X-Served-By` header or logs)
3. Verify roughly 5 requests go to each instance

**Expected Result**: ✅ Requests distributed evenly (50/50 or close distribution)

**Failure Indicators**:

- ❌ All requests go to one instance
- ❌ Uneven distribution (e.g., 8/2 split)

---

### Test Case 1.2: Health Check - Healthy Target Detection

**Objective**: Verify active health checks detect healthy targets

**Setup**:

- Both user-service instances started and responding to `/actuator/health` with 200 OK
- Health check interval: 5 seconds
- Success threshold: 2 (consecutive)

**Test Steps**:

1. Send request to `/api/v1/users` - should succeed
2. Verify Kong's Admin API returns both targets as "healthy"
   ```bash
   curl -s http://localhost:8001/upstreams/user-service-upstream/health/
   ```
3. Wait 10 seconds (2 cycles of 5s intervals)
4. Verify both targets remain healthy

**Expected Result**: ✅ Both targets show `healthy: true`

**Failure Indicators**:

- ❌ Targets missing from health endpoint response
- ❌ Targets showing `healthy: false` despite running service
- ❌ Inconsistent health status

---

### Test Case 1.3: Health Check - Unhealthy Target Detection

**Objective**: Verify active health checks detect and exclude unhealthy targets

**Setup**:

- Start with 2 healthy instances
- Failure threshold: 3 (consecutive HTTP failures)
- Failure interval: 5 seconds

**Test Steps**:

1. Verify both targets start as healthy (use Admin API as in 1.2)
2. Stop one user-service instance (kill or pause)
3. Wait 15 seconds (3 cycles × 5s = 15s for 3 failed checks)
4. Check health status again:
   ```bash
   curl -s http://localhost:8001/upstreams/user-service-upstream/health/
   ```
5. Send 5 requests to `/api/v1/users` - should all route to remaining healthy instance

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

**Setup**:

- From test 1.3: one instance stopped and marked unhealthy
- Recovery threshold: 2 (consecutive) successes over 5s intervals

**Test Steps**:

1. Restart the stopped user-service instance
2. Verify it responds to `/actuator/health` with 200 OK
3. Wait 10 seconds (2 cycles × 5s for 2 successes to accumulate)
4. Check health status:
   ```bash
   curl -s http://localhost:8001/upstreams/user-service-upstream/health/
   ```
5. Send 10 requests - should now be distributed across both instances

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

- Kong proxy-cache plugin enabled on `user-service-protected` route
- Cache TTL: 30 seconds
- Memory strategy for testing
- Route: `/api/v1/users` (GET requests only)

### Test Case 2.1: Cache Hit - Identical Request Within TTL

**Objective**: Verify identical GET requests return cached response

**Setup**:

- User service ready
- Valid JWT token

**Test Steps**:

1. Send first GET request:
   ```bash
   curl -i -H "Authorization: Bearer <JWT>" http://localhost:8000/api/v1/users
   ```
   Note response time (T1) and `X-Cache-Status` header
2. Immediately send identical request again
   Note response time (T2) and `X-Cache-Status` header
3. Verify T2 < T1 (cached response faster)
4. Verify `X-Cache-Status: HIT`

**Expected Result**:

- ✅ Second response much faster
- ✅ X-Cache-Status: HIT (or similar cache hit indicator)
- ✅ Response body identical to first request

**Failure Indicators**:

- ❌ X-Cache-Status: MISS on second request
- ❌ No significant time difference
- ❌ Cache header missing

---

### Test Case 2.2: Cache Invalidation After TTL Expiration

**Objective**: Verify cache expires after 30 second TTL

**Setup**:

- User service with data endpoint
- TTL: 30 seconds

**Test Steps**:

1. Send GET request to `/api/v1/users`:
   ```bash
   curl -H "Authorization: Bearer <JWT>" http://localhost:8000/api/v1/users
   ```
   Note response time (T1)
2. Wait 35 seconds (exceeds 30s TTL)
3. Send same request again
   Note response time (T2)
4. Check `X-Cache-Status` header

**Expected Result**:

- ✅ After 35 seconds, response is slower (cache expired)
- ✅ X-Cache-Status: MISS
- ✅ Fetched fresh from backend

**Failure Indicators**:

- ❌ X-Cache-Status: HIT after 35 seconds
- ❌ TTL not respected
- ❌ Response still fast (not fetched fresh)

---

### Test Case 2.3: Cache Bypass for Non-GET Methods

**Objective**: Verify caching only applies to GET requests

**Setup**:

- POST/PUT/DELETE support enabled on route
- Caching configured for GET only

**Test Steps**:

1. Send POST request to `/api/v1/users` with body:
   ```bash
   curl -X POST -H "Authorization: Bearer <JWT>" \
     -H "Content-Type: application/json" \
     -d '{"name":"Test User"}' \
     http://localhost:8000/api/v1/users
   ```
   Note `X-Cache-Status` header
2. Send identical POST again
   Note `X-Cache-Status` header

**Expected Result**:

- ✅ X-Cache-Status: BYPASS or MISS (not cached)
- ✅ Both requests reach backend (not served from cache)

**Failure Indicators**:

- ❌ X-Cache-Status: HIT on POST
- ❌ Second POST doesn't reach backend
- ❌ POST requests cached incorrectly

---

### Test Case 2.4: Cache Key Consideration - Authorization Header Variance

**Objective**: Verify cache respects Authorization header variance

**Setup**:

- Two different JWT tokens (different users)
- `vary_headers: [Authorization]` configured

**Test Steps**:

1. Send GET request with Token A:
   ```bash
   curl -H "Authorization: Bearer <TOKEN_A>" http://localhost:8000/api/v1/users
   ```
   Note response includes user info from Token A
2. Send GET request with Token B (different user):
   ```bash
   curl -H "Authorization: Bearer <TOKEN_B>" http://localhost:8000/api/v1/users
   ```
   Note response includes user info from Token B
3. Send another request with Token A:
   Verify it returns cached data for Token A (not Token B data)

**Expected Result**:

- ✅ Token A request returns user A data
- ✅ Token B request returns different user B data
- ✅ Token A request (3rd) returns user A data from cache
- ✅ Data is NOT mixed/corrupted between users

**Failure Indicators**:

- ❌ Token B request returns Token A data
- ❌ Cache not respecting Authorization header variance
- ❌ Security issue: user data leakage between tokens

---

## 3. Circuit Breaker

### Prerequisites

- Circuit-breaker plugin enabled on `user-service-protected` route
- Failure threshold: 5 consecutive failures
- Success threshold: 2 consecutive successes (for HALF-OPEN)
- Timeout (OPEN → HALF-OPEN): 30 seconds
- Fallback status: 503
- Fallback body: `{"message":"Service temporarily unavailable"}`

### Test Case 3.1: Circuit Closed - Normal Operation

**Objective**: Verify circuit breaker doesn't interfere during normal operation

**Setup**:

- User service healthy
- All requests should succeed

**Test Steps**:

1. Send 10 GET requests to `/api/v1/users`:
   ```bash
   for i in {1..10}; do
     curl -H "Authorization: Bearer <JWT>" http://localhost:8000/api/v1/users
   done
   ```
2. Check response status codes (should all be 200)
3. Check `X-CircuitBreaker-State` header

**Expected Result**:

- ✅ All 10 requests return 200
- ✅ X-CircuitBreaker-State: CLOSED
- ✅ No fallback responses

**Failure Indicators**:

- ❌ Any request returns 503
- ❌ X-CircuitBreaker-State something other than CLOSED
- ❌ Fallback response received

---

### Test Case 3.2: Circuit Transitions to OPEN After Failure Threshold

**Objective**: Verify circuit opens after 5 consecutive failures

**Setup**:

- Failure threshold: 5
- User service responding with errors (e.g., 500, 503, timeout)

**Test Steps**:

1. Make user service return errors (e.g., kill service, or deploy faulty version)
2. Send 5 requests expecting failures:
   ```bash
   for i in {1..5}; do
     curl -v -H "Authorization: Bearer <JWT>" http://localhost:8000/api/v1/users
   done
   ```
3. After 5 failures, check headers on next request (request #6):
   ```bash
   curl -v -H "Authorization: Bearer <JWT>" http://localhost:8000/api/v1/users
   ```

**Expected Result**:

- ✅ Requests 1-5: receive actual backend errors (500/503)
- ✅ Request 6: returned 503 immediately by Kong with fallback body
- ✅ Request 6: X-CircuitBreaker-State: OPEN
- ✅ No backend request on request 6 (fast-fail)

**Failure Indicators**:

- ❌ After 5 failures, request 6 still reaches backend
- ❌ X-CircuitBreaker-State still CLOSED
- ❌ Fallback response not returned

---

### Test Case 3.3: Circuit Transitions to HALF-OPEN After Timeout

**Objective**: Verify circuit tries recovery after open timeout (30 seconds)

**Setup**:

- From Test 3.2: Circuit is in OPEN state
- Timeout: 30 seconds
- Service now recovered (responding with 200)

**Test Steps**:

1. Confirm circuit is OPEN (send request, get 503 with fallback)
2. Restart/recover user service (should return 200 OK)
3. Wait 30 seconds
4. Send test request:
   ```bash
   curl -v -H "Authorization: Bearer <JWT>" http://localhost:8000/api/v1/users
   ```

**Expected Result**:

- ✅ X-CircuitBreaker-State: HALF-OPEN
- ✅ Request is forwarded to backend (not using fallback)
- ✅ Receives 200 response from healthy backend

**Failure Indicators**:

- ❌ X-CircuitBreaker-State still OPEN
- ❌ Before 30 seconds, circuit transitions (should stay OPEN)
- ❌ After 30 seconds, still returns 503 fallback

---

### Test Case 3.4: Circuit Transitions Back to CLOSED on Recovery

**Objective**: Verify circuit closes after 2 consecutive successes in HALF-OPEN state

**Setup**:

- From Test 3.3: Circuit is HALF-OPEN
- Service is healthy
- Success threshold: 2

**Test Steps**:

1. From HALF-OPEN state, send 2 successful requests:
   ```bash
   for i in {1..2}; do
     curl -H "Authorization: Bearer <JWT>" http://localhost:8000/api/v1/users
     sleep 1
   done
   ```
2. After 2 successes, send another request and check state:
   ```bash
   curl -v -H "Authorization: Bearer <JWT>" http://localhost:8000/api/v1/users
   ```

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

**Setup**:

- Circuit in OPEN state
- Fallback status: 503
- Fallback body: `{"message":"Service temporarily unavailable"}`

**Test Steps**:

1. Trigger circuit to OPEN (from Test 3.2)
2. Send request while circuit OPEN:
   ```bash
   curl -i -H "Authorization: Bearer <JWT>" http://localhost:8000/api/v1/users
   ```
3. Inspect response:
   - Status code
   - Content-Type header
   - Response body

**Expected Result**:

- ✅ Status: 503
- ✅ Content-Type: application/json (or appropriate)
- ✅ Body: `{"message":"Service temporarily unavailable"}`
- ✅ X-CircuitBreaker-State: OPEN

**Failure Indicators**:

- ❌ Status is not 503
- ❌ Wrong fallback body
- ❌ Missing or incorrect headers

---

### Test Case 3.6: Circuit Reset on Single Failure (from HALF-OPEN back to OPEN)

**Objective**: Verify circuit reopens if failure occurs during HALF-OPEN

**Setup**:

- Circuit in HALF-OPEN state (1 success registered)
- Service becomes unstable again

**Test Steps**:

1. From HALF-OPEN state with 1 success, make service fail
2. Send next request (should fail):
   ```bash
   curl -H "Authorization: Bearer <JWT>" http://localhost:8000/api/v1/users
   ```
3. Verify circuit state returns to OPEN
4. Next requests should use fallback (503)

**Expected Result**:

- ✅ Request returns backend error (5xx)
- ✅ X-CircuitBreaker-State transitions back to OPEN
- ✅ Subsequent requests receive fallback 503
- ✅ Success counter reset

**Failure Indicators**:

- ❌ Circuit remains HALF-OPEN after failure
- ❌ Subsequent requests reach failing backend
- ❌ One failure doesn't cause reopening

---

## Integration Tests

### Test Case 4.1: Load Balancing + Health Checks + Cache

**Objective**: Verify cache works correctly with load-balanced backends

**Setup**:

- 2 healthy instances
- Cache TTL: 30s

**Test Steps**:

1. Send GET request (cached at instance A):
   ```bash
   curl -H "Authorization: Bearer <JWT>" http://localhost:8000/api/v1/users
   ```
   Note response time
2. Send 10 identical requests (should hit cache):
   ```bash
   for i in {1..10}; do
     curl -H "Authorization: Bearer <JWT>" http://localhost:8000/api/v1/users
   done
   ```
3. Stop instance A
4. Wait for health check to mark it UNHEALTHY (15s)
5. Send request - should route to instance B (no cache from A)

**Expected Result**:

- ✅ Caching works across load balancing
- ✅ When instance A removed, routing to instance B works
- ✅ No 502/503 errors during transition

**Failure Indicators**:

- ❌ Cache causes routing issues
- ❌ Requests stuck to died instance
- ❌ Errors during instance removal

---

### Test Case 4.2: Load Balancing + Circuit Breaker

**Objective**: Verify circuit breaker works distributed across backends

**Setup**:

- 2 instances
- One instance failing

**Test Steps**:

1. Make instance A fail (respond with 500)
2. Instance B continues normal (200)
3. Send requests - should start routing to B due to circuit on A
4. After 5 failures to A, verify fallback (503) is returned instead of continuing to hammer A

**Expected Result**:

- ✅ Requests prefer healthy B
- ✅ Circuit opens for A after failures
- ✅ Fallback 503 returned
- ✅ No cascading failures

**Failure Indicators**:

- ❌ Circuit breaker not protecting
- ❌ Continuing to route to failing A
- ❌ Backend overload

---

## Performance Baselines (Optional)

These are reference metrics to validate Group 2 features provide expected benefits:

| Metric                             | Baseline (Without Group 2) | Expected (With Group 2) | Threshold             |
| ---------------------------------- | -------------------------- | ----------------------- | --------------------- |
| Cache Hit Response Time            | ~50-100ms                  | ~5-10ms                 | 80% improvement       |
| P99 Response Time (Multi-Instance) | ~80ms                      | ~60ms                   | 25% improvement       |
| Recovery Time (Dead Instance)      | Manual intervention        | <20s (auto-detect)      | Automatic             |
| Error Cascade Duration             | Indefinite                 | <30s (circuit timeout)  | Circuit limits damage |

---

## Test Execution Checklist

- [ ] All instances started and healthy
- [ ] Kong reachable at `http://localhost:8000`
- [ ] Admin API accessible at `http://localhost:8001`
- [ ] Valid JWT token available for protected endpoints
- [ ] Load balancing tests completed (1.1-1.4)
- [ ] Caching tests completed (2.1-2.4)
- [ ] Circuit breaker tests completed (3.1-3.6)
- [ ] Integration tests completed (4.1-4.2)
- [ ] No runtime errors in Kong logs
- [ ] Documentation updated with results

---

## Notes

- Test environment: Local Docker Compose setup
- All tests assume `/api/v1/users` endpoint exists and returns JSON
- JWT tokens must be valid (not expired)
- Timestamps in logs should be consistent with test execution
- Cache strategy is `memory` for testing (ephemeral)
- Production should use `redis` for cache persistence
