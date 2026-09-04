# CarFinder

Swipe-based car finder built with **Kotlin Multiplatform** (Android + iOS) and **Compose Multiplatform**.
UI adapted from the CarMark project's swipe deck.

You answer a 6-step quiz (budget, usage, seats, fuel, gearbox, top priority), then swipe through a deck
of cars. Every swipe teaches a small taste model, and a ranked **Matches** tab explains *why* each car
scores what it does.

## Match algorithm

1. **Hard filters** — cars are excluded if price > 1.3× budget, seats < required, or gearbox ≠ preference.
2. **Soft scoring (0–100)** — weighted average of components: budget fit, lifestyle fit (usage × body type
   matrix), running costs (fuel price × consumption @ 15 000 km/yr), space (seats + boot vs need),
   performance, eco, driving fun, fuel choice, gearbox. The quiz's "what matters most" pick doubles the
   chosen component's weight (1.0 vs 0.35).
3. **Learned taste** — every like/pass nudges per-attribute affinities (`body:*`, `fuel:*`, `price:*`)
   by ±0.15, clamped to [-1, 1]. Its weight ramps from 0 → 3 over the first 10 swipes.
4. **Exploration** — the deck interleaves exploit (highest score) with explore (random) picks so the
   model keeps getting diverse signal.
5. **Explainability** — top components produce human-readable reasons ("Fits 5 people + luggage",
   "2 500 € under budget") shown on cards, and the detail screen shows the full score breakdown.

## Build

```sh
./gradlew :androidApp:assembleDebug     # Android APK
./gradlew :shared:allTests              # match-engine unit tests (JVM)
```

Android Studio: open the repo root and run `androidApp`.
iOS: `shared` builds a static `Shared.framework` (iosArm64 / iosSimulatorArm64); the SwiftUI shell in
`iosApp/` calls `MainViewControllerKt.MainViewController()`. (No checked-in .xcodeproj — add one via
Xcode or the KMP wizard; verified on the Kotlin side only, no Mac in CI.)

## Structure

```
shared/src/commonMain/kotlin/lt/carfinder/
├── model/Models.kt       # Car, UserPrefs, AppState (kotlinx-serialization)
├── engine/MatchEngine.kt # scoring, learning, deck ordering, ranking
├── data/Catalog.kt       # 27 seed cars
├── data/StateStore.kt    # JSON state persistence via expect/actual FileStore
├── AppViewModel.kt       # tabs, routes, swipes, quiz completion
└── ui/                   # App shell, Quiz, Swipe deck, Matches, Detail, Profile
```
