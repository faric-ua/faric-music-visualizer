#!/data/data/com.termux/files/usr/bin/bash
set -eu

REPO="${1:-faric-ua/faric-music-visualizer}"
SECRETS_FILE="${2:-$HOME/.faric-secrets/music-visualizer-dev/github-secrets.txt}"
VERIFY_REF="${3:-main}"

gh auth status -h github.com >/dev/null 2>&1 || {
  echo "GitHub CLI не авторизований."
  exit 1
}

test -f "$SECRETS_FILE" || {
  echo "Не знайдено: $SECRETS_FILE"
  exit 1
}

EXPECTED_KEYS="
VISUALIZER_DEV_KEYSTORE_B64
VISUALIZER_DEV_STORE_PASSWORD
VISUALIZER_DEV_KEY_ALIAS
VISUALIZER_DEV_KEY_PASSWORD
VISUALIZER_DEV_CERT_SHA256
"

for key in $EXPECTED_KEYS; do
  line="$(grep -m1 "^${key}=" "$SECRETS_FILE" || true)"
  test -n "$line" || { echo "Відсутнє значення: $key"; exit 1; }
  value="${line#*=}"
  test -n "$value" || { echo "Порожнє значення: $key"; exit 1; }
  printf "%s" "$value" | gh secret set "$key" --repo "$REPO"
  echo "OK: $key"
done

echo
echo "Імена secrets:"
gh secret list --repo "$REPO" | grep -E "^VISUALIZER_DEV_" || true

echo
echo "Запускаю Android build: $VERIFY_REF"
gh workflow run android.yml --repo "$REPO" --ref "$VERIFY_REF"
echo "PASS: secrets передані GitHub без виведення значень."
