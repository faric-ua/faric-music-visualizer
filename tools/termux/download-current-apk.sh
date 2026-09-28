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

RUN_ID=""
BUILD_SHA=""

while IFS=$'\t' read -r candidate_run candidate_sha; do
  [ -n "$candidate_run" ] || continue
  [ -n "$candidate_sha" ] || continue

  if ! git merge-base --is-ancestor "$candidate_sha" "$HEAD_SHA" 2>/dev/null; then
    continue
  fi

  if git diff --quiet "$candidate_sha" "$HEAD_SHA" --       app       build.gradle.kts       settings.gradle.kts       gradle.properties       .github/workflows/android.yml; then
    RUN_ID="$candidate_run"
    BUILD_SHA="$candidate_sha"
    break
  fi
done < <(
  gh api "repos/$REPO/actions/workflows/$WORKFLOW/runs?branch=main&status=success&per_page=20"     --jq '.workflow_runs[] | [.id, .head_sha] | @tsv'
)

if [ -z "$RUN_ID" ]; then
  echo "Немає успішного Android run для поточного APK source."
  echo "Поточний HEAD: $HEAD_SHA"
  echo "Потрібен новий Android build."
  exit 1
fi

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
echo "PASS: APK відповідає поточному Android source:"
echo "  $DEST/$(basename "$APK")"
echo "GitHub run: $RUN_ID"
echo "APK source SHA: $BUILD_SHA"
echo "Current repo HEAD: $HEAD_SHA"
echo

echo "Відкриваю папку завантаження…"

# Open the exact directory in Android's file UI. ACTION_VIEW is important:
# ACTION_OPEN_DOCUMENT_TREE shows a folder-selection dialog with
# "Використовувати цю папку", which is not what we want here.
RELATIVE_DIR="Documents/FARIC-Music-Visualizer/packages/v${VERSION}"
ENCODED_RELATIVE_DIR="${RELATIVE_DIR//\//%2F}"
DIR_URI="content://com.android.externalstorage.documents/document/primary%3A${ENCODED_RELATIVE_DIR}"

opened=0

if command -v am >/dev/null 2>&1; then
  if am start \
    -a android.intent.action.VIEW \
    -d "$DIR_URI" \
    -t "vnd.android.document/directory" \
    -f 0x10000000 \
    >/dev/null 2>&1; then
    opened=1
  fi
fi

# Secondary fallback for file managers that understand filesystem paths.
if [ "$opened" -eq 0 ] && command -v termux-open >/dev/null 2>&1; then
  if termux-open "$DEST" >/dev/null 2>&1; then
    opened=1
  fi
fi

if [ "$opened" -eq 0 ]; then
  echo "Не вдалося автоматично відкрити папку."
  echo "APK збережено тут:"
  echo "  $DEST"
fi
