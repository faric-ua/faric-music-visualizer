#!/data/data/com.termux/files/usr/bin/bash
# V178 (faric 23.10 21:06: «щоб я міг оновити з GitHub і зібрати APK у себе на телефоні»).
# Kotlin and resources are built here; the C++ part (projectM) is taken ready-made from the last
# GitHub APK (release phone-build) — the phone has no Android NDK. If the C++ code changed since that
# APK, this build cannot be done here — then GitHub (items 9 → 8).
set -u

REPO="faric-ua/faric-music-visualizer"
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT" || exit 1

GRADLE_VERSION="9.4.1"
GRADLE_HOME_DIR="$HOME/gradle/gradle-$GRADLE_VERSION"
GRADLE="$GRADLE_HOME_DIR/bin/gradle"
SIGN_ENV="$HOME/.faric-secrets/music-visualizer-dev/local-build-env.sh"
DOCS="/storage/emulated/0/Documents/FARIC-Music-Visualizer"
NATIVE_CACHE="$HOME/.cache/faric-visualizer-native"

fail() { echo "✖ $*"; }
ok() { echo "✔ $*"; }

# 1. The code — fresh from GitHub, like item 3.
if ! git diff --quiet || ! git diff --cached --quiet; then
  fail "У коді є незбережені зміни (пункт 2 покаже які). Збірку не починаю."
  exit 1
fi
branch="$(git branch --show-current)"
echo "Оновлюю код з GitHub…"
git fetch -q --prune origin "+refs/heads/$branch:refs/remotes/origin/$branch" || { fail "Немає зв'язку з GitHub."; exit 1; }
git merge -q --ff-only "origin/$branch" || { fail "Не вдалось оновити код (гілка розійшлась). Пункт 3."; exit 1; }
HEAD_SHA="$(git rev-parse HEAD)"
VERSION="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n 1)"
echo "Версія в коді: v$VERSION ($(git rev-parse --short=7 HEAD))"

