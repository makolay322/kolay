# Değişiklik günlüğü

Biçim: [Keep a Changelog](https://keepachangelog.com/tr/). Modül sürümleri `tools/release.sh` ile yayınlanır
(görünen sürüm `1.0.x` yama sayısıyla artar; dahili `versionCode` her yayında +1 olur ve kullanıcıya gösterilmez).

## 1.000
### Değişti
- Uygulama ikonunun arka planı açık/koyu temaya göre seçilir (açıkta yumuşak renkli zemin, koyuda koyu zemin + açık logo; Android 12+ duvar kağıdı renkleriyle). Açılış animasyonu ~2,2 sn'ye yavaşladı ve başlatıcıdan açılışta sonuna kadar ekranda kalır (QR kutucuğu ve paylaşım hedefi beklemeden açılır). **Kabuk 1.012 gerekir.**
- Cuma Mesajı: hilal küçültülüp başlığın üstüne alındı, başlığa gölge eklendi ve başlık bandına yıldız düşmez; hilal artık yazıyla çakışmaz.
### Değişti (logo)
- Yeni “Ko” logosu uygulama ikonu oldu. Android 12+ cihazlarda ikonun renkleri duvar kağıdından gelir (dinamik renk); Android 13+ temalı ikon desteği vardır. Açılış ekranında logo animasyonla belirir (K kayar, O yaylanarak açılır, nokta belirir). **Kabuk 1.011 gerekir** (Ayarlar → “Kabuğu indir ve yükle”).
- Ana sayfada canlı logo: saat gibi günün saatine göre renk değiştirir (sabah pembe-mavi, gündüz turkuaz, akşam mercan-mor, gece çivit), parıltısı yavaşça döner, nokta hafifçe nabız atar. Başlatıcıdaki ikon bu şekilde canlandırılamaz; Android bunu yalnızca Google'ın Saat/Takvim uygulamalarına tanır.
### Değişti (arayüz cilası)
- Tüm ekranlar ortak ölçülere bağlandı: 16 dp kenar boşluğu, düğme yükseklikleri (alt çubuk 56, içerik 52, ikincil 48), tek tip bölüm etiketi, kart ve giriş alanı.
- Boş, izin ve hata ekranları tek tasarımda (ikon rozeti, başlık, açıklama, eylem düğmesi). Fotoğraf izni reddedildiğinde “İzin ver / Ayarları aç”, ayarlardan dönünce liste kendiliğinden açılır.
- Yeni ikonlar: fotoğraf, konum, cilalama, yapıştır, indir, katmanlar, video, belge. Aynı ikonu paylaşan karolar ayrıştı (OCR belge, Sıkıştır fotoğraf, YouTube video).
- Segmentli seçiciler (OCR tıbbi/genel, Helal barkod/etiket) aynı bileşen. “Kopyala” her yerde düğme üstünde “Kopyalandı” onayı verir.
- Namaz: vakitler kartta; geçmiş vakitler soluk, sıradaki vurgulu; tarih başlıkta; konum aranırken yükleniyor göstergesi; izin/konum hatalarında eylem düğmesi; ayarlardan dönünce ekran tazelenir (tam zamanlı alarm düğmesi bayat kalmaz). Kayıtlı konum varken yenileme başarısızsa vakitler silinmez, kısa bilgi verilir.
- Cuma: önizleme yuvarlatılmış kare kartta, ilk çizimde yükleniyor göstergesi; “Paylaş” birincil düğme.
- YouTube: klavyeden “Git” ile başlar, klavye kapanır, durum satırında küçük yükleniyor göstergesi.
- Sıkıştır: işlem sırasında yükleniyor göstergesi; sonuç ve hata ekranları yeni tasarımda.
- Helal: hata kartı ve alt çubuk düzenlendi. OCR: hata sonucunda “Başka sayfa seç”.
### Kaldırıldı
- Sesli Not aracı (ve kabuktaki mikrofon izni), Cuma Mesajı'nda dikey/yatay seçeneği, Ayarlar'daki "Ekran görüntüsünü engelle" (kayıtlı "açık" değer kabuk açılışında temizlenir).
### Düzeltildi
- Genel OCR sonrası tıbbi modun Tesseract ayarı bozuluyordu; geri yüklenir. Namaz ekranında vakit geçişinde sayaç zinciri çoğalıyordu; güney yarım küre enlem etiketi; widget çift çiziliyordu. Genel moddaki “Cilala” artık metni önce cihazda maskeler.
### Değişti
- Namaz widget'ına dokununca renk paleti rastgele değişir (uygulama açılmaz). Kabuk 1.008.
- Cuma görselleri biraz aydınlatıldı; menüde "kare" yazar. Namaz ekranında satırlar/düğmeler kırpılmayacak boyuta döndü.
- Cuma Mesajı yalnızca kare (1:1) görsel üretir; başlık yalnızca "Cumamız Mübarek Olsun" (Arapça satır ve "Cumanız" kalktı).
- Namaz widget'ı: arka plan yok; pastel gradyanlı, ince yazılı saat; dar widget'ta saat/dakika alt alta büyük çizilir. **Kabuk 1.007 gerekir.**
- "Tıbbi OCR" karosu "OCR" oldu (tıbbi/genel mod seçimi içeride kalır).
- Namaz Vakitleri ekranı sıkıştırıldı: tüm vakitler kaydırmadan sığar.
### Eklendi
- YouTube Özet paylaşım menüsü hedefi: başka uygulamalardan paylaşılan YouTube bağlantısı doğrudan özetlenir.
- Tıbbi OCR'a Genel OCR modu (Gemini ile isteğe bağlı temizleme); mod anahtarı.
- Helal Sorgu: barkod/etiket mod anahtarı; açılış modu barkod, otomatik OCR geçişi kaldırıldı.
- Namaz: vakit başına ayrı hatırlatıcı anahtarı; boyutlandırılabilir modern widget (yalnızca sıradaki vaktin 24 sa saati). **Kabuk 1.006 + Host API 4 gerekir.**
- Cuma Mesajı: 189 doğrulanmış hadis (tek kaynak: Buhârî/Müslim/Tirmizî), "Cumamız/Cumanız Mübarek Olsun", yalnızca koyu/olgun tonlar, pencere ışıksız siluetler.
- QR, Fotoğraf Sıkıştır ve YouTube Özeti karoları ana sayfanın altında ince yatay şeritler.
- Namaz Vakitleri aracı + ana ekran widget'ı + hatırlatıcı: konumdan vakitler hesaplanır, widget sıradaki vakti ve geri sayımı gösterir, vakit çıkmadan 15 dk önce bildirim gelir. **Kabuk 1.005 + Host API 3 gerekir** (konum, bildirim, alarm izinleri ve widget kabukta).
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
