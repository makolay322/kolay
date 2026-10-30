# Yeni araç ekleme

Bir araç = `Tool` uygulaması + en az bir `Screen`. Ana sayfa `Tools.ALL` listesini kartlara çevirir; kart animasyonu,
ikon kutusu, geçiş ve geri tuşu hazır gelir.

## 1. Araç sınıfı
`payload/src/main/java/app/kolay/payload/tools/<ad>/<Ad>Tool.java`:
```java
public final class NotTool implements Tool {
    @Override public String id() { return "not"; }                 // sabit, ekranda görünmez
    @Override public String title() { return "Hızlı Not"; }
    @Override public String subtitle() { return "Panoya hızlı metin"; }
    @Override public Icon.Kind icon() { return Icon.Kind.COPY; }   // ui/Icon.java'daki hazır ikonlar
    @Override public Screen open(Ctx c) { return new NotScreen(); }
}
```
## 2. Ekran
```java
final class NotScreen extends Screen {
    @Override public View create() {                 // c: Ctx — tema, host, nav, dp(), arka plan havuzu
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(new Header(c, "Hızlı Not", "…", () -> c.nav.pop()).view);
        // … içerik: Ui.text / Ui.button / Ui.surface …
        return page;
    }
    @Override public void onShown() { /* giriş animasyonları: Ui.appear(view, gecikmeMs) */ }
}
```
## 3. Kaydet
`Tools.java` içindeki `ALL` dizisine `new NotTool()` ekle. Bitti.

## Ekranlar arası geçiş
| Amaç | Çağrı | Hareket |
|---|---|---|
| Yeni sayfa aç | `c.nav.push(screen)` | sağdan kayarak girer |
| Geri dön | `c.nav.pop()` | sağa kayarak çıkar |
| Akış içinde değiştir | `c.nav.replace(screen)` | çapraz solma + hafif ölçek |
Geçiş sürerken gelen istek kuyruğa alınır (son istek kazanır). Geri tuşu: ekranda `onBack()` `true` dönerse tüketilir, yoksa `pop()`.

## Kurallar
- Renk/ölçü sabit yazma: `c.p` (Palette) ve `c.dp()`; açık/koyu tema otomatik uyar. ([UI.md](UI.md))
- Uzun işleri `c.io` havuzunda yap, arayüze `c.ui.post(...)` ile dön. Ekran yok edilince (`onDestroy`) işi iptal et / sonucu yok say.
- Sır gerekiyorsa `c.host.secret("ad")` / Ayarlar'a bir giriş alanı ekle. **Kodda veya depoda sır tutma.**
- Çıktıyı/kullanıcı verisini diske veya loga yazma (gerekçeli ve belgelenmiş istisna yoksa).
- Yeni izin gerekiyorsa `shell/src/main/AndroidManifest.xml`'e eklenir → bu **kabuk değişikliğidir** (yeni `Kolay.apk` elle yüklenir). Mümkünse mevcut izinlerle çöz.
- Yeni yerel kütüphane (`.so`) de kabuğa girer; payload saf Java kalır.
- Yeni bir `Host` yeteneği gerekiyorsa: `sdk` `Host`'a metot ekle, `Host.API` + `Config.SHELL_API` + modül `app.kolay.api` artır, önce kabuğu dağıt ([OPERATIONS.md](OPERATIONS.md#kabuk-sürümü-yayınlama-nadir)).

## Yayın
`git commit` → `tools/release.sh`. Telefonda *Şimdi denetle → Şimdi yenile*.
