# 在 monorepo 中使用兩個 services

RushBook 使用一個 Gradle monorepo，內含可獨立部署的 Booking Service 與
Notification Service。Booking Service 擁有 reservation correctness、outbox
publisher 與 simulation UI；Notification Service 消費 booking events 並提供
delivery results。這樣能展示 Kafka consumer 的獨立 scaling 與 failure
isolation，又不會把小型 domain 拆成不必要的 microservices。
