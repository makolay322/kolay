# İşletme

## Anahtarlar
Hepsi **depo dışında**, `~/.kolay/` (izin 700). Aşağıdakileri **yedekle** (şifreli taşınabilir disk / parola yöneticisi).

| Dosya | Ne işe yarar | Kaybolursa |
|---|---|---|
| `release.keystore` + `keystore.properties` | APK imzası (kabuk ve modül) | Telefondaki uygulamanın üzerine yüklenemez; kaldırıp yeniden kurmak gerekir (veri/ayarlar gider). |
| `update_key.pem` | Modül güncellemelerinin ECDSA imzası | Mevcut kurulumlar güncelleme alamaz; yeni anahtarla yeni kabuk yüklenmeli. |

Yeni makinede: dosyaları aynı konuma kopyala, `chmod 700 ~/.kolay && chmod 600 ~/.kolay/*`, `keystore.properties` içindeki `storeFile` yolunu düzelt.

İlk üretim (sıfırdan):
```bash
mkdir -p ~/.kolay && chmod 700 ~/.kolay && cd ~/.kolay
openssl ecparam -name prime256v1 -genkey -noout -out update_key.pem
PW=$(openssl rand -hex 16)
keytool -genkeypair -keystore release.keystore -alias kolay -keyalg RSA -keysize 3072 -validity 36500 \
        -storepass "$PW" -keypass "$PW" -dname "CN=Kolay"
printf 'storeFile=%s/release.keystore\nstorePassword=%s\nkeyAlias=kolay\nkeyPassword=%s\n' "$HOME/.kolay" "$PW" "$PW" > keystore.properties
openssl ec -in update_key.pem -pubout -outform DER | base64 | tr -d '\n'   # → Config.UPDATE_PUBLIC_KEYS
```

## Modül sürümü yayınlama
```bash
git status            # temiz olmalı (değişiklikleri önce commit et)
tools/release.sh      # veya: tools/release.sh 1.1.0  (sürüm adı; verilmezse yama +1: 1.0.1 → 1.0.2)
```
Sürüm adı `1.0.x` biçimindedir, etiket `v1.0.x`; hızlı artmasın diye yalnızca yama sayısı büyür. Betik: dahili `versionCode`'u +1 yapar (cihazda gösterilmez) → modül + kabuk derler → `payload.apk`'yı SHA-256 ile özetler, ECDSA ile imzalar → `manifest.json` yazar →
sürüm commit'i + `v<vc>` etiketi push eder → `gh release create ... --latest`. Cihazlar en geç 6–12 saatte alır
(ya da Ayarlar → *Şimdi denetle*).

Yayından sonra: `curl -sL .../releases/latest/download/manifest.json` ile manifest'i kontrol et ve CHANGELOG'u güncelle.

## Kabuk sürümü yayınlama (nadir)
Kabuk, yalnızca sözleşme (`Host`) büyüdüğünde, yerel kütüphane/izin/manifest değiştiğinde veya imza anahtarı rotasyonunda değişir.
1. `shell/build.gradle` içinde `versionCode`/`versionName` artır (+ gerekiyorsa `Host.API`, `Config.SHELL_API`).
2. `tools/release.sh` (kabuk de aynı Release'e `Kolay.apk` olarak eklenir).
3. Kullanıcı (sen) `Kolay.apk`'yı elle yükler. Yeni modül daha yüksek `api` istiyorsa, eski kabuklu cihazlar "kabuk güncellemesi gerekli" uyarısı görür ve modülü almaz — **önce kabuğu dağıt, sonra o API'yi isteyen modülü yayınla.**

## Geri alma (kötü sürüm yayınlandı)
Cihazlar `versionCode` düşürmez; bu yüzden **düzeltme ileriye doğrudur**:
1. `git revert <kötü commit>` (veya eski kodu geri getir) → `tools/release.sh` → versionCode daha yüksek, kod eski/sağlam.
2. Kötü sürümü almış cihazlar: sürüm açılışta çöküyorsa kendiliğinden 2 oturumda kara listeye girer ve önceki sürüme düşer; çalışıp bozuk davranıyorsa yukarıdaki düzeltme sürümü iner.
3. İstersen kötü GitHub Release'ini sil (`gh release delete vN --yes`); `latest` bir öncekine döner. Ama cihaza inmiş kopyayı etkilemez, bu yüzden 1. adım şart.

## Anahtar rotasyonu (planlı)
1. Yeni ECDSA anahtarı üret; açık anahtarı `Config.UPDATE_PUBLIC_KEYS` dizisine **ekle** (eskisini silme).
2. Yeni kabuk yayınla, tüm cihazlara yükle (elle).
3. Cihazlar yeni kabuktayken `tools/release.sh`'ın anahtarını yenisine çevir (`update_key.pem`).
4. Bir sonraki kabuk sürümünde eski açık anahtarı diziden çıkar.

## Anahtar ele geçirilirse / kaybolursa (olay müdahalesi)
**Şüphe varsa güncellemeleri durdur:** GitHub hesabında 2FA'yı doğrula, yetkisiz Release var mı bak (`gh release list`), şüpheli olanları sil.
- `update_key.pem` ele geçirildi: saldırgan geçerli imzalı sahte modül üretebilir. Yeni anahtar üret, yalnızca yeni anahtarı içeren yeni kabuk yayınla (eskisini dizide **bırakma**) ve telefonlara elle yükle. Eski anahtara güvenen kabuk kalmayana dek risk sürer.
- `release.keystore` ele geçirildi: sahte "güncelleme" APK'sı aynı imzayla yüklenebilir. Yeni keystore + yeni `applicationId` ile yeni kurulum gerekir; eskiyi kaldır.
- Kayıp (çalınma yok): yukarıdaki "Kaybolursa" tablosu.
- API anahtarı sızdıysa: sağlayıcı konsolundan iptal et/yenile, telefonda Ayarlar'dan yenisini gir (depoyu etkilemez).

## Depo ayarları (GitHub)
- Hesapta **2FA zorunlu** olmalı; depo erişimi yalnızca sahibinde.
- `main` dalı için "force push yasak / silme yasak" koruması önerilir (Settings → Branches).
- Secret scanning ve push protection (public depoda varsayılan) açık kalsın.
- Release'leri yalnızca `tools/release.sh` üretmeli; elle yüklenen dosya imzasız olacağı için cihazlarca reddedilir.
