#!/data/data/com.termux/files/usr/bin/bash
set -eu

REPO="faric-ua/faric-music-visualizer"
WORKFLOW="android.yml"
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT"

gh auth status -h github.com >/dev/null 2>&1 || { echo "GitHub CLI не авторизований."; exit 1; }

if ! git diff --quiet || ! git diff --cached --quiet || [ -n "$(git ls-files --others --exclude-standard)" ]; then
  echo "Робоче дерево не чисте."
  git status --short
  exit 1
fi

HEAD_SHA="$(git rev-parse HEAD)"
VERSION="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n 1)"
test -n "$VERSION"

RUN_ID="$(gh api "repos/$REPO/actions/workflows/$WORKFLOW/runs?head_sha=$HEAD_SHA&status=success&per_page=20" --jq '.workflow_runs[0].id // empty')"
test -n "$RUN_ID" || { echo "Немає успішного Android run для $HEAD_SHA"; exit 1; }

ARTIFACT="$(gh api "repos/$REPO/actions/runs/$RUN_ID/artifacts" --jq '.artifacts[] | select(.expired == false) | .name' | grep "^FARIC-Music-Visualizer-v${VERSION}-Debug$" | head -n 1)"
test -n "$ARTIFACT" || { echo "Не знайдено exact artifact для run $RUN_ID"; exit 1; }

TMP="$PREFIX/tmp/faric-music-visualizer-apk-${RUN_ID}"
rm -rf "$TMP"
mkdir -p "$TMP"

gh run download "$RUN_ID" --repo "$REPO" --name "$ARTIFACT" --dir "$TMP"

APK="$TMP/FARIC-Music-Visualizer-v${VERSION}-debug.apk"
SHA_FILE="$APK.sha256"
test -f "$APK"
test -f "$SHA_FILE"

(cd "$TMP" && sha256sum -c "$(basename "$SHA_FILE")")

DEST="/storage/emulated/0/Documents/FARIC-Music-Visualizer/packages/v${VERSION}"
mkdir -p "$DEST"
cp "$APK" "$SHA_FILE" "$DEST/"
(cd "$DEST" && sha256sum -c "$(basename "$SHA_FILE")")

echo
echo "PASS: exact APK поточного commit:"
echo "  $DEST/$(basename "$APK")"
echo "GitHub run: $RUN_ID"
echo "Source SHA: $HEAD_SHA"
