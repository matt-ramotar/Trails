# Trails Design Revision R2 Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Status (September 19, 2026):** executed task by task with a review gate per task and a final whole-branch review; every step is ticked. Verification record: [docs/evidence/r2/README.md](../../evidence/r2/README.md). Open owner items are listed in that record's post-review section and in the progress summary handed over with this plan.

**Goal:** Bring the accepted M1 Android journey (Explore → search/filter → Trail detail → Save to list → Saved → Collection) and its shared components in line with design revision R2 of the Trails Figma (September 18, 2026) without changing the Store6 read/mutation paths or the M1 acceptance semantics.

**Architecture:** Tokens and reusable components live in `foundation/designsystem`; `data/trail/api` gains backward-compatible model and query fields with defaults; `feat/filters` and `feat/savetrail` own the sheets, the card and the status helpers; `screen/*` own layouts; `app/core` owns the navigation shell. Every new `Trail` or `TrailQuery` field has a default so cached JSON and journals decode unchanged. Messaging moves from card notices to a dark toast, a one-line status line and a decision layout inside the existing save sheet.

**Tech Stack:** Kotlin Multiplatform 2.2.20, Compose Multiplatform 1.9.0 (Material 3), Circuit 0.30.0, Metro 0.6.7, SQLDelight 2.1.0, kotlinx.serialization, the pinned Store6/repaired-Atom tuple (unchanged), desktop Compose UI tests (`compose.desktop.uiTestJUnit4`) as already used by `multiplatform/app/core`.

## Global Constraints

- Visual authority: Figma file `4B7GK9ndPVQ1BFIKGg0Zqj`, page `142:2`, section `153:97` (28 frames), Components · Messaging page `248:2777`, revision dated September 18, 2026. Frames 26–28 are `264:3418`, `264:3966`, `263:3756`. Figma components: Difficulty marker `257:2858`, Trail card `150:21`, Toast `275:4292`, Status line `275:4329`, Status indicator `275:4328`, Decision sheet `275:4339`.
- Light tokens: background and surface `#FFFFFF`; foreground `#171E14`; muted `#545A52`; border `#E6E8E4`; accent forest `#1D4B35`; citron `#A9F184`; soft `#F4F5F4`; dark `#0D1F18`; clay `#BC624A`; warning `#8A4B12`; danger `#A5352B`. Difficulty markers: Easy `#43A047` circle, Moderate `#F2B82E` rounded square, Hard `#EE6A45` triangle, Strenuous `#5E3A27` diamond.
- Button rule: citron = the single hero action a screen exists for (Save trail, Map, Start recording, Pause, Resume, Save activity); dark = commits and empty-state primaries (Show N trails, Apply filters, Explore trails, Try again, Search, Finish hike, Remove from saved); soft = secondary; white 48 dp circle with shadow = icon actions; accent forest is never a button fill. Buttons are pills (radius 999) with a 52 dp reference height and a 48 dp minimum target.
- Messaging rule: completions use a dark one-line toast 16 dp above the navigation that dismisses itself and may carry one citron action; pending, offline, failed and attention states use a one-line status line (indicator, one sentence of at most 44 characters, optional underlined action) beside the content it describes; choices use the sheet with a title, one sentence, a dark primary and a ghost secondary. No titled cards with nested buttons. A local save is not a sync; parked work gets no Retry.
- Typography: Manrope headings and Inter body from the bundled variable fonts; no runtime font download. Metric units everywhere.
- M1 has no map, location, sharing, downloads, custom routes or subscriptions. R2 controls that need them (download circle, Map pill, share and more circles, On-trail directions, Get directions, Customize route, Preview and Photo tour cards, Top sights, Downloads and Invites tabs, Recommended lists, quick-action pill, Distance away) are omitted from the M1 preview and recorded as deviations, never left as dead controls.
- Catalog: `WorldTrails.kt` seeds 50 real routes (42 day hikes, then eight multi-day treks) through the versioned reseed in `M1Backend` (`SEED_VERSION`, `deleteBackendTrails`); a reseed preserves saved memberships, journals, receipts and client identity. Photographs are bundled per trail ID (`TrailPhoto(trailId)`); photographer credits with source and licence links appear on Trail detail and Welcome only (`TrailPhotoCredit`). No attribute is invented for a real route: Strenuous is the eight treks, activities are Hiking for every route plus Backpacking for the treks, the card highlight is derived from feature tags, and Kid-friendly and Highest point are omitted (DEV-30) until the catalog records sourced values. The Nearby chip is retired; region is selected only through search text.
- Keep the API/impl module split, Metro `@Inject`, Circuit `Ui`/`Presenter`, `TrailsTheme` accessors. No React or JavaScript HeroUI dependency.
- Tests use `kotlin.test` on JVM. Desktop Compose UI tests need these two lines in the module's `jvmTest` dependencies, exactly as in `multiplatform/app/core/build.gradle.kts`: `implementation(compose.desktop.uiTestJUnit4)` and `runtimeOnly(compose.desktop.currentOs)`.
- Preserve the first failure (console log or XML) of every red step before fixing it; never rerun an unchanged failure to obtain green. One Gradle runner at a time.
- Commit messages end with `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`.

## Prerequisite

The M1 tree, the world-trail catalog and the location photography are committed on `matt-ramotar/v2.5` (`0f69369`, `481346e`, `6435f57`). Only two untracked groups may remain: `docs/evidence/m1/` (local capture archives) and `redesign/` (reference screenshots). Verify:

```bash
git status --short | grep -v '^?? docs/evidence/m1/' | grep -v '^?? redesign/' | wc -l
```

Expected: `0`. If it is not zero, stop and commit or stash the stray change before Task 1.

## File Structure

| File | Responsibility |
| --- | --- |
| `docs/trails-completion-contract.md`, `docs/trails-m1-design-contract.md`, `docs/trails-reference-inventory.md`, `docs/trails-reference-deviations.md`, `docs/superpowers/specs/2026-09-18-world-trails-seed-design.md` | Contract text, copy table, deviations DEV-23…DEV-28 and DEV-30, R2 addendum to the catalog specification (Task 1) |
| `multiplatform/foundation/designsystem/src/commonMain/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/theme/Colors.kt`, `Typography.kt` | R2 token values, status and difficulty colours, two heading sizes (Task 2) |
| `…/designsystem/component/TrailsButton.kt` (new) | `TrailsButton`, `ButtonTone`, `TrailsIconCircle` (Task 3) |
| `…/designsystem/icon/Icons.kt` | Heart, HeartFilled, Star, Tick, Sync, Circle, Sparkles, MoreHorizontal entries (Task 3) |
| `…/designsystem/component/DifficultyMarker.kt` (new) | `DifficultyMarkerKind`, `DifficultyMarker` (Task 4) |
| `multiplatform/data/trail/api/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/TrailData.kt` | `STRENUOUS`, `TrailActivity`, `TrailSort`, new `Trail`/`TrailQuery` fields (Tasks 4, 5); Nearby constants removed (Task 8) |
| `multiplatform/data/trail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/WorldTrails.kt`, `TrailCatalog.kt`, `M1Backend.kt` | Strenuous treks, derived activities, matcher, sort, seed-version bumps (Tasks 4, 5) |
| `multiplatform/feat/savetrail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/feat/savetrail/TrailSaveCard.kt` | R2 card, derived highlight, marker mapping, status helpers (Tasks 6, 10) |
| `…/designsystem/component/TrailPhoto.kt`, `multiplatform/foundation/designsystem/TRAIL_PHOTOS.md`, `ASSETS.md`, `…/screen/welcome/impl/…/WelcomeUi.kt` | Photo credit leaves the image; attribution notes (Task 6) |
| `…/feat/savetrail/impl/…/RealSaveTrailFeature.kt` | Sheet copy, decision layout, completion toast (Task 10) |
| `multiplatform/feat/filters/api/…/FiltersFeature.kt`, `…/feat/filters/impl/…/RealFiltersFeature.kt` | `FilterSection`, R2 filter sheet (Task 7) |
| `multiplatform/screen/explore/api/…/ExploreState.kt`, `…/explore/impl/…/ExplorePresenter.kt`, `ExploreUi.kt` | R2 Explore header, chips, sort (Task 8) |
| `multiplatform/screen/traildetail/impl/…/TrailDetailUi.kt` | R2 detail layout (Task 9) |
| `…/designsystem/component/TrailsStatus.kt` (new) | `StatusKind`, `TrailsStatusLine`, `TrailsToast` (Task 10) |
| `multiplatform/screen/saved/impl/…/SavedUi.kt`, `…/screen/collection/impl/…/CollectionUi.kt`, `…/screen/welcome/impl/…/WelcomeUi.kt`, `multiplatform/app/core/…/MainViewController.kt`, `M1Content.kt` | Status-line migration (Task 10), Saved layout (Task 11), floating navigation (Task 12) |
| `…/designsystem/component/TrailsFloatingNav.kt` (new) | `TrailsDestination`, `TrailsFloatingNav` (Task 12) |
| `docs/evidence/r2/README.md` (new) | Verification record (Task 13) |

`…` abbreviates `src/commonMain/kotlin/org/mobilenativefoundation/trails/<module package>`; test files use `src/jvmTest/kotlin/…`.

---

### Task 1: Record design revision R2 in the contracts and inventory

**Files:**
- Modify: `docs/trails-completion-contract.md`
- Modify: `docs/trails-m1-design-contract.md`
- Modify: `docs/trails-reference-inventory.md`
- Modify: `docs/trails-reference-deviations.md`
- Modify: `docs/superpowers/specs/2026-09-18-world-trails-seed-design.md`

**Interfaces:**
- Consumes: the R2 spec `docs/superpowers/specs/2026-09-18-alltrails-redesign-second-pass-design.md`.
- Produces: the copy table below (used verbatim by Task 10) and deviation IDs DEV-23…DEV-28 and DEV-30 (cited by Tasks 5–9 and 11).

- [x] **Step 1: Replace the authority sentence in the completion contract**

In `docs/trails-completion-contract.md`, replace the item beginning `2. [Trails v3 in Figma]` with:

```markdown
2. [Trails v4 · AllTrails redesign in Figma](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=153-97) is the visual reference as revised on September 18, 2026 (design revision R2). The main page is `142:2`; the app section `153:97` holds twenty-eight 390 × 844 frames; messaging components live on the [Components · Messaging](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=248-2777) page. The inventory below records the original twenty frame IDs and the eight R2 additions.
```

- [x] **Step 2: Replace the navigation-roots sentence**

Replace the sentence `The full navigation roots are **Explore, Saved, Activity, Profile**. Moments is reached from Explore.` with:

```markdown
The full navigation roots are **Explore, For You, Navigate, Saved, Activity** in a floating pill bar; Profile opens from the avatar in screen headers. For You carries the former Moments content.
```

- [x] **Step 3: Append the R2 frames to the inventory table**

After the `F20` row add:

```markdown
| F21 `236:2165` | Collection detail | M1 | Title, membership count, R2 trail cards; each row opens its own trail. |
| F22 `236:2181` | All saved trails | M1 | Deduplicated union of memberships as R2 cards. |
| F23 `236:2210` | Downloads (empty) | Deferred | Offline maps are not in the contract; tab omitted until a contract change (DEV-24). |
| F24 `236:2234` | Invites (empty) | M4 decision | Requires a sharing/invite contract before it is enabled. |
| F25 `236:2258` | Navigate | M3 | Ready sheet with Start recording (citron hero). |
| F26 `264:3418` | Trail detail · Route and sights | M2 | Route map overlays, media cards, route actions, Top sights, tags; M1 renders tags only. |
| F27 `264:3966` | Filters · More options | M1 (partial) | Switches and Activity chips are M1; Distance away needs location (M2). |
| F28 `263:3756` | Sight detail | M2 | Map pins and sight sheet; needs route geometry and POI data. |
```

- [x] **Step 4: Replace the token bullet and add the button and messaging rules**

In "Design-to-Compose and asset handoff" replace the bullet beginning `- Map Forest \`#1D4B35\`, Citron \`#D9F48C\`` with these three bullets:

```markdown
- Map the R2 tokens into `TrailsTheme`: Forest `#1D4B35` (brand surfaces, selected states, icons; never a button fill), Citron `#A9F184` (hero action), Dark `#0D1F18` (commit action, toast), White `#FFFFFF` (background and surfaces), Ink `#171E14`, Muted `#545A52`, Border `#E6E8E4`, Soft `#F4F5F4`, Clay `#BC624A`, message warning `#8A4B12` and danger `#A5352B`. Difficulty markers: Easy `#43A047` circle, Moderate `#F2B82E` square, Hard `#EE6A45` triangle, Strenuous `#5E3A27` diamond. Reuse the existing theme access API.
- Buttons follow one hierarchy: citron for the single hero action a screen exists for, dark for commits and empty-state primaries, soft for secondary actions, white 48 dp circles for icon actions. Accent forest is never a button fill.
- Feedback uses three patterns: a dark one-line toast above the navigation for completions, a one-line status line beside its subject for pending/offline/failed/attention states, and the sheet for decisions. Card notices with icons in circles, title-plus-description pairs and nested buttons are retired.
```

- [x] **Step 5: Update the M1 interaction contract**

In `docs/trails-m1-design-contract.md`:

a. Replace the whole "Theme role" table with:

```markdown
| Theme role | R2 value | Translation |
| --- | --- | --- |
| Background and Surface | `#FFFFFF` | `TrailsTheme.colors.background` / `.surface` |
| Foreground / Ink | `#171E14` | Primary text, outlined chip borders inherit Border |
| Muted | `#545A52` | Secondary text; 6.6:1 on white |
| Border | `#E6E8E4` | Dividers, tag outlines, grabber |
| Accent / Forest | `#1D4B35` | Selected chips, icons, brand surfaces; never a button fill |
| Citron | `#A9F184` | Hero action fill with Ink text (11.7:1) |
| Dark | `#0D1F18` | Commit action fill and toast surface with white text |
| Soft | `#F4F5F4` | Secondary buttons, unselected chips, search field |
| Clay | `#BC624A` | Error accent/stroke only |
| Warning / Danger | `#8A4B12` / `#A5352B` | Status indicators (Attention / Failed) |
```

b. In the Compose translation table replace the `Button`, `Trail card` and `Status panels` rows with:

```markdown
| Button `147:633` | `TrailsButton` with tones Hero (Citron/Ink), Commit (Dark/White), Secondary (Soft/Ink); `TrailsIconCircle` for icon actions | Pill shape; 52 dp reference height; min 48 dp Android target; loading, disabled, pressed and accessible label |
| Trail card `150:21` | `TrailSaveCard` | Photo 233 dp with heart circle (download circle deferred, DEV-24); optional highlight chip; facts row: star, rating (count), difficulty marker + name, length |
| Status line `275:4329`, Toast `275:4292`, Decision sheet `275:4339` | `TrailsStatusLine`, `TrailsToast`, decision layout inside the save sheet | One sentence beside its subject; completion toast above navigation, polite live region, dismisses after 5 s; pending and read staleness remain independent |
```

c. In the enabled-control inventory replace the `For you / F01`, `Nearby / F01`, `Lakes / Forest / F01`, `Filters icon / F01, F16` and `Difficulty/Distance chips / F16` rows with:

```markdown
| All chip / F01 | `OpenFilters(ALL)`; filter feature | Copy current selectors | Dismiss keyboard before presenting sheet | 1, 9 |
| Difficulty / Length / Elevation gain chips / F01 | `OpenFilters(section)`; filter feature | Copy current selectors | Sheet scrolls to that section | 1, 9 |
| Sort · Most popular / F01 | `Sort(sort)`; Explore Presenter | Same membership, ordered by `TrailSort` | Most popular is the fixture recommended order; no personalization claim | 1 |
| Strenuous chip / F09 | `ToggleDifficulty`; filter feature | Draft count only | Same as other difficulties | 1, 9 |
| Elevation gain slider / F09 | `ElevationChanged`; filter feature | Draft count only | Labelled native slider (DEV-20); upper stop means no maximum; Highest point is omitted (DEV-30) | 1, 9 |
| Dog-friendly switch / F09 | `ToggleSuitability`; filter feature | Draft count only | Row label toggles the switch; Kid-friendly is omitted (DEV-30) | 1, 9 |
| Activity chips · Hiking, Backpacking / F09 | `ToggleActivity`; filter feature | Draft count only; any selected activity matches | Multi-select; none selected means any activity; other R2 activities are omitted (DEV-30) | 1, 9 |
```

d. In "Query contract" replace the **For you**/**Nearby**/**Lakes**/**Forest** bullet with:

```markdown
- **Sort**: Most popular is the catalog's documented recommended order; Highest rated, Shortest and Longest reorder the same membership. Region is selected only through search text in R2: the Nearby chip is retired and a checkpointed region is cleared on restore. Feature toggles live in Filters. Elevation gain is inclusive canonical metres whose upper stop means no maximum; Dog-friendly maps to the `DOG_FRIENDLY` feature; Activity matches any selected activity (Hiking on every route, Backpacking on the eight treks). All selectors combine with AND.
```

e. In the reliability matrix replace the quoted copy with the strings in the table below (same rows, new text).

- [x] **Step 6: Append the R2 section to the reference inventory**

Append to `docs/trails-reference-inventory.md`:

```markdown
## September 18 revision R2

