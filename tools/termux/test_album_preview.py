#!/usr/bin/env python3
"""V143: перевірка «Прев'ю альбому» без телефона й без інтернету (синтетичні пісні)."""
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
                    "-metadata", "comment=for memory....", str(path)], check=True)


def test_unit() -> None:
    assert ap.norm_title("BONUS TRACK 11 — TEN ROADS, ONE HEART.mp3") == ap.norm_title("TEN ROADS, ONE HEART")
    assert ap.norm_title("TRACK 01 — IGNITION POINT (2)") == ap.norm_title("IGNITION POINT")
    e = [1.0] * 200
    for i in range(100, 140):
        e[i] = 50.0
    assert abs(ap.loudest_start(e, 100.0, 20.0) - 50.0) < 0.6, ap.loudest_start(e, 100.0, 20.0)
    assert ap.loudest_start(e, 22.0, 20.0) == 0.0
    print("unit PASS")


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
    outs = [p for p in album_dir.iterdir() if p.name.startswith("00 — PREVIEW")]
    assert len(outs) == 1, outs
    out = outs[0]
    assert out.name == "00 — PREVIEW — TEST PULSE (White Side).mp3", out.name
    for name, size in before.items():  # оригінали не змінені
        assert (album_dir / name).stat().st_size == size

    info = ffprobe_json(out)
    dur = float(info["format"]["duration"])
    assert abs(dur - (4 * 20 - 3 * 3)) < 1.5, dur
    tags = {k.upper(): v for k, v in info["format"]["tags"].items()}
    assert tags["FARIC_ALBUM_STORY_UK"] == story["album_story"]["uk"], tags["FARIC_ALBUM_STORY_UK"]
    assert tags["FARIC_ALBUM_STORY_EN"] == story["album_story"]["en"]
    assert "FARIC_STORY_UK" not in tags and "FARIC_STORY_EN" not in tags
    assert "PREVIEW" in tags["FARIC_STORY_SOURCE"]
    assert tags["ALBUM"] == "VOL. 99 — TEST PULSE — White Side"
    order = [l.split(". ", 1)[1].split(" — ")[0] for l in tags["FARIC_PREVIEW_TRACKS"].splitlines()]
    assert order == ["FIRST ONE", "SECOND ONE", "LAST ONE", "SOMETHING ELSE"], order
    # найгучніше місце: FIRST ONE гучна з 40 с; шматок починається на 3 с раніше (перехід — на наростанні)
    first_line = tags["FARIC_PREVIEW_TRACKS"].splitlines()[0]
    assert "0:37–0:57" in first_line, first_line
    pics = [s for s in info["streams"] if s["codec_type"] == "video"]
    assert pics and pics[0]["width"] == 1000 and pics[0]["height"] == 1000, pics
    assert pics[0].get("disposition", {}).get("attached_pic") == 1

    # повторний запуск не перезаписує, а робить «(2)»
    assert ap.main(["--folder", str(album_dir), "--config", str(cfg_path), "--yes"]) == 0
    assert (album_dir / "00 — PREVIEW — TEST PULSE (White Side) (2).mp3").exists()
    print(f"end-to-end PASS: {out.name}, {dur:.1f} s")


def test_real_config() -> None:
    cfg = json.loads((HERE / "album-preview-albums.json").read_text(encoding="utf-8"))
    vols = [a["vol"] for a in cfg["albums"]]
    assert vols == sorted(set(vols)), vols
    for a in cfg["albums"]:
        assert a["album"].startswith(f"VOL. {a['vol']} — ") and a["match"] in a["album"], a
        assert (a["story_id"] is None) == (a["story_file"] is None), a
    print("config PASS")


if __name__ == "__main__":
    test_unit()
    test_real_config()
    test_end_to_end()
    print("ALL PASS")
