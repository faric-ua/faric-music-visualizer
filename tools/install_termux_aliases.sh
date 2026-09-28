#!/data/data/com.termux/files/usr/bin/bash
set -e

REPO="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
RC_FILE="$HOME/.bashrc"

case "${SHELL:-}" in
  */zsh) RC_FILE="$HOME/.zshrc" ;;
esac

touch "$RC_FILE"

add_alias() {
  NAME="$1"
  COMMAND="$2"
  sed -i "/^alias ${NAME}=/d" "$RC_FILE"
  printf "\nalias %s='%s'\n" "$NAME" "$COMMAND" >> "$RC_FILE"
}

add_alias "visualizer-code" "cd \"$REPO\""
add_alias "visualizer-menu" "bash \"$REPO/scripts/termux-menu.sh\""
add_alias "vis" "bash \"$REPO/scripts/termux-menu.sh\""

echo "Configured:"
echo "  visualizer-code"
echo "  visualizer-menu"
echo "  vis"
echo
echo "Activate now:"
echo "  source \"$RC_FILE\""
