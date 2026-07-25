# RushBook

RushBook 是一套限量名額活動報名系統，也是一個用來學習 **Kafka** 與
**Kubernetes** 的實作專案。

## 情境

一場活動只有 **10 個名額**，卻有 **100 個人同時報名**。

RushBook 必須保證不超賣，並在報名成功後透過 Kafka 通知其他服務。整套系統
最後會部署到 Kubernetes，練習 deployment、scaling、observability 與 failure
recovery。

## 從這裡開始

1. [Lesson 00：5–10 分鐘看懂 RushBook](docs/lessons/00-design-the-system.md)
2. [Lesson 01：建立 two-service skeleton](docs/lessons/01-two-service-skeleton.md)
3. [Lesson 02：建立 Events 與 database migrations](docs/lessons/02-events-and-database-migrations.md)
4. [Lesson 03：防止超賣](docs/lessons/03-prevent-overselling.md)
5. [查看完整課程目錄](docs/lessons/README.md)

## 會用到的技術

- Java 25、Spring Boot 4 與 Gradle
- PostgreSQL、Spring JDBC 與 Flyway
- Apache Kafka、Strimzi 與 KRaft
- Docker、Kubernetes、kind 與 Helm
- Prometheus、Grafana、Testcontainers 與 k6

## 目前進度

- [x] Lesson 00：理解情境與系統架構
- [x] Lesson 01：建立兩個 Spring Boot services
- [x] Lesson 02：建立 Events 與 database migrations
- [x] Lesson 03：防止超賣
- [ ] Lesson 04：完成 Registration lifecycle
