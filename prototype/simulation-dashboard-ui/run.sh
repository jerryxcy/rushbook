#!/usr/bin/env bash
set -euo pipefail

prototype_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "THROWAWAY PROTOTYPE: http://localhost:4173/?variant=A"
python3 -m http.server 4173 --directory "$prototype_dir"
