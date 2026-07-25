# 不將敏感資料提交至 Git

真實的 passwords、tokens、API keys、private keys、service-account files、
kubeconfigs 與其他 credentials 必須留在 Git 之外。Repository 只提供不具
權限的 placeholders 與 `.env.example`；執行環境透過 environment variables
或 CI secret store 注入實際值。

每個 lesson commit 前都必須執行 staged-content scanner，並 review 即將
提交的 diff。Scanner 只能降低常見誤提交的風險，無法取代人工檢查；若敏感
資料曾進入 Git history，必須立即撤銷或輪替該 credential，再清理 history。
