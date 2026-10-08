#!/usr/bin/env python3
"""Safely import FARIC user documentation from Android Downloads.

Imports only known user guide files. ZIP members cannot escape the target.
Works with standard Python 3, no external packages or GitHub credentials.
"""
import os
import re
import shutil
import zipfile
from pathlib import Path

DEST = Path(os.environ["FARIC_DOC_DEST"])
DOWNLOADS = [
    Path.home() / "storage" / "downloads",
    Path("/storage/emulated/0/Download"),
    Path("/storage/emulated/0/Downloads"),
]
ZIP_PATTERN = re.compile(
    r"FARIC_User_Guide_UK_v(\d+\.\d+\.\d+)_COMPLETE\.zip", re.IGNORECASE
)
FILE_PATTERN = re.compile(
    r"FARIC_User_Guide_UK_v(\d+\.\d+\.\d+)\.(pdf|docx|html|md)", re.IGNORECASE
)
MAX_FILE_SIZE = 25_000_000


def candidates():
    found = []
    seen = set()
    for folder in DOWNLOADS:
        if not folder.is_dir():
            continue
        for file in folder.iterdir():
            if not file.is_file():
                continue
            if not (ZIP_PATTERN.fullmatch(file.name) or FILE_PATTERN.fullmatch(file.name)):
                continue
            try:
                # Termux home/storage/downloads may alias /storage/emulated/0/Download.
                key = (file.name.lower(), file.stat().st_size, file.stat().st_mtime_ns)
                if key not in seen:
                    found.append(file)
                    seen.add(key)
            except OSError:
                continue
    return sorted(found, key=lambda path: path.stat().st_mtime)


def copy_member(archive, member, version):
    # Do not extract ZIP directories, nested paths, links or arbitrary files.
    if member.is_dir() or "/" in member.filename or "\\" in member.filename:
        return False
    matched = FILE_PATTERN.fullmatch(member.filename)
    if matched is None or matched.group(1) != version:
        return False
    if member.file_size > MAX_FILE_SIZE:
        print("Пропущено надто великий файл:", member.filename)
        return False
    target_folder = DEST / ("v" + version)
    target_folder.mkdir(parents=True, exist_ok=True)
    target = target_folder / member.filename
    with archive.open(member) as source, target.open("wb") as output:
        shutil.copyfileobj(source, output)
    print("✓", target)
    return True


def main():
    count = 0
    for source in candidates():
        zipped = ZIP_PATTERN.fullmatch(source.name)
        try:
            if zipped:
                with zipfile.ZipFile(source) as archive:
                    broken = archive.testzip()
                    if broken:
                        print("ZIP пошкоджено:", source.name, broken)
                        continue
                    for member in archive.infolist():
                        count += int(copy_member(archive, member, zipped.group(1)))
            else:
                matched = FILE_PATTERN.fullmatch(source.name)
                if source.stat().st_size > MAX_FILE_SIZE:
                    print("Пропущено надто великий файл:", source.name)
                    continue
                folder = DEST / ("v" + matched.group(1))
                folder.mkdir(parents=True, exist_ok=True)
                target = folder / source.name
                if source.resolve() != target.resolve():
                    shutil.copy2(source, target)
                print("✓", target)
                count += 1
        except (OSError, zipfile.BadZipFile) as error:
            print("Не вдалося імпортувати", source.name, "-", error)

    if count:
        print("\nPASS: імпортовано документів:", count)
        print("Документи знаходяться в:", DEST)
    else:
        print("Не знайдено комплекту FARIC у Download/Downloads.")
        print("Завантаж ZIP або PDF/DOCX/HTML з попередньої відповіді,")
        print("потім повтори Documentation → 6.")


if __name__ == "__main__":
    main()
