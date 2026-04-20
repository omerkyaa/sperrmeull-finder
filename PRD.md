# Product Requirements Document (PRD) - SperrmüllFinder

## Proje Özeti
**SperrmüllFinder** - Sperrmüll (kaldırım atığı) paylaşım ve keşif platformu. Kullanıcılar bulundukları bölgedeki ücretsiz eşyaları paylaşabilir, keşfedebilir ve Premium özelliklerle gelişmiş deneyim yaşayabilir.

**Platform**: Android (Kotlin + Jetpack Compose + Firebase)  
**Hedef SDK**: 34 (minSdk 24)  
**Mimari**: Clean Architecture (Data → Domain → UI)  

## YAPILANLAR LİSTESİ

### ✅ **TAMAMLANAN GÖREVLER (94/177 - %53.1)**

#### 🏗️ **1. PROJE KURULUMU VE ALTYAPI (15/15 - %100)**

##### 1.1 Proje Yapılandırması (5/5 - %100) ✅
- [x] **1.1.1** Android Studio projesi oluştur (Kotlin + Compose) ✅
- [x] **1.1.2** Version Catalog (libs.versions.toml) kurulumu ✅  
- [x] **1.1.3** Build.gradle konfigürasyonu (Compose BOM, Hilt) ✅
- [x] **1.1.4** Modül yapısı oluştur (app, data, domain, core) ✅
- [x] **1.1.5** Gradle scripts optimizasyonu ve ProGuard kuralları ✅

##### 1.2 Firebase Entegrasyonu (5/5 - %100) ✅
- [x] **1.2.1** Firebase projesi oluştur ve google-services.json ekle ✅
- [x] **1.2.2** Firebase Auth, Firestore, Storage SDK'ları ekle ✅
- [x] **1.2.3** Firebase Remote Config, Analytics, Crashlytics ekle ✅
- [x] **1.2.4** Firebase App Check (Play Integrity) yapılandırması ✅
- [x] **1.2.5** FCM (Firebase Cloud Messaging) kurulumu ✅


### 🔐 **2. KİMLİK DOĞRULAMA SİSTEMİ (8/8 - %100)** ✅

#### 2.1 Auth Domain Katmanı (3/3) ✅
- [x] **2.1.1** AuthRepository interface ve User entity ✅
- [x] **2.1.2** LoginUseCase, RegisterUseCase, LogoutUseCase ✅
- [x] **2.1.3** AuthState ve auth event handling ✅

#### 2.2 Auth Data Katmanı (2/2) ✅
- [x] **2.2.1** FirebaseAuthDataSource implementasyonu ✅
- [x] **2.2.2** AuthRepositoryImpl ve user data mapping ✅

#### 2.3 Auth UI Katmanı (3/3) ✅
- [x] **2.3.1** AuthViewModel ve UiState management ✅
- [x] **2.3.2** Login/Register/ForgotPassword Compose ekranları ✅
- [x] **2.3.3** Auth navigation flow ve validation ✅

### 👤 **3. KULLANICI PROFİLİ VE SEVIYE SİSTEMİ (14/14 - %100)** ✅

#### 3.1 User Domain (4/4 - %100) ✅
- [x] **3.1.1** User entity (XP, Level, Honesty, Premium status) ✅
- [x] **3.1.2** UserRepository interface ve use cases ✅
- [x] **3.1.3** XPManager: Level hesaplama algoritması ✅
- [x] **3.1.4** HonestyManager: Dürüstlük puanı sistemi ✅

#### 3.2 User Data (3/3 - %100) ✅
- [x] **3.2.1** FirebaseUserDataSource (Firestore users koleksiyonu) ✅
- [x] **3.2.2** UserRepositoryImpl ve data mapping ✅
- [x] **3.2.3** XP transactions ve user_badges koleksiyonları ✅

#### 3.3 Profile UI (7/7 - %100) ✅
- [x] **3.3.1** ProfileViewModel ve profile state management ✅
- [x] **3.3.2** Profile screen (XP/Level/Honesty gösterimi) ✅
- [x] **3.3.3** Premium badge ve frame gösterimleri ✅
- [x] **3.3.4** User posts grid (thumbnail/full preview toggle) ✅
- [x] **3.3.5** Settings ve profile edit sayfaları ✅
- [x] **3.3.6** Pinterest-style grid layout ve archive functionality ✅
- [x] **3.3.7** UserProfileScreen (diğer kullanıcılar için) ✅

---------

## YAPILACAKLAR LİSTESİ

---

### 🗺️ **6. HARİTA VE KONUM SİSTEMİ (12/12 - %100)** ✅

