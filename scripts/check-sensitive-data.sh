#!/usr/bin/env bash
set -euo pipefail

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "錯誤：請在 Git repository 內執行此腳本。" >&2
  exit 2
fi

blocked_file_found=0

while IFS= read -r -d '' staged_file; do
  file_name="${staged_file##*/}"

  case "$file_name" in
    .env.example)
      ;;
    .env|.env.*|id_rsa|id_ed25519|*.pem|*.key|*.p12|*.pfx|*.jks|*.keystore|\
    kubeconfig*|credentials.json|service-account*.json|*.auto.tfvars|*.tfstate|\
    *.tfstate.*)
      echo "錯誤：staged 檔案疑似包含 credential：$staged_file" >&2
      blocked_file_found=1
      ;;
  esac
done < <(git diff --cached --name-only --diff-filter=ACMR -z)

if ((blocked_file_found != 0)); then
  echo "請移除該檔案的 staged 狀態，改用 placeholder 或外部 secret store。" >&2
  exit 1
fi

secret_matches="$(
  git grep --cached -IEn \
    -e 'AKIA[0-9A-Z]{16}' \
    -e 'ASIA[0-9A-Z]{16}' \
    -e 'gh[pousr]_[A-Za-z0-9_]{20,}' \
    -e 'github_pat_[A-Za-z0-9_]{20,}' \
    -e 'AIza[0-9A-Za-z_-]{35}' \
    -e 'sk-[A-Za-z0-9_-]{20,}' \
    -e '-----BEGIN ([A-Z0-9]+ )?PRIVATE KEY-----' \
    -- . ':!scripts/check-sensitive-data.sh' || true
)"

if [[ -n "$secret_matches" ]]; then
  echo "錯誤：staged 內容符合常見的 secret signature：" >&2
  echo "$secret_matches" >&2
  echo "請確認內容、撤銷真實 credential，並從 staged changes 移除。" >&2
  exit 1
fi

echo "通過：staged filenames 與內容未發現已知的高可信度 secret signature。"
echo "提醒：scanner 無法取代人工檢查，commit 前仍要閱讀 git diff --cached。"
