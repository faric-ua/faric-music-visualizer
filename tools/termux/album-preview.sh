#!/data/data/com.termux/files/usr/bin/bash
# V143 — «Прев'ю альбому»: по ~20 с найсильнішого місця кожної пісні з плавними переходами,
# кругла наклейка диска як обкладинка, у тегах — історія альбому. Логіка — album_preview.py.
set -u
HERE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

echo "ПРЕВ'Ю АЛЬБОМУ"
echo

if ! command -v ffmpeg >/dev/null 2>&1 || ! command -v ffprobe >/dev/null 2>&1; then
  echo "Потрібна програма ffmpeg — ставлю (один раз, ~1–2 хв)…"
  pkg install -y ffmpeg || { echo "Не вдалося поставити ffmpeg."; exit 1; }
fi
if ! command -v python3 >/dev/null 2>&1; then
  echo "Потрібен Python — ставлю (один раз)…"
  pkg install -y python || { echo "Не вдалося поставити Python."; exit 1; }
fi

if [ ! -r /storage/emulated/0/Music ]; then
  echo "Термуксу потрібен доступ до пам'яті телефона."
  echo "Зараз з'явиться запит — натисни «Дозволити»."
  termux-setup-storage
  sleep 3
fi

python3 "$HERE/album_preview.py" "$@"