#### 6.1 Map Domain (4/4 - %100) ✅
- [x] **6.1.1** LocationRepository interface ve entities ✅
- [x] **6.1.2** MapUseCase: Post filtering by location ✅
- [x] **6.1.3** LocationManager: GPS ve permission handling ✅
- [x] **6.1.4** GeoHashUtils: Efficient location queries ✅

#### 6.2 Map Data (3/3 - %100) ✅
- [x] **6.2.1** GoogleMapsDataSource integration ✅
- [x] **6.2.2** Location-based Firestore queries ✅
- [x] **6.2.3** Clustering algorithm implementation ✅

#### 6.3 Map UI (5/5 - %100) ✅
- [x] **6.3.1** MapViewModel ve location state ✅
- [x] **6.3.2** Google Maps Compose integration ✅
- [x] **6.3.3** Custom marker design (Basic/Premium styles) ✅
- [x] **6.3.4** Cluster rendering ve performance optimization ✅
- [x] **6.3.5** Filter bar ve radius controls ✅

### 💳 **4. PREMIUM SİSTEMİ VE REVENUECAT (15/15 - %100)** ✅

#### 4.1 Premium Domain (5/5 - %100) ✅
- [x] **4.1.1** PremiumRepository interface ve entities ✅
- [x] **4.1.2** PurchaseUseCase, RestorePurchasesUseCase ✅
- [x] **4.1.3** PremiumManager: Entitlement state management ✅
- [x] **4.1.4** Premium gating logic (Basic vs Premium features) ✅
- [x] **4.1.5** XP boost calculation (level-based multipliers) ✅

#### 4.2 RevenueCat Integration (5/5 - %100) ✅
- [x] **4.2.1** RevenueCat SDK kurulumu ve konfigürasyonu ✅
- [x] **4.2.2** RevenueCatManager: Product fetching ve purchase flow ✅
- [x] **4.2.3** Entitlement listener ve real-time sync ✅
- [x] **4.2.4** Purchase validation ve error handling ✅
- [x] **4.2.5** Restore purchases implementation ✅

#### 4.3 Premium UI (5/5 - %100) ✅
- [x] **4.3.1** PremiumViewModel ve purchase state ✅
- [x] **4.3.2** Paywall screen (paketler ve fiyat gösterimi) ✅
- [x] **4.3.3** Purchase success/error handling ✅
- [x] **4.3.4** Premium feature unlock animations ✅
- [x] **4.3.5** "Upgrade to Premium" CTA placements ✅

### 📱 **5. ANA EKRANLAR VE NAVİGASYON (23/23 - %100)** ✅

#### 5.1 Launch ve Onboarding (3/3 - %100) ✅
- [x] **5.1.1** Splash screen (app logo, brand colors) ✅
- [x] **5.1.2** Onboarding flow (3-4 sayfa: features, permissions) ✅
- [x] **5.1.3** Initial user setup (şehir seçimi, izinler) ✅

#### 5.2 Home Screen (7/7 - %100) ✅
- [x] **5.2.1** HomeViewModel ve post feed state ✅
- [x] **5.2.2** Hoşgeldiniz banner (otomatik kaybolma) ✅
- [x] **5.2.3** Post feed (Paging 3, pull-to-refresh) ✅
- [x] **5.2.4** Post card design (images, description, distance) ✅
- [x] **5.2.5** Premium vs Basic radius filtering ✅
- [x] **5.2.6** Basic search functionality (redirect to Search screen) ✅
- [x] **5.2.7** Real-time Firebase data loading with profile images ✅

#### 5.3 Search Screen - Premium Gated (8/8 - %100) ✅
- [x] **5.3.1** SearchViewModel ve advanced filtering state ✅
- [x] **5.3.2** Premium gated search interface (Basic: locked UI + paywall) ✅
- [x] **5.3.3** Advanced filters (category, city, radius, availability, time range) ✅
- [x] **5.3.4** Search results with Paging 3 (Premium only) ✅
- [x] **5.3.5** Sort options (nearest, newest, most liked, most commented) ✅
- [x] **5.3.6** Debounced search with query persistence ✅
- [x] **5.3.7** Empty/Error/Loading states with premium messaging ✅
- [x] **5.3.8** Paywall integration for all Basic interactions ✅

#### 5.4 Bottom Navigation (4/4 - %100) ✅
- [x] **5.4.1** BottomNavigation component (Home, Search, Map, Camera, Profile) ✅
- [x] **5.4.2** Navigation state preservation ✅
- [x] **5.4.3** Premium indicators ve access control ✅
- [x] **5.4.4** Deep linking support ✅

#### 5.5 Post Detail (5/5 - %100) ✅
- [x] **5.5.1** PostDetailViewModel ve realtime updates ✅
- [x] **5.5.2** Image carousel (1-3 photos, pinch-to-zoom) ✅
- [x] **5.5.3** Like/Comment system (realtime counts) ✅
- [x] **5.5.4** "Still there?" voting (Premium feature) ✅
- [x] **5.5.5** Report functionality ✅

