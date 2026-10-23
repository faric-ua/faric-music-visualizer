#!/data/data/com.termux/files/usr/bin/bash
set -u

REPO="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
ASSET_REPO="$HOME/faric-music-visualizer-assets"

pause_menu() {
  echo
  printf "Натисни Enter, щоб повернутися..."
  read -r _
}

show_status() {
  clear
  cd "$REPO" || exit 1
  echo "FARIC Music Visualizer"
  echo
  git status -sb
  echo
  echo "Remote:"
  git remote -v
  pause_menu
}

update_project() {
  clear
  cd "$REPO" || exit 1

  if ! git diff --quiet || ! git diff --cached --quiet || [ -n "$(git ls-files --others --exclude-standard)" ]; then
    echo "Є локальні або нові файли. Оновлення зупинено."
    git status --short
    pause_menu
    return
  fi

  branch="$(git branch --show-current)"
  test -n "$branch" || { echo "Не вдалося визначити гілку."; pause_menu; return; }

  before="$(git rev-parse HEAD)"

  if ! git fetch --prune origin "+refs/heads/$branch:refs/remotes/origin/$branch"; then
    echo
    echo "Не вдалося отримати оновлення з GitHub."
    pause_menu
    return
  fi

  if ! git merge --ff-only "refs/remotes/origin/$branch"; then
    echo
    echo "Fast-forward оновлення не вдалося. Нічого не форсую."
    pause_menu
    return
  fi

  after="$(git rev-parse HEAD)"

  echo
  if [ "$before" = "$after" ]; then
    echo "PASS: репозиторій уже актуальний."
  else
    echo "PASS: репозиторій оновлено."
    echo "Було:  $(printf '%.10s' "$before")"
    echo "Стало: $(printf '%.10s' "$after")"
  fi

  echo
  echo "Оновлюю саме меню…"
  sleep 1

  exec bash "$REPO/scripts/termux-menu.sh"
}

open_code() {
  clear
  cd "$REPO" || exit 1
  echo "Shell у:"
  pwd
  echo "Для повернення в меню: exit"
  echo
  "${SHELL:-$PREFIX/bin/bash}" -i
}

open_assets() {
  clear

  if [ ! -d "$ASSET_REPO/.git" ]; then
    echo "FARIC assets ще не підключені."
    echo
    echo "Спочатку вибери:"
    echo "11 — Підключити / відновити FARIC assets"
    pause_menu
    return
  fi

  cd "$ASSET_REPO" || exit 1
  echo "Shell у FARIC assets:"
  pwd
  echo
  echo "masters/    — оригінали"
  echo "production/ — підготовлені набори для програми"
  echo
  echo "Для повернення в меню: exit"
  echo
  "${SHELL:-$PREFIX/bin/bash}" -i
}

run_tool() {
  clear
  cd "$REPO" || exit 1
  bash "$1"
  pause_menu
}

# Шапка меню: велика назва між смужками (faric 2026-10-16 — «щоб одразу видно, яке це меню»).
# Букви — напівблоки ▀▄█ (шрифт 3×5, згенеровано скриптом menu_banner.py), ширина 41 знаків.
# Вужчий екран — проста шапка одним рядком.
show_banner() {
  local c="\033[1;92m" d="\033[0;92m" z="\033[0m" cols
  cols="$(stty size 2>/dev/null | cut -d' ' -f2)"
  if [ "${cols:-80}" -ge 41 ] 2>/dev/null; then
    echo -e "${d}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${z}"
    echo -e "${c}          █▄ ▄█ █ █ █▀▀ ▀█▀ █▀▀${z}"
    echo -e "${c}          █ ▀ █ █ █ ▀▀█  █  █${z}"
    echo -e "${c}          ▀   ▀ ▀▀▀ ▀▀▀ ▀▀▀ ▀▀▀${z}"
    echo -e "${c} █ █ ▀█▀ █▀▀ █ █ █▀█ █   ▀█▀ ▀▀█ █▀▀ █▀▄${z}"
    echo -e "${c} █ █  █  ▀▀█ █ █ █▀█ █    █  ▄▀  █▀  █▀▄${z}"
    echo -e "${c}  ▀  ▀▀▀ ▀▀▀ ▀▀▀ ▀ ▀ ▀▀▀ ▀▀▀ ▀▀▀ ▀▀▀ ▀ ▀${z}"
    echo -e "${d}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${z}"
  else
    echo -e "${c}━━ MUSIC VISUALIZER ━━${z}"
  fi
}

