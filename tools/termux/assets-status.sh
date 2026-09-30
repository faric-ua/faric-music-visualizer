#!/data/data/com.termux/files/usr/bin/bash
set -u

ASSET_DIR="$HOME/faric-music-visualizer-assets"

echo "========================================"
echo " FARIC Assets — статус"
echo "========================================"
echo
echo "Очікувана папка:"
echo "  $ASSET_DIR"
echo

if [ ! -d "$ASSET_DIR/.git" ]; then
  echo "STATUS: ще не підключено."
  echo "Запусти пункт «Підключити / відновити FARIC assets»."
  exit 0
fi

cd "$ASSET_DIR" || exit 1

git status -sb
echo
echo "Remote:"
git remote -v
echo
echo "LFS master-файлів:"
git lfs ls-files | wc -l | tr -d ' '
echo
echo "Фізичних master image-файлів:"
find masters -type f \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.webp' \) 2>/dev/null | wc -l | tr -d ' '
echo
echo "Розмір локальної папки:"
du -sh "$ASSET_DIR" 2>/dev/null | awk '{print $1}'
echo

POINTERS=0
while IFS= read -r -d '' f; do
  if head -n 1 "$f" 2>/dev/null | grep -q '^version https://git-lfs.github.com/spec/v1$'; then
    POINTERS=$((POINTERS + 1))
  fi
done < <(find masters -type f \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.webp' \) -print0 2>/dev/null)

if [ "$POINTERS" -eq 0 ]; then
  echo "LFS materialization: PASS — локально лежать реальні файли."
else
  echo "LFS materialization: WARNING — pointer-файлів: $POINTERS"
fi
