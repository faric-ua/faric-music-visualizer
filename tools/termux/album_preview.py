#!/usr/bin/env python3
"""V144 — «Прев'ю альбому» (меню MUSIC VISUALIZER → 21).

З кожної пісні альбому береться найсильніше місце (~20 с), шматки склеюються з плавними
переходами (одна пісня затихає — наступна наростає). Обкладинка — кругла наклейка диска
з Google Диска (зменшена до 1000×1000 JPEG), у тегах — лише історія альбому
(FARIC_ALBUM_STORY_UK / _EN). Готовий файл кладеться поруч із піснями:
«PREVIEW — <назва> — <сторона> Side.mp3». Оригінали не змінюються.

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


def number_of(name: str, track_tag: str = "") -> tuple[int, bool] | None:
    """(номер, бонус) з назви («TRACK 03 — …», «BONUS TRACK 12 — …», «BONUS 02 …») або з тегу номера («3/13»)."""
    up = name.upper()
    m = re.search(r"BONUS\s*(?:TRACK)?\s*(\d+)", up)
    if m:
        return int(m.group(1)), True
    m = re.search(r"TRACK\s*(\d+)", up)
    if m:
        return int(m.group(1)), False
    m = re.match(r"\s*(\d+)", track_tag or "")
    if m:
        return int(m.group(1)), False
    return None


def story_position(story: dict, title: str, name: str, track_tag: str) -> int | None:
    """V194: яка це пісня альбому (індекс у story["tracks"]): точна назва → назва починається з назви з
    історії («AFTERGLOW V2» = AFTERGLOW) → номер (TRACK n; BONUS n; номер понад останній TRACK — бонус)."""
    tracks = story.get("tracks", [])
    key = norm_title(title or name)
    for i, t in enumerate(tracks):
        if norm_title(t["title"]) == key:
            return i
    best = None
    for i, t in enumerate(tracks):
        k = norm_title(t["title"])
        if len(k) >= 4 and key.startswith(k) and (best is None or len(k) > len(norm_title(tracks[best]["title"]))):
            best = i
    if best is not None:
        return best
    num = number_of(title or "", "") or number_of(name, track_tag)
    if not num:
        return None
    n, bonus = num
    last = max((t["n"] for t in tracks if not t.get("bonus")), default=0)
    if not bonus and last and n > last:
        n, bonus = n - last, True
    elif bonus and last and n > last:
        n -= last  # «BONUS TRACK 12» після 10 треків — другий бонус
    for i, t in enumerate(tracks):
        if t["n"] == n and bool(t.get("bonus")) == bonus:
            return i
    return None


def track_label(t: dict) -> str:
    return f"{'BONUS' if t.get('bonus') else 'TRACK'} {t['n']:02d}"


def display_title(title: str, name: str) -> str:
    """Назва пісні без «TRACK 03 —» / «BONUS TRACK 11 —» і без «.mp3»."""
    s = (title or Path(name).stem).strip()
    s = re.sub(r"^\s*(BONUS\s+)?(TRACK\s*)?\d+\s*[—–-]\s*", "", s, flags=re.I)
    return s.strip() or Path(name).stem


def order_tracks(files: list[Path], titles: dict[Path, str], story: dict | None,
                 track_tags: dict[Path, str] | None = None) -> list[Path]:
    """V194 (faric 24.10 23:07: «по порядковому номеру: 01…10, бонус 1, 2, 3»): за історією альбому —
    TRACK 01–10, потім BONUS 01–03; чого нема в історії — у кінці за назвою файлу."""
    if not story:
        return sorted(files, key=fallback_key)
    tracks = story.get("tracks", [])
    matched, rest = [], []
    for f in files:
        pos = story_position(story, titles.get(f, ""), f.name, (track_tags or {}).get(f, ""))
        if pos is None:
            rest.append(f)
        else:
            t = tracks[pos]
            matched.append(((1 if t.get("bonus") else 0, t["n"], f.name), f))
    return [f for _, f in sorted(matched, key=lambda x: x[0])] + sorted(rest, key=fallback_key)


def tracklist_lines(parts: list, titles: dict, story: dict | None, track_tags: dict | None = None) -> list[str]:
    """V194: «TRACK 01 — IGNITION POINT — 4:24–4:44», за порядком у прев'ю."""
    out = []
    for i, (f, s, l) in enumerate(parts):
        label = f"{i + 1:02d}"
        if story:
            pos = story_position(story, titles.get(f, ""), f.name, (track_tags or {}).get(f, ""))
            if pos is not None:
                label = track_label(story["tracks"][pos])
        out.append(f"{label} — {display_title(titles.get(f, ''), f.name)} — {fmt_time(s)}–{fmt_time(s + l)}")
    return out