# 2. What the phone needs (the same as for Audio Converter / Track Catcher).
command -v java >/dev/null 2>&1 || { fail "Немає Java (pkg install openjdk-17)."; exit 1; }
for tool in curl unzip; do command -v "$tool" >/dev/null 2>&1 || { fail "Немає $tool (pkg install $tool)."; exit 1; }; done
if [ -z "${ANDROID_HOME:-}" ] && [ -d "$HOME/android-sdk/platforms" ]; then export ANDROID_HOME="$HOME/android-sdk"; fi
[ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME/platforms/android-36" ] || { fail "Немає Android SDK 36 (~/android-sdk/platforms/android-36) — як для Audio Converter."; exit 1; }
export ANDROID_SDK_ROOT="$ANDROID_HOME"
grep -q "aapt2FromMavenOverride" "$HOME/.gradle/gradle.properties" 2>/dev/null || { fail "Немає aapt2 для телефона (~/.gradle/gradle.properties) — як для Audio Converter."; exit 1; }
[ -f "$SIGN_ENV" ] || { fail "Немає ключа підпису PulseDeck на цьому телефоні ($SIGN_ENV)."; echo "Без нього APK не стане поверх. Збирай на GitHub: пункти 9 → 8."; exit 1; }
# shellcheck disable=SC1090
. "$SIGN_ENV"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties

if [ ! -x "$GRADLE" ]; then
  echo "Перший раз: завантажую Gradle $GRADLE_VERSION (~140 МБ, один раз)…"
  mkdir -p "$HOME/gradle"
  zip="$HOME/gradle/gradle-$GRADLE_VERSION-bin.zip"
  curl -fL --progress-bar -o "$zip" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" || { fail "Не вдалось завантажити Gradle."; rm -f "$zip"; exit 1; }
  unzip -q -o "$zip" -d "$HOME/gradle" && rm -f "$zip"
  [ -x "$GRADLE" ] || { fail "Gradle не розпакувався."; exit 1; }
  ok "Gradle $GRADLE_VERSION готовий."
fi

# 3. The C++ part — from the last GitHub APK, only if the C++ code is the same.
release_sha="$(gh release view phone-build --repo "$REPO" --json body --jq '.body' 2>/dev/null | sed -n 's/.*commit \([0-9a-f]\{40\}\).*/\1/p' | head -n 1)"
[ -n "$release_sha" ] || { fail "На GitHub ще нема готового APK (реліз phone-build)."; echo "Один раз збери на GitHub: пункти 9 → 10."; exit 1; }
git cat-file -e "$release_sha^{commit}" 2>/dev/null || git fetch -q origin "$release_sha" 2>/dev/null || true
if ! git diff --quiet "$release_sha" "$HEAD_SHA" -- app/src/main/cpp 2>/dev/null; then
  fail "C++-частина (projectM) змінилась після останньої збірки на GitHub."
  echo "Цю версію телефон зібрати не може — пункти 9 → 10 → 8."
  exit 1
fi
native_dir="$NATIVE_CACHE/$release_sha"
if [ ! -d "$native_dir/arm64-v8a" ]; then
  echo "Беру готову C++-частину з APK на GitHub…"
  tmp="$HOME/.cache/faric-visualizer-native-download"
  rm -rf "$tmp" && mkdir -p "$tmp"
  gh release download phone-build --repo "$REPO" --pattern "*.apk" --dir "$tmp" || { fail "Не вдалось завантажити APK з GitHub."; exit 1; }
  apk="$(ls "$tmp"/*.apk | head -n 1)"
  rm -rf "$NATIVE_CACHE"/*/ 2>/dev/null
  mkdir -p "$native_dir"
  unzip -q -o "$apk" 'lib/arm64-v8a/*' -d "$tmp/x" || { fail "У APK нема C++-бібліотек."; exit 1; }
  mv "$tmp/x/lib/arm64-v8a" "$native_dir/arm64-v8a"
  rm -rf "$tmp"
fi
ok "C++-частина: $(ls "$native_dir/arm64-v8a" | tr '\n' ' ')"

# 4. Build.
mkdir -p "$DOCS/logs"
log="$DOCS/logs/phone-build-$(date +%Y%m%d-%H%M%S).log"
echo "Збираю PulseDeck v$VERSION на телефоні… (перший раз довго: Gradle качає бібліотеки)"
"$GRADLE" -p "$ROOT" :app:assembleDebug -PfaricPrebuiltNative="$native_dir" --stacktrace --console=plain > "$log" 2>&1 &
pid=$!
start=$(date +%s)
while kill -0 "$pid" 2>/dev/null; do
  sleep 3
  el=$(( $(date +%s) - start ))
  step="$(grep -E '^> Task |Download |^> Configure' "$log" 2>/dev/null | tail -n 1 | sed 's/^> //; s#https\?://[^ ]*/##' | cut -c1-52)"
  printf '\r⏳ %d:%02d  %-52s' $((el / 60)) $((el % 60)) "${step:-готуюсь…}"
done
wait "$pid"
rc=$?
# Free the phone's memory (lesson from Audio Converter, 23.10 02:36).
"$GRADLE" --stop >/dev/null 2>&1 || true
command -v pkill >/dev/null 2>&1 && pkill -f KotlinCompileDaemon >/dev/null 2>&1
el=$(( $(date +%s) - start ))
printf '\r%-70s\r' ""
echo "Час збірки: $((el / 60)) хв $((el % 60)) с"
if [ "$rc" != 0 ]; then
  fail "Збірка не вдалась. Коротко (скрін для Claude):"
  { grep -E '^e: |error:' "$log" | head -n 15; sed -n '/What went wrong/,/^\* Try/p' "$log" | grep -v '^\* Try' | head -n 20; } | cut -c1-200
  echo "Повний лог: Documents/FARIC-Music-Visualizer/logs/$(basename "$log")"
  exit 1
fi

src="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
[ -s "$src" ] || { fail "Збірка пройшла, але APK не знайдено."; exit 1; }
dest_dir="$DOCS/packages/v$VERSION"
dest="$dest_dir/FARIC-Music-Visualizer-v$VERSION-phone.apk"
mkdir -p "$dest_dir"
cp "$src" "$dest"
(cd "$dest_dir" && sha256sum "$(basename "$dest")" > "$(basename "$dest").sha256")
ok "Готово: Documents/FARIC-Music-Visualizer/packages/v$VERSION/$(basename "$dest")"
echo "Відкриваю встановлення — натисни «Оновити»."
if command -v termux-open >/dev/null 2>&1; then
  termux-open "$dest" >/dev/null 2>&1 || echo "Відкрий файл у «Моїх файлах»."
else
  echo "Відкрий файл у «Моїх файлах»."
fi