while true; do
  clear
  show_banner
  echo "      FARIC Music Visualizer"
  echo
  echo "ПРОЄКТ"
  echo "1 — Відкрити shell у коді"
  echo "2 — Git status"
  echo "3 — Оновити репозиторій з GitHub"
  echo "4 — Показати стан проєкту (STATUS)"
  echo
  echo "BUILD / SIGNING"
  echo "5 — Створити окремий development signer"
  echo "6 — Передати signer secrets у GitHub"
  echo "7 — Backup development signer"
  echo "8 — Завантажити APK і відкрити папку"
  echo "9 — Запустити Android build"
  echo "10 — Статус Android build"
  echo "22 — Зібрати APK тут, на телефоні (без чекання GitHub)"
  echo
  echo "MASTER ASSETS"
  echo "11 — Підключити / відновити FARIC assets"
  echo "12 — Оновити FARIC assets з GitHub"
  echo "13 — Зберегти FARIC assets у GitHub"
  echo "14 — Статус FARIC assets"
  echo "15 — Відкрити shell у FARIC assets"
  echo "16 — Імпортувати production-pack з Downloads"
  echo "17 — Синхронізувати Cyber Shark production у app"
  echo "18 — Імпортувати PulseDeck HUD skin (Downloads / вибір файлу)"
  echo "19 — Імпортувати PulseDeck Clean Blocks v2 (Downloads / вибір файлу)"
  echo
  echo "DOCUMENTATION"
  echo "20 — Documentation →"
  echo
  echo "МУЗИКА"
  echo "21 — Прев'ю альбому (по 20 с з кожної пісні + історія альбому)"
  echo
  echo "0 — Вийти"
  echo
  printf "Вибір: "
  read -r choice

  case "$choice" in
    1) open_code ;;
    2) show_status ;;
    3) update_project ;;
    4) clear; cat "$REPO/STATUS.md"; pause_menu ;;
    5) run_tool "$REPO/tools/termux/generate-dev-signing-key.sh" ;;
    6) run_tool "$REPO/tools/termux/configure-github-signing-secrets.sh" ;;
    7) run_tool "$REPO/tools/termux/backup-dev-signing-key.sh" ;;
    8) run_tool "$REPO/tools/termux/download-current-apk.sh" ;;
    9) run_tool "$REPO/tools/termux/run-android-build.sh" ;;
    10) run_tool "$REPO/tools/termux/android-build-status.sh" ;;
    11) run_tool "$REPO/tools/termux/assets-bootstrap.sh" ;;
    12) run_tool "$REPO/tools/termux/assets-update.sh" ;;
    13) run_tool "$REPO/tools/termux/assets-push.sh" ;;
    14) run_tool "$REPO/tools/termux/assets-status.sh" ;;
    15) open_assets ;;
    16) run_tool "$REPO/tools/termux/assets-import-production-pack.sh" ;;
    17) run_tool "$REPO/tools/termux/sync-cyber-shark-production-to-app.sh" ;;
    18) run_tool "$REPO/tools/termux/import-pulsedeck-hud-skin.sh" ;;
    19) run_tool "$REPO/tools/termux/import-pulsedeck-master-plate.sh" ;;
    20) bash "$REPO/tools/termux/documentation-menu.sh" ;;
    21) run_tool "$REPO/tools/termux/album-preview.sh" ;;
    22) run_tool "$REPO/tools/termux/phone-build-apk.sh" ;;
    0) clear; exit 0 ;;
    *) echo "Невідомий пункт."; sleep 1 ;;
  esac
done
