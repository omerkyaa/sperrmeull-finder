# Cursor Rules — SperrmüllFinder (Kotlin • Compose • Firebase)

## 0) Genel İlke & Kapsam

### Platform
- **Android yalnız** (telefon + küçük tablet), minSdk 24+, targetSdk = latest stable
- **Dil/Stack**: Kotlin (JDK 17), Jetpack Compose + Material 3, Navigation Component, Coroutines + Flow, Hilt DI
- **Firebase**: Auth, Firestore, Storage, Remote Config, Analytics, Crashlytics, FCM. App Check (Play Integrity) açık
- **Harita & Kamera**: Google Maps + Play Services Location (clustering), CameraX
- **Görüntü**: Glide (Compose için Landscapist-Glide)

### SDK Versiyonları (Kod ile Uyumlu)
**KRITIK: Kod her zaman bu SDK versiyonlarına göre yazılacak. SDK değişikliği yapılmadan önce rules.md güncellenmeli.**

**Core Android:**
- Kotlin: 1.9.20
- Android Gradle Plugin: 8.13.0
- compileSdk: 34, minSdk: 24, targetSdk: 34

**Jetpack Compose & UI:**
- Compose BOM: 2023.10.01
- Compose Compiler: 1.5.5
- Material3: 1.1.2 (PullToRefresh experimental/unavailable in this version)
- Activity Compose: 1.8.2
- Navigation Compose: 2.7.5

**Firebase BOM & Services:**
- Firebase BOM: 32.6.0 (Auth, Firestore, Storage, Analytics, Crashlytics, FCM, Remote Config, App Check)

**Premium & Monetization:**
- RevenueCat: 7.3.5 (callback-based APIs, ReceiveCustomerInfoCallback, LogInCallback, PurchaseCallback interfaces)
- RevenueCat Package API: packageInfo.identifier (NOT storeProduct.id) - storeProduct API not available in 7.3.5
- Purchase Flow: UI Layer → ViewModel → Repository.purchaseWithActivity(activity, productId) → RevenueCatManager
- Implementation: Activity context required for all purchases; Repository methods guide to proper WithActivity methods

**Maps & Location:**
- Play Services Maps: 18.2.0
- Play Services Location: 21.0.1
- Maps Compose: 4.3.0

**Camera:**
- CameraX: 1.3.1
# ML Kit will be added in future updates

**Networking & Data:**
- OkHttp: 4.12.0
- Retrofit: 2.9.0
- Room: 2.6.1
- Kotlinx Serialization: 1.6.1

**Image Loading:**
- Landscapist Glide: 2.2.10
- Glide: 4.16.0

**Dependency Injection:**
- Hilt: 2.48.1
- Hilt Navigation Compose: 1.1.0

**Coroutines & Async:**
- Kotlinx Coroutines: 1.7.3

**Premium & Monetization:**
- RevenueCat: 9.21.0 (Modern API with Coroutines & Flow)
- RevenueCat UI: 9.21.0 (Native Paywalls & Customer Center)

**Other Libraries:**
- Paging: 3.2.1
- WorkManager: 2.9.0
- Accompanist Permissions: 0.32.0

### Monetizasyon: Ödeme Altyapısı

**RevenueCat Entegrasyonu:**
- **SDK Version**: 9.21.0 (Modern Coroutines + Flow API)
- Tüm satın alma akışları RevenueCat üzerinden yürütülecek
- Google Play Billing (GPB) sadece cihazdaki faturalama katmanı olarak kullanılır
- Doğrulama, entitlement ve durum takibi RevenueCat tarafından yapılır
- **Entitlement adı**: `SperrmuellFinder Premium` (tek doğruluk kaynağı)
- UI ve özellik kapıları yalnızca entitlement durumuna göre açılır/kapanır
- **Native Paywalls**: RevenueCat UI library ile Dashboard'dan kontrol edilen paywall'lar
- **Customer Center**: Self-service subscription management (iptal, plan değiştirme, fatura görüntüleme)

