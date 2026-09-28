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

  if ! git diff --quiet ||
     ! git diff --cached --quiet ||
     [ -n "$(git ls-files --others --exclude-standard)" ]; then
    echo "Є локальні або нові файли. Оновлення зупинено."
    git status --short
    pause_menu
    return
  fi

  branch="$(git branch --show-current)"
  if [ -z "$branch" ]; then
    echo "Не вдалося визначити гілку."
    pause_menu
    return
  fi

  git fetch --prune origin \
    "+refs/heads/$branch:refs/remotes/origin/$branch" &&
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
  echo "0 — Вийти"
  echo
  printf "Вибір: "
  read -r choice

  case "$choice" in
    1) open_code ;;
    2) show_status ;;
    3) update_project ;;
    4)
      clear
      cat "$REPO/ACTIVE_PLAN.md"
      pause_menu
      ;;
    0) clear; exit 0 ;;
    *) echo "Невідомий пункт."; sleep 1 ;;
  esac
done