---

### 🗺️ **6. HARİTA VE KONUM SİSTEMİ (12/12 - %100)** ✅
#### 6.2 Map Data (3/3 - %100) ✅
- [x] **6.2.1** GoogleMapsDataSource integration ✅
- [x] **6.2.2** Location-based Firestore queries ✅
- [x] **6.2.3** Clustering algorithm implementation ✅

#### 6.3 Map UI (5/5 - %100) ✅
- [x] **6.3.1** MapViewModel ve location state ✅
- [x] **6.3.2** Google Maps Compose integration ✅
- [x] **6.3.3** Custom marker design (Basic/Premium styles) ✅
- [x] **6.3.4** Cluster rendering ve performance optimization ✅
- [x] **6.3.5** Filter bar ve radius controls ✅

---

### 📸 **7. KAMERA İNTEGRASYONU (6/6 - %100)** ✅

#### 7.1 Camera Domain (2/2) ✅
- [x] **7.1.1** CameraRepository interface ve entities ✅
- [x] **7.1.2** TakePhotoUseCase, UploadPostUseCase ✅

#### 7.2 Camera UI (4/4) ✅
- [x] **7.2.1** CameraViewModel ve capture state ✅
- [x] **7.2.2** CameraX Compose integration ✅
- [x] **7.2.3** Multi-photo capture (1-3 photos) ✅
- [x] **7.2.4** Upload progress ve success feedback ✅

Not: ML Kit entegrasyonu gelecek güncellemede eklenecektir.

---

### 📋 **7.5 POST CARD & POST DETAIL SİSTEMİ (7/7 - %100)** ✅

#### 7.5.1 PostCard Component (4/4 - %100) ✅
- [x] **7.5.1.1** Instagram-style PostCard composable with Material3 design ✅
- [x] **7.5.1.2** Real-time Firebase integration with denormalized user data ✅
- [x] **7.5.1.3** Premium frame overlays, level badges, and user interactions ✅
- [x] **7.5.1.4** Like/Comment/Share/More actions with proper callbacks ✅

#### 7.5.2 PostDetail Screen (3/3 - %100) ✅
- [x] **7.5.2.1** Full-screen post detail with image carousel and HorizontalPager ✅
- [x] **7.5.2.2** Comments and Likes bottom sheets with pagination ✅
- [x] **7.5.2.3** Real-time interactions, reporting, and user navigation ✅

**Teknik Detaylar:**
- ✅ FirestoreConstants updated with likes/comments fields
- ✅ German/English string resources added for all UI text
- ✅ PostRepository extended with getPostLikes, incrementViewCount, incrementShareCount
- ✅ Use cases implemented: GetPostDetailUseCase, GetPostLikesUseCase, AddCommentUseCase
- ✅ ViewModels: PostDetailViewModel with real-time state management
- ✅ Components: CommentsBottomSheet, LikesBottomSheet, ImageCarousel
- ✅ Utility classes: DateTimeFormatters, LocationFormatters, NumberShortener
- ✅ Professional Instagram/TikTok-style UX with smooth animations

---

### 🔔 **8. BİLDİRİM SİSTEMİ (10/10 - %100)** ✅

#### 8.1 Notification Domain (3/3 - %100) ✅
- [x] **8.1.1** NotificationRepository interface ✅
- [x] **8.1.2** NotificationUseCase ve type definitions ✅
- [x] **8.1.3** NotificationTokenHelper: FCM token management ✅

#### 8.2 FCM Implementation (4/4 - %100) ✅
- [x] **8.2.1** FCM service ve message handling ✅
- [x] **8.2.2** Notification channel setup ✅
- [x] **8.2.3** Token registration ve refresh ✅
- [x] **8.2.4** Permission handling ve rationale ✅

#### 8.3 Notification UI (3/3 - %100) ✅
- [x] **8.3.1** NotificationViewModel ve list state ✅
- [x] **8.3.2** Notification list screen ✅
- [x] **8.3.3** Deep link handling (post, profile, etc.) ✅

**Teknik Detaylar:**
- ✅ SperrmuellFirebaseMessagingService: Professional FCM service with app logo branding
- ✅ NotificationTokenHelper: Multi-device token management with Firestore sync
- ✅ NotificationRepository + NotificationRepositoryImpl: Real-time Firestore listeners
- ✅ Domain models: Notification, NotificationType with deep link data
- ✅ Use cases: ListenNotificationsUseCase, MarkNotificationReadUseCase, GetUnreadCountUseCase
- ✅ NotificationsViewModel: Real-time state management with navigation events
- ✅ NotificationsScreen: Material3 UI with empty states, mark all as read
- ✅ Deep link handling: Post detail, user profile, premium navigation
- ✅ Permission handling: Android 13+ POST_NOTIFICATIONS support
- ✅ Professional error handling and logging throughout
- ✅ Localized notification content with string resources
- ✅ Rules.md compliant: Clean Architecture, no hardcoded strings

