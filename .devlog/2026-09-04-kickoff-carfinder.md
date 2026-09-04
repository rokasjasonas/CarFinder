# CarFinder kickoff — KMP swipe-based car finder from CarMark UI

**Tags:** kotlin-multiplatform, compose-multiplatform, android, ios, recommendation-engine

Created CarFinder from scratch as a Kotlin Multiplatform app (Android + iOS, shared Compose Multiplatform
UI), reusing the Tinder-style swipe deck from the CarMark project. CarFinder helps users find their perfect
car: a 6-step quiz captures hard constraints + priorities, swiping trains a small taste model, and a
Matches tab ranks the catalog with explainable reasons.

## Decisions

- **Stack copied from CarMark** (Kotlin 2.3.21, Compose Multiplatform 1.11.1, M3 1.9.0, AGP 9.2.1
  `com.android.kotlin.multiplatform.library`, hand-rolled Tab/Route navigation, expect/actual `FileStore`
  for JSON state persistence) — it already compiled on this machine and the swipe gesture code
  (`SwipeScreen.kt`: `detectDragGestures` + 30%-width threshold + rotation/scale/alpha on progress +
  LIKE/NOPE stamps) transferred almost verbatim.
- **Dropped from CarMark:** Ktor, Coil, Google Sign-In, WebView/autoplius scraper — CarFinder is
  offline-first with a local 27-car catalog; no backend needed for v1.
- **Car "photos"** are procedural: gradient background (per-car accent colour) + body-type emoji + brand
  watermark (`CarArt` composable). No licence-free car-photo CDN exists to rely on; the `Car` model can
  gain an `imageUrl` later and swap `CarArt` for `AsyncImage` in one place.
- **Worked in place, not a worktree:** the target directory was empty, so there was no checkout to
  protect; `git init` + greenfield commit.

## The "best car" decision algorithm

Implemented in `shared/src/commonMain/kotlin/lt/carfinder/engine/MatchEngine.kt`:

1. **Hard filters** (`excluded`): price > 1.3× budget, seats < minSeats, gearbox mismatch.
2. **Weighted soft score** 0–100 over components: budget taper (1.0 → 0 between budget and 1.3×budget),
   usage×bodyType fit matrix (±EV/AWD/7-seater bonuses), running costs (fuel-price × consumption at
   15 000 km/yr), space (seats + boot vs usage need), performance, eco, fun (RWD/coupé/hp), fuel choice,
   gearbox. Quiz's single "what matters most" pick gets weight 1.0, the rest 0.35.
3. **Taste learning** (`learn`): each swipe nudges `body:*`, `fuel:*`, `price:<bucket>` affinities
   ±0.15 clamped to [-1, 1]; component weight ramps 0→3 over first 10 swipes (`tasteWeight`).
4. **Deck exploration** (`deck`): every 3rd card is the top-scored unseen car (exploit), the others are
   random unseen picks (explore), seeded `Random(42)` for stability.

## Errors hit along the way

1. `val fun: Float` in the `Weights` data class — `fun` is a hard keyword; produced bizarre cascade
   ("Parameter name expected", "The expression cannot be a selector") in three unrelated files. Renamed
   to `drivingFun`.
2. Non-exhaustive `when` in the usage-fit matrix — SPORT branch was missing `WAGON` (KMP compile
   caught it).
3. `vm.swipe(top.id, good = ...)` — copied CarMark's parameter name (`good`); ours is `liked`.
4. JAVA_HOME pointed at `/usr/lib/jvm/...` — the JDKs actually live in
   `~/.sdkman/candidates/java/21.0.12.1-tem` (my first `ls` output was from the sdkman dir, not
   /usr/lib/jvm which doesn't exist).
5. My own test bug: budget-taper test used a €34k car against a €35k budget (under budget → no taper);
   changed to €40k.

## Verification

- `./gradlew :shared:allTests` — 9 engine tests (hard filters, taper, learning deltas, deck exclusion,
  rank order, eco ordering) — green.
- `./gradlew :androidApp:assembleDebug` — APK installed on running emulator (emulator-5554), full flow
  driven by adb taps: quiz → deck (11 cars correctly survive €25k + 5-seat filters) → swipe right/left →
  Matches shows liked SUVs ranked 71% > non-liked 69–70% > Corolla 65% → Detail shows score breakdown →
  state survived app reinstall (FileStore persistence).

### Media & examples

- `assets/2026-09-04-android-quiz.png` — budget quiz step
- `assets/2026-09-04-android-deck.png` — swipe deck with match badge + reason chips
- `assets/2026-09-04-android-matches.png` — ranked matches with learned taste applied
- `assets/2026-09-04-android-profile.png` — preference weights + taste-learning progress
- `assets/2026-09-04-android-detail.png` — explainable score breakdown

### Blog angle

"Teaching taste in 300 lines: an explainable swipe-learning engine for a KMP car finder."
