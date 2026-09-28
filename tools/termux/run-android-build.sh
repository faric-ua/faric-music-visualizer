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

if ! git diff --quiet ||
   ! git diff --cached --quiet ||
   [ -n "$(git ls-files --others --exclude-standard)" ]; then
  echo "Є локальні зміни. Build не запускаю."
  git status --short
  exit 1
fi

branch="$(git branch --show-current)"
test -n "$branch" || {
  echo "Не вдалося визначити поточну гілку."
  exit 1
}

git fetch --prune origin "+refs/heads/$branch:refs/remotes/origin/$branch"

local_sha="$(git rev-parse HEAD)"
remote_sha="$(git rev-parse "refs/remotes/origin/$branch")"

if [ "$local_sha" != "$remote_sha" ]; then
  echo "Локальний HEAD не збігається з origin/$branch."
  echo "Спочатку виконай пункт 3 — оновлення репозиторію."
  echo "Local:  $local_sha"
  echo "Remote: $remote_sha"
  exit 1
fi

echo "Запускаю Android workflow:"
echo "  repo:   $GH_REPO"
echo "  ref:    $branch"
echo "  source: $local_sha"
echo

gh workflow run "$WORKFLOW"   --repo "$GH_REPO"   --ref "$branch"

echo
echo "PASS: workflow dispatch відправлено."
echo "Перевір статус пунктом 10."