PREVIEW_NOTE = {
    "uk": "З чого складено це прев'ю — шматки пісень (хвилина:секунда):",
    "en": "What this preview is made of — song excerpts (min:sec):",
}


def story_with_tracklist(text: str, lines: list[str], lang: str) -> str:
    """V194: історія диска + у кінці список шматків, з яких зроблене прев'ю."""
    if not text or not lines:
        return text
    return text.rstrip() + "\n\n" + PREVIEW_NOTE[lang] + "\n" + "\n".join(lines)


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


# ---------- кругла наклейка: рівне коло замість «рваних» країв ----------

DISC_SCAN = 400  # розмір, на якому шукаємо край диска й дірку (досить і швидко)


def _disc_mask(src: Path, n: int = DISC_SCAN) -> list[bytearray] | None:
    """Маска «тут є диск» n×n: з прозорості PNG, а якщо вона суцільна — за кольором фону в кутах."""
    def raw(vf: str, pix: str, ch: int) -> bytes:
        res = subprocess.run(["ffmpeg", "-nostdin", "-v", "error", "-i", str(src), "-frames:v", "1",
                              "-vf", f"scale={n}:{n},{vf}", "-f", "rawvideo", "-pix_fmt", pix, "-"],
                             capture_output=True, check=True, stdin=subprocess.DEVNULL)
        data = res.stdout
        return data if len(data) == n * n * ch else b""
    alpha = raw("format=rgba,alphaextract", "gray", 1)
    if alpha and min(alpha) < 128 and sum(1 for a in alpha if a >= 128) > n * n * 0.3:
        return [bytearray(1 if alpha[y * n + x] >= 128 else 0 for x in range(n)) for y in range(n)]
    rgb = raw("format=rgb24", "rgb24", 3)
    if not rgb:
        return None
    def px(x, y):
        i = (y * n + x) * 3
        return rgb[i], rgb[i + 1], rgb[i + 2]
    corners = [px(2, 2), px(n - 3, 2), px(2, n - 3), px(n - 3, n - 3)]
    bg = tuple(sum(c[k] for c in corners) // 4 for k in range(3))
    if max(abs(c[k] - bg[k]) for c in corners for k in range(3)) > 40:
        return None  # кути різного кольору — фону нема, це квадратна картинка
    def far(x, y):
        r, g, b = px(x, y)
        return abs(r - bg[0]) + abs(g - bg[1]) + abs(b - bg[2]) > 60
    return [bytearray(1 if far(x, y) else 0 for x in range(n)) for y in range(n)]


def disc_geometry(mask: list[bytearray]) -> tuple[float, float, float, float] | None:
    """(cx, cy, r_out, r_in) у пікселях маски. r_out — трохи всередину від найглибшої вм'ятини краю,
    r_in — трохи назовні від найдальшого «рваного» краю дірки. None — це не диск."""
    import math
    n = len(mask)
    sx = sy = cnt = 0
    for y, row in enumerate(mask):
        for x, v in enumerate(row):
            if v:
                sx += x; sy += y; cnt += 1
    if cnt < n * n * 0.3:
        return None
    cx, cy = sx / cnt, sy / cnt
    rmax = math.hypot(n, n) / 2
    outs, ins = [], []
    for k in range(180):
        a = 2 * math.pi * k / 180
        dx, dy = math.cos(a), math.sin(a)
        def on(r):
            x, y = int(round(cx + dx * r)), int(round(cy + dy * r))
            return 0 <= x < n and 0 <= y < n and mask[y][x] == 1
        r = rmax
        while r > 0 and not (on(r) and on(r - 1) and on(r - 2)):
            r -= 1
        outs.append(r)
        r = 0.0
        while r < rmax and not (on(r) and on(r + 1) and on(r + 2)):
            r += 1
        ins.append(r)
    outs.sort(); ins.sort()
    r_out = outs[int(len(outs) * 0.05)] - n * 0.006
    r_in = ins[int(len(ins) * 0.95)] + n * 0.006 if ins[len(ins) // 2] > n * 0.01 else 0.0
    if r_out < n * 0.3 or r_in >= r_out * 0.6:
        return None
    return cx, cy, r_out, r_in


def make_disc_cover(src: Path, out: Path, size: int = COVER_SIZE) -> bool:
    """Кругла наклейка: рівне коло з рівною діркою, згладжені краї, прозорий фон (PNG)."""
    mask = _disc_mask(src)
    geo = disc_geometry(mask) if mask else None
    if not geo:
        return False
    n = len(mask)
    cx, cy, r_out, r_in = geo
    try:
        res = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries",
                              "stream=width,height", "-of", "csv=p=0", str(src)],
                             capture_output=True, text=True, check=True, stdin=subprocess.DEVNULL)
        w, h = (int(v) for v in res.stdout.strip().split(",")[:2])
    except (subprocess.CalledProcessError, ValueError):
        return False
    kx, ky = w / n, h / n
    side = 2 * r_out * (kx + ky) / 2
    x0, y0 = cx * kx - side / 2, cy * ky - side / 2
    half = size / 2
    ro = half - 1.0
    ri = r_in / r_out * ro if r_in else 0.0
    d = "hypot(X-%.1f,Y-%.1f)" % (half, half)
    alpha = f"255*clip({ro:.2f}-{d}+0.5,0,1)" + (f"*clip({d}-{ri:.2f}+0.5,0,1)" if ri else "")
    vf = (f"format=rgba,crop={side:.0f}:{side:.0f}:{max(0.0, x0):.0f}:{max(0.0, y0):.0f}:exact=1,"
          f"scale={size}:{size}:flags=lanczos,"
          f"geq=r='r(X,Y)':g='g(X,Y)':b='b(X,Y)':a='{alpha}'")
    try:
        run(["ffmpeg", "-nostdin", "-v", "error", "-y", "-i", str(src), "-frames:v", "1", "-vf", vf,
             "-pix_fmt", "rgba", "-compression_level", "9", str(out)])
        return out.exists() and out.stat().st_size > 0
    except subprocess.CalledProcessError:
        return False


