# RushBook

RushBook 管理限量名額活動的正確報名流程，即使許多 Attendee 同時競爭少量、
彼此等價的 Spot，也必須維持所有 domain invariants。

## Language

**Event**:
Organizer 開放報名的一場限量名額活動。
_避免使用_：Session、show、offering

**Spot**:
Event 容量中一個彼此等價的單位。Spot 不是指定座位，也不是特定時間資源。
_避免使用_：Seat、ticket、slot

**Organizer**:
建立及管理 Event 的人。
_避免使用_：Admin、host、owner

**Attendee**:
嘗試或已經取得 Event 報名資格的人。
_避免使用_：User、customer、buyer

**Hold**:
Registration 對一個 Spot 的暫時占用。Attendee 若未在 Event 的 Hold Period
內確認，Hold 就會到期。
_避免使用_：Temporary booking、reservation

**Booking**:
Attendee 已確認，並取得 Event 一個 Spot 的 Registration。
_避免使用_：Order、purchase、ticket

**Registration**:
一位 Attendee 對一場 Event 的一次報名嘗試。它從 Hold 開始，最後成為
Booking 或到期的嘗試。
_避免使用_：Order、transaction

**Hold Period**:
Event 給 Attendee 確認新 Hold 的時間長度。修改 Hold Period 不會改變既有
Hold 的到期時間。
_避免使用_：Booking timeout、reservation timeout
