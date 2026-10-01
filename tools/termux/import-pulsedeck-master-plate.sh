#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
DOWNLOADS="$HOME/storage/downloads"
SHARED="$HOME/storage/shared"
EXPECTED_SHA256="edf839ad45c09f1bb0b19f14e0a218dac428a21f0793d083c3b7454034a5cb0d"
TARGET_DIR="$REPO/skin/pulsedeck_master"
TEMP_PICKED_ZIP=""

clear
echo "PulseDeck Master Plate import"
echo "========================"
echo

if [ ! -d "$REPO/.git" ]; then
  echo "FAIL: не знайдено Git repository: $REPO"
  exit 1
fi

if [ ! -d "$SHARED" ]; then
  echo "FAIL: Termux не бачить спільне сховище Android."
  echo "Запусти один раз: termux-setup-storage"
  exit 1
fi

# ChatGPT/Android Download Manager can place a downloaded file either directly
# in Download or in a nested app/download folder. Search the shared storage
# first instead of assuming one exact physical path.
SEARCH_ROOTS=(
  "$DOWNLOADS"
  "$SHARED/Download"
  "$SHARED/Downloads"
  "/storage/emulated/0/Download"
  "/sdcard/Download"
)

ZIP=""

for root in "${SEARCH_ROOTS[@]}"; do
  [ -d "$root" ] || continue

  candidate="$(
    find "$root" -maxdepth 4 -type f \
      \( -iname 'PulseDeck_MasterPlate_v1.zip' -o -iname 'PulseDeck_MasterPlate_v1*.zip' \) \
      -print 2>/dev/null | sort | tail -n 1
  )"

  if [ -n "$candidate" ]; then
    ZIP="$candidate"
    break
  fi
done

# Last filesystem fallback: search all shared user storage, but keep the search
# shallow enough to avoid crawling unrelated app data.
if [ -z "$ZIP" ]; then
  ZIP="$(
    find "$SHARED" -maxdepth 5 -type f \
      \( -iname 'PulseDeck_MasterPlate_v1.zip' -o -iname 'PulseDeck_MasterPlate_v1*.zip' \) \
      -print 2>/dev/null | sort | tail -n 1
  )"
fi

# Optional Android picker fallback. If Termux:API is installed, this lets the
# user select the downloaded ZIP even when Android exposes it only through a
# document/content provider rather than a normal filesystem path.
if [ -z "$ZIP" ] && command -v termux-storage-get >/dev/null 2>&1; then
  echo "ZIP автоматично не знайдено."
  echo "Відкриваю системний вибір файлу..."
  echo "Вибери PulseDeck_MasterPlate_v1.zip"
  echo

  TEMP_PICKED_ZIP="$REPO/.tmp-PulseDeck_MasterPlate_v1.zip"
  rm -f "$TEMP_PICKED_ZIP"

  if termux-storage-get "$TEMP_PICKED_ZIP"; then
    if [ -f "$TEMP_PICKED_ZIP" ]; then
      ZIP="$TEMP_PICKED_ZIP"
    fi
  fi
fi

if [ -z "$ZIP" ]; then
  echo "FAIL: PulseDeck_MasterPlate_v1.zip не знайдено."
  echo
  echo "Файл у Download Manager є, але Android не дав Termux прямий файловий шлях."
  echo
  echo "Зроби так:"
  echo "1. Відкрий 'Файли' / My Files."
  echo "2. Знайди PulseDeck_MasterPlate_v1.zip."
  echo "3. Перемісти його в: Внутрішня пам'ять/Download"
  echo "4. Повернись сюди й знову запусти пункт 19."
  echo
  echo "Підказка: поточні ZIP, які Termux бачить у shared storage:"
  find "$SHARED" -maxdepth 4 -type f -iname '*.zip' -print 2>/dev/null | tail -n 20 || true
  exit 1
fi

cleanup() {
  if [ -n "$TEMP_PICKED_ZIP" ]; then
    rm -f "$TEMP_PICKED_ZIP"
  fi
}
trap cleanup EXIT

echo "ZIP:"
echo "$ZIP"
echo

ACTUAL_SHA256="$(sha256sum "$ZIP" | awk '{print $1}')"
if [ "$ACTUAL_SHA256" != "$EXPECTED_SHA256" ]; then
  echo "FAIL: checksum не збігається."
  echo "Очікується: $EXPECTED_SHA256"
  echo "Отримано:   $ACTUAL_SHA256"
  echo
  echo "Файл не розпаковую."
  exit 1
fi

echo "PASS: checksum OK."
echo

BAD_ENTRY="$(
  unzip -Z1 "$ZIP" | awk '
    $0 !~ /^skin\/pulsedeck_master\// { print; exit }
    $0 ~ /(^|\/)\.\.($|\/)/ { print; exit }
  '
)"

if [ -n "$BAD_ENTRY" ]; then
  echo "FAIL: небезпечний або неочікуваний шлях у ZIP:"
  echo "$BAD_ENTRY"
  exit 1
fi

cd "$REPO"

BRANCH="$(git branch --show-current)"
if [ -z "$BRANCH" ]; then
  echo "FAIL: не вдалося визначити git branch."
  exit 1
fi

echo "Імпортую master plate..."
rm -rf "$TARGET_DIR"
unzip -oq "$ZIP" -d "$REPO"

if [ ! -f "$TARGET_DIR/manifest.json" ]; then
  echo "FAIL: після розпаковки немає manifest.json."
  exit 1
fi

REQUIRED=(
  "master_playing.webp"
  "master_paused.webp"
  "manifest.json"
  "README.md"
)

for rel in "${REQUIRED[@]}"; do
  if [ ! -f "$TARGET_DIR/$rel" ]; then
    echo "FAIL: відсутній $rel"
    exit 1
  fi
done

ASSET_COUNT="$(find "$TARGET_DIR" -type f \( -name '*.webp' -o -name '*.png' \) | wc -l | tr -d ' ')"
echo "PASS: master assets: $ASSET_COUNT"
echo

git add -- "$TARGET_DIR"

if git diff --cached --quiet -- "$TARGET_DIR"; then
  echo "PASS: цей skin pack уже є в repository, змін немає."
  exit 0
fi

echo "Файли, які підуть у commit:"
git --no-pager diff --cached --stat -- "$TARGET_DIR"
echo

git commit -m "assets: add PulseDeck reference-first master plate"

echo
echo "Синхронізуюсь із origin/$BRANCH перед push..."
if ! git fetch --prune origin "+refs/heads/$BRANCH:refs/remotes/origin/$BRANCH"; then
  echo "FAIL: не вдалося отримати актуальний origin/$BRANCH."
  exit 1
fi

if ! git rebase "refs/remotes/origin/$BRANCH"; then
  echo
  echo "FAIL: rebase зупинився через конфлікт."
  echo "Нічого не форсую."
  echo "Скинь цей екран у чат."
  exit 1
fi

echo
echo "Push → origin/$BRANCH"
git push origin "$BRANCH"

echo
echo "========================================"
echo "PASS: PulseDeck Master Plate у GitHub."
echo "Path: skin/pulsedeck_master/"
echo "Assets: $ASSET_COUNT"
echo "========================================"
