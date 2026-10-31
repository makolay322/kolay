#!/usr/bin/env python3
"""Shell-update fields of manifest.json (see docs/UPDATE_PROTOCOL.md).
  manifest_shell.py add   MANIFEST APK KEY VERSION_CODE VERSION_NAME URL   sign APK with the update key and add the fields
  manifest_shell.py carry MANIFEST PREVIOUS_MANIFEST                       copy the fields from the previous release (payload-only releases)
The payload fields are never touched, so their signature stays valid. The private key is only passed to openssl, never printed."""
import base64, hashlib, json, os, subprocess, sys

FIELDS = ("shellVersionCode", "shellVersionName", "shellSize", "shellSha256", "shellSignature", "shellUrl")

def load(p):
    with open(p, encoding="utf-8") as f: return json.load(f)

def save(p, m):
    with open(p, "w", encoding="utf-8") as f: json.dump(m, f, separators=(",", ":"))

def main(a):
    if len(a) == 3 and a[0] == "carry":
        m = load(a[1])
        try: prev = load(a[2])
        except (OSError, ValueError): return
        if all(k in prev for k in FIELDS):
            m.update({k: prev[k] for k in FIELDS}); save(a[1], m)
    elif len(a) == 7 and a[0] == "add":
        _, mp, apk, key, vc, vn, url = a
        data = open(apk, "rb").read()
        sig = subprocess.run(["openssl", "dgst", "-sha256", "-sign", key], input=data, capture_output=True, check=True).stdout
        m = load(mp)
        m.update({"shellVersionCode": int(vc), "shellVersionName": vn, "shellSize": len(data),
                  "shellSha256": hashlib.sha256(data).hexdigest(), "shellSignature": base64.b64encode(sig).decode(), "shellUrl": url})
        save(mp, m)
    else:
        sys.exit(__doc__)

main(sys.argv[1:])