**Ürün & Paket Yönetimi:**
- SKU/Package tanımları RevenueCat Dashboard'da yönetilir
- Uygulama dashboard'daki ürünleri dinamik çeker (sabit ürün listesi hard-code yok)
- **Product IDs (Google Play SKU'ları)**:
  - Premium subscriptions: `weekly`, `monthly`, `yearly`
  - XP paketleri: `xp_500`, `xp_1500` (optional, gelecekte eklenebilir)
  - Post extension: `post_extend_6h` (optional, gelecekte eklenebilir)
- **Offering ID**: `default` (RevenueCat Dashboard'da yapılandırılır)
- Deneme/İntro fiyatları (free trial / intro offer) desteklenir
- Gösterimler RevenueCat'ten gelen fiyat/metinle yapılır (yerelleştirilmiş)
- **Önerilen Fiyatlar**: Weekly €2.99, Monthly €9.99, Yearly €79.99

**Durum Senkronizasyonu:**
- **RevenueCat → App**: Entitlement değişimleri realtime Flow ile dinlenir ve PremiumManager tekil kaynak olarak güncellenir
- **App → Firestore**: Entitlement aktif ise `users/{uid}`'de `ispremium=true`, `premiumuntil` güncellenir; pasif olursa `ispremium=false` yapılır. Firestore bilgilendirme amaçlıdır, karar vermez
- **Restore Purchases**: "Satın alımları geri yükle" akışı RevenueCat ile yapılır; sonuç UI'de i18n metinlerle bildirilir
- **Modern API**: Coroutine-based (suspendCancellableCoroutine), Flow-based updates, PurchaseParams API

**Edge Case / Hata Politikası:**
- **Kaynak önceliği**: RevenueCat (1) → GPB sinyali (2) → Firestore alanı (3, sadece gösterim)
- **Ağ/hizmet kesintisinde**: mevcut entitlement graceful degrade (önbellek + kısa süreli erişim), netleşince durum yeniden senkronize edilir
- Para iadesi / iptal / fatura sorunları RevenueCat webhooks'la ele alınır; uygulama seviyesinde entitlement düşüşü UI'ye anında yansıtılır

**Analytics & Bildirim:**
- **Satın alma olayları**: `purchaseSuccess(productId,type)`, `premiumActivated(period)`, `purchaseRestore`
- **Başarılı Premium/Xp alımlarında**: `purchases/{uid}/{purchaseId}` dokümanı yazılır ve `notifications/{uid}`'e in-app bildirim düşülür (push opsiyonel)

**Gating Kuralları (Özet):**
- **Premium kapıları**: sınırsız harita/arama, availability yüzdeleri, premium filtreler, favori bölge/kategori uyarıları, 10 dk erken erişim, arşiv tam detay
- **Basic sınırlamaları**: 1.5 km yarıçap, availability yüzdeleri gizli, arşiv thumbnail-only, sınırlı filtre/uyarı

**Do/Don't (Cursor'a talimat):**

**DO:**
- RevenueCat SDK 9.21.0 ile entitlement'ı tek doğruluk kaynağı yap
- Modern Coroutines + Flow API kullan (suspendCancellableCoroutine, StateFlow)
- Ürün/fiyat/metinleri dashboard'dan dinamik çek ve strings i18n ile UI'ye yansıt
- PremiumManager + RevenueCatManager koordineli çalışsın; duplicated state yazma
- Entitlement değişiminde yalnız UI kapıları tetikle; iş kuralları Firestore alanlarına bakmasın
- Native Paywalls (RevenueCat UI) ve Customer Center kullan
- Real-time Flow updates ile instant entitlement changes

**DON'T:**
- GPB yanıtına bakarak premium açma (doğrulama her zaman RevenueCat)
- Ürün listelerini/ID'leri hard-code etme
- Premium durumunu yalnız Firestore'a göre belirleme
- Callback-based API kullanma (modern coroutine API tercih et)
- Old SDK patterns (7.x) kullanma

**Remote Config (opsiyonel):**
```
feature_premium_enabled = true
paywall_ab_test_bucket = "control|variant_a|variant_b"
rc_show_xp_packs = true
```

**i18n:**
- Tüm satın alma/paywall/restore/hata metinleri `values-de/strings.xml` (default) ve `values-en/strings.xml`'e eklenecek; hard-coded metin yok

**Güvenlik:**
- Satın alma akışında uygulama token/gizli anahtar tutmaz; RevenueCat server-side doğrulama yapar
- App Check (Play Integrity) açık; şüpheli cihazlarda satın alma akışı engellenebilir (feature flag)

### Genel Kurallar
- **Yerelleştirme**: DE varsayılan, cihaz dili Almanca değilse EN fallback. Hard-coded metin yasak
- **Tasarım**: Modern M3; kart-tabanlı, oval köşeler, hafif gölgeler, açık/koyu mod; erişilebilirlik (TalkBack, kontrast, 48dp hit target)
- **Kalite**: Üretim kalitesinde, crash-free ana akış, clean architecture, test edilebilir

**Bu dosya kod içermez; Cursor'dan talep: tam, hatasız, modüler, tekrar etmeyen proje çıktısı.**

---

## 1) Mimarî & Modüller

### Katmanlar
- **data** (remote/local) → **domain** (use-case) → **ui** (ViewModel + Compose ekranlar)

### Modüller (paketleme örneği)
- **data/** (repositories, firebase sources, room, dtos, mappers)
- **domain/** (entities, usecases, repository interfaces)
- **ui/** (screens: launch, auth, home, map, camera, profile, notifications, search, settings, premium)
- **managers/** (PremiumManager, LevelManager, ConfigManager/RemoteConfigManager, NotificationTokenHelper, HonestyManager, LocationManager, MLKitManager, UploadManager, ModerationManager, MapMarkerManager, XPManager, WorkerManager)
- **core/common/** (Result, error, logger, extensions, ValidationUtils, TimeUtils, MarkerUtils, GeoHashUtils, ImageUtils)

### Navigation
- Single-Activity, multi-fragment/Compose destinations, SafeArgs/deeplink
- Sekmeler arası state korunur (scroll, filtre, harita konumu)

---

## 2) Uygulama Akışı (Sabit)

**Ana Akış:**
- Launch → Auth (Login/Register/Forgot) → Home (BottomNav: Home, Map, Camera, Profile)

**Ekran Detayları:**
- **Home**: üst küçük hoşgeldiniz ekranı (birkaç saniye sonra kaybolur), altında yakına göre sıralı feed (Paging 3, pull-to-refresh)
- **Map**: tam ekran cluster, premium marker stilleri + filtre çubuğu
- **Camera**: açılır açılmaz CameraX; 1–3 foto; ML blur + kategori; GPS otomatik (manuel değişmez); Storage → Firestore; başarıda Home'da en üste kendi gönderisi
- **Profile**: XP/Level/Honesty, premium rozet/çerçeve, gönderiler ve arşiv (Basic: sadece thumbnail, Premium: tam detay)
- **PostDetail**: carousel, beğeni/yorum realtime, rapor, premium "Still there?/Taken" oylaması (yüzde sadece Premium'a görünür)
- **Diğer**: Notifications / Search / Followers / UserProfile / Settings + Premium sayfaları dâhil

---

## 3) Firestore Şeması (özet, küçük harf)

### Koleksiyonlar minimum:

**users/{uid}:**
- `displayname`, `email`, `photourl`, `city`, `dob`, `gender`
- `ispremium`, `premiumuntil`, `xp`, `level`, `honesty`
- `badges[]`, `favorites{regions[],categories[]}`
- `created_at`, `updated_at`, `device_tokens[]`, `frame_level`
- **Field Mapping**: Firestore fields (lowercase) ↔ Kotlin properties (camelCase) via @PropertyName

**posts/{postid}:**
- `ownerid`, `images[]`, `description`, `location{lat,lng}`, `city`
- `created_at`, `expires_at(+72h)`, `category_en[]`, `category_de[]`
- `availability_percent(100)`, `likes_count`, `comments_count`
- `status("active"|"archived"|"removed")`, `views_count`

**Diğer Koleksiyonlar:**
- `comments/{commentid}` (veya posts/{postid}/comments)
- `followers/{uid}/following/{targetUid}`, `followers/{uid}/followers/{sourceUid}`
- `reports/{reportid}`: type("post"|"comment"|"user"), targetid, reporterid, reason, created_at, status("open"|"approved"|"dismissed"), admin_notes
- `xp_transactions/{uid}/{txid}`: delta, reason, created_at, premium_bonus_applied, level_before, level_after
- `availability_votes/{postid}/{voteid}`: voterid, vote("still_there"|"taken"), created_at, judged_by_admin?, honesty_delta?
- `notifications/{uid}/{notifid}`: type, title, body, deeplink, created_at, read
- `device_tokens/{uid}/{tokenid}`: token, platform, granted, created_at, updated_at
- `moderation_queue/{entryid}`, `admin_logs/{logid}`, `post_views/{postid}/{viewerid}`
- `purchases/{uid}/{purchaseid}`, `badges/{badgeid}`, `user_badges/{uid}/{badgeid}`

**Not:** `expires_at` ile 72 saatte auto-archive (Cloud Functions veya WorkManager)

---

## 4) Premium • XP • Honesty Kuralları

### Premium Sınırlamaları
- **Basic**: Map/Search 1.5km yarıçap limiti; availability yüzdeleri gizli; arşiv thumbnail only; kısıtlı filtre/notification
- **Premium**: sınırsız mesafe; availability yüzdeleri + filtreler; favori bölge/kategori bildirimi; yeni postları 10 dk önce görme; marker bonusları (renk/animasyon/boyut)

### XP Sistemi
- **Formül (kümülatif)**: 50 + (level × level × 100); Level 2 başı 100 XP
- **İlk kayıt**: 100 xp verilir, kişi direk level 1 olarak başlar
- **Sonraki seviyeler**: kümülatif olarak devam et. Gereken xp tamamlandığında leveli arttır

### XP Kazançları
- post +50, daily +10, like +15, comment_received +20, comment_write +15
- share_external +25, leaderboard (500/250/150), premium_task +500

### Premium XP Boost
- Lv1–4: +5%, Lv5–9: +7%, Lv10–14: +10%, Lv15+: +20%

### Honesty Sistemi
- **Başlangıç**: 100
- **Paylaşım yasağı**: <30 iken (admin ban 3g/1h/1a/3a/süresiz)
- **Kazançlar**: 72 saat şikâyetsiz post +5; onaylı şikâyet −20; availability doğru oy +2 / yanlış −3
- **Liderlik**: haftalık Top10 liste (Premium kendi sırasını görebilir, Basic göremez)

---

## 5) Monetizasyon

### Premium Paketleri
- **Premium**: €2.99/1 ay (+800 XP + bildirim)
- **Premium Plus**: €3.99/1 ay (+badge +1500 XP)

### XP Paketleri
- €0.99 → +500 XP
- €1.49 → +1500 XP + badge

### Post Ömrü Uzatma
- **Premium**: +6 saat ücretsiz
- **Basic**: €0.99 vererek uzatabilir

### UI Kuralları
- "Upgrade to Premium" animasyonlu CTA yalnız Basic'te görünür
- Satın alma sonrası: purchases + in-app notifications + XP/Premium grant

---

## 6) Remote Config Varsayılanları

```
feature_premium_enabled = true
basic_radius_meters = 1500
early_access_minutes = 10
availability_penalty_step = 20
max_post_images = 3
post_expire_hours = 72
free_premium_extend_hours = 6

// Search Configuration
search_enabled = true
search_page_size = 20
search_debounce_ms = 500
allow_availability_filter = true
search_max_hours_back = 72

xp_share_post = 50
xp_daily_login = 10
xp_like_received = 15
xp_comment_received = 20
xp_comment_write = 15
xp_share_external = 25
xp_premium_task = 500

xp_boost_lvl1_4 = 0.05
xp_boost_lvl5_9 = 0.07
xp_boost_lvl10_14 = 0.10
xp_boost_lvl15p = 0.20
```

---

## 7) Bildirim & Token

### NotificationTokenHelper
- İzin akışı, token kayıt/yenileme; device_tokens senkron

### Tetikler
- Yeni yorum/like, takip, premium bitiş, XP/Premium satın alımı
- Görev, favori bölgede yeni post (Premium)

---

## 8) Harita & Marker

### Premium Marker Özellikleri
- **Çerçeve seviyesine göre renk**: Gold (1-15 level arası premiumlar) / Crystal (Lv15+ Lottie animasyonlu)
- **Premium marker**: %10 büyük ve subtle bounce
- **Basic marker**: soluk gri

### Performans
- Cluster renderer performanslı; metin boyutu/densite dimen ile

---

## 9) Moderasyon & ML Gizlilik

### ML Kit Entegrasyonu
Not: ML Kit entegrasyonu ve blur özellikleri gelecek güncellemede eklenecektir.

### Rapor Akışı
- Post/yorum; admin panelden kaldır/ban (3g/1h/1a/3a/süresiz)
- moderation_queue ve admin_logs kayıtları
- Yanlış rapor onaylanırsa muhataba Honesty −; doğruysa +

---

## 10) Branding & Icons (Rules) — Android logosu yok, yalnız uygulama logosu

### Zorunlu Varlıklar (drawables & mipmap)
- **drawable/app_logo** → ana uygulama logosu (vektör önerilir). Tüm büyük görsellerde, splash/sayfa başlıklarında ve bildirim largeIcon'da bu kullanılacak
- **drawable/ic_notification_app_logo** → bildirim küçük ikon için tek renk (beyaz) silüet sürüm (Android'in küçük ikon yönergesi gereği; arka planı şeffaf)
- **mipmap-anydpi-v26/ic_launcher (adaptive)** → launcher icon. Foreground: app_logo; Background: markalı arka plan (uyumlu renk/gradient)
  - android:icon ve android:roundIcon yalnızca @mipmap/ic_launcher'ı işaret eder

### Kullanım Kuralları
**Android robot/varsayılan ikon kullanılmayacak.**

**Bildirimlerde:**
- smallIcon = R.drawable.ic_notification_app_logo (tek renk)
- largeIcon = R.drawable.app_logo
- Kanal/özet bildirimler, foreground service bildirimleri dahil her yerde aynı kural

**App içi (Toolbar, boş ekranlar, paywall, ayarlar, about):** app_logo

**Splash/Launch ekranı:** merkezde app_logo (uygun boyut), marka rengi arka plan

**ShareSheet/Deep Link önizlemeleri:** mümkünse app_logo

### İsimlendirme & Metin
- Uygulama adı her yerde string resources'tan (@string/app_name) çekilir — sabit metin yok
- Bildirim başlık/gövde metinleri DE/EN i18n ile (values-de / values-en)

### Kalite & Erişilebilirlik
- app_logo vektör veya yüksek çözünürlük (xxxhdpi+), kontrast ve koyu/açık temada görünür
- ic_notification_app_logo tek renk (beyaz), 24×24dp optik hizalı; küçük ekranlarda seçilebilirlik yüksek

### CI/QA Kontrolü
- Manifest'te android:icon / android:roundIcon = @mipmap/ic_launcher (Android varsayılanı yasak)
- Bildirim üretiminde hiçbir yerde ic_launcher veya Android varsayılan ikon kullanılmıyor
- FCM remote notification payload'larında smallIcon override edilmeyecek; app tarafı her zaman ic_notification_app_logo'yı ayarlayacak
- **Marka tutarlılığı**: Ayarlar, Bildirimler, Foreground Service, Splash, Paywall, About sayfalarında app_logo görsel doğrulaması

---

## 11) Güvenlik (özet)

### Erişim Kuralları
- Kullanıcı yalnız kendi `users/{uid}`, `notifications/{uid}`, `xp_transactions/{uid}`, `purchases/{uid}` okuyabilir
- posts okuma public; yazma yalnız oturum sahibi
- availability_votes: kullanıcı başına tek oy; admin görür
- xp_transactions ve Premium grant yalnız Functions/sunucu yazar
- Storage: yalnız kendi dosyalarına yaz; post görselleri public okuma (moderasyon uygun ise)

---

## 12) Performans

### Optimizasyonlar
- **Büyük görsellerde**: compression + WEBP/AVIF, thumbnail() ve önbellek
- **Liste/akış**: Paging 3, DiffUtil; Firestore sorguları indeksli + limitli
- **Harita**: viewport-bound sorgular; bellek/disk LRU; sızıntı yok (lifecycle aware)
- **Ağ**: OkHttp caching + gzip. Crashlytics ve Analytics etkin

---

## 13) Test & QA

### Test Türleri
- **Unit**: Level/XP formülü, premium boost, availability yüzdesi
- **Instrumented**: Kamera→ML→Upload→Publish; Map radius gating; Arşiv görünürlük
- **UI (Compose testing/Espresso)**: Home ilk kart, realtime sayaçlar, paywall yönlendirmeleri

### QA Checklist (özet)
- Dil seçimi (DE→EN fallback)
- Kamera 1–3 foto + ML blur + kategori + otomatik konum
- Map sınırları (Basic vs Premium)
- Arşiv (Basic thumb/Premium detay)
- Availability bar sadece Premium'a
- XP/Honesty kuralları, satın alma kayıtları
- FCM token akışı, happy-path crash=0

---

## 14) Proje Yapılandırması

### Versiyonlar
- Version Catalog (libs.versions.toml), Compose BOM
- "latest stable" hedef; sabit sürüm pinleme

### Kaynaklar
- **values-de/strings.xml** (default) ve **values-en/strings.xml** (fallback)
- Tüm UI metinleri strings; plurals/datetime yerelleştirme

### Tema & Renkler
- purple_dark, chip_gray gibi ek anahtarlar
- Metin boyutları ve marker cluster dimen'ları

### Gizlilik & Hukuk
- DSGVO, community-moderation notları, rapor/ban prosedürleri

### Docs
- README.md, PRD.md, SECURITY_RULES.md, **data.md** (Firestore değişiklik günlüğü)
- **Kural**: Firebase şema/alan değişiklikleri her committe data.md'ye işlenecek; aksi kabul edilmez

---

## 15) Compose & UI Kuralları (çok önemli)

### Compose M3 Kuralları
- **Compose M3** (XML yerine, xml artık hiç yok, sakın xml yapma). Tüm ekranlar Compose ile
- State tek kaynaktan (ViewModel); StateFlow/UiState
- Loading/Empty/Error durumları her listede; shimmer/empty state
- **No hard-coded string**; tüm metinler stringResource(id = R.string.*)
- Landscapist-Glide görüntü yükleme; placeholder/error görselleri; Coil kullanma, Landscapist kullan
- Deterministik layout: animasyonlar sorunsuz; frame drop yok
- **Erişilebilirlik**: contentDescription, semantics, kontrast

---

## 16) Cursor-Spesifik "DO/DON'Ts"

### DO
- Tek seferde tam ve hatasız dosya üret (eksiksiz importlar, derlenebilir)
- Aynı dosyayı ikinci kez üretme. Değişiklik gerekiyorsa yalnız diff/patch uygula (mevcut isimleri koru)
- Yeni feature için: Interface (domain repo) → UseCase → Data repo impl → ViewModel → UI (Compose) zinciri
- İzin akışlarında rationale açıklaması + ayarlara yönlendirme
- Remote Config anahtarlarını UI'dan oku, mantıklı varsayılan ver
- Room/Firestore mapping tutarlı; TypeConverter'lar ekli
- ViewModel'lerde basit Flow yapıları kullan; complex nested flows yerine MutableStateFlow + flatMapLatest

### DON'T
- Kodu çoğaltma / aynı model, adapter, utils'i tekrar yazma
- Hard-coded string kullanma (log/exception mesajları dâhil)
- Kararsız/deneysel API seçme; stable sürümler tercih et
- Çökme riskli yaklaşımlardan kaçın (null güvenliği, lifecycle leaks)

---

## 17) Kabul/Bitirme Kriterleri

### Ana Akış Kriterleri
- Auth → Home/Map/Camera/Profile kesintisiz çalışır
- Basic/Premium gating her ekran için doğru
- ML blur + kategori kararlı, crash yok
- Postlar 72 saatte otomatik arşiv
- XP/Level/Honesty görselleri ve mantığı doğru; satın alma & bildirim kayıtları Firestore'da eksiksiz
- DE/EN kaynaklar tam; hiçbir sabit metin yok
- **Crashlytics**: fatal=0 (happy-path)

---

## 18) Varsayılan "İstek Şablonu" (Cursor için)

### "Şu feature'ı üret":
1. Interface + UseCase (domain)
2. Repository impl (data, Firestore/Room)
3. ViewModel (State/Effect, Flow)
4. Compose ekran(lar) (loading/empty/error dahil)
5. Nav graph güncellemesi (deeplink varsa)
6. Strings (DE/EN) eklenmesi
7. Remote Config okuması (varsa)
8. Analytics event(ler)
9. Security rules notu
10. Tests (unit + gerekirse UI)

---

## 19) Ek Notlar

### Özel Kurallar
- **Kamera**: galeri kapalı; yalnız çekim ve maksimum 3 foto eklenebilir
- **Konum**: otomatik GPS; manuel değişim yok
- **Kategoriler**: içerde EN, UI'da DE; category_en[] + category_de[] yaz
- **Arşiv**: Basic → küçük grid (3 sütun) sadece thumbnail; Premium → tam detay, istatistik, yorum/like
- **Availability**: "Taken" oyu availability_penalty_step kadar düşürür (varsayılan 20)
- **Search (Premium Gated)**: Basic kullanıcılar UI görür ama kullanamaz; tüm etkileşimler paywall açar; Premium tam özellikli arama + filtreler

---

## Özet Tek Kural

**Tekrarsız üret, Compose + M3 + en yeni stable sürümler kullan, tüm metinleri strings'e koy (DE/EN), premium/basic kapılarını doğru uygula, crash'e yol açabilecek tasarımlardan uzak dur, her yeni özellikte domain→data→ui zincirini ve testleri eksiksiz kur.**
