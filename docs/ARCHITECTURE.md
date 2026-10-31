# Mimari

## Neden kabuk + modül?
Android bir APK'yı sessizce kendi kendine güncelleyemez (her güncelleme kullanıcı onayı ister). Kolay bunu şöyle aşar:
**değişmeyen küçük bir kabuk APK'sı**, **sık değişen kodu** (arayüz + araçlar) ayrı bir APK dosyasından, çalışma anında
`DexClassLoader` ile yükler. Modül dosyası GitHub'dan iner; kullanıcıdan onay gerekmez ama kabuk her indirmeyi
[imzayla doğrular](UPDATE_PROTOCOL.md).

## Parçalar

| Modül | Paket | Çıktı | İçerik |
|---|---|---|---|
| `sdk` | `app.kolay.sdk` | (kabuğa gömülü kütüphane) | `Host`, `Module` arayüzleri |
| `shell` | `app.kolay.shell` | `Kolay.apk` (`app.kolay`) | `MainActivity`, `Registry`, `Loader`, `Updater`, `UpdateJob`, `SecretStore`, `Verify`, `Config`; `QrTileService` (hızlı ayar kutucuğu), `ShellInstaller` + `InstallResultReceiver` (uygulama içi kabuk güncellemesi); Tesseract `.so` + `deu.traineddata`; ZXing çözücüsü; gömülü yedek modül |
| `payload` | `app.kolay.payload` | `payload.apk` (`app.kolay.payload`, kurulabilir değil) | `Entry`, `ui/*` (tasarım sistemi), `screens/*`, `tools/*` |

Bağımlılık yönü: `payload → sdk`, `shell → sdk`. Payload, sdk ve Tesseract'a **yalnızca derleme zamanı** (`compileOnly`) bağlıdır;
çalışırken bu sınıflar kabuğun sınıf yükleyicisinden gelir (`DexClassLoader`'ın `parent`'ı).

## Açılış akışı (`MainActivity.boot`)

```
onCreate
 ├─ Registry.ensureBundled()        gömülü modülü files/payloads/<vc>/ altına kopyala (salt-okunur)
 ├─ Registry.evaluatePreviousSession()   önceki oturum sağlıklı mıydı? (aşağıya bak)
 ├─ ensureTessdata()
 ├─ for aday in Registry.candidates():   yeniden eskiye; kara listedekiler yok; gömülü en sonda
 │    try  Loader.load(aday)        meta + imza + DexClassLoader
 │         Registry.startSession(vc)
 │         module.onCreate(host)    ← payload arayüzü kurar, host.ready() der
 │         ✔ bitti
 │    catch Throwable → kara listeye al (gömülü hariç), sıradakini dene
 └─ hepsi başarısız → showFatal()   "Kolay açılamadı" + sürüm sayfası bağlantısı
```
Sonra: `UpdateJob.schedule` (12 saatte bir) ve `Updater.checkAsync` (en çok 6 saatte bir) başlar.

### Oturum sağlığı ve kara liste
`Registry` her oturum için `{vc, ready, crashed}` tutar:
- `startSession(vc)` → ready=false, crashed=false
- `host.ready()` → ready=true
- yakalanmamış istisna (`Thread.setDefaultUncaughtExceptionHandler`) veya modül geri çağrımında istisna → crashed=true

Bir sonraki açılışta `evaluatePreviousSession`: ready değilse **veya** crashed ise "kötü oturum", sayaç +1; sağlıklıysa sayaç sıfırlanır.
**Üst üste 2 kötü oturum → sürüm kara listeye girer** ve bir daha seçilmez. Gömülü modül asla kara listeye girmez (son çare).

### Dizin düzeni (`filesDir/payloads/`)
```
<versionCode>/payload.apk   salt-okunur (Android 14+ gereği)
<versionCode>/payload.sig   indirilen modüllerde ECDSA imzası (gömülüde yok)
```
`prune()` gömülü + en yeni iki sürümü tutar, gerisini siler.

