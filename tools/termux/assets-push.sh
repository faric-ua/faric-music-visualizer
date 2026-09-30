#!/data/data/com.termux/files/usr/bin/bash
set -u

ASSET_DIR="$HOME/faric-music-visualizer-assets"

echo "========================================"
echo " FARIC Assets — зберегти у GitHub"
echo "========================================"
echo

if [ ! -d "$ASSET_DIR/.git" ]; then
  echo "FARIC assets ще не підключені."
  echo "Спочатку запусти пункт «Підключити / відновити FARIC assets»."
  exit 2
fi

cd "$ASSET_DIR" || exit 1

if git diff --quiet && git diff --cached --quiet && [ -z "$(git ls-files --others --exclude-standard)" ]; then
  echo "Немає локальних змін для збереження."
  exit 0
fi

echo "Буде збережено:"
echo
git status --short
echo
printf "Продовжити? [y/N]: "
read -r answer
case "$answer" in
  y|Y|yes|YES|так|Так|ТАК) ;;
  *) echo "Скасовано."; exit 0 ;;
esac

echo
printf "Опис commit (Enter = assets: update archive): "
read -r msg
[ -n "$msg" ] || msg="assets: update archive"

git add -A || exit 1

echo
echo "LFS:"
git lfs ls-files | tail -n 20 || true
echo

git commit -m "$msg" || exit 1
git push || exit 1

echo
echo "PASS: зміни FARIC assets збережені у GitHub."
