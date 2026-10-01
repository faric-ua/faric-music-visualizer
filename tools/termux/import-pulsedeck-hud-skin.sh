#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
DOWNLOADS="$HOME/storage/downloads"
EXPECTED_SHA256="82988f103fecf34a8d1cc6c72f7b2ed65c3e4c1707ecd12c7101e057912b88f4"
TARGET_DIR="$REPO/skin/pulsedeck_hud"

clear
echo "PulseDeck HUD skin import"
echo "========================"
echo

if [ ! -d "$REPO/.git" ]; then
  echo "FAIL: не знайдено Git repository: $REPO"
  exit 1
fi

if [ ! -d "$DOWNLOADS" ]; then
  echo "FAIL: Termux не бачить Downloads."
  echo "Запусти один раз: termux-setup-storage"
  exit 1
fi

ZIP="$(
  find "$DOWNLOADS" -maxdepth 1 -type f \
    \( -name 'PulseDeckHUD_SkinPack_v1.zip' -o -name 'PulseDeckHUD_SkinPack_v1*.zip' \) \
    -print 2>/dev/null | sort | tail -n 1
)"

if [ -z "$ZIP" ]; then
  echo "FAIL: у Downloads немає PulseDeckHUD_SkinPack_v1.zip"
  echo
  echo "Спочатку скачай ZIP з чату в Downloads."
  exit 1
fi

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
    $0 !~ /^skin\/pulsedeck_hud\// { print; exit }
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

echo "Імпортую modular skin..."
rm -rf "$TARGET_DIR"
unzip -oq "$ZIP" -d "$REPO"

if [ ! -f "$TARGET_DIR/manifest.json" ]; then
  echo "FAIL: після розпаковки немає manifest.json."
  exit 1
fi

REQUIRED=(
  "transport/rail.png"
  "transport/shuffle.png"
  "transport/previous.png"
  "transport/play.png"
  "transport/pause.png"
  "transport/next.png"
  "transport/repeat.png"
  "quick_actions/rail.png"
  "quick_actions/theme.png"
  "quick_actions/board.png"
  "quick_actions/visualizer.png"
  "quick_actions/export.png"
  "utility/back.png"
  "utility/menu.png"
  "utility/favorite.png"
  "utility/track_more.png"
  "progress/waveform.png"
  "progress/progress_line.png"
  "progress/progress_thumb.png"
  "hero/F_core.png"
  "hero/reactor_frame.png"
  "background/energy_waves.png"
  "background/reactor_energy_ring.png"
)

for rel in "${REQUIRED[@]}"; do
  if [ ! -f "$TARGET_DIR/$rel" ]; then
    echo "FAIL: відсутній $rel"
    exit 1
  fi
done

PNG_COUNT="$(find "$TARGET_DIR" -type f -name '*.png' | wc -l | tr -d ' ')"
echo "PASS: PNG assets: $PNG_COUNT"
echo

git add -- "$TARGET_DIR"

if git diff --cached --quiet -- "$TARGET_DIR"; then
  echo "PASS: цей skin pack уже є в repository, змін немає."
  exit 0
fi

echo "Файли, які підуть у commit:"
git diff --cached --stat -- "$TARGET_DIR"
echo

git commit -m "assets: add modular PulseDeck HUD skin pack"

echo
echo "Push → origin/$BRANCH"
git push origin "$BRANCH"

echo
echo "========================================"
echo "PASS: PulseDeck HUD skin у GitHub."
echo "Path: skin/pulsedeck_hud/"
echo "PNG:  $PNG_COUNT"
echo "========================================"