## Sürüm seçimi ve etkinleşme
- Güncelleyici yalnızca `versionCode > highestKnown()` olanı kabul eder (diskteki, kara listedeki ve gömülü sürümlerin en yükseği) → **geri alma saldırısı yok**.
- Yeni modül indirilince **çalışan modüle dokunulmaz**. `Host.restart()` (= `activity.recreate()`) çağrılınca açılış akışı baştan işler ve yeni sürümü yükler. Ana sayfada "Yeni sürüm hazır" bandı bunu tetikler.

## Sözleşme (`sdk`)
- `Host` (kabuk → modül): etkinlik, sırlar, tessdata konumu, `ready()`, sürüm bilgisi, ekran görüntüsü engeli, güncelleme durumu/denetimi/`restart()`.
- `Module` (modül → kabuk): `onCreate`, `onResume`, `onPause` (API 2), `onPermissionResult`, `onBackPressed`, `onDestroy`.
- **Uyumluluk kuralı:** yalnızca eklenir. Modül `app.kolay.api` meta verisiyle ihtiyacı olan sözleşme düzeyini bildirir;
  kabuk `MIN_PAYLOAD_API ≤ api ≤ SHELL_API` dışındakini yüklemez, güncelleyici de indirmez ("kabuk güncellemesi gerekli" uyarısı çıkar).

## Modül içi yapı (`payload/`)
```
Entry                    Module uygulaması: tema, Nav kökü, ana sayfa, geri tuşu, ömür döngüsü
ui/                      Palette, Ctx, Ui, Icon, Nav, Screen, Header, Arc, Bar, Toggle  → docs/UI.md
screens/                 HomeScreen, SettingsScreen
tools/Tool, Tools        araç arayüzü + kayıt listesi                                   → docs/ADDING_A_TOOL.md
tools/ocr/               Tıbbi OCR: OcrTool, Picker/Progress/ResultScreen, Ocr, Mask, Session
tools/qr/                QR Okuyucu: QrTool, QrScreen (arayüz), QrCamera (camera2), QrDecoder (ZXing)
```
UI tamamen programatiktir (XML kaynak yok): payload sınıf yükleyiciyle gelir, kabuğun `Resources` nesnesinde kendi kaynakları bulunmaz.

## Tıbbi OCR akışı
1. `PickerScreen`: MediaStore'dan son 90 fotoğraf; seçim `Session.selected` içinde.
2. `ProgressScreen` → `Ocr.run(pages)`: her sayfa gri tonlama + hafif kontrast, ≤2400 px, Tesseract (`deu`, PSM single column).
3. `Mask.collect` tüm sayfalardan isim adaylarını toplar; `Mask.apply` her sayfada maskeler (`[NAME]`, `[GEBURTSDATUM]`, `[ADRESSE]`, `[ORT]`, `[IBAN]`).
4. Maskeli metin **tek istekte** buluta gider: sayfaları doğru sıraya dizer + OCR hatalarını düzeltir + çöpü temizler, içerik eklemez/çıkarmaz.
5. `ResultScreen`: kopyala / paylaş. Hiçbir şey diske yazılmaz.

Bulut isteği başarısız olursa maskeli ham OCR gösterilir (uyarı notuyla).

## QR Okuyucu ve hızlı ayar kutucuğu
- `QrCamera`: arka kamera, tek arka plan iş parçacığı; önizleme (`TextureView`) + YUV akışı. Yalnızca en yeni kare çözülür (eskiler atılır) → gecikme birikmez. Uygulama arka plana geçince (`Module.onPause`) kamera bırakılır.
- `QrDecoder`: ZXing `QRCodeReader`, yalnızca QR; ters renkli kodlar her 3. boş karede denenir. Saf Java.
- Sonuç diske/loga yazılmaz; bağlantı kendiliğinden açılmaz, kullanıcı "Bağlantıyı aç"a dokunur (yalnızca http/https).
- Kutucuk: `QrTileService` (kabuk) `app.kolay.action.QR_SCAN` eylemiyle `MainActivity`'yi açar; `Entry` ilk açılışta bu eylemi görürse ana sayfanın üstüne QR ekranını iter ve eylemi tüketir (yeniden oluşturmada tekrar açılmaz).