Design revision R2 (live inspection September 18, 2026) supersedes the September 15 token and component rows above for implementation while the frozen evidence set stays intact. Section `153:97` now holds 28 frames (F21–F28 listed in the completion contract). Local component sets: Difficulty marker `257:2858` (Easy `257:2850`, Moderate `257:2852`, Hard `257:2854`, Strenuous `257:2856`), Collection tile `257:2867` (Cover=Photo `236:2112`, Cover=Placeholder `257:2859`), Toast `275:4292`, Status indicator `275:4328`, Status line `275:4329`, Decision sheet `275:4339`. Trail card `150:21` properties: `Rating#259:0`, `Difficulty#259:1`, `Length#259:2`, `Difficulty marker#259:3`, `Show highlight#258:0`. Token values, the button rule and the messaging rule are recorded in the completion contract. Card messaging components are archived (hidden) on the Components · Messaging page. The Trails+ paywall was not adapted. Reference: `docs/superpowers/specs/2026-09-18-alltrails-redesign-second-pass-design.md`.
```

- [x] **Step 7: Append deviations DEV-23…DEV-28**

Append rows to the ledger table in `docs/trails-reference-deviations.md`:

```markdown
| DEV-23 | R2 map-dependent controls on M1 screens (Map pill, download circle, On-trail directions, expand, Get directions, media cards, Top sights) | Omitted from the M1 preview; no dead controls | M2 map/geometry/POI proof restores them |
| DEV-24 | Downloads tab and download circles imply offline maps | Omitted; offline maps are outside the contract | Explicit contract change |
| DEV-25 | Distance away filter needs a location fix | Omitted from the M1 sheet | M2 location proof |
| DEV-26 | Customize route button implies custom route editing | Omitted; excluded by contract | Explicit contract change |
| DEV-27 | Two-thumb R2 ranges (Length, Elevation gain) versus native sliders | Length keeps two labelled sliders (DEV-20); Elevation gain uses one maximum slider | Installed large-text and TalkBack check in Task 13 |
| DEV-28 | R2 activity pictograms and the Figma Switch on state | Activity chips are text-only; native Switch shows real off/on state | None required |
| DEV-30 | R2 Kid-friendly switch, Highest point range and activity chips beyond Hiking and Backpacking | Omitted; the world-trail catalog records no sourced value for them and no attribute is invented for a real route | Catalog revision that sources the attributes |
```

- [x] **Step 8: Append the R2 addendum to the world-trail catalog specification**

Append to `docs/superpowers/specs/2026-09-18-world-trails-seed-design.md`:

```markdown
## Design revision R2 addendum (September 18, 2026)

Design revision R2 adds a fourth difficulty tier. The eight multi-day treks (positions 43–50) are seeded as `STRENUOUS` because they are overnight itineraries; the day hikes keep their source-led grades, so the catalog holds 5 easy, 16 moderate, 21 hard and 8 strenuous entries. Every route carries the derived activity `HIKING`; the eight treks also carry `BACKPACKING`. No highest-point or kid-friendly value is recorded because the sources above were not researched for them (DEV-30). The Nearby chip is retired: region is selected only through search text, and `NEARBY_DEMO_REGION` is removed. Seed versions 3 and 4 re-serialize these fields for existing installations; saved memberships, journals and receipts are preserved as before. Photographer credits appear on Trail detail and Welcome, not on cards.
```

- [x] **Step 9: Verify and commit**

Run:

```bash
git diff --check && grep -ci "design revision R2" docs/trails-completion-contract.md docs/trails-reference-inventory.md && grep -c "R2 addendum" docs/superpowers/specs/2026-09-18-world-trails-seed-design.md
```

Expected: no whitespace errors; each count ≥ 1.

```bash
git add docs/trails-completion-contract.md docs/trails-m1-design-contract.md docs/trails-reference-inventory.md docs/trails-reference-deviations.md docs/superpowers/specs/2026-09-18-world-trails-seed-design.md
git commit -m "docs(design): record design revision R2 in the contracts

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

**R2 copy table (verbatim strings for Task 10):**

| Situation | String | Indicator and action |
| --- | --- | --- |
| Refresh failed, content kept (Explore) | `Couldn’t refresh · Showing saved trails` | Failed + `Try again` |
| Offline, no cached results (Explore) | `Offline · Saved trails are still here` | Offline + `Open Saved` |
| Query failed, no content | `Couldn’t load trails` | Failed + `Try again` |
| Draft count failed | `Couldn’t count trails · Filters are kept` | Failed + `Retry count` |
| Saving locally | `Saving on this device…` | Pending |
| Uncertain admission | `Checking your save…` | Pending; dark button `Check save` |
| Local admission failed | `Couldn’t save · Your choices are still here` | Failed |
| Committed, remote pending (card, row) | `Waiting to sync` | Pending |
| Committed, remote pending (detail, sheet) | `Saved on this device · Waiting to sync` | Pending |
| Syncing | `Syncing…` | Pending |
| Finishing adoption | `Finishing your save` | Pending |
| Retryable sync failure | `Sync needs attention · Local save is kept` | Failed + `Try again` |
| Parked, app incompatible | `App update needed to sync · Saved here` | Attention |
| Parked, other cause | `<repository reason> · Saved here` | Attention |
| Saved unavailable | `Couldn’t load your saved trails` | Failed + `Try again` |
| Collection unavailable | `This collection isn’t available` | Failed + `Try again` |
| Details missing | `Some trail details aren’t on this device yet` | Info + `Try again` |
| Detail refresh failed | `Couldn’t refresh · Showing this device’s copy` | Failed + `Try again` |
| Detail unavailable | `Not on this device yet` / `Couldn’t open this trail` | Failed; dark button `Try again` |
| Completion toast | `Saved to {collection}` / `Saved to {n} lists` / `Removed from saved` | Toast; action `View` except for removal |
| Navigation checkpoint failure | `Couldn’t save your place` | Failed + `Retry` |
| Bootstrap failure | `<message>` | Failed; dark button `Try again` |

---

### Task 2: R2 tokens and heading sizes

**Files:**
- Modify: `multiplatform/foundation/designsystem/src/commonMain/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/theme/Colors.kt`
- Modify: `multiplatform/foundation/designsystem/src/commonMain/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/theme/Typography.kt`
- Test: `multiplatform/foundation/designsystem/src/jvmTest/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/theme/TrailsTokensTest.kt`

**Interfaces:**
- Produces: `TrailsExtendedColors.onCitron`, `onDark`, `warning`, `danger`, `difficultyEasy`, `difficultyModerate`, `difficultyHard`, `difficultyStrenuous` (all `Color`), read through `TrailsTheme.colors`. Existing roles keep their names with R2 values.

- [x] **Step 1: Write the failing token test**

```kotlin
package org.mobilenativefoundation.trails.foundation.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class TrailsTokensTest {
    @Test
    fun lightTokensMatchDesignRevisionR2() {
        val colors = TrailsExtendedColorsLight
        assertEquals(Color(0xFFFFFFFF), colors.background)
        assertEquals(Color(0xFFFFFFFF), colors.surface)
        assertEquals(Color(0xFF171E14), colors.textPrimary)
        assertEquals(Color(0xFF545A52), colors.textSecondary)
        assertEquals(Color(0xFFE6E8E4), colors.border)
        assertEquals(Color(0xFF1D4B35), colors.accent)
        assertEquals(Color(0xFFA9F184), colors.citron)
        assertEquals(Color(0xFFF4F5F4), colors.soft)
        assertEquals(Color(0xFF0D1F18), colors.dark)
        assertEquals(Color(0xFF171E14), colors.onCitron)
        assertEquals(Color(0xFFFFFFFF), colors.onDark)
        assertEquals(Color(0xFF8A4B12), colors.warning)
        assertEquals(Color(0xFFA5352B), colors.danger)
        assertEquals(Color(0xFF43A047), colors.difficultyEasy)
        assertEquals(Color(0xFFF2B82E), colors.difficultyModerate)
        assertEquals(Color(0xFFEE6A45), colors.difficultyHard)
        assertEquals(Color(0xFF5E3A27), colors.difficultyStrenuous)
    }
}
```

- [x] **Step 2: Run it and keep the failure**

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest --tests "*TrailsTokensTest*" 2>&1 | tee /private/tmp/r2-task2-red.log
```

Expected: compilation fails with `Unresolved reference 'onCitron'` (the new roles do not exist yet).

- [x] **Step 3: Update the private colour constants in `Colors.kt`**

Replace the block from `private val Forest` through `private val Clay` with:

```kotlin
private val Forest = Color(0xFF1D4B35)
private val Citron = Color(0xFFA9F184)
private val Stone = Color(0xFFFFFFFF) // R2 background is white; the Stone name stays for source compatibility
private val Ink = Color(0xFF171E14)
private val Muted = Color(0xFF545A52)
private val Border = Color(0xFFE6E8E4)
private val Soft = Color(0xFFF4F5F4)
private val ForestDark = Color(0xFF0D1F18)
private val Clay = Color(0xFFBC624A)
private val Warning = Color(0xFF8A4B12)
private val Danger = Color(0xFFA5352B)
private val DifficultyEasy = Color(0xFF43A047)
private val DifficultyModerate = Color(0xFFF2B82E)
private val DifficultyHard = Color(0xFFEE6A45)
private val DifficultyStrenuous = Color(0xFF5E3A27)
```

- [x] **Step 4: Add the new roles to `TrailsExtendedColors`**

After `val errorAccent: Color = Clay,` inside the data class add:

```kotlin
    val onCitron: Color = Ink,
    val onDark: Color = White,
    val warning: Color = Warning,
    val danger: Color = Danger,
    val difficultyEasy: Color = DifficultyEasy,
    val difficultyModerate: Color = DifficultyModerate,
    val difficultyHard: Color = DifficultyHard,
    val difficultyStrenuous: Color = DifficultyStrenuous,
```

`TrailsExtendedColorsDark` and `TrailsExtendedColorsLight` need no edits: the new roles take their defaults and the light copy already overrides `background`, `surface`, `textPrimary`, `textSecondary`, `border`, `accent`, `onAccent`, `soft` and `attention`.

- [x] **Step 5: Set the two R2 heading sizes in `Typography.kt`**

In `buildTrailsTypography` replace the `headlineLarge` and `headlineMedium` lines with:

```kotlin
    headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
```

- [x] **Step 6: Run the test to verify it passes**

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest --tests "*TrailsTokensTest*"
```

Expected: `BUILD SUCCESSFUL`, 1 test passed.

- [x] **Step 7: Commit**

```bash
git add multiplatform/foundation/designsystem
git commit -m "feat(design): adopt design revision R2 tokens and heading sizes

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 3: Button hierarchy, icon circle and icon additions

**Files:**
- Modify: `multiplatform/foundation/designsystem/build.gradle.kts`
- Create: `multiplatform/foundation/designsystem/src/commonMain/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/component/TrailsButton.kt`
- Modify: `…/designsystem/component/TrailsM1.kt` (`TrailsM1Button`)
- Modify: `…/designsystem/icon/Icons.kt`
- Test: `multiplatform/foundation/designsystem/src/jvmTest/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/component/TrailsButtonTest.kt`

**Interfaces:**
- Produces: `enum class ButtonTone { Hero, Commit, Secondary }`; `@Composable fun TrailsButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tone: ButtonTone = ButtonTone.Commit, enabled: Boolean = true, loading: Boolean = false, leadingIcon: Painter? = null, accessibilityLabel: String? = null)`; `@Composable fun TrailsIconCircle(icon: Painter, contentDescription: String?, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = TrailsTheme.colors.textPrimary, container: Color = TrailsTheme.colors.surface)`; icons `Icons.Outlined.HeartFilled`, `Star`, `Tick`, `Sync`, `Circle`, `Sparkles`, `MoreHorizontal` (the outline heart is the existing `Icons.Outlined.Favorite`).
- `TrailsM1Button` keeps its signature and delegates: `secondary = true` → `Secondary`, otherwise `Commit`. Later tasks switch hero call sites to `TrailsButton(tone = ButtonTone.Hero)`.

- [x] **Step 1: Add the desktop UI test harness to the design system module**

In `multiplatform/foundation/designsystem/build.gradle.kts`, inside `kotlin { sourceSets { … } }`, add after the `androidMain` block:

```kotlin
        jvmTest {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                runtimeOnly(compose.desktop.currentOs)
            }
        }
```

- [x] **Step 2: Write the failing pixel test**

```kotlin
package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class TrailsButtonTest {
    private fun fillOf(tone: ButtonTone): Color {
        var pixel = Color.Unspecified
        runDesktopComposeUiTest {
            setContent { TrailsTheme { TrailsButton("Save trail", onClick = {}, tone = tone, modifier = Modifier.testTag("button")) } }
            val image = onNodeWithTag("button").captureToImage()
            pixel = image.toPixelMap()[6, image.height / 2]
        }
        return pixel
    }

    @Test fun heroToneFillsCitron() = assertEquals(Color(0xFFA9F184), fillOf(ButtonTone.Hero))
    @Test fun commitToneFillsDark() = assertEquals(Color(0xFF0D1F18), fillOf(ButtonTone.Commit))
    @Test fun secondaryToneFillsSoft() = assertEquals(Color(0xFFF4F5F4), fillOf(ButtonTone.Secondary))
}
```

- [x] **Step 3: Run it and keep the failure**

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest --tests "*TrailsButtonTest*" 2>&1 | tee /private/tmp/r2-task3-red.log
```

Expected: compilation fails with `Unresolved reference 'ButtonTone'`.

- [x] **Step 4: Create `TrailsButton.kt`**

```kotlin
package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** R2 hierarchy: Hero is the one action a screen exists for, Commit applies or confirms, Secondary sits beside a primary. */
enum class ButtonTone { Hero, Commit, Secondary }

/** [accessibilityLabel] puts a modal invoker's label on the actionable node without a duplicate child. */
@Composable
fun TrailsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ButtonTone = ButtonTone.Commit,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: Painter? = null,
    accessibilityLabel: String? = null,
) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val container = when (tone) {
        ButtonTone.Hero -> colors.citron
        ButtonTone.Commit -> colors.dark
        ButtonTone.Secondary -> colors.soft
    }
    val content = when (tone) {
        ButtonTone.Hero -> colors.onCitron
        ButtonTone.Commit -> colors.onDark
        ButtonTone.Secondary -> colors.textPrimary
    }
    Button(
        onClick,
        modifier.heightIn(min = 52.dp).then(
            if (accessibilityLabel == null) Modifier
            else Modifier.semantics { this.text = AnnotatedString(accessibilityLabel) },
        ),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(TrailsTheme.radii.pill),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = if (tone == ButtonTone.Hero) colors.citron.copy(alpha = 0.55f) else colors.soft,
            disabledContentColor = colors.textSecondary,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = content, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        } else if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, Modifier.size(18.dp), tint = content)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            modifier = if (accessibilityLabel == null) Modifier else Modifier.clearAndSetSemantics {},
            style = typography.labelLarge,
        )
    }
}

/** White 48 dp circle with a soft shadow for icon actions over photos, maps and headers. */
@Composable
fun TrailsIconCircle(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TrailsTheme.colors.textPrimary,
    container: Color = TrailsTheme.colors.surface,
) {
    IconButton(
        onClick,
        modifier
            .size(48.dp)
            .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.16f), spotColor = Color.Black.copy(alpha = 0.16f))
            .clip(CircleShape)
            .background(container),
    ) { Icon(icon, contentDescription, Modifier.size(22.dp), tint = tint) }
}
```

- [x] **Step 5: Make `TrailsM1Button` delegate**

In `TrailsM1.kt` replace the whole `TrailsM1Button` function with:

```kotlin
/** Compatibility wrapper for existing call sites; new code uses [TrailsButton] and chooses a [ButtonTone]. */
@Composable
fun TrailsM1Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    secondary: Boolean = false,
    accessibilityLabel: String? = null,
) = TrailsButton(
    text, onClick, modifier,
    tone = if (secondary) ButtonTone.Secondary else ButtonTone.Commit,
    enabled = enabled, loading = loading, accessibilityLabel = accessibilityLabel,
)
```

Remove the now-unused `Button`, `ButtonDefaults`, `CircularProgressIndicator` and `PaddingValues` imports from `TrailsM1.kt` if the compiler reports them unused.

- [x] **Step 6: Add the icons**

In `Icons.kt`, inside `object Outlined`, add after `Cancel`:

```kotlin
        val HeartFilled
            @Composable
            get(): Icon = IconProvider(contentDescription = "HeartFilled") { painterResource(Res.drawable.favorite_solid_rounded) }

        val Star
            @Composable
            get(): Icon = IconProvider(contentDescription = "Star") { painterResource(Res.drawable.star_stroke_rounded) }

        val Tick
            @Composable
            get(): Icon = IconProvider(contentDescription = "Tick") { painterResource(Res.drawable.tick_02_stroke_rounded) }

        val Sync
            @Composable
            get(): Icon = IconProvider(contentDescription = "Sync") { painterResource(Res.drawable.arrow_reload_horizontal_stroke_rounded) }

        val Circle
            @Composable
            get(): Icon = IconProvider(contentDescription = "Circle") { painterResource(Res.drawable.circle_stroke_rounded) }

        val Sparkles
            @Composable
            get(): Icon = IconProvider(contentDescription = "Sparkles") { painterResource(Res.drawable.sparkles_solid_rounded) }

        val MoreHorizontal
            @Composable
            get(): Icon = IconProvider(contentDescription = "MoreHorizontal") { painterResource(Res.drawable.more_horizontal_stroke_rounded) }
```

All seven drawables already exist under `composeResources/drawable`.

- [x] **Step 7: Run the tests and compile the consumers**

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest --tests "*TrailsButtonTest*" :multiplatform:app:core:compileKotlinJvm
```

Expected: 3 tests pass; app core still compiles (every existing `TrailsM1Button` call site now renders dark or soft).

- [x] **Step 8: Commit**

```bash
git add multiplatform/foundation/designsystem
git commit -m "feat(design): add the R2 button hierarchy, icon circle and icons

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 4: Strenuous difficulty for the treks, marker component and seed version 3

**Files:**
- Modify: `multiplatform/data/trail/api/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/TrailData.kt`
- Modify: `multiplatform/data/trail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/WorldTrails.kt`
- Modify: `multiplatform/data/trail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/M1Backend.kt`
- Create: `…/designsystem/component/DifficultyMarker.kt`
- Test: `multiplatform/data/trail/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/data/trail/TrailQueryMatchTest.kt` (new)
- Test: `multiplatform/data/trail/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/data/trail/WorldTrailsTest.kt` (extend)
- Test: `multiplatform/foundation/designsystem/src/jvmTest/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/component/DifficultyMarkerTest.kt`

**Interfaces:**
- Produces: `TrailDifficulty.STRENUOUS`, carried by the eight multi-day treks (`classic-inca-trail`, `everest-base-camp-trek`, `tour-du-mont-blanc`, `milford-track`, `torres-del-paine-w-trek`, `kilimanjaro-machame-route`, `laugavegur-trail`, `overland-track`); `SEED_VERSION = 3L`; `enum class DifficultyMarkerKind { EASY, MODERATE, HARD, STRENUOUS }`; `fun TrailsExtendedColors.difficulty(kind: DifficultyMarkerKind): Color`; `@Composable fun DifficultyMarker(kind: DifficultyMarkerKind, modifier: Modifier = Modifier)`.
- Consumes: Task 2 difficulty colours; the versioned reseed committed in `481346e` (`M1Backend.init` clears `backend_trail` and `RealTrailDataFactory` clears the catalog cache whenever `backend_meta.seeded != SEED_VERSION`; `TrailReseedTest` already proves saved memberships survive). No upsert query is added; bumping the version is the whole migration.

