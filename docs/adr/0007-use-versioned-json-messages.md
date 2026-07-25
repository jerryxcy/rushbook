# 使用有版本的 JSON messages

RushBook 將 Kafka messages serialize 為 JSON，其中包含明確的 message type、
schema version、occurrence time，以及由 JSON Schema 記錄的穩定 payload
contract。Avro 與 Schema Registry 延後處理，讓第一版先聚焦在 Kafka
delivery、partitioning、retries 與 compatibility，不必同時維運額外的 schema
service。
