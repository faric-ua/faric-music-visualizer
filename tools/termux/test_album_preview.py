#!/usr/bin/env python3
"""V144: перевірка «Прев'ю альбому» без телефона й без інтернету (синтетичні пісні)."""
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import album_preview as ap  # noqa: E402


def ffprobe_json(p: Path) -> dict:
    out = subprocess.run(["ffprobe", "-v", "error", "-print_format", "json", "-show_format",
                          "-show_streams", str(p)], capture_output=True, text=True, check=True).stdout
    return json.loads(out)


def make_track(path: Path, title: str, loud_from: float, dur: float = 90.0, freq: int = 220) -> None:
    expr = f"0.05*sin(2*PI*{freq}*t)*(1+15*between(t\\,{loud_from}\\,{loud_from + 20}))"
    subprocess.run(["ffmpeg", "-nostdin", "-v", "error", "-y", "-f", "lavfi", "-i",
                    f"aevalsrc={expr}|{expr}:s=48000:d={dur}",
                    "-b:a", "128k", "-metadata", f"title={title}", "-metadata", "artist=Faric(AI)",
                    "-metadata", "album=VOL. 99 — TEST PULSE — White Side",
                    "-metadata", "album_artist=Faric", "-metadata", "composer=Faric",
                    "-metadata", "copyright=Saney", "-metadata", "genre=Trance", "-metadata", "date=2026",
                    "-metadata", "comment=for memory....",
                    "-metadata", "TSSE=Track Catcher; Audio Converter 0.24.1",
                    "-metadata", "CONVERTER=Audio Converter 0.29.0 · лише теги",
                    "-metadata", f"FARIC_STORY_UK=Історія пісні {title}",
                    "-metadata", f"lyrics=words of {title}",
                    "-metadata", "track=7", "-fflags", "+bitexact", str(path)], check=True)


def test_unit() -> None:
    assert ap.norm_title("BONUS TRACK 11 — TEN ROADS, ONE HEART.mp3") == ap.norm_title("TEN ROADS, ONE HEART")
    assert ap.norm_title("TRACK 01 — IGNITION POINT (2)") == ap.norm_title("IGNITION POINT")
    e = [1.0] * 200
    for i in range(100, 140):
        e[i] = 50.0
    assert abs(ap.loudest_start(e, 100.0, 20.0) - 50.0) < 0.6, ap.loudest_start(e, 100.0, 20.0)
    assert ap.loudest_start(e, 22.0, 20.0) == 0.0
    assert ap.fmt_long(4 * 3600 / 60 * 19 + 5) == "1:16:05", ap.fmt_long(4 * 3600 / 60 * 19 + 5)
    assert ap.cd_fit(70 * 60).startswith("влазить на CD (і на 80")
    assert "вільно 3:30" in ap.cd_fit(76 * 60 + 30)
    assert "зайве 2:15" in ap.cd_fit(82 * 60 + 15)
    print("unit PASS")


