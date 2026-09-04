# CarFinder

Swipe-based car finder built with **Kotlin Multiplatform** (Android + iOS) and **Compose Multiplatform**.
UI adapted from the CarMark project's swipe deck. Live listings are harvested from **autoplius.lt** and
**autogidas.lt** inside an in-app WebView — no backend, no API keys.

## How it works

1. **Quiz** — budget, main use, fuel, gearbox, and the single priority that matters most.
2. **Browse** — an in-app WebView per site. Injected JS harvests every listing card you scroll past
   (id, title, price, year, mileage, fuel, gearbox, kW→hp, photo) and the listing page for the full gallery.
   Consent banners (Funding Choices / OneTrust / autogidas custom) are auto-accepted once per session.
3. **Discover** — Tinder-style deck of harvested listings. Match % + reason chips on every card.
4. **Match algorithm** (`shared/.../engine/MatchEngine.kt`):
   - hard filters: price > 1.3× budget, known gearbox mismatch;
   - soft 0–100 score over components that have data: budget taper, lifestyle fit (usage × body-type
     matrix), running-cost estimate, mileage, space proxy, performance, eco, fun, fuel/gearbox preference;
   - **taste learning**: each swipe nudges `body:*` / `fuel:*` / `price:*` affinities ±0.15, its weight
     ramps 0→3 over the first 10 swipes;
   - deck mixes exploit (best score) with explore (random) picks.
5. **Matches** — ranked top-12 with explainable reasons; detail screen shows the full score breakdown.

## Build

```sh
./gradlew :androidApp:assembleDebug     # Android APK
./gradlew :shared:allTests              # engine + parser unit tests (JVM)
```

Android Studio: open the repo root and run `androidApp`.
iOS: `shared` builds a static `Shared.framework` (iosArm64 / iosSimulatorArm64 — K/N compilation verified);
the SwiftUI shell in `iosApp/` calls `MainViewControllerKt.MainViewController()` (needs a Mac to link).

## Structure

```
shared/src/commonMain/kotlin/lt/carfinder/
├── model/Models.kt       # Car (marketplace listing), UserPrefs, AppState
├── sites/Sites.kt        # per-site config + extractor JS + payload parsing
├── engine/MatchEngine.kt # scoring, learning, deck ordering, ranking
├── data/StateStore.kt    # JSON state persistence via expect/actual FileStore
├── AppViewModel.kt       # tabs, routes, harvest upserts, deck, matches
└── ui/                   # App shell, Browse (WebView), Quiz, Swipe deck, Matches, Detail, Profile
```
