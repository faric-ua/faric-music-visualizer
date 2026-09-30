#!/data/data/com.termux/files/usr/bin/bash
set -u

MAIN_REPO="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
ASSET_DIR="$HOME/faric-music-visualizer-assets"

pause_local() {
  echo
  printf "Натисни Enter, щоб продовжити..."
  read -r _
}

echo "========================================"
echo " FARIC Assets — підключення / відновлення"
echo "========================================"
echo
echo "Папка:"
echo "  $ASSET_DIR"
echo

if ! command -v git >/dev/null 2>&1; then
  echo "Встановлюю git..."
  pkg install -y git || exit 1
fi

if ! command -v git-lfs >/dev/null 2>&1 && ! git lfs version >/dev/null 2>&1; then
  echo "Встановлюю git-lfs..."
  pkg install -y git-lfs || exit 1
fi

git lfs install || exit 1

MAIN_REMOTE="$(git -C "$MAIN_REPO" remote get-url origin 2>/dev/null || true)"
case "$MAIN_REMOTE" in
  git@github.com:*)
    ASSET_REMOTE="git@github.com:faric-ua/faric-music-visualizer-assets.git"
    ;;
  https://github.com/*)
    ASSET_REMOTE="https://github.com/faric-ua/faric-music-visualizer-assets.git"
    ;;
  *)
    ASSET_REMOTE="https://github.com/faric-ua/faric-music-visualizer-assets.git"
    ;;
esac

echo "GitHub:"
echo "  $ASSET_REMOTE"
echo

if [ -d "$ASSET_DIR/.git" ]; then
  echo "Репозиторій уже існує. Оновлюю його..."
  cd "$ASSET_DIR" || exit 1

  if ! git diff --quiet || ! git diff --cached --quiet || [ -n "$(git ls-files --others --exclude-standard)" ]; then
    echo
    echo "STOP: у FARIC assets є локальні зміни."
    echo "Спочатку використай у меню «Зберегти FARIC assets у GitHub»"
    echo "або розбери зміни вручну."
    echo
    git status --short
    exit 2
  fi

  branch="$(git branch --show-current)"
  [ -n "$branch" ] || branch="main"

  git fetch --prune origin "+refs/heads/$branch:refs/remotes/origin/$branch" || exit 1
  git merge --ff-only "refs/remotes/origin/$branch" || exit 1
  git lfs pull || exit 1
else
  if [ -e "$ASSET_DIR" ]; then
    echo "STOP: $ASSET_DIR існує, але це не Git-репозиторій."
    echo "Я нічого не видаляю автоматично."
    exit 2
  fi

  echo "Клоную FARIC master-assets..."
  git clone "$ASSET_REMOTE" "$ASSET_DIR" || exit 1
  cd "$ASSET_DIR" || exit 1
  git lfs pull || exit 1
fi

cd "$ASSET_DIR" || exit 1

POINTERS=0
while IFS= read -r -d '' f; do
  if head -n 1 "$f" 2>/dev/null | grep -q '^version https://git-lfs.github.com/spec/v1$'; then
    POINTERS=$((POINTERS + 1))
  fi
done < <(find masters -type f \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.webp' \) -print0 2>/dev/null)

MASTER_COUNT="$(find masters -type f \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.webp' \) 2>/dev/null | wc -l | tr -d ' ')"

echo
echo "========================================"
if [ "$POINTERS" -eq 0 ]; then
  echo "PASS: FARIC assets готові."
else
  echo "WARNING: знайдено LFS pointer-файлів: $POINTERS"
  echo "Спробуй ще раз пункт «Оновити FARIC assets з GitHub»."
fi
echo "Master-файлів: $MASTER_COUNT"
echo "Папка: $ASSET_DIR"
echo "Розмір: $(du -sh "$ASSET_DIR" 2>/dev/null | awk '{print $1}')"
echo "========================================"
