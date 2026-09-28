#!/data/data/com.termux/files/usr/bin/bash
set -eu

REPO="faric-ua/faric-music-visualizer"
WORKFLOW="android.yml"
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT"

command -v gh >/dev/null 2>&1 || { echo "GitHub CLI (gh) не знайдено."; exit 1; }
gh auth status -h github.com >/dev/null 2>&1 || { echo "GitHub CLI не авторизований."; exit 1; }

if ! git diff --quiet || ! git diff --cached --quiet || [ -n "$(git ls-files --others --exclude-standard)" ]; then
  echo "Робоче дерево не чисте. APK не завантажую."
  git status --short
  exit 1
fi

branch="$(git branch --show-current)"
test -n "$branch" || { echo "Не вдалося визначити поточну гілку."; exit 1; }

echo "Перевіряю origin/$branch…"
git fetch --prune origin "+refs/heads/$branch:refs/remotes/origin/$branch"

LOCAL_SHA="$(git rev-parse HEAD)"
REMOTE_SHA="$(git rev-parse "refs/remotes/origin/$branch")"

if [ "$LOCAL_SHA" != "$REMOTE_SHA" ]; then
  if git merge-base --is-ancestor "$LOCAL_SHA" "$REMOTE_SHA"; then
    echo "Є нові коміти. Оновлюю репозиторій перед завантаженням APK…"
    git merge --ff-only "$REMOTE_SHA"
  else
    echo "Локальна гілка розійшлася з origin/$branch."
    echo "Local:  $LOCAL_SHA"
    echo "Remote: $REMOTE_SHA"
    exit 1
  fi
fi

HEAD_SHA="$(git rev-parse HEAD)"
VERSION="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n 1)"
test -n "$VERSION"

echo "Поточна версія: v$VERSION"
echo "HEAD: $(git rev-parse --short=12 HEAD)"
echo

RUN_ID=""
BUILD_SHA=""
ARTIFACT=""

while IFS=$'\t' read -r candidate_run candidate_sha; do
  [ -n "$candidate_run" ] || continue
  [ -n "$candidate_sha" ] || continue

  if ! git cat-file -e "$candidate_sha^{commit}" 2>/dev/null; then
    git fetch -q origin "$candidate_sha" || true
  fi

  git merge-base --is-ancestor "$candidate_sha" "$HEAD_SHA" 2>/dev/null || continue

  git diff --quiet "$candidate_sha" "$HEAD_SHA" -- \
    app \
    build.gradle.kts \
    settings.gradle.kts \
    gradle.properties \
    .github/workflows/android.yml || continue

  candidate_artifact="$(gh api "repos/$REPO/actions/runs/$candidate_run/artifacts" --jq '.artifacts[] | select(.expired == false) | .name' | grep "^FARIC-Music-Visualizer-v${VERSION}-Debug$" | head -n 1 || true)"
  [ -n "$candidate_artifact" ] || continue

  RUN_ID="$candidate_run"
  BUILD_SHA="$candidate_sha"
  ARTIFACT="$candidate_artifact"
  break
done < <(
  gh api "repos/$REPO/actions/workflows/$WORKFLOW/runs?branch=$branch&status=success&per_page=50" --jq '.workflow_runs[] | [.id, .head_sha] | @tsv'
)

if [ -z "$RUN_ID" ]; then
  echo "Немає готового Android artifact для поточного APK source."
  echo "Версія: v$VERSION"
  echo "HEAD: $HEAD_SHA"
  echo
  latest="$(gh api "repos/$REPO/actions/workflows/$WORKFLOW/runs?branch=$branch&per_page=1" --jq '.workflow_runs[0] | [.id, .status, (.conclusion // "-"), .head_sha] | @tsv' 2>/dev/null || true)"
  if [ -n "$latest" ]; then
    echo "Останній Android run: $latest"
  fi
  echo "Якщо build ще виконується — перевір пункт 10 і повтори пункт 8 після ✓."
  exit 1
fi

echo "Знайдено Android run $RUN_ID"
echo "Artifact: $ARTIFACT"
echo

TMP="$PREFIX/tmp/faric-music-visualizer-apk-${RUN_ID}"
rm -rf "$TMP"
mkdir -p "$TMP"

gh run download "$RUN_ID" --repo "$REPO" --name "$ARTIFACT" --dir "$TMP"

APK="$TMP/FARIC-Music-Visualizer-v${VERSION}-debug.apk"
SHA_FILE="$APK.sha256"

test -f "$APK" || { echo "У artifact немає $(basename "$APK")"; find "$TMP" -maxdepth 2 -type f -print; exit 1; }
test -f "$SHA_FILE" || { echo "У artifact немає $(basename "$SHA_FILE")"; exit 1; }

(cd "$TMP" && sha256sum -c "$(basename "$SHA_FILE")")

DEST="/storage/emulated/0/Documents/FARIC-Music-Visualizer/packages/v${VERSION}"
mkdir -p "$DEST"
cp "$APK" "$SHA_FILE" "$DEST/"
(cd "$DEST" && sha256sum -c "$(basename "$SHA_FILE")")

echo
echo "PASS: APK завантажено і перевірено:"
echo "  $DEST/$(basename "$APK")"
echo "GitHub run: $RUN_ID"
echo "APK source SHA: $BUILD_SHA"
echo "Current HEAD: $HEAD_SHA"
echo

opened=0
echo "Відкриваю папку APK…"

if command -v pm >/dev/null 2>&1 && pm path com.sec.android.app.myfiles >/dev/null 2>&1 && command -v am >/dev/null 2>&1; then
  if am start -W -a android.intent.action.VIEW -d "file://$DEST" -t "resource/folder" -p com.sec.android.app.myfiles >/dev/null 2>&1; then
    opened=1
  fi
fi

if [ "$opened" -eq 0 ] && command -v am >/dev/null 2>&1; then
  RELATIVE_DIR="Documents/FARIC-Music-Visualizer/packages/v${VERSION}"
  ENCODED_RELATIVE_DIR="${RELATIVE_DIR//\//%2F}"
  DIR_URI="content://com.android.externalstorage.documents/document/primary%3A${ENCODED_RELATIVE_DIR}"
  if am start -a android.intent.action.VIEW -d "$DIR_URI" -t "vnd.android.document/directory" -f 0x10000000 >/dev/null 2>&1; then
    opened=1
  fi
fi

if [ "$opened" -eq 0 ] && command -v termux-open >/dev/null 2>&1; then
  if termux-open --view "$DEST" >/dev/null 2>&1; then
    opened=1
  fi
fi

if [ "$opened" -eq 0 ]; then
  echo "Не вдалося автоматично відкрити точну папку."
  echo "APK збережено тут:"
  echo "  $DEST"
fi
