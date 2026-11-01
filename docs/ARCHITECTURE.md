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
- `Host` (kabuk → modül): etkinlik, sırlar, tessdata konumu, `ready()`, sürüm bilgisi, güncelleme durumu/denetimi/`restart()`.
- `Module` (modül → kabuk): `onCreate`, `onResume`, `onPause` (API 2), `onPermissionResult`, `onBackPressed`, `onDestroy`.
- **Uyumluluk kuralı:** yalnızca eklenir. Modül `app.kolay.api` meta verisiyle ihtiyacı olan sözleşme düzeyini bildirir;
  kabuk `MIN_PAYLOAD_API ≤ api ≤ SHELL_API` dışındakini yüklemez, güncelleyici de indirmez ("kabuk güncellemesi gerekli" uyarısı çıkar).

## Modül içi yapı (`payload/`)
```
Entry                    Module uygulaması: tema, Nav kökü, ana sayfa, geri tuşu, ömür döngüsü
ui/                      Palette, Ctx, Ui, Icon, Nav, Screen, Header, Arc, Bar, Toggle  → docs/UI.md
screens/                 HomeScreen, SettingsScreen
tools/Tool, Tools        araç arayüzü + kayıt listesi                                   → docs/ADDING_A_TOOL.md
tools/ocr/               OCR (tıbbi/genel mod): OcrTool, Picker/Progress/ResultScreen, Ocr, Mask, Session
tools/qr/                QR Okuyucu: QrTool, QrScreen (arayüz), QrCamera (camera2; kipler QR / PRODUCT barkod / TEXT fotoğraf), QrDecoder (ZXing)
tools/halal/             Helal Sorgu: HelalTool, ScanScreen/LabelScreen (CamScreen tabanı), Off (Open Food Facts), LabelOcr, Halal (istem + karar), WorkScreen, VerdictScreen
tools/friday/            Cuma Mesajı: FridayTool, FridayScreen (önizleme/kaydet/paylaş), FridayArt (Canvas ile arka plan + yazı), Hadiths (hadis listesi)
tools/namaz/             Namaz Vakitleri: NamazTool, NamazScreen (saatler, geri sayım, hatırlatıcı anahtarı, widget düğmesi, konum alma)
tools/apps/              Uygulama Güncelle: AppsTool, AppsScreen (liste + sıra), Sources (APKPure/APKMirror/LiteAPKs), Browser (WebView), Installer
```
UI tamamen programatiktir (XML kaynak yok): payload sınıf yükleyiciyle gelir, kabuğun `Resources` nesnesinde kendi kaynakları bulunmaz.

## OCR akışı
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

## Helal Sorgu akışı
1. `ScanScreen`: kamera `PRODUCT` kipinde; kare 90° döndürülüp EAN-13/8, UPC-A/E, Code 128 okunur (1-B okuyucular yalnızca satır tarar).
2. `WorkScreen`: `Off.lookup` Open Food Facts'e **yalnızca barkod rakamlarını** gönderir; içindekiler (tr → genel → de → en) alınır.
3. Barkod yoksa/içindekiler boşsa/ağ yoksa `LabelScreen` (kamera `TEXT` kipi, ≤3,5 MP gri kare) → `CropScreen` (kullanıcı yalnızca içindekiler kısmını çerçeveler; çevre yazı OCR girdisini gürültülü yapar) → `LabelOcr` (Tesseract `deu`) açılır. Fotoğraf OCR biter bitmez geri dönüştürülür; metin diske yazılmaz.
4. `Halal.judge`: içindekiler tek istekte Hanefi ölçütleriyle değerlendirilir (sıcaklık 0). Çıktının 1. satırı `HELAL | HARAM | ŞÜPHELİ`, gerisi gerekçe. Belirsiz/bozuk liste → ŞÜPHELİ.
5. `VerdictScreen`: renkli karar + gerekçe + kullanılan içindekiler. "Fetva değildir" notu sabittir.
Not: gömülü OCR verisi yalnızca `deu`; Türkçe özel harfler (ş ğ ı) yanlış okunabilir, istem bunu bağlamdan düzeltmesini ister.

