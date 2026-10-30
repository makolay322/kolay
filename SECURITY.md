# Güvenlik

## Güvenlik açığı bildirme
Bu bir kişisel projedir; bir açık bulursan depo sahibine (`@makolay322`) GitHub üzerinden **özel** olarak ulaş
(Security → *Report a vulnerability*). Herkese açık issue açma.

## Tehdit modeli

| Varlık | Tehdit | Önlem |
|---|---|---|
| Telefondaki kod bütünlüğü | GitHub hesabı/depo ele geçirilir, sahte güncelleme yayınlanır | Güncelleme **ECDSA ile imzalı**; açık anahtar kabuğa gömülü, özel anahtar yalnızca geliştirici makinesinde. İmzasız/uyumsuz dosya yüklenmez. |
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
- GitHub Actions iş akışı işlemleri yalnızca derleme/tarama yapar; imza anahtarı CI'da **bulunmaz**.

## Kodda değişmez ilkeler
Bkz. [CLAUDE.md](CLAUDE.md#değişmez-kurallar-ihlal--güvenlikstabilite-hatası). Özet: depoda sır yok, OCR metni diske/loga yok, yalnızca HTTPS,
güncelleme yalnızca imzayla, sözleşme yalnızca büyür.
