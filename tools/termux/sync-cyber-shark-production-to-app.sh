#!/data/data/com.termux/files/usr/bin/bash
set -u

MAIN_REPO="$HOME/faric-music-visualizer"
ASSET_REPO="$HOME/faric-music-visualizer-assets"
SRC="$ASSET_REPO/production/faric/cyber-shark/v1/app/layers"
DST="$MAIN_REPO/app/src/main/res/drawable-nodpi"
GRADLE="$MAIN_REPO/app/build.gradle.kts"

echo "========================================"
echo " FARIC — Cyber Shark production sync"
echo "========================================"
echo

if [ ! -d "$MAIN_REPO/.git" ]; then
  echo "ERROR: main repo not found:"
  echo "  $MAIN_REPO"
  exit 2
fi

if [ ! -d "$ASSET_REPO/.git" ]; then
  echo "ERROR: FARIC assets repo not found."
  echo "Use menu item 11 first."
  exit 2
fi

cd "$ASSET_REPO" || exit 1
git lfs pull || exit 1

for name in frame creature wordmark; do
  f="$SRC/$name.webp"

  if [ ! -f "$f" ]; then
    echo "ERROR: missing production asset:"
    echo "  $f"
    echo
    echo "Use menu item 16 to import FARIC_PRODUCTION_PACK_CYBER_SHARK_V1.zip first."
    exit 2
  fi

  magic1="$(head -c 4 "$f" 2>/dev/null || true)"
  magic2="$(dd if="$f" bs=1 skip=8 count=4 2>/dev/null || true)"

  if [ "$magic1" != "RIFF" ] || [ "$magic2" != "WEBP" ]; then
    echo "ERROR: invalid WebP bytes:"
    echo "  $f"
    echo "Header: $magic1 / $magic2"
    exit 2
  fi
done

cd "$MAIN_REPO" || exit 1

if ! git diff --quiet || ! git diff --cached --quiet || [ -n "$(git ls-files --others --exclude-standard)" ]; then
  echo "STOP: main repo has local changes."
  echo
  git status --short
  echo
  echo "No files were overwritten."
  exit 2
fi

mkdir -p "$DST"

cp "$SRC/frame.webp" "$DST/cyber_shark_frame.webp"
cp "$SRC/creature.webp" "$DST/cyber_shark_creature.webp"
cp "$SRC/wordmark.webp" "$DST/cyber_shark_wordmark.webp"

if grep -q 'versionCode = 26' "$GRADLE"; then
  sed -i 's/versionCode = 26/versionCode = 27/' "$GRADLE"
fi

if grep -q 'versionName = "0.7.1"' "$GRADLE"; then
  sed -i 's/versionName = "0.7.1"/versionName = "0.7.2"/' "$GRADLE"
fi

echo "Production assets copied:"
ls -lh \
  "$DST/cyber_shark_frame.webp" \
  "$DST/cyber_shark_creature.webp" \
  "$DST/cyber_shark_wordmark.webp"

echo
echo "Git status:"
git status --short

echo
printf "Commit and push corrected production assets now? [y/N]: "
read -r answer

case "$answer" in
  y|Y|yes|YES|так|Так|ТАК) ;;
  *)
    echo
    echo "Files are copied locally but NOT committed."
    echo "Run this menu item again only after resolving local changes."
    exit 0
    ;;
esac

git add \
  "$DST/cyber_shark_frame.webp" \
  "$DST/cyber_shark_creature.webp" \
  "$DST/cyber_shark_wordmark.webp" \
  "$GRADLE" || exit 1

git commit -m "fix: sync real Cyber Shark production assets for v0.7.2" || exit 1
git push || exit 1

echo
echo "========================================"
echo "PASS: real Cyber Shark production assets"
echo "were pushed to the main app repository."
echo
echo "Expected release: v0.7.2 / versionCode 27"
echo
echo "Next:"
echo "  10 — Status Android build"
echo "  8  — Download APK after PASS"
echo "========================================"
