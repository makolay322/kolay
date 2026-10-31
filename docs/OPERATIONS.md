# İşletme

## Bağımsız çalışma (bilgisayara bağlı olmadan)
Yayın hattı tamamen GitHub'dadır; hiçbir makine gerekmez:
- **Düzenle:** github.com'da dosyayı aç → kalem, ya da `.` tuşu (web editör) / Codespaces. Yerel kurulum gerekmez.
- **Yayınla:** `main`'e commit/merge. `payload/**`, `shell/**`, `sdk/**` veya `gradle/**` değiştiyse `release.yml` derler, imzalar, `vX` Release'ini ve `Kolay.apk` + `payload.apk` + `manifest.json`'u yayınlar. Sürüm numarası son manifestten türetilir, depoya geri yazılmaz. Elle tetik: Actions → *release* → *Run workflow*.
- **Doğrula:** Actions'ta `build` ve `release` yeşil olmalı; telefonda Ayarlar → *Şimdi denetle*.
- **Eski Release temizliği:** Actions → *purge-releases* (elle).
- **Gereken Secret'lar:** `UPDATE_KEY_PEM`, `KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (Settings → Secrets and variables → Actions). Eksikse `release` işi uyarı verir ya da kabuksuz çıkar.
- **Yerelde ne kaldı:** yalnızca anahtar yedeği (`~/.kolay`, parola yöneticisi/şifreli disk) ve APK'yı telefona ilk kez kurmak. Yerel `tools/release.sh` / `release-shell.sh` acil durum yedeğidir.
- **Dikkat:** Release/anahtar iş akışlarını (`.github/workflows/`) ve `tools/release.sh`'ı değiştiren her PR'ı dikkatle incele; Secret'ları bunlar kullanır. `main` dalını korumalı tut (zorunlu PR, 2FA).

## Anahtarlar
Asıl kaynak **depo dışında**, `~/.kolay/` (izin 700); CI için kopyaları GitHub Secrets'ta durur. **GitHub Secrets yalnızca yazılır, geri okunamaz**: yerel dosyalar yedeksiz kaybolursa tek kopya CI'da kalır ve elinize geri gelmez. Aşağıdakileri **yedekle** (şifreli taşınabilir disk / parola yöneticisi).

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
**Otomatik (varsayılan):** `main`'e `payload/**` veya `sdk/**` (ya da Gradle dosyaları) değişikliği push et → `.github/workflows/release.yml`
sırayla: gizli anahtar taraması → modülü derler → `UPDATE_KEY_PEM` ile imzalar → imzayı yayından önce doğrular → `v1.0.x` Release'i (`payload.apk` + `manifest.json`) açar.
Sürüm **durumsuzdur**: bir sonraki `versionCode`/`versionName`, son yayınlanan manifest'ten türetilir (yama +1; `1.0.1 → 1.0.2`), depoya commit geri yazılmaz.
Yalnızca doküman/kabuk değişiklikleri sürüm üretmez. Zorla: GitHub → Actions → *release* → *Run workflow* (veya `gh workflow run release`).

**Elle (yedek):** `main` push edilmiş ve ağaç temizken
```bash
tools/release.sh      # veya: tools/release.sh 1.1.0  (sürüm adı; verilmezse yama +1)
```
Aynı betik yerelde `~/.kolay` anahtarlarını kullanır ve ayrıca keystore ile imzalı `Kolay.apk`'yı Release'e ekler. Aynı anda ikisi çalışırsa ikinci "release vX already exists" der; sorun değil.

Sürüm adı `1.NNN` biçimindedir (örn. 1.002), etiket `v1.NNN`. Cihazlar en geç 6–12 saatte alır (ya da Ayarlar → *Şimdi denetle*).
Yayından sonra: `curl -sL .../releases/latest/download/manifest.json` ile kontrol et ve CHANGELOG'u güncelle.

