#!/usr/bin/env bash
# Publishes a new payload release. Usage: tools/release.sh [versionName]
# Runs in two places with the same logic:
#   - GitHub Actions (.github/workflows/release.yml): key from secret UPDATE_KEY_PEM; only payload.apk + manifest.json are published.
#   - Locally: key from ~/.kolay/update_key.pem; Kolay.apk (shell, signed with ~/.kolay/release.keystore) is attached too.
# Versions are stateless: the next versionCode/versionName derive from the latest published manifest (no commit-back to the repo).
set -euo pipefail
cd "$(dirname "$0")/.."
command -v gh >/dev/null || { echo "gh not installed"; exit 1; }
CI_MODE=${GITHUB_ACTIONS:-false}
KEYDIR="$HOME/.kolay"
TMP=$(mktemp -d); trap 'rm -rf "$TMP"; git checkout -q -- payload/version.properties 2>/dev/null || true' EXIT

if [ "$CI_MODE" = true ]; then
  [ -n "${UPDATE_KEY_PEM:-}" ] || { echo "secret UPDATE_KEY_PEM missing"; exit 1; }
  ( umask 077; printf '%s\n' "$UPDATE_KEY_PEM" > "$TMP/update_key.pem" )
  KEY="$TMP/update_key.pem"; SHA_HEAD=$GITHUB_SHA
else
  for f in keystore.properties release.keystore update_key.pem; do [ -f "$KEYDIR/$f" ] || { echo "missing $KEYDIR/$f"; exit 1; }; done
  [ "$(git rev-parse --abbrev-ref HEAD)" = main ] || { echo "release only from main"; exit 1; }
  [ -z "$(git status --porcelain)" ] || { echo "working tree not clean; commit first"; exit 1; }
  git fetch -q origin main
  [ "$(git rev-parse HEAD)" = "$(git rev-parse origin/main)" ] || { echo "push main first (HEAD must equal origin/main)"; exit 1; }
  KEY="$KEYDIR/update_key.pem"; SHA_HEAD=$(git rev-parse HEAD)
fi
REPO=$(git config remote.origin.url | sed -E 's#.*github.com[:/]##; s#\.git$##')

# --- version: one above the latest published manifest (fallback: the base in payload/version.properties) ---
PROP=payload/version.properties
VC=$(sed -n 's/^versionCode=//p' "$PROP"); CUR=$(sed -n 's/^versionName=//p' "$PROP")
if curl -fsSL "https://github.com/$REPO/releases/latest/download/manifest.json" -o "$TMP/latest.json" 2>/dev/null; then
  LVC=$(sed -n 's/.*"versionCode":\([0-9]*\).*/\1/p' "$TMP/latest.json"); LVN=$(sed -n 's/.*"versionName":"\([^"]*\)".*/\1/p' "$TMP/latest.json")
  [ -n "$LVC" ] && [ "$LVC" -ge "$VC" ] && { VC=$LVC; CUR=$LVN; }
fi
VC=$(( VC + 1 ))
VN="${1:-${CUR%.*}.$(( ${CUR##*.} + 1 ))}"   # default: bump the patch number (1.0.1 -> 1.0.2)
gh release view "v$VN" >/dev/null 2>&1 && { echo "release v$VN already exists"; exit 1; }
printf 'versionCode=%s\nversionName=%s\n' "$VC" "$VN" > "$PROP"

GRADLE=./gradlew; [ -x "$GRADLE" ] || GRADLE=gradle
OUT=build/release; rm -rf "$OUT"; mkdir -p "$OUT"
if [ "$CI_MODE" = true ]; then $GRADLE -q --no-daemon :payload:assembleRelease; else $GRADLE -q :payload:assembleRelease :shell:assembleRelease; fi
cp payload/build/outputs/apk/release/payload-release.apk "$OUT/payload.apk"
[ "$CI_MODE" = true ] || cp shell/build/outputs/apk/release/shell-release.apk "$OUT/Kolay.apk"

SHA=$(shasum -a 256 "$OUT/payload.apk" | cut -d' ' -f1)
SIZE=$(wc -c < "$OUT/payload.apk" | tr -d ' ')
SIG=$(openssl dgst -sha256 -sign "$KEY" "$OUT/payload.apk" | base64 | tr -d '\n')
# self-check: the signature must verify with the public key before anything is published
openssl ec -in "$KEY" -pubout -out "$TMP/pub.pem" 2>/dev/null
echo "$SIG" | base64 -d > "$TMP/sig.der"
openssl dgst -sha256 -verify "$TMP/pub.pem" -signature "$TMP/sig.der" "$OUT/payload.apk" >/dev/null || { echo "signature self-check failed"; exit 1; }
PUB=$(openssl ec -in "$KEY" -pubout -outform DER 2>/dev/null | base64 | tr -d '\n')
grep -q "$PUB" shell/src/main/java/app/kolay/shell/Config.java || { echo "signing key is not in Config.UPDATE_PUBLIC_KEYS"; exit 1; }

API=$(sed -n 's/.*app.kolay.api" android:value="\([0-9]*\)".*/\1/p' payload/src/main/AndroidManifest.xml)
cat > "$OUT/manifest.json" <<JSON
{"versionCode":$VC,"versionName":"$VN","api":$API,"size":$SIZE,"sha256":"$SHA","signature":"$SIG","url":"https://github.com/$REPO/releases/download/v$VN/payload.apk"}
JSON

FILES=("$OUT/payload.apk" "$OUT/manifest.json"); [ "$CI_MODE" = true ] || FILES+=("$OUT/Kolay.apk")
gh release create "v$VN" "${FILES[@]}" --target "$SHA_HEAD" --title "$VN" --notes "Payload $VN (versionCode $VC) — commit ${SHA_HEAD:0:7}" --latest
echo "released v$VN (versionCode $VC)"
