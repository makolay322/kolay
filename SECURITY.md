# Güvenlik

## Güvenlik açığı bildirme
Bu bir kişisel projedir; bir açık bulursan depo sahibine (`@makolay322`) GitHub üzerinden **özel** olarak ulaş
(Security → *Report a vulnerability*). Herkese açık issue açma.

## Tehdit modeli

| Varlık | Tehdit | Önlem |
|---|---|---|
| Telefondaki kod bütünlüğü | GitHub hesabı/depo ele geçirilir, sahte güncelleme yayınlanır | Güncelleme **ECDSA ile imzalı**; açık anahtar uygulamaya gömülü, özel anahtar yalnızca geliştirici makinesinde ve GitHub Secret'ta (aşağıya bak). Ayrıca APK aynı paket adı ve aynı imza sertifikasıyla imzalı olmalı (Android de bunu ayrıca denetler). İmzasız/uyumsuz dosya kurulmaz. |
| Aynı | Ağ saldırganı (MITM) | Yalnızca HTTPS (`network_security_config`), yalnızca sistem CA'ları, indirme sunucusu `github.com`/`*.githubusercontent.com` ile sınırlı, SHA-256 + imza. |
| Aynı | Eski (zafiyetli) sürümü geri yükletme | `versionCode` yalnızca artar; yüklü sürümden küçük/eşit güncelleme reddedilir (Android de sürüm düşürmeye izin vermez). |
| Kararlılık | Bozuk sürüm uygulamayı kilitler | Otomatik geri dönüş yok (tek APK'ya geçişte kaldırıldı). Önlem: CI derlemesi yeşil olmadan sürüm çıkmaz; düzeltme daha yüksek sürümle gelir ve arka plan işi ekran kapalıyken kurar; en kötü durumda Release'teki APK elle yüklenir (veri kalır). |
| API anahtarı | Depoya/APK'ya sızması | Depoda ve APK'larda yok; cihazda Android Keystore (AES-GCM, dışa aktarılamaz anahtar) ile şifreli. İstek başlığında gider, URL'de değil. |
| Hasta verisi | Buluta kimlik bilgisi gitmesi | İsim/doğum tarihi/adres/IBAN **cihazda** maskelenir, yalnızca maskeli metin gider (yerel regex sınırı: aşağıya bak). |
| Hasta verisi | Diskte/yedekte kalması | Sonuç diske yazılmaz; bulut yedek ve cihaz aktarımı kapalı (`allowBackup=false`, `dataExtractionRules`). Pano kopyası "hassas" işaretlenir. Ekran görüntüsü engeli (`FLAG_SECURE`) sahibinin kararıyla kaldırıldı; ekran kaydı/son uygulamalar önizlemesi artık korunmaz. |
| Uygulama yüzeyi | Başka uygulamaların erişimi | Dışa açık bileşenler: başlatıcı etkinlik, “YouTube Özet” paylaşım hedefi (yalnızca düz metin alır, bağlantı olarak işler) ve namaz widget alıcısı (yalnızca renk paleti değiştirir); servis `BIND_JOB_SERVICE` izniyle korunur. İzinler: `INTERNET`, `READ_MEDIA_IMAGES`, kamera, yaklaşık konum (yalnızca cihazda), bildirim, tam zamanlı alarm, açılışta başlatma (namaz hatırlatıcısı), uygulama yükleme isteği (kendi güncellemesi ve “Uygulama Güncelle”). Paket görünürlüğü yalnızca başlatıcı simgesi olan uygulamalarla sınırlı (`<queries>`). “Uygulama Güncelle” indirdiği dosyayı kurmadan önce paket adı, sürüm ve imzanın yüklü uygulamayla aynı olduğunu denetler; dosya kurulumdan hemen sonra silinir. |
| Tedarik zinciri | Zararlı bağımlılık | Gradle bağımlılık doğrulaması (`gradle/verification-metadata.xml`, SHA-256) ve Gradle dağıtım özeti sabitlenmiş. |

## Bilinen sınırlar (dürüst liste)
- **Maskeleme kural tabanlıdır.** Unvansız, doğum tarihi/etiket yanında geçmeyen ve başka sayfada da yer almayan tek başına bir isim sızabilir. Bulut isteği öncesi çıktıyı bu yüzden "mükemmel anonimleştirme" saymayın.
- Telefon köklenmişse (root) uygulamaya özel veriler korunamaz.
- Debug imzalı yerel derlemeler üretim anahtarını taşımaz ve güncelleme almaz; dağıtım için kullanılmamalı.
- `build.yml` yalnızca derler/tarar; imza anahtarı görmez, PR'larda da çalışır.
- `release.yml` yalnızca `main`'e push'ta (veya elle) çalışır ve **güncelleme imza anahtarını** (`UPDATE_KEY_PEM` Secret) kullanır. Sınırlar: fork/PR çalıştırmaları Secret görmez; üçüncü taraf Action'lar commit SHA'sına sabitlenmiştir; iş `contents: write` dışında yetki almaz; anahtar yalnızca geçici dosyaya yazılır. **Kabul edilen risk:** depoya yazma yetkisi olan biri (veya GitHub hesabın) ele geçirilirse sahte güncelleme yayınlanabilir; bu zaten Release yükleyebilen herkes için geçerliydi, ama artık anahtar da GitHub'dadır. Hesapta 2FA zorunlu, `main` dalına yalnızca sen yazarsın. APK keystore'u da Secret olarak GitHub'dadır (`KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`); sahibi bilerek kabul etti. Tek APK düzeninde sahte bir güncellemenin cihaza kurulabilmesi için **ikisi birden** gerekir: manifest imzası için güncelleme anahtarı ve Android'in kabul etmesi için keystore. İkisi de GitHub Secret'ta olduğundan GitHub hesabının korunması (2FA) esastır. Workflow keystore'u yalnızca geçici dosyaya açıp işten sonra silmelidir. `~/.kolay` yedeği yine şarttır.

## Kodda değişmez ilkeler
Bkz. [CLAUDE.md](CLAUDE.md#değişmez-kurallar-ihlal--güvenlikstabilite-hatası). Özet: depoda sır yok, OCR metni diske/loga yok, yalnızca HTTPS,
güncelleme yalnızca imzayla.
