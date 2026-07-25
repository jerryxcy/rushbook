# 將 Holds 與 Bookings 建模為 Registrations

RushBook 將 Registration 儲存為一筆 lifecycle record：從 Held state 開始，
再 transition 到 Booked 或 Expired。Hold 與 Booking 仍是不同的 domain
concepts，但使用單一 aggregate 與 table，比在獨立的 Hold 與 Booking tables
之間移動資料，更容易強制執行 confirmation、attendee uniqueness、history
與 conditional state transitions。