---

### 🏆 **9. SOSYAL ÖZELLİKLER (12/12 - %100)** ✅

#### 9.1 Social Domain (4/4 - %100) ✅
- [x] **9.1.1** FollowRepository, CommentRepository interfaces ✅
- [x] **9.1.2** Follow/Unfollow, Like/Unlike use cases ✅
- [x] **9.1.3** Comment sistem ve realtime updates ✅
- [x] **9.1.4** User search ve discovery ✅

#### 9.2 Social Data (4/4 - %100) ✅
- [x] **9.2.1** Followers/Following Firestore collections ✅
- [x] **9.2.2** Comments subcollection implementation ✅
- [x] **9.2.3** Like/Unlike optimistic updates ✅
- [x] **9.2.4** Real-time listeners (comments, likes) ✅

#### 9.3 Social UI (4/4 - %100) ✅
- [x] **9.3.1** Comments section (bottom sheet/full screen) ✅
- [x] **9.3.2** User profile pages (other users) ✅
- [x] **9.3.3** Followers/Following lists ✅
- [x] **9.3.4** Search functionality (users, posts) ✅

**Teknik Detaylar:**
- ✅ SocialRepository: Follow/Unfollow, Comments, User Search with Firestore
- ✅ SocialRepositoryImpl: Real-time listeners, optimistic updates, batch operations
- ✅ Domain models: Follow, FollowStats with business logic
- ✅ Use cases: FollowUserUseCase, CommentUseCase, SearchUsersUseCase, GetFollowersUseCase
- ✅ SocialViewModel: State management, event handling, error management
- ✅ FollowersScreen: Material3 UI with real-time followers/following lists
- ✅ UserSearchScreen: Search users, suggested users, follow actions
- ✅ FirestoreConstants: Social collections (follows, comments) with proper field mapping
- ✅ Dependency injection: SocialRepository binding in RepositoryModule
- ✅ Professional error handling and logging throughout
- ✅ Rules.md compliant: Clean Architecture, no hardcoded strings, Material3 design

---

### ⚙️ **10. AYARLAR VE YÖNETİM SİSTEMİ (8/8 - %100)** ✅

#### 10.1 Settings Domain (2/2 - %100) ✅
- [x] **10.1.1** SettingsRepository interface ve user preferences management ✅
- [x] **10.1.2** Remote Config integration for dynamic settings ✅

#### 10.2 Settings UI (6/6 - %100) ✅
- [x] **10.2.1** Settings screen with categories (account, privacy, notifications) ✅
- [x] **10.2.2** Language selection (DE/EN) with persistent storage ✅
- [x] **10.2.3** Theme selection (light/dark/system) with Material3 support ✅
- [x] **10.2.4** Notification preferences with granular controls ✅
- [x] **10.2.5** Privacy settings ve GDPR compliant data controls ✅
- [x] **10.2.6** About page ve legal links (sperrmuellfinder.com integration) ✅

**Teknik Detaylar:**
- ✅ SettingsRepository: DataStore ile persistent user preferences storage
- ✅ SettingsRepositoryImpl: Firebase Remote Config integration, GDPR compliance
- ✅ Domain models: UserPreferences, AppTheme, Language, NotificationSettings, PrivacySettings
- ✅ Use cases: GetUserPreferencesUseCase, UpdateUserPreferencesUseCase, GetAppInfoUseCase
- ✅ SettingsViewModel: Professional state management with reactive UI updates
- ✅ SettingsScreen: Material3 categorized interface with card-based design
- ✅ AboutScreen: Professional app info with developer details (Oemer KAYA)
- ✅ Dialog components: ThemeSelectionDialog, LanguageSelectionDialog
- ✅ Website integration: sperrmuellfinder.com links (privacy, terms, imprint, support)
- ✅ GDPR compliance: Data export/deletion functionality
- ✅ Dependency injection: Hilt module with proper singleton scoping
- ✅ i18n support: Complete German/English string resources
- ✅ Professional error handling and logging throughout
- ✅ Rules.md compliant: Clean Architecture, DataStore persistence, Material3 design

---

### 🛡️ **11. MODERASYON VE GÜVENLİK (0/10 - %0)**

#### 11.1 Moderation Domain (0/3)
- [ ] **11.1.1** ModerationRepository ve report entities
- [ ] **11.1.2** ReportUseCase ve content validation
- [ ] **11.1.3** ModerationManager: Auto-moderation rules

