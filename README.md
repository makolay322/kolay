# Kolay

Telefonda çalışan, **kendini GitHub'dan güncelleyen kişisel araç kutusu**. Tek bir Android uygulaması (Android 10+);
araçlar arayüz modülüyle birlikte arka planda güncellenir.

## Araçlar

| Araç | Ne yapar |
|---|---|
| **OCR** | Belge fotoğraflarını cihazda okur (Tesseract). *Tıbbi* modda kişisel verileri cihazda maskeler, sayfaları sıraya dizip temizlenmiş metin verir; *Genel* modda ham metin ve isteğe bağlı cilalama. |
| **Helal Sorgu** | Barkoddan (Open Food Facts) ya da etiket fotoğrafından içindekileri alır, Hanefi ölçütleriyle *helal / haram / şüpheli* değerlendirmesi yapar. |
| **Cuma Mesajı** | Doğrulanmış 189 hadisten biriyle rastgele, yatay 3:2 Cuma görseli çizer; kaydet / paylaş. |
| **Namaz Vakitleri** | Konuma göre vakitler (Diyanet açıları), geri sayım, vakit başına hatırlatıcı ve ana ekran widget'ı. |
| **Uygulama Güncelle** | Google Play dışından yüklenen uygulamalar için APKPure, APKMirror veya LiteAPKs'te (uygulama başına seçilir) yeni sürüm arar, indirir, kurar ve dosyayı siler. |
| **YouTube Özeti** | YouTube bağlantısından Türkçe özet; paylaşım menüsünde “YouTube Özet” olarak da çıkar. |
| **Fotoğraf Sıkıştır** | Seçilen fotoğrafın %90 kaliteli JPEG kopyasını `Pictures/Kolay`'a kaydeder. |
| **QR Okuyucu** | Hızlı QR okuma; bildirim panelindeki hızlı ayar kutucuğundan da açılır. |

Ana sayfadaki “Ko” logosu günün saatine göre renk değiştirir; uygulama açıldığında ve öne her geldiğinde kısa bir giriş animasyonu oynatır.
Yeni araç eklemek: [docs/ADDING_A_TOOL.md](docs/ADDING_A_TOOL.md).

## Kurulum

1. [Releases](https://github.com/makolay322/kolay/releases/latest) sayfasından **`Kolay.apk`** dosyasını indirip yükle.
2. Uygulamayı aç → sağ üstte **Ayarlar** → *Gemini API anahtarı* alanına anahtarı yapıştır → **Kaydet**. Anahtar yalnızca
   cilalama, özet ve helal değerlendirmesi için gerekir; diğer araçlar anahtarsız çalışır.
3. Sonraki sürümler kendiliğinden iner; ana sayfada “Yeni sürüm hazır” bandı çıkınca dokunman yeterli.

Bazı sürümler kabuk değişikliği de içerir (ikon, izin, widget gibi). O zaman Ayarlar'da **“Kabuğu indir ve yükle”** düğmesi
çıkar; tek dokunuşla yeni `Kolay.apk` indirilir, doğrulanır ve Android yükleyicisine verilir.

API anahtarı yalnızca telefonda, Android Keystore ile şifreli tutulur. Bu depoda **hiçbir sır yoktur**.

## Nasıl çalışır (kısaca)

```
┌──────────────── Kolay.apk (kabuk, app.kolay) ────────────────┐
│ izinler · widget · hatırlatıcı · Tesseract verisi · anahtar   │  nadiren değişir
│ deposu · güncelleyici · modül yükleyici · geri dönüş          │
│        ▲ yükler                  │ indirir + doğrular          │
│  ┌─────┴──────────────┐           ▼                            │
│  │ modül (payload.apk)│ ◄── GitHub Releases ◄── release.yml   │  her sürümde değişir
│  │ arayüz + araçlar   │    (SHA-256 + ECDSA imzası)            │
│  └────────────────────┘                                        │
└────────────────────────────────────────────────────────────────┘
```

- **Kabuk** (`shell/`): kararlı çekirdek. Manifest (izinler, widget, kutucuk, paylaşım hedefi), anahtar deposu, güncelleme ve kurtarma.
- **Modül** (`payload/`): arayüz ve tüm araçlar. Yeni sürüm arka planda iner, imzası doğrulanır, uygulama yenilenince devreye girer.
- **Sözleşme** (`sdk/`): kabuk ile modül arasındaki arayüz (`Host`, `Module`); yalnızca büyür, bozulmaz.

`main`'e yapılan her push GitHub Actions'ta derlenir, imzalanır ve yeni sürüm olarak yayınlanır (`1.001`, `1.002`, …).
Ayrıntı: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Güvenlik özeti

- Güncelleme dosyaları GitHub'da durur ama **yalnızca sahibinin anahtarıyla imzalıdır**; kabuk imzayı doğrulamadan kod çalıştırmaz.
- Bozuk/çöken bir modül otomatik kara listeye alınır, önceki sağlam sürüme dönülür.
- OCR'da kişisel veriler buluta gitmeden önce cihazda maskelenir; OCR metni diske yazılmaz.
- “Uygulama Güncelle” bir dosyayı yalnızca paket adı, sürümü ve imzası yüklü uygulamayla uyuşuyorsa kurar.
- Yedekleme kapalı, yalnızca HTTPS.

Tam model: [SECURITY.md](SECURITY.md).

## Geliştirme ve işletme

| Konu | Doküman |
|---|---|
| Mimari, açılış akışı, araçların iç işleyişi | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| Güncelleme protokolü ve manifest biçimi | [docs/UPDATE_PROTOCOL.md](docs/UPDATE_PROTOCOL.md) |
| Derleme, kurulum, test | [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) |
| Sürüm çıkarma, anahtarlar, geri alma, olay müdahalesi | [docs/OPERATIONS.md](docs/OPERATIONS.md) |
| Yeni araç ekleme | [docs/ADDING_A_TOOL.md](docs/ADDING_A_TOOL.md) |
| Arayüz sistemi (renkler, ölçüler, hareket) | [docs/UI.md](docs/UI.md) |
| Başka oturumlar / yapay zekâ için çalışma kuralları | [CLAUDE.md](CLAUDE.md) |
| Sürüm notları | [CHANGELOG.md](CHANGELOG.md) |

## Hızlı komutlar

```bash
./gradlew :shell:assembleRelease   # kabuk APK'sı (içine güncel modülü gömer); JDK 17 + Android SDK gerekir
tools/release.sh [sürümAdı]        # elle modül sürümü (normalde main'e push yeterli)
```

Üçüncü taraf bileşenler: [NOTICE.md](NOTICE.md).
