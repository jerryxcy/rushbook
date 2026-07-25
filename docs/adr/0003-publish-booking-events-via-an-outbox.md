# 透過 outbox 發布 Booking events

Booking confirmation 在 PostgreSQL 中同步完成，並在同一個 transaction 寫入
`BookingConfirmed` outbox record。另一個 publisher 再將該 record 傳遞至
Kafka，因此 Kafka outage 不會阻擋 Booking，而 downstream consumers 仍能以
at-least-once delivery 收到 events。
