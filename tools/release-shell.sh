#!/usr/bin/env bash
# Attaches the current shell APK (Kolay.apk) to the LATEST release and adds its signed entry to that release's manifest.json,
# so installed apps can offer "Kabuğu indir ve yükle". Run locally (needs ~/.kolay keys) after the payload release exists,
# e.g. when the automatic release (CI) was first and could not carry the shell. Payload fields of the manifest stay untouched.
set -euo pipefail
cd "$(dirname "$0")/.."
KEYDIR="$HOME/.kolay"
for f in keystore.properties release.keystore update_key.pem; do [ -f "$KEYDIR/$f" ] || { echo "missing $KEYDIR/$f"; exit 1; }; done
[ "$(git rev-parse --abbrev-ref HEAD)" = main ] || { echo "run from main"; exit 1; }
[ -z "$(git status --porcelain)" ] || { echo "working tree not clean"; exit 1; }
git fetch -q origin main
[ "$(git rev-parse HEAD)" = "$(git rev-parse origin/main)" ] || { echo "HEAD must equal origin/main"; exit 1; }
TAG=$(gh release view --json tagName -q .tagName)
REPO=$(git config remote.origin.url | sed -E 's#.*github.com[:/]##; s#\.git$##')
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
gh release download "$TAG" -p manifest.json -D "$TMP"
./gradlew -q :shell:assembleRelease
cp shell/build/outputs/apk/release/shell-release.apk "$TMP/Kolay.apk"
SVC=$(sed -n "s/.*versionCode \([0-9]*\).*/\1/p" shell/build.gradle | head -1)
SVN=$(sed -n "s/.*versionName '\([^']*\)'.*/\1/p" shell/build.gradle | head -1)
tools/manifest_shell.py add "$TMP/manifest.json" "$TMP/Kolay.apk" "$KEYDIR/update_key.pem" "$SVC" "$SVN" "https://github.com/$REPO/releases/download/$TAG/Kolay.apk"
gh release upload "$TAG" "$TMP/Kolay.apk" "$TMP/manifest.json" --clobber
echo "shell $SVN (versionCode $SVC) attached to $TAG"
