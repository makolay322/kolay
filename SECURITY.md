# Güvenlik

## Güvenlik açığı bildirme
Bu bir kişisel projedir; bir açık bulursan depo sahibine (`@makolay322`) GitHub üzerinden **özel** olarak ulaş
(Security → *Report a vulnerability*). Herkese açık issue açma.

## Tehdit modeli

| Varlık | Tehdit | Önlem |
|---|---|---|
| Telefondaki kod bütünlüğü | GitHub hesabı/depo ele geçirilir, sahte güncelleme yayınlanır | Güncelleme **ECDSA ile imzalı**; açık anahtar kabuğa gömülü, özel anahtar yalnızca geliştirici makinesinde ve GitHub Secret'ta (aşağıya bak). İmzasız/uyumsuz dosya yüklenmez. |
| Aynı | Ağ saldırganı (MITM) | Yalnızca HTTPS (`network_security_config`), yalnızca sistem CA'ları, indirme sunucusu `github.com`/`*.githubusercontent.com` ile sınırlı, SHA-256 + imza. |
| Aynı | Eski (zafiyetli) sürümü geri yükletme | `versionCode` yalnızca artar; kabul edilen en yüksek sürümden küçük/eşit modül reddedilir. |
| Aynı | Diskte modül dosyasının değiştirilmesi | Dosya uygulamaya özel dizinde, salt-okunur; indirilen modül her açılışta imzayla yeniden doğrulanır. |
| Kararlılık | Bozuk sürüm uygulamayı kilitler | Açılış sağlık takibi, 2 kötü oturumda kara liste, gömülü yedek modül. |
| API anahtarı | Depoya/APK'ya sızması | Depoda ve APK'larda yok; cihazda Android Keystore (AES-GCM, dışa aktarılamaz anahtar) ile şifreli. İstek başlığında gider, URL'de değil. |
| Hasta verisi | Buluta kimlik bilgisi gitmesi | İsim/doğum tarihi/adres/IBAN **cihazda** maskelenir, yalnızca maskeli metin gider (yerel regex sınırı: aşağıya bak). |
| Hasta verisi | Diskte/yedekte kalması | Sonuç diske yazılmaz; bulut yedek ve cihaz aktarımı kapalı (`allowBackup=false`, `dataExtractionRules`). Pano kopyası "hassas" işaretlenir. |
| Hasta verisi | Ekran kaydı / son uygulamalar önizlemesi | Ayarlar'dan açılabilen `FLAG_SECURE`. |
| Uygulama yüzeyi | Başka uygulamaların erişimi | Tek dışa açık bileşen başlatıcı etkinlik; servis `BIND_JOB_SERVICE` izniyle korunur; yalnızca `INTERNET` ve `READ_MEDIA_IMAGES`. |
| Tedarik zinciri | Zararlı bağımlılık | Gradle bağımlılık doğrulaması (`gradle/verification-metadata.xml`, SHA-256) ve Gradle dağıtım özeti sabitlenmiş. |

## Bilinen sınırlar (dürüst liste)
- **Maskeleme kural tabanlıdır.** Unvansız, doğum tarihi/etiket yanında geçmeyen ve başka sayfada da yer almayan tek başına bir isim sızabilir. Bulut isteği öncesi çıktıyı bu yüzden "mükemmel anonimleştirme" saymayın.
- Telefon köklenmişse (root) uygulamaya özel veriler korunamaz.
- Debug imzalı yerel derlemeler üretim anahtarını taşımaz ve güncelleme almaz; dağıtım için kullanılmamalı.
- `build.yml` yalnızca derler/tarar; imza anahtarı görmez, PR'larda da çalışır.
- `release.yml` yalnızca `main`'e push'ta (veya elle) çalışır ve **güncelleme imza anahtarını** (`UPDATE_KEY_PEM` Secret) kullanır. Sınırlar: fork/PR çalıştırmaları Secret görmez; üçüncü taraf Action'lar commit SHA'sına sabitlenmiştir; iş `contents: write` dışında yetki almaz; anahtar yalnızca geçici dosyaya yazılır. **Kabul edilen risk:** depoya yazma yetkisi olan biri (veya GitHub hesabın) ele geçirilirse sahte güncelleme yayınlanabilir; bu zaten Release yükleyebilen herkes için geçerliydi, ama artık anahtar da GitHub'dadır. Hesapta 2FA zorunlu, `main` dalına yalnızca sen yazarsın. APK keystore'u da (kabuk imzası) Secret olarak GitHub'dadır (`KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`); sahibi bilerek kabul etti. Ek risk küçüktür: güncelleme anahtarı zaten uzaktan kod çalıştırabiliyor; keystore ek olarak sahte `Kolay.apk` imzalatabilir, ama o yine elle kurulmak zorundadır. Workflow keystore'u yalnızca geçici dosyaya açıp işten sonra silmelidir. `~/.kolay` yedeği yine şarttır.

## Kodda değişmez ilkeler
Bkz. [CLAUDE.md](CLAUDE.md#değişmez-kurallar-ihlal--güvenlikstabilite-hatası). Özet: depoda sır yok, OCR metni diske/loga yok, yalnızca HTTPS,
güncelleme yalnızca imzayla, sözleşme yalnızca büyür.
