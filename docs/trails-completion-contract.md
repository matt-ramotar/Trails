# Trails design and completion contract

Updated September 18, 2026. This is the product contract for the [implementation plan](superpowers/plans/2026-09-14-trails-v25-store6-completion.md). It records the user's current design direction and the proposed delivery boundaries. The M1 Android implementation and its bounded acceptance evidence are recorded separately below; later Figma milestones remain open.

## Authority and implementation boundary

1. The user's latest decisions take precedence: build the custom Trails design, allow inspiration beyond AllTrails, retain the original compass, and finish both polished UX and real Store6 integration.
2. [Trails v4 · AllTrails redesign in Figma](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=153-97) is the visual reference as revised on September 18, 2026 (design revision R2). The main page is `142:2`; the app section `153:97` holds twenty-eight 390 × 844 frames; messaging components live on the [Components · Messaging](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=248-2777) page. The inventory below records the original twenty frame IDs and the eight R2 additions.
3. [Brand identity](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=171-1133) supplies the visual identity. The selected icon is the **original HeroUI compass**, not either subsequently rejected custom visual. The [icon study](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=203-1684) and [production master](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=204-1685) were verified live: a 1024 × 1024 square, unmasked master, original compass component `172:1138`, and foreground frame `210:1704`. Its compass instance is 576 × 576 at (224, 224).
4. HeroUI is the Figma component/design basis. Implementation remains **Kotlin Multiplatform + Compose + Circuit + Metro + TrailsTheme**. Do not introduce a React app, WebView, or a JavaScript HeroUI dependency to reproduce the design. Map components and tokens into the existing Compose design system.
5. [Mobbin observations](trails-design-inspiration.md), Airbnb/TikTok inspiration, and [Apple design guidance](https://developer.apple.com/design/) inform the design. An exact AllTrails collection census is no longer an implementation prerequisite or a completion criterion.

The shared Atom/Sower plan still controls joint compatibility and Atom adoption ordering. This contract supersedes older exact-AllTrails product-scope language in linked plans/specifications only. It does not change Sower policy, Atom release/publication gates, or transient-versus-durable ownership.

Figma is mutable. Before implementing each milestone, record its node IDs, capture/export date, reference screenshots, relevant component/token values, and asset hashes in `docs/trails-reference-inventory.md`. Retain stable checked-in assets or a reproducible fetch mechanism; expiring image URLs and one developer's Desktop directory are not build inputs. Resolve later design edits against that snapshot explicitly.

## M1 design handoff

The September 15–16 execution completed the bounded design prerequisite: [M1 interaction contract](trails-m1-design-contract.md), [reference inventory](trails-reference-inventory.md), [deviation ledger](trails-reference-deviations.md), and [independent review](evidence/m1/design-review.md). These documents close enabled M1 control/state behavior and preserve local hashed assets. The original 20 frames remain intact; an additive Figma board supplies 16 reliability states. Bounded JVM/Android C3 compatibility passes. Production M1 is implemented, installed and accepted for the bounded Android journey. All nine script cells pass with per-build evidence; the final APK repeats native accessibility and Applied Offline save/restart/reconnect while preserving existing receipts. See [execution evidence](evidence/m1/README.md) for their current status.

## Full scope and staged delivery

The [September 16 continuation](evidence/m1/continuation/README.md) refreshes the live design/dependency snapshots and corrects F03's missing selected-trail sample review excerpt. Its updated installed APK passes the central Offline save/restart/reconnect check with 199 receipt-conservation assertions; 20 affected data tests and the new excerpt's 200% layout also pass. Earlier acceptance remains cumulative with explicit APK attribution. The restored AVD has a new M1 installation identity, and this continuation preserves its first receipt before adding exactly one more on reconnect. It does not relabel the earlier six-receipt run as the current device state.

M1 is the first working journey, not the definition of finished. Maps, Moments, recording, activity, profile, and the remaining controls below remain part of the full Trails experience.

| Milestone | Result | Required data and capability |
| --- | --- | --- |
| M1: Discover and save | Explore → search/filter → trail detail → Save to list → Saved; save offline, restart, reopen, reconnect | Store6 persisted trail/query reads; account-owned saved membership and durable mutation journal; truthful sample entry and restored account |
| M2: Explore places and Moments | Coherent list/map/detail selection and Moments → trail detail; working For you/Following selection | Map provider/attribution and route geometry; stable trail IDs and query identity; seeded, persisted Moments feed and observed actions |
| M3: Record and revisit | Start → recording → pause/resume → finish → save/discard → activity history | Location permission/lifecycle proof; durable recording log; idempotent Store6 completed-activity submission and derived totals |
| M4: Account and complete polish | Profile, preferences, collection management, all navigation roots, recovery states and selected runtime hosts | Persisted account/profile/preferences, sign-out isolation, complete controls, accessibility, performance and clean-checkout verification |

The full navigation roots are **Explore, For You, Navigate, Saved, Activity** in a floating pill bar; Profile opens from the avatar in screen headers. For You carries the former Moments content. Filters and Save to list are reusable features, not separate bottom tabs. Search and larger-text examples are states/layouts; they do not require standalone navigation destinations. Recording owns its active-session navigation behavior.

For the M1 development preview, expose only functional M1 routes. Record temporary omissions of non-M1 tabs, Moments, map, Start trail, and profile actions in the implementation discrepancy ledger; do not leave them enabled with empty handlers or point them at an unrelated ski screen. The final navigation and every in-scope action must be restored and verified by M4. These temporary preview differences do not pass full design acceptance.

| Runtime | Current planning boundary |
| --- | --- |
| Android | Required first installed preview and completion evidence. Reuse the Android host; recheck the available emulator before use. |
| iOS | Full runtime parity remains proposed, not silently approved by an iOS-shaped Figma file. Task 3 records the final platform contract before claiming the sample finished. If selected, add the native host and all corresponding gates. |
| JVM/Desktop and Web | Existing targets are not proof of runnable or durable hosts. Preserve existing compilation where compatible; final runtime scope remains an explicit decision. Do not remove targets silently to make Store6 compile. |

Map rendering and location recording are confirmed design scope. Provider, attribution, real-device behavior and background recording guarantees need their own proof. Offline basemap downloads, custom route editing, subscriptions, purchases, review submission, and a full social/follow-management product are not added just because AllTrails has them. Any such expansion needs an explicit contract revision. Displayed review excerpts and Moments controls still need consistent sample data and working behavior.

## Verified Figma inventory

Node IDs are in file `4B7GK9ndPVQ1BFIKGg0Zqj`. Rows describe design intent, not implemented or tested runtime behavior.

| ID | Frame | Milestone | Required implementation result |
| --- | --- | --- | --- |
| F01 `148:52` | Explore | M1, M2, M4 controls | Search and quick filters drive real fixture queries; stable cards open their own trail; bookmarks share saved state; remaining controls map to later milestones. |
| F02 `148:55` | For You | M2 (preview shipped) | Feature card and "More like" rows come from a seeded per-account fixture; hearts share saved state; the avatar and any personalization wait for M4/M2. |
| F03 `148:58` | Trail detail | M1, M2, M3 | Facts, image, review excerpt and saved state come from the selected ID; View route and Start trail connect to M2/M3. |
| F04 `148:61` | Map | M2 | Real map/route rendering, attribution, selected trail card and return state; no decorative SVG presented as a navigable map. |
| F05 `148:64` | Recording | M3 | Time, distance, elevation, GPS status and durability copy reflect the recorder; Pause/Finish work. |
| F06 `148:68` | Saved | M1, M4 | Collections/All trails and collection rows show real membership and counts; add/manage actions work before being enabled. |
| F07 `148:71` | Activity | M3 (preview shipped) | History and monthly totals derive from seeded sample activities; rows open their trail; recorded history arrives with the M3 recorder. |
| F08 `148:74` | Profile | M4 | Identity and counts use shared account data; Saved, Activity and Trail preferences routes preserve context. |
| F09 `148:77` | Filters | M1 | Draft selections, apply and dismiss have distinct effects; result count is computed; query is retained. |
| F10 `148:80` | Save to list | M1 | Selected collections are a draft until Save; dismissal cancels draft changes; admission failure retains the draft. |
| F11 `159:856` | Recording paused | M3 | Pause stops recording according to the declared sampling/timer contract; Resume continues the same session. |
| F12 `159:858` | Finish hike | M3 | Keep recording/close return to the correct session state; confirm seals one recording. |
| F13 `159:860` | Hike complete | M3 | Save submits one completed activity; discard/close behavior is designed to prevent accidental loss of an unsaved recording. |
| F14 `159:862` | Saved confirmation | M1 | Feedback follows durable local admission; collection counts and bookmarks agree; remote acknowledgement is separate. |
| F15 `160:1096` | Activity saved | M3 | Exactly one history row and one totals update; pending submission is not mislabeled remote success. |
| F16 `197:1158` | Search results | M1 | Query, filters, count and results agree; clear/edit and keyboard/back behavior are defined. |
| F17 `197:1172` | No results | M1 | This is a successful empty query, not a network error; Clear filters preserves the query and reruns it. |
| F18 `197:1186` | Offline | M1 | Keep query/context; Retry and Open saved work. Say saved trails are available only when locally available. |
| F19 `197:1200` | Larger text | M1–M4 | Real system text scaling reflows facts and controls; this static example does not prove Dynamic Type or accessibility support. |
| F20 `197:1260` | Saved empty | M1 | A known empty saved collection offers Explore; distinguish unknown/unavailable account data. |
| F21 `236:2165` | Collection detail | M1 | Title, membership count, R2 trail cards; each row opens its own trail. |
| F22 `236:2181` | All saved trails | M1 | Deduplicated union of memberships as R2 cards. |
| F23 `236:2210` | Downloads (empty) | Deferred | Offline maps are not in the contract; tab omitted until a contract change (DEV-24). |
| F24 `236:2234` | Invites (empty) | M4 decision | Requires a sharing/invite contract before it is enabled. |
| F25 `236:2258` | Navigate | M3 (preview shipped) | Schematic route preview of the current trail; Start recording announces that recording is coming; the recorder is M3. |
| F26 `264:3418` | Trail detail · Route and sights | M2 | Route map overlays, media cards, route actions, Top sights, tags; M1 renders tags only. |
| F27 `264:3966` | Filters · More options | M1 (partial) | Switches and Activity chips are M1; Distance away needs location (M2). |
| F28 `263:3756` | Sight detail | M2 | Map pins and sight sheet; needs route geometry and POI data. |

Every visible control gets an inventory entry with its event, state owner, data operation, navigation target, success/failure behavior, and test/evidence link. Prototype links alone do not prove those semantics. The 28-frame inventory (F01–F28) is complete as a current top-level Figma census, but it is not a complete state machine or a claim that every required detail view has been designed.

### Required state work before the affected milestone

- M1: first load; refresh with retained content; focused search and keyboard dismissal; cleared query; loading and failed result counts; pagination if the final query contract requires it; local-cache miss; database/journal admission failure; pending save; failed/parked save recovery; rapid save/unsave; collection detail/create/edit/delete semantics for enabled controls; account/session restoration and sign-out with pending work.
- M2: map loading/provider error, permission denied/restricted, no location fix, stale location, selection off screen and meaningful empty Moments/Following feed; specify expected outcomes for currently schematic map/Moments actions.
- M3: denied/lost location, permission change, interrupted/recovered recording, sampling gaps, app/background transitions, save failure, duplicate finish/save, discard confirmation, activity detail and no history.
- M4: profile/preferences load and persistence failure; account transition/authorization failure; font scaling, screen-reader traversal, reduced-motion and reduced-transparency alternatives on selected platforms.

These additions follow the approved style and ordinary platform behavior. Do not wait for a matching Mobbin capture. Settle behavior that can lose data, change account ownership, or expand a platform guarantee before implementing the affected operation. Independent milestones continue while a later capability is unresolved.

## Data and ownership contract

- `TrailId` identifies one hiking route across Explore, search, map, Moments, detail, saved membership and activity references. Public facts, media and route geometry are separate from account-owned collections and activity.
- `TrailQuery` is normalized text, area, difficulty, distance and feature selectors, sort, and cursor/bounds only when those actually select results. Persist ordered result membership separately from canonical trail entities. A late earlier query cannot replace current results.
- Saved membership is a set keyed by account, collection and trail. Use a desired-state command such as `SetTrailCollections(accountId, trailId, collectionIds, operationId)` with a versioned payload, stable operation identity and a defined per-trail ordering policy. Do not implement blind toggles or lose a later save/unsave intention during retry.
- One repository projection drives card bookmarks, detail, collection contents and counts. A draft sheet selection is not yet durable; show local-save success only after durable admission. Show pending remote work from the journal/optimistic overlay, independently of stale-read state.
- Store6 owns persisted reads and mutation recovery behind Trails-owned APIs. Account services own startup/reconnect draining, bounded retries, refresh and cancellation. Atom owns the save-flow's transient draft/admission/error transitions; its sequential interpreter must not await remote settlement or collect infinite streams. Initial Atom wiring is manual until the shared candidate/toolchain is proven.
- Session identity must restore before account stores open. The developer Offline setting must be applied before the first fake-service fetch/drain. Late results and drain jobs from a retired account cannot enter a new account.
- Recording samples require a dedicated durable recorder and platform lifecycle owner. Store6 submits a completed activity; its mutation journal is not a high-frequency GPS log.
- A database/cache hit is not downloaded map coverage. Bundled or persisted fixture photography must remain available when the M1 offline demo promises it. Keep raw coordinates/meters/seconds canonical and derive display units and totals consistently.

### Truthful sample data

Seed the 50 real routes in the [world-trail catalog specification](superpowers/specs/2026-09-18-world-trails-seed-design.md), with `Half Dome` (`half-dome`) and `Trolltunga` (`trolltunga`) first, plus collections and one demo account with stable IDs. “For you” uses the documented finite catalog order. Design revision R2 retires the Nearby chip: region is selected only through search text, and `Yosemite National Park, USA` remains the largest seeded region with three trails. It does not imply live GPS. Figma counts such as “24 trails,” “3 trails,” and profile totals are illustrative; compute them from the fixture instead of copying independent constants into screens. Every visible trail row opens its own record.

Route facts are sourced in the catalog specification. Ratings, review counts and review excerpts remain sample data, and the existing three photo tiles remain illustrative. Trek durations use days; sub-day duration labels retain their existing format.

Seed version 2 replaces the eight fictional routes and clears the shared catalog cache before the repository opens. It preserves account saved memberships, mutation journals, backend receipts, client identity and authority counters. A restored Explore area other than the Yosemite demo region is cleared while the other selectors remain. Memberships for retired trail IDs stay stored and produce the existing **Some trail details aren't available on this device yet** notice in Saved or collection detail. Saved's **Try again** only rereads the catalog cache; fetching Explore can restore missing details for current catalog IDs, but cannot restore retired routes. A restored detail route for a retired ID shows **Trail unavailable**. Earlier evidence under `docs/evidence/m1` remains attributed to its recorded build and fixture.

The fake backend is intentionally part of the sample. The baseline legacy services run in process through `NetworkGate`; the M1 trail service now persists configuration, authority and applied-operation receipts in SQLDelight storage. Keep that persistent authority for restart demonstrations. Restarting the client must not reset the authority against which the journal is recovering. Add a documented debug launch/test preset that applies Offline before bootstrap for the separate fresh-install scenario; it cannot depend on settings already saved by a previous installation.

## M1 acceptance script

Run against the newly built Android APK, real Store6 persistence and the fake services. Record commit, dependency manifest, device/OS, fixture ID, logs and screenshots. Repository tests alone do not pass this milestone.

1. Start with the documented demo account online. Open Explore, search/filter, open a known trail, and return. Verify stable ID, query, scroll position and consistent facts.
2. Open the developer drawer, enable **Offline**, and verify the setting reached its applied state. Airplane mode alone is insufficient for an in-process fake backend.
3. Open Save to list, change the selected collections, and save. After durable local admission, verify the detail bookmark, Explore card, Saved list and counts agree, with truthful pending feedback. Dismissing an unsubmitted sheet must leave membership unchanged.
4. Force-stop and relaunch without clearing app data. Verify the same account and Offline setting restore before fetch/drain; cached detail, saved membership and the pending operation survive.
5. Disable Offline and verify it is applied. Observe bounded account-owned draining and one logical backend effect. All projections converge without duplicate membership/counts or lost later intent.
6. Repeat with save/unsave/save, local persistence failure, failed refresh, late search results, duplicate delivery, and failures around server acceptance, durable acknowledgement and local adoption. Preserve evidence from the first failure before fixing it.
7. Switch accounts or sign out during a read and during pending work. Verify isolation, retirement and the documented policy for old-account pending work.
8. Clean-install separately using the documented debug preset that applies fake-service Offline before bootstrap. Show unavailable/retry behavior instead of fabricating an empty result or claiming unavailable saved data. Reconnect and recover through the normal UI. This is a separate reset scenario, not a reset between steps 3–5.
9. Check keyboard/back/sheet dismissal, repeated taps, large text and TalkBack on the installed journey. Compare it with the captured Figma slice and record temporary M1-only omissions separately.

Use file-backed value and journal databases in JVM persistence tests and reopen them in a new process or new driver lifecycle. The existing `JdbcSqliteDriver.IN_MEMORY` factory does not prove restart survival. Android force-stop is a separate required process-death check. Do not clear storage or reset fixtures between steps 3–5.

Example Android process boundary, after verifying the intended emulator serial and installed application ID:

```bash
adb -s "$TRAILS_DEVICE" shell am force-stop org.mobilenativefoundation.trails.android
adb -s "$TRAILS_DEVICE" shell monkey -p org.mobilenativefoundation.trails.android -c android.intent.category.LAUNCHER 1
```

`TRAILS_DEVICE` is selected from `adb devices` during execution; no serial is assumed by this contract. Capture the offline/pending UI and logs on both sides of the restart.

## Design-to-Compose and asset handoff

- Map the R2 tokens into `TrailsTheme`: Forest `#1D4B35` (brand surfaces, selected states, icons; never a button fill), Citron `#A9F184` (hero action), Dark `#0D1F18` (commit action, toast), White `#FFFFFF` (background and surfaces), Ink `#171E14`, Muted `#545A52`, Border `#E6E8E4`, Soft `#F4F5F4`, Clay `#BC624A`, message warning `#8A4B12` and danger `#A5352B`. Difficulty markers: Easy `#43A047` circle, Moderate `#F2B82E` square, Hard `#EE6A45` triangle, Strenuous `#5E3A27` diamond. Reuse the existing theme access API.
- Buttons follow one hierarchy: citron for the single hero action a screen exists for, dark for commits and empty-state primaries, soft for secondary actions, white 48 dp circles for icon actions. Accent forest is never a button fill.
- Feedback uses three patterns: a dark one-line toast above the navigation for completions, a one-line status line beside its subject for pending/offline/failed/attention states, and the sheet for decisions. Card notices with icons in circles, title-plus-description pairs and nested buttons are retired.
- Bundle the selected Manrope/Inter assets and licenses when permitted; use platform font scaling and fallback behavior. Do not depend on runtime font downloads for the offline demo.
- Rebuild the HeroUI button, chip, search field, card, sheet, checkbox and tab treatments as reusable Compose components with semantic state. The native library is design provenance, not an implementation dependency.
- Use the original compass component for the icon and brand mark. Keep its master square/unmasked and preserve SVG alignment. Produce separate Android adaptive/monochrome and selected Apple-platform assets according to verified platform requirements; Figma's rounded appearance studies are not production masks.
- Preserve content/control separation, readable labels, clear selected states, adequate touch targets and sheet dismissal. Validate native platform minimums, contrast, screen-reader semantics and system text scaling. A static larger-text frame or Figma blur is not runtime accessibility or Liquid Glass proof.
- Export permitted imagery/icons to durable project assets with provenance. The existing Hugeicons Desktop directory can supply source material but cannot remain an undeclared clean-checkout dependency.

## Definition of finished

All four milestones and all inventory rows have implemented behavior and evidence. Every visible control is functional; required failure/empty/pending/permission states are usable; figures and statuses reflect actual data. Android and every other selected runtime pass their declared contract. Store6 powers the real trail/account/activity reads and relevant mutations; Store5 is absent from the final runtime dependency graph. A clean checkout resolves the pinned dependencies, builds, launches and reproduces the demo. Material design discrepancies and unsupported capability claims prevent completion.

M1, the Figma prototype, local compilation, public Atom readiness, and a green backend suite each prove only their own scope.
