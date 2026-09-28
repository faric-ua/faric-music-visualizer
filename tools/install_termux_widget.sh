#!/data/data/com.termux/files/usr/bin/bash
set -eu

REPO="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
SHORTCUT_DIR="$HOME/.shortcuts"
SHORTCUT="$SHORTCUT_DIR/FARIC Visualizer"

mkdir -p "$SHORTCUT_DIR"
chmod 700 "$SHORTCUT_DIR"

cat > "$SHORTCUT" <<EOT
#!/data/data/com.termux/files/usr/bin/bash
exec bash "$REPO/scripts/termux-menu.sh"
EOT

chmod 700 "$SHORTCUT"

echo "Termux:Widget shortcut installed:"
echo "  FARIC Visualizer"
echo
echo "Press Refresh in Termux:Widget."
