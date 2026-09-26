# Remaining roots preview design: For You, Navigate, Activity

Date: 2026-09-20. Status: approved design for the three navigation roots the R2 alignment left unbuilt. It previews the M2 For You root and the M3 Navigate and Activity roots with seeded data and no map provider or recorder, so the app's five-root shell is complete and honest while the map (M2) and recording (M3) capabilities remain future milestones.

## Outcome and decisions

The floating pill navigation gains For You, Navigate and Activity as working roots with their own back stacks and checkpoints. The user chose:

- **Navigate is the F25 screen only.** A route preview of the current trail with the sheet copy `Ready when you are` / `Choose a trail or record your own route.` and a citron `Start recording` button that shows the dark toast `Recording is coming soon`. No recorder, no recording screens (F05, F11–F13, F15 stay M3).
- **No basemap.** The route is a deterministic schematic polyline drawn on a Canvas (loops close, other routes run corner to corner), labelled `Route preview · schematic`. The map's settings and locate circles are omitted. Map tiles, geometry, attribution and the mini route thumbnails remain M2.
- **Order:** Navigate, then Activity, then For You, in one plan.
- **Seeded fixtures.** For You reads a per-account recommendation fixture; Activity reads per-account sample activities. Both are seeded by the fake backend on first request, like the collections seed, and are labelled sample data on Welcome.

Calls made where Figma or the contract left gaps: Navigate shows the trail you last opened, else your first saved trail, else Half Dome; Activity rows open the trail's detail because Figma has no activity-detail frame; the For You avatar is omitted until Profile exists (M4); the Activity row's bookmark glyph becomes the R2 heart circle wired to the save flow.

## Architecture

- **Data** stays in `data/trail` (the master plan's tentative `data/activity` module would need the fake backend and account databases that are internal here). The API gains `CompletedActivity`, `ForYouFeed`, `ActivityRepository`, `ForYouRepository`, and `TrailAccount.activities` / `TrailAccount.forYou`. The fake backend seeds and serves both fixtures per account through its existing request gate (latency, error rate, offline). Fixture rows live in the backend database's otherwise-unused `cache_row` table under the namespaces `backend-activities` and `backend-foryou`, so installed builds need no schema migration. Each account caches both feeds in its value database through a small Store6 read (`AccountFeed`) using `cache_row` namespaces `activities` and `foryou`, mirroring the catalog's source of truth and bookkeeper.
- **Navigation.** `Root` becomes `EXPLORE, FOR_YOU, NAVIGATE, SAVED, ACTIVITY`, each with a stack and a navigator. The checkpoint adds `forYouRoutes`, `navigateRoutes`, `activityRoutes` and `lastTrail` with defaults, so version-1 checkpoints written before this change still decode. `M1Navigation` gains `selectForYou()`, `selectNavigate()`, `selectActivity()` and `lastOpenedTrailId`.
- **Screens.** New Circuit modules `screen/foryou`, `screen/navigate`, `screen/activity` (api + impl) registered in `ActiveGraph`; presenters for state, no Atom (nothing transient or multi-step). Shared components come from the R2 work: `TrailPhoto`, `TrailFactsRow`, `TrailBookmark`, `TrailsButton`, `TrailsStatusLine`, `TrailsToastHost`, `rememberCheckpointedListState`.
- **Shell.** `M1Content` renders all five `TrailsDestination` items and checkpoints every stack.

## Screen specifications

### Navigate (F25 `236:2258`)

Full-bleed soft surface. Top: a white pill with the pin icon and the trail name (opens Trail detail), and the label pill `Route preview · schematic`. Middle: `TrailRouteSchematic(seed = trail.id, loop = LOOP in features)` — a white halo, a dark route line, a dark start marker and, for non-loops, a citron end marker. Bottom sheet surface (top corners `radii.sheet`): `Ready when you are` (headlineMedium, heading), `Choose a trail or record your own route.` (bodyLarge muted), `Start recording` (`ButtonTone.Hero`). The toast host sits above the sheet. Loading: `Finding your trail…`; unavailable: status line `Not on this device yet` (offline) or `Couldn’t load this trail` with `Try again`.

### Activity (F07 `148:71`)

Heading `Activity` (displayMedium) and `A little progress. A lot of fresh air.` Month card on the dark surface: `<MONTH> SO FAR`, three stats (`km walked`, `trails`, `outside` in whole hours) computed from this calendar month's sample activities, and seven bars for the last seven days (citron on the most recent day with activity). `Your recent adventures`: the newest activity as a large card (photo, heart, name, `Yesterday · 22.7 km · 11 h 0 min`), the rest as rows with a 64 dp thumbnail, name, the same summary line and the heart. Rows open the trail's detail. Empty: `Your hikes will show up here` with a dark `Explore trails`. Offline without cache: `Offline · Activity isn’t on this device yet`; failed: `Couldn’t load your activity`; refresh failure: `Couldn’t refresh · Showing this device’s copy`.

Dates use the device time zone: `Today`, `Yesterday`, else `Sep 10`.

### For You (F02 `148:55`)

Heading `For you` (displayMedium, no avatar). Feature card (photo of the featured trail, dark scrim, white compass + `trails` mark, fixture headline and subline) opens the featured trail. Section `Based on your activity` with `More like <anchor trail name>`, then rows for the recommended trails that resolve in the cached catalog: 64 dp thumbnail, name, `TrailFactsRow(showCount = false)` (star rating · marker difficulty · length) and the heart. Offline without cache: `Offline · Picks aren’t on this device yet`; failed: `Couldn’t load your picks`; refresh failure as Activity.

## Fixtures

- Sample activities (per account, dated from the first request): Half Dome 1 day ago, Mount Takao Trail 1 3 days ago, Bondi to Coogee 6 days ago, Diamond Head 9 days ago, Preikestolen 15 days ago, Lake Agnes Tea House 24 days ago; distance, duration and gain are the catalog's values for the route.
- For You fixture: featured `trolltunga` with `A weekend worth the walk` / `Discover a quieter side of outside.`; anchor `half-dome`; recommended `mist-trail-to-nevada-fall`, `upper-yosemite-fall-trail`, `angels-landing`, `franconia-ridge-loop`, `preikestolen`, `ben-nevis-mountain-track`.
- Welcome discloses: `Real trails and location photography. Ratings, reviews, activity history and recommendations are sample data.`

## Deviations recorded

DEV-31 Start recording shows a coming-soon toast (recorder M3); DEV-32 schematic route preview, labelled, no basemap or geometry (M2); DEV-33 Navigate map controls omitted; DEV-34 For You avatar omitted (Profile M4); DEV-35 activity rows open Trail detail, no activity-detail screen; DEV-36 Activity uses the R2 heart instead of the bookmark glyph; DEV-37 activity history and recommendations are seeded sample data.

## Testing and evidence

JVM: fixture validity and account-feed persistence (data), checkpoint decoding of legacy and new roots (navigation), the schematic route function (design system), month summary and date labels (activity), and one desktop Compose UI test per root. Installed: the existing JVM gate plus assemble/install without wiping data, exercise each root and its restore across a restart, 200 % font scale, uiautomator trees for the new controls, paired captures of F02, F07 and F25, recorded under `docs/evidence/roots/README.md`.

## Out of scope

Map provider and route geometry (M2), the recorder and activity submission (M3), profile and the For You avatar (M4), any personalization logic, the Distance away filter, downloads, sharing.