**Secret kurulumu / yenileme** (bir kez; değer ekrana yazılmaz):
```bash
gh secret set UPDATE_KEY_PEM < ~/.kolay/update_key.pem
# Kabuk imzası (keystore) için:
base64 -i ~/.kolay/release.keystore | gh secret set KEYSTORE_B64
# storePassword, keyAlias, keyPassword değerleri ~/.kolay/keystore.properties içindedir:
gh secret set KEYSTORE_PASSWORD   # KEY_ALIAS ve KEY_PASSWORD için de aynısı
```
Keystore Secret'ları 2026-10-31'de yüklendi; `release.yml` bunları kullanıp kabuğu CI'da imzalar (anahtar geçici dosyaya açılır, iş sonunda silinir).
Anahtar rotasyonunda yeni anahtarı aynı komutla yükle (bkz. aşağıdaki rotasyon adımları).

## Kabuk sürümü yayınlama (nadir)
**Otomatik:** `KEYSTORE_B64` (release.keystore base64) ile birlikte `KEYSTORE_PROPERTIES` (keystore.properties içeriği) **veya** `KEYSTORE_PASSWORD` + `KEY_ALIAS` + `KEY_PASSWORD` Secret'ları tanımlıysa `release.yml` kabuğu da derler, imzalar, `Kolay.apk`'yı Release'e ekler ve manifestine yazar. Tanımlı değilse yalnızca modül çıkar, manifestteki kabuk alanları önceki sürümden taşınır.
**Elle (yedek):**
Kabuk, yalnızca sözleşme (`Host`) büyüdüğünde, yerel kütüphane/izin/manifest değiştiğinde veya imza anahtarı rotasyonunda değişir.
1. `shell/build.gradle` içinde `versionCode`/`versionName` artır (+ gerekiyorsa `Host.API`, `Config.SHELL_API`).
2. `main`'e push et: `shell/**` değişikliği `release.yml`'i tetikler, `Kolay.apk` Release'e eklenir (elle: `tools/release.sh`).
3. Kabuk CI'da eklenemediyse (Secret eksik) yedek yol: `tools/release-shell.sh` kabuğu derleyip imzalar, **son** Release'e ve manifestine ekler. Kabuğu ≥ 1.0.5 olan telefonlar Ayarlar'dan “Kabuğu indir ve yükle” ile alır.
4. Uygulama içi yol yoksa (eski kabuk, ilk kurulum) kullanıcı (sen) `Kolay.apk`'yı elle yükler. Yeni modül daha yüksek `api` istiyorsa, eski kabuklu cihazlar "kabuk güncellemesi gerekli" uyarısı görür ve modülü almaz — **önce kabuğu dağıt, sonra o API'yi isteyen modülü yayınla.**

## Geri alma (kötü sürüm yayınlandı)
Cihazlar `versionCode` düşürmez; bu yüzden **düzeltme ileriye doğrudur**:
1. `git revert <kötü commit>` (veya eski kodu geri getir) → `tools/release.sh` → versionCode daha yüksek, kod eski/sağlam.
2. Kötü sürümü almış cihazlar: sürüm açılışta çöküyorsa kendiliğinden 2 oturumda kara listeye girer ve önceki sürüme düşer; çalışıp bozuk davranıyorsa yukarıdaki düzeltme sürümü iner.
3. İstersen kötü GitHub Release'ini sil (`gh release delete vN --yes`); `latest` bir öncekine döner. Ama cihaza inmiş kopyayı etkilemez, bu yüzden 1. adım şart.

## Anahtar rotasyonu (planlı)
1. Yeni ECDSA anahtarı üret; açık anahtarı `Config.UPDATE_PUBLIC_KEYS` dizisine **ekle** (eskisini silme).
2. Yeni kabuk yayınla, tüm cihazlara yükle (elle).
3. Cihazlar yeni kabuktayken `tools/release.sh`'ın anahtarını yenisine çevir (`update_key.pem`) ve GitHub Secret'ı da güncelle.
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
- Release'leri yalnızca `release.yml` veya `tools/release.sh` üretmeli; elle yüklenen dosya imzasız olacağı için cihazlarca reddedilir.

## Eski sürümleri temizleme
Yeni sürüm yayında olduğu doğrulandıktan sonra: GitHub → Actions → *purge-releases* → *Run workflow* (veya `gh workflow run purge-releases`). Son Release dışındakilerin hepsini ve etiketlerini siler; geri alınamaz. Önce silme: cihazlar `latest` adresini kaybeder.
