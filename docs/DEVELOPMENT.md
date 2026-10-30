# Geliştirme

## Gereksinimler
- JDK 17 (`JAVA_HOME`), Android SDK (platform 34, build-tools 34+) ve kökte `local.properties` → `sdk.dir=/yol/android-sdk`
- `gh` (GitHub CLI, giriş yapılmış) — yalnızca sürüm yayınlamak için
- `~/.kolay/` altında imza anahtarları (aşağıda). **Depoda yoktur.**

## İlk kurulum (yeni makine)
```bash
git clone https://github.com/makolay322/kolay && cd kolay
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :shell:assembleRelease        # ilk seferde Gradle ve bağımlılıkları indirir
```
Anahtar yoksa derleme **debug anahtarıyla** imzalar ve çalışır; ama bu APK'lar üretimdeki kurulumun üzerine yüklenemez ve
güncelleme imzası üretemez. Sürüm yayınlamak için anahtarlar gerekir ([OPERATIONS.md](OPERATIONS.md#anahtarlar)).

## Yapı
| Komut | Sonuç |
|---|---|
| `./gradlew :payload:assembleRelease` | `payload/build/outputs/apk/release/payload-release.apk` |
| `./gradlew :shell:assembleRelease` | `shell/build/outputs/apk/release/shell-release.apk` (modülü `assets/`'e gömer) |

`shell`, `bundlePayload` görevi aracılığıyla önce `payload`'ı derler ve çıktıyı `assets/payload.apk` olarak paketler.
Sürüm bilgisi: modül için `payload/version.properties` (`tools/release.sh` artırır), kabuk için `shell/build.gradle` (`versionCode`/`versionName`, elle).

## Telefona yükleme
- İlk kurulum: `shell-release.apk` (veya Release'teki `Kolay.apk`) telefona kopyalanıp elle yüklenir.
  (HyperOS/MIUI'de `adb install` için "USB üzerinden yükle" geliştirici seçeneği açık olmalı.)
- Sonrası otomatik. Hızlı deneme: modül değiştir → `tools/release.sh` → telefonda Ayarlar → *Şimdi denetle* → *Şimdi yenile*.

## Test
Emülatör/otomatik arayüz testi yoktur; cihazda elle denenir. Masaüstü JVM'de denenebilen saf Java parçalar:
- `shell/.../Verify.java` (SHA-256, ECDSA) — `openssl dgst -sign` ile üretilmiş imzayla doğrulama
- `payload/.../Mask.java` (isim maskeleme) — **uydurma** örneklerle; gerçek belge/isim kullanma

Cihazda kontrol listesi (sürüm öncesi):
1. Temiz kurulum → ana sayfa animasyonla açılıyor, Ayarlar'da sürüm bilgisi doğru.
2. Anahtarı kaydet → Tıbbi OCR → 2–3 fotoğraf seç (karışık sıra) → sonuç, sıra doğru, `[NAME]` görünüyor.
3. Kopyala / Paylaş çalışıyor; uygulamayı kapat-aç → sonuç diske yazılmamış.
4. Ayarlar → *Şimdi denetle* → durum metni mantıklı.
5. Koyu/açık tema geçişi arayüzü bozmuyor; geri tuşu her ekranda beklenen yere dönüyor.

## Sorun giderme
| Belirti | Neden / çözüm |
|---|---|
| `Kolay açılamadı` ekranı | Gömülü modül bile yüklenemedi: sürüm sayfasından kabuğu yeniden yükle. Logcat etiketi `Kolay`. |
| Güncelleme "imza doğrulanamadı" | Release'i farklı anahtarla imzaladın ya da `Config.UPDATE_PUBLIC_KEYS` eski. Anahtarları karşılaştır. |
| "kabuk güncellemesi gerekli" | Yeni modül daha yüksek `app.kolay.api` istiyor: yeni `Kolay.apk`'yı elle yükle. |
| Yeni sürüm gelmiyor | Önceki sürüm kara listeye girmiş olabilir; `versionCode`'u artırıp yeniden yayınla. 6 saat bekleme: Ayarlar'dan zorla denetle. |
| `Gradle verification` hatası | Bağımlılık değişti: `./gradlew --write-verification-metadata sha256 :shell:assembleRelease`, farkı incele. |
| Derleme `Unable to locate a Java Runtime` | `JAVA_HOME` JDK 17'ye işaret etmeli. |
