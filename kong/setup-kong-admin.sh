#!/bin/bash
# =============================================================================
# Kong Admin API Setup Script
# Alternative to declarative config - uses Kong Admin API (cURL)
# Run this AFTER Kong is started in DB mode (not DB-less mode)
# =============================================================================

KONG_ADMIN="http://localhost:8001"

echo "============================================="
echo " Kong Gateway Offloading - Admin API Setup"
echo "============================================="

# ---------------------------------------------------------------------------
# 1. Create the User Service
# ---------------------------------------------------------------------------
echo -e "\n==> Creating user-service..."
curl -s -X POST ${KONG_ADMIN}/services \
  -d name=user-service \
  -d url=http://user-service:8080 \
  -d retries=3 \
  -d connect_timeout=5000

# ---------------------------------------------------------------------------
# 2. Create Routes
# ---------------------------------------------------------------------------
echo -e "\n\n==> Creating public auth route..."
curl -s -X POST ${KONG_ADMIN}/services/user-service/routes \
  -d name=user-service-public \
  -d "paths[]=/api/v1/auth" \
  -d "methods[]=POST" \
  -d strip_path=false \
  -d preserve_host=true

echo -e "\n\n==> Creating protected users route..."
curl -s -X POST ${KONG_ADMIN}/services/user-service/routes \
  -d name=user-service-protected \
  -d "paths[]=/api/v1/users" \
  -d "methods[]=GET" \
  -d "methods[]=POST" \
  -d "methods[]=PUT" \
  -d "methods[]=DELETE" \
  -d "methods[]=PATCH" \
  -d strip_path=false \
  -d preserve_host=true

echo -e "\n\n==> Creating admin route..."
curl -s -X POST ${KONG_ADMIN}/services/user-service/routes \
  -d name=user-service-admin \
  -d "paths[]=/api/v1/admin" \
  -d "methods[]=GET" \
  -d "methods[]=POST" \
  -d "methods[]=PUT" \
  -d "methods[]=DELETE" \
  -d strip_path=false \
  -d preserve_host=true

# ---------------------------------------------------------------------------
# 3. Create Consumers
# ---------------------------------------------------------------------------
echo -e "\n\n==> Creating admin consumer..."
curl -s -X POST ${KONG_ADMIN}/consumers \
  -d username=admin-user \
  -d custom_id=admin-001

echo -e "\n\n==> Creating regular consumer..."
curl -s -X POST ${KONG_ADMIN}/consumers \
  -d username=regular-user \
  -d custom_id=user-001

# ---------------------------------------------------------------------------
# 4. Setup JWT Credentials
# ---------------------------------------------------------------------------
echo -e "\n\n==> Setting up JWT for admin-user..."
curl -s -X POST ${KONG_ADMIN}/consumers/admin-user/jwt \
  -d key=admin-issuer \
  -d secret="your-admin-jwt-secret-key-min-32-chars!!" \
  -d algorithm=HS256

echo -e "\n\n==> Setting up JWT for regular-user..."
curl -s -X POST ${KONG_ADMIN}/consumers/regular-user/jwt \
  -d key=user-issuer \
  -d secret="your-user-jwt-secret-key-min-32-chars!!" \
  -d algorithm=HS256

# ---------------------------------------------------------------------------
# 5. Setup ACL Groups
# ---------------------------------------------------------------------------
echo -e "\n\n==> Adding admin-user to groups..."
curl -s -X POST ${KONG_ADMIN}/consumers/admin-user/acls -d group=admin-group
curl -s -X POST ${KONG_ADMIN}/consumers/admin-user/acls -d group=user-group

echo -e "\n\n==> Adding regular-user to groups..."
curl -s -X POST ${KONG_ADMIN}/consumers/regular-user/acls -d group=user-group

# ---------------------------------------------------------------------------
# 6. Enable JWT Plugin on protected routes
# ---------------------------------------------------------------------------
echo -e "\n\n==> Enabling JWT plugin on protected route..."
curl -s -X POST ${KONG_ADMIN}/routes/user-service-protected/plugins \
  -d name=jwt \
  -d "config.claims_to_verify[]=exp" \
  -d config.key_claim_name=iss

echo -e "\n\n==> Enabling JWT plugin on admin route..."
curl -s -X POST ${KONG_ADMIN}/routes/user-service-admin/plugins \
  -d name=jwt \
  -d "config.claims_to_verify[]=exp" \
  -d config.key_claim_name=iss

# ---------------------------------------------------------------------------
# 7. Enable ACL Plugin
# ---------------------------------------------------------------------------
echo -e "\n\n==> Enabling ACL on protected route (user-group + admin-group)..."
curl -s -X POST ${KONG_ADMIN}/routes/user-service-protected/plugins \
  -d name=acl \
  -d "config.allow[]=user-group" \
  -d "config.allow[]=admin-group"

echo -e "\n\n==> Enabling ACL on admin route (admin-group only)..."
curl -s -X POST ${KONG_ADMIN}/routes/user-service-admin/plugins \
  -d name=acl \
  -d "config.allow[]=admin-group"

# ---------------------------------------------------------------------------
# 8. Enable Request Transformer (Forward identity headers)
# ---------------------------------------------------------------------------
echo -e "\n\n==> Enabling request-transformer on protected route..."
curl -s -X POST ${KONG_ADMIN}/routes/user-service-protected/plugins \
  -d name=request-transformer \
  -d "config.add.headers[]=X-Gateway-Auth:kong-verified" \
  -d "config.remove.headers[]=Authorization"

echo -e "\n\n==> Enabling request-transformer on admin route..."
curl -s -X POST ${KONG_ADMIN}/routes/user-service-admin/plugins \
  -d name=request-transformer \
  -d "config.add.headers[]=X-Gateway-Auth:kong-verified" \
  -d "config.remove.headers[]=Authorization"

# ---------------------------------------------------------------------------
# 9. Enable Rate Limiting (Global)
# ---------------------------------------------------------------------------
echo -e "\n\n==> Enabling global rate-limiting..."
curl -s -X POST ${KONG_ADMIN}/plugins \
  -d name=rate-limiting \
  -d config.minute=100 \
  -d config.hour=5000 \
  -d config.policy=local

# ---------------------------------------------------------------------------
# 10. Enable CORS (Global)
# ---------------------------------------------------------------------------
echo -e "\n\n==> Enabling global CORS..."
curl -s -X POST ${KONG_ADMIN}/plugins \
  -d name=cors \
  -d "config.origins[]=*" \
  -d "config.methods[]=GET" \
  -d "config.methods[]=POST" \
  -d "config.methods[]=PUT" \
  -d "config.methods[]=DELETE" \
  -d "config.methods[]=PATCH" \
  -d "config.methods[]=OPTIONS" \
  -d config.credentials=true \
  -d config.max_age=3600

# ---------------------------------------------------------------------------
# 11. Upload SSL Certificate
# ---------------------------------------------------------------------------
echo -e "\n\n==> Uploading SSL certificate..."
curl -s -X POST ${KONG_ADMIN}/certificates \
  -F "cert=@./kong/ssl/server.crt" \
  -F "key=@./kong/ssl/server.key" \
  -F "snis[]=localhost"

echo -e "\n\n============================================="
echo " Setup complete!"
echo "============================================="
echo "Kong Proxy HTTPS: https://localhost:8443"
echo "Kong Proxy HTTP:  http://localhost:8000"
echo "Kong Admin API:   http://localhost:8001"
