# Design revision R2 alignment — September 19, 2026

Branch `matt-ramotar/v2.5`, HEAD `41a908d9fd55355926dcefbf8fa179b7e4a2598f` when this verification ran. Every number below was measured during this task on this branch state; nothing is carried over from an earlier run unless the text says so.

| Task | Commit | Subject |
| --- | --- | --- |
| 1 | `27929da` | docs(design): record design revision R2 in the contracts |
| 1 | `88a68b6` | docs(design): reconcile the contracts with the R2 navigation and retired Nearby chip |
| 1 | `498e7c1` | docs(design): retire the Nearby chip throughout the catalog specification and ledger |
| 1 | `cc31a2b` | docs(design): historicize the seed-version-2 restore semantics in the catalog specification |
| 1 | `6724aa7` | docs(design): make the Task 1 verification grep case-insensitive |
| 2 | `a81d44e` | feat(design): adopt design revision R2 tokens and heading sizes |
| 3 | `7821512` | feat(design): add the R2 button hierarchy, icon circle and icons |
| 4 | `32f8cf8` | feat(trail): grade the eight treks Strenuous and add the difficulty marker component |
| 5 | `301b3e1` | feat(trail): extend the query model with R2 selectors, sort and derived activities |
| 6 | `63bbb20` | feat(savetrail): render the R2 trail card with heart, highlight chip and structured facts |
| 7 | `6fab1dd` | feat(filters): rebuild the sheet for design revision R2 |
| 8 | `c30bc0c` | feat(explore): adopt the R2 header, filter chips and sort control |
| 9 | `7ab5f7b` | feat(traildetail): adopt the R2 hero, rating row, facts and expandable description |
| 10 | `2953b31` | feat(design): replace card notices with the R2 toast, status line and decision layout |
| 10 | `73777f3` | fix(design): announce the toast, keep status actions tappable and bound the parked copy |
| 11 | `65034c5` | feat(saved): adopt the R2 tabs, list tiles and placeholders |
| 11 | `f5e8268` | fix(saved): show the empty state only when nothing is saved |
| 12 | `41a908d` | feat(app): render the R2 floating pill navigation for the enabled roots |

## JVM gate

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest :multiplatform:data:trail:impl:jvmTest \
  :multiplatform:feat:savetrail:impl:jvmTest :multiplatform:feat:filters:impl:jvmTest \
  :multiplatform:screen:explore:impl:jvmTest :multiplatform:screen:traildetail:impl:jvmTest \
  :multiplatform:screen:saved:impl:jvmTest :multiplatform:di:graph:active:jvmTest \
  :multiplatform:app:bootstrap:impl:jvmTest :multiplatform:app:core:jvmTest \
  :multiplatform:app:core:compileKotlinJvm 2>&1 | tee /private/tmp/r2-jvm-gate.log
```

**BUILD SUCCESSFUL in 8s**, 432 actionable tasks: 66 executed, 366 up-to-date (log at `/private/tmp/r2-jvm-gate.log`, local and untracked). In that invocation `:multiplatform:data:trail:impl:jvmTest` and `:multiplatform:app:core:jvmTest` were `UP-TO-DATE`, so their XML still came from earlier task runs. To keep every count measured in this task, the same eleven targets were run again with Gradle's per-task `--rerun` after each `jvmTest`: **BUILD SUCCESSFUL in 14s**, 432 actionable tasks: 61 executed, 371 up-to-date (log at `/private/tmp/r2-jvm-gate-rerun.log`). All ten test tasks executed in that second invocation, and the counts below are read from the XML it wrote.

| Module | Suites | Tests | Failures | Errors | Skipped |
| --- | --- | --- | --- | --- | --- |
| `:multiplatform:foundation:designsystem` | 5 | 8 | 0 | 0 | 0 |
| `:multiplatform:data:trail:impl` | 7 | 32 | 0 | 0 | 0 |
| `:multiplatform:feat:savetrail:impl` | 4 | 19 | 0 | 0 | 0 |
| `:multiplatform:feat:filters:impl` | 1 | 1 | 0 | 0 | 0 |
| `:multiplatform:screen:explore:impl` | 2 | 3 | 0 | 0 | 0 |
| `:multiplatform:screen:traildetail:impl` | 1 | 1 | 0 | 0 | 0 |
| `:multiplatform:screen:saved:impl` | 1 | 2 | 0 | 0 | 0 |
| `:multiplatform:di:graph:active` | 1 | 6 | 0 | 0 | 0 |
| `:multiplatform:app:bootstrap:impl` | 1 | 2 | 0 | 0 | 0 |
| `:multiplatform:app:core` | 2 | 14 | 0 | 0 | 0 |
| **Total** | **25** | **88** | **0** | **0** | **0** |

`:multiplatform:app:core:compileKotlinJvm` succeeded in both invocations.

The first failure of each earlier R2 task is preserved outside the repository under `/private/tmp` (untracked, kept as written by those tasks): `r2-task2-red.log` (8,919 B), `r2-task3-red.log` (8,237 B), `r2-task4-red.log` (9,173 B), `r2-task4-marker-red.log` (9,037 B), `r2-task5-red.log` (16,577 B), `r2-task6-red.log` (12,681 B), `r2-task7-red.log` (34,490 B), `r2-task8-red.log` (19,449 B), `r2-task9-red.log` (19,606 B), `r2-task10-red.log` (22,516 B), `r2-task10-fix-red.log` (12,593 B), `r2-task11-red.log` (19,354 B), `r2-task11-fix-red.log` (18,862 B), `r2-task12-red.log` (9,348 B). No Task 1 red log exists because Task 1 was documentation only.

## Build and install

```bash
./gradlew :apps:android:assembleDebug 2>&1 | tee /private/tmp/r2-assemble.log
shasum -a 256 apps/android/build/outputs/apk/debug/android-debug.apk
$HOME/Library/Android/sdk/platform-tools/adb -s emulator-5554 install -r \
  apps/android/build/outputs/apk/debug/android-debug.apk
