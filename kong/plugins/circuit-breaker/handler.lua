local plugin = {
  PRIORITY = 1005,
  VERSION = "0.1.0",
}

-- Import required modules
local cjson = require("cjson")

-- Use Kong's cache for circuit breaker state
local cache = ngx.shared.kong_cache
if not cache then
  cache = ngx.shared.kong
end

-- Helper: Generate unique key for this route
local function get_circuit_key(conf)
  local route_id = ngx.ctx.route and ngx.ctx.route.id or "unknown"
  return "circuit_breaker:" .. route_id
end

-- Helper: Get current state
local function get_state(conf)
  local key = get_circuit_key(conf)
  local state_data = cache:get(key)
  if state_data then
    return cjson.decode(state_data)
  end
  return {
    state = "CLOSED",
    failure_count = 0,
    success_count = 0,
    last_open_time = 0
  }
end

-- Helper: Update state
local function update_state(conf, state_data)
  local key = get_circuit_key(conf)
  cache:set(key, cjson.encode(state_data), 3600)
end

-- Helper: Check if circuit should transition from OPEN to HALF-OPEN
local function should_transition_to_half_open(state, conf)
  if state.state == "OPEN" then
    local elapsed = ngx.now() - state.last_open_time
    if elapsed >= (conf.open_timeout_ms / 1000) then
      return true
    end
  end
  return false
end

function plugin:access(conf)
  local state = get_state(conf)
  
  -- Check if should transition from OPEN to HALF-OPEN
  if should_transition_to_half_open(state, conf) then
    state.state = "HALF-OPEN"
    state.success_count = 0
    state.failure_count = 0
    update_state(conf, state)
  end
  
  -- If circuit is OPEN (and not transitioning), return fallback response
  if state.state == "OPEN" then
    ngx.status = tonumber(conf.fallback_status) or 503
    ngx.header["X-CircuitBreaker-State"] = "OPEN"
    ngx.header["Content-Type"] = "application/json"
    ngx.say(conf.fallback_body)
    return ngx.exit(ngx.status)
  end
  
  -- Store current state for header_filter phase
  ngx.ctx.circuit_state = state
end

function plugin:header_filter(conf)
  if not ngx.ctx.circuit_state then
    return
  end
  
  local state = ngx.ctx.circuit_state
  local status = tonumber(ngx.status)
  
  -- Determine if response indicates success (2xx, 3xx) or failure
  local is_success = status >= 200 and status < 400
  local is_failure = status >= 400
  
  -- Update counters based on response
  if is_success then
    state.failure_count = 0
    state.success_count = state.success_count + 1
    
    -- If in HALF-OPEN and threshold reached, transition to CLOSED
    if state.state == "HALF-OPEN" and state.success_count >= conf.success_threshold then
      state.state = "CLOSED"
      state.success_count = 0
      state.failure_count = 0
    end
  elseif is_failure then
    state.failure_count = state.failure_count + 1
    state.success_count = 0
    
    -- If failure threshold reached, open circuit
    if state.failure_count >= conf.failure_threshold then
      state.state = "OPEN"
      state.last_open_time = ngx.now()
    end
  end
  
  update_state(conf, state)
  
  -- Add debug header with current circuit state
  ngx.header["X-CircuitBreaker-State"] = state.state
end

return plugin
