# Kolay

Telefonda çalışan, **kendini GitHub'dan güncelleyen kişisel araç kutusu**. Tek bir Android uygulaması; içindeki araçlar
zamanla çoğalır, yeni bir araç eklemek APK'yı yeniden yüklemeyi gerektirmez.

| Araç | Ne yapar |
|---|---|
| **Tıbbi OCR** | Almanca belge fotoğraflarını okur, kişisel verileri *cihazda* maskeler, sayfaları doğru sıraya dizip temizlenmiş düz metin verir (kopyala / paylaş). |

> Yeni araçlar `payload/` içine eklenir — bkz. [Araç ekleme](docs/ADDING_A_TOOL.md).

## Nasıl çalışır (kısaca)

```
┌────────────── Kolay.apk (kabuk) ──────────────┐
│ Tesseract · izinler · gizli anahtar deposu     │   nadiren değişir, elle yüklenir
│ güncelleyici · modül yükleyici · geri dönüş    │
│        ▲ yükler            │ indirir + doğrular │
│  ┌─────┴──────┐             ▼                    │
│  │ modül (APK)│ ◄── GitHub Releases ◄── tools/release.sh
│  └────────────┘   (SHA-256 + ECDSA imzası)      │   her sürümde değişir, otomatik gelir
└────────────────────────────────────────────────┘
```

- **Kabuk** (`shell/`): kararlı çekirdek. Yerel kütüphaneler, API anahtarı deposu, güncelleme ve kurtarma mantığı.
- **Modül** (`payload/`): arayüz ve tüm araçlar. Yeni sürüm arka planda iner, doğrulanır, uygulama yenilenince devreye girer.
- **Sözleşme** (`sdk/`): kabuk ile modül arasındaki arayüz; yalnızca büyür, bozulmaz.

Ayrıntı: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Kurulum

1. [Releases](https://github.com/makolay322/kolay/releases/latest) sayfasından **`Kolay.apk`** dosyasını indirip yükle (bir kez).
2. Uygulamayı aç → sağ üstte **Ayarlar** → *Gemini API anahtarı* alanına anahtarı yapıştır → **Kaydet**.
3. Bitti. Sonraki sürümler kendiliğinden iner; ana sayfada "Yeni sürüm hazır" bandı çıkınca dokunman yeterli.

API anahtarı yalnızca telefonda, Android Keystore ile şifreli tutulur. Bu depoda **hiçbir sır yoktur**.

## Güvenlik özeti

- Güncelleme dosyaları GitHub'da durur ama **yalnızca sende duran özel anahtarla imzalıdır**; kabuk imzayı doğrulamadan kod çalıştırmaz.
- Kötü/çöken bir modül otomatik kara listeye alınır, önceki sağlam sürüme dönülür.
- Kişisel veriler buluta gitmeden önce cihazda maskelenir; OCR çıktısı diske yazılmaz.
- Yedekleme kapalı, yalnızca HTTPS, isteğe bağlı ekran görüntüsü engeli.

Tam model: [SECURITY.md](SECURITY.md).

## Geliştirme ve işletme

| Konu | Doküman |
|---|---|
| Mimari, açılış akışı, durum makinesi | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| Güncelleme protokolü ve manifest biçimi | [docs/UPDATE_PROTOCOL.md](docs/UPDATE_PROTOCOL.md) |
| Derleme, kurulum, test | [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) |
| Sürüm çıkarma, anahtarlar, geri alma, olay müdahalesi | [docs/OPERATIONS.md](docs/OPERATIONS.md) |
| Yeni araç ekleme | [docs/ADDING_A_TOOL.md](docs/ADDING_A_TOOL.md) |
| Arayüz sistemi ve hareket ilkeleri | [docs/UI.md](docs/UI.md) |
| Başka oturumlar / yapay zekâ için çalışma kuralları | [CLAUDE.md](CLAUDE.md) |
| Sürüm notları | [CHANGELOG.md](CHANGELOG.md) |

## Hızlı komutlar

```bash
./gradlew :shell:assembleRelease   # kabuk APK'sı (içine güncel modülü gömer)
tools/release.sh [sürümAdı]        # modül sürümü: derle → imzala → etiketle → push → GitHub Release
```

Üçüncü taraf bileşenler: [NOTICE.md](NOTICE.md).