```

- Assembly: **BUILD SUCCESSFUL in 9s**, 1242 actionable tasks: 130 executed, 1112 up-to-date.
- APK: `apps/android/build/outputs/apk/debug/android-debug.apk`, 88,826,263 bytes — the path in the task brief was the actual output path.
- APK SHA-256: `726b3753f035a6f00143cc37c9c89521afd849fe43af9e3aea74fa22a3f02466`.
- Install: `Performing Streamed Install / Success`. The installed image on the device, `/data/app/~~hh3C8M9rebH8E39i2NL5RA==/org.mobilenativefoundation.trails.android-6P9KvkWM1tLMNlwKNt1vmw==/base.apk`, hashes to the same `726b3753…`, so the exercised binary is byte-identical to the built one.
- Device: serial `emulator-5554`, AVD `Trails_Preview_API_35`, Android 15, API 35, 1280 × 2856 px at 480 dpi (density 3.0, 426.7 × 952 dp).
- `adb` is not on `PATH` in this environment; every command used `$HOME/Library/Android/sdk/platform-tools/adb`. The emulator was already booted and was not wiped; no app data was cleared at any point.

## Catalog reseed

Databases were copied with the M1 process boundary — `am force-stop`, then `run-as … tar -cf -` into `/private/tmp`, then read off-device. `scripts/m1-database-evidence.py` produced the `*-db.json` reports unchanged. That reader reports `backend_meta.seeded` as a boolean and hashes trail identifiers outside the original eight-trail allowlist, so a second read-only reader, [`tools/r2-seed-evidence.py`](tools/r2-seed-evidence.py), recorded the integer seed version, the catalog order, the difficulty grades, the derived activities and the collection names into the `*-seed.json` files beside them.

| | Before R2 (installed build, captured before `install -r`) | After the first launch of the R2 build |
| --- | --- | --- |
| Report | [`database/01-pre-r2-db.json`](database/01-pre-r2-db.json) · [`database/01-pre-r2-seed.json`](database/01-pre-r2-seed.json) | [`database/02-post-reseed-db.json`](database/02-post-reseed-db.json) · [`database/02-post-reseed-seed.json`](database/02-post-reseed-seed.json) |
| Archive SHA-256 | `68cfed5b1d89dfca92b744c5232fed5cfe3f6c9cbad56f79c9eb2cf2988419cd` | `a0dde14fe49d489cec1b242ef7652be49b4e386487ba95d9c3a9aac218132621` |
| `backend_meta.seeded` | **2** | **4** |
| Backend trails | 50 | 50 |
| First trail by position | `half-dome` | `half-dome` |
| Difficulty grades | EASY 5, MODERATE 18, HARD 27, STRENUOUS 0 | EASY 5, MODERATE 16, HARD 21, **STRENUOUS 8** |
| Strenuous trails | none | `classic-inca-trail`, `everest-base-camp-trek`, `tour-du-mont-blanc`, `milford-track`, `torres-del-paine-w-trek`, `kilimanjaro-machame-route`, `laugavegur-trail`, `overland-track` |
| Serialized activities | none (the field was absent from the stored payloads) | HIKING 50, BACKPACKING 8 — the eight treks |
| Saved memberships (account `1bc8e0cf…`) | `alpine-lake-loop` → `weekend`, `half-dome` → `weekend` | unchanged: `alpine-lake-loop` → `weekend`, `half-dome` → `weekend` |
| Backend receipts | 5 | 5 |
| Journal command acceptances | 5 (account `1bc8e0cf…`), 0 (account `1359ac8a…`) | unchanged |
| Collection rows | `weekend` "Weekend adventures", `favorites` "My favorites" (both accounts) | unchanged — see DEV-29 |
| Catalog cache rows | `trail` 50, `query` 25 | `trail` 1 — the reseed invalidated the cached catalog and only the reopened detail had been refetched at capture time |

The installed pre-R2 build was found at seed version 2 with the full 50-trail catalog and two saved memberships, which is exactly the comparison the reseed needed; no state had to be invented. The reseed is also visible in the app: with only the Strenuous filter applied Explore reports **8 trails** ([capture 39](android/39-explore-strenuous-eight.png)), and Half Dome remains graded Hard on the card and the detail.

## Acceptance cells rerun on R2

Process per stage, following `docs/evidence/m1/continuation/README.md` (that M1 folder was read, never modified): `am force-stop` → `run-as … tar` → database reader → `am start`. Captures were taken with [`tools/r2-emulator.py`](tools/r2-emulator.py), a copy of `scripts/m1-emulator.py` that writes into `docs/evidence/r2/android` and stores each screenshot scaled to 844 px tall; it exports the accessibility tree before the screenshot, as the M1 helper does, and records both the captured and the stored PNG digest in [`android/actions.jsonl`](android/actions.jsonl).

| Cell | Captures | Result and boundary |
| --- | --- | --- |
| 1 — Explore, search, filter, sort, detail, return | [02](android/02-explore-after-back.png), [03](android/03-explore-search-inca.png), [04](android/04-filter-sheet.png), [05](android/05-filter-strenuous.png), [06](android/06-filter-elevation-activity.png), [07](android/07-explore-strenuous-applied.png), [08](android/08-sort-menu.png), [09](android/09-explore-sort-shortest.png), [10](android/10-detail-classic-inca.png), [11](android/11-explore-return-state.png), [12](android/12-explore-all-reset.png), [13](android/13-explore-unfiltered.png), [14](android/14-explore-default.png), [39](android/39-explore-strenuous-eight.png) (each with its `.xml`) | Pass. Unfiltered Explore reports 50 trails with the R2 chips (All, Difficulty, Length, Elevation gain) and the sort control; `inca` narrows to 1 trail, Classic Inca Trail, graded **Strenuous**; the Strenuous option in the sheet toggles to `checked=true` and the applied summary line appears; the sort menu offers Most popular / Highest rated / Shortest / Longest and the control then reads **Sort by Shortest**; opening Classic Inca Trail shows the R2 detail, and the system Back returns to Explore with search, filter and sort intact. The All chip opens the whole filter sheet rather than clearing filters; filters were cleared by unticking Strenuous and applying. Nearby is absent from the chip row. |
| 3 — Save Half Dome to a list, toast and pending projections | [15](android/15-save-sheet-half-dome.png), [16](android/16-sheet-dismissed.png), [17](android/17-save-sheet-reopened.png), [18](android/18-removed-toast.png), [19](android/19-removed-toast.png), [20](android/20-after-remove-confirm.png), [21](android/21-developer-drawer.png), [22](android/22-drawer-offline-toggled.png), [23](android/23-drawer-offline-applied.png), [24](android/24-save-sheet-offline.png), [25](android/25-toast-saved.png), [26](android/26-card-pending.png), [27](android/27-saved-pending.png), [28](android/28-collection-weekend.png), [29](android/29-relaunch-pending.png) · [`database/03-online-baseline-db.json`](database/03-online-baseline-db.json) · [`database/04-offline-pending-db.json`](database/04-offline-pending-db.json) | Pass. Half Dome arrived already in Weekend adventures, so the cell was exercised as a real change: online the trail was removed through the R2 decision layout (`Remove saved trail?` · `This removes the trail from every collection.` · `Remove from saved` / `Keep editing`), which settled to 6 receipts; then Offline was applied in the developer drawer and the hero circle save added Weekend adventures again. The toast reads **`Saved to Weekend adventures`** with the **`View`** action, and the pending status line appears on the detail (`Saved here · Waiting for a connection`), on the Explore card and in the collection (`Waiting for a connection`) and on the Saved root and collection header (`1 change waiting to sync`). The journal holds one `UNPREPARED` execution at client sequence 7 with one intent, while the backend still shows 6 receipts and no new requests — the save was durable without reaching the server. |
| 5 — Reconnect and converge | [30](android/30-drawer-online-applied.png), [31](android/31-collection-settled.png), [32](android/32-saved-settled.png), [33](android/33-saved-retry-cleared.png) · [`database/05-reconnected-db.json`](database/05-reconnected-db.json) · [`database/05-reconnected-seed.json`](database/05-reconnected-seed.json) | Pass. Switching Offline off cleared `1 change waiting to sync` from the Saved root and the collection header and `Waiting for a connection` from the card. Receipts went 6 → **7** (exactly one added), applications 6 → 7, push attempts 6 → 7, requests 85 → 87; every pending journal row was reclaimed (intent, execution, attempt, ack, failure and effect counts are all 0) and client sequence 7 is retired and server-confirmed. Half Dome's membership in `weekend` is confirmed both locally and in the backend table. Boundary: `Some trail details aren’t on this device yet` stays on the Saved root and does not clear on `Try again`, because the pre-R2 membership `alpine-lake-loop` is not one of the 50 world trails the R2 catalog seeds; that is a legacy-data condition of this upgraded installation, not a new R2 failure. |
| 9 — Back, sheet dismissal, 200% font scale, accessibility tree | [11](android/11-explore-return-state.png), [16](android/16-sheet-dismissed.png), [17](android/17-save-sheet-reopened.png), [34](android/34-saved-font-200.png), [35](android/35-explore-font-200.png), [36](android/36-detail-font-200.png), [37](android/37-save-sheet-font-200.png), [38](android/38-detail-font-restored.png), [41](android/41-filter-sheet-font-200.png), [42](android/42-filter-actions-font-200.png), [43](android/43-explore-restored-final.png) · [`accessibility.json`](accessibility.json) | Pass with a stated boundary. The system Back returns from detail to Explore with state intact and dismisses the save sheet, and the cancelled draft is discarded (reopening shows Weekend adventures ticked, My favorites unticked). At `font_scale 2.0` the Saved root, Explore, the trail detail, the save sheet and the filter sheet all keep their labels and actions: the difficulty chips wrap to two rows, both length sliders and the elevation slider keep their labelled values, and `Show 50 trails` and `Cancel` stay reachable above the system navigation. `font_scale` was restored to `1.0` and re-verified twice. TalkBack traversal was **not** performed — see the next section. |

Cells **2, 4, 6, 7 and 8 were not rerun as cells**; they retain the APK attribution recorded in the M1 evidence. Two of them were exercised incidentally while running the cells above, and only what was observed is claimed: the developer drawer applied and restored the fake Offline setting (cell 2's mechanism — [21](android/21-developer-drawer.png), [23](android/23-drawer-offline-applied.png), [30](android/30-drawer-online-applied.png), and `mode: OFFLINE` then `ONLINE` in reports 04 and 05), and a force-stop with pending work followed by relaunch preserved the pending change, the account, the settings and the cached detail (cell 4's mechanism — report 04 and [capture 29](android/29-relaunch-pending.png)). Fault injection, rapid duplicate delivery, account switching during reads and a separate fresh Offline installation were not exercised in this task.

## Accessibility and 200% font scale

[`accessibility.json`](accessibility.json) records, for each of 31 elements, the capture it was read from, its role, clickable / checked / selected state, pixel bounds and the size in dp at this device's density 3.0. Every measured control meets 48 dp except the Material slider rows, which are called out below:

| Element | Role | State | Size |
| --- | --- | --- | --- |
| Saved status-line action `Try again` (settled and pending) | Button | clickable | 58.3 × 48.0 dp |
| Toast action `View` | Button | clickable | 48.0 × 48.0 dp |
| Explore `All` chip | View | clickable | 65.3 × 48.0 dp |
| Explore `Difficulty ⌄` chip | View | clickable | 107.3 × 48.0 dp |
| Explore sort control | TextView | clickable | 126.7 × 48.0 dp |
| Floating navigation, current root (`Explore`) | View | `selected=true`, not clickable | 189.3 × 56.0 dp |
| Floating navigation, other root (`Saved`) | View | clickable | 189.3 × 56.0 dp |
| Card heart circle, saved (`Edit saved collections for Half Dome`) | Button | clickable | 48.0 × 48.0 dp |
| Card heart circle, unsaved (`Save Diamond Head Summit Trail`) | Button | clickable | 48.0 × 48.0 dp |
| Detail hero heart circle, saved / unsaved | Button | clickable | 48.0 × 48.0 dp |
| Filter sheet `Strenuous` option | View | `checked` false → true | 112.3 × 48.0 dp |
| Save sheet collection rows | View | `checked` true / false | 378.7 × 52.0 dp |
| Filter sheet `Dog-friendly` switch | View | clickable | 378.7 × 48.0 dp |
| Filter sheet length and elevation sliders | SeekBar | adjustable | **398.7 × 44.0 dp** at both 100% and 200% — the pinned Material slider row height, unchanged by R2 and already carried by DEV-20 / DEV-27 |
| At `font_scale 2.0`: `Try again`, `All`, the sort control, the current navigation root, the card heart circle | — | — | 113.0 × 48.0, 79.7 × 48.0, 215.7 × 48.0, 189.3 × 67.0, 48.0 × 48.0 dp |
| At `font_scale 2.0`: the `Strenuous` option, the `Hiking` chip, `Show 50 trails`, `Cancel` | View | clickable | 171.3 × 48.0, 111.0 × 48.0, 378.7 × 63.0, 378.7 × 63.0 dp |

**TalkBack boundary.** `com.google.android.marvin.talkback` is present on the image, but `accessibility_enabled` is `0` and `enabled_accessibility_services` is `null`, and no accessibility service setting was changed in this task. Spoken traversal order, focus order and announcements were therefore **not** evaluated; the evidence above is read from `uiautomator dump` accessibility trees, which give labels, roles, selected and checked states and bounds, not speech. The earlier TalkBack result in the M1 ledger keeps its own APK attribution.

## Paired comparisons

The Android captures were taken by this task. The Figma side was exported by the controller on September 19, 2026 from the live file with the Figma MCP screenshot export (`get_screenshot`, 390 × 844, scale 1), and the last column was written after viewing each pair. Two frames were mislabelled in the task brief and are corrected here: `148:80` (F10) is the Save to list sheet and `159:862` (F14) is the Saved confirmation toast, so the save-sheet capture pairs with F10, the toast capture (25) pairs with F14, and the filter sheet's lower half pairs with F27 `264:3966` (Filters · More options), exported in addition. All Android screenshots are stored at 844 px tall, while the device renders 426.7 × 952 dp against the 390 × 844 dp frames, so the app shows more vertical content than a frame does and every comparison carries that geometry difference. No pixel diff was run; the differences are observed by eye.

| Frame | Figma | Android | Source capture and what the Android side shows | Observed differences |
| --- | --- | --- | --- | --- |
| F01 `148:52` Explore with results | [`f01-figma.png`](screenshots/f01-figma.png) | [`f01-android.png`](screenshots/f01-android.png) | Capture 14. Search field, the All / Difficulty / Length / Elevation gain chips, `50 trails` with the sort control, cards with highlight pill, structured facts and heart circle, and the floating pill navigation. | Same structure, spacing and type scale; content differs (50 real trails and photographs against the frame's 24-trail illustration set). The card omits the download circle, the mini route map and the photo pager dots, and the screen omits the Map pill (DEV-23, DEV-24). The navigation pill carries two roots instead of five (DEV-01). The saved heart renders filled forest on white where the frame shows the unsaved outline. |
| F03 `148:58` Trail detail, Half Dome | [`f03-figma.png`](screenshots/f03-figma.png) | [`f03-android.png`](screenshots/f03-android.png) | Capture 01. Hero photo with Back and the heart circle, title, rating · difficulty · region row, the four-fact row, expandable description, feature tags and the primary save action. | Hero, sheet overlap, title, facts row and four-fact strip match. The frame's share and more circles, mini map, pager dots, route preview and Download / Map bar are absent (DEV-23, DEV-24, DEV-26). `Show more` sits on its own line rather than inline after the third line, and `Est. time` wraps to two lines for `11 h 0 min`. The frame replaces the navigation with its action bar; the app keeps the floating navigation under the full-width hero button, so the detail shows both. Rating and region are plain text, not underlined links. |
| F09 `148:77` Filter sheet | [`f09-figma.png`](screenshots/f09-figma.png) | [`f09-android.png`](screenshots/f09-android.png) | Capture 44, re-taken after the post-review fixes on the same screen as the retired capture 12: Filters title with a close control, Difficulty (Easy, Moderate, Hard, Strenuous), Length with the two labelled sliders (DEV-20, DEV-27) and the sections below. | Title, close circle and marker chips match; the selected Strenuous chip fills forest. Length uses two labelled single-thumb Material sliders instead of one two-thumb range (DEV-20, DEV-27), Elevation gain uses one slider, and Highest point and Distance away are absent (DEV-25, DEV-30). Each thumb now draws in the ink role and reads against the white sheet, and the dense step ticks are suppressed while the stepped values stay; one Material stop indicator remains at the end of an incomplete track. The frame pins `Show 24 trails` at the bottom; the app's commit button scrolls with the sheet content. |
| F27 `264:3966` Filters · More options | [`f27-figma.png`](screenshots/f27-figma.png) | [`f27-android.png`](screenshots/f27-android.png) | Capture 06. Elevation gain with one maximum slider (DEV-27), the Dog-friendly switch, Activity as text-only Hiking and Backpacking chips (DEV-28, DEV-30), Trail features, and the Show N trails / Cancel actions. | The Dog-friendly switch, Activity chips and Trail features checkboxes follow the frame's order. Distance away and Kid-friendly are absent (DEV-25, DEV-30), Activity offers two chips instead of eight (DEV-30), and the switch shows the native off state where the frame shows on (DEV-28). The app adds a `Cancel` secondary button under the commit button; the frame has none. |
| F10 `148:80` Save to list, Half Dome | [`f10-figma.png`](screenshots/f10-figma.png) | [`f10-android.png`](screenshots/f10-android.png) | Capture 15. `Save to a list`, the trail name, the two collection rows with their ticked state, and the single primary action with Cancel. The second row reads `My favorites` on this upgraded installation (DEV-29). | Title, checkbox rows and the citron `Save trail` match. The app shows the trail name as plain text where the frame has a thumbnail with length and difficulty, the rows carry no `N trails · Created by you` subtitle, there is no close circle and no `Choose a collection` label, and a `Cancel` secondary button is added. |
| F14 `159:862` Saved confirmation toast | [`f14-figma.png`](screenshots/f14-figma.png) | [`f14-android.png`](screenshots/f14-android.png) | Capture 45, re-taken after the post-review fixes. The dark toast `Saved to Weekend adventures` with the citron `View` action after saving Trolltunga from Trail detail. The retired capture 25 was taken offline on Half Dome, so it also showed the detail's status line `Saved here · Waiting for a connection`; this one was taken online, so the detail carries no pending status line. | The toast surface, copy, tick and citron action match the frame. The frame shows the toast on the Saved root above the navigation; the app shows it where the save happened (Trail detail). **The overlap still exists**: the toast still covers the top of the full-width hero button, because the toast host sits above the navigation rather than above the screen's own sticky action. What changed is that the toast is now a Material surface, so a tap on its body is consumed instead of reaching the button underneath it. |
| F06 `148:68` Saved lists | [`f06-figma.png`](screenshots/f06-figma.png) | [`f06-android.png`](screenshots/f06-android.png) | Capture 32. `Saved` heading, the Lists / All trails tabs, the status line with its action, the list tiles with counts and the empty-list placeholder, and the floating navigation. | Heading, tabs, two-up tiles, heart placeholder and `0 saved` copy match. The frame's add, search and avatar header actions, the Downloads and Invites tabs, the `Last updated` sort and the Recommended lists section are absent (DEV-24, F24, F06 later rows). The unselected tab shows a light underline where the frame shows none, and the status line `Some trail details aren’t on this device yet · Try again` has no counterpart in the frame. |
| F21 `236:2165` Weekend adventures collection | [`f21-figma.png`](screenshots/f21-figma.png) | [`f21-android.png`](screenshots/f21-android.png) | Capture 31. Back control, collection title, `2 trails · Your collection`, and the saved trail cards. One of the two memberships is the legacy `alpine-lake-loop`, which the R2 catalog no longer contains, so only one card renders and the missing-details status line remains. | The cards match the R2 card. The frame sets the title inline beside the back arrow in the title style with `3 trails · Created by you`; the app stacks a back arrow over a display-size title with `2 trails · Your collection`. Download circles and mini maps are absent (DEV-23, DEV-24). Only one card renders because of the legacy membership. |

## Capture index

All 43 files below exist under `android/`, each with a matching `.xml` accessibility-tree dump. Three file names (18, 19 and 22) were chosen before the screen was read; they are described accurately here rather than renamed after the fact.

| Files | What the capture shows |
| --- | --- |
| `01-detail-half-dome.png` · `01-detail-half-dome.xml` | First screen after installing and launching the R2 APK: the saved session restored the Half Dome detail, graded Hard, with the R2 hero, rating row, facts row, description and tag row |
| `02-explore-after-back.png` · `02-explore-after-back.xml` | Native Back from the detail returns to Explore: 50 trails, the All/Difficulty/Length/Elevation gain chips, Sort by Most popular, cards and the floating navigation |
| `03-explore-search-inca.png` · `03-explore-search-inca.xml` | Search `inca`: 1 trail, Classic Inca Trail graded Strenuous with the Multi-day highlight |
| `04-filter-sheet.png` · `04-filter-sheet.xml` | Filter sheet opened from the Difficulty chip, Strenuous not selected |
| `05-filter-strenuous.png` · `05-filter-strenuous.xml` | The same sheet after tapping Strenuous; the option's parent node reports checked=true |
| `06-filter-elevation-activity.png` · `06-filter-elevation-activity.xml` | Filter sheet scrolled to Elevation gain, Dog-friendly, Activity (Hiking, Backpacking), Trail features, Show 1 trail and Cancel |
| `07-explore-strenuous-applied.png` · `07-explore-strenuous-applied.xml` | Explore with the applied Strenuous summary line and 1 trail |
| `08-sort-menu.png` · `08-sort-menu.xml` | Sort control open: Most popular, Highest rated, Shortest, Longest |
| `09-explore-sort-shortest.png` · `09-explore-sort-shortest.xml` | Explore after choosing Shortest: the control reads Sort by Shortest |
| `10-detail-classic-inca.png` · `10-detail-classic-inca.xml` | Classic Inca Trail detail: Strenuous, 42.0 km, 2500 m, 4 days |
| `11-explore-return-state.png` · `11-explore-return-state.xml` | Native Back returns to Explore with the search, the Strenuous filter and Sort by Shortest retained |
| `12-explore-all-reset.png` · `12-explore-all-reset.xml` | Filter sheet opened from the All chip (Filters title and Close filters), Strenuous still checked |
| `13-explore-unfiltered.png` · `13-explore-unfiltered.xml` | Explore after clearing the search and the Strenuous filter: 50 trails, still Sort by Shortest |
| `14-explore-default.png` · `14-explore-default.xml` | Explore restored to Sort by Most popular: the unfiltered R2 default state |
| `15-save-sheet-half-dome.png` · `15-save-sheet-half-dome.xml` | Save sheet on Half Dome: Weekend adventures ticked, My favorites unticked, Save trail and Cancel |
| `16-sheet-dismissed.png` · `16-sheet-dismissed.xml` | The Back key dismissed the save sheet after ticking My favorites |
| `17-save-sheet-reopened.png` · `17-save-sheet-reopened.xml` | The sheet reopened: the cancelled draft was discarded (Weekend adventures ticked, My favorites unticked) |
| `18-removed-toast.png` · `18-removed-toast.xml` | The sheet after unticking Weekend adventures; the primary action became Remove from saved (the file name was chosen before the screen was read) |
| `19-removed-toast.png` · `19-removed-toast.xml` | The R2 decision layout: Remove saved trail?, the consequence sentence, Remove from saved and Keep editing (the file name was chosen before the screen was read) |
| `20-after-remove-confirm.png` · `20-after-remove-confirm.xml` | The removal settled online: the hero circle label became Save Half Dome |
| `21-developer-drawer.png` · `21-developer-drawer.xml` | Developer Tools drawer opened with a content swipe, status Applied, Offline mode off |
| `22-drawer-offline-toggled.png` · `22-drawer-offline-toggled.xml` | The drawer after the first tap on the Offline switch, which did not register: the switch is still off |
| `23-drawer-offline-applied.png` · `23-drawer-offline-applied.xml` | The drawer after the second tap: Offline mode on, status Applied |
| `24-save-sheet-offline.png` · `24-save-sheet-offline.xml` | The save sheet reopened on Half Dome while Offline is applied |
| `25-toast-saved.png` · `25-toast-saved.xml` | Toast `Saved to Weekend adventures` with the `View` action, and the detail status line `Saved here · Waiting for a connection` |
| `26-card-pending.png` · `26-card-pending.xml` | The Explore card for Half Dome carries the pending status line `Waiting for a connection` |
| `27-saved-pending.png` · `27-saved-pending.xml` | Saved root while pending: `1 change waiting to sync`, `Some trail details aren’t on this device yet` with Try again, Weekend adventures (2 trails), My favorites (0 saved) and the Empty list placeholder |
| `28-collection-weekend.png` · `28-collection-weekend.xml` | Weekend adventures collection while pending: 2 trails · Your collection, the sync status line and the card status line |
| `29-relaunch-pending.png` · `29-relaunch-pending.xml` | The same collection after force-stop and relaunch: the pending change and its status line survived |
| `30-drawer-online-applied.png` · `30-drawer-online-applied.xml` | The drawer after switching Offline off: status Applied, Offline mode off |
| `31-collection-settled.png` · `31-collection-settled.xml` | Weekend adventures after reconnecting: both sync status lines are gone from the card and the header |
| `32-saved-settled.png` · `32-saved-settled.xml` | Saved root after reconnecting: `1 change waiting to sync` is gone |
| `33-saved-retry-cleared.png` · `33-saved-retry-cleared.xml` | The Saved root after tapping Try again: `Some trail details aren’t on this device yet` remains (see the legacy membership boundary) |
| `34-saved-font-200.png` · `34-saved-font-200.xml` | Saved root at font scale 2.0 |
| `35-explore-font-200.png` · `35-explore-font-200.xml` | Explore at font scale 2.0 |
| `36-detail-font-200.png` · `36-detail-font-200.xml` | Trail detail at font scale 2.0 |
| `37-save-sheet-font-200.png` · `37-save-sheet-font-200.xml` | Save sheet at font scale 2.0 with both actions still reachable |
| `38-detail-font-restored.png` · `38-detail-font-restored.xml` | Trail detail after restoring font scale 1.0; Half Dome is saved again and carries no status line |
| `39-explore-strenuous-eight.png` · `39-explore-strenuous-eight.xml` | Explore with only the Strenuous filter applied: 8 trails |
| `40-explore-restored.png` · `40-explore-restored.xml` | The preview left unfiltered: 50 trails, Sort by Most popular |
| `41-filter-sheet-font-200.png` · `41-filter-sheet-font-200.xml` | Filter sheet at font scale 2.0: the difficulty chips wrap to two rows and both length sliders keep their labelled values |
| `42-filter-actions-font-200.png` · `42-filter-actions-font-200.xml` | The same sheet scrolled at font scale 2.0: elevation slider, Dog-friendly, Activity, Trail features, Show 50 trails and Cancel |
| `43-explore-restored-final.png` · `43-explore-restored-final.xml` | Font scale back at 1.0 and the preview left on unfiltered Explore: 50 trails, Sort by Most popular |

## Deviations

| ID | R2 evidence from this task |
| --- | --- |
| DEV-23 — map-dependent controls omitted | No map pill, download circle, directions, expand control, media cards or Top sights appear in [F01](screenshots/f01-android.png), [F03](screenshots/f03-android.png) or [F21](screenshots/f21-android.png); no dead control was found in any of the 43 captures. |
| DEV-24 — Downloads tab and download circles omitted | The Saved root shows only the Lists and All trails tabs ([F06](screenshots/f06-android.png), [capture 32](android/32-saved-settled.png)); the detail has no download circle ([F03](screenshots/f03-android.png)). |
| DEV-25 — Distance away filter omitted | The filter sheet holds Difficulty, Length, Elevation gain, Dog-friendly, Activity and Trail features and no distance-away control ([F09](screenshots/f09-android.png), [F27](screenshots/f27-android.png), [capture 04](android/04-filter-sheet.png)). |
| DEV-26 — Customize route omitted | The trail detail's only actions are Back, the heart circle, Show more and the save button ([F03](screenshots/f03-android.png), [capture 10](android/10-detail-classic-inca.png)). |
| DEV-27 — two-thumb ranges as native sliders | Length keeps `Minimum length · 0 km` and `Maximum length · No maximum` as two labelled sliders and Elevation gain a single `Maximum elevation gain · No maximum` slider ([capture 04](android/04-filter-sheet.png), [F27](screenshots/f27-android.png)); all three sliders keep their labels and values at 200% in [capture 41](android/41-filter-sheet-font-200.png) and [capture 42](android/42-filter-actions-font-200.png), where the apply and cancel actions also stay reachable; their nodes measure 398.7 × 44.0 dp, and slider adjustment by touch or by TalkBack was not exercised. |
| DEV-28 — activity pictograms and the Figma Switch on state | Activity is rendered as the text-only chips `Hiking` and `Backpacking`, and the Dog-friendly control is a native Switch with real state ([F27](screenshots/f27-android.png)). |
| DEV-29 — seeded collection renamed to Favorites | This installation predates R2, so its `favorites` row keeps the name `My favorites` in both accounts' `collection_row` tables ([`database/05-reconnected-seed.json`](database/05-reconnected-seed.json)) and in the save sheet ([F14](screenshots/f14-android.png)). The seed uses `INSERT OR IGNORE`, so no rename happened on upgrade. |
| DEV-30 — Kid-friendly, Highest point and further activities omitted | The sheet has no Kid-friendly switch and no Highest point range, and Activity offers only Hiking and Backpacking ([F27](screenshots/f27-android.png)); the reseeded catalog serializes exactly those two activities, 50 HIKING and 8 BACKPACKING ([`database/02-post-reseed-seed.json`](database/02-post-reseed-seed.json)). |

## Boundaries and first failures from this task

- **Figma exports were added by the controller.** This task did not run a Figma export; the controller exported the eight frames on September 19, 2026 and wrote the observed-differences column above.
- **TalkBack was not run.** No accessibility service was enabled and no service setting was changed. Labels, roles, selected and checked states and 48 dp bounds come from `uiautomator dump` trees, not from spoken traversal.
- **Capture helper first failure.** The first run of the copied capture helper wrote its files under the session scratchpad instead of the repository, because `ROOT` resolved relative to the copied script. That capture was discarded and the helper was corrected to the repository path before any evidence here was taken; the corrected copy is [`tools/r2-emulator.py`](tools/r2-emulator.py). No app behavior was involved.
- **The Offline switch changed state without a logged second input.** `actions.jsonl` records one tap on the `Offline mode` switch; [capture 22](android/22-drawer-offline-toggled.png), taken 15 s later, shows the switch off, and [capture 23](android/23-drawer-offline-applied.png), 39 s after that, shows it on with status `Applied`. The input that produced the change is not in the log, so no claim is made about a second tap. Both captures are kept.
- **A helper tap failed and is recorded.** `actions.jsonl` contains `No node 'Save trail' at index 0`: unticking Weekend adventures had already relabelled the primary action to `Remove from saved`. The correct control was then tapped. The failure entry is left in the log.
- **Legacy membership without a catalog entry.** `alpine-lake-loop`, saved before R2, is not among the 50 world trails, so `Some trail details aren’t on this device yet` persists on the Saved root and `Try again` does not clear it ([capture 33](android/33-saved-retry-cleared.png)). Recorded, not fixed here.
- **Stored screenshots are downscaled.** Each capture is stored at 844 px tall; `actions.jsonl` records both the digest of the raw 1280 × 2856 capture and the digest of the stored file.
- **Raw archives stay outside the repository.** The five `databases` tars live in `/private/tmp` (`trails-r2-pre-install`, `after-first-launch`, `online-baseline`, `offline-pending`, `reconnected`); their SHA-256 values are inside the reports. No APK and no video is stored in the repository.
- **Not covered.** M2 maps and Moments, M3 recording, M4 profile and the full five-root shell remain open; the acceptance cells not rerun keep their earlier attribution; the eight-trail-era fixtures in `scripts/m1-database-evidence.py` mean that reader hashes world-trail identifiers, which is why `tools/r2-seed-evidence.py` exists beside it.

## Post-review fixes (September 19, 2026)

The final whole-branch review of this alignment found must-fix items; they were fixed in one commit on the same branch, after which the two screens they change were re-captured. Everything above stays as it was measured at `41a908d`, except the F09 and F14 rows, which now describe the re-captured images.

| Commit | Subject |
| --- | --- |
| `7dfc644` | fix(design): make sliders visible, keep sort out of the cache key and block toast pass-through |

What that commit changes: the filter-sheet slider thumbs use the ink role and their dense step ticks are hidden while the stepped values stay; `TrailSort` is no longer part of the catalog's Store cache key, so changing the sort offline reorders the cached membership instead of missing a new key; the completion toast is a Material surface, so its body consumes taps instead of passing them to the content underneath; the parked `App update needed to sync` copy follows a typed `TrailSyncCause` classified from Store6's `MutationFailureKind.CODEC` rather than matching words in a failure message; the JOURNALED completion path gains a desktop Compose UI test; and the dead `errorAccent` role, an inert `repository.state.collectAsState()` statement, the loading spinner's invisible colour, the Welcome status line's unmerged live region and a `Clear filters` that dropped the chosen sort are cleaned up.

### JVM gate for the fix commit

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest :multiplatform:data:trail:impl:jvmTest \
  :multiplatform:feat:savetrail:impl:jvmTest :multiplatform:feat:filters:impl:jvmTest \
  :multiplatform:screen:explore:impl:jvmTest :multiplatform:screen:traildetail:impl:jvmTest \
  :multiplatform:screen:saved:impl:jvmTest :multiplatform:di:graph:active:jvmTest \
  :multiplatform:app:bootstrap:impl:jvmTest :multiplatform:app:core:jvmTest \
  :multiplatform:app:core:compileKotlinJvm 2>&1 | tee /private/tmp/r2-final-fix-gate.log
```

