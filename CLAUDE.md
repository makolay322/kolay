# Kolay — çalışma kılavuzu (insanlar ve yapay zekâ oturumları için)

Bu depo birden fazla oturumdan yönetilir. **Önce bunu oku**, sonra [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Üç parça, üç kural
| Dizin | Rol | Değişiklik sıklığı | Kural |
|---|---|---|---|
| `sdk/` | Kabuk↔modül sözleşmesi (`Host`, `Module`) | çok nadir | **Yalnızca ekle.** Metot silme/değiştirme yok. `Module`'e sadece `default` metot. Kırıcı değişiklikte `Host.API` + `Config.SHELL_API` + modül manifestindeki `app.kolay.api` birlikte artar. |
| `shell/` | Kararlı çekirdek APK (`app.kolay`) | nadir | Mantığı ince tut. Yeni özellik buraya değil `payload/`'a. Değiştirirsen yeni kabuk APK'sı elle yüklenmeli. |
| `payload/` | Arayüz + araçlar (`app.kolay.payload`) | sık | Normal geliştirme burada. `Entry` giriş noktasıdır; yeniden adlandırma. |

## Değişmez kurallar (ihlal = güvenlik/stabilite hatası)
1. **Depoya sır girmez**: API anahtarı, `*.keystore`, `*.pem`, `keystore.properties`, `local.properties`. Anahtar cihazda Ayarlar'dan girilir (`Host.secret`).
2. Hasta verisi/örnek belge/gerçek isim depoya girmez (test örneklerinde uydurma isim kullan).
3. `payload/` **`compileOnly`** bağımlılık kullanır (sdk, Tesseract). Bunları payload içine paketleme: sınıf çakışması ve çift yükleme olur.
4. Payload `host.ready()` çağırmalı (ilk ekran görününce). Çağırmayan sürüm "bozuk açılış" sayılır, 2 kez üst üste olursa kara listeye girer.
5. `versionCode` yalnızca artar (`tools/release.sh` yönetir). Geri alma = eski kodla **daha yüksek** sürüm yayınlamak.
6. İmza anahtarları depo dışında: hepsi `~/.kolay/` altında; **yalnızca güncelleme anahtarı** (`update_key.pem`) ayrıca GitHub Secret `UPDATE_KEY_PEM` olarak durur (otomatik yayın için). APK keystore'u (`release.keystore`) **asla** GitHub'a yüklenmez. Asla commit etme, asla çıktıya yazma; iş akışını (`release.yml`) anahtarı yazdıracak/yükletecek biçimde değiştirme.
7. `main`'e `payload/**` veya `sdk/**` değişikliği push etmek **otomatik olarak yeni sürüm yayınlar** (`.github/workflows/release.yml`). Yarım iş main'e girmesin; sürüm elle de çıkarılabilir (`tools/release.sh`).
7. Ağ yalnızca HTTPS; güncelleme adresi `Config.REPO_BASE` ile başlamalı, indirme sunucusu `Config.trustedHost` ile sınırlı.
8. OCR metni diske/loga yazılmaz. Loglara kişisel veri koyma.
9. Gradle bağımlılık doğrulaması açık (`gradle/verification-metadata.xml`); bağımlılık eklersen `./gradlew --write-verification-metadata sha256 :shell:assembleRelease` ile güncelle ve farkı gözle incele.

## Sık işler
```bash
./gradlew :shell:assembleRelease          # yerel derleme (gerekli: JDK 17, Android SDK, local.properties → sdk.dir)
tools/release.sh                          # yeni modül sürümü yayınla (çalışma ağacı temiz olmalı, gh girişi gerekir)
```
- Yeni araç: [docs/ADDING_A_TOOL.md](docs/ADDING_A_TOOL.md). Arayüz: [docs/UI.md](docs/UI.md) (renk/ölçü sabit kodlama; `Palette` + `Ui` kullan).
- Sürüm/anahtar/geri alma: [docs/OPERATIONS.md](docs/OPERATIONS.md).

## Test gerçekliği
Cihaz testi elle yapılır (emülatör yok). Saf Java parçalar (`Verify`, `Mask`) masaüstünde JVM ile denenebilir.
Bir değişikliği "çalışıyor" diye raporlamadan önce neyin gerçekten çalıştırıldığını yaz; cihazda denenmediyse söyle.

## Dil ve stil
Arayüz metinleri ve dokümanlar Türkçe; kod, tanımlayıcılar ve kod yorumları İngilizce. Çevredeki koda uy (yorum yoğunluğu, adlandırma).
Arayüzde yalnızca Ayarlar'daki anahtar alanı "Gemini API anahtarı" diye adlandırılır; diğer yerlerde servis adı yerine "cilalama" denir.
