# 使用 Strimzi 維運 Kafka

RushBook 透過 Helm 安裝 Strimzi Cluster Operator，並用 `Kafka` 與
`KafkaNodePool` custom resources 宣告 KRaft Kafka cluster。Local development
先使用一個同時擔任 controller 與 broker 的 node，以節省資源；之後再使用
三個 nodes 進行 replication 與 broker-failure experiments。我們刻意不自行
編寫 Kafka StatefulSets。