- [x] **Step 1: Write the failing matcher test**

```kotlin
package org.mobilenativefoundation.trails.data.trail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrailQueryMatchTest {
    @Test
    fun strenuousFilterSelectsExactlyTheEightTreks() {
        val query = TrailQuery(difficulties = setOf(TrailDifficulty.STRENUOUS)).normalized()
        val strenuous = worldTrails.filter { it.matches(query) }
        assertEquals(worldTrails.takeLast(8).map { it.id }, strenuous.map { it.id })
        assertTrue(strenuous.all { it.durationMinutes >= 1440 })
    }
}
```

- [x] **Step 2: Extend the catalog test**

In `WorldTrailsTest.recommendedOrderStartsWithPinnedDayHikesAndEndsWithEightTreks` add after the `% 1440 == 0` assertion:

```kotlin
        assertTrue(worldTrails.take(42).none { it.difficulty == TrailDifficulty.STRENUOUS })
        assertTrue(worldTrails.takeLast(8).all { it.difficulty == TrailDifficulty.STRENUOUS })
        assertEquals(
            mapOf(TrailDifficulty.EASY to 5, TrailDifficulty.MODERATE to 16, TrailDifficulty.HARD to 21, TrailDifficulty.STRENUOUS to 8),
            worldTrails.groupingBy { it.difficulty }.eachCount(),
        )
```

- [x] **Step 3: Run both and keep the failure**

```bash
./gradlew :multiplatform:data:trail:impl:jvmTest --tests "*TrailQueryMatchTest*" --tests "*WorldTrailsTest*" 2>&1 | tee /private/tmp/r2-task4-red.log
```

Expected: compilation fails with `Unresolved reference 'STRENUOUS'`.

- [x] **Step 4: Add the difficulty value**

In `TrailData.kt` replace the enum with:

```kotlin
@Serializable
enum class TrailDifficulty { EASY, MODERATE, HARD, STRENUOUS }
```

Cached rows, journals and navigation checkpoints only ever contain the three earlier names, so they decode unchanged. The filter sheet iterates `TrailDifficulty.entries`, so it shows the new value without a code change until Task 7 rebuilds it.

- [x] **Step 5: Grade the eight treks Strenuous**

In `WorldTrails.kt`, in each of the eight `trail(` entries whose `id` is listed under Interfaces (the last eight entries of the list; their `minutes` is a multiple of 1440), replace `difficulty = TrailDifficulty.HARD,` (six treks) or `difficulty = TrailDifficulty.MODERATE,` (`milford-track`, `laugavegur-trail`) with `difficulty = TrailDifficulty.STRENUOUS,`. Then add these two lines to the file's KDoc after the line ending `with persisted records.`:

```kotlin
 * The eight multi-day treks carry the design revision R2 Strenuous tier because they are overnight
 * itineraries; day hikes keep the source-led grades recorded in the catalog specification.
```

- [x] **Step 6: Bump the seed version**

In `M1Backend.kt` replace `internal const val SEED_VERSION = 2L` with:

```kotlin
/** 3: design revision R2 grades the eight treks Strenuous. Installed builds reseed on the next open. */
internal const val SEED_VERSION = 3L
```

- [x] **Step 7: Run the data suite to verify it passes**

```bash
./gradlew :multiplatform:data:trail:impl:jvmTest
```

Expected: every test passes, including the new matcher test, the extended catalog test and the existing `TrailReseedTest`, `TrailPersistenceTest`, `TrailReviewPersistenceTest`, `TrailFaultRecoveryTest` and `StorageLifetimeTest`.

- [x] **Step 8: Write the failing marker colour test**

```kotlin
package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsExtendedColorsLight

class DifficultyMarkerTest {
    @Test
    fun eachKindMapsToItsR2Colour() {
        val colors = TrailsExtendedColorsLight
        assertEquals(Color(0xFF43A047), colors.difficulty(DifficultyMarkerKind.EASY))
        assertEquals(Color(0xFFF2B82E), colors.difficulty(DifficultyMarkerKind.MODERATE))
        assertEquals(Color(0xFFEE6A45), colors.difficulty(DifficultyMarkerKind.HARD))
        assertEquals(Color(0xFF5E3A27), colors.difficulty(DifficultyMarkerKind.STRENUOUS))
    }
}
```

Run `./gradlew :multiplatform:foundation:designsystem:jvmTest --tests "*DifficultyMarkerTest*"`; expected failure `Unresolved reference 'DifficultyMarkerKind'`.

- [x] **Step 9: Create `DifficultyMarker.kt`**

```kotlin
package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsExtendedColors
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** Mirrors the Figma set `Trails / Difficulty marker`: circle, rounded square, triangle, diamond. */
enum class DifficultyMarkerKind { EASY, MODERATE, HARD, STRENUOUS }

fun TrailsExtendedColors.difficulty(kind: DifficultyMarkerKind): Color = when (kind) {
    DifficultyMarkerKind.EASY -> difficultyEasy
    DifficultyMarkerKind.MODERATE -> difficultyModerate
    DifficultyMarkerKind.HARD -> difficultyHard
    DifficultyMarkerKind.STRENUOUS -> difficultyStrenuous
}

/** Decorative. The adjacent difficulty text carries the meaning, so the marker adds no semantics node. */
@Composable
fun DifficultyMarker(kind: DifficultyMarkerKind, modifier: Modifier = Modifier) {
    val color = TrailsTheme.colors.difficulty(kind)
    Canvas(modifier.size(12.dp)) {
        val w = size.width
        val h = size.height
        when (kind) {
            DifficultyMarkerKind.EASY -> drawCircle(color)
            DifficultyMarkerKind.MODERATE -> drawRoundRect(color, cornerRadius = CornerRadius(w * 0.22f))
            DifficultyMarkerKind.HARD -> drawPath(Path().apply { moveTo(w / 2f, 0f); lineTo(w, h); lineTo(0f, h); close() }, color)
            DifficultyMarkerKind.STRENUOUS -> drawPath(Path().apply { moveTo(w / 2f, 0f); lineTo(w, h / 2f); lineTo(w / 2f, h); lineTo(0f, h / 2f); close() }, color)
        }
    }
}
```

- [x] **Step 10: Run the marker test and the existing data suites**

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest --tests "*DifficultyMarkerTest*" :multiplatform:data:trail:impl:jvmTest
```

Expected: the marker test passes and the data suite stays green.

- [x] **Step 11: Commit**

```bash
git add multiplatform/data/trail multiplatform/foundation/designsystem
git commit -m "feat(trail): grade the eight treks Strenuous and add the difficulty marker component

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 5: R2 query model, sort and derived activities

**Files:**
- Modify: `multiplatform/data/trail/api/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/TrailData.kt`
- Modify: `multiplatform/data/trail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/WorldTrails.kt`
- Modify: `multiplatform/data/trail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/TrailCatalog.kt`
- Modify: `multiplatform/data/trail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/M1Backend.kt`
- Test: `multiplatform/data/trail/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/data/trail/TrailQueryMatchTest.kt` (extend)
- Test: `multiplatform/data/trail/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/data/trail/WorldTrailsTest.kt` (extend)

**Interfaces:**
- Produces: `enum class TrailActivity { HIKING, BACKPACKING }`; `enum class TrailSort { MOST_POPULAR, HIGHEST_RATED, SHORTEST, LONGEST }`; `Trail.activities: Set<TrailActivity>` (derived in the catalog helper: every route is `HIKING`, the eight treks add `BACKPACKING`); `TrailQuery.minElevationGain: Int`, `maxElevationGain: Int?`, `dogFriendly: Boolean`, `activities: Set<TrailActivity>`, `sort: TrailSort`; `internal fun TrailSort.comparator(): Comparator<Trail>`; `SEED_VERSION = 4L`.
- Every new field has a default, and kotlinx.serialization omits defaults, so existing cache keys, cached rows, journals and navigation checkpoints decode and match unchanged.
- Not modelled (DEV-30): highest point, kid-friendly and activities other than the two above. The catalog has no sourced value for them, and nothing is invented for a real route. The card highlight is derived in Task 6, not stored.

- [x] **Step 1: Add the failing tests**

Append to `TrailQueryMatchTest`:

```kotlin
    @Test
    fun activitySelectorsMatchAnySelectedActivityAndCombineWithDogFriendly() {
        val backpacking = TrailQuery(activities = setOf(TrailActivity.BACKPACKING)).normalized()
        assertEquals(worldTrails.takeLast(8).map { it.id }, worldTrails.filter { it.matches(backpacking) }.map { it.id })
        val dogFriendly = TrailQuery(dogFriendly = true, activities = setOf(TrailActivity.HIKING, TrailActivity.BACKPACKING)).normalized()
        val matched = worldTrails.filter { it.matches(dogFriendly) }
        assertEquals(12, matched.size)
        assertTrue(matched.all { TrailFeature.DOG_FRIENDLY in it.features })
    }

    @Test
    fun elevationGainBoundsAreInclusive() {
        val query = TrailQuery(minElevationGain = 1463, maxElevationGain = 1463).normalized()
        assertEquals(listOf("half-dome"), worldTrails.filter { it.matches(query) }.map { it.id })
    }

    @Test
    fun mostPopularKeepsCatalogOrderAndTheOtherSortsReorderIt() {
        assertEquals(worldTrails.map { it.id }, worldTrails.sortedWith(TrailSort.MOST_POPULAR.comparator()).map { it.id })
        assertEquals("diamond-head-summit-trail", worldTrails.sortedWith(TrailSort.SHORTEST.comparator()).first().id)
        assertEquals("tour-du-mont-blanc", worldTrails.sortedWith(TrailSort.LONGEST.comparator()).first().id)
        assertEquals(4.9, worldTrails.sortedWith(TrailSort.HIGHEST_RATED.comparator()).first().rating)
    }

    @Test
    fun defaultSelectorsKeepTheExistingCacheKey() {
        assertEquals("{}", Json.encodeToString(TrailQuery.serializer(), TrailQuery().normalized()))
    }

    @Test
    fun legacyTrailJsonDecodesWithR2Defaults() {
        val legacy = """{"id":"x","name":"X","region":"R","description":"d","difficulty":"EASY","distanceMeters":1000,"elevationMeters":10,"durationMinutes":20,"rating":4.0,"reviewCount":1,"features":[],"photoIndex":0}"""
        assertEquals(emptySet(), Json.decodeFromString(Trail.serializer(), legacy).activities)
    }
```

Add `import kotlinx.serialization.json.Json` to the test file. In `WorldTrailsTest.catalogHasFiftyDistinctValidTrails` add inside the `forEach`, after the `reviewExcerpt` assertion:

```kotlin
            assertTrue(TrailActivity.HIKING in trail.activities, trail.id)
            assertEquals(trail.durationMinutes >= 1440, TrailActivity.BACKPACKING in trail.activities, trail.id)
```

- [x] **Step 2: Run and keep the failure**

```bash
./gradlew :multiplatform:data:trail:impl:jvmTest --tests "*TrailQueryMatchTest*" --tests "*WorldTrailsTest*" 2>&1 | tee /private/tmp/r2-task5-red.log
```

Expected: compilation fails with `Unresolved reference 'TrailActivity'`.

- [x] **Step 3: Extend the API models**

In `TrailData.kt` add after `TrailFeature`:

```kotlin
/** Derived from route facts, never authored per route: every route is hiked; multi-day treks are backpacked. */
@Serializable
enum class TrailActivity { HIKING, BACKPACKING }

/** Most popular is the catalog's documented recommended order; the others reorder the same membership. */
@Serializable
enum class TrailSort { MOST_POPULAR, HIGHEST_RATED, SHORTEST, LONGEST }
```

In `Trail` add after `val reviewExcerpt: String? = null,`:

```kotlin
    val activities: Set<TrailActivity> = emptySet(),
```

Replace `TrailQuery` with:

```kotlin
@Serializable
data class TrailQuery(
    val text: String = "",
    val region: String? = null,
    val difficulties: Set<TrailDifficulty> = emptySet(),
    val minMeters: Int = 0,
    val maxMeters: Int? = null,
    val features: Set<TrailFeature> = emptySet(),
    val minElevationGain: Int = 0,
    val maxElevationGain: Int? = null,
    val dogFriendly: Boolean = false,
    val activities: Set<TrailActivity> = emptySet(),
    val sort: TrailSort = TrailSort.MOST_POPULAR,
) {
    fun normalized(): TrailQuery = copy(
        text = text.trim().replace(Regex("\\s+"), " ").lowercase(),
        region = region?.trim()?.lowercase()?.takeIf { it.isNotEmpty() },
        minMeters = minMeters.coerceAtLeast(0),
        maxMeters = maxMeters?.coerceAtLeast(minMeters.coerceAtLeast(0)),
        difficulties = difficulties.sortedBy { it.name }.toSet(),
        features = features.sortedBy { it.name }.toSet(),
        minElevationGain = minElevationGain.coerceAtLeast(0),
        maxElevationGain = maxElevationGain?.coerceAtLeast(minElevationGain.coerceAtLeast(0)),
        activities = activities.sortedBy { it.name }.toSet(),
    )
}
```

- [x] **Step 4: Derive the activities in the catalog helper**

In `WorldTrails.kt`, inside `private fun trail(`, add after `reviewExcerpt = review,`:

```kotlin
    // Derived, not sourced: every catalog route is a hiking route; overnight itineraries are backpacking.
    activities = if (minutes >= 1440) setOf(TrailActivity.HIKING, TrailActivity.BACKPACKING) else setOf(TrailActivity.HIKING),
```

- [x] **Step 5: Extend the matcher, add the comparator and the sort**

In `TrailCatalog.kt` replace `matches` with:

```kotlin
internal fun Trail.matches(query: TrailQuery): Boolean =
    (query.text.isEmpty() || "${name.lowercase()} ${region.lowercase()}".contains(query.text)) &&
        (query.region == null || region.lowercase() == query.region) &&
        (query.difficulties.isEmpty() || difficulty in query.difficulties) &&
        distanceMeters >= query.minMeters && (query.maxMeters?.let { distanceMeters <= it } ?: true) &&
        elevationMeters >= query.minElevationGain && (query.maxElevationGain?.let { elevationMeters <= it } ?: true) &&
        (!query.dogFriendly || TrailFeature.DOG_FRIENDLY in features) &&
        (query.activities.isEmpty() || activities.any { it in query.activities }) &&
        features.containsAll(query.features)

/** `sortedWith` is stable, so the zero comparator keeps the catalog's recommended order. */
internal fun TrailSort.comparator(): Comparator<Trail> = when (this) {
    TrailSort.MOST_POPULAR -> Comparator { _, _ -> 0 }
    TrailSort.HIGHEST_RATED -> compareByDescending<Trail> { it.rating }.thenByDescending { it.reviewCount }
    TrailSort.SHORTEST -> compareBy { it.distanceMeters }
    TrailSort.LONGEST -> compareByDescending { it.distanceMeters }
}
```

In `M1Backend.search` replace `.filter { it.matches(query.normalized()) }` with:

```kotlin
                .filter { it.matches(query.normalized()) }
                .sortedWith(query.sort.comparator())
```

and, in the same file, replace the `SEED_VERSION` declaration from Task 4 so installed rows are re-serialized with `activities`:

```kotlin
/** 4: design revision R2 serializes the derived activities (3 graded the treks Strenuous). Installed builds reseed on the next open. */
internal const val SEED_VERSION = 4L
```

- [x] **Step 6: Run the whole data suite**

```bash
./gradlew :multiplatform:data:trail:impl:jvmTest
```

Expected: all tests pass, including the five new matcher tests, the extended catalog test and the earlier persistence, review, fault, lifetime and reseed tests.

- [x] **Step 7: Commit**

```bash
git add multiplatform/data/trail
git commit -m "feat(trail): extend the query model with R2 selectors, sort and derived activities

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 6: R2 trail card

**Files:**
- Modify: `multiplatform/feat/savetrail/impl/build.gradle.kts`
- Modify: `multiplatform/feat/savetrail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/feat/savetrail/TrailSaveCard.kt`
- Modify: `multiplatform/foundation/designsystem/src/commonMain/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/component/TrailPhoto.kt` (credit overlay removed)
- Modify: `multiplatform/screen/welcome/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/welcome/WelcomeUi.kt` (one call site)
- Modify: `multiplatform/foundation/designsystem/TRAIL_PHOTOS.md`, `multiplatform/foundation/designsystem/ASSETS.md` (attribution notes)
- Test: `multiplatform/feat/savetrail/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/feat/savetrail/TrailSaveCardTest.kt`

**Interfaces:**
- Produces: `fun TrailDifficulty.markerKind(): DifficultyMarkerKind`; `fun Trail.highlight(): String?` (derived from duration and feature tags; nothing is authored per route); `@Composable fun TrailFactsRow(trail: Trail, modifier: Modifier = Modifier, showCount: Boolean = true, trailing: String = trailDistance(trail.distanceMeters))`; `TrailSaveCard` and `TrailBookmark` keep their signatures; `TrailPhoto(trailId: String, modifier: Modifier = Modifier, describeImage: Boolean = false)` loses `showCredit`. The bookmark is now a heart icon circle (`Icons.Outlined.Favorite` / `HeartFilled`) with unchanged labels `Save {name}` / `Edit saved collections for {name}`.
- Consumes: Task 3 `TrailsIconCircle`, Task 4 `DifficultyMarker`.
- Photographer credits leave cards and collection covers; `TrailPhotoCredit` on Trail detail (Task 9) and Welcome carries attribution, source and licence links.
- The R2 download circle is omitted (DEV-24); the mini route map is omitted (DEV-23).

- [x] **Step 1: Add the desktop UI test harness to the module**

In `multiplatform/feat/savetrail/impl/build.gradle.kts` add inside `sourceSets`:

```kotlin
        jvmTest {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                runtimeOnly(compose.desktop.currentOs)
            }
        }
