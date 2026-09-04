# CarFinder goes live: harvesting autoplius.lt + autogidas.lt

**Tags:** kotlin-multiplatform, compose-multiplatform, webview-scraping, android, debugging

Replaced CarFinder's offline 27-car seed catalog with live listings harvested from the two biggest
Lithuanian car marketplaces, autoplius.lt and autogidas.lt, via injected WebView extractors (the approach
proven in CarMark). The match engine was reworked to score partial marketplace data, since listings expose
far fewer attributes than a curated catalog.

## What changed

- `Car` model became a marketplace listing: `source`, `url`, optional price/year/mileage/fuel/gearbox/
  bodyType/powerHp, photo list. Junk entries are capped (400) and swiped ones evicted first.
- One generic extractor JS for both sites, injected on page finish + SPA history updates, now **self-installs
  as a `setInterval` harvester** that posts only new/changed payloads (2.5 s cadence) — lazy-rendered card
  lists defeated fixed native-side retry delays.
- `MatchEngine` skips components whose inputs are missing and renormalises weights: mileage as cost proxy,
  boot-space proxy from body type, kW→hp conversion, Lithuanian body-type slugs (`hecbekas`, `universalas`,
  `visureigis/krosoveris`) parsed from titles *and* URLs.
- Coil (downgraded 3.6.1 → 3.3.0) loads listing photos; procedural gradient art remains the no-photo fallback.

## Debugging war stories

1. **Coil 3.6.1 broke Kotlin/Native with `KLIB resolver: Could not find ...klib`** — the file existed, was a
   valid zip, and even a full `~/.gradle` purge didn't help. The klib manifest says `compiler_version=2.4.10`;
   our compiler is 2.3.21. Compose 1.11.1 klibs (built with 2.3.20) resolve fine. Downgrading Coil to 3.3.0
   (Kotlin 2.1-era) fixed it instantly. Lesson: check the *build compiler* of third-party K/N klibs when the
   resolver reports missing files that exist.
2. **`fun` is a hard keyword** — a `Weights` field named `fun` produced nonsense parse errors in three other
   files. Renamed `drivingFun`.
3. **Cloudflare blocked both headless-browser tools**, so the extractor was debugged through the app's own
   WebView via `webview_devtools_remote` + a tiny Python CDP client (`pip install websocket-client`,
   `suppress_origin=True` — the handshake 403s without it).
4. **autoplius consent wall**: Google Funding Choices (`fc-button`) — content JS aborts until accepted, and the
   accepted state needs `location.reload()` before the listing price renders. The extractor now auto-clicks
   consent (text-matched, selector-restricted to real consent UI) once per session (`sessionStorage` flag).
5. **autogidas uses `/skelbimas/` (singular) with 10-digit zero-padded ids** vs autoplius `/skelbimai/`. One
   regex covers both. The wanted-ad filter (`^ieškau`) keeps "looking for a car" ads out of the deck.
6. **The emulator's LMK was the invisible boss fight**: swap exhausted (Cartuple app importing listings in the
   background + two WebViews), killing webview renderers mid-debug — CDP targets vanished, pages "navigated
   themselves", PIDs churned. `lowmemorykiller: ... device is low on swap (168kB) and thrashing (301%)` in the
   log explained everything. Force-stopping the other app fixed the "random" behaviour.

## Verification

- 17 unit tests green (engine partial-data scoring, both-sites URL regex, Lithuanian term normalisation,
  harvest-payload parsing).
- On emulator: quiz → Browse (site switcher chips) → 25 autoplius + 20 autogidas listings harvested → deck
  shows live cars with photos, match %, reason chips → swipes update Matches ranking → gallery prefetch works
  (26-photo BMW iX3) → state survives reinstall.

### Media & examples

- `assets/2026-09-04-deck-live.png` — live autoplius listing in the swipe deck
- `assets/2026-09-04-matches-live.png` — ranked matches across both sites

### Blog angle

"WebView harvesting for fun and profit: one extractor, two Lithuanian car sites, and a memory-thrashing emulator."
