# No browsing needed: the app fetches for you, and every answer sharpens the matches

**Tags:** kotlin-multiplatform, compose-multiplatform, matching, product-design, android, testing

The v0.2 app made the user browse car listings so the harvester could scrape pages as they visited. That
was the wrong product. Now the app does the fetching itself in hidden WebViews, and a Refine questionnaire
replaces browsing: each answered question raises a precision meter and immediately re-ranks the matches.

## What changed

- **Browse tab deleted.** A hidden harvester queues search pages per site (max 5 pages each, 12s rounds)
  whenever the swipe deck drops below 10 eligible cars, plus one gallery fetch per listing for more photos.
  The user never sees a WebView.
- **Refine questionnaire** (`Refine.kt`, `RefineScreen.kt`): budget, usage, gearbox, fuel, year slider,
  mileage slider, power slider, body chips, brand chips. `Refine.sharpness` = answered questions / 10,
  shown as "🎯 N% tuned". Sliders commit on `onValueChangeFinished` (no re-rank storms while dragging).
- **MatchEngine hard filters** (`excluded()`): budget×1.25, year, mileage, power, gearbox — applied in
  both `rank()` and `deck()`. Unknown fields never exclude; known conflicts do.
- Scores re-rank live as answers change: brand/body component weight 1.5 (0.3 for non-liked), freshness 0.8,
  mileage as running-cost proxy, all renormalized over the components that actually have data.

## Bugs found by watching the emulator, not the unit tests

1. **The `?: return false` trap.** `excluded()` began with
   `val price = car.priceEur ?: return false` — meaning a car with *no price* skipped **every** hard filter,
   not just the budget one. Result: a 2012 BMW X5 and a 2016 Audi Q5 with no price surfaced even though the
   user had set "from 2017". The fix restructured the function so each filter stands alone:

   ```kotlin
   val price = car.priceEur
   if (price != null && price > prefs.budgetEur * HARD_BUDGET_FACTOR) return true
   if (prefs.minYear != null && car.year != null && car.year < prefs.minYear) return true
   // ... mileage / power / gearbox, same shape
   ```

   Regression test: three priceless cars with a too-old year / too-many-km / too-few-hp are excluded, a
   clean priceless car still passes.
2. **Service ads that out-score real cars.** "Automobilių supirkimas" / "Perkame automobilius" (we-buy-cars
   ads, tow-truck banner, €1 000) ranked at 72% — above a real 2018 BMW 530. Cause: the junk ad has almost
   no fields, so after weight renormalization the *only* present component (budget: way under) dominates
   and the score inflates. Two-layer fix in `Sites`:
   - `junkTitle()`: reject we-buy/wanted/parts ads (`supirkim|perkame|išperkame|pirktume|^ieškau|parduodu.*dalim`…).
     The old filter only matched titles *starting* with "perku" — "Perkame" slipped through.
   - `plausible(car)`: a real listing always has a year or a mileage; require one.
3. **`adb install -r` preserves app data — bugs in saved state outlive the fix.** After shipping the parse
   filter, the junk ad *still* appeared: `saveState()` persists listings via DataStore, and upsert never
   removes, so the pre-fix junk car kept living in restored state. Fix: `AppViewModel.init` purges
   `!Sites.plausible` cars from loaded state once. Lesson: when you tighten ingest validation, also
   re-validate what restore gives you.
4. **There is no `:shared:jvmTest`.** The shared module targets android (with `withHostTest {}`) + iOS only;
   host-unit tests run via `:shared:testAndroidHostTest`, results in
   `shared/build/test-results/testAndroidHostTest/TEST-*.xml`.

## Verification

25 tests green (`:shared:testAndroidHostTest`, 16 MatchEngine + 9 Sites). On the emulator: quiz → deck
self-fills with zero browsing (16 cars); answering year ≥2017 + SUV lifts the meter 50% → 60% → 70% and the
Matches list re-ranks (XC60 75% on top); priceless old cars and we-buy ads gone after the last install.
Screenshots in `/tmp/opencode/`: `refine2.png`/`refine3.png` (precision climbing), `matches1.png` (the bug:
junk ad + priceless X5), `matches4.png` (clean list).

## Design note

The "precision" framing (answered/10, % tuned) turned out to be the honest way to show hidden complexity:
the score math already renormalizes over known fields, so questions-answered is a real proxy for how much
of the score is informed rather than guessed.
