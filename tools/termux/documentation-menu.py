#!/usr/bin/env python3
"""FARIC Documentation submenu for Termux; no writes inside the Git repository."""
from __future__ import annotations
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
DOCS = ROOT / "docs"
GUIDE = DOCS / "user" / "FARIC_USER_GUIDE_UK.md"
FOLDER = Path("/storage/emulated/0/Documents/FARIC-Music-Visualizer/documentation")
CACHE = Path.home() / ".cache/faric-music-visualizer/documentation"
RENDER = Path(__file__).with_name("render-documentation-html.py")

def clear():
    if sys.stdout.isatty():
        print("\033[2J\033[H", end="")

def pause():
    try:
        input("\nEnter — повернутися в меню: ")
    except EOFError:
        pass

def open_android(path: Path):
    if not path.is_file():
        print("Файл не знайдено:", path)
        return
    opener = shutil.which("termux-open")
    if opener and subprocess.run([opener, "--view", str(path)], check=False).returncode == 0:
        print("Відкрито:", path)
    else:
        print("Не вдалося відкрити автоматично. Шлях:", path)

def open_html(markdown: Path):
    if not markdown.is_file():
        print("Немає файла:", markdown)
        return
    CACHE.mkdir(parents=True, exist_ok=True)
    dest = CACHE / (markdown.stem + ".html")
    status = subprocess.run([sys.executable, str(RENDER), str(markdown), str(dest)], check=False)
    if status.returncode == 0:
        open_android(dest)
    else:
        print("Не вдалося підготувати HTML.")

def downloaded(ext: str):
    folders = [FOLDER, Path("/storage/emulated/0/Download"), Path("/storage/emulated/0/Downloads"),
               Path.home() / "storage/downloads", DOCS / "user"]
    for folder in folders:
        if not folder.is_dir():
            continue
        for file in sorted(folder.iterdir(), reverse=True):
            if not file.is_file() or file.suffix.casefold() != "." + ext:
                continue
            name = file.name.casefold()
            if "faric" not in name or "guide" not in name:
                continue
            if folder != FOLDER:
                try:
                    FOLDER.mkdir(parents=True, exist_ok=True)
                    target = FOLDER / file.name
                    if not target.exists():
                        shutil.copy2(file, target)
                    file = target
                except OSError:
                    pass
            open_android(file)
            return
    print("Файл ." + ext + " не знайдено. Збережи його з чату в Download")
    print("або поклади в:", FOLDER)
    print("PDF/DOCX не збережені в GitHub як файли.")

def browse(folder: Path):
    page = 0
    while True:
        entries = sorted((p for p in folder.iterdir() if p.is_dir() or p.suffix.casefold() in
                          (".md", ".html", ".pdf", ".docx", ".txt")),
                         key=lambda p: (not p.is_dir(), p.name.casefold()))
        pages = max(1, (len(entries)+6)//7)
        page = min(page, pages-1)
        shown = entries[page*7:page*7+7]
        clear()
        print("FARIC / DOCUMENTATION / КАТАЛОГ")
        print(str(folder.relative_to(ROOT)) + " · сторінка " + str(page+1) + "/" + str(pages) + "\n")
        for index, file in enumerate(shown, 1):
            print(str(index) + " — " + ("[Папка] " if file.is_dir() else "") + file.name)
        print("\n8 — Попередня    9 — Наступна    0 — Назад")
        try:
            choice = input("Вибір: ").strip()
        except EOFError:
            return
        if choice == "0":
            return
        if choice == "8":
            page = max(0, page-1)
        elif choice == "9":
            page = min(pages-1, page+1)
        elif choice.isdigit() and 1 <= int(choice) <= len(shown):
            file = shown[int(choice)-1]
            if file.is_dir():
                browse(file)
            else:
                open_html(file) if file.suffix.casefold() == ".md" else open_android(file)
                pause()

def core():
    docs = [("Почати тут", ROOT / "START_HERE_ASSISTANT.md"),
            ("Поточний стан", ROOT / "CURRENT_HANDOFF.md"),
            ("Активний план", ROOT / "ACTIVE_PLAN.md"),
            ("Відкриті проблеми", ROOT / "OPEN_FINDINGS.md"),
            ("Правила відповідей", ROOT / "ASSISTANT_RESPONSE_CONTRACT.md"),
            ("Правила посібника", DOCS / "user" / "README.md")]
    while True:
        clear()
        print("DOCUMENTATION / ОСНОВНІ ДОКУМЕНТИ\n")
        for i, (name, _) in enumerate(docs, 1):
            print(str(i) + " — " + name)
        print("0 — Назад")
        try:
            item = input("Вибір: ").strip()
        except EOFError:
            return
        if item == "0":
            return
        if item.isdigit() and 1 <= int(item) <= len(docs):
            open_html(docs[int(item)-1][1])
            pause()

def show_folder():
    try:
        FOLDER.mkdir(parents=True, exist_ok=True)
        print("Документи:", FOLDER)
    except OSError:
        print("Немає доступу до Documents. Спробуй termux-setup-storage.")
        return
    opener = shutil.which("termux-open")
    if opener:
        subprocess.run([opener, "--view", str(FOLDER)], check=False)

def show_online():
    url = "https://github.com/faric-ua/faric-music-visualizer/blob/main/docs/user/FARIC_USER_GUIDE_UK.md"
    opener = shutil.which("termux-open-url")
    if opener:
        subprocess.run([opener, url], check=False)
    else:
        print(url)

def main():
    while True:
        clear()
        print("====================================")
        print("       FARIC / DOCUMENTATION")
        print("====================================")
        print("ПОСІБНИК КОРИСТУВАЧА")
        print("1 — HTML · читати офлайн")
        print("2 — PDF · відкрити завантажений")
        print("3 — DOCX · відкрити завантажений")
        print("4 — Markdown · вихідний файл")
        print("\nДОКУМЕНТАЦІЯ ПРОЄКТУ")
        print("5 — Каталог документації ›")
        print("6 — Основні документи ›")
        print("7 — Папка Documentation")
        print("8 — Посібник у GitHub")
        print("\n0 — Назад до FARIC")
        try:
            choice = input("Вибір: ").strip()
        except EOFError:
            return
        if choice == "0":
            return
        if choice == "1":
            open_html(GUIDE)
        elif choice == "2":
            downloaded("pdf")
        elif choice == "3":
            downloaded("docx")
        elif choice == "4":
            open_android(GUIDE)
        elif choice == "5":
            browse(DOCS)
            continue
        elif choice == "6":
            core()
            continue
        elif choice == "7":
            show_folder()
        elif choice == "8":
            show_online()
        else:
            print("Невідомий пункт")
        pause()

if __name__ == "__main__":
    main()
