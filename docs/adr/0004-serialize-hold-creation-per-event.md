# 以 Event 為單位 serialize Hold creation

RushBook 會先用 `SELECT ... FOR UPDATE` lock Event row，再檢查已使用的
capacity 並建立 Hold。這會 serialize 熱門 Event 的 Hold creation，也可能
限制 throughput；但在不使用 Redis 或 distributed lock 的情況下，它能讓
「絕不超賣」這項 invariant 在多個 application replicas 之間仍容易證明。
我們會先用 load tests 測量 contention，再考慮更複雜的設計。
