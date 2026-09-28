#!/data/data/com.termux/files/usr/bin/bash
set -eu

SECRET_ROOT="$HOME/.faric-secrets/music-visualizer-dev"
OUT_DIR="/storage/emulated/0/Documents/FARIC-Music-Visualizer/backups"

test -d "$SECRET_ROOT" || { echo "Не знайдено signer: $SECRET_ROOT"; exit 1; }
command -v openssl >/dev/null 2>&1 || { echo "Встанови openssl: pkg install openssl"; exit 1; }

mkdir -p "$OUT_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$OUT_DIR/music-visualizer-dev-signer-${STAMP}.tar.enc"

echo "OpenSSL попросить НОВИЙ backup password."
echo "Не надсилай цей пароль у чат."
echo

tar -C "$HOME" -cf - ".faric-secrets/music-visualizer-dev"   | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 -out "$OUT"

sha256sum "$OUT" > "$OUT.sha256"
sha256sum -c "$OUT.sha256"

echo
echo "PASS: backup створено:"
echo "  $OUT"
echo "  $OUT.sha256"
