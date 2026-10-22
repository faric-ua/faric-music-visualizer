#!/usr/bin/env python3
"""V144 — «Прев'ю альбому» (меню MUSIC VISUALIZER → 21).

З кожної пісні альбому береться найсильніше місце (~20 с), шматки склеюються з плавними
переходами (одна пісня затихає — наступна наростає). Обкладинка — кругла наклейка диска
з Google Диска (зменшена до 1000×1000 JPEG), у тегах — лише історія альбому
(FARIC_ALBUM_STORY_UK / _EN). Готовий файл кладеться поруч із піснями:
«00 — PREVIEW — <назва> (<сторона> Side).mp3». Оригінали не змінюються.

Потрібно: ffmpeg + ffprobe (Termux: pkg install ffmpeg), Python 3.
Тест без інтернету: FARIC_PREVIEW_OFFLINE_DIR=<папка з файлами <id>.json / <id>.png>.
"""
from __future__ import annotations

import argparse
import array
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.request
from pathlib import Path

AUDIO_EXT = {".mp3", ".flac", ".m4a", ".wav", ".ogg", ".opus", ".aac"}
DEFAULT_ROOTS = ["/storage/emulated/0/Music", "/storage/emulated/0/Download"]
CONFIG = Path(__file__).with_name("album-preview-albums.json")
ANALYSIS_RATE = 4000  # Гц — для пошуку гучного місця досить
BLOCK_SEC = 0.5
COVER_SIZE = 1000


def say(msg: str = "") -> None:
    print(msg, flush=True)


def run(cmd: list[str], **kw) -> subprocess.CompletedProcess:
    # stdin закритий: ffmpeg не має «з'їдати» натискання клавіш у меню і не чекає на ввід у CI.
    return subprocess.run(cmd, check=True, stdin=subprocess.DEVNULL, **kw)


# ---------- пошук і впізнавання ----------

def audio_files(folder: Path) -> list[Path]:
    out = []
    for p in sorted(folder.iterdir()):
        if p.is_file() and p.suffix.lower() in AUDIO_EXT and "PREVIEW" not in p.name.upper():
            out.append(p)
    return out


def find_album_folders(roots: list[str], max_depth: int = 5, min_files: int = 3) -> list[Path]:
    found: list[Path] = []
    for root in roots:
        base = Path(root)
        if not base.is_dir():
            continue
        for dirpath, dirnames, _ in os.walk(base):
            depth = len(Path(dirpath).relative_to(base).parts)
            dirnames[:] = [d for d in dirnames if not d.startswith(".") and d != "Android"]
            if depth >= max_depth:
                dirnames[:] = []
            p = Path(dirpath)
            try:
                if len(audio_files(p)) >= min_files:
                    found.append(p)
            except OSError:
                pass
    return found


def probe(path: Path) -> dict:
    res = subprocess.run(
        ["ffprobe", "-v", "error", "-print_format", "json", "-show_format", "-show_streams", str(path)],
        capture_output=True, text=True, check=True, stdin=subprocess.DEVNULL)
    data = json.loads(res.stdout)
    fmt = data.get("format", {})
    tags = {k.lower(): v for k, v in (fmt.get("tags") or {}).items()}
    for s in data.get("streams", []):
        for k, v in (s.get("tags") or {}).items():
            tags.setdefault(k.lower(), v)
    has_pic = any(s.get("codec_type") == "video" for s in data.get("streams", []))
    return {"duration": float(fmt.get("duration") or 0), "tags": tags, "has_pic": has_pic,
            "raw_tags": dict(fmt.get("tags") or {})}


# Теги, що стосуються саме однієї пісні (або які прев'ю пише саме) — їх з пісні НЕ копіюємо.
SONG_ONLY_TAGS = {
    "title", "track", "tracknumber", "tracktotal", "totaltracks", "length", "tlen", "duration",
    "bpm", "tbpm", "initialkey", "key", "isrc", "encoder", "tsse",
    "replaygain_track_gain", "replaygain_track_peak", "itunnorm", "itunsmpb",
    "musicbrainz_trackid", "musicbrainz_releasetrackid", "acoustid_id", "acoustid_fingerprint",
    "faric_story_uk", "faric_story_en", "faric_story_source",
    "faric_album_story_uk", "faric_album_story_en", "faric_preview_tracks",
}
PREVIEW_ENCODER = "PulseDeck Preview"


