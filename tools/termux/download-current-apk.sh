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

DEST="/storage/emulated/0/Download/FARIC-Music-Visualizer-v${VERSION}-build"
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

# Android's document picker understands an initial *document* URI. Using
# --es sends a plain String and can be silently ignored; --eu sends a Uri.
RELATIVE_DIR="Download/FARIC-Music-Visualizer-v${VERSION}-build"
ENCODED_RELATIVE_DIR="${RELATIVE_DIR//\//%2F}"
INITIAL_URI="content://com.android.externalstorage.documents/document/primary%3A${ENCODED_RELATIVE_DIR}"

if command -v am >/dev/null 2>&1; then
  if am start \
    -a android.intent.action.OPEN_DOCUMENT_TREE \
    --eu android.provider.extra.INITIAL_URI "$INITIAL_URI" \
    >/dev/null 2>&1; then
    exit 0
  fi
fi

# Secondary fallback. Some Android file managers support opening filesystem
# directories through Termux:API, others do not.
if command -v termux-open >/dev/null 2>&1; then
  termux-open "$DEST" >/dev/null 2>&1 || true
fi

echo "Якщо файловий менеджер не відкрився автоматично:"
echo "  $DEST"
