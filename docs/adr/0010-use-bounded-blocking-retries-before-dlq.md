# 進入 DLQ 前使用有上限的 blocking retries

Notification Service 對失敗的 Kafka record 在原 partition 進行三次短
backoff retry，之後才發布到 dead-letter topic。這能保留 partition order，
代價則是暫時的 head-of-line blocking。Non-blocking retry topics 與 manual
replay 會等到較簡單的 failure behavior 已有測量結果後再加入。
