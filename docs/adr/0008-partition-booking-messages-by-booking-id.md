# 以 Booking ID partition Booking messages

Kafka booking messages 使用 `bookingId` 作為 record key，讓同一筆 Booking
的 messages 保持順序，不同 Bookings 則能分散到多個 partitions。若使用
RushBook Event ID，熱門 Event 會形成 hot partition；若使用 unique message
ID，則會失去有用的 per-Booking ordering。