def test_order_and_tracklist() -> None:
    """V194 (faric 24.10 23:07): порядок TRACK 01–10, BONUS 01–03 і список шматків у кінці історії диска.
    Назви — як у справжньому прев'ю Black Side (там AFTERGLOW V2 стояв останнім)."""
    story = {"tracks": [{"n": i + 1, "bonus": False, "title": t} for i, t in enumerate(
        ["IGNITION POINT", "NEON LANES", "AFTERGLOW", "ELECTRIC RAIN", "PULSE CONTROL", "SKYBRIDGE",
         "BLUE MIRAGE", "NIGHT SHIFT", "STARLIGHT RUN", "HORIZON ECHOES"])]
        + [{"n": 1, "bonus": True, "title": "TEN ROADS, ONE HEART"},
           {"n": 2, "bonus": True, "title": "TEN ROADS, ONE OATH"},
           {"n": 3, "bonus": True, "title": "TWO VOICES, ONE ROAD"}]}
    names = ["BONUS TRACK 11 — TEN ROADS, ONE HEART.mp3", "TRACK 02 — NEON LANES.mp3", "TRACK 03 — AFTERGLOW V2.mp3",
             "TWO VOICES, ONE ROAD..mp3", "IGNITION POINT.mp3", "BONUS TRACK 12 — TEN ROADS, ONE OATH.mp3",
             "TRACK 10 — HORIZON ECHOES.mp3", "TRACK 04 — ELECTRIC RAIN.mp3", "TRACK 05 — PULSE CONTROL.mp3",
             "TRACK 06 — SKYBRIDGE.mp3", "TRACK 07 — BLUE MIRAGE.mp3", "TRACK 08 — NIGHT SHIFT.mp3",
             "TRACK 09 — STARLIGHT RUN.mp3", "something else.mp3"]
    files = [Path("/x") / n for n in names]
    titles = {f: Path(f.name).stem for f in files}
    titles[files[3]] = "TWO VOICES, ONE ROAD."
    ordered = ap.order_tracks(files, titles, story, {})
    got = [f.name for f in ordered]
    assert got[0] == "IGNITION POINT.mp3" and got[2] == "TRACK 03 — AFTERGLOW V2.mp3", got
    assert got[9] == "TRACK 10 — HORIZON ECHOES.mp3", got
    assert got[10:13] == ["BONUS TRACK 11 — TEN ROADS, ONE HEART.mp3", "BONUS TRACK 12 — TEN ROADS, ONE OATH.mp3",
                          "TWO VOICES, ONE ROAD..mp3"], got
    assert got[13] == "something else.mp3", got
    # Без назви в історії — за номером з тегу: 3 → AFTERGLOW; 12 після 10 треків → BONUS 02.
    assert ap.story_position(story, "NEW NAME", "x.mp3", "3/13") == 2
    assert ap.story_position(story, "NEW NAME", "x.mp3", "12") == 11
    parts = [(f, 264.0, 20.0) for f in ordered[:3]] + [(ordered[12], 330.0, 20.0)]
    lines = ap.tracklist_lines(parts, titles, story, {})
    assert lines == ["TRACK 01 — IGNITION POINT — 4:24–4:44", "TRACK 02 — NEON LANES — 4:24–4:44",
                     "TRACK 03 — AFTERGLOW V2 — 4:24–4:44", "BONUS 03 — TWO VOICES, ONE ROAD. — 5:30–5:50"], lines
    text = ap.story_with_tracklist("Про що диск. …", lines, "uk")
    assert text.startswith("Про що диск. …\n\n" + ap.PREVIEW_NOTE["uk"] + "\nTRACK 01 — "), text
    assert ap.story_with_tracklist("", lines, "en") == ""
    print("order + tracklist PASS")