```

- [x] **Step 2: Write the failing card test**

```kotlin
package org.mobilenativefoundation.trails.feat.savetrail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import org.mobilenativefoundation.trails.data.trail.Trail
import org.mobilenativefoundation.trails.data.trail.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.TrailFeature
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class TrailSaveCardTest {
    private val trail = Trail(
        "half-dome", "Half Dome", "Yosemite National Park, USA", "Granite summit.", TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486,
        setOf(TrailFeature.WATERFALL, TrailFeature.SUMMIT, TrailFeature.FOREST), 2,
    )

    @Test
    fun cardShowsStructuredFactsHighlightAndHeartTarget() = runDesktopComposeUiTest {
        setContent { TrailsTheme { TrailSaveCard(trail, snapshot = null, onOpen = {}, onSave = {}) } }
        onNodeWithText("4.9 (2486)").assertIsDisplayed()
        onNodeWithText("Hard").assertIsDisplayed()
        onNodeWithText("22.7 km").assertIsDisplayed()
        onNodeWithText("Waterfall").assertIsDisplayed()
        onNodeWithText("Save Half Dome").assertIsDisplayed()
    }
}
```

- [x] **Step 3: Run and keep the failure**

```bash
./gradlew :multiplatform:feat:savetrail:impl:jvmTest --tests "*TrailSaveCardTest*" 2>&1 | tee /private/tmp/r2-task6-red.log
```

Expected: the test fails at `onNodeWithText("4.9 (2486)")` (the current card renders one `★ 4.9 (2486)  ·  Hard  ·  22.7 km` string) and `Waterfall` is absent.

- [x] **Step 4: Rewrite the card, bookmark and facts row**

In `TrailSaveCard.kt` replace `TrailSaveCard` and `TrailBookmark` with the code below and add `markerKind`, `TrailFactsRow` and `HighlightChip`. Keep `displayName`, `trailDistance`, `trailDuration`, `syncLabel`, `TrailSyncNotice`, `SavedSyncNotice`, `M1Loading` and `M1Heading` unchanged in this task (Task 10 migrates the notices).

```kotlin
fun TrailDifficulty.markerKind(): DifficultyMarkerKind = when (this) {
    TrailDifficulty.EASY -> DifficultyMarkerKind.EASY
    TrailDifficulty.MODERATE -> DifficultyMarkerKind.MODERATE
    TrailDifficulty.HARD -> DifficultyMarkerKind.HARD
    TrailDifficulty.STRENUOUS -> DifficultyMarkerKind.STRENUOUS
}

/** Derived from the catalog's feature tags and duration; nothing is authored per route. */
fun Trail.highlight(): String? = when {
    durationMinutes >= 1440 -> "Multi-day"
    TrailFeature.WATERFALL in features -> "Waterfall"
    TrailFeature.SUMMIT in features -> "Summit views"
    TrailFeature.LAKE in features -> "Lakeside"
    TrailFeature.FOREST in features -> "Forest"
    else -> null
}

/** R2 facts line shared by cards, rows and the detail rating row; announced as one line. */
@Composable
fun TrailFactsRow(
    trail: Trail,
    modifier: Modifier = Modifier,
    showCount: Boolean = true,
    trailing: String = trailDistance(trail.distanceMeters),
) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Row(
        modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Star.painter, contentDescription = null, Modifier.size(14.dp), tint = colors.textPrimary)
        Text(if (showCount) "${trail.rating} (${trail.reviewCount})" else "${trail.rating}", style = typography.bodyMedium, color = colors.textSecondary)
        Text("·", style = typography.bodyMedium, color = colors.textSecondary)
        DifficultyMarker(trail.difficulty.markerKind())
        Text(trail.difficulty.displayName(), style = typography.bodyMedium, color = colors.textSecondary)
        Text("·", style = typography.bodyMedium, color = colors.textSecondary)
        Text(trailing, style = typography.bodyMedium, color = colors.textSecondary)
    }
}

@Composable
private fun HighlightChip(text: String, modifier: Modifier = Modifier) {
    val colors = TrailsTheme.colors
    val shape = RoundedCornerShape(TrailsTheme.radii.pill)
    Row(
        modifier.shadow(4.dp, shape).clip(shape).background(colors.surface).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Eye.painter, contentDescription = null, Modifier.size(14.dp), tint = colors.textPrimary)
        Text(text, style = TrailsTheme.typography.labelMedium, color = colors.textPrimary)
    }
}

/** A shared save affordance with separate card navigation and heart touch targets. */
@Composable
fun TrailSaveCard(trail: Trail, snapshot: SavedSnapshot?, onOpen: () -> Unit, onSave: (() -> Unit) -> Unit, modifier: Modifier = Modifier) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val saved = snapshot?.memberships?.get(trail.id)?.isNotEmpty()
    val sync = snapshot?.syncByTrail?.get(trail.id)
    Column(modifier.fillMaxWidth().clickable(onClick = onOpen), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().height(233.dp).clip(RoundedCornerShape(TrailsTheme.radii.lg))) {
            TrailPhoto(trail.id, modifier = Modifier.fillMaxSize())
            trail.highlight()?.let { HighlightChip(it, Modifier.align(Alignment.TopStart).padding(12.dp)) }
            TrailBookmark(trail.name, saved, onSave, Modifier.align(Alignment.TopEnd).padding(12.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(trail.name, style = typography.titleSmall.copy(fontSize = 18.sp, lineHeight = 24.sp), color = colors.textPrimary)
            Text(trail.region, style = typography.bodyMedium, color = colors.textSecondary)
            TrailFactsRow(trail)
            syncLabel(sync, snapshot?.offline == true)?.let { Text(it, style = typography.labelSmall, color = colors.accent) }
        }
    }
}

@Composable
fun TrailBookmark(name: String, saved: Boolean?, onClick: (() -> Unit) -> Unit, modifier: Modifier = Modifier) {
    val colors = TrailsTheme.colors
    val focusReturn = rememberTrailsFocusReturnTarget()
    val label = if (saved == true) "Edit saved collections for $name" else "Save $name"
    TrailsIconCircle(
        icon = if (saved == true) Icons.Outlined.HeartFilled.painter else Icons.Outlined.Favorite.painter,
        contentDescription = null,
        onClick = { onClick(focusReturn::restore) },
        modifier = modifier.then(focusReturn.modifier).semantics {
            text = AnnotatedString(label)
            stateDescription = when (saved) { true -> "Saved"; false -> "Not saved"; null -> "Saved status unavailable" }
        },
        tint = if (saved == true) colors.accent else colors.textPrimary,
    )
}
```

Add these imports to the file: `androidx.compose.ui.draw.shadow`, `androidx.compose.ui.unit.sp`, `org.mobilenativefoundation.trails.foundation.designsystem.component.DifficultyMarker`, `org.mobilenativefoundation.trails.foundation.designsystem.component.DifficultyMarkerKind`, `org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsIconCircle` (the wildcard `designsystem.component.*` import already present covers the last three). Remove the now-unused `IconButton` and `CircleShape` imports if flagged.

- [x] **Step 5: Move attribution off the photograph**

In `TrailPhoto.kt` replace the `TrailPhoto` composable with:

```kotlin
/** Bundled location photography, selected by stable trail ID so legacy caches need no migration. Attribution lives in [TrailPhotoCredit]. */
@Composable
fun TrailPhoto(trailId: String, modifier: Modifier = Modifier, describeImage: Boolean = false) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val photo = trailPhotographs[trailId]
    Box(modifier.background(colors.soft)) {
        if (photo == null) {
            Text("Photo unavailable", Modifier.align(Alignment.Center), color = colors.textSecondary, style = typography.bodySmall)
        } else {
            Image(
                painterResource(photo.image),
                contentDescription = if (describeImage) photo.description else null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alignment = photo.alignment,
            )
        }
    }
}
```

Remove the now-unused imports `androidx.compose.foundation.layout.fillMaxWidth`, `androidx.compose.foundation.layout.padding` and `androidx.compose.ui.graphics.Color`. `TrailPhotoCredit` is unchanged.

In `WelcomeUi.kt` replace `TrailPhoto(trailId = "half-dome", modifier = Modifier.fillMaxSize(), showCredit = false)` with `TrailPhoto("half-dome", modifier = Modifier.fillMaxSize())`.

In `TRAIL_PHOTOS.md` replace the sentence `Photographer credits appear on cards and collection covers, with source and license links on trail details and Welcome.` with `Photographer credits, with source and license links, appear on trail details and Welcome; cards and collection covers show the photograph alone.` In `ASSETS.md` replace `Credits appear with the image, and detail and Welcome provide source and license links.` with `Credits, source and license links appear on trail detail and Welcome through TrailPhotoCredit; cards and collection tiles show the photograph alone.`

- [x] **Step 6: Run the card test and compile the screens**

```bash
./gradlew :multiplatform:feat:savetrail:impl:jvmTest --tests "*TrailSaveCardTest*" :multiplatform:foundation:designsystem:compileKotlinJvm :multiplatform:app:core:compileKotlinJvm
```

Expected: 1 test passes; the design system, Welcome, Explore, Saved, Collection and Trail detail screens compile with the new card, heart and photo signature.

- [x] **Step 7: Commit**

```bash
git add multiplatform/feat/savetrail multiplatform/foundation/designsystem multiplatform/screen/welcome
git commit -m "feat(savetrail): render the R2 trail card with heart, highlight chip and structured facts

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 7: R2 filter sheet

**Files:**
- Modify: `multiplatform/feat/filters/api/src/commonMain/kotlin/org/mobilenativefoundation/trails/feat/filters/FiltersFeature.kt`
- Modify: `multiplatform/feat/filters/impl/build.gradle.kts`
- Modify: `multiplatform/feat/filters/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/feat/filters/RealFiltersFeature.kt`
- Modify: `multiplatform/foundation/designsystem/src/commonMain/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/component/TrailsM1.kt` (`TrailsM1Chip` gains `leadingIcon`)
- Modify: `multiplatform/screen/explore/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/explore/ExplorePresenter.kt` (one call site)
- Test: `multiplatform/feat/filters/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/feat/filters/RealFiltersFeatureTest.kt`

**Interfaces:**
- Produces: `enum class FilterSection { ALL, DIFFICULTY, LENGTH, ELEVATION }`; `suspend fun FiltersFeature.show(query: TrailQuery, section: FilterSection = FilterSection.ALL, onDismiss: () -> Unit = {}): TrailQuery?`; `TrailsM1Chip(…, leadingIcon: (@Composable () -> Unit)? = null)`.
- Consumes: Task 3 `TrailsButton`/`TrailsIconCircle`, Task 4 `DifficultyMarker`, Task 5 selectors, Task 6 `displayName()`/`markerKind()` (the filters module gains a dependency on `feat/savetrail/impl`, mirroring `screen/explore/impl`).
- Distance away is omitted (DEV-25). Length keeps two labelled sliders; Elevation gain uses one maximum slider (DEV-27). Highest point and Kid-friendly are omitted (DEV-30); the Activity chips are Hiking and Backpacking. Draft-count failure keeps `TrailsM1Notice` until Task 10.

- [x] **Step 1: Extend the API**

Replace `FiltersFeature.kt` with:

```kotlin
package org.mobilenativefoundation.trails.feat.filters

import androidx.compose.runtime.Composable
import org.mobilenativefoundation.trails.data.trail.TrailQuery

/** Sheet section an Explore chip asks the sheet to scroll to. */
enum class FilterSection { ALL, DIFFICULTY, LENGTH, ELEVATION }

interface FiltersFeature {
    /**
     * Returns only an explicitly applied draft; dismissal leaves the original query unchanged.
     * [onDismiss] is an ephemeral invoker callback, excluded when the caller is cancelled/replaced.
     */
    suspend fun show(query: TrailQuery, section: FilterSection = FilterSection.ALL, onDismiss: () -> Unit = {}): TrailQuery?
    @Composable fun Content()
}
```

In `ExplorePresenter.kt` change the call `filters.show(query.copy(text = text), intent.onDismiss)` to `filters.show(query.copy(text = text), onDismiss = intent.onDismiss)` so it keeps compiling until Task 8 adds sections.

- [x] **Step 2: Add the harness and the savetrail dependency to the filters module**

In `multiplatform/feat/filters/impl/build.gradle.kts` add `implementation(projects.multiplatform.feat.savetrail.impl)` to `commonMain` dependencies and add:

```kotlin
        jvmTest {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                runtimeOnly(compose.desktop.currentOs)
            }
        }
```

- [x] **Step 3: Give chips an optional leading icon**

In `TrailsM1.kt` replace `TrailsM1Chip` with:

```kotlin
@Composable
fun TrailsM1Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        label = { Text(text, style = typography.labelMedium) },
        leadingIcon = leadingIcon,
        shape = RoundedCornerShape(TrailsTheme.radii.pill),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.soft,
            labelColor = colors.textPrimary,
            selectedContainerColor = colors.accent,
            selectedLabelColor = colors.onAccent,
        ),
    )
}
```

- [x] **Step 4: Write the failing sheet test**

```kotlin
package org.mobilenativefoundation.trails.feat.filters

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class RealFiltersFeatureTest {
    private class CountingRepository : TrailRepository {
        override fun observeQuery(query: TrailQuery): Flow<LoadState<List<Trail>>> = flowOf(LoadState(emptyList(), loading = false))
        override fun observeTrail(id: String): Flow<LoadState<Trail>> = flowOf(LoadState(loading = false))
        override suspend fun refreshQuery(query: TrailQuery) {}
        override suspend fun refreshTrail(id: String) {}
        override suspend fun count(query: TrailQuery): LoadState<Int> = LoadState(3, loading = false)
    }

    @Test
    fun dogFriendlySwitchAndStrenuousChipReachTheAppliedQuery() = runDesktopComposeUiTest {
        val feature = RealFiltersFeature(CountingRepository())
        val applied = CoroutineScope(Dispatchers.Unconfined).async { feature.show(TrailQuery()) }
        setContent { TrailsTheme { feature.Content() } }
        waitForIdle()
        onNodeWithText("Dog-friendly").performClick()
        onNodeWithText("Strenuous").performClick()
        waitUntil(5_000) { onAllNodesWithText("Show 3 trails").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Show 3 trails").performClick()
        val result = runBlocking { withTimeout(5_000) { applied.await() } }!!
        assertTrue(result.dogFriendly)
        assertEquals(setOf(TrailDifficulty.STRENUOUS), result.difficulties)
    }
}
```

- [x] **Step 5: Run and keep the failure**

```bash
./gradlew :multiplatform:feat:filters:impl:jvmTest --tests "*RealFiltersFeatureTest*" 2>&1 | tee /private/tmp/r2-task7-red.log
```

Expected: the test fails at `onNodeWithText("Dog-friendly")` (the current sheet has no such row; its feature checkboxes are Lakes & water, Forest shade and Big views).

- [x] **Step 6: Rewrite the sheet**

Replace the `show` function and the `Content` composable in `RealFiltersFeature.kt` with the following (the class header, `Request` and imports stay; add the imports listed after the code):

```kotlin
    private class Request(val original: TrailQuery, val section: FilterSection, val result: CompletableDeferred<TrailQuery?>, val onDismiss: () -> Unit)
    private var request by mutableStateOf<Request?>(null)

    override suspend fun show(query: TrailQuery, section: FilterSection, onDismiss: () -> Unit): TrailQuery? {
        request?.result?.complete(null)
        val next = Request(query, section, CompletableDeferred(), onDismiss)
        request = next
        var completed = false
        return try { next.result.await().also { completed = true } } finally {
            if (request === next) {
                request = null
                // Account/screen cancellation or replacement never pulls focus to an old caller.
                if (completed) next.onDismiss()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val current = request ?: return
        key(current) {
            val colors = TrailsTheme.colors
            val typography = TrailsTheme.typography
            var draft by remember { mutableStateOf(current.original) }
            var count by remember { mutableStateOf(LoadState<Int>()) }
            var countFor by remember { mutableStateOf<TrailQuery?>(null) }
            var retry by remember { mutableIntStateOf(0) }
            val scroll = rememberScrollState()
            val sectionOffsets = remember { mutableStateMapOf<FilterSection, Int>() }
            fun update(next: TrailQuery) { draft = next; count = LoadState(); countFor = null }
            LaunchedEffect(draft, retry) {
                val selected = draft
                delay(180)
                val result = try { repository.count(selected.normalized()) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { LoadState<Int>(loading = false, error = failure.message ?: "Couldn’t count trails") }
                if (draft == selected) { count = result; countFor = selected }
            }
            val sectionTarget = sectionOffsets[current.section]
            LaunchedEffect(current.section, sectionTarget) {
                if (current.section != FilterSection.ALL && sectionTarget != null) scroll.animateScrollTo(sectionTarget)
            }
            val matchingCount = countFor == draft && !count.loading
            val rangeColors = SliderDefaults.colors(thumbColor = colors.surface, activeTrackColor = colors.textPrimary, inactiveTrackColor = colors.border)
            ModalBottomSheet(
                onDismissRequest = { current.result.complete(null) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = colors.surface,
                shape = RoundedCornerShape(topStart = TrailsTheme.radii.sheet, topEnd = TrailsTheme.radii.sheet),
            ) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(scroll).padding(horizontal = 24.dp)
                        .navigationBarsPadding().imePadding().padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Filters", style = typography.headlineMedium, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                        TrailsIconCircle(Icons.Outlined.Cancel.painter, "Close filters", { current.result.complete(null) }, container = colors.soft)
                    }

                    SectionHeading("Difficulty", FilterSection.DIFFICULTY, sectionOffsets)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrailDifficulty.entries.forEach { difficulty ->
                            TrailsM1Chip(
                                difficulty.displayName(), difficulty in draft.difficulties,
                                { update(draft.copy(difficulties = if (difficulty in draft.difficulties) draft.difficulties - difficulty else draft.difficulties + difficulty)) },
                                leadingIcon = { DifficultyMarker(difficulty.markerKind()) },
                            )
                        }
                    }

                    SectionHeading("Length", FilterSection.LENGTH, sectionOffsets)
                    val minimumKm = draft.minMeters / 1000
                    val maximumKm = (draft.maxMeters ?: MAX_LENGTH_METERS) / 1000
                    Text("$minimumKm–${draft.maxMeters?.let { "$maximumKm" } ?: "${MAX_LENGTH_METERS / 1000}+"} km", style = typography.bodyMedium, color = colors.textSecondary)
                    Column {
                        Text("Minimum length · $minimumKm km", style = typography.bodyMedium, color = colors.textPrimary)
                        Slider(
                            value = minimumKm.toFloat(),
                            onValueChange = { value -> update(draft.copy(minMeters = value.roundToInt().coerceIn(0, maximumKm) * 1000)) },
                            enabled = maximumKm > 0,
                            valueRange = 0f..maximumKm.toFloat(),
                            steps = (maximumKm - 1).coerceAtLeast(0),
                            colors = rangeColors,
                            modifier = Modifier.semantics { text = AnnotatedString("Minimum length"); stateDescription = "$minimumKm kilometers" },
                        )
                    }
                    Column {
                        Text("Maximum length · ${draft.maxMeters?.let { "$maximumKm km" } ?: "No maximum"}", style = typography.bodyMedium, color = colors.textPrimary)
                        Slider(
                            value = maximumKm.toFloat(),
                            onValueChange = { value ->
                                val maximum = value.roundToInt().coerceIn(minimumKm, MAX_LENGTH_METERS / 1000) * 1000
                                update(draft.copy(maxMeters = maximum.takeIf { it < MAX_LENGTH_METERS }))
                            },
                            enabled = minimumKm < MAX_LENGTH_METERS / 1000,
                            valueRange = minimumKm.toFloat()..(MAX_LENGTH_METERS / 1000).toFloat(),
                            steps = (MAX_LENGTH_METERS / 1000 - minimumKm - 1).coerceAtLeast(0),
                            colors = rangeColors,
                            modifier = Modifier.semantics { text = AnnotatedString("Maximum length"); stateDescription = draft.maxMeters?.let { "$maximumKm kilometers" } ?: "No maximum length" },
                        )
                    }

                    SectionHeading("Elevation gain", FilterSection.ELEVATION, sectionOffsets)
                    MaximumSlider(
                        label = "Maximum elevation gain", value = draft.maxElevationGain, top = MAX_GAIN_METERS, step = 100, colors = rangeColors,
                        onChange = { update(draft.copy(maxElevationGain = it)) },
                    )

                    SwitchRow("Dog-friendly", draft.dogFriendly) { update(draft.copy(dogFriendly = it)) }

                    Text("Activity", style = typography.titleLarge, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrailActivity.entries.forEach { activity ->
                            TrailsM1Chip(activity.displayName(), activity in draft.activities, {
                                update(draft.copy(activities = if (activity in draft.activities) draft.activities - activity else draft.activities + activity))
                            })
                        }
                    }

                    Text("Trail features", style = typography.titleLarge, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                    listOf(TrailFeature.LAKE to "Lakes & water", TrailFeature.FOREST to "Forest shade", TrailFeature.SUMMIT to "Big views").forEach { (feature, label) ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleableRow(feature in draft.features) {
                                update(draft.copy(features = if (feature in draft.features) draft.features - feature else draft.features + feature))
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = feature in draft.features, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = colors.accent))
                            Spacer(Modifier.width(12.dp))
                            Text(label, style = typography.bodyLarge, color = colors.textPrimary)
                        }
                    }

                    if (matchingCount && count.error != null) TrailsM1Notice("Couldn’t count trails · Filters are kept", isError = true, actionLabel = "Retry count", onAction = { count = LoadState(); countFor = null; retry++ })
                    TrailsButton(
                        text = when { !matchingCount -> "Counting trails…"; count.data != null -> "Show ${count.data} ${if (count.data == 1) "trail" else "trails"}"; else -> "Apply filters" },
                        onClick = { current.result.complete(draft) },
                        tone = ButtonTone.Commit,
                        enabled = matchingCount,
                        loading = !matchingCount,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TrailsButton("Cancel", { current.result.complete(null) }, tone = ButtonTone.Secondary, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
```

