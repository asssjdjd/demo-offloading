# Kong Gateway Offloading Audit

Muc tieu: doi chieu nhung gi da co trong folder `kong/` voi 3 nhom chuc nang trong `README.md`.

Nguon doi chieu:

- `demo-offloading/kong/kong.yml`
- `demo-offloading/kong/kong.conf`
- `demo-offloading/kong/plugins/jwt-header-injector/handler.lua`
- `demo-offloading/kong/setup-kong-admin.sh`
- `demo-offloading/kong/ssl/generate-certs.sh`
- `demo-offloading/docker-compose.yml` (de xac nhan plugin custom duoc enable runtime)

## 1) Security and Identity Offloading

| Chuc nang trong README | Trang thai | Bang chung                                                                                    | Ghi chu                                 |
| ---------------------- | ---------- | --------------------------------------------------------------------------------------------- | --------------------------------------- |
| Authentication (JWT)   | Da co      | `kong.yml`: plugin `jwt` tren route `user-service-protected` va `user-service-admin`          | Xac thuc theo claim `iss`, verify `exp` |
| Token validation       | Da co      | `kong.yml`: `claims_to_verify: exp`                                                           | Validation co ban cua JWT da bat        |
| Authorization (ACL)    | Da co      | `kong.yml`: plugin `acl` cho `user-group`, `admin-group`                                      | Route admin chi cho `admin-group`       |
| SSL/TLS termination    | Da co      | `kong.conf`: `proxy_listen ... 8443 ssl`, `ssl_cert`, `ssl_cert_key`; `ssl/generate-certs.sh` | Da co cert self-signed cho dev          |

Nhan xet bo sung:

- Co `consumers`, `jwt_secrets`, `acls` trong `kong.yml`.
- Co custom plugin `jwt-header-injector` de inject `X-User-*` tu JWT claim (`handler.lua`).

## 2) Performance and Reliability Offloading

| Chuc nang trong README             | Trang thai | Bang chung                                                               | Ghi chu                                                                                                                  |
| ---------------------------------- | ---------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------ |
| Rate limiting                      | Da co      | `kong.yml`: plugin global `rate-limiting` (minute 100, hour 5000)        | Da co gioi han request                                                                                                   |
| Throttling / request size limiting | Da co      | `kong.yml`: plugin `request-size-limiting` (`10 MB`)                     | Gioi han kich thuoc payload                                                                                              |
| Response caching                   | Chua thay  | Khong co plugin `proxy-cache` trong `kong.yml`                           | Chua bat cache tai gateway                                                                                               |
| Load balancing                     | Mot phan   | Co khai bao service/route trong Kong                                     | Hien tai moi thay 1 upstream host `user-service:8080`, chua thay nhieu instance/upstream targets de can bang tai thuc su |
| Circuit breaker                    | Chua thay  | Khong co plugin/custom logic circuit breaker trong `kong.yml`/`plugins/` | Chua trien khai                                                                                                          |

Nhan xet bo sung:

- Co cau hinh timeout/retries o `services.user-service` (`retries`, `connect_timeout`, `read_timeout`, `write_timeout`) ho tro tang do ben.

## 3) Observability and Transformation

| Chuc nang trong README          | Trang thai | Bang chung                                                                              | Ghi chu                                                                |
| ------------------------------- | ---------- | --------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| Monitoring (Prometheus metrics) | Chua thay  | Khong co plugin `prometheus` trong `kong.yml`                                           | Chua co metrics plugin                                                 |
| Logging                         | Da co      | `kong.yml`: plugin `file-log`; `kong.conf`: access/error log ra stdout/stderr           | Logging gateway da bat                                                 |
| Request/Response transformation | Da co      | `kong.yml`: `request-transformer`, `response-transformer`; custom `jwt-header-injector` | Co xoa `Authorization`, them header trusted, lam sach response headers |
| Tracing (Jaeger/Zipkin)         | Chua thay  | Khong co plugin/cau hinh tracing trong `kong.yml`                                       | Chua trien khai tracing                                                |
| Analytics                       | Chua thay  | Khong co plugin analytics rieng                                                         | Chua co dashboard/analytics plugin                                     |

## Tong ket nhanh

- Da trien khai manh nhat: **Security/Identity** (JWT, ACL, SSL/TLS) va mot phan **Transformation + Logging**.
- Da co mot phan **Performance/Reliability** (rate limiting, request-size limiting, timeout/retry), nhung **chua co caching/circuit breaker day du**.
- **Observability nang cao** (Prometheus, tracing, analytics) hien chua thay trong folder `kong/`.

## Danh sach plugin dang thay trong cau hinh

- Built-in: `jwt`, `acl`, `rate-limiting`, `cors`, `request-size-limiting`, `request-transformer`, `response-transformer`, `file-log`
- Custom: `jwt-header-injector`

## Danh sach plugin co trong README nhung chua thay duoc cau hinh

- `proxy-cache`
- `prometheus`
- Tracing plugins/integration (Jaeger/Zipkin)
- Circuit breaker plugin (custom)
- Analytics plugin
