# SperrmüllFinder — Documentation

Deep-dive documentation for the SperrmüllFinder Android app.
For a high-level overview, see the [root README](../README.md).

## Contents

### Getting started
- [BUILD.md](BUILD.md) — Build & run instructions.

### Firebase
- [firebase/SETUP.md](firebase/SETUP.md) — Firebase project setup (Auth, Firestore, Storage, App Check, FCM, Remote Config).
- [firebase/INDEXES.md](firebase/INDEXES.md) — Composite index strategy.
- [firebase/SECURITY_RULES.md](firebase/SECURITY_RULES.md) — Firestore / Storage security rules.

### Features (deep dives)
- [features/MAP.md](features/MAP.md) — Google Maps, clustering, geohash queries.
- [features/SEARCH.md](features/SEARCH.md) — Premium-gated advanced search (Paging 3, filters, sort).
- [features/COMMENTS.md](features/COMMENTS.md) — Real-time comments subsystem.
- [features/LIKES.md](features/LIKES.md) — Likes with optimistic updates.
- [features/FOLLOWERS.md](features/FOLLOWERS.md) — Follow graph + denormalized counts.

### Admin
- [admin/ADMIN.md](admin/ADMIN.md) — Admin system (roles, moderation, user lifecycle).
- [admin/DEPLOYMENT.md](admin/DEPLOYMENT.md) — Cloud Functions + admin tooling deployment.

### Monetization
- [monetization/REVENUECAT.md](monetization/REVENUECAT.md) — RevenueCat integration (entitlements, webhook reconciliation).
- [monetization/PAYWALL.md](monetization/PAYWALL.md) — Modern paywall UI.

### Product
- [../PRD.md](../PRD.md) — Product requirements + feature matrix.
- [../rules.md](../rules.md) — Engineering conventions (Clean Architecture, no hardcoded strings, Material 3, …).