Add these top-level declarations at the bottom of the file:

```kotlin
private const val MAX_LENGTH_METERS = 50_000
private const val MAX_GAIN_METERS = 2_000

private fun metres(value: Int): String = value.toString().reversed().chunked(3).joinToString(",").reversed() + " m"

fun TrailActivity.displayName(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

@Composable
private fun SectionHeading(text: String, section: FilterSection, offsets: MutableMap<FilterSection, Int>) {
    Text(
        text, style = TrailsTheme.typography.titleLarge, color = TrailsTheme.colors.textPrimary,
        modifier = Modifier.onGloballyPositioned { offsets[section] = it.positionInParent().y.roundToInt() }.semantics { heading() },
    )
}

/** One labelled native slider; the top stop means no maximum (DEV-27). */
@Composable
private fun MaximumSlider(label: String, value: Int?, top: Int, step: Int, colors: SliderColors, onChange: (Int?) -> Unit) {
    val typography = TrailsTheme.typography
    val palette = TrailsTheme.colors
    val shown = value?.let { metres(it) } ?: "No maximum"
    Column {
        Text("$label · $shown", style = typography.bodyMedium, color = palette.textPrimary)
        Slider(
            value = (value ?: top).toFloat(),
            onValueChange = { raw -> val rounded = (raw / step).roundToInt() * step; onChange(rounded.takeIf { it < top }) },
            valueRange = 0f..top.toFloat(),
            steps = (top / step - 1).coerceAtLeast(0),
            colors = colors,
            modifier = Modifier.semantics { text = AnnotatedString(label); stateDescription = shown },
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = TrailsTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = TrailsTheme.typography.bodyLarge, color = colors.textPrimary)
        Switch(checked = checked, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = colors.dark, checkedThumbColor = colors.surface))
    }
}

private fun Modifier.toggleableRow(checked: Boolean, onClick: () -> Unit): Modifier =
    toggleable(value = checked, role = Role.Checkbox, onValueChange = { onClick() })
```

Required imports (add to the existing list): `androidx.compose.foundation.shape.RoundedCornerShape`, `androidx.compose.ui.Alignment`, `androidx.compose.ui.layout.onGloballyPositioned`, `androidx.compose.ui.layout.positionInParent`, `androidx.compose.ui.semantics.Role`, `org.mobilenativefoundation.trails.feat.savetrail.displayName`, `org.mobilenativefoundation.trails.feat.savetrail.markerKind`, `org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons`. `SliderColors`, `Switch` and `SwitchDefaults` come from the existing `androidx.compose.material3.*` import; `mutableStateMapOf` from `androidx.compose.runtime.*`.

- [x] **Step 7: Run the sheet test and compile Explore**

```bash
./gradlew :multiplatform:feat:filters:impl:jvmTest --tests "*RealFiltersFeatureTest*" :multiplatform:screen:explore:impl:compileKotlinJvm
```

Expected: 1 test passes; Explore compiles with the named `onDismiss` argument.

- [x] **Step 8: Commit**

```bash
git add multiplatform/feat/filters multiplatform/foundation/designsystem multiplatform/screen/explore
git commit -m "feat(filters): rebuild the sheet for design revision R2

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 8: R2 Explore header, filter chips and sort

**Files:**
- Modify: `multiplatform/screen/explore/api/build.gradle.kts` (depend on `feat/filters/api`)
- Modify: `multiplatform/screen/explore/api/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/explore/ExploreState.kt`
- Modify: `multiplatform/screen/explore/impl/build.gradle.kts` (test harness)
- Modify: `multiplatform/screen/explore/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/explore/ExplorePresenter.kt`
- Modify: `multiplatform/screen/explore/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/explore/ExploreUi.kt`
- Modify: `…/designsystem/component/TrailsM1.kt` (`TrailsM1SearchField` becomes the soft pill)
- Modify: `multiplatform/data/trail/api/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/TrailData.kt` (Nearby constants removed)
- Modify: `multiplatform/data/trail/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/data/trail/WorldTrailsTest.kt`, `multiplatform/screen/explore/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/screen/explore/ExploreQueryRestoreTest.kt` (Nearby tests replaced)
- Test: `multiplatform/screen/explore/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/screen/explore/ExploreUiTest.kt`

**Interfaces:**
- Produces: `ExploreIntent.Filters(val section: FilterSection = FilterSection.ALL, val onDismiss: () -> Unit)`, `ExploreIntent.Sort(val sort: TrailSort)`; `fun TrailSort.displayName(): String`. Removes `ExploreIntent.Recommended`, `Nearby` and `ToggleFeature` and the `NEARBY_DEMO_REGION` / `NEARBY_DEMO_REGION_LABEL` constants (For you order is `MOST_POPULAR`; region comes only from search text and a checkpointed region is cleared on restore; features live in Filters).
- Consumes: Task 7 `FilterSection` and `show(query, section, onDismiss)`; Task 6 card.
- The Map pill is omitted (DEV-23).

- [x] **Step 1: Wire the dependencies**

In `multiplatform/screen/explore/api/build.gradle.kts` add `implementation(projects.multiplatform.feat.filters.api)` to the `commonMain` dependencies. In `multiplatform/screen/explore/impl/build.gradle.kts` add:

```kotlin
        jvmTest {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                runtimeOnly(compose.desktop.currentOs)
            }
        }
```

- [x] **Step 2: Write the failing UI test**

```kotlin
package org.mobilenativefoundation.trails.screen.explore

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.feat.filters.FilterSection
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class ExploreUiTest {
    private val trail = Trail("half-dome", "Half Dome", "Yosemite National Park, USA", "Granite summit.", TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486, setOf(TrailFeature.WATERFALL, TrailFeature.SUMMIT), 2)

    @Test
    fun sortMenuSendsTheSelectedOrderAndFilterChipsOpenTheirSection() = runDesktopComposeUiTest {
        val sent = mutableListOf<ExploreIntent>()
        val state = ExploreState(text = "", query = TrailQuery(), results = LoadState(listOf(trail), loading = false), saved = LoadState(loading = false)) { sent += it }
        setContent { TrailsTheme { ExploreUi().Content(state, Modifier) } }
        onNodeWithText("Sort by Most popular").performClick()
        onNodeWithText("Highest rated").performClick()
        onNodeWithText("Length ⌄").performClick()
        assertEquals(ExploreIntent.Sort(TrailSort.HIGHEST_RATED), sent.filterIsInstance<ExploreIntent.Sort>().single())
        assertEquals(FilterSection.LENGTH, sent.filterIsInstance<ExploreIntent.Filters>().single().section)
    }
}
```

- [x] **Step 3: Run and keep the failure**

```bash
./gradlew :multiplatform:screen:explore:impl:jvmTest --tests "*ExploreUiTest*" 2>&1 | tee /private/tmp/r2-task8-red.log
```

Expected: compilation fails with `Unresolved reference 'Sort'`.

- [x] **Step 4: Update the intents**

In `ExploreState.kt` replace the `sealed interface ExploreIntent` with:

```kotlin
sealed interface ExploreIntent : CircuitUiEvent {
    data class ScrollChanged(val position: M1ScrollPosition) : ExploreIntent
    data class QueryChanged(val text: String) : ExploreIntent
    data object SubmitSearch : ExploreIntent
    data object ClearQuery : ExploreIntent
    data class Filters(val section: FilterSection = FilterSection.ALL, val onDismiss: () -> Unit) : ExploreIntent
    data object ClearFilters : ExploreIntent
    data class Sort(val sort: TrailSort) : ExploreIntent
    data class OpenTrail(val trail: Trail) : ExploreIntent
    data class SaveTrail(val trail: Trail, val onDismiss: () -> Unit) : ExploreIntent
    data object Retry : ExploreIntent
    data object OpenSaved : ExploreIntent
}
```

Add `import org.mobilenativefoundation.trails.feat.filters.FilterSection`.

- [x] **Step 5: Update the presenter**

In `ExplorePresenter.present()` replace the `Filters`, `Recommended`, `Nearby` and `ToggleFeature` branches with:

```kotlin
                is ExploreIntent.Filters -> scope.launch {
                    filters.show(query.copy(text = text), intent.section, intent.onDismiss)?.let { setSelectors(it); submittedText = text }
                }
                is ExploreIntent.Sort -> setSelectors(selectors.copy(sort = intent.sort))
```

Replace `restoredExploreSelectors` at the bottom of the file with:

```kotlin
/** Quick area selectors are retired with R2: a checkpointed region is cleared; text is re-entered from the field. */
internal fun restoredExploreSelectors(query: TrailQuery): TrailQuery = query.normalized().copy(text = "", region = null)
```

- [x] **Step 6: Retire the Nearby constants and their tests**

Delete `NEARBY_DEMO_REGION` and `NEARBY_DEMO_REGION_LABEL` from `TrailData.kt`. In `WorldTrailsTest.kt` replace the whole `nearbySelectsTheLargestDemoRegionAndDogTagsRespectUsNationalParks` test with:

```kotlin
    @Test
    fun yosemiteIsTheLargestRegionAndDogTagsRespectUsNationalParks() {
        val yosemite = worldTrails.filter { it.region == "Yosemite National Park, USA" }
        assertEquals(listOf("half-dome", "mist-trail-to-nevada-fall", "upper-yosemite-fall-trail"), yosemite.map { it.id })
        assertEquals(yosemite.size, worldTrails.groupingBy { it.region }.eachCount().values.maxOrNull())
        worldTrails.filter { "National Park, USA" in it.region }.forEach { trail ->
            assertFalse(TrailFeature.DOG_FRIENDLY in trail.features, trail.id)
        }
    }
```

Replace the whole `ExploreQueryRestoreTest.kt` with:

```kotlin
package org.mobilenativefoundation.trails.screen.explore

import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.data.trail.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.TrailFeature
import org.mobilenativefoundation.trails.data.trail.TrailQuery
import org.mobilenativefoundation.trails.data.trail.TrailSort

class ExploreQueryRestoreTest {
    @Test
    fun checkpointedTextAndRegionAreClearedWithoutLosingOtherSelectors() {
        val checkpoint = TrailQuery(
            text = "lake",
            region = "Yosemite National Park, USA",
            difficulties = setOf(TrailDifficulty.MODERATE),
            minMeters = 1000,
            maxMeters = 20000,
            features = setOf(TrailFeature.LAKE),
            sort = TrailSort.SHORTEST,
        )

        assertEquals(checkpoint.normalized().copy(text = "", region = null), restoredExploreSelectors(checkpoint))
    }

    @Test
    fun defaultCheckpointRestoresTheDefaultQuery() {
        assertEquals(TrailQuery(), restoredExploreSelectors(TrailQuery()))
    }
}
```

- [x] **Step 7: Make the search field the R2 soft pill**

In `TrailsM1.kt`, inside `TrailsM1SearchField`, replace the `shape` line and the `modifier = modifier…` chain with:

```kotlin
    val shape = RoundedCornerShape(TrailsTheme.radii.pill)
    …
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(colors.soft, shape)
            .border(if (focused) 2.dp else 0.dp, if (focused) colors.accent else Color.Transparent, shape)
            .semantics { contentDescription = "Search trails" },
```

The rest of the field (icon, clear action, IME Search) is unchanged.

- [x] **Step 8: Rewrite the Explore UI**

Replace the whole `ExploreUi.kt` with:

```kotlin
package org.mobilenativefoundation.trails.screen.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.data.trail.TrailQuery
import org.mobilenativefoundation.trails.data.trail.TrailSort
import org.mobilenativefoundation.trails.feat.filters.FilterSection
import org.mobilenativefoundation.trails.feat.savetrail.*
import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

fun TrailSort.displayName(): String = when (this) {
    TrailSort.MOST_POPULAR -> "Most popular"
    TrailSort.HIGHEST_RATED -> "Highest rated"
    TrailSort.SHORTEST -> "Shortest"
    TrailSort.LONGEST -> "Longest"
}

