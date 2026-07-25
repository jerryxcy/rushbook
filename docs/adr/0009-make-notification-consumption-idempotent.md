# 讓 Notification consumption 具備 idempotency

Notification Service 會在 Inbox table 記錄每一個 Kafka `messageId`，並與
notification delivery result 在同一個 PostgreSQL transaction commit。Kafka
offset 只會在 transaction 成功後 commit，因此 crash 後發生 at-least-once
redelivery 時，不會產生重複的 business effect。