**BUILD SUCCESSFUL in 9s**, 428 actionable tasks: 66 executed, 8 from cache, 354 up-to-date (log at `/private/tmp/r2-final-fix-gate.log`, local and untracked). The ten `jvmTest` result directories were deleted before this invocation so no count below is carried over from an earlier run; six of the tasks were then served from the Gradle build cache, which restores the XML of a run with identical inputs. The log contains no warning line.

| Module | Suites | Tests | Failures | Errors | Skipped |
| --- | --- | --- | --- | --- | --- |
| `:multiplatform:foundation:designsystem` | 5 | 10 | 0 | 0 | 0 |
| `:multiplatform:data:trail:impl` | 8 | 33 | 0 | 0 | 0 |
| `:multiplatform:feat:savetrail:impl` | 5 | 22 | 0 | 0 | 0 |
| `:multiplatform:feat:filters:impl` | 1 | 1 | 0 | 0 | 0 |
| `:multiplatform:screen:explore:impl` | 2 | 4 | 0 | 0 | 0 |
| `:multiplatform:screen:traildetail:impl` | 1 | 1 | 0 | 0 | 0 |
| `:multiplatform:screen:saved:impl` | 1 | 2 | 0 | 0 | 0 |
| `:multiplatform:di:graph:active` | 1 | 6 | 0 | 0 | 0 |
| `:multiplatform:app:bootstrap:impl` | 1 | 2 | 0 | 0 | 0 |
| `:multiplatform:app:core` | 2 | 14 | 0 | 0 | 0 |
| **Total** | **27** | **95** | **0** | **0** | **0** |

