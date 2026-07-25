# 使用 commit 順序，而非 strict FIFO

當多個 Hold 同時競爭同一個 Spot 時，RushBook 將名額給最先成功 commit 的
request。第一版保證 capacity correctness，但不保證 request arrival order；
strict FIFO 需要 centralized admission queue 與具有全域意義的 request
ordering。未來可將 waiting room 作為獨立能力加入。
