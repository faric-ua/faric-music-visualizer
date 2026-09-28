#!/data/data/com.termux/files/usr/bin/bash
set -eu

SECRET_ROOT="$HOME/.faric-secrets/music-visualizer-dev"
KEYSTORE="$SECRET_ROOT/faric-music-visualizer-dev.jks"
CERT_DER="$SECRET_ROOT/faric-music-visualizer-dev.cer"
SECRETS_FILE="$SECRET_ROOT/github-secrets.txt"
LOCAL_ENV_FILE="$SECRET_ROOT/local-build-env.sh"
ALIAS="faric-music-visualizer-dev"

if ! command -v keytool >/dev/null 2>&1; then
  echo "keytool не знайдено. Встанови: pkg install openjdk-17"
  exit 1
fi

if [ -e "$SECRET_ROOT" ]; then
  echo "Каталог уже існує: $SECRET_ROOT"
  echo "Signer не буде перезаписаний."
  exit 1
fi

mkdir -p "$SECRET_ROOT"
chmod 700 "$SECRET_ROOT"
PASSWORD="$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')"

keytool -genkeypair   -keystore "$KEYSTORE"   -storetype JKS   -storepass "$PASSWORD"   -keypass "$PASSWORD"   -alias "$ALIAS"   -keyalg RSA   -keysize 3072   -validity 3650   -dname "CN=FARIC Music Visualizer Development, OU=Development, O=faric-ua, C=UA"   >/dev/null 2>&1

keytool -exportcert   -keystore "$KEYSTORE"   -storepass "$PASSWORD"   -alias "$ALIAS"   -file "$CERT_DER"   >/dev/null 2>&1

CERT_SHA256="$(sha256sum "$CERT_DER" | awk '{print $1}')"
KEYSTORE_B64="$(base64 "$KEYSTORE" | tr -d '\n')"

cat > "$SECRETS_FILE" <<EOF
VISUALIZER_DEV_KEYSTORE_B64=$KEYSTORE_B64
VISUALIZER_DEV_STORE_PASSWORD=$PASSWORD
VISUALIZER_DEV_KEY_ALIAS=$ALIAS
VISUALIZER_DEV_KEY_PASSWORD=$PASSWORD
VISUALIZER_DEV_CERT_SHA256=$CERT_SHA256
EOF

cat > "$LOCAL_ENV_FILE" <<EOF
export VISUALIZER_DEV_KEYSTORE_PATH='$KEYSTORE'
export VISUALIZER_DEV_STORE_PASSWORD='$PASSWORD'
export VISUALIZER_DEV_KEY_ALIAS='$ALIAS'
export VISUALIZER_DEV_KEY_PASSWORD='$PASSWORD'
export VISUALIZER_DEV_CERT_SHA256='$CERT_SHA256'
EOF

chmod 600 "$KEYSTORE" "$CERT_DER" "$SECRETS_FILE" "$LOCAL_ENV_FILE"

echo
echo "PASS: окремий development signer створено."
echo "Keystore: $KEYSTORE"
echo "GitHub secrets file: $SECRETS_FILE"
echo "Local build env: $LOCAL_ENV_FILE"
echo "Certificate SHA-256: $CERT_SHA256"
echo
echo "Не копіюй ці файли в Git і не надсилай їх у чат."
