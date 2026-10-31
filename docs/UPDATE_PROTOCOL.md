# Güncelleme protokolü

## Yayın içeriği
Her GitHub Release (etiket `v<versionName>`, örn. `v1.0.1`) üç dosya taşır:

| Dosya | Amaç |
|---|---|
| `payload.apk` | Modül (kod konteyneri), `app.kolay.payload` |
| `manifest.json` | Cihazın okuduğu açıklama + imza |
| `Kolay.apk` | O sürümü gömülü taşıyan kabuk (ilk kurulum / kabuk güncellemesi için) |

Cihaz yalnızca şu sabit adresi sorgular (GitHub "latest" yönlendirmesi, API anahtarı/kota gerekmez):
`https://github.com/makolay322/kolay/releases/latest/download/manifest.json`

## `manifest.json`
```json
{
  "versionCode": 4,
  "versionName": "1.0.1",
  "api": 1,
  "size": 38236,
  "sha256": "<payload.apk SHA-256, küçük harf hex>",
  "signature": "<base64(DER ECDSA-SHA256 imza), payload.apk baytları üzerinde>",
  "url": "https://github.com/makolay322/kolay/releases/download/v1.0.1/payload.apk"
}
```
- İsteğe bağlı kabuk alanları (hepsi birlikte): `shellVersionCode`, `shellVersionName`, `shellSize`, `shellSha256`, `shellSignature` (aynı güncelleme anahtarıyla, `Kolay.apk` baytları üzerinde), `shellUrl` (`REPO_BASE + "releases/download/"` ile başlamalı). Yalnızca yerelde (keystore'lu makinede) eklenir: `tools/release.sh` yerel modu veya `tools/release-shell.sh`. Yalnızca-modül (CI) sürümleri önceki manifestten **aynen taşır** (`tools/manifest_shell.py carry`).
- `signature` **dosya baytlarını** imzalar (manifest alanlarını değil). `versionCode` APK'nın kendi içinde olduğundan imzalı veriye dahildir.
- `api`, modülün `AndroidManifest.xml`'indeki `app.kolay.api` ile aynı olmalı (cihaz çapraz kontrol eder).

## Cihaz tarafı adımlar (`Updater.run`)
1. Manifest'i indir (≤64 KB). Alanları oku.
2. `url`, `Config.REPO_BASE + "releases/download/"` ile başlamalı; yönlendirmeden sonra son sunucu `github.com` veya `*.githubusercontent.com` ve HTTPS olmalı.
3. `size` ≤ 40 MB. `api > SHELL_API` ise indirme; "kabuk güncellemesi gerekli" durumu kaydet.
4. `versionCode ≤ highestKnown()` ise "güncel" de.
5. Geçici dosyaya indir (beklenen boyutu aşarsa kes). Sonra sırayla:
   SHA-256 eşleşmesi → ECDSA imzası (`Config.UPDATE_PUBLIC_KEYS` içindeki **herhangi** bir anahtarla) →
   `getPackageArchiveInfo` ile paket adı, `versionCode`, `app.kolay.api` manifestle uyumlu mu.
6. `files/payloads/<vc>/` altına yaz, **salt-okunur** yap, eskileri buda, dinleyiciye ("Yeni sürüm hazır") haber ver.

Herhangi bir adım başarısızsa dosya atılır, çalışan sürüm etkilenmez, 30 dk sonra tekrar denenir (başarıda 6 saat).

## Yükleme anında yeniden doğrulama
`Loader.load` indirilmiş modülde **her açılışta** imzayı tekrar doğrular (bozulmaya/kurcalamaya karşı ek katman) ve paket adı/sürüm/api'yi kontrol eder.
Gömülü modül kabuk APK'sının içinde olduğu için (Android imzasıyla korunur) ayrıca imza istemez.

## Anahtarlar
- **Güncelleme imzası:** ECDSA P-256. Özel anahtar `~/.kolay/update_key.pem` (depo dışı). Açık anahtarlar `Config.UPDATE_PUBLIC_KEYS` dizisinde (X.509 DER, base64).
- **APK imzası (Android):** `~/.kolay/release.keystore`. Kabuğun üzerine yükleme (güncelleme) için aynı anahtar gerekir.
- Rotasyon ve kaza durumları: [OPERATIONS.md](OPERATIONS.md).

## İmza komutu (referans)
```bash
openssl dgst -sha256 -sign ~/.kolay/update_key.pem payload.apk | base64 | tr -d '\n'
```
Açık anahtar üretimi: `openssl ec -in ~/.kolay/update_key.pem -pubout -outform DER | base64 | tr -d '\n'`.

## Kabuk güncellemesi (cihaz tarafı, `ShellInstaller`)
Kullanıcı Ayarlar'da “Kabuğu indir ve yükle”ye dokununca (düğme, manifestte `shellVersionCode` > yüklü kabuk olduğunda görünür):
1. `canRequestPackageInstalls()` yoksa “bilinmeyen kaynaklardan yükleme” ayarı açılır; kullanıcı izin verip tekrar dokunur.
2. Manifest yeniden alınır; `shellUrl` ön eki, boyut (≤120 MB), sürüm kontrol edilir; indirme `Updater.download` ile (yalnızca GitHub, HTTPS).
3. SHA-256 → ECDSA imzası → `getPackageArchiveInfo`: paket adı `app.kolay`, `versionCode` manifestle aynı, imza sertifikası yüklü kabukla aynı.
4. `PackageInstaller` oturumu açılır; `InstallResultReceiver` sistem onay ekranını gösterir. **Onayı Android ister; sessiz yükleme yoktur.** Hata `Registry` durumuna yazılır.