#### 11.2 Security Implementation (0/4)
- [ ] **11.2.1** Firestore Security Rules yazımı
- [ ] **11.2.2** Storage Security Rules (image upload)
- [ ] **11.2.3** User input validation ve sanitization
- [ ] **11.2.4** Rate limiting ve abuse prevention

#### 11.3 Moderation UI (0/3)
- [ ] **11.3.1** Report dialog ve reason selection
- [ ] **11.3.2** Content flagging indicators
- [ ] **11.3.3** User blocking functionality

---

### 📊 **12. ANALİTİK VE PERFORMANS (0/8 - %0)**

#### 12.1 Analytics Implementation (0/4)
- [ ] **12.1.1** Firebase Analytics events tanımları
- [ ] **12.1.2** User behavior tracking (posts, searches, purchases)
- [ ] **12.1.3** Performance monitoring (Crashlytics)
- [ ] **12.1.4** Custom metrics ve dashboard

#### 12.2 Performance Optimization (0/4)
- [ ] **12.2.1** Image compression ve caching (Glide)
- [ ] **12.2.2** Database query optimization
- [ ] **12.2.3** Memory leak prevention
- [ ] **12.2.4** Battery optimization (background tasks)

---

### 🧪 **13. TEST VE KALİTE GÜVENCESİ (0/12 - %0)**

#### 13.1 Unit Tests (0/4)
- [ ] **13.1.1** Domain layer tests (use cases, business logic)
- [ ] **13.1.2** Repository tests (mocking)
- [ ] **13.1.3** ViewModel tests (state management)
- [ ] **13.1.4** Utility classes tests (XP calculation, etc.)

#### 13.2 Integration Tests (0/4)
- [ ] **13.2.1** Firebase integration tests
- [ ] **13.2.2** Camera ve ML Kit tests
- [ ] **13.2.3** Navigation flow tests
- [ ] **13.2.4** Premium feature gating tests

#### 13.3 UI Tests (0/4)
- [ ] **13.3.1** Compose UI tests (screens, components)
- [ ] **13.3.2** End-to-end flow tests (auth to post)
- [ ] **13.3.3** Premium upgrade flow tests
- [ ] **13.3.4** Accessibility tests (TalkBack, contrast)

---

### 🚀 **14. DEPLOYMENT VE RELEASE (0/8 - %0)**

#### 14.1 Build Optimization (0/4)
- [ ] **14.1.1** ProGuard/R8 optimization rules
- [ ] **14.1.2** APK size optimization
- [ ] **14.1.3** Build variants (debug, staging, release)
- [ ] **14.1.4** Signing configuration

#### 14.2 Store Preparation (0/4)
- [ ] **14.2.1** Play Store listing (screenshots, description)
- [ ] **14.2.2** App icon ve branding materials
- [ ] **14.2.3** Privacy policy ve terms of service
- [ ] **14.2.4** Beta testing setup (internal/closed)

---

## YAPILDILAR LİSTESİ

### ✅ **TAMAMLANAN GÖREVLER (134/177 - %75.7)**

#### 🏗️ **1. PROJE KURULUMU VE ALTYAPI (15/15 - %100)**

##### 1.1 Proje Yapılandırması (5/5 - %100) ✅
- [x] **1.1.1** Android Studio projesi oluştur (Kotlin + Compose) ✅
- [x] **1.1.2** Version Catalog (libs.versions.toml) kurulumu ✅  
- [x] **1.1.3** Build.gradle konfigürasyonu (Compose BOM, Hilt) ✅
- [x] **1.1.4** Modül yapısı oluştur (app, data, domain, core) ✅
- [x] **1.1.5** Gradle scripts optimizasyonu ve ProGuard kuralları ✅

##### 1.2 Firebase Entegrasyonu (5/5 - %100) ✅
- [x] **1.2.1** Firebase projesi oluştur ve google-services.json ekle ✅
- [x] **1.2.2** Firebase Auth, Firestore, Storage SDK'ları ekle ✅
- [x] **1.2.3** Firebase Remote Config, Analytics, Crashlytics ekle ✅
- [x] **1.2.4** Firebase App Check (Play Integrity) yapılandırması ✅
- [x] **1.2.5** FCM (Firebase Cloud Messaging) kurulumu ✅

### 🔐 **2. KİMLİK DOĞRULAMA SİSTEMİ (8/8 - %100)** ✅

#### 2.1 Auth Domain Katmanı (3/3) ✅
- [x] **2.1.1** AuthRepository interface ve User entity ✅
- [x] **2.1.2** LoginUseCase, RegisterUseCase, LogoutUseCase ✅
- [x] **2.1.3** AuthState ve auth event handling ✅

#### 2.2 Auth Data Katmanı (2/2) ✅
- [x] **2.2.1** FirebaseAuthDataSource implementasyonu ✅
- [x] **2.2.2** AuthRepositoryImpl ve user data mapping ✅

