#!/data/data/com.termux/files/usr/bin/bash
set -eu

GH_REPO="faric-ua/faric-music-visualizer"
WORKFLOW="android.yml"
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT"

command -v gh >/dev/null 2>&1 || {
  echo "GitHub CLI (gh) не знайдено."
  exit 1
}

gh auth status -h github.com >/dev/null 2>&1 || {
  echo "GitHub CLI не авторизований."
  exit 1
}

echo "Android workflow — останні 3 runs"
echo "Repo: $GH_REPO"
echo "Local HEAD: $(git rev-parse --short=12 HEAD)"
echo

gh run list   --repo "$GH_REPO"   --workflow "$WORKFLOW"   --limit 3

echo
echo "✓ = успіх, X = failure/cancelled, * = ще виконується."