def test_end_to_end() -> None:
    tmp = Path(tempfile.mkdtemp())
    album_dir = tmp / "Music" / "Audio Converter" / "VOL. 99 — TEST PULSE" / "White Side"
    album_dir.mkdir(parents=True)
    # Імена файлів навмисно в «чужому» порядку: правильний порядок — з історії.
    make_track(album_dir / "BONUS TRACK 11 — LAST ONE.mp3", "LAST ONE", 30, freq=330)
    make_track(album_dir / "TRACK 01 — FIRST ONE.mp3", "FIRST ONE", 40, freq=220)
    make_track(album_dir / "TRACK 02 — SECOND ONE.mp3", "SECOND ONE", 55, freq=262)
    make_track(album_dir / "zz extra.mp3", "SOMETHING ELSE", 20, freq=392)

    offline = tmp / "drive"
    offline.mkdir()
    story = {"format": "faric-story-v1", "version": 3, "updated": "2026-10-23",
             "album_story": {"uk": "Про що диск. Тест.\n\nТрохи історії. Абзац два.", "en": "What it is about. Test."},
             "tracks": [{"n": 1, "bonus": False, "title": "FIRST ONE"},
                        {"n": 2, "bonus": False, "title": "SECOND ONE"},
                        {"n": 1, "bonus": True, "title": "LAST ONE"}]}
    (offline / "STORY99.json").write_text(json.dumps(story, ensure_ascii=False), encoding="utf-8")
    subprocess.run(["ffmpeg", "-nostdin", "-v", "error", "-y", "-f", "lavfi", "-i", "color=c=red:s=1600x1600",
                    "-frames:v", "1", str(offline / "DISC99.png")], check=True)
    cfg = {"albums": [{"vol": 99, "album": "VOL. 99 — TEST PULSE", "match": "TEST PULSE",
                       "story_file": "vol-99-test.json", "story_id": "STORY99", "story_version": 3,
                       "disc": {"white": "DISC99", "black": None}}]}
    cfg_path = tmp / "cfg.json"
    cfg_path.write_text(json.dumps(cfg, ensure_ascii=False), encoding="utf-8")

    os.environ["FARIC_PREVIEW_OFFLINE_DIR"] = str(offline)
    before = {p.name: p.stat().st_size for p in album_dir.iterdir()}
    rc = ap.main(["--folder", str(album_dir), "--config", str(cfg_path), "--yes"])
    assert rc == 0, rc
    outs = [p for p in album_dir.iterdir() if p.name.startswith("PREVIEW — ")]
    assert len(outs) == 1, outs
    out = outs[0]
    assert out.name == "PREVIEW — TEST PULSE — White Side.mp3", out.name
    for name, size in before.items():  # оригінали не змінені
        assert (album_dir / name).stat().st_size == size

    info = ffprobe_json(out)
    dur = float(info["format"]["duration"])
    assert abs(dur - (4 * 20 - 3 * 3)) < 1.5, dur
    tags = {k.upper(): v for k, v in info["format"]["tags"].items()}
    # V194: історія диска + у кінці список шматків (за порядком альбому, з хвилинами).
    assert tags["FARIC_ALBUM_STORY_UK"].startswith(story["album_story"]["uk"] + "\n\n" + ap.PREVIEW_NOTE["uk"]), tags["FARIC_ALBUM_STORY_UK"]
    assert tags["FARIC_ALBUM_STORY_EN"].startswith(story["album_story"]["en"] + "\n\n" + ap.PREVIEW_NOTE["en"])
    assert tags["FARIC_ALBUM_STORY_UK"].endswith(tags["FARIC_PREVIEW_TRACKS"]), tags["FARIC_PREVIEW_TRACKS"]
    assert tags["FARIC_PREVIEW_TRACKS"].startswith("TRACK 01 — "), tags["FARIC_PREVIEW_TRACKS"]
    assert "FARIC_STORY_UK" not in tags and "FARIC_STORY_EN" not in tags
    assert "PREVIEW" in tags["FARIC_STORY_SOURCE"]
    assert tags["ALBUM"] == "VOL. 99 — TEST PULSE — White Side"
    # спільні теги альбому — з пісні; пісенні (назва, номер, слова, історія пісні) — ні
    for k, v in {"ARTIST": "Faric(AI)", "ALBUM_ARTIST": "Faric", "COMPOSER": "Faric", "COPYRIGHT": "Saney",
                 "GENRE": "Trance", "DATE": "2026", "COMMENT": "for memory....",
                 "CONVERTER": "Audio Converter 0.29.0 · лише теги",
                 "ENCODER": "Track Catcher; Audio Converter 0.24.1; PulseDeck Preview"}.items():
        assert tags.get(k) == v, (k, tags.get(k))
    assert tags["TITLE"] == "PREVIEW — TEST PULSE — White Side" and tags["TRACK"] == "0", tags
    assert not any(k.startswith("LYRICS") for k in tags), tags.keys()
    order = [l.split(" — ")[1] for l in tags["FARIC_PREVIEW_TRACKS"].splitlines()]
    assert order == ["FIRST ONE", "SECOND ONE", "LAST ONE", "SOMETHING ELSE"], order
    # найгучніше місце: FIRST ONE гучна з 40 с; шматок починається на 3 с раніше (перехід — на наростанні)
    first_line = tags["FARIC_PREVIEW_TRACKS"].splitlines()[0]
    assert "0:37–0:57" in first_line, first_line
    pics = [s for s in info["streams"] if s["codec_type"] == "video"]
    assert pics and pics[0]["width"] == 1000 and pics[0]["height"] == 1000, pics
    assert pics[0].get("disposition", {}).get("attached_pic") == 1

    # повторний запуск не перезаписує, а робить «(2)»
    assert ap.main(["--folder", str(album_dir), "--config", str(cfg_path), "--yes"]) == 0
    assert (album_dir / "PREVIEW — TEST PULSE — White Side (2).mp3").exists()
    print(f"end-to-end PASS: {out.name}, {dur:.1f} s")


