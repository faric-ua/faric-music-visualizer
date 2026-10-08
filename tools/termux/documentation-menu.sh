#!/data/data/com.termux/files/usr/bin/bash
# Nested FARIC documentation browser. No app modifications or build dispatch.
set -u
REPO="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
DOC_DIR="${FARIC_DOC_DIR:-/storage/emulated/0/Documents/FARIC-Music-Visualizer/documentation}"
GITHUB="https://github.com/faric-ua/faric-music-visualizer"

pause_docs() {
  echo
  printf "Натисни Enter, щоб продовжити..."
  read -r _ || true
}

open_url() {
  if command -v termux-open-url >/dev/null 2>&1; then
    termux-open-url "$1"
  elif command -v termux-open >/dev/null 2>&1; then
    termux-open "$1"
  else
    echo "Відкрий у браузері: $1"
  fi
}

open_guide() {
  local ext="$1" mime="$2" found=""
  if [ -d "$DOC_DIR" ]; then
    found="$(find "$DOC_DIR" -maxdepth 2 -type f -name "FARIC_User_Guide_UK_v*.$ext" 2>/dev/null | sort -V | tail -n 1)"
  fi
  if [ -z "$found" ] || [ ! -f "$found" ]; then
    echo "Файл .$ext поки відсутній у Documentation."
    echo "Завантаж FARIC_User_Guide_UK_v0.19.43_COMPLETE.zip із чату в Download."
    echo "Тоді натисни 6 — Імпорт ZIP. Актуальна онлайн-версія: пункт 4."
    return
  fi
  echo "Відкриваю: $found"
  if command -v termux-open >/dev/null 2>&1; then
    termux-open -c "$mime" "$found" || echo "Відкрий цей файл у файловому менеджері: $found"
  else
    echo "termux-open недоступний. Відкрий вручну: $found"
  fi
}

import_docs() {
  if ! command -v python3 >/dev/null 2>&1; then
    echo "Потрібен Python 3 (встановлення вручну: pkg install python)."
    return
  fi
  if ! mkdir -p "$DOC_DIR" 2>/dev/null; then
    echo "Немає доступу до Documents. Перевір дозволи Termux: termux-setup-storage"
    return
  fi
  FARIC_DOC_DEST="$DOC_DIR" python3 "$REPO/tools/termux/import-documentation.py"
}

open_folder() {
  mkdir -p "$DOC_DIR" 2>/dev/null || {
    echo "Немає доступу до $DOC_DIR. Перевір termux-setup-storage"
    return
  }
  local opened=0
  if command -v am >/dev/null 2>&1; then
    local uri="content://com.android.externalstorage.documents/document/primary%3ADocuments%2FFARIC-Music-Visualizer%2Fdocumentation"
    am start -a android.intent.action.VIEW -d "$uri" \
      -t "vnd.android.document/directory" -f 0x10000000 >/dev/null 2>&1 && opened=1
  fi
  if [ "$opened" -eq 0 ] && command -v termux-open >/dev/null 2>&1; then
    termux-open --view "$DOC_DIR" >/dev/null 2>&1 && opened=1
  fi
  if [ "$opened" -eq 0 ]; then
    echo "Відкрий у файловому менеджері: $DOC_DIR"
  fi
}

project_docs() {
  local choice=""
  while true; do
    clear 2>/dev/null || true
    echo "====== DOCUMENTATION / ПРОЄКТ ======"
    echo "1 — Посібник (GitHub)"
    echo "2 — Уся документація (docs/)"
    echo "3 — Герої та аудіоформи"
    echo "4 — Аналіз відеореференсів"
    echo "5 — План робіт"
    echo "6 — Відомі проблеми"
    echo "7 — Історія збірок"
    echo "0 — Назад"
    echo
    printf "Вибір: "
    read -r choice || return
    case "$choice" in
      1) open_url "$GITHUB/blob/main/docs/user/FARIC_USER_GUIDE_UK.md" ;;
      2) open_url "$GITHUB/tree/main/docs" ;;
      3) open_url "$GITHUB/blob/main/docs/architecture/VISUAL_ELEMENTS_TAXONOMY.md" ;;
      4) open_url "$GITHUB/blob/main/docs/visualizer/AUDIOFORMS_VIDEO_REFERENCE_WORKFLOW.md" ;;
      5) open_url "$GITHUB/blob/main/ACTIVE_PLAN.md" ;;
      6) open_url "$GITHUB/blob/main/OPEN_FINDINGS.md" ;;
      7) open_url "$GITHUB/blob/main/BUILD_CHECKPOINTS.md" ;;
      0) return ;;
      *) echo "Невідомий пункт." ;;
    esac
    pause_docs
  done
}

while true; do
  clear 2>/dev/null || true
  echo "=================================="
  echo " FARIC · DOCUMENTATION"
  echo "=================================="
  echo "ПОСІБНИК КОРИСТУВАЧА"
  echo "1 — HTML (офлайн)"
  echo "2 — PDF"
  echo "3 — DOCX / Word"
  echo "4 — Актуальний посібник GitHub"
  echo
  echo "ДОКУМЕНТАЦІЯ"
  echo "5 — Інші документи проєкту →"
  echo "6 — Імпортувати ZIP / файли з Downloads"
  echo "7 — Відкрити папку Documentation"
  echo "0 — Назад у головне меню"
  echo
  printf "Вибір: "
  read -r choice || exit 0
  case "$choice" in
    1) open_guide html text/html ;;
    2) open_guide pdf application/pdf ;;
    3) open_guide docx application/vnd.openxmlformats-officedocument.wordprocessingml.document ;;
    4) open_url "$GITHUB/blob/main/docs/user/FARIC_USER_GUIDE_UK.md" ;;
    5) project_docs; continue ;;
    6) import_docs ;;
    7) open_folder ;;
    0) exit 0 ;;
    *) echo "Невідомий пункт." ;;
  esac
  pause_docs
done
