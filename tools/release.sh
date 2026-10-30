#!/usr/bin/env bash
# Publishes a new payload release. Usage: tools/release.sh [versionName]
# Needs (all outside the repo): ~/.kolay/{keystore.properties,release.keystore,update_key.pem}, gh (logged in), JDK 17, Android SDK.
set -euo pipefail
cd "$(dirname "$0")/.."
KEYDIR="$HOME/.kolay"
for f in keystore.properties release.keystore update_key.pem; do [ -f "$KEYDIR/$f" ] || { echo "missing $KEYDIR/$f"; exit 1; }; done
command -v gh >/dev/null || { echo "gh not installed"; exit 1; }
[ "$(git rev-parse --abbrev-ref HEAD)" = main ] || { echo "release only from main"; exit 1; }
[ -z "$(git status --porcelain)" ] || { echo "working tree not clean; commit first"; exit 1; }

PROP=payload/version.properties
VC=$(( $(sed -n 's/^versionCode=//p' "$PROP") + 1 ))
CUR=$(sed -n 's/^versionName=//p' "$PROP")
VN="${1:-${CUR%.*}.$(( ${CUR##*.} + 1 ))}"   # default: bump the patch number (1.0.1 -> 1.0.2)
trap 'git checkout -- "$PROP"' ERR
printf 'versionCode=%s\nversionName=%s\n' "$VC" "$VN" > "$PROP"

GRADLE=./gradlew; [ -x "$GRADLE" ] || GRADLE=gradle
$GRADLE -q :payload:assembleRelease :shell:assembleRelease

OUT=build/release; rm -rf "$OUT"; mkdir -p "$OUT"
cp payload/build/outputs/apk/release/payload-release.apk "$OUT/payload.apk"
cp shell/build/outputs/apk/release/shell-release.apk "$OUT/Kolay.apk"

SHA=$(shasum -a 256 "$OUT/payload.apk" | cut -d' ' -f1)
SIZE=$(wc -c < "$OUT/payload.apk" | tr -d ' ')
SIG=$(openssl dgst -sha256 -sign "$KEYDIR/update_key.pem" "$OUT/payload.apk" | base64 | tr -d '\n')
# self-check: the signature must verify with the public key before anything is published
openssl ec -in "$KEYDIR/update_key.pem" -pubout -out "$OUT/pub.pem" 2>/dev/null
echo "$SIG" | base64 -d > "$OUT/sig.der"
openssl dgst -sha256 -verify "$OUT/pub.pem" -signature "$OUT/sig.der" "$OUT/payload.apk" >/dev/null || { echo "signature self-check failed"; exit 1; }
PUB=$(openssl ec -in "$KEYDIR/update_key.pem" -pubout -outform DER 2>/dev/null | base64 | tr -d '\n')
grep -q "$PUB" shell/src/main/java/app/kolay/shell/Config.java || { echo "signing key is not in Config.UPDATE_PUBLIC_KEYS"; exit 1; }
rm -f "$OUT/pub.pem" "$OUT/sig.der"

API=$(sed -n 's/.*app.kolay.api" android:value="\([0-9]*\)".*/\1/p' payload/src/main/AndroidManifest.xml)
REPO=$(git config remote.origin.url | sed -E 's#.*github.com[:/]##; s#\.git$##')
cat > "$OUT/manifest.json" <<JSON
{"versionCode":$VC,"versionName":"$VN","api":$API,"size":$SIZE,"sha256":"$SHA","signature":"$SIG","url":"https://github.com/$REPO/releases/download/v$VN/payload.apk"}
JSON

git add "$PROP"; git commit -q -m "Release payload $VN"
git tag "v$VN"; git push -q origin HEAD "v$VN"
gh release create "v$VN" "$OUT/payload.apk" "$OUT/manifest.json" "$OUT/Kolay.apk" --title "$VN" --notes "Payload $VN (versionCode $VC)" --latest
echo "released v$VN (versionCode $VC)"