@Inject
class ExploreUi : Ui<ExploreState> {
    @Composable
    override fun Content(state: ExploreState, modifier: Modifier) {
        val colors = TrailsTheme.colors
        val typography = TrailsTheme.typography
        val keyboard = LocalSoftwareKeyboardController.current
        val scroll = rememberCheckpointedListState(state.initialScroll.index, state.initialScroll.offset, state.results.data != null) { index, offset ->
            state.send(ExploreIntent.ScrollChanged(M1ScrollPosition(index, offset)))
        }
        val queryIdentity = state.query.toString()
        var displayedQuery by rememberSaveable { mutableStateOf(queryIdentity) }
        LaunchedEffect(queryIdentity) {
            if (displayedQuery != queryIdentity) { scroll.scrollToItem(0); displayedQuery = queryIdentity }
        }
        LazyColumn(
            modifier.fillMaxSize().background(colors.background).semantics { paneTitle = "Explore" }, state = scroll,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "search") {
                TrailsM1SearchField(state.text, { state.send(ExploreIntent.QueryChanged(it)) }, {
                    state.send(ExploreIntent.SubmitSearch); keyboard?.hide()
                }, modifier = Modifier.fillMaxWidth())
            }
            item(key = "filter-chips") {
                val filtersFocus = rememberTrailsFocusReturnTarget()
                fun open(section: FilterSection) { keyboard?.hide(); state.send(ExploreIntent.Filters(section, filtersFocus::restore)) }
                Column {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrailsM1Chip(
                            "All", selected = false, onClick = { open(FilterSection.ALL) },
                            modifier = filtersFocus.modifier.semantics { contentDescription = "Filters" },
                            leadingIcon = { Icon(Icons.Outlined.FilterHorizontal.painter, contentDescription = null, Modifier.size(16.dp), tint = colors.textPrimary) },
                        )
                        TrailsM1Chip("Difficulty ⌄", state.query.difficulties.isNotEmpty(), { open(FilterSection.DIFFICULTY) })
                        TrailsM1Chip("Length ⌄", state.query.minMeters > 0 || state.query.maxMeters != null, { open(FilterSection.LENGTH) })
                        TrailsM1Chip("Elevation gain ⌄", state.query.minElevationGain > 0 || state.query.maxElevationGain != null, { open(FilterSection.ELEVATION) })
                    }
                    appliedSummary(state.query)?.let {
                        Text(it, style = typography.labelSmall, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            val trails = state.results.data
            if (state.results.loading) item(key = "loading") { M1Loading(if (trails == null) "Finding trails…" else "Refreshing trails…") }
            if (state.results.error != null) item(key = "error") {
                TrailsM1Notice(
                    if (trails != null) "Couldn’t refresh · Showing saved trails"
                    else if (state.results.offline) "Offline · Saved trails are still here"
                    else "Couldn’t load trails",
                    isError = true, actionLabel = if (state.results.loading) null else "Try again",
                    onAction = { state.send(ExploreIntent.Retry) },
                )
                if (trails == null) TrailsButton("Open saved trails", { state.send(ExploreIntent.OpenSaved) }, tone = ButtonTone.Secondary, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            }
            if (trails != null) {
                if (trails.isEmpty()) item(key = "empty") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        M1Heading("No trails match yet")
                        Text("Try a wider length or fewer filters.", style = typography.bodyLarge, color = colors.textSecondary)
                        TrailsButton("Clear filters", { state.send(ExploreIntent.ClearFilters) }, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    item(key = "count") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${trails.size} ${if (trails.size == 1) "trail" else "trails"}", style = typography.labelLarge, color = colors.textPrimary)
                            SortMenu(state.query.sort) { state.send(ExploreIntent.Sort(it)) }
                        }
                    }
                    items(trails, key = { it.id }) { trail ->
                        TrailSaveCard(trail, state.saved.data, { state.send(ExploreIntent.OpenTrail(trail)) }, { onDismiss -> state.send(ExploreIntent.SaveTrail(trail, onDismiss)) })
                    }
                }
            }
        }
    }
}

/** Applied selectors stay visible beside the chips; the chips themselves open the sheet. */
internal fun appliedSummary(query: TrailQuery): String? {
    val parts = buildList {
        if (query.difficulties.isNotEmpty()) add(query.difficulties.joinToString(", ") { it.displayName() })
        if (query.minMeters > 0 || query.maxMeters != null) add("${query.minMeters / 1000}–${query.maxMeters?.div(1000)?.toString() ?: "50+"} km")
        if (query.minElevationGain > 0 || query.maxElevationGain != null) add("${query.minElevationGain}–${query.maxElevationGain?.toString() ?: "2,000+"} m gain")
        if (query.dogFriendly) add("Dog-friendly")
        if (query.activities.isNotEmpty()) add(query.activities.joinToString(", ") { it.name.lowercase().replace('_', ' ').replaceFirstChar { c -> c.uppercase() } })
        if (query.features.isNotEmpty()) add(query.features.joinToString(", ") { it.name.lowercase().replace('_', ' ').replaceFirstChar { c -> c.uppercase() } })
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun SortMenu(selected: TrailSort, onSelect: (TrailSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, modifier = Modifier.semantics { text = AnnotatedString("Sort by ${selected.displayName()}") }) {
            Text("${selected.displayName()} ⌄", modifier = Modifier.clearAndSetSemantics {}, style = TrailsTheme.typography.labelMedium, color = TrailsTheme.colors.textPrimary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            TrailSort.entries.forEach { sort ->
                DropdownMenuItem(text = { Text(sort.displayName()) }, onClick = { open = false; onSelect(sort) })
            }
        }
    }
}
```

- [x] **Step 9: Run the tests and the navigation recovery tests**

```bash
./gradlew :multiplatform:screen:explore:impl:jvmTest :multiplatform:data:trail:impl:jvmTest :multiplatform:di:graph:active:jvmTest :multiplatform:app:core:jvmTest
```

Expected: `ExploreUiTest` and the replaced `ExploreQueryRestoreTest` pass; the data suite passes with the renamed Yosemite test; `M1NavigationRecoveryTest`, `M1ScrollCheckpointTest` and `DeveloperToolsDrawerHostTest` still pass (checkpoints serialize `TrailQuery`, which only gained defaulted fields).

- [x] **Step 10: Commit**

```bash
git add multiplatform/screen/explore multiplatform/foundation/designsystem multiplatform/data/trail
git commit -m "feat(explore): adopt the R2 header, filter chips and sort control

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 9: R2 trail detail

**Files:**
- Modify: `multiplatform/screen/traildetail/impl/build.gradle.kts` (test harness)
- Modify: `multiplatform/screen/traildetail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/traildetail/TrailDetailUi.kt`
- Test: `multiplatform/screen/traildetail/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/screen/traildetail/TrailDetailUiTest.kt`

**Interfaces:**
- Consumes: Task 3 `TrailsButton`/`TrailsIconCircle`, Task 6 `TrailFactsRow`/`TrailBookmark`/`TrailPhoto(trailId)`, `trailDistance`/`trailDuration`, the existing `TrailPhotoCredit` (attribution block at the end of the sheet content).
- Omitted for M1 (DEV-23/DEV-26): share and more circles, route preview overlays, Preview/Photo tour cards, Customize route, Get directions, Top sights, Download and Map bar buttons. The sticky bar keeps one hero Save button.
- Refresh error and sync notices keep `TrailsM1Notice`/`TrailSyncNotice` until Task 10.

- [x] **Step 1: Add the test harness**

In `multiplatform/screen/traildetail/impl/build.gradle.kts` add the same `jvmTest` dependencies block as in Task 8, Step 1.

- [x] **Step 2: Write the failing UI test**

```kotlin
package org.mobilenativefoundation.trails.screen.traildetail

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class TrailDetailUiTest {
    private val trail = Trail(
        "half-dome", "Half Dome", "Yosemite National Park, USA", "Climb past Yosemite's waterfalls and forest to the granite summit. ".repeat(12),
        TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486, setOf(TrailFeature.WATERFALL, TrailFeature.SUMMIT, TrailFeature.FOREST), 2,
    )

    @Test
    fun factsUseR2LabelsAndShowMoreExpandsTheDescription() = runDesktopComposeUiTest {
        val state = TrailDetailState(trail = LoadState(trail, loading = false), saved = LoadState(loading = false)) {}
        setContent { TrailsTheme { TrailDetailUi().Content(state, Modifier) } }
        onNodeWithText("Elev. gain").assertIsDisplayed()
        onNodeWithText("Route type").assertIsDisplayed()
        onNodeWithText("Out & back").assertIsDisplayed()
        onNodeWithText("Photo source").assertExists()
        onNodeWithText("Show more").performClick()
        onNodeWithText("Show less").assertIsDisplayed()
    }
}
```

- [x] **Step 3: Run and keep the failure**

```bash
./gradlew :multiplatform:screen:traildetail:impl:jvmTest --tests "*TrailDetailUiTest*" 2>&1 | tee /private/tmp/r2-task9-red.log
```

Expected: fails at `onNodeWithText("Elev. gain")` (the current label is `Elevation`).

- [x] **Step 4: Rewrite the detail UI**

Replace the whole `TrailDetailUi.kt` with:

```kotlin
package org.mobilenativefoundation.trails.screen.traildetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.data.trail.Trail
import org.mobilenativefoundation.trails.data.trail.TrailFeature
import org.mobilenativefoundation.trails.feat.savetrail.*
import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@Inject
class TrailDetailUi : Ui<TrailDetailState> {
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content(state: TrailDetailState, modifier: Modifier) {
        val colors = TrailsTheme.colors
        val typography = TrailsTheme.typography
        val trail = state.trail.data
        val snapshot = state.saved.data
        val saveButtonFocus = rememberTrailsFocusReturnTarget()
        val saved = trail?.let { current -> snapshot?.memberships?.get(current.id)?.isNotEmpty() }
        val scroll = rememberCheckpointedScrollState(state.initialScrollOffset, trail != null) { state.send(TrailDetailIntent.ScrollChanged(it)) }
        Column(modifier.fillMaxSize().background(colors.background)) {
            Column(Modifier.weight(1f).verticalScroll(scroll.state).then(scroll.contentModifier)) {
                if (trail == null) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        TrailsIconCircle(Icons.Outlined.ArrowLeft.painter, "Back", { state.send(TrailDetailIntent.Back) })
                        if (state.trail.loading) M1Loading("Opening this trail…")
                        else {
                            M1Heading("Trail unavailable")
                            TrailsM1Notice(if (state.trail.offline) "Not on this device yet" else "Couldn’t open this trail", isError = true)
                            TrailsButton("Try again", { state.send(TrailDetailIntent.Retry) }, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    Box(Modifier.fillMaxWidth()) {
                        TrailPhoto(trail.id, modifier = Modifier.fillMaxWidth().height(360.dp), describeImage = true)
                        TrailsIconCircle(Icons.Outlined.ArrowLeft.painter, "Back", { state.send(TrailDetailIntent.Back) }, Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 12.dp))
                        TrailBookmark(trail.name, saved, { onDismiss -> state.send(TrailDetailIntent.Save(onDismiss)) }, Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = 12.dp))
                        Column(
                            Modifier.fillMaxWidth().padding(top = 336.dp)
                                .clip(RoundedCornerShape(topStart = TrailsTheme.radii.card, topEnd = TrailsTheme.radii.card))
                                .background(colors.surface).padding(horizontal = 20.dp, vertical = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Text(trail.name, style = typography.headlineLarge, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                            TrailFactsRow(trail, trailing = trail.region)
                            if (state.trail.loading) M1Loading("Refreshing trail details…")
                            if (state.trail.error != null) TrailsM1Notice("Couldn’t refresh · Showing this device’s copy", isError = true, actionLabel = if (state.trail.loading) null else "Try again", onAction = { state.send(TrailDetailIntent.Retry) })
                            TrailSyncNotice(snapshot?.syncByTrail?.get(trail.id), snapshot?.offline == true, snapshot?.syncing == true, { state.send(TrailDetailIntent.RetrySync) })
                            TrailFacts(trail)
                            ExpandableDescription(trail.description)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                trail.features.forEach { TrailTag(it.label()) }
                            }
                            trail.reviewExcerpt?.takeIf { it.isNotBlank() }?.let { excerpt ->
                                Text("What hikers are saying", style = typography.titleLarge, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                                Text("“$excerpt”", style = typography.bodyMedium, color = colors.textSecondary)
                                Text("Sample review", style = typography.labelSmall, color = colors.textSecondary)
                            }
                            TrailPhotoCredit(trail.id)
                        }
                    }
                }
            }
            if (trail != null) TrailsButton(
                if (saved == true) "Edit saved collections" else "Save trail",
                { state.send(TrailDetailIntent.Save(saveButtonFocus::restore)) },
                saveButtonFocus.modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                tone = ButtonTone.Hero,
                leadingIcon = Icons.Outlined.Favorite.painter,
                accessibilityLabel = if (saved == true) "Edit saved collections" else "Save trail",
            )
        }
    }
}

@Composable
private fun TrailFacts(trail: Trail) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TrailFact("Length", trailDistance(trail.distanceMeters), Modifier.weight(1f))
        FactDivider()
        TrailFact("Elev. gain", "${trail.elevationMeters} m", Modifier.weight(1f))
        FactDivider()
        TrailFact("Est. time", trailDuration(trail.durationMinutes), Modifier.weight(1f))
        FactDivider()
        TrailFact("Route type", if (TrailFeature.LOOP in trail.features) "Loop" else "Out & back", Modifier.weight(1f))
    }
}

@Composable
private fun FactDivider() {
    Box(Modifier.fillMaxHeight().width(1.dp).background(TrailsTheme.colors.border))
}

@Composable
private fun TrailFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = TrailsTheme.typography.titleLarge, color = TrailsTheme.colors.textPrimary)
        Text(label, style = TrailsTheme.typography.bodySmall, color = TrailsTheme.colors.textSecondary)
    }
}

@Composable
private fun ExpandableDescription(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, style = TrailsTheme.typography.bodyLarge, color = TrailsTheme.colors.textPrimary, maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
        Text(
            if (expanded) "Show less" else "Show more",
            style = TrailsTheme.typography.titleSmall, color = TrailsTheme.colors.textPrimary, textDecoration = TextDecoration.Underline,
            modifier = Modifier.heightIn(min = 48.dp).wrapContentHeight().clickable(role = Role.Button) { expanded = !expanded },
        )
    }
}

@Composable
private fun TrailTag(text: String) {
    val colors = TrailsTheme.colors
    Text(
        text, style = TrailsTheme.typography.labelMedium, color = colors.textPrimary,
        modifier = Modifier.border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

private fun TrailFeature.label(): String = when (this) {
    TrailFeature.LAKE -> "Lakeside"
    TrailFeature.FOREST -> "Forest shade"
    TrailFeature.WATERFALL -> "Waterfall"
    TrailFeature.SUMMIT -> "Views"
    TrailFeature.LOOP -> "Loop"
    TrailFeature.DOG_FRIENDLY -> "Dog-friendly"
}
```

- [x] **Step 5: Run the test to verify it passes**

```bash
./gradlew :multiplatform:screen:traildetail:impl:jvmTest --tests "*TrailDetailUiTest*"
```

Expected: 1 test passes.

- [x] **Step 6: Commit**

```bash
git add multiplatform/screen/traildetail
git commit -m "feat(traildetail): adopt the R2 hero, rating row, facts and expandable description

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 10: Toast, status line and decision layout replace card notices

**Files:**
- Modify: `multiplatform/foundation/designsystem/build.gradle.kts` (coroutines dependency)
- Create: `…/designsystem/component/TrailsStatus.kt`
- Modify: `…/designsystem/component/TrailsButton.kt` (`ButtonTone.Ghost`)
- Modify: `…/designsystem/component/TrailsM1.kt` (delete `TrailsM1Notice` at the end)
- Modify: `multiplatform/feat/savetrail/api/src/commonMain/kotlin/org/mobilenativefoundation/trails/feat/savetrail/SaveTrailFeature.kt`
- Modify: `multiplatform/feat/savetrail/impl/…/TrailSaveCard.kt`, `RealSaveTrailFeature.kt`
- Modify: `multiplatform/feat/filters/impl/…/RealFiltersFeature.kt`
- Modify: `multiplatform/screen/explore/impl/…/ExploreUi.kt`, `multiplatform/screen/traildetail/impl/…/TrailDetailUi.kt`, `multiplatform/screen/saved/impl/…/SavedUi.kt`, `multiplatform/screen/collection/impl/…/CollectionUi.kt`, `multiplatform/screen/welcome/impl/…/WelcomeUi.kt`
- Modify: `multiplatform/app/core/src/commonMain/kotlin/org/mobilenativefoundation/trails/app/MainViewController.kt`, `M1Content.kt`
- Test: `…/designsystem/src/jvmTest/…/component/TrailsStatusTest.kt`, `multiplatform/feat/savetrail/impl/src/jvmTest/…/SyncStatusTest.kt`

**Interfaces:**
- Produces: `enum class StatusKind { PENDING, OFFLINE, FAILED, ATTENTION, INFO }`; `@Composable fun TrailsStatusLine(kind: StatusKind, message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null)`; `data class TrailsToastData(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null)`; `@Composable fun TrailsToast(data: TrailsToastData, modifier: Modifier = Modifier, showCheck: Boolean = true)`; `@Composable fun TrailsToastHost(toast: TrailsToastData?, onDismissed: () -> Unit, modifier: Modifier = Modifier, durationMillis: Long = 5_000)`; `ButtonTone.Ghost`; `data class SyncStatus(val kind: StatusKind, val message: String, val canRetry: Boolean = false)`; `fun syncStatus(sync: TrailSync?, offline: Boolean, detailed: Boolean = false): SyncStatus?`; `SaveTrailFeature.Toast(modifier: Modifier = Modifier)`.
- `TrailSyncNotice` and `SavedSyncNotice` keep their signatures and render status lines. `syncLabel` is removed.
- Consumes: the Task 1 copy table.

- [x] **Step 1: Add coroutines to the design system and the Ghost tone**

In `multiplatform/foundation/designsystem/build.gradle.kts` add `implementation(libs.kotlinx.coroutines.core)` to `commonMain`. In `TrailsButton.kt` change the enum to `enum class ButtonTone { Hero, Commit, Secondary, Ghost }` and extend both `when` blocks with `ButtonTone.Ghost -> Color.Transparent` (container) and `ButtonTone.Ghost -> colors.textPrimary` (content).

- [x] **Step 2: Write the failing component tests**

```kotlin
package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class TrailsStatusTest {
    @Test
    fun statusLineShowsMessageAndRunsItsAction() = runDesktopComposeUiTest {
        var retried = false
        setContent { TrailsTheme { TrailsStatusLine(StatusKind.FAILED, "Couldn’t refresh · Showing saved trails", actionLabel = "Try again", onAction = { retried = true }) } }
        onNodeWithText("Couldn’t refresh · Showing saved trails").assertIsDisplayed()
        onNodeWithText("Try again").performClick()
        assertTrue(retried)
    }

    @Test
    fun toastHostClearsItselfAfterFiveSeconds() = runDesktopComposeUiTest {
        mainClock.autoAdvance = false
        var toast by mutableStateOf<TrailsToastData?>(TrailsToastData("Saved to Weekend adventures", "View") {})
        setContent { TrailsTheme { TrailsToastHost(toast, onDismissed = { toast = null }) } }
        mainClock.advanceTimeByFrame()
        onNodeWithText("Saved to Weekend adventures").assertIsDisplayed()
        mainClock.advanceTimeBy(5_100)
        mainClock.advanceTimeByFrame()
        onNodeWithText("Saved to Weekend adventures").assertDoesNotExist()
    }
}
```

- [x] **Step 3: Write the failing copy test**

```kotlin
package org.mobilenativefoundation.trails.feat.savetrail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.data.trail.TrailSync
import org.mobilenativefoundation.trails.data.trail.TrailSyncStatus
import org.mobilenativefoundation.trails.foundation.designsystem.component.StatusKind

class SyncStatusTest {
    @Test
    fun pendingCopyFollowsPlacementAndOffline() {
        val pending = TrailSync(TrailSyncStatus.PENDING, pendingCount = 1)
        assertEquals("Waiting to sync", syncStatus(pending, offline = false)!!.message)
        assertEquals("Waiting for a connection", syncStatus(pending, offline = true)!!.message)
        assertEquals("Saved on this device · Waiting to sync", syncStatus(pending, offline = false, detailed = true)!!.message)
        assertEquals("Saved here · Waiting for a connection", syncStatus(pending, offline = true, detailed = true)!!.message)
    }

    @Test
    fun retryableFailureIsFailedWithRetry() {
        val status = syncStatus(TrailSync(TrailSyncStatus.PENDING, canRetry = true), offline = false)!!
        assertEquals(StatusKind.FAILED, status.kind)
        assertTrue(status.canRetry)
        assertEquals("Sync needs attention · Local save is kept", status.message)
    }

    @Test
    fun parkedIncompatibleWorkAsksForAnUpdate() {
        val status = syncStatus(TrailSync(TrailSyncStatus.PARKED, reason = "Payload version 3 needs a newer app"), offline = false)!!
        assertEquals(StatusKind.ATTENTION, status.kind)
        assertEquals("App update needed to sync · Saved here", status.message)
    }

    @Test
    fun syncedHasNoLine() = assertEquals(null, syncStatus(TrailSync(TrailSyncStatus.SYNCED), offline = false))
}
```

- [x] **Step 4: Run both and keep the failures**

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest --tests "*TrailsStatusTest*" :multiplatform:feat:savetrail:impl:jvmTest --tests "*SyncStatusTest*" 2>&1 | tee /private/tmp/r2-task10-red.log
```

Expected: compilation fails with `Unresolved reference 'StatusKind'`.

- [x] **Step 5: Create `TrailsStatus.kt`**

```kotlin
package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** Mirrors the Figma set `Trails / Status indicator`. */
enum class StatusKind { PENDING, OFFLINE, FAILED, ATTENTION, INFO }

@Composable
private fun StatusIndicator(kind: StatusKind) {
    val colors = TrailsTheme.colors
    when (kind) {
        StatusKind.OFFLINE -> Box(Modifier.size(6.dp).clip(CircleShape).background(colors.textSecondary))
        StatusKind.PENDING -> Icon(Icons.Outlined.Sync.painter, contentDescription = null, Modifier.size(14.dp), tint = colors.textSecondary)
        StatusKind.FAILED -> Icon(Icons.Outlined.Alert.painter, contentDescription = null, Modifier.size(14.dp), tint = colors.danger)
        StatusKind.ATTENTION -> Icon(Icons.Outlined.Alert.painter, contentDescription = null, Modifier.size(14.dp), tint = colors.warning)
        StatusKind.INFO -> Icon(Icons.Outlined.Circle.painter, contentDescription = null, Modifier.size(14.dp), tint = colors.textSecondary)
    }
}

/** One sentence beside the content it describes. Failed and Attention use foreground text; the action is a separate button node. */
@Composable
fun TrailsStatusLine(
    kind: StatusKind,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val emphasis = kind == StatusKind.FAILED || kind == StatusKind.ATTENTION
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f, fill = false).semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusIndicator(kind)
            Text(message, style = typography.bodySmall, color = if (emphasis) colors.textPrimary else colors.textSecondary)
        }
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = typography.titleSmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = colors.textPrimary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.heightIn(min = 48.dp).wrapContentHeight().clickable(role = Role.Button, onClick = onAction),
            )
        }
    }
}

data class TrailsToastData(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null)

/** Dark one-line completion toast; announced once as a polite live region. */
@Composable
fun TrailsToast(data: TrailsToastData, modifier: Modifier = Modifier, showCheck: Boolean = true) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).shadow(8.dp, shape).clip(shape).background(colors.dark)
            .padding(horizontal = 16.dp, vertical = 12.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCheck) Icon(Icons.Outlined.Tick.painter, contentDescription = null, Modifier.size(16.dp), tint = colors.citron)
        Text(data.message, style = typography.titleSmall, color = colors.onDark, modifier = Modifier.weight(1f))
        if (data.actionLabel != null && data.onAction != null) {
            Text(
                data.actionLabel, style = typography.titleSmall, color = colors.citron,
                modifier = Modifier.heightIn(min = 48.dp).wrapContentHeight().clickable(role = Role.Button, onClick = data.onAction),
            )
        }
    }
}

/** Hosts one toast at the bottom of its container and clears it after [durationMillis]. Place it inside the scaffold content so it sits above the navigation. */
@Composable
fun TrailsToastHost(toast: TrailsToastData?, onDismissed: () -> Unit, modifier: Modifier = Modifier, durationMillis: Long = 5_000) {
    LaunchedEffect(toast) { if (toast != null) { delay(durationMillis); onDismissed() } }
    if (toast != null) Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        TrailsToast(toast, Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp))
    }
}
```

- [x] **Step 6: Replace the sync helpers in `TrailSaveCard.kt`**

Delete `syncLabel`, `TrailSyncNotice` and `SavedSyncNotice` and add:

```kotlin
data class SyncStatus(val kind: StatusKind, val message: String, val canRetry: Boolean = false)

/** R2 copy for one trail's sync state. [detailed] adds the local-save clause used on detail and in the sheet. */
fun syncStatus(sync: TrailSync?, offline: Boolean, detailed: Boolean = false): SyncStatus? = when (sync?.status) {
    null, TrailSyncStatus.SYNCED -> null
    TrailSyncStatus.PENDING -> when {
        sync.canRetry -> SyncStatus(StatusKind.FAILED, "Sync needs attention · Local save is kept", canRetry = true)
        detailed && offline -> SyncStatus(StatusKind.PENDING, "Saved here · Waiting for a connection")
        detailed -> SyncStatus(StatusKind.PENDING, "Saved on this device · Waiting to sync")
        offline -> SyncStatus(StatusKind.PENDING, "Waiting for a connection")
        else -> SyncStatus(StatusKind.PENDING, "Waiting to sync")
    }
    TrailSyncStatus.SYNCING -> SyncStatus(StatusKind.PENDING, "Syncing…")
    TrailSyncStatus.FINISHING -> SyncStatus(StatusKind.PENDING, "Finishing your save")
    TrailSyncStatus.PARKED -> {
        val reason = sync.reason.orEmpty()
        val incompatible = reason.contains("version", ignoreCase = true) || reason.contains("newer app", ignoreCase = true) || reason.contains("update", ignoreCase = true)
        SyncStatus(StatusKind.ATTENTION, if (incompatible) "App update needed to sync · Saved here" else "${reason.ifBlank { "Sync is paused" }.take(30).trimEnd('.')} · Saved here")
    }
}

@Composable
fun TrailSyncNotice(sync: TrailSync?, offline: Boolean, syncing: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier, detailed: Boolean = true) {
    val status = syncStatus(sync, offline, detailed) ?: return
    val retryable = status.canRetry && !syncing
    TrailsStatusLine(status.kind, status.message, modifier, actionLabel = if (retryable) "Try again" else null, onAction = if (retryable) onRetry else null)
}

@Composable
fun SavedSyncNotice(snapshot: SavedSnapshot, onRetry: () -> Unit) {
    val relevant = snapshot.syncByTrail.values.filter { it.status != TrailSyncStatus.SYNCED }
    if (relevant.isEmpty()) return
    val attention = relevant.firstOrNull { it.status == TrailSyncStatus.PARKED || it.canRetry }
    val finishing = relevant.firstOrNull { it.status == TrailSyncStatus.FINISHING }
    when {
        attention != null -> TrailSyncNotice(attention, snapshot.offline, snapshot.syncing, onRetry, detailed = false)
        finishing != null -> TrailSyncNotice(finishing, snapshot.offline, snapshot.syncing, onRetry, detailed = false)
        snapshot.syncing -> TrailsStatusLine(StatusKind.PENDING, "Syncing your saved trails…")
        else -> TrailsStatusLine(
            if (snapshot.offline) StatusKind.OFFLINE else StatusKind.PENDING,
            "${relevant.size} ${if (relevant.size == 1) "change" else "changes"} waiting to sync",
        )
    }
}
```

In `TrailSaveCard` replace the `syncLabel(...)?.let { Text(...) }` line with:

```kotlin
            syncStatus(sync, snapshot?.offline == true)?.let { TrailsStatusLine(it.kind, it.message) }
```

- [x] **Step 7: Move the completion into a toast in the save sheet**

In `SaveTrailFeature.kt` add to the interface:

```kotlin
    /** Hosts the completion toast; the app places it inside the scaffold content so it sits above the navigation. */
    @Composable fun Toast(modifier: Modifier = Modifier)
```

with `import androidx.compose.ui.Modifier`.

In `RealSaveTrailFeature.kt`:

a. Add the field and the host:

```kotlin
    private var toast by mutableStateOf<TrailsToastData?>(null)

    @Composable
    override fun Toast(modifier: Modifier) { TrailsToastHost(toast, onDismissed = { toast = null }, modifier) }
```

b. Inside `Content`, after `val projection by repository.state.collectAsState()`, add:

```kotlin
            val completion = if (state.phase == SavePhase.JOURNALED) {
                val names = state.collections.filter { it.id in state.selected }.map { it.name }
                when { state.selected.isEmpty() -> "Removed from saved"; names.size == 1 -> "Saved to ${names.single()}"; else -> "Saved to ${names.size} lists" }
            } else null
            val viewTarget = state.selected.singleOrNull()
            LaunchedEffect(completion) {
                if (completion != null) {
                    toast = TrailsToastData(completion, actionLabel = if (state.selected.isEmpty()) null else "View") { toast = null; onViewSaved(viewTarget) }
                    dismiss(current)
                }
            }
```

c. Replace the heading call with `M1Heading(if (state.phase == SavePhase.CONFIRM_REMOVE) "Remove saved trail?" else "Save to a list")`.

d. Replace the phase branches:

```kotlin
                        SavePhase.LOADING -> M1Loading("Opening your collections…")
                        SavePhase.UNAVAILABLE -> {
                            TrailsStatusLine(StatusKind.FAILED, "Couldn’t open your collections")
                            TrailsButton("Try again", { send(SaveFlowIntent.LoadChoices) }, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())
                        }
                        SavePhase.CONFIRM_REMOVE -> {
                            Text("This removes the trail from every collection.", style = typography.bodyMedium, color = colors.textSecondary)
                            TrailsButton("Remove from saved", { send(SaveFlowIntent.Submit(Uuid.random().toString(), confirmedRemoval = true)) }, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())
                            TrailsButton("Keep editing", { send(SaveFlowIntent.KeepEditing) }, tone = ButtonTone.Ghost, modifier = Modifier.fillMaxWidth())
                        }
                        SavePhase.JOURNALED -> Unit
```

and inside the `else` branch's inner `when (state.phase)`:

```kotlin
                                SavePhase.ADMITTING -> TrailsStatusLine(StatusKind.PENDING, "Saving on this device…")
                                SavePhase.RECONCILING -> TrailsStatusLine(StatusKind.PENDING, "Checking your save…")
                                SavePhase.UNKNOWN -> {
                                    TrailsStatusLine(StatusKind.PENDING, "Checking your save…")
                                    TrailsButton("Check save", { send(SaveFlowIntent.Reconcile) }, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())
                                }
                                else -> {
                                    if (state.phase == SavePhase.FAILED) TrailsStatusLine(StatusKind.FAILED, "Couldn’t save · Your choices are still here")
                                    if (state.collections.isEmpty()) TrailsStatusLine(StatusKind.INFO, "No collections are available for this account")
                                    val label = if (state.phase == SavePhase.FAILED) "Try saving again" else if (state.selected.isEmpty() && state.original.isNotEmpty()) "Remove from saved" else "Save trail"
                                    TrailsButton(
                                        label,
                                        { send(SaveFlowIntent.Submit(Uuid.random().toString())) },
                                        tone = if (label == "Save trail") ButtonTone.Hero else ButtonTone.Commit,
                                        leadingIcon = if (label == "Save trail") Icons.Outlined.Favorite.painter else null,
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = state.selected.isNotEmpty() || state.original.isNotEmpty(),
                                    )
                                }
```

e. Replace the trailing lines with:

```kotlin
                    dispatchError?.let { TrailsStatusLine(StatusKind.FAILED, it) }
                    if (state.canDismiss && state.phase != SavePhase.CONFIRM_REMOVE && state.phase != SavePhase.JOURNALED) TrailsButton("Cancel", { dismiss(current) }, tone = ButtonTone.Secondary, modifier = Modifier.fillMaxWidth())
```

Add `import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons`. The unused `LiveRegionMode`/`liveRegion` imports can be removed.

- [x] **Step 8: Host the toast in the app shell and migrate the remaining notices**

a. In `M1Content.kt` replace the Scaffold content lambda body with:

```kotlin
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                navigation.persistenceError?.let {
                    TrailsStatusLine(StatusKind.FAILED, "Couldn’t save your place", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), actionLabel = "Retry", onAction = navigation::retryCheckpoint)
                }
                savedState.SaveableStateProvider(navigation.selectedRoot.name) {
                    val platformNavigator = rememberCircuitNavigator(
                        backStack = navigation.backStack,
                        onRootPop = {},
                        enableBackHandler = navigation.backStack.size > 1 && !developerToolsOpen,
                    )
                    NavigableCircuitContent(backStack = navigation.backStack, navigator = platformNavigator, modifier = Modifier.fillMaxSize())
                }
            }
            graph.saves.Toast(Modifier.align(Alignment.BottomCenter))
        }
    }
```

with imports `androidx.compose.ui.Alignment`, `org.mobilenativefoundation.trails.foundation.designsystem.component.StatusKind`, `org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsStatusLine` (replacing the `TrailsM1Notice` import).

b. In `MainViewController.kt` replace `TrailsM1Notice(current.message, isError = true)` with `TrailsStatusLine(StatusKind.FAILED, current.message)` and the `TrailsM1Button("Try again", …)` with `TrailsButton("Try again", bootstrapCoordinator::retry, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())`, updating imports.

c. Apply this mechanical replacement everywhere else (`ExploreUi.kt`, `TrailDetailUi.kt`, `SavedUi.kt`, `CollectionUi.kt`, `RealFiltersFeature.kt`, `WelcomeUi.kt`): every `TrailsM1Notice(text, isError = e, actionLabel = a, onAction = f)` becomes `TrailsStatusLine(if (e) StatusKind.FAILED else StatusKind.INFO, text, actionLabel = a, onAction = f)`; a call without `isError` becomes `StatusKind.INFO`. Then set these specific kinds and strings:

| File | Old text | New call |
| --- | --- | --- |
| `ExploreUi.kt` | error item | `TrailsStatusLine(if (trails != null) StatusKind.FAILED else if (state.results.offline) StatusKind.OFFLINE else StatusKind.FAILED, <same message>, actionLabel = if (state.results.loading) null else if (trails == null && state.results.offline) "Open Saved" else "Try again", onAction = { if (trails == null && state.results.offline) state.send(ExploreIntent.OpenSaved) else state.send(ExploreIntent.Retry) })`; drop the separate "Open saved trails" button |
| `SavedUi.kt` | `We couldn’t open your saved trails.` | `TrailsStatusLine(StatusKind.FAILED, "Couldn’t load your saved trails", actionLabel = "Try again", onAction = { state.send(SavedIntent.Retry) })` |
| `SavedUi.kt` | `Couldn’t refresh your saved trails. Your available trails are still here.` | `"Couldn’t refresh · Your saved trails are still here"`, `StatusKind.FAILED` |
| `SavedUi.kt`, `CollectionUi.kt` | `Some … details aren’t available on this device yet.` | `"Some trail details aren’t on this device yet"`, `StatusKind.INFO` |
| `CollectionUi.kt` | `This collection isn’t available.` | `"This collection isn’t available"`, `StatusKind.FAILED` |
| `CollectionUi.kt` | `Couldn’t refresh this collection. …` | `"Couldn’t refresh · Your trails are still here"`, `StatusKind.FAILED` |
| `TrailDetailUi.kt` | both notices | already use R2 strings; kinds `StatusKind.FAILED` |
| `RealFiltersFeature.kt` | count failure | `TrailsStatusLine(StatusKind.FAILED, "Couldn’t count trails · Filters are kept", actionLabel = "Retry count", onAction = { count = LoadState(); countFor = null; retry++ })` |
| `WelcomeUi.kt` | existing text | same text, `StatusKind.FAILED`, same action |

d. Delete `TrailsM1Notice` from `TrailsM1.kt` together with its now-unused `Surface`, `BorderStroke` and `TextButton` imports.

- [x] **Step 9: Run every affected suite and compile the app**

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest :multiplatform:feat:savetrail:impl:jvmTest :multiplatform:feat:filters:impl:jvmTest :multiplatform:screen:explore:impl:jvmTest :multiplatform:screen:traildetail:impl:jvmTest :multiplatform:app:core:jvmTest :multiplatform:app:core:compileKotlinJvm
```

Expected: all pass, including the seven `SaveTrailAtomTest` cases (the Atom is untouched) and the two new suites. Any remaining `TrailsM1Notice` reference is a compile error to fix in Step 8c.

- [x] **Step 10: Commit**

```bash
git add multiplatform docs
git commit -m "feat(design): replace card notices with the R2 toast, status line and decision layout

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 11: R2 Saved layout with list tiles and placeholders

**Files:**
- Modify: `multiplatform/screen/saved/impl/build.gradle.kts` (test harness)
- Modify: `multiplatform/screen/saved/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/saved/SavedUi.kt`
- Modify: `multiplatform/data/trail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/RealTrailAccount.kt` (seed name)
- Test: `multiplatform/screen/saved/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/screen/saved/SavedUiTest.kt`

**Interfaces:**
- Consumes: `SavedState`/`SavedIntent` (unchanged), Task 6 card and `TrailPhoto(trailId)` (tiles show the photograph alone; attribution stays on detail and Welcome), Task 10 status lines, Task 3 `TrailsButton`.
- Omitted for M1: Downloads and Invites tabs (DEV-24, F24), the quick-action pill, Recommended lists and the sort control (F06 R2 later rows).
- The seeded second collection is named `Favorites` for new installations; existing installations keep `My favorites` because the seed uses `INSERT OR IGNORE` (record in the ledger during Task 13).

- [x] **Step 1: Add the test harness**

In `multiplatform/screen/saved/impl/build.gradle.kts` add the same `jvmTest` dependencies block as in Task 8, Step 1.

- [x] **Step 2: Write the failing UI test**

```kotlin
package org.mobilenativefoundation.trails.screen.saved

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class SavedUiTest {
    private val trail = Trail("half-dome", "Half Dome", "Yosemite National Park, USA", "Granite summit.", TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486, setOf(TrailFeature.WATERFALL, TrailFeature.SUMMIT), 2)

    @Test
    fun tilesShowCountsPlaceholdersAndTheAllTrailsTab() = runDesktopComposeUiTest {
        val sent = mutableListOf<SavedIntent>()
        val snapshot = SavedSnapshot(
            collections = listOf(TrailCollection("weekend", "Weekend adventures"), TrailCollection("favorites", "Favorites")),
            memberships = mapOf(trail.id to setOf("weekend")), trails = listOf(trail), syncByTrail = emptyMap(),
        )
        val state = SavedState(LoadState(snapshot, loading = false), allTrails = false) { sent += it }
        setContent { TrailsTheme { SavedUi().Content(state, Modifier) } }
        onNodeWithText("1 trail").assertIsDisplayed()
        onNodeWithText("0 saved").assertIsDisplayed()
        onNodeWithContentDescription("Empty list").assertIsDisplayed()
        onNodeWithText("All trails").performClick()
        assertEquals(SavedIntent.SelectSegment(true), sent.single())
    }
}
```

- [x] **Step 3: Run and keep the failure**

```bash
./gradlew :multiplatform:screen:saved:impl:jvmTest --tests "*SavedUiTest*" 2>&1 | tee /private/tmp/r2-task11-red.log
```

Expected: fails at `onNodeWithText("0 saved")` (the current tile says `0 trails`).

- [x] **Step 4: Rewrite the Saved UI**

Replace the whole `SavedUi.kt` with:

```kotlin
package org.mobilenativefoundation.trails.screen.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.data.trail.Trail
import org.mobilenativefoundation.trails.data.trail.TrailCollection
import org.mobilenativefoundation.trails.feat.savetrail.*
import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.screen.explore.M1ScrollPosition

@Inject
class SavedUi : Ui<SavedState> {
    @Composable
    override fun Content(state: SavedState, modifier: Modifier) {
        val colors = TrailsTheme.colors
        val typography = TrailsTheme.typography
        val collectionsScroll = rememberCheckpointedListState(state.collectionsScroll.index, state.collectionsScroll.offset, state.content.data != null && !state.allTrails) { index, offset ->
            state.send(SavedIntent.ScrollChanged(false, M1ScrollPosition(index, offset)))
        }
        val trailsScroll = rememberCheckpointedListState(state.trailsScroll.index, state.trailsScroll.offset, state.content.data != null && state.allTrails) { index, offset ->
            state.send(SavedIntent.ScrollChanged(true, M1ScrollPosition(index, offset)))
        }
        val snapshot = state.content.data
        LazyColumn(
            modifier.fillMaxSize().background(colors.background),
            state = if (state.allTrails) trailsScroll else collectionsScroll,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "heading") { Text("Saved", style = typography.displayMedium, color = colors.textPrimary, modifier = Modifier.semantics { heading() }) }
            item(key = "segments") { SavedTabs(state.allTrails) { state.send(SavedIntent.SelectSegment(it)) } }
            if (state.content.loading) item(key = "loading") { M1Loading(if (snapshot == null) "Opening your saved trails…" else "Refreshing saved trails…") }
            if (snapshot == null && !state.content.loading) item(key = "unavailable") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TrailsStatusLine(StatusKind.FAILED, "Couldn’t load your saved trails", actionLabel = "Try again", onAction = { state.send(SavedIntent.Retry) })
                    TrailsButton("Explore trails", { state.send(SavedIntent.Explore) }, tone = ButtonTone.Secondary, modifier = Modifier.fillMaxWidth())
                }
            }
            if (snapshot != null) {
                if (state.content.error != null) item(key = "read-error") {
                    TrailsStatusLine(StatusKind.FAILED, "Couldn’t refresh · Your saved trails are still here", actionLabel = if (state.content.loading) null else "Try again", onAction = { state.send(SavedIntent.Retry) })
                }
                item(key = "sync") { SavedSyncNotice(snapshot) { state.send(SavedIntent.RetrySync) } }
                val savedIds = snapshot.memberships.filterValues { it.isNotEmpty() }.keys
                val trails = snapshot.trails.filter { it.id in savedIds }.distinctBy { it.id }
                if (trails.size < savedIds.size) item(key = "missing-details") {
                    TrailsStatusLine(StatusKind.INFO, "Some trail details aren’t on this device yet", actionLabel = if (state.content.loading) null else "Try again", onAction = { state.send(SavedIntent.Retry) })
                }
                if (state.allTrails) {
                    if (trails.isEmpty()) item(key = "empty") { SavedEmpty { state.send(SavedIntent.Explore) } }
                    items(trails, key = { "trail-${it.id}" }) { trail ->
                        TrailSaveCard(trail, snapshot, { state.send(SavedIntent.OpenTrail(trail)) }, { onDismiss -> state.send(SavedIntent.SaveTrail(trail, onDismiss)) })
                    }
                } else {
                    if (snapshot.collections.isEmpty()) item(key = "empty") { SavedEmpty { state.send(SavedIntent.Explore) } }
                    items(snapshot.collections.chunked(2), key = { row -> "row-${row.first().id}" }) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEach { collection ->
                                val members = snapshot.memberships.filterValues { collection.id in it }.keys
                                CollectionTile(collection, snapshot.trails.firstOrNull { it.id in members }, members.size, Modifier.weight(1f)) {
                                    state.send(SavedIntent.OpenCollection(collection.id))
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedTabs(allTrails: Boolean, onSelect: (Boolean) -> Unit) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        listOf("Lists" to false, "All trails" to true).forEach { (label, value) ->
            val selected = allTrails == value
            Column(
                Modifier.heightIn(min = 48.dp).selectable(selected = selected, role = Role.Tab, onClick = { onSelect(value) }),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(label, style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = if (selected) colors.textPrimary else colors.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(if (selected) colors.textPrimary else colors.border))
            }
        }
    }
}

@Composable
private fun CollectionTile(collection: TrailCollection, cover: Trail?, count: Int, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Column(modifier.clickable(role = Role.Button, onClick = onOpen), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(TrailsTheme.radii.lg)).background(colors.soft), contentAlignment = Alignment.Center) {
            if (cover != null) TrailPhoto(cover.id, modifier = Modifier.fillMaxSize())
            else Icon(Icons.Outlined.Favorite.painter, contentDescription = "Empty list", Modifier.size(28.dp), tint = colors.textPrimary)
        }
        Text(collection.name, style = typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 21.sp), color = colors.textPrimary)
        Text(if (count == 0) "0 saved" else "$count ${if (count == 1) "trail" else "trails"}", style = typography.bodyMedium, color = colors.textSecondary)
    }
}

@Composable
private fun SavedEmpty(onExplore: () -> Unit) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 16.dp)) {
        Text("Keep your next escape", style = typography.titleLarge, color = colors.textPrimary)
        Text("Save a trail that catches your eye. You’ll find it here when you’re ready.", style = typography.bodyLarge, color = colors.textSecondary)
        TrailsButton("Explore trails", onExplore, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())
    }
}
```

- [x] **Step 5: Rename the seeded second collection**

In `RealTrailAccount.kt` change `values.m1Queries.putCollection("favorites", "My favorites", 1)` to `values.m1Queries.putCollection("favorites", "Favorites", 1)`.

- [x] **Step 6: Run the test, the data suite and the app compile**

```bash
./gradlew :multiplatform:screen:saved:impl:jvmTest --tests "*SavedUiTest*" :multiplatform:data:trail:impl:jvmTest :multiplatform:app:core:compileKotlinJvm
```

Expected: all pass.

- [x] **Step 7: Commit**

```bash
git add multiplatform/screen/saved multiplatform/data/trail
git commit -m "feat(saved): adopt the R2 tabs, list tiles and placeholders

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 12: Floating pill navigation

**Files:**
- Create: `…/designsystem/component/TrailsFloatingNav.kt`
- Modify: `multiplatform/app/core/src/commonMain/kotlin/org/mobilenativefoundation/trails/app/M1Content.kt`
- Test: `…/designsystem/src/jvmTest/…/component/TrailsFloatingNavTest.kt`

**Interfaces:**
- Produces: `enum class TrailsDestination(val label: String) { EXPLORE, FOR_YOU, NAVIGATE, SAVED, ACTIVITY }`; `@Composable fun TrailsFloatingNav(items: List<TrailsDestination>, selected: TrailsDestination, onSelect: (TrailsDestination) -> Unit, modifier: Modifier = Modifier)`.
- The M1 preview renders only `EXPLORE` and `SAVED` (DEV-01); M2–M4 add the other destinations when their roots are functional.

- [x] **Step 1: Write the failing test**

```kotlin
package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class TrailsFloatingNavTest {
    @Test
    fun selectingSavedReportsTheDestination() = runDesktopComposeUiTest {
        var selected: TrailsDestination? = null
        setContent {
            TrailsTheme { TrailsFloatingNav(listOf(TrailsDestination.EXPLORE, TrailsDestination.SAVED), TrailsDestination.EXPLORE, onSelect = { selected = it }) }
        }
        onNodeWithText("Explore").assertIsSelected()
        onNodeWithText("Saved").performClick()
        assertEquals(TrailsDestination.SAVED, selected)
    }
}
```

- [x] **Step 2: Run and keep the failure**

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest --tests "*TrailsFloatingNavTest*" 2>&1 | tee /private/tmp/r2-task12-red.log
```

Expected: compilation fails with `Unresolved reference 'TrailsDestination'`.

- [x] **Step 3: Create `TrailsFloatingNav.kt`**

```kotlin
package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** The five R2 roots. Hosts pass only the destinations that are functional in the current milestone. */
enum class TrailsDestination(val label: String) {
    EXPLORE("Explore"), FOR_YOU("For You"), NAVIGATE("Navigate"), SAVED("Saved"), ACTIVITY("Activity"),
}

@Composable
private fun TrailsDestination.icon(): Painter = when (this) {
    TrailsDestination.EXPLORE -> Icons.Outlined.Search.painter
    TrailsDestination.FOR_YOU -> Icons.Outlined.Sparkles.painter
    TrailsDestination.NAVIGATE -> Icons.Outlined.DiscoverCircle.painter
    TrailsDestination.SAVED -> Icons.Outlined.Favorite.painter
    TrailsDestination.ACTIVITY -> Icons.Outlined.ChartIncrease.painter
}

/** White floating pill, 16 dp from the edges, selected item on a soft pill. */
@Composable
fun TrailsFloatingNav(
    items: List<TrailsDestination>,
    selected: TrailsDestination,
    onSelect: (TrailsDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val shape = RoundedCornerShape(TrailsTheme.radii.pill)
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .shadow(12.dp, shape, ambientColor = Color.Black.copy(alpha = 0.14f), spotColor = Color.Black.copy(alpha = 0.14f))
            .clip(shape).background(colors.surface).padding(8.dp).selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val active = item == selected
            Column(
                Modifier.weight(1f).heightIn(min = 56.dp).clip(shape).background(if (active) colors.soft else Color.Transparent)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(item) }).padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(item.icon(), contentDescription = null, Modifier.size(24.dp), tint = colors.textPrimary)
                Text(item.label, style = if (active) typography.labelSmall.copy(fontWeight = FontWeight.SemiBold) else typography.labelSmall, color = colors.textPrimary)
            }
        }
    }
}
```

- [x] **Step 4: Use it in the M1 shell**

In `M1Content.kt` replace the `bottomBar = { NavigationBar(...) { … } }` argument with:

```kotlin
        bottomBar = {
            TrailsFloatingNav(
                items = listOf(TrailsDestination.EXPLORE, TrailsDestination.SAVED),
                selected = if (navigation.selectedRoot == Root.EXPLORE) TrailsDestination.EXPLORE else TrailsDestination.SAVED,
                onSelect = { if (it == TrailsDestination.EXPLORE) navigation.selectExplore() else navigation.selectSavedTab() },
            )
        },
```

Replace the `TrailsCompass`, `Icons` and Material `NavigationBar`/`NavigationBarItem` imports with `org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsDestination` and `TrailsFloatingNav`.

- [x] **Step 5: Run the tests and compile**

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest --tests "*TrailsFloatingNavTest*" :multiplatform:app:core:jvmTest :multiplatform:app:core:compileKotlinJvm
```

Expected: all pass (`M1ScrollCheckpointTest` and `DeveloperToolsDrawerHostTest` do not depend on the Material bar).

- [x] **Step 6: Commit**

```bash
git add multiplatform/foundation/designsystem multiplatform/app/core
git commit -m "feat(app): render the R2 floating pill navigation for the enabled roots

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 13: Verification, installed evidence and ledger

**Files:**
- Create: `docs/evidence/r2/README.md`
- Modify: `docs/trails-reference-deviations.md` (evidence column of DEV-23…DEV-28, plus the `Favorites` seed note)
- Modify: `docs/superpowers/plans/2026-09-14-trails-v25-store6-completion.md` (tick 8g)

**Interfaces:** Consumes every earlier task; produces the R2 acceptance record referenced by the master plan.

- [x] **Step 1: Run the complete JVM gate**

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest :multiplatform:data:trail:impl:jvmTest :multiplatform:feat:savetrail:impl:jvmTest :multiplatform:feat:filters:impl:jvmTest :multiplatform:screen:explore:impl:jvmTest :multiplatform:screen:traildetail:impl:jvmTest :multiplatform:screen:saved:impl:jvmTest :multiplatform:di:graph:active:jvmTest :multiplatform:app:bootstrap:impl:jvmTest :multiplatform:app:core:jvmTest :multiplatform:app:core:compileKotlinJvm 2>&1 | tee /private/tmp/r2-jvm-gate.log
```

Expected: `BUILD SUCCESSFUL`; record the test counts from the XML under each module's `build/test-results/jvmTest`.

- [x] **Step 2: Assemble, install and record the APK identity**

```bash
./gradlew :apps:android:assembleDebug 2>&1 | tee /private/tmp/r2-assemble.log
shasum -a 256 apps/android/build/outputs/apk/debug/android-debug.apk
adb devices
adb -s "$TRAILS_DEVICE" install -r apps/android/build/outputs/apk/debug/android-debug.apk
```

Select `TRAILS_DEVICE` from `adb devices` (the earlier evidence used `emulator-5554`, AVD `Trails_Preview_API_35`). Record the SHA-256 in the README.

- [x] **Step 3: Re-run the affected M1 acceptance cells on the R2 build**

Follow the exact process boundary in `docs/evidence/m1/continuation/README.md` (force-stop, `run-as … tar` capture, `scripts/m1-database-evidence.py`, relaunch). First confirm the catalog reseed on the installed build: the pre-R2 database (seed version 2) must open with 50 trails, Half Dome first, the eight treks graded Strenuous, `backend_meta.seeded = 4` in the captured database and every earlier saved membership intact. Then exercise cells 1 (Explore: search `inca`, the Strenuous chip, sort Shortest, open Classic Inca Trail, return, with the new chips and sort), 3 (Save Half Dome to a list: hero Save, toast `Saved to Weekend adventures · View`, pending status line on card/detail/Saved), 5 (reconnect: status lines clear, one added receipt) and 9 (Back, sheet dismissal, 200% font scale, TalkBack traversal of the status line action, the All chip, the sort control and the floating navigation). Capture numbered PNG/XML files with the helper as before and keep the first failure of anything that breaks.

- [x] **Step 4: Capture paired R2 comparisons**

For frames F01 `148:52`, F03 `148:58`, F09 `148:77`, F10 `148:80`, F14 `159:862`, F06 `148:68`, F21 `236:2165`, export 390 × 844 PNGs from Figma (the `get_screenshot` MCP tool, `maxDimension` 844) and screenshot the installed app in the same state and content. Save both sets under `docs/evidence/r2/screenshots/` as `f01-figma.png` / `f01-android.png` and so on.

- [x] **Step 5: Write the evidence README**

Create `docs/evidence/r2/README.md` with these sections and the actual values gathered above:

```markdown
# Design revision R2 alignment — <date>

Branch, HEAD, list of R2 commits (Tasks 1–12) with hashes.

## JVM gate
Command, outcome, per-module executed test counts (from XML), first-failure logs kept under /private/tmp/r2-task*-red.log.

## Build and install
APK SHA-256, device serial, AVD, Android version, install command.

## Catalog reseed
Seed version before and after, trail count, Strenuous count, preserved memberships (all read from the captured database).

## Acceptance cells rerun on R2
| Cell | Captures | Result and boundary |
| 1 | … | … |
| 3 | … | … |
| 5 | … | … |
| 9 | … | … |
Cells 2, 4, 6, 7 and 8 retain their earlier APK attribution; state which were not rerun.

## Paired comparisons
Table of frame → figma/android capture paths with observed differences.

## Deviations
DEV-23…DEV-28 evidence links; the `My favorites` name on pre-R2 installations.
```

Every value in the README is measured during this task; leave nothing unfilled.

- [x] **Step 6: Update the ledger and the master plan**

In `docs/trails-reference-deviations.md` fill the evidence column of DEV-23…DEV-28 and DEV-30 with links into `docs/evidence/r2/`, and add:

```markdown
| DEV-29 | Seeded collection renamed to Favorites | New installations seed `Favorites`; installations from before R2 keep `My favorites` because the seed is INSERT OR IGNORE | Documented; a rename migration is a later explicit decision |
```

In the master plan tick `8g` and link the README.

- [x] **Step 7: Commit**

```bash
git add docs/evidence/r2 docs/trails-reference-deviations.md docs/superpowers/plans/2026-09-14-trails-v25-store6-completion.md
git commit -m "docs(evidence): record the R2 alignment verification

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

## Self-review

- **Spec coverage:** tokens (Task 2), button rule (Tasks 3, 7–11), difficulty markers and the Strenuous treks (Task 4), structured card facts, download omission and derived highlight (Task 6), extended filters and query (Tasks 5, 7), Explore chips and sort with Nearby retired (Task 8), detail hero/rating/facts/description/tags/attribution (Task 9), toast/status line/decision layout and copy (Task 10), Saved tiles and placeholders (Task 11), floating navigation (Task 12), contract, catalog-spec addendum and evidence updates (Tasks 1, 13). R2 elements deliberately deferred are listed in Global Constraints (DEV-23…DEV-28, DEV-30) and the master plan's later-milestone table.
- **Catalog rebase (September 18):** Tasks 1, 4, 5, 6, 8, 9, 11 and 13 were rewritten after `481346e` (50 world trails, versioned reseed, Nearby bound to Yosemite) and `6435f57` (bundled photography keyed by trail ID). Decisions: the eight treks are Strenuous; activities are derived (Hiking, plus Backpacking for treks); the card highlight is derived from feature tags; Kid-friendly and Highest point are omitted (DEV-30); Nearby is retired; photographer credits stay on detail and Welcome only.
- **Placeholder scan:** the only unfilled content is the evidence README, whose values exist only after execution; every code step is complete.
- **Type consistency:** `ButtonTone` gains `Ghost` in Task 10 after its introduction in Task 3; `TrailFactsRow`, `markerKind()`, `highlight()`, `displayName()` (Task 6) are consumed by Tasks 7–9 with the same signatures; `TrailPhoto(trailId, modifier, describeImage)` (Task 6) is called that way in Tasks 9 and 11; `TrailActivity` has exactly `HIKING` and `BACKPACKING` in Tasks 5, 7 and 8; `SEED_VERSION` is 3 after Task 4 and 4 after Task 5, matching the Task 1 addendum and the Task 13 reseed check; `syncStatus(sync, offline, detailed)` matches its test; `FilterSection` values match between Tasks 7 and 8; `TrailsToastData` is used identically in Tasks 10 and its test.

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-09-18-trails-design-revision-alignment.md`. Two execution options: **Subagent-Driven** (fresh subagent per task with review between tasks, via superpowers:subagent-driven-development) or **Inline** (superpowers:executing-plans with checkpoints). Both require the Prerequisite check to pass first.
