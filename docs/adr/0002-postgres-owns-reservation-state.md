# PostgreSQL 擁有 reservation state

PostgreSQL 是 Holds、Bookings、capacity 與 Hold expiration 的 source of
truth。第一版不使用 Redis，因為用 Redis TTL 或 distributed lock 保證
reservation correctness，會在 RushBook 證明較簡單的 transactional model
之前，就引入第二個 source of truth。未來可將 Redis 用於 admission control
或 rate limiting。
