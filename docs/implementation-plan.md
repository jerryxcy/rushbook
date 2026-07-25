# 實作計畫

RushBook依照線性的Lessons實作，不使用日曆天數切分。每一課只導入一項
architecture change，讓repository維持可執行，同步更新教學文件、通過quality
gate，最後建立annotated Git tag。

課程索引位於[docs/lessons/README.md](lessons/README.md)。
新增課程時，課程作者可從[Lesson 文件範本](maintainers/lesson-template.md)開始。

## 發布規則

1. 一個published lesson等於一個green commit。
2. Commit必須同時包含code、tests與完成的lesson document。
3. Commit前必須實際執行lesson內的所有commands。
4. 只要code改變了architecture或product truth，就必須在同一個commit更新文件。
5. 每個lesson commit都建立annotated `lesson-NN-slug` tag。
6. 不rewrite已經push的lesson history。
7. Main保持線性；學習者需要練習時，從tag建立自己的branch。
8. Commit前必須執行敏感資料檢查；真實credentials絕不進入Git。

完整規則與事件處理方式請見[敏感資料與憑證政策](security.md)。

範例：

```bash
git show lesson-06-transactional-outbox
git diff lesson-05-concurrency-tests..lesson-06-transactional-outbox
git switch -c my-outbox-exercise lesson-05-concurrency-tests
```

## Lesson quality gate

建立tag前必須確認：

- Gradle build成功；
- unit tests與該課相關integration tests通過；
- formatting與static checks通過；
- 如果該課導入container或cluster，對應smoke tests必須通過；
- 文件中的expected output與實際證據一致；
- lesson包含理解題與完整答案；
- staged內容通過敏感資料檢查；
- 人工閱讀`git diff --cached`，確認沒有scanner無法辨識的敏感值；
- 產生的artifacts已正確ignore，working tree乾淨。

## Test layers

### Unit tests

在不啟動infrastructure的情況下，測試state transitions、validation、message
mapping與error semantics。

### PostgreSQL integration tests

使用Testcontainers啟動真實PostgreSQL，驗證row locks、constraints、
transactions、lazy expiration、outbox claiming與Inbox idempotency。Mock
database不能作為concurrency correctness的證據。

### Kafka integration tests

使用Testcontainers Kafka驗證publishing、key selection、consumer redelivery、
retries與DLQ routing。

### Kubernetes smoke tests

建立乾淨kind cluster、載入immutable image tags、以Helm安裝infrastructure與
RushBook、等待readiness、執行一次end-to-end Booking；失敗時收集cluster
diagnostics。

### Load與failure tests

使用k6產生burst traffic。透過文件化scripts scale或kill components，再以API
results、database state、Kafka lag、metrics與logs驗證行為。

## 完成定義

RushBook達到portfolio-ready時必須符合：

- 所有domain invariants都有concurrent integration tests；
- Kafka unavailable時Booking仍成功，恢復後outbox會發布；
- consumer redelivery只產生一個Notification effect；
- retries與DLQ行為可觀察且有測試；
- Simulation Dashboard能展示真實end-to-end run；
- Helm能把系統部署到乾淨kind cluster；
- Grafana能解釋至少三個injected failures；
- GitHub Actions會重複執行kind smoke test；
- 每課都有green commit、annotated tag、可執行證據、問題與答案。

## Stretch lessons

核心curriculum完成後才考慮：

- Booking cancellation與`BookingCancelled` ordering；
- Redis admission control或rate limiting；
- retry topics與manual DLQ replay；
- Avro與Schema Registry；
- 三node Kafka durability experiments；
- strict FIFO waiting room；
- managed-cloud deployment與Terraform。