#### 2.3 Auth UI Katmanı (3/3) ✅
- [x] **2.3.1** AuthViewModel ve UiState management ✅
- [x] **2.3.2** Login/Register/ForgotPassword Compose ekranları ✅
- [x] **2.3.3** Auth navigation flow ve validation ✅

### 👤 **3. KULLANICI PROFİLİ VE SEVIYE SİSTEMİ (14/14 - %100)** ✅

#### 3.1 User Domain (4/4 - %100) ✅
- [x] **3.1.1** User entity (XP, Level, Honesty, Premium status) ✅
- [x] **3.1.2** UserRepository interface ve use cases ✅
- [x] **3.1.3** XPManager: Level hesaplama algoritması ✅
- [x] **3.1.4** HonestyManager: Dürüstlük puanı sistemi ✅

#### 3.2 User Data (3/3 - %100) ✅
- [x] **3.2.1** FirebaseUserDataSource (Firestore users koleksiyonu) ✅
- [x] **3.2.2** UserRepositoryImpl ve data mapping ✅
- [x] **3.2.3** XP transactions ve user_badges koleksiyonları ✅

#### 3.3 Profile UI (7/7 - %100) ✅
- [x] **3.3.1** ProfileViewModel ve profile state management ✅
- [x] **3.3.2** Profile screen (XP/Level/Honesty gösterimi) ✅
- [x] **3.3.3** Premium badge ve frame gösterimleri ✅
- [x] **3.3.4** User posts grid (thumbnail/full preview toggle) ✅
- [x] **3.3.5** Settings ve profile edit sayfaları ✅
- [x] **3.3.6** Pinterest-style grid layout ve archive functionality ✅
- [x] **3.3.7** UserProfileScreen (diğer kullanıcılar için) ✅

---

## 📈 **PROJE İLERLEME ANALİZİ**

### **Genel Tamamlanma Durumu: %75.7 (134/177 görev)**

#### **Kategori Bazında İlerleme:**
- 🏗️ **Proje Kurulumu**: %100 (15/15) ✅ TAMAMLANDI
- 🔐 **Auth Sistemi**: %100 (8/8) ✅ TAMAMLANDI
- 👤 **Kullanıcı Profili**: %100 (14/14) ✅ TAMAMLANDI
- 💳 **Premium Sistemi**: %100 (15/15) ✅ TAMAMLANDI
- 📱 **Ana Ekranlar**: %100 (23/23) ✅ TAMAMLANDI
- 🗺️ **Harita Sistemi**: %100 (12/12) ✅ TAMAMLANDI
- 📸 **Kamera ve ML**: %40 (6/15) ✅ TEMEL ÖZELLİKLER HAZIR (ML Kit gelecek güncellemede)
- 📋 **PostCard & PostDetail**: %100 (7/7) ✅ TAMAMLANDI
- 🔔 **Bildirim Sistemi**: %100 (10/10) ✅ TAMAMLANDI
- 🏆 **Sosyal Özellikler**: %100 (12/12) ✅ TAMAMLANDI
- ⚙️ **Ayarlar ve Yönetim**: %100 (8/8) ✅ TAMAMLANDI
- 🛡️ **Moderasyon**: %0 (0/10)
- 📊 **Analitik**: %0 (0/8)
- 🧪 **Test ve QA**: %0 (0/12)
- 🚀 **Deployment**: %0 (0/8)

#### **Öncelik Sırası (Önerilen):**
1. ✅ **Proje Kurulumu** (Altyapı - %100) - TAMAMLANDI
2. ✅ **Auth Sistemi** (Temel işlevsellik - %100) - TAMAMLANDI
3. ✅ **Kullanıcı Profili** (User experience - %100) - TAMAMLANDI
4. ✅ **Premium Sistemi** (Monetizasyon - %100) - TAMAMLANDI
5. ✅ **Ana Ekranlar** (UI foundation - %100) - TAMAMLANDI
6. ✅ **Harita Sistemi** (Core feature - %100) - TAMAMLANDI
7. **Kamera ve ML** (Core feature - %0)
8. **Sosyal Özellikler** (Engagement - %0)

#### **Kritik Milestone'lar:**
- **Alpha Release** (Auth + Basic features): ~%25 🎯 AŞILDI! (%51.0)
- **Beta Release** (All core features): ~%70 🎯 YAKLASIYORUZ!
- **Production Release** (Polish + optimization): ~%100

#### **Tahmini Süre:**
- **Toplam**: ~3-4 ay (full-time development)
- **Sprint bazında**: 2 haftalık sprintler (14 sprint)

---

## 🎯 **SONRAKİ ADIMLAR**