# ---------- збирання ----------

def fmt_time(sec: float) -> str:
    sec = int(round(sec))
    return f"{sec // 60}:{sec % 60:02d}"


CD_MINUTES = 80  # звичайний CD-R (700 МБ); старі диски — 74 хв (650 МБ)


def fmt_long(sec: float) -> str:
    sec = int(round(sec))
    h, m, s_ = sec // 3600, sec // 60 % 60, sec % 60
    return f"{h}:{m:02d}:{s_:02d}" if h else f"{m}:{s_:02d}"


def cd_fit(total_sec: float) -> str:
    """V194: чи влазить сторона на один аудіо-CD."""
    if total_sec <= 74 * 60:
        return "влазить на CD (і на 80 хв, і на старий 74 хв)"
    if total_sec <= CD_MINUTES * 60:
        return f"влазить на CD 80 хв (вільно {fmt_time(CD_MINUTES * 60 - total_sec)}); на старий 74 хв — ні"
    return f"НЕ влазить на CD 80 хв — зайве {fmt_time(total_sec - CD_MINUTES * 60)}"


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
        track_tags = {f: info[f]["tags"].get("track", "") for f in files}
        files = order_tracks(files, titles, story, track_tags)
        if len(files) > 13:
            say(f"Пісень більше 13 ({len(files)}) — беру всі по порядку.")
        total = sum(info[f]["duration"] for f in files)
        say(f"Усього на стороні: {fmt_long(total)} ({len(files)} пісень) — {cd_fit(total)}")

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
            if fetch(album["disc"][side], raw):
                if make_disc_cover(raw, tmp / "cover.png"):
                    cover = tmp / "cover.png"
                    say("   наклейка: рівне коло, прозорий фон (PNG)")
                elif make_cover(raw, tmp / "cover.jpg"):
                    cover = tmp / "cover.jpg"
                    say("   наклейку не вдалося обрізати колом — квадратом, як є")
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
        out = unique_path(folder / f"PREVIEW — {base_name} — {side_name} Side.mp3")
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

        lines = tracklist_lines(parts, titles, story, track_tags)
        tracklist = "\n".join(lines)
        # Спільні теги — з першої пісні альбому (уже в правильному порядку).
        ref_raw = info[parts[0][0]]["raw_tags"]
        meta = common_tags(ref_raw)
        if not any(k.lower() == "album" for k in meta) and album:
            meta["album"] = album["album"]
        meta.update({
            "title": f"PREVIEW — {base_name} — {side_name} Side",
            "track": "0",
            "TSSE": encoder_list(ref_raw),
            "FARIC_PREVIEW_TRACKS": tracklist,
        })
        if story:
            meta["FARIC_ALBUM_STORY_UK"] = story_with_tracklist(story.get("album_story", {}).get("uk", ""), lines, "uk")
            meta["FARIC_ALBUM_STORY_EN"] = story_with_tracklist(story.get("album_story", {}).get("en", ""), lines, "en")
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