## Cuma Mesajı akışı
- `FridayArt.render(seed, hadis, kaynak)`: görsel tamamen Canvas ile çizilir (görsel dosyası yok): gradyan, geometrik desen (yıldız/daire), hilâl, yıldızlar, cami silueti, yarı saydam panelde hadis. Aynı seed aynı arka planı verir; seed'in yarısı siyah (koyu) temadır. Boyut yalnızca yatay 3:2, 1500×1000 (`FridayArt.W/H`): başlık üstte ortada, hilal üst köşede (başlığın yanında, üstüne binmez), hadis paneli ortada, cami silueti altta.
- `Hadiths.ALL`: `{metin, kaynak}`; kaynak tek bir derlemedir ("Buhârî N", "Müslim N" veya "Tirmizî N"), hadis hangisinde rivayet ediliyorsa o. Metin kendi Türkçe meâlimizdir; numara↔hadis eşleşmesi fawazahmed0/hadith-api (jsDelivr) İngilizce metinleriyle doğrulandı, Tirmizî'de yalnızca Elbânî'nin sahih/hasan dediği hadisler alındı. Yeni hadis eklerken aynı doğrulamayı yap; numarasından emin değilsen ekleme. ~230 karakteri geçmesin.
- Yalnızca koyu ve olgun tonlar (pencere ışıksız cami siluetleri); başlık yalnızca "Cumamız Mübarek Olsun".
- Görsel yalnızca Kaydet/Paylaş'a basılınca `Pictures/Kolay`'a yazılır (aynı görsel iki kez yazılmaz). Ağ ve Gemini kullanılmaz.