### **Hemen Başlanacak Görevler:**
1. **Premium Domain Layer** - PremiumRepository interface ve entities
2. **RevenueCat SDK Entegrasyonu** - Kurulum ve konfigürasyon
3. **Premium UI Components** - Paywall screen ve purchase flow
4. **Ana Ekranlar** - Home screen ve navigation setup

### **Bu Hafta Hedefleri:**
- ✅ Proje kurulumu tamamla (%100) - TAMAMLANDI
- ✅ Firebase temel entegrasyonu (%100) - TAMAMLANDI  
- ✅ Auth sistemi (%100) - TAMAMLANDI
- ✅ Kullanıcı Profili sistemi (%100) - TAMAMLANDI
- 🎯 Premium sistemi başlangıcı (%25) - SIRADAKİ

---

---

## 🔍 **SEARCH SCREEN - PREMİUM GATED DETAYLI SPESİFİKASYON**

### **Amaç ve Genel Yaklaşım**

Premium kullanıcılar için tam özellikli arama ve gelişmiş filtreleme ekranı. Basic kullanıcılar search ikonuna bastığında aynı ekrana gelir; üstte filtre/arama UI'sını görür ama kullanamaz. Ortada premium gereksinimi mesajı ve "Premium'a Geç" CTA bulunur.

### **🎨 UX & Ekran Düzeni**

#### **Üst Bar (Herkese Görünür)**
- **Arama alanı** (query), "Temizle", "Filtreler" butonu, sıralama menüsü
- **Basic'te**: Hepsi disabled (tıklanınca paywall sheet'i açılır)
- **Premium'da**: Aktif, debounce'lu canlı arama

#### **Filtre Çekmecesi / Bottom Sheet**
- **Kategori**: UI'da DE, dahili EN (category_de[] göster, category_en[] ile sorgula)
- **Şehir** (city)
- **Mesafe/Yarıçap**: Premium sınırsız; Basic Remote Config'teki 1.5 km gösterilir ama kilitli
- **Availability**: "Still there?" / "Taken" → yalnız Premium
- **Zaman**: Son 24s/48s/72s
- **Sıralama**: En yakın / En yeni / En çok beğeni / En çok yorum

#### **Sonuç Alanı**
- **Premium**: Paging 3 ile sonsuz liste (Post kartları = Home ile tutarlı), kategori chip'leri, availability bar görünür
- **Basic**: Orta bölümde paywall mesajı + CTA (Lottie animasyonlu). Altında örnek kart skeletonları (gri placeholder) — tıklayınca da paywall açılır

#### **Boş / Hata / Yükleme Durumları**
- **Loading**: Shimmer/skeleton
- **Empty**: "Sonuç bulunamadı" + ipuçları (daha geniş yarıçap, kategori kaldır, tarih aralığı)
- **Error**: Yeniden dene, bağlantı kontrolü, offline cache uyarısı

### **🔒 Gating Mantığı (Kesin)**

#### **Basic Kullanıcı**
- Arama alanına yazılamaz, filtreler açılamaz; tüm etkileşimler paywall açar
- Sonuç listesi gösterilmez; yalnız skeleton ve premium bilgilendirme
- "Yarıçap" kontrolü kilitli olarak 1.5 km değeriyle görsel bilgi amaçlı durur

#### **Premium Kullanıcı**
- Tüm filtreler aktif; availability ve sınırsız yarıçap açık
- Favori bölgeler/kategoriler (users.favorites) ile ön ayar butonları

### **🔧 Sorgu & Veri Katmanı**

#### **Kaynak**: Firestore (posts)
- **Temel kriterler**: `status == "active" AND created_at > now - post_expire_hours`
- **Konum**: Cihaz konumundan geohash viewport veya haversine filtreleme
- **Metin arama**: Basit "description contains" + kategori kesişimi (etiketler category_en[])

#### **Sıralama**:
- **"En yakın"**: mesafe ASC (client-side sort; sorgu limit + sonrası hesap)
- **"En yeni"**: created_at DESC (indeks gerekir)
- **"En çok beğeni/yorum"**: likes_count DESC / comments_count DESC (bileşik indeks)

#### **Sayfalama**: Paging 3 + limit() + startAfter(), stabil snapshot'lar

#### **Önbellek**: Room cache (son arama sonuçları ve son kullanılan filtre seti). Premium için son 5 arama DataStore/Room'da saklanır

### **⚡ Performans & Optimizasyon**

- **Resim yükü**: Glide (Compose: Landscapist-Glide) + thumbnail(0.2f), WEBP/AVIF tercih
- **Sorgu limitleri**: İlk sayfa 20, sonraki sayfalar 20 (Remote Config ile ayarlanabilir)
- **Debounce**: Query girişlerinde 400–600 ms
- **Viewport-bound istek**: Map ile entegrasyonda görünür alanı baz al (opsiyonel gelişmiş)
- **Local fuse**: Filtre değişikliklerinde önce cache, ardından remote güncelle