`:multiplatform:app:core:compileKotlinJvm` succeeded. The first failure of the sort-cache test is preserved at `/private/tmp/r2-final-fix-red.log` (untracked).

### Build and install for the re-capture

- Assembly: `./gradlew :apps:android:assembleDebug` — **BUILD SUCCESSFUL in 4s**, 1238 actionable tasks: 105 executed, 1133 up-to-date.
- APK: `apps/android/build/outputs/apk/debug/android-debug.apk`, 88,826,007 bytes.
- APK SHA-256: `efefa676e83514b59ce253c42606eec8037126cf18e1a68c8ae95bb22428b16d`.
- Install: `adb -s emulator-5554 install -r …` → `Performing Streamed Install / Success`. The installed image, `/data/app/~~GbFj7DczoGLDX7P5FtHT4Q==/org.mobilenativefoundation.trails.android-dN7soEstdMcAtwVAlQx4Jw==/base.apk`, hashes to the same `efefa676…`.
- The `Trails_Preview_API_35` AVD had shut down between the gate and the capture and was restarted with `emulator -avd Trails_Preview_API_35`. Its `default_boot` snapshot failed to load, so this was a cold boot of the same userdata image; no `-wipe-data` was used and no app data was cleared, and the installation still carried its pre-R2 `My favorites` collection and its earlier saves.

### Screenshots re-captured on that APK

Two files were replaced, both at the same 378 × 844 format as the rest; the other twelve paired images are unchanged.

| File | New source capture | What changed in the image |
| --- | --- | --- |
| [`screenshots/f09-android.png`](screenshots/f09-android.png) | [`android/44-filter-sheet-after-fixes.png`](android/44-filter-sheet-after-fixes.png) · `.xml` | The three slider thumbs are drawn in the ink role and are visible on the white sheet, where the retired image showed none; the dotted step-tick tracks are now plain. |
| [`screenshots/f14-android.png`](screenshots/f14-android.png) | [`android/45-toast-saved-after-fixes.png`](android/45-toast-saved-after-fixes.png) · `.xml` | The same toast copy and citron `View`, taken online on Trolltunga rather than offline on Half Dome. The toast still overlaps the full-width hero button; only pass-through was fixed, and the image shows the overlap plainly. |

The capture index above lists the 43 captures of the original verification; captures 44 and 45 are the two added here, each with its `.xml` tree, and both are recorded in `android/actions.jsonl` like the rest.