def test_real_config() -> None:
    cfg = json.loads((HERE / "album-preview-albums.json").read_text(encoding="utf-8"))
    vols = [a["vol"] for a in cfg["albums"]]
    assert vols == sorted(set(vols)), vols
    for a in cfg["albums"]:
        assert a["album"].startswith(f"VOL. {a['vol']} — ") and a["match"] in a["album"], a
        assert (a["story_id"] is None) == (a["story_file"] is None), a
    print("config PASS")


def _alpha_at(png: Path, size: int = 1000) -> bytes:
    return subprocess.run(["ffmpeg", "-nostdin", "-v", "error", "-i", str(png), "-vf", "format=rgba,alphaextract",
                           "-f", "rawvideo", "-pix_fmt", "gray", "-"], capture_output=True, check=True).stdout


def test_disc_cover() -> None:
    """Кругла наклейка з «рваним» краєм і рваною діркою → рівне коло, рівна дірка, прозорий фон."""
    import math
    tmp = Path(tempfile.mkdtemp())
    t, d = "atan2(Y-800,X-800)", "hypot(X-800,Y-800)"
    variants = {
        # прозорий PNG, рваний край і дірка, під прозорістю — білий «мусор»
        "alpha": f"format=rgba,geq=r='if(lt({d},820),40+X/8,255)':g='if(lt({d},820),20+Y/9,255)':b='if(lt({d},820),160,255)'"
                 f":a='255*lt({d},760+18*sin(17*{t})+9*sin(41*{t}))*gt({d},115+14*sin(23*{t})+6*sin(57*{t}))'",
        # непрозорий PNG на чорному фоні
        "black": f"format=rgb24,geq=r='if(lt({d},760+18*sin(17*{t}))*gt({d},120+14*sin(23*{t})),60+X/8,0)'"
                 f":g='if(lt({d},760+18*sin(17*{t}))*gt({d},120+14*sin(23*{t})),40+Y/9,0)'"
                 f":b='if(lt({d},760+18*sin(17*{t}))*gt({d},120+14*sin(23*{t})),180,0)'",
    }
    for name, vf in variants.items():
        src, out = tmp / f"{name}.png", tmp / f"{name}_clean.png"
        subprocess.run(["ffmpeg", "-nostdin", "-v", "error", "-y", "-f", "lavfi", "-i", "color=c=white:s=1600x1600",
                        "-frames:v", "1", "-vf", vf, str(src)], check=True)
        assert ap.make_disc_cover(src, out), name
        a = _alpha_at(out)
        assert len(a) == 1000 * 1000, (name, len(a))
        at = lambda x, y: a[int(y) * 1000 + int(x)]
        assert at(3, 3) == 0 and at(996, 996) == 0 and at(500, 500) == 0, name     # кути й дірка прозорі
        for k in range(360):                                                       # кільце — суцільне
            c, s_ = math.cos(math.radians(k)), math.sin(math.radians(k))
            assert at(500 + 470 * c, 500 + 470 * s_) == 255, (name, k, "edge")
            assert at(500 + 300 * c, 500 + 300 * s_) == 255, (name, k, "body")
        for k in (45, 135, 225, 315):                                              # за колом — прозоро
            c, s_ = math.cos(math.radians(k)), math.sin(math.radians(k))
            assert at(500 + 520 * c, 500 + 520 * s_) == 0, (name, k, "outside")
        hole = [r for r in range(0, 400) if at(500 + r, 500) == 255][0]            # дірка кругла: однакова по всіх боках
        for k in range(0, 360, 15):
            c, s_ = math.cos(math.radians(k)), math.sin(math.radians(k))
            assert at(500 + (hole + 3) * c, 500 + (hole + 3) * s_) == 255, (name, k, "hole edge")
            assert at(500 + (hole - 3) * c, 500 + (hole - 3) * s_) < 255, (name, k, "hole inside")
        print(f"disc cover PASS ({name}): hole r={hole}px, {out.stat().st_size // 1024} KB")


if __name__ == "__main__":
    test_unit()
    test_order_and_tracklist()
    test_real_config()
    test_disc_cover()
    test_end_to_end()
    print("ALL PASS")