### **🔧 Remote Config (Öneri)**
```
search_enabled = true
search_page_size = 20
search_debounce_ms = 500
basic_radius_meters = 1500        // paywall'da gösterim için
allow_availability_filter = true   // sadece Premium efektif kullanır
search_max_hours_back = 72
```

### **📊 Analytics (Önerilen Event'ler)**
- `search_opened(source)` // bottomNav, deeplink, paywallRedirect
- `search_query_changed(query_len)`
- `search_filters_applied(categories, city, radius, availability, sort)`
- `search_results_shown(count, page)`
- `search_result_click(postId, position)`
- `search_no_results(shown=true)`
- `paywall_shown(context="search_locked")`
- `paywall_cta_click(context="search_locked")`

### **🌐 UI Metinleri & i18n**

Tüm metinler strings (DE default, EN fallback). Örnek anahtarlar:
- `search_title`, `search_hint`, `search_clear`, `search_filters`, `search_sort_by`
- `search_sort_nearest`, `search_sort_newest`, `search_sort_most_liked`, `search_sort_most_commented`
- `search_filter_city`, `search_filter_radius`, `search_filter_availability`, `search_filter_time_range`
- `search_apply`, `search_reset`
- `search_locked_title`, `search_locked_subtitle`, `search_locked_cta_upgrade`
- `search_empty_title`, `search_empty_tip_expand_radius`, `search_empty_tip_remove_filters`
- `search_error_title`, `search_error_retry`

**Hard-coded yasak.**

### **💳 Paywall Davranışı**

- Basic kullanıcı her etkileşimde Paywall BottomSheet: avantaj listesi, fiyatlar RevenueCat'ten dinamik, "Premium'a Geç" CTA
- Satın alma başarılıysa: Search ekranı otomatik re-enable ve mevcut query/filtreyle yeniden ara

### **🔐 Güvenlik & Kurallar**

- `posts` okuma public; yazma yok
- Availability yüzdesi ve oy metrikleri yalnız Premium kart görünümünde (Basic'e gizle)
- Sorgu suistimali (çok hızlı ardışık istekler) için client-side rate limit (+ opsiyonel Cloud Functions guard)
- App Check zorunlu

### **🧪 Test & QA**

#### **Unit**: 
- Filtre kombinasyonlarının doğru query param'larına map edilmesi
- Availability & kategori mapping (DE→EN)

#### **Instrumented**:
- Basic → tüm butonlar paywall açıyor mu?
- Premium → debounce, paging, sort doğru mu?
- 1.5 km kilidi yalnız Basic'te mi görsel?

#### **UI**:
- Loading/empty/error durumları görsel kontrol
- Paywall CTA akışı → RevenueCat → entitlement güncellenince ekran unlock
- Performans: 200+ sonuçta scroll/paging akıcı, frame drop yok
- Erişilebilirlik: contentDescription, focus order, minimum dokunma alanları

### **🔗 Entegrasyonlar**

- **PremiumManager + RevenueCatManager**: Entitlement ile Search'ün kilidini kontrol eder
- **NotificationTokenHelper**: Premium favori bölge/kategori bildirimi tercihlerini persist eder
- **Analytics**: Remote Config varyantlarına göre (A/B) paywall metrikleri
- **Map entegrasyonu**: Search sonuçlarından marker'lara geçiş (opsiyonel buton) — yalnız Premium

### **✅ "Do/Don't" (Cursor için)**

#### **DO**
- Search ekranını tek Compose ekran halinde; ViewModel ile StateFlow tabanlı; Paging 3
- Filtre state'i SavedStateHandle + DataStore ile persist et (Premium için "son kullanılan")
- Basic'te tek bir kaynaktan gating: PremiumManager (RevenueCat = source of truth)

#### **DON'T**
- Query/filtre değerlerini hard-code etme; Remote Config + strings kullan
- Availability yüzdelerini Basic'e gösterme
- Aynı adapter/model/mapper'ı tekrar yazma (mevcut Post kartlarını kullan)

### **📋 Özet**

Search sayfası Premium'a tam açık, Basic'e görsel ama kilitli; her etkileşim paywall tetikler. Sorgular Firestore + geohash/mapping ile optimize edilir; kategori UI'da DE, dahilde EN. Paging, i18n, RC, analytics, güvenlik ve performans şarttır.

---

*Bu PRD, rules.md dosyasındaki tüm kurallar ve gereksinimler dikkate alınarak oluşturulmuştur. Her görev tamamlandığında "YAPILDILAR" listesine taşınacak ve ilerleme yüzdesi güncellenecektir.*
