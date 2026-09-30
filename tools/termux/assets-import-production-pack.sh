#!/data/data/com.termux/files/usr/bin/bash
set -u

ASSET_DIR="$HOME/faric-music-visualizer-assets"
DOWNLOADS="$HOME/storage/downloads"

echo "========================================"
echo " FARIC Assets — імпорт production-pack"
echo "========================================"
echo

if [ ! -d "$ASSET_DIR/.git" ]; then
  echo "FARIC assets ще не підключені."
  echo "Спочатку використай пункт 11 у головному меню."
  exit 2
fi

if [ ! -d "$DOWNLOADS" ]; then
  echo "Termux storage ще не підключено."
  echo
  echo "Виконай один раз:"
  echo "  termux-setup-storage"
  echo
  echo "Потім повтори цей пункт меню."
  exit 2
fi

PACK="$(ls -1t "$DOWNLOADS"/FARIC_PRODUCTION_PACK_*.zip 2>/dev/null | head -n 1 || true)"

if [ -z "$PACK" ]; then
  echo "У Downloads не знайдено:"
  echo "  FARIC_PRODUCTION_PACK_*.zip"
  echo
  echo "Спочатку завантаж production-pack із чату в Downloads."
  exit 2
fi

echo "Знайдено найновіший пакет:"
echo "  $PACK"
echo

if ! command -v unzip >/dev/null 2>&1; then
  echo "Встановлюю unzip..."
  pkg install -y unzip || exit 1
fi

# Safety: only relative production paths + optional PACK_INFO.txt are allowed.
BAD="$(unzip -Z1 "$PACK" | grep -E '(^/|(^|/)\.\.(/|$))' || true)"
if [ -n "$BAD" ]; then
  echo "STOP: небезпечні шляхи в ZIP."
  echo "$BAD"
  exit 2
fi

UNKNOWN="$(unzip -Z1 "$PACK" | grep -Ev '^(production/|PACK_INFO\.txt$)' || true)"
if [ -n "$UNKNOWN" ]; then
  echo "STOP: пакет містить неочікувані файли:"
  echo "$UNKNOWN"
  exit 2
fi

MANIFEST_PATH="$(unzip -Z1 "$PACK" | grep '^production/.*/manifest\.json$' | head -n 1 || true)"
if [ -z "$MANIFEST_PATH" ]; then
  echo "STOP: у пакеті немає production manifest.json"
  exit 2
fi

TARGET_REL="$(dirname "$MANIFEST_PATH")"
TARGET="$ASSET_DIR/$TARGET_REL"

echo "Ціль:"
echo "  $TARGET_REL"
echo

if [ -e "$TARGET" ]; then
  echo "STOP: така production-версія вже існує."
  echo "Я її не перезаписую:"
  echo "  $TARGET"
  exit 2
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

unzip -q "$PACK" -d "$TMP" || exit 1

mkdir -p "$(dirname "$TARGET")"
cp -a "$TMP/$TARGET_REL" "$TARGET" || exit 1

echo
echo "Імпортовано:"
find "$TARGET" -maxdepth 3 -type f | sed "s#^$ASSET_DIR/##" | sort

echo
echo "Git status:"
cd "$ASSET_DIR" || exit 1
git status --short -- "$TARGET_REL"

echo
echo "PASS: production-pack імпортовано локально."
echo
echo "Наступний крок:"
echo "  13 — Зберегти FARIC assets у GitHub"
