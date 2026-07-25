# 敏感資料與憑證政策

RushBook 是公開的學習專案。真實的 credentials 一旦進入 Git commit，即使
之後刪除檔案，仍可能留在 history、fork、cache 或 clone 中。因此我們同時
使用 ignore rules、staged-content scanner、人工 review，未來也會在 CI
重複檢查。

## 絕對不能提交的內容

- 真實的 passwords、API keys、access tokens 與 refresh tokens；
- private keys、keystores 與 signing certificates；
- 含有權限的 `.env`、kubeconfig、cloud credentials 或 service-account
  files；
- production database URLs、connection strings 或其他內含 secret 的設定；
- 從本機或 CI 產生、可能夾帶敏感資訊的 diagnostics 與 state files。

## Repository 可以包含的內容

- `.env.example`，但只能放明顯無效的 placeholders；
- Kubernetes `Secret` 或 Helm values 的結構範例，但不能放真實值；
- local development 使用的無權限示範帳密，例如
  `RUSHBOOK_DB_PASSWORD=replace-me`；
- 說明如何從 environment variables 或 secret store 注入值的文件。

範例值必須一眼就能辨識為不可使用，例如 `replace-me`、`example.invalid` 或
`changeme-for-local-only`。不要貼上真實值後再打算於 commit 前替換。

## Local 與 CI 注入方式

- Local：複製 `.env.example` 為被 Git ignore 的 `.env`，再填入本機值。
- Kubernetes：manifest 只引用 Secret 名稱；真實 Secret 由部署環境建立。
- GitHub Actions：從 repository/environment secrets 注入，workflow 不寫死
  值，也不將值輸出到 logs。
- Cloud：使用 cloud secret manager 或 workload identity，不提交長效
  credentials。

## 每次 commit 前

```bash
git add --all
git diff --cached --stat
git diff --cached
scripts/check-sensitive-data.sh
```

Scanner 會檢查 staged filenames，以及常見的高可信度 secret signatures。
它只能降低誤提交風險，不能保證找出所有敏感資料；最後仍必須人工閱讀 staged
diff。

## 如果 secret 已經進入 Git

1. 立刻撤銷或輪替 credential；刪除 commit 不是撤銷。
2. 停止 push 或部署相關內容。
3. 確認 secret 曾經出現在哪些 commits、remotes、logs 與 artifacts。
4. 視情況清理 Git history，並通知所有已 clone repository 的協作者。
5. 加入能防止同類事件再次發生的 ignore rule 或 scanner rule。

安全順序是「先 revoke/rotate，再清理 history」，因為無法證明已洩漏的值從未
被複製。
