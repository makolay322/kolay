# Değişiklik günlüğü

Biçim: [Keep a Changelog](https://keepachangelog.com/tr/). Modül sürümleri `tools/release.sh` ile yayınlanır
(görünen sürüm `1.0.x` yama sayısıyla artar; dahili `versionCode` her yayında +1 olur ve kullanıcıya gösterilmez).

## 1.000
### Eklendi
- Cuma Mesajı aracı: rastgele İslami temalı (yarısı siyah) 3:2 görsel üzerine Buhârî, Müslim ve Tirmizî'den ünlü hadislerden biri yazılır; yeni görsel/başka hadis/dikey 2:3, kaydet ve paylaş. Görsel Canvas ile üretilir, ağ gerekmez.
- Uygulama içi kabuk güncellemesi: yeni sürüm daha yeni kabuk istediğinde Ayarlar'da “Kabuğu indir ve yükle” çıkar; kabuk APK'sı Release'ten indirilir, SHA-256 + güncelleme anahtarı imzası + paket adı/sürüm/imza sertifikası doğrulanır, sistem yükleyicisine verilir (onayı Android sorar, sessiz yükleme mümkün değil). İlk kez “bilinmeyen kaynaktan yükleme” izni istenir. Manifest'e `shell*` alanları eklendi; `tools/release-shell.sh` ve `tools/manifest_shell.py`.
- QR Okuyucu aracı: kamera açılır açılmaz tarar (arka kamera, fener, yalnızca en yeni kare çözülür); sonuç önce gösterilir, bağlantı ancak dokununca açılır. Bildirim panelinde (hızlı ayarlar) "QR Okuyucu" kutucuğu: tek dokunuşla uygulamayı QR ekranında açar; araç ekranındaki "Hızlı ayarlara ekle" düğmesi (Android 13+) kutucuğu tek onayla ekler.
- **Kabuk 1.0.5 (elle yüklenmeli) + sözleşme API 2:** kabuk CAMERA izni, ZXing çözücüsü ve hızlı ayar kutucuğu servisini taşır; `Module.onPause()` eklendi (kamera arka plana geçince bırakılır). QR'lı modül `api=2` ister; eski kabuklu cihazlar "kabuk güncellemesi gerekli" görür.
- Otomatik yayın: `main`'e modül değişikliği push edilince GitHub Actions derler, imzalar, Release açar (`release.yml`). Sürüm numarası artık durumsuz (son manifest'ten).
- YouTube Özeti aracı: yapıştırılan bağlantının transkripti özgün dilinde alınır, Gemini ile Türkçe özetlenir.
- Fotoğraf Sıkıştır aracı: seçilen fotoğrafın %90 kaliteli JPEG kopyası Pictures/Kolay'a kaydedilir (tarih, cihaz ve çekim EXIF bilgileri taşınır; konum, Android izin kısıtı nedeniyle taşınmayabilir).
### Düzeltildi
- Ayarlar: "Şimdi denetle"ye art arda basınca indirilen sürüm uygulanamıyor ve "güncel" deniyordu; düğme artık "Şimdi yenile"de kalıyor.
### Değişti
- Ana sayfa: araçlar dikdörtgen liste yerine 2 sütunlu karo düzeninde.
- Ayarlar: "Cilalama anahtarı" alanı "Gemini API anahtarı" olarak yeniden adlandırıldı.

## 1.0.1 — ilk yayın
### Eklendi
- Uygulama adı **Kolay**; çok araçlı kutu mimarisi (ana sayfa + araç kaydı).
- Yeni arayüz sistemi: açık/koyu tema, animasyonlu gezinme, dokunma geri bildirimi, yönlü geçişler, özel ikonlar.
- Ayarlar ekranı: API anahtarı, ekran görüntüsü engeli, güncelleme durumu.
- Sayfa sırasını düzelten çoklu fotoğraf OCR akışı; iptal edilebilir ilerleme ekranı.
- Güncelleme: çoklu imza anahtarı (rotasyon), indirme sunucusu doğrulaması, "Yeni sürüm hazır" bandı ve canlı yenileme.
- Güvenlik: yedekleme/aktarım kapalı, ağ güvenliği yapılandırması, Gradle bağımlılık doğrulaması, hassas pano işareti.
- Düz (flat) yeni simge: K harfi + devre izi; Android tema simgesi (monochrome) desteği.
- Tıbbi OCR aracı: Tesseract + yerel isim maskeleme + cilalama.
- Eski deneme sürümleri (v2, v3) ve etiketleri silindi; bu sürüm tek başlangıç noktasıdır.
