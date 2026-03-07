# Kong Gateway - Thiet Ke Nhom 2 (Performance and Reliability)

Muc tieu tai lieu nay:

- Xac dinh can bo sung gi cho Nhom 2 trong Kong.
- Thiet ke cach tach rieng file/folder de giam conflict khi merge.
- Dua ra lo trinh trien khai theo buoc, khong pha vo cau hinh hien tai.

## 1) Hien trang Nhom 2 (theo `kong/kong.yml`)

Dang co:

- `rate-limiting` (global): minute 100, hour 5000
- `request-size-limiting` (global): 10 MB
- timeout va retries o service `user-service`

Chua co:

- `proxy-cache` (response caching)
- cau hinh load-balancing da instance/thuc su (upstream + targets)
- circuit breaker plugin

## 2) Muc tieu bo sung cho Nhom 2

Can them 3 nhom tinh nang:

1. Response Caching: giam tai backend cho cac GET endpoint.
2. Load Balancing: route den nhieu target backend thay vi 1 host duy nhat.
3. Circuit Breaker: fail-fast khi backend loi lien tuc.

## 3) Thiet ke tranh conflict khi merge

Khong sua truc tiep mot khoi lon trong `kong/kong.yml`.
Thay vao do, tach theo folder con rieng cho Nhom 2:

```text
kong/
|-- kong.yml
|-- group2-performance/
|   |-- README.md
|   |-- 10-upstreams.targets.yml
|   |-- 20-plugins.proxy-cache.yml
|   |-- 30-plugins.circuit-breaker.yml
|   `-- merge-notes.md
`-- plugins/
    `-- circuit-breaker/
        |-- handler.lua
        `-- schema.lua
```

Y nghia:

- Team A (security, observability) tiep tuc lam viec tren `kong/kong.yml`.
- Team B (group 2) chi thao tac trong `kong/group2-performance/*` va `kong/plugins/circuit-breaker/*`.
- Khi merge, chi can copy/append cac block trong file con vao `kong/kong.yml` theo thu tu danh so `10/20/30`.

## 4) Noi dung de xuat cho tung file con

## 4.1 `kong/group2-performance/10-upstreams.targets.yml`

```yaml
upstreams:
  - name: user-service-upstream
    algorithm: round-robin
    healthchecks:
      active:
        type: http
        http_path: /actuator/health
        timeout: 2
        concurrency: 5
        healthy:
          interval: 5
          successes: 2
        unhealthy:
          interval: 5
          http_failures: 3
          tcp_failures: 3
          timeouts: 3
    targets:
      - target: user-service:8080
        weight: 100
      # Neu scale service, them target tiep theo o day
      # - target: user-service-2:8080
      #   weight: 100
```

Sau do doi `services.user-service.host` trong `kong.yml` thanh `user-service-upstream`.

## 4.2 `kong/group2-performance/20-plugins.proxy-cache.yml`

```yaml
plugins:
  - name: proxy-cache
    route: user-service-protected
    config:
      response_code:
        - 200
      request_method:
        - GET
      content_type:
        - application/json
      cache_ttl: 30
      strategy: memory
      vary_headers:
        - Authorization
```

Ghi chu:

- `strategy: memory` phu hop demo local.
- Neu production, nen dung Redis strategy.

## 4.3 `kong/group2-performance/30-plugins.circuit-breaker.yml`

```yaml
plugins:
  - name: circuit-breaker
    route: user-service-protected
    config:
      failure_threshold: 5
      success_threshold: 2
      open_timeout_ms: 30000
      fallback_status: 503
      fallback_body: '{"message":"Service temporarily unavailable"}'
```

Plugin nay la custom plugin, can folder `kong/plugins/circuit-breaker/`.

## 5) Skeleton custom plugin circuit-breaker

## 5.1 `kong/plugins/circuit-breaker/schema.lua`

```lua
local typedefs = require "kong.db.schema.typedefs"

return {
  name = "circuit-breaker",
  fields = {
    { consumer = typedefs.no_consumer },
    { protocols = typedefs.protocols_http },
    {
      config = {
        type = "record",
        fields = {
          { failure_threshold = { type = "integer", required = true, default = 5 } },
          { success_threshold = { type = "integer", required = true, default = 2 } },
          { open_timeout_ms = { type = "integer", required = true, default = 30000 } },
          { fallback_status = { type = "integer", required = true, default = 503 } },
          { fallback_body = { type = "string", required = true, default = '{"message":"Service temporarily unavailable"}' } }
        }
      }
    }
  }
}
```

## 5.2 `kong/plugins/circuit-breaker/handler.lua`

```lua
local plugin = {
  PRIORITY = 1005,
  VERSION = "0.1.0",
}

function plugin:access(conf)
  -- TODO:
  -- 1) Doc state tu shared dict/redis
  -- 2) Neu OPEN va chua het timeout thi tra fallback ngay
  -- 3) Neu HALF-OPEN thi cho phep mot so request thu nghiem
end

function plugin:header_filter(conf)
  -- TODO:
  -- cap nhat failure/success counter dua tren status code backend
end

return plugin
```

## 6) Cap nhat Docker de nap plugin custom moi

Trong `docker-compose.yml`, mo rong:

```yaml
environment:
  KONG_PLUGINS: "bundled,jwt-header-injector,circuit-breaker"
```

Khong can doi volume vi da mount ca `./kong/plugins:/etc/kong/plugins:ro`.

## 7) Quy trinh merge de giam conflict

De xuat workflow cho team:

1. Team Group 2 chi commit trong:
   - `kong/group2-performance/*`
   - `kong/plugins/circuit-breaker/*`
2. Tao PR rieng "merge integration" de append block vao `kong/kong.yml`.
3. Su dung marker ro rang trong `kong/kong.yml`:

```yaml
# >>> GROUP2-PERF START
# (paste tu kong/group2-performance/*.yml)
# <<< GROUP2-PERF END
```

4. Neu co conflict, chi xu ly trong block marker, khong anh huong cac nhom khac.

## 8) Thu tu uu tien implement

1. Upstream + active healthcheck (de co load-balancing that).
2. Proxy-cache cho cac GET endpoint nhieu traffic.
3. Circuit-breaker plugin (skeleton truoc, toi uu sau).

## 9) Checklist truoc khi release

- [ ] Kong start duoc voi plugin moi (`circuit-breaker`).
- [ ] Route GET cache hoat dong dung TTL.
- [ ] Healthcheck danh dau target unhealthy khi backend loi.
- [ ] Fallback 503 tra ve khi circuit OPEN.
- [ ] Khong thay doi hanh vi security hien co (`jwt`, `acl`).