## Namaz Vakitleri akışı (Host API 4, kabuk ≥ 1.007)
- Hesap, widget ve alarmlar **kabukta** çalışır (uygulama kapalıyken de gerekir): `PrayerCalc` (saf Java, JVM'de denenir), `Prayer` (ayarlar + alarm + bildirim), `PrayerWidget`, `PrayerReceiver`. Payload yalnızca ön yüzdür; `Host.prayer*` metotlarıyla konuşur.
- Konum: payload `ACCESS_COARSE_LOCATION` ister, son bilinen/anlık konumu alır, yalnızca koordinat `Host.prayerSetLocation` ile kabukta saklanır (~5 km'den az oynama yok sayılır). İnternete gönderilmez.
- Yöntem: Diyanet açıları (imsak 18°, yatsı 17°), Hanefi ikindi (gölge 2), ihtiyat payları `PrayerCalc.TUNE_MIN` (güneş −7, öğle +5, ikindi +4, akşam +7 dk). Hafızaya dayalı; Diyanet takvimiyle karşılaştırılıp ayarlanmalı. Yüksek enlemde yatsı/imsak "gecenin açıya oranlı payı" ile hesaplanır.
- Widget: arka plansız, dokununca rastgele pastel palet; sıradaki vaktin 24 saatlik saati pastel gradyan, ince (light) yazı tipiyle `Canvas` bitmap'i olarak çizilir ve widget boyutunu doldurur (dar/dikeyde saat ve dakika alt alta, geniş olunca tek satır; `onAppWidgetOptionsChanged` ile yeniden çizilir). Güncelleme: vakit değişince alarm + 30 dk'da bir sistem güncellemesi. Sıradaki vakit "Güneş" hariçtir.
- Hatırlatıcı her vakit için ayrı ayrı açılıp kapatılır (`Host.prayerReminderMask/prayerSetReminderMask`, bit maskesi); kapalı vakitlerin alarmı kurulmaz. Her vaktin **bitmesine 15 dk kala** (Sabah→güneş, Öğle→ikindi, İkindi→akşam, Akşam→yatsı, Yatsı→ertesi imsak) bildirim. Tek seferde yalnızca sıradaki alarm kurulur; tetiklenince bir sonraki kurulur. Önyükleme, saat/saat dilimi değişimi ve kabuk güncellemesinde yeniden kurulur. Android 12+ tam zamanlı alarm izni yoksa `setAndAllowWhileIdle` (birkaç dakika kayabilir); ekrandaki düğme izin sayfasını açar.

## Uygulama Güncelle akışı (kabuk ≥ 1.013)
- Liste: başlatıcı simgesi olan, sistem uygulaması olmayan ve yükleyicisi Google Play (`com.android.vending`) olmayan uygulamalar. Görünürlük için kabuk manifestinde `<queries>` (MAIN/LAUNCHER) vardır; `QUERY_ALL_PACKAGES` kullanılmaz.
- Kaynak uygulama başına seçilir (segmentli seçici, varsayılan APKPure), anahtarla takipten çıkarılabilir. Ayarlar `app_updates` tercih dosyasında: `src:<paket>`, `off:<paket>`, `new:<paket>` (sitede görülen yeni sürüm).
- Üç site de tarayıcı olmayan isteklere Cloudflare doğrulaması döndürür (masaüstü/sunucudan `curl` 403 alır). Bu yüzden `Browser` gerçek bir `WebView` kullanır: sayfayı telefondaki tarayıcı gibi açar, küçük DOM betikleriyle okur, indirmeyi `DownloadListener` ile yakalar. Doğrulama 8 sn'de geçmezse ya da indirme kendiliğinden başlamazsa sayfa ekranda gösterilir, kullanıcı dokunur; sonrası otomatik sürer. Ana çerçeve gezintisi kaynağın kendi alan adları ve `.apk/.xapk/.apkm/.apks` bağlantılarıyla sınırlıdır (reklam yönlendirmeleri düşer).
- Bulma: APKPure arama → uygulama sayfası `/<slug>/<paket>/download` (sürüm: `data-dt-version` / JSON-LD / başlık; dosya: `d.apkpure.com/b/...`). APKMirror paket adıyla arama → uygulama başına en yeni kararlı sürüm → sürüm sayfasının varyant tablosu (APK > paket, telefonun mimarisi/universal, nodpi) → varyant sayfasında paket adı doğrulaması ve `downloadButton` → `download.php`. LiteAPKs uygulama adıyla arama → sayfada paket adı (Google Play bağlantısı) doğrulaması, "Version" satırı, `/download/…` zinciri. Paket adı "tam ad" olarak eşlenir (`org.app` ≠ `org.app.beta`).
- Kurulum: dosya Kolay'ın tarayıcı çerezleri/kullanıcı aracısıyla `cache/app-updates/` altına iner (yalnızca HTTPS, ≤ 2 GB). Düz APK ya da XAPK/APKM/APKS paketi açılır (mimari bölümleri yalnızca telefonun işlemcileri için alınır). Kontroller: paket adı aynı, `versionCode` yüklüden büyük, imza yüklü uygulamayla aynı (farklıysa Android zaten reddeder; mod/yeniden imzalı dosyalar bu yüzden kurulamaz). Tek `PackageInstaller` oturumu; dosyalar oturuma yazılır yazılmaz silinir, ekran açılış/kapanışında klasör temizlenir.
- Onay: Android, başka bir yükleyiciyle kurulmuş uygulamanın güncellemesi için onay ister. Android 12+'da `USER_ACTION_NOT_REQUIRED` istenir: Kolay bir kez güncelledikten sonra (kayıtlı yükleyici Kolay olur) sonraki güncellemeler onaysız geçebilir. Güncellemeler sırayla yapılır; ekranda tek onay olur.
- Arka planda otomatik kontrol yok (WebView ve doğrulama ön planda çalışır); "Kontrol et ve güncelle" takip edilen tüm uygulamaları sırayla kontrol edip yeni sürümü olanları kurar.

## Araç düzeni ve genel OCR (Host API 4)
- `Tool.compact()` true olan araçlar (QR, Fotoğraf Sıkıştır, YouTube Özeti) ana sayfada karo ızgarasının altında ince yatay şeritler olarak gösterilir.
- Tıbbi OCR: iki mod, anahtarla seçilir. Varsayılan "Tıbbi"; "Genel" mod yerel OCR sonucunu tıbbi bağlam varsaymadan gösterir, Kopyala yanında isteğe bağlı "Gemini ile temizle" (gevşek istem) sunar. OCR metni diske yazılmaz.
- Helal Sorgu: açılış modu barkod; barkod bulunamazsa otomatik etiket OCR'ına geçilmez, kullanıcı anahtarla "Etiket" moduna geçer.
- YouTube Özeti paylaşım hedefi: kabukta `activity-alias .YoutubeShare` ("YouTube Özet", `ACTION_SEND text/plain`); payload `Entry` paylaşılan bağlantıyı alıp özeti doğrudan başlatır.
