# RushBook 課程

RushBook透過architecture deltas學習。每一課回答一個問題，只加入足以證明答案
的最小code，並以一個可執行的Git checkpoint結束。

## 如何學習一課

Lesson 00 是 5–10 分鐘的快速導讀；以下完整流程從 Lesson 01 開始使用：

1. 閱讀「目標」、「核心觀念」與「問題情境」。
2. 在閱讀「實作導覽」前先停下來，預測自己的設計。
3. 檢查該lesson的Git diff。
4. 執行tests與experiment。
5. 不展開答案，先自行回答「理解題」。
6. 再與「答案與預期推理」比較。

## Lessons

### [Lesson 00 — 先看懂 RushBook](00-design-the-system.md)

透過搶名額情境與一張簡化架構圖，快速理解PostgreSQL、Kafka與Kubernetes在
專案中的分工。

證據：能用自己的話說明RushBook解決的問題與三個核心元件的責任。

### Lesson 01 — 建立two-service skeleton

建立Java 25／Spring Boot 4.1 Gradle monorepo、Booking Service、Notification
Service、build conventions、health endpoints與第一個CI build。

證據：`./gradlew test`能啟動兩個application contexts。

### Lesson 02 — 建立Events與database migrations

導入PostgreSQL、Flyway、Spring JDBC、schema ownership、Event creation、
validation與Testcontainers。

證據：migrations能在真實PostgreSQL執行，Event API tests通過。

### Lesson 03 — 防止超賣

在Event row lock保護下建立Held Registrations，並以多threads、多connections
證明capacity invariant。

證據：一百位Attendees競爭十個Spots，恰好十個Holds成功。

### Lesson 04 — 完成Registration lifecycle

加入per-Event Hold Period、lazy expiration、confirmation、duplicate-request
behavior與conditional transitions。

證據：expiration與Confirm races只會得到一個合法結果。

### Lesson 05 — 讓concurrency證據可重複

建立deterministic stress-style integration tests與k6 burst scenario。量測
transaction latency並解釋Event row hotspot。

證據：重複執行永不超賣，並能產生有意義的latency results。

### Lesson 06 — 加入transactional outbox

讓Booking與`BookingConfirmed` outbox data atomic commit，並以`SKIP LOCKED`
claim outbox work。

證據：concurrent publishers不會把同一筆claimed row當成新工作重複處理，
unpublished rows在process restart後仍存在。

### Lesson 07 — 發布versioned Kafka messages

加入Kafka、JSON Schema、`bookingId` record keys、topic configuration、
producer metrics與outbox publication state。

證據：Kafka在預期partition key上收到符合schema的message。

### Lesson 08 — 建立idempotent Notification consumer

加入Notification schema、Inbox pattern、delivery record、consumer group與
delivery feed API。

證據：刻意redeliver只會產生一次Notification business effect。

### Lesson 09 — 處理retries與DLQ

加入三次bounded blocking retries、exponential backoff、poison-message
injection、DLQ publication與可見failure state。

證據：transient failure會恢復；persistent failure只進入DLQ一次；partition
lag能展示head-of-line blocking。

### Lesson 10 — 建立Simulation Dashboard

加入static HTML與JavaScript、synthetic Attendees、可調concurrency、REST
polling、真實live pipeline與最近的Notification feed。

證據：browser能展示一百位Attendees競爭十個Spots，且不繞過公開APIs。

### Lesson 11 — 安全地containerize

建立reproducible non-root images、immutable tags、container health checks與
local image build conventions。

證據：兩個services都以non-root身分執行並正確回應health probes。

### Lesson 12 — 建立kind platform

建立disposable kind cluster、安裝Strimzi、宣告single-node KRaft Kafka，並
部署具有storage與probes的local-only PostgreSQL StatefulSet。

證據：從checked-in configuration重建cluster後，所有dependencies都ready。

### Lesson 13 — 使用Helm封裝RushBook

建立專案Helm chart，包含Deployments、Services、ConfigMaps、Secrets、probes、
resources與environment overrides。

證據：能在乾淨cluster完成install、upgrade、rollback與uninstall。

### Lesson 14 — 觀測系統

輸出Micrometer metrics、安裝Prometheus與Grafana，並建立HTTP、Registration、
outbox、Kafka、Notification與Kubernetes dashboards。

證據：dashboard變化能對應已知simulation與backlog。

### Lesson 15 — 破壞並恢復platform

加入安全的external fault scripts與runbooks，涵蓋consumer outage、Booking pod
termination、Kafka restart與PostgreSQL unavailable。

證據：每份runbook都能在recovery前後預測、觀察並解釋系統行為。

### Lesson 16 — 在GitHub Actions測試Kubernetes

Build images、建立kind、以Helm安裝、執行end-to-end smoke tests，失敗時輸出
diagnostics。

證據：乾淨的hosted runner能重現完整deployment與test。
