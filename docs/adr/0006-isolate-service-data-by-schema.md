# 以 schema 隔離 service data

為了降低維運複雜度，Booking Service 與 Notification Service 共用一個
PostgreSQL instance，但各自擁有獨立的 schema、Flyway migrations 與
credentials。任何 service 都不得 query 另一個 service 的 schema；資料只能
透過 Kafka 或 HTTP APIs 交換。未來若將 schemas 拆成不同 databases，也不必
改變 domain ownership。
