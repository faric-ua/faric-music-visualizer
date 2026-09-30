#!/data/data/com.termux/files/usr/bin/bash
set -u

ASSET_DIR="$HOME/faric-music-visualizer-assets"

echo "========================================"
echo " FARIC Assets — оновлення з GitHub"
echo "========================================"
echo

if [ ! -d "$ASSET_DIR/.git" ]; then
  echo "FARIC assets ще не підключені."
  echo "Запусти пункт «Підключити / відновити FARIC assets»."
  exit 2
fi

cd "$ASSET_DIR" || exit 1

if ! git diff --quiet || ! git diff --cached --quiet || [ -n "$(git ls-files --others --exclude-standard)" ]; then
  echo "STOP: є локальні зміни. Я їх не перезаписую."
  echo
  git status --short
  echo
  echo "Спочатку збережи їх через пункт меню «Зберегти FARIC assets у GitHub»."
  exit 2
fi

branch="$(git branch --show-current)"
[ -n "$branch" ] || { echo "Не вдалося визначити гілку."; exit 1; }

before="$(git rev-parse HEAD)"

git fetch --prune origin "+refs/heads/$branch:refs/remotes/origin/$branch" || exit 1
git merge --ff-only "refs/remotes/origin/$branch" || exit 1
git lfs pull || exit 1

after="$(git rev-parse HEAD)"

echo
if [ "$before" = "$after" ]; then
  echo "PASS: FARIC assets уже актуальні."
else
  echo "PASS: FARIC assets оновлено."
  echo "Було:  $(printf '%.10s' "$before")"
  echo "Стало: $(printf '%.10s' "$after")"
fi
echo "Папка: $ASSET_DIR"