def common_tags(raw: dict) -> dict:
    """Спільні теги альбому з однієї пісні (виконавець, альбом, права, рік, жанр, коментар,
    CONVERTER, …) — без назви, номера, слів і історії самої пісні."""
    out = {}
    for k, v in raw.items():
        low = k.lower()
        if low in SONG_ONLY_TAGS or low.startswith("lyrics") or low.startswith("unsyncedlyrics"):
            continue
        if isinstance(v, str) and v.strip():
            out[k] = v
    return out


def encoder_list(raw: dict) -> str:
    """ENCODER — список (правило конвертера): що було в пісні + прев'ю в кінці, без повторів."""
    old = next((v for k, v in raw.items() if k.lower() in ("encoder", "tsse")), "")
    items = [x.strip() for x in old.split(";") if x.strip() and not x.strip().startswith("Lavf")]
    if PREVIEW_ENCODER not in items:
        items.append(PREVIEW_ENCODER)
    return "; ".join(items)


def identify_album(folder: Path, first_tags: dict, config: dict) -> tuple[dict | None, str]:
    text = " ".join([first_tags.get("album", ""), str(folder)]).upper()
    album = None
    for a in config["albums"]:
        if a["match"].upper() in text:
            album = a
            break
    side = "black" if "BLACK" in text else "white"
    return album, side


def norm_title(s: str) -> str:
    s = s.upper()
    s = re.sub(r"\(\s*2\s*\)", " ", s)
    s = re.sub(r"\.(MP3|FLAC|M4A|WAV|OGG|OPUS|AAC)$", "", s)
    s = re.sub(r"^\s*(BONUS\s+)?TRACK\s*\d+\s*[—–-]\s*", "", s)
    s = re.sub(r"^\s*EXPERIMENTAL\s+DUET\s*[—–-]\s*", "", s)
    return re.sub(r"[^A-Z0-9А-ЯІЇЄҐ]+", "", s)


def fallback_key(p: Path):
    name = p.name.upper()
    m = re.match(r"\s*TRACK\s*(\d+)", name)
    if m:
        return (0, int(m.group(1)), name)
    m = re.match(r"\s*BONUS\s+TRACK\s*(\d+)", name)
    if m:
        return (1, int(m.group(1)), name)
    return (2, 0, name)


def order_tracks(files: list[Path], titles: dict[Path, str], story: dict | None) -> list[Path]:
    if not story:
        return sorted(files, key=fallback_key)
    order = {}
    for i, t in enumerate(story.get("tracks", [])):
        order.setdefault(norm_title(t["title"]), i)
    matched, rest = [], []
    for f in files:
        key = norm_title(titles.get(f) or f.name)
        (matched if key in order else rest).append((order.get(key, 0), f))
    return [f for _, f in sorted(matched, key=lambda x: x[0])] + sorted((f for _, f in rest), key=fallback_key)


# ---------- найсильніше місце ----------

