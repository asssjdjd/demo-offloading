-- =============================================================================
-- Kong Custom Plugin: jwt-header-injector
-- Extracts claims from validated JWT and injects them as X-User-* headers
-- This runs AFTER the built-in JWT plugin validates the token
-- =============================================================================
local cjson = require "cjson.safe"
local ngx_b64 = require "ngx.base64"

local plugin = {
  PRIORITY = 900,
  VERSION = "1.0.0",
}

--- Decode a base64url-encoded string (Đặt ngoài hàm access để tối ưu)
local function base64url_decode(input)
  local remainder = #input % 4
  if remainder > 0 then
    input = input .. string.rep("=", 4 - remainder)
  end
  input = input:gsub("-", "+"):gsub("_", "/")
  return ngx.decode_base64(input)
end

--- Extract claims from JWT token
local function extract_jwt_claims(token)
  if not token then return nil, "no token provided" end
  token = token:gsub("^[Bb]earer%s+", "")
  local parts = {}
  for part in token:gmatch("[^%.]+") do table.insert(parts, part) end
  if #parts ~= 3 then return nil, "invalid JWT format" end

  local payload_json = base64url_decode(parts[2])
  if not payload_json then return nil, "failed to decode payload" end

  return cjson.decode(payload_json)
end

function plugin:access(conf)

  local auth_header = kong.request.get_header("Authorization")
  if not auth_header then return end

  local claims, err = extract_jwt_claims(auth_header)
  if claims then
    if claims.sub then kong.service.request.set_header("X-User-Id", claims.sub) end
    if claims.role then kong.service.request.set_header("X-User-Role", claims.role) end
    if claims.username then kong.service.request.set_header("X-User-Username", claims.username) end
    if claims.email then kong.service.request.set_header("X-User-Email", claims.email) end
    if claims.scope then kong.service.request.set_header("X-User-Scope", claims.scope) end

    local consumer = kong.client.get_consumer()
    if consumer then
      kong.service.request.set_header("X-Consumer-Username", consumer.username or "")
      kong.service.request.set_header("X-Consumer-Custom-Id", consumer.custom_id or "")
    end
  else
    kong.log.warn("Failed to extract JWT claims: ", err)
  end
end

return plugin