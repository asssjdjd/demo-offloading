# Group 2 - Performance and Reliability (Modular Files)

Folder nay chua cac block cau hinh rieng cho nhom 2 de tranh conflict khi merge.

Quy uoc file:

- `10-*`: upstream/targets/load balancing
- `20-*`: cache va plugin performance
- `30-*`: circuit breaker (custom plugin config)

Cach dung:

1. Chinh sua cac file trong folder nay.
2. Khi can integrate, copy block can thiet vao `../kong.yml` trong marker:

```yaml
# >>> GROUP2-PERF START
# <<< GROUP2-PERF END
```

3. Tranh sua truc tiep block plugin cua nhom khac trong `kong.yml`.