def block_energies(path: Path) -> list[float]:
    res = subprocess.run(
        ["ffmpeg", "-nostdin", "-v", "error", "-i", str(path), "-vn", "-ac", "1", "-ar", str(ANALYSIS_RATE),
         "-f", "s16le", "-"], capture_output=True, check=True, stdin=subprocess.DEVNULL)
    samples = array.array("h")
    samples.frombytes(res.stdout[: len(res.stdout) // 2 * 2])
    if sys.byteorder == "big":
        samples.byteswap()
    n = int(ANALYSIS_RATE * BLOCK_SEC)
    out = []
    for i in range(0, len(samples) - n + 1, n):
        blk = samples[i:i + n]
        out.append(sum(x * x for x in blk) / n)
    return out


def loudest_start(energies: list[float], duration: float, seg: float) -> float:
    """Початок вікна seg с з найбільшою середньою енергією; не з самого вступу і не впритул до кінця."""
    if duration <= seg + 4 or not energies:
        return 0.0
    win = max(1, int(round(seg / BLOCK_SEC)))
    first = int((duration * 0.08) / BLOCK_SEC)
    last = int((duration - seg - 2) / BLOCK_SEC)
    last = min(last, len(energies) - win)
    if last < first:
        return max(0.0, min(duration * 0.3, duration - seg))
    acc = sum(energies[first:first + win])
    best, best_i = acc, first
    for i in range(first + 1, last + 1):
        acc += energies[i + win - 1] - energies[i - 1]
        if acc > best * 1.0001:
            best, best_i = acc, i
    return best_i * BLOCK_SEC


# ---------- Диск ----------

def fetch(file_id: str, dest: Path) -> bool:
    offline = os.environ.get("FARIC_PREVIEW_OFFLINE_DIR")
    if offline:
        for p in Path(offline).glob(file_id + ".*"):
            shutil.copy(p, dest)
            return True
        return False
    url = f"https://drive.google.com/uc?export=download&id={file_id}"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 PulseDeck-preview"})
        with urllib.request.urlopen(req, timeout=60) as r:
            data = r.read()
    except Exception as e:  # мережа, 403, 404
        say(f"   ! не вдалося скачати з Диска: {e}")
        return False
    head = data[:200].lstrip().lower()
    if head.startswith(b"<!doctype html") or head.startswith(b"<html"):
        say("   ! Диск віддав сторінку замість файлу (немає доступу за посиланням?)")
        return False
    dest.write_bytes(data)
    return True


def make_cover(src: Path, out: Path) -> bool:
    try:
        run(["ffmpeg", "-nostdin", "-v", "error", "-y", "-i", str(src), "-frames:v", "1", "-vf",
             f"scale={COVER_SIZE}:{COVER_SIZE}:force_original_aspect_ratio=decrease,"
             f"pad={COVER_SIZE}:{COVER_SIZE}:(ow-iw)/2:(oh-ih)/2:color=black,format=yuvj420p",
             "-q:v", "3", str(out)])
        return out.exists() and out.stat().st_size > 0
    except subprocess.CalledProcessError:
        return False


# ---------- збирання ----------

def fmt_time(sec: float) -> str:
    sec = int(round(sec))
    return f"{sec // 60}:{sec % 60:02d}"


def unique_path(p: Path) -> Path:
    if not p.exists():
        return p
    i = 2
    while True:
        q = p.with_name(f"{p.stem} ({i}){p.suffix}")
        if not q.exists():
            return q
        i += 1


def build_preview(folder: Path, config: dict, seg: float, fade: float, assume_yes: bool) -> Path | None:
    files = audio_files(folder)
    if len(files) < 2:
        say("У папці менше двох пісень — прев'ю не роблю.")
        return None
    say(f"Папка: {folder}")
    say(f"Пісень: {len(files)}. Читаю теги…")
    info = {f: probe(f) for f in files}
    first_tags = info[files[0]]["tags"]
    album, side = identify_album(folder, first_tags, config)
    side_name = "Black" if side == "black" else "White"
    if album:
        say(f"Альбом: {album['album']} · {side_name} Side")
    else:
        say("Альбом не впізнано (нема в списку) — прев'ю без історії й без наклейки диска.")

    tmp = Path(tempfile.mkdtemp(prefix="pulsedeck-preview-"))
    try:
        story = None
        if album and album.get("story_id"):
            say("Качаю історію альбому з Диска…")
            sp = tmp / "story.json"
            if fetch(album["story_id"], sp):
                try:
                    story = json.loads(sp.read_text(encoding="utf-8"))
                except ValueError:
                    say("   ! файл історії зіпсований — без історії")
        elif album:
            say("Для цього альбому історії ще нема — прев'ю без історії.")

        titles = {f: info[f]["tags"].get("title", "") for f in files}
        files = order_tracks(files, titles, story)
        if len(files) > 13:
            say(f"Пісень більше 13 ({len(files)}) — беру всі по порядку.")

        say("Шукаю найсильніше місце в кожній пісні…")
        parts = []
        for f in files:
            dur = info[f]["duration"]
            # Починаємо трохи раніше гучного місця: перехід (fade) припадає на наростання,
            # а сам дроп звучить уже чисто.
            start = max(0.0, loudest_start(block_energies(f), dur, seg) - fade)
            length = min(seg, max(1.0, dur - start))
            parts.append((f, start, length))
            say(f"   {fmt_time(start)}–{fmt_time(start + length)}  {titles.get(f) or f.name}")

        cover = None
        if album and (album.get("disc") or {}).get(side):
            say("Качаю наклейку диска з Диска і зменшую…")
            raw = tmp / "disc.png"
            if fetch(album["disc"][side], raw) and make_cover(raw, tmp / "cover.jpg"):
                cover = tmp / "cover.jpg"
        if cover is None and info[files[0]]["has_pic"]:
            say("Беру обкладинку з першої пісні…")
            emb = tmp / "embedded"
            try:
                run(["ffmpeg", "-nostdin", "-v", "error", "-y", "-i", str(files[0]), "-an", "-frames:v", "1",
                     "-c:v", "copy", "-f", "image2", str(emb)])
                if make_cover(emb, tmp / "cover.jpg"):
                    cover = tmp / "cover.jpg"
            except subprocess.CalledProcessError:
                pass
        if cover is None:
            say("   (без обкладинки)")

        n = len(parts)
        fade = min(fade, min(p[2] for p in parts) / 3)
        total = sum(p[2] for p in parts) - fade * (n - 1)
        say(f"Склеюю шматки ({n}) з переходами по {fade:g} с — вийде {fmt_time(total)}.")

        base_name = (album["album"].split("—", 1)[-1].strip() if album
                     else (first_tags.get("album") or folder.name))
        out = unique_path(folder / f"00 — PREVIEW — {base_name} ({side_name} Side).mp3")
        if not assume_yes:
            ans = input(f"Створити «{out.name}»? Enter — так, n — ні: ").strip().lower()
            if ans.startswith("n") or ans.startswith("н"):
                say("Скасовано.")
                return None

        cmd = ["ffmpeg", "-nostdin", "-v", "error", "-y"]
        for f, start, length in parts:
            cmd += ["-ss", f"{start:.2f}", "-t", f"{length:.2f}", "-i", str(f)]
        if cover:
            cmd += ["-i", str(cover)]
        chains = [f"[{i}:a]aformat=sample_fmts=fltp:sample_rates=48000:channel_layouts=stereo[a{i}]"
                  for i in range(n)]
        prev = "a0"
        for i in range(1, n):
            cur = f"x{i}"
            chains.append(f"[{prev}][a{i}]acrossfade=d={fade:.2f}:c1=qsin:c2=qsin[{cur}]")
            prev = cur
        fade_in = min(1.5, total / 4)
        fade_out = min(3.0, total / 4)
        chains.append(f"[{prev}]afade=t=in:d={fade_in:.2f},afade=t=out:st={total - fade_out:.2f}:d={fade_out:.2f}[out]")
        # -map_metadata -1: ffmpeg сам НЕ переносить теги першої пісні (там її назва, слова, історія) —
        # спільні теги ставимо самі нижче.
        cmd += ["-filter_complex", ";".join(chains), "-map", "[out]", "-map_metadata", "-1", "-map_chapters", "-1"]
        if cover:
            cmd += ["-map", f"{n}:v", "-c:v", "copy", "-disposition:v:0", "attached_pic",
                    "-metadata:s:v", "title=Album cover", "-metadata:s:v", "comment=Cover (front)"]
        # +bitexact: ffmpeg не підміняє «Програма-кодувальник» своїм «Lavf…» — пишемо свій список (TSSE).
        cmd += ["-c:a", "libmp3lame", "-b:a", "320k", "-id3v2_version", "3", "-write_id3v1", "0",
                "-fflags", "+bitexact"]

        tracklist = "\n".join(
            f"{i + 1:02d}. {titles.get(f) or f.stem} — {fmt_time(s)}–{fmt_time(s + l)}"
            for i, (f, s, l) in enumerate(parts))
        # Спільні теги — з першої пісні альбому (уже в правильному порядку).
        ref_raw = info[parts[0][0]]["raw_tags"]
        meta = common_tags(ref_raw)
        if not any(k.lower() == "album" for k in meta) and album:
            meta["album"] = album["album"]
        meta.update({
            "title": f"{base_name} — PREVIEW ({side_name} Side)",
            "track": "0",
            "TSSE": encoder_list(ref_raw),
            "FARIC_PREVIEW_TRACKS": tracklist,
        })
        if story:
            meta["FARIC_ALBUM_STORY_UK"] = story.get("album_story", {}).get("uk", "")
            meta["FARIC_ALBUM_STORY_EN"] = story.get("album_story", {}).get("en", "")
            meta["FARIC_STORY_SOURCE"] = (f"{album.get('story_file')} · v{story.get('version')} · "
                                          f"{story.get('updated', '')} · PREVIEW")
        for k, v in meta.items():
            if v:
                cmd += ["-metadata", f"{k}={v}"]
        cmd.append(str(out))
        say("Кодую MP3 320 кбіт/с…")
        run(cmd)
        say()
        say("ГОТОВО ✔")
        say(f"Файл: {out}")
        say(f"Тривалість: {fmt_time(total)} · обкладинка: {'так' if cover else 'ні'} · "
            f"історія альбому: {'так' if story else 'ні'}")
        return out
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def choose_folder(roots: list[str], config: dict) -> Path | None:
    say("Шукаю папки з піснями…")
    folders = find_album_folders(roots)
    if not folders:
        say("Не знайшов папок з піснями в: " + ", ".join(roots))
        return None

    def score(p: Path):
        up = str(p).upper()
        known = any(a["match"].upper() in up for a in config["albums"])
        return (0 if known else 1, str(p))

    folders.sort(key=score)
    say()
    for i, p in enumerate(folders, 1):
        try:
            rel = p.relative_to("/storage/emulated/0")
        except ValueError:
            rel = p
        say(f"{i} — {rel}  ({len(audio_files(p))} пісень)")
    say("0 — Назад")
    say()
    ans = input("Вибери папку: ").strip()
    if not ans.isdigit() or int(ans) == 0 or int(ans) > len(folders):
        return None
    return folders[int(ans) - 1]


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="Прев'ю альбому: по 20 с з кожної пісні з переходами")
    ap.add_argument("--folder", help="папка з піснями (без вибору зі списку)")
    ap.add_argument("--roots", nargs="*", default=DEFAULT_ROOTS)
    ap.add_argument("--config", default=str(CONFIG))
    ap.add_argument("--seconds", type=float, default=20.0)
    ap.add_argument("--fade", type=float, default=3.0)
    ap.add_argument("--yes", action="store_true", help="не питати підтвердження")
    a = ap.parse_args(argv)
    for tool in ("ffmpeg", "ffprobe"):
        if not shutil.which(tool):
            say(f"Немає {tool}. Термукс: pkg install ffmpeg")
            return 1
    config = json.loads(Path(a.config).read_text(encoding="utf-8"))
    folder = Path(a.folder) if a.folder else choose_folder(a.roots, config)
    if folder is None:
        return 0
    if not folder.is_dir():
        say(f"Немає такої папки: {folder}")
        return 1
    try:
        out = build_preview(folder, config, a.seconds, a.fade, a.yes)
    except subprocess.CalledProcessError as e:
        say(f"Помилка ffmpeg (код {e.returncode}). Оригінали не змінено.")
        return 1
    return 0 if out or not a.yes else 1


if __name__ == "__main__":
    sys.exit(main())
