# Persistence 使用 explicit SQL

兩個 services 都使用 Spring JDBC 與 Flyway，而不使用 JPA。RushBook 的核心
行為依賴清楚可見的 PostgreSQL locking、conditional updates、`SKIP LOCKED`
與 uniqueness constraints。Explicit SQL 能讓 transaction 與 concurrency
semantics 容易 review，不會被 ORM flush 或 persistence-context behavior
遮蔽。
