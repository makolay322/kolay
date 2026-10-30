# Arayüz sistemi

Hedef: "yerleşik, cilalı" his — az ama anlamlı hareket, tutarlı boşluk, açık/koyu tema. Her şey `payload/.../ui/` altında; ekranlar yalnızca bu parçaları birleştirir.

## Parçalar
| Sınıf | Görev |
|---|---|
| `Palette` | Renk belirteçleri (açık/koyu). Sistem temasından seçilir; tema değişince etkinlik yeniden kurulur. |
| `Ctx` | `host`, `a` (Activity), `p` (Palette), `nav`, `ui` (Handler), `io` (havuz), `dp()` |
| `Ui` | Metin/buton/yüzey fabrikaları, dokunma geri bildirimi, ortak hareket eğrileri ve yardımcıları |
| `Icon` | 24 birimlik ızgarada çizgi ikonlar (path); renk serbest, her boyutta keskin |
| `Nav` / `Screen` | Yığın + yönlü geçişler |
| `Header` | Geri düğmesi + başlık + değişen alt başlık |
| `Arc`, `Bar`, `Toggle` | Dönen yay yükleyici, kayan ilerleme çubuğu, yaylı anahtar |

## Renk belirteçleri
`bg` sayfa · `surface` kart · `surface2` girdi/ikincil düğme · `ink` ana metin · `muted` ikincil metin · `line` ayraç ·
`primary`/`onPrimary` vurgu · `primarySoft`/`onPrimarySoft` yumuşak vurgu (ikon kutuları, tonlu düğme) · `danger`.
Açık temada kartlar gölgeli, koyu temada ince çerçeveli (gölge koyuda okunmaz).

## Ölçü ve tipografi
- Köşe: kart 22 dp, düğme 16, girdi 14, küçük kutu 12. Sayfa kenar boşluğu 16–20 dp; kartlar arası 12 dp.
- Dokunma hedefi ≥ 44 dp (ikon düğmesi 44, ana düğmeler 48–56).
- Yazı: başlık 22–36 sp / 700, gövde 15–17 / 400–600, etiket 12 / 600 + harf aralığı. `Ui.text(c, metin, sp, renk, ağırlık)`.

## Hareket ilkeleri
| Durum | Hareket | Süre / eğri |
|---|---|---|
| Sayfa girişi (push) | sağdan kay + belir; eski sayfa hafifçe geri çekilip solar | 360 ms, `EMPH` (vurgulu yavaşlama) |
| Geri (pop) | tersi | 240–320 ms, `EXIT`/`EMPH` |
| Akış içi geçiş (replace) | çapraz solma + 0.96→1 ölçek | 340 ms |
| Liste girişi | aşağıdan 22 dp yüksel + belir, öğeler arası 35–55 ms kademe | 420 ms, `EMPH` |
| Dokunma | 0.97'ye küçül, bırakınca yay payıyla geri | 90 ms / 260 ms `POP` |
| Seçim (fotoğraf) | görsel 0.86'ya küçülür, çerçeve belirir, onay rozeti yaylanarak açılır | 260–300 ms `POP` |
| Metin değişimi | sönüp 5 dp kayarak yeni metin belirir (`Ui.swapText`) | 90 + 160 ms |
| Alt eylem çubuğu | seçim varken aşağıdan süzülür | 320 ms `EMPH` |
| Geri bildirim | `performHapticFeedback` (seçim: CLOCK_TICK, onay: CONFIRM) | — |

Kurallar: animasyon bilgi taşır, süs değildir; 450 ms'yi aşma; sistem animasyon ölçeği (geliştirici seçeneği) otomatik uygulanır;
giriş animasyonları yalnızca `onShown()` içinde başlar (geri dönüşte tekrarlanmaz).

## Yeni görsel parça eklerken
1. Renk için `Palette`'e belirteç ekle (açık + koyu), sabit hex kullanma.
2. Ölçüleri `c.dp()` ile ver.
3. Etkileşimli ise `Ui.surface` (ripple + gölge) ve `Ui.press` kullan.
4. İkon gerekiyorsa `Icon.Kind`'a path ekle (24'lük ızgara, 2 birim çizgi).
