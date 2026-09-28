#!/data/data/com.termux/files/usr/bin/bash
set -u

REPO="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

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

  git fetch --prune origin "+refs/heads/$branch:refs/remotes/origin/$branch" &&
  git merge --ff-only "refs/remotes/origin/$branch"
  pause_menu
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

run_tool() {
  clear
  cd "$REPO" || exit 1
  bash "$1"
  pause_menu
}

while true; do
  clear
  echo "========================================"
  echo "      FARIC Music Visualizer"
  echo "========================================"
  echo
  echo "1 — Відкрити shell у коді"
  echo "2 — Git status"
  echo "3 — Оновити проєкт з GitHub"
  echo "4 — Показати ACTIVE_PLAN"
  echo "5 — Створити окремий development signer"
  echo "6 — Передати signer secrets у GitHub"
  echo "7 — Backup development signer"
  echo "8 — Завантажити APK поточного commit"
  echo "0 — Вийти"
  echo
  printf "Вибір: "
  read -r choice

  case "$choice" in
    1) open_code ;;
    2) show_status ;;
    3) update_project ;;
    4) clear; cat "$REPO/ACTIVE_PLAN.md"; pause_menu ;;
    5) run_tool "$REPO/tools/termux/generate-dev-signing-key.sh" ;;
    6) run_tool "$REPO/tools/termux/configure-github-signing-secrets.sh" ;;
    7) run_tool "$REPO/tools/termux/backup-dev-signing-key.sh" ;;
    8) run_tool "$REPO/tools/termux/download-current-apk.sh" ;;
    0) clear; exit 0 ;;
    *) echo "Невідомий пункт."; sleep 1 ;;
  esac
done
