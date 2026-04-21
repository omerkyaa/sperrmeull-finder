# SperrmüllFinder

> **Discover free bulk-waste treasures around you.**
> A production-grade Android application built with Jetpack Compose, Clean Architecture, and Firebasel — enabling users to share, discover, and claim bulk-waste items (*Sperrmüll*) in their neighborhood.
> Website: [spermuellfinder.de](https://sperrmuellfinder.de)


[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202023.10.01-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-BoM%2032.6-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)
[![MinSdk](https://img.shields.io/badge/minSdk-24-brightgreen)](#)
[![TargetSdk](https://img.shields.io/badge/targetSdk-34-blue)](#)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-success)](#architecture)
[![License](https://img.shields.io/badge/License-Proprietary-lightgrey)](#license)

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Firebase Backend](#firebase-backend)
- [Monetization & Premium](#monetization--premium)
- [Security & Compliance](#security--compliance)
- [Performance & Quality](#performance--quality)
- [Getting Started](#getting-started)
- [Build Variants](#build-variants)
- [Localization](#localization)
- [Roadmap](#roadmap)
- [About the Author](#about-the-author)
- [License](#license)

---

## Overview

**SperrmüllFinder** is a location-based social platform that turns sidewalk bulk waste (*Sperrmüll*) into a reuse economy. Users photograph items left on the curb, share them on a real-time feed and map, and help neighbors discover free furniture, appliances, toys, and more — reducing landfill waste and promoting circular reuse.

The app is targeted at the **German market** (DACH region), ships with full **German + English** localization, and is built to production standards: Clean Architecture, multi-module Gradle build, Firebase App Check, RevenueCat-powered subscriptions, Cloud Functions moderation, and a hardened Firestore rule set.

> **Status:** ~75% feature-complete (134 / 177 tracked tasks). Core product — Auth, Profile, Feed, Search, Map, Camera, Post Detail, Social, Notifications, Premium, Settings — is **shipped**. Moderation, analytics, and full test suites are on the roadmap.

### Problem it solves

- **For residents:** Find and claim free items nearby before they are hauled to landfill.
- **For the environment:** Divert reusable goods from the waste stream — cities like Berlin alone generate >100,000 tons of bulk waste per year.
- **For the community:** Create a social layer around everyday sustainability.

---

## Key Features

### Discovery
- **Real-time feed** (Paging 3 + pull-to-refresh) with Instagram-style post cards.
- **Interactive Google Map** with custom markers, clustering, and Basic/Premium styling.
- **Advanced search** (Premium-gated) with category, city, radius, availability, and time-range filters plus sort modes (nearest / newest / most liked / most commented).
- **Radius filtering** — Basic users: 1.5 km (Remote Config). Premium: unlimited.
- **Geohash-based queries** for efficient location lookups.

### Sharing
- **CameraX integration** with multi-photo capture (1–3 images per post).
- **Upload pipeline** with progress feedback, Exif-aware rotation, and Firebase Storage persistence.
- **Post lifecycle** — active / taken / expired (configurable via Remote Config).
- **"Still there?" voting** (Premium) — crowd-sourced availability signal.

### Social Layer
- **Follow / Unfollow** with denormalized follower/following counts.
- **Likes & Comments** with optimistic updates and real-time Firestore listeners.
- **User search & discovery** screen with suggestions.
- **Public user profiles** with Pinterest-style post grid and archive view.
- **Reporting & blocking** — flow for content/users, integrated with Cloud Functions moderation.

### Identity & Profile
- **Email/password auth** via Firebase Auth, with forgot-password flow and validation.
- **Level & XP system** — gamified progression with configurable thresholds.
- **Honesty score** — reputation metric based on "still there?" votes and reports.
- **Premium badges & profile frames** — visual status indicators.

### Notifications
- **FCM push notifications** with branded icon, deep-link payloads, and Android 13+ permission handling.
- **In-app notification center** with real-time listeners and mark-as-read.
- **Cloud Function triggers** — new post near Premium users, comments, follows, moderation events.
- **Multi-device token management** synced to Firestore.

### Premium (RevenueCat)
- **Modern paywall** with dynamic pricing, Lottie animations, and entitlement-aware UI.
- **Subscription tiers** with monthly/yearly packages.
- **Entitlement listener** — instant unlock across the app on successful purchase.
- **Restore purchases** flow and Cloud Function webhook reconciliation.
- **Feature gating** — source of truth is `PremiumManager` backed by RevenueCat entitlements.

### Settings
- **Theme selection** — light / dark / system (Material 3 dynamic colors).
- **Language** — DE / EN with persistent storage (DataStore).
- **Granular notification preferences.**
- **GDPR-compliant** data export and account deletion.
- **About / Legal** — links to privacy, terms, imprint, support.

### Admin & Moderation (Cloud Functions)
- **3-tier ban system** (soft ban / hard ban / account deletion).
- **Content moderation** — delete/restore posts and comments.
- **Report queue** handlers with auto-triggered moderation flows.
- **Premium grant/revoke/reconcile** admin tools.
- **Admin notifications & warnings** with custom claims for role control.

---

## Tech Stack

### Core
| Area | Tech |
|------|------|
| **Language** | Kotlin 1.9.20, JVM 17 |
| **UI** | Jetpack Compose (BOM 2023.10.01), Material 3, Navigation Compose |
| **Architecture** | Clean Architecture (data / domain / ui), MVVM + UseCases |
| **DI** | Hilt 2.48.1 (with `hilt-navigation-compose`, `hilt-work`) |
| **Async** | Kotlinx Coroutines 1.7.3 + Flow / StateFlow |
| **Build** | Gradle 8.13, AGP 8.13, KSP (replaces KAPT), Version Catalog |

### Backend (Firebase)
- **Auth** (email/password, forgot password)
- **Firestore** (real-time listeners, composite indexes, geohash queries)
- **Cloud Storage** (image uploads, signed URLs)
- **Cloud Functions** for Node.js 20 (admin, moderation, webhooks, triggers)
- **Cloud Messaging (FCM)** with multi-device token sync
- **Remote Config** (feature flags, tunables, paywall variants)
- **Analytics + Crashlytics**
- **App Check** (Play Integrity)

### Data & Persistence
- **Room 2.6.1** — local cache of posts, search results, and user prefs
- **DataStore** — typed user preferences (theme, language, filters)
- **Paging 3** — infinite feed + search results
- **WorkManager** — background uploads and sync jobs

### Networking & Serialization
- **OkHttp 4.12** + logging interceptor
- **Retrofit 2.9**
- **kotlinx.serialization 1.6.1**

### Media & UI Polish
- **CameraX 1.3.1** (multi-photo capture)
- **Glide + Landscapist-Glide** (Compose image loading with thumbnails)
- **Google Maps Compose 4.3** + Play Services Maps/Location
- **Lottie Compose** (paywall, empty states, success animations)
- **Accompanist Permissions** (runtime permissions)
- **Core Splash Screen** API

### Monetization
- **RevenueCat 9.21** (SDK + Paywall UI)
- **OneSignal 5.6** (marketing push channel)
- **Google AdMob + UMP** (consent-aware banner ads, Basic tier)

### Quality & Tooling
- **Detekt 1.23.4** — static analysis
- **Android Lint** — release checks with baseline
- **ProGuard / R8** — full minification + resource shrinking on release/staging
- **JUnit 4, MockK, Turbine, Espresso, Compose UI Test**

---

## Architecture

Multi-module **Clean Architecture** with unidirectional dependency flow:

```
┌─────────────────────────────────────────────────────────────┐
│                          :app                               │
│  Compose UI, ViewModels, Navigation, DI bindings, Services  │
└───────────────────────┬─────────────────────────────────────┘
                        │ depends on
                ┌───────▼────────┐
                │    :domain     │   Pure Kotlin — business logic,
                │  UseCases,     │   entities, repository interfaces.
                │  Entities,     │   No Android / Firebase imports.
                │  Interfaces    │
                └───────▲────────┘
                        │ implemented by
                ┌───────┴────────┐
                │     :data      │   Firebase / Room / Retrofit
                │  DataSources,  │   datasources, mappers, repository
                │  Repositories, │   implementations, DTOs.
                │  Mappers       │
                └───────▲────────┘
                        │ uses
                ┌───────┴────────┐
                │     :core      │   Shared utilities, constants,
                │  Utils, DI     │   base classes, extensions.
                │  primitives    │
                └────────────────┘
```

### Design principles

- **Single-source-of-truth** per domain (e.g. `PremiumManager` wraps RevenueCat).
- **StateFlow-first** UI — ViewModels expose immutable `UiState` sealed classes.
- **Paging 3** everywhere an infinite list exists (feed, search, followers, comments).
- **Repository interfaces in domain**, implementations in data — enabling easy testing with MockK.
- **No hardcoded strings** in UI — all text goes through `strings.xml` with DE/EN variants.
- **Feature-per-package** inside `:app/ui/` (auth, home, map, camera, search, profile, premium, …).

---

## Project Structure

```
SperrmuellFinder/
├── app/                          # :app — Android application module
│   └── src/main/kotlin/com/omerkaya/sperrmuellfinder/
│       ├── SperrmullFinderApplication.kt
│       ├── core/                 # App-level utilities
│       ├── di/                   # Hilt modules (App, Firebase, RevenueCat, …)
│       ├── fcm/                  # Firebase Messaging Service
│       ├── manager/              # PremiumManager, NotificationTokenHelper, …
│       ├── service/              # Background / foreground services
│       └── ui/                   # Compose screens per feature
│           ├── auth/             #   Login, Register, Forgot-Password
│           ├── home/             #   Feed
│           ├── search/           #   Premium-gated advanced search
│           ├── map/              #   Google Maps + clustering
│           ├── camera/           #   CameraX multi-capture
│           ├── post/             #   Post creation
│           ├── postdetail/       #   Carousel + likes/comments sheets
│           ├── profile/          #   Self profile (edit, posts grid)
│           ├── social/           #   User discovery, UserProfileScreen
│           ├── followers/        #   Followers / following lists
│           ├── comments/         #   Comments bottom sheet
│           ├── likes/            #   Likes bottom sheet
│           ├── notifications/    #   Notification center
│           ├── premium/          #   Paywall + upgrade CTAs
│           ├── settings/         #   Theme, language, privacy, about
│           ├── onboarding/       #   3-step onboarding
│           ├── ads/              #   AdMob banners (Basic tier)
│           ├── admin/            #   Admin tools UI
│           ├── report/, ban/, block/  # Moderation flows
│           ├── components/       #   Reusable UI building blocks
│           ├── main/             #   Scaffold + BottomNavigation
│           └── navigation/       #   Nav graph + deep links
│
├── data/                         # :data — Firebase/Room/Retrofit implementations
│   └── src/main/kotlin/.../data/
│       ├── datasource/           # FirebaseAuthDataSource, GoogleMapsDataSource, …
│       ├── repository/           # *RepositoryImpl
│       ├── dto/, mapper/, model/ # DTOs + domain-mapping
│       ├── paging/               # Paging 3 sources
│       ├── messaging/            # FCM service wiring
│       ├── notification/         # Notification Firestore sync
│       ├── source/               # Remote Config, DataStore wrappers
│       └── util/
│
├── domain/                       # :domain — Pure Kotlin, no Android deps
│   └── src/main/kotlin/.../domain/
│       ├── model/                # User, Post, Comment, Follow, Notification, …
│       ├── repository/           # Repository interfaces
│       ├── usecase/              # One class per operation
│       ├── manager/              # Business-logic managers (XP, Honesty, …)
│       └── util/
│
├── core/                         # :core — Shared utilities & constants
│
├── functions/                    # Firebase Cloud Functions (Node 20)
│   ├── adminManagement.js        # setAdminRole, removeAdminRole, getMyAdminRole
│   ├── userManagement.js         # softBan / hardBan / delete / unban
│   ├── premiumManagement.js      # grant / revoke / reconcile
│   ├── contentModeration.js      # deletePost / deleteComment / restorePost
│   ├── notifications.js          # sendAdminNotification, sendWarning
│   ├── notificationTriggers.js   # onNotificationCreated, onPostCreatedNotifyNearbyPremium
│   ├── reportManagement.js       # onReportCreated + moderation actions
│   ├── accountDeletion.js        # GDPR-compliant deletion flow
│   ├── revenueCatWebhook.js      # Subscription state reconciliation
│   └── tests/                    # Firestore-rules tests
│
├── firestore.rules               # Hardened security rules
├── firestore.indexes.json        # Composite indexes (feed, search, map, social)
├── storage.rules                 # Image upload rules (size, type, auth)
├── firebase.json, .firebaserc    # Firebase project config
├── gradle/libs.versions.toml     # Version Catalog — single source of versions
├── settings.gradle.kts           # Modules: app, core, data, domain
└── README.md
```

---

## Firebase Backend

### Firestore collections

| Collection | Purpose |
|------------|---------|
| `users` | Profile, XP, level, honesty, premium status, FCM tokens |
| `posts` | Items with images, geohash, category, status, counts |
| `posts/{id}/comments` | Threaded comments (real-time) |
| `likes` | User ↔ post likes (denormalized) |
| `follows` | Follower / following graph |
| `notifications` | Per-user notification inbox |
| `reports` | User-submitted reports for moderation |
| `xp_transactions`, `user_badges` | Gamification ledger |
| `admin_roles` | Custom-claim backed admin ACL |

### Security

- **Firestore rules** enforce ownership on writes, public read for `posts`, role-based admin checks, rate limiting on writes, and content validation (field types, lengths, image count 1–3).
- **Storage rules** restrict image uploads to authenticated users, file size limits, and allowed MIME types.
- **App Check (Play Integrity)** is mandatory — protects APIs from scraping and replay.
- **Custom claims** drive admin/moderator role control, issued via `setAdminRole` Cloud Function.

### Cloud Functions (Node 20)

Organised by domain (see `functions/`). Key exported functions:

- **Admin:** `setAdminRole`, `removeAdminRole`, `getMyAdminRole`
- **User lifecycle:** `softBanUser`, `hardBanUser`, `deleteUserAccount`, `unbanUser`
- **Premium:** `grantPremium`, `revokePremium`, `reconcilePremiumStatus`
- **Moderation:** `deletePost`, `deleteComment`, `restorePost`
- **Notifications:** `sendAdminNotification`, `sendWarning`, `onNotificationCreated`, `onPostCreatedNotifyNearbyPremium`
- **Reports:** `onReportCreated`, `dismissReport`, `warnUser`, `deleteReportedContent`
- **Webhooks:** RevenueCat subscription state reconciliation
- **Utilities:** backfill scripts, disabled-user reactivation scripts

---

## Monetization & Premium

### Subscription (RevenueCat)

- SDK-driven paywall — prices, trial eligibility, and packages fetched dynamically.
- `PremiumManager` is the **single source of truth**; all feature gates subscribe to its entitlement `StateFlow`.
- Purchase success triggers instant UI unlock (no app restart, no polling).
- Cloud Function webhook keeps Firestore `users.premium_*` fields in sync for server-side gating.

### Premium features

- Unlimited search radius (vs 1.5 km Basic).
- Advanced search + all filters + sort modes.
- "Still there?" voting + availability bar on post cards.
- Category/region favorites + targeted push notifications.
- Premium visual identity (profile frame, badge, premium marker style on map).
- Ad-free experience.

### Ads (Basic tier)

- AdMob banner on the home feed.
- Google UMP (User Messaging Platform) for GDPR-compliant consent.
- Debug build uses Google's official test ad unit IDs; release build reads production IDs from `local.properties` / env vars.

---

## Security & Compliance

- **GDPR:** in-app data export and full account deletion (Cloud Function cascades deletion across Firestore, Storage, Auth, RevenueCat).
- **Privacy by design:** Availability metrics and "still there?" ratios hidden from Basic users.
- **App Check** enforced for all Firebase calls.
- **No secrets in VCS:** `local.properties`, keystore files, `google-services.json` (service-account variant), and Firebase admin keys are gitignored.
- **Proguard/R8** full minification + resource shrinking + NDK debug symbol upload on release.
- **Signed release & staging** with separate build configs and distinct AdMob unit IDs.

---

## Performance & Quality

- **Paging 3** with stable snapshot keys — no jitter or duplicates on refresh.
- **Image pipeline** — thumbnail(0.2f) preview → full-res with Glide disk+memory cache.
- **Debounced search** (500 ms, Remote-Config tunable).
- **Geohash prefix queries** keep location reads O(1) per tile.
- **Denormalized counts** (likes, comments, followers) avoid expensive aggregations.
- **Compose compiler metrics** emitted on debug builds (`build/compose_metrics/`).
- **Lint + Detekt + ProGuard** gate release builds.
- **Core library desugaring** — `java.time`, streams available on API 24+.

---

## Getting Started

### Prerequisites

- **Android Studio Iguana** (or newer) with Android SDK 34 installed.
- **JDK 17** (AGP 8.13 requirement).
- **Firebase CLI** (for deploying rules/functions): `npm i -g firebase-tools`.
- **Node 20** (for Cloud Functions).
- A **Firebase project** with Auth, Firestore, Storage, FCM, App Check, and Remote Config enabled.
- A **RevenueCat** account with configured offerings.
- A **Google Maps API key** for Android.

### 1. Clone

```bash
git clone https://github.com/<your-username>/SperrmuellFinder.git
cd SperrmuellFinder
```

### 2. Configure `local.properties`

Copy the template and fill in your keys:

```bash
cp local.properties.example local.properties
# then edit local.properties with your real values
```

Debug builds use Google's official **test ad units** automatically — no AdMob keys required for development.

### 3. Add `google-services.json`

A template is provided at `app/google-services.json.example`. Download your real file from **Firebase Console → Project settings → Your apps → google-services.json** and place it at `app/google-services.json` (gitignored).

### 4. Build & run

```bash
./gradlew assembleDebug
# or run from Android Studio: select app > device > Run
```

### 5. Deploy backend

```bash
# Firestore rules & indexes
firebase deploy --only firestore:rules,firestore:indexes,storage:rules

# Cloud Functions
cd functions && npm install
firebase deploy --only functions
```

---

## Build Variants

| Variant | App ID suffix | Name | Minified | Firebase env | Purpose |
|---------|---------------|------|----------|--------------|---------|
| **debug** | — | `SperrmüllFinder Debug` | No | Dev | Local development, test ads, analytics off |
| **staging** | — | `SperrmüllFinder Staging` | Yes | Prod-like | QA, production-ish build with full R8 |
| **release** | — | `SperrmüllFinder` | Yes | Prod | Play Store distribution |

All variants share the same `applicationId` (`com.omerkaya.sperrmuellfinder`) so they connect to the same Firebase project — use a distinct SHA-1 per signing config.

---

## Localization

- **Default:** German (`de`).
- **Fallback:** English (`en`).
- **Coverage:** 100% of user-facing strings — titles, empty states, error messages, notifications, paywall copy, legal links.
- **Enforcement:** `lint` with `abortOnError = true` — hardcoded strings fail the build. `MissingTranslation` and `ExtraTranslation` are explicitly disabled to allow the DE→EN fallback pattern.
- **Runtime switch:** Language selector in Settings persists to DataStore and applies without restart.

---

## Roadmap

Tracked in [`PRD.md`](PRD.md). Remaining work (~25%):

- [ ] **Moderation (11):** ModerationRepository, content validation, user-input sanitization, rate limiting, report-reason dialog, content flagging indicators, user blocking UI.
- [ ] **Analytics (12):** Firebase Analytics events for funnels, performance monitoring dashboards, custom metrics, battery / memory profiling.
- [ ] **Testing (13):** Unit tests for UseCases + ViewModels (MockK + Turbine), Firebase integration tests, Compose UI tests, accessibility audit.
- [ ] **Deployment (14):** Final ProGuard pass, APK size budget, Play Store listing, beta testing tracks.
- [ ] **ML Kit** (labeling / object detection) for auto-categorization — planned future release.

---

## Documentation Index

Extensive documentation is organised under [`docs/`](docs/README.md):

- **Product:** [`PRD.md`](PRD.md) · [`rules.md`](rules.md) — Requirements and engineering conventions.
- **Build:** [`docs/BUILD.md`](docs/BUILD.md) — Build & run instructions.
- **Firebase:** [`docs/firebase/SETUP.md`](docs/firebase/SETUP.md) · [`INDEXES.md`](docs/firebase/INDEXES.md) · [`SECURITY_RULES.md`](docs/firebase/SECURITY_RULES.md)
- **Features:** [`MAP`](docs/features/MAP.md) · [`SEARCH`](docs/features/SEARCH.md) · [`COMMENTS`](docs/features/COMMENTS.md) · [`LIKES`](docs/features/LIKES.md) · [`FOLLOWERS`](docs/features/FOLLOWERS.md)
- **Admin:** [`ADMIN`](docs/admin/ADMIN.md) · [`DEPLOYMENT`](docs/admin/DEPLOYMENT.md)
- **Monetization:** [`REVENUECAT`](docs/monetization/REVENUECAT.md) · [`PAYWALL`](docs/monetization/PAYWALL.md)

---

## About the Author

**Ömer Kaya** — Android / Full-Stack Developer building production-grade Kotlin + Firebase applications.

- Website: [spermuellfinder.de](https://sperrmuellfinder.de)
- Focus: Jetpack Compose, Clean Architecture, Firebase at scale, Gradle performance, subscription economies (RevenueCat).

This project demonstrates end-to-end ownership of a modern Android product: multi-module architecture, real-time backend, monetization, moderation tooling, i18n, CI-ready build system, and GDPR compliance.

---

## License

**Proprietary — All rights reserved.**
This repository is published as a professional portfolio reference. You may review and evaluate the code for **recruiting / technical-review purposes only**. Commercial use, redistribution, or derivative works are **not permitted** without the explicit written consent of the author.

© 2025–2026 Ömer Kaya. SperrmüllFinder® is a project of Ömer Kaya.
