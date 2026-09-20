# Remaining roots preview verification — September 20, 2026

## Authorized Offline Retry follow-up

Production source `036ed843b3c3bb411c9176e958ff13a71514f1da` adds a developer-only **Activity refresh → Try again** action bound to the current Activity presenter and account. A real cached Offline refresh now retains the complete feed without a spurious failure line; genuine online and missing-cache failures keep recovery. The owner authorized this additional bounded fix-and-review round; the original acceptance criterion was not waived.

The installed action is now measured: [Applied Offline and actual action87](android/87-retry-followup-offline-applied.png), the timestamped tap in [actions.jsonl](android/actions.jsonl), [retained content88](android/88-retry-followup-offline-after-retry.png), [complete summary/hero89](android/89-retry-followup-offline-top.png), and [all history rows90](android/90-retry-followup-offline-rows.png), each with paired XML. September totals remain **46.9 km / 5 trails / 22h**, with the expected Half Dome hero and no refresh-failure line. The same developer action with Applied Online/error1 displays the real failure in [92](android/92-retry-followup-online-failure-visible.png); after restoring Online/error0, the visible inline Retry clears it in [94](android/94-retry-followup-online-recovered.png). [Acceptance record](retry-followup-acceptance.json), [exact action notes](logs/retry-followup-device-actions.md).

APK SHA-256 **`e972d12c10c33da12bf74b054122af5273b6120e5e31f2b379a81bc2841c3b83`**, **87,952,275 bytes**. Assembly succeeded in 11s (1,386 tasks: 130 executed, 5 from cache, 1,251 up-to-date); installation with `-r` returned **Success**, and an independent device hash matched. [Build/install identity](retry-followup-build-install.json), [assembly log](logs/retry-followup-assemble.log), [install log](logs/retry-followup-install.log). The Android compile emitted only the two previously recorded drawer warnings (deprecated clipboard API and an unnecessary safe call).

Captures85–95 belong to this APK. [85](android/85-retry-followup-restored.png) restores the pre-check Explore position. [Before10](database/10-before-retry-followup-snapshot.json), [after11](database/11-after-retry-followup-snapshot.json) and the [read-only comparison](database/retry-followup-comparison.json) prove all five existing memberships, fixture payloads and seed4 unchanged, with original backend config restored. [95](android/95-retry-followup-restored-settings.png) and the [settings report](logs/retry-followup-device-settings.json) confirm Explore selected, font1.0, gestures and accessibility0/no service restored. The temporary three-button navigation condition is documented. Isolated ADB5038 was stopped; shared5037 and emulator-5556 stayed untouched. No new device-driving failure occurred in this follow-up.

The Offline error classifier deliberately recognizes only the existing Store6-wrapped fake-backend rejection when its cache is usable. It fails closed if that diagnostic format changes: the failure line reappears. Real-account regressions cover this dependency, prior online errors, uncached recovery and incomplete catalog recovery. Offline rejection occurs before the backend request counter increments; an Offline counter increment is not claimed as proof.

## JVM gate

The latest evidence is **137 passing tests / 40 suites / 13 modules**, zero failures/errors/skips. All thirteen test targets in the prescribed gate ran freshly at `036ed84` in 24s. Compilation remained incremental; app-core compilation was UP-TO-DATE. Review then found new SLF4J no-provider warnings in Activity's captured test stderr, despite a clean compiler console. Test-only correction `a3df23f` adds the matching runtime logger; a fresh Activity gate passed all17 tests in8s with every XML stdout/stderr stream empty. The final counts combine that replacement Activity run with the unaffected twelve modules from the full gate; they are not a new all-module execution at `a3df23f`. Production sources and the installed APK are unchanged by the test dependency.

[Current per-module counts and exact XML paths](jvm/current-counts.json), [full gate log](jvm/retry-followup/full/gradle.log), [full source hashes](jvm/retry-followup/full/source-sha256.json), [logger correction verification](jvm/retry-followup/logger-runtime/verification.json), [fresh Activity log](jvm/retry-followup/logger-runtime/gradle.log). The complete command is the prescribed thirteen-module command retained under Original JVM gate below, with per-task `--rerun` and app-core compilation. The correction command was `./gradlew :multiplatform:screen:activity:impl:jvmTest --rerun`.

| Module under `:multiplatform` | Tests | Suites |
|---|---:|---:|
| `foundation:designsystem` | 14 | 6 |
| `data:trail:impl` | 36 | 10 |
| `feat:savetrail:impl` | 22 | 5 |
| `feat:filters:impl` | 1 | 1 |
| `screen:explore:impl` | 4 | 2 |
| `screen:traildetail:impl` | 1 | 1 |
| `screen:saved:impl` | 2 | 1 |
| `screen:navigate:impl` | 5 | 2 |
| `screen:activity:impl` | 17 | 5 |
| `screen:foryou:impl` | 8 | 2 |
| `di:graph:active` | 8 | 1 |
| `app:bootstrap:impl` | 2 | 1 |
| `app:core` | 17 | 3 |
| **Total** | **137** | **40** |

First-red evidence is retained in [retry-followup/red](jvm/retry-followup/red/) (original `/private/tmp/roots-retry-followup-red.log`). Distinct test-harness failures remain in [lifetime-compile-failure](jvm/retry-followup/lifetime-compile-failure/), [integration-first-failures](jvm/retry-followup/integration-first-failures/) and [cold-ui-followup-failure](jvm/retry-followup/cold-ui-followup-failure/). The retained cold Offline-to-content UI regression exposed forced lazy-list remeasurement during composition under the desktop test scheduler. Deferring restoration with `requestScrollToItem` passes that scenario and the existing nonzero60/9 checkpoint regression; no installed Android crash is claimed. The original SLF4J stderr is preserved in the full-run XML, with clean replacement evidence kept separately. No unchanged failure was rerun to obtain green. Existing injected BackendConfigSync failure logging remains the previously documented baseline diagnostic.

## Earlier final-review fix verification

This earlier wave used production source `57d6b85b21c7f7322525e2dfd7837e7c743a9775`. The consolidated fix restores the shared per-trail sync status on Activity heroes/rows and For You recommendations, and makes Activity's catalog dependency recover through Retry while keeping history. The focused regressions and complete fresh gate are retained in [final-fix JVM evidence](jvm/final-fix/): **128 tests across 13 modules/38 suites, zero failures/errors/skips**, with all thirteen JVM test tasks and app-core compilation freshly executed. The successful gate has no compiler warnings. [Historical per-module counts](jvm/final-fix/current-counts.json), [full gate log](jvm/final-fix/logs/roots-final-fix-jvm-gate.log), [tested-source hashes](jvm/final-fix/tested-source.json), [log hashes](jvm/final-fix/logs/inventory.json). Earlier failed harness, behavioral and test-compilation attempts remain in that log inventory; none was rerun unchanged to obtain green.

That wave’s assembly succeeded in 2s, 1,382 tasks (74 executed, 1,308 up-to-date), with no compiler warnings ([log](logs/final-fix-assemble.log)). APK **88,558,103 bytes**, SHA-256 **`ef6e34d0a1dbaabb3ce9509a2c34fede2b380c8e7742877b2703345bd61bd71c`**. Installation on emulator-5554 with `-r` returned **Success**, and device `pm path` plus `sha256sum` matched the local APK. [Identity report](final-fix-build-install.json), [install log](logs/final-fix-install.log). No wipe or app-data clear occurred.

| Earlier-wave assertion | Evidence and result |
|---|---|
| Retained Navigate checkpoint | [72](android/72-final-fix-restored-navigate.png) and paired XML restore Navigate/Trolltunga after the new install. Original R2 upgrade proof remains separately attributed below. |
| Activity hero and row sync status | Offline applied in [75](android/75-final-fix-offline-applied.xml); save-sheet actions added Half Dome and Mount Takao to My favorites. [79 PNG](android/79-final-fix-activity-pending-visible.png)/[XML](android/79-final-fix-activity-pending-visible.xml) show both fully visible `Waiting for a connection` lines and saved hearts. |
| For You recommendation sync status | The same shared save flow added Mist Trail to My favorites. [80 PNG](android/80-final-fix-foryou-pending.png)/[XML](android/80-final-fix-foryou-pending.xml) show the saved heart and inline `Waiting for a connection`. |
| Online transition | Online restored and Applied in [81](android/81-final-fix-online-restored.xml). [82](android/82-final-fix-foryou-synced.png) and [83](android/83-final-fix-activity-synced.png), with paired trees, show waiting lines removed while hearts remain saved. |
| Test changes restored | All three temporary favorites additions were removed through their save sheets. The stopped [before08](database/08-before-final-fix-snapshot.json)/[after09](database/09-after-final-fix-snapshot.json) archives and [backend comparison](database/final-fix-comparison.json) prove all five original memberships restored, fixture payloads unchanged, seed4 unchanged and original backend configuration restored. |
| Device settings restored | [84](android/84-final-fix-gestures-restored.png) and [settings report](logs/final-fix-device-settings.json): font1.0, gestural navigation enabled, three-button navigation disabled, accessibility0/no services. Stored backend config `{}` resolves to its original ONLINE/error0/50–200ms/rate-limit0 defaults. Isolated ADB5038 stopped successfully after verification; shared5037 untouched. |

The final-wave [action notes](logs/final-fix-device-actions.md) identify the first drawer gesture failure and temporary three-button capture condition. Capture74 is Google Calendar onboarding after system Back intercepted the swipe and is excluded from product acceptance. Capture77's PNG/XML straddle toast disappearance;79 supplies matching Activity proof. Capture83's earlier XML contains a toast absent from its PNG; both support removal of inline waiting lines. The [capture index](android/capture-index.json) assigns that wave’s exact APK to accepted captures72–84.

At this earlier wave, the actual installed cached Activity Offline Retry remained unmeasured. The owner-authorized follow-up above closes that measurement gap. These earlier save-sync checks exercised mutations; the original unsuccessful Retry probes43–46 remain below. Induced Activity catalog failure has presenter/UI regression evidence, without an installed catalog-failure claim.

## Branch and commits

Initial acceptance source: `matt-ramotar/remaining-roots` at `dc354d1a0ef777452515bf763b887602bfb0f16a` (captures 01–63 and original JVM gate). Navigation correction source: `6e56873fc298cf874cb68eb3b76cc4419026fd66`. The corrected APK was installed and independently device-hashed before captures 66–71; its 200% navigation revalidation passes on all three roots. Captures 64/65 have unknown APK provenance and are excluded from corrected-build acceptance. The later authorized follow-up at `036ed84` exercises the actual Offline Retry; `a3df23f` corrects test-only logging. Final review disposition is recorded in the execution ledger. The original acceptance package changed documentation and evidence only. Protected `docs/evidence/m1/` and `redesign/` were not changed. Every measurement below comes from this task; earlier failure logs are explicitly attributed to their originating tasks.

## Final review fix wave

The later consolidated source fix wave has a **fresh 128-test, 38-suite, 13-module JVM gate**, with zero failures/errors/skips and fresh app-core compilation. [Final-fix source attribution, per-module counts, XML and logs](jvm/final-fix/README.md) are separate from all historical results below. This local gate covers the sync-feedback and Activity catalog-recovery fixes; the separately attributed authorized follow-up above supplies the installed cached Offline Retry proof.

## Original JVM gate

The complete prescribed gate ran once, adding per-task `--rerun` to each `jvmTest` to obtain fresh execution rather than stale XML. No failing gate was rerun unchanged.

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest --rerun :multiplatform:data:trail:impl:jvmTest --rerun :multiplatform:feat:savetrail:impl:jvmTest --rerun :multiplatform:feat:filters:impl:jvmTest --rerun :multiplatform:screen:explore:impl:jvmTest --rerun :multiplatform:screen:traildetail:impl:jvmTest --rerun :multiplatform:screen:saved:impl:jvmTest --rerun :multiplatform:screen:navigate:impl:jvmTest --rerun :multiplatform:screen:activity:impl:jvmTest --rerun :multiplatform:screen:foryou:impl:jvmTest --rerun :multiplatform:di:graph:active:jvmTest --rerun :multiplatform:app:bootstrap:impl:jvmTest --rerun :multiplatform:app:core:jvmTest --rerun :multiplatform:app:core:compileKotlinJvm
```

**BUILD SUCCESSFUL in 55s**, 495 actionable tasks: 92 executed, 403 up-to-date. All thirteen test targets executed freshly. Core JVM compilation passed. No compiler warnings were found in either task8 Gradle log. [Complete log](logs/roots-jvm-gate.log), retained also at `/private/tmp/roots-jvm-gate.log`; [machine-readable counts](jvm/counts.json), with fresh suite XML under `jvm/`.

| Module | Suites | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| `:multiplatform:foundation:designsystem` | 6 | 11 | 0 | 0 | 0 |
| `:multiplatform:data:trail:impl` | 10 | 36 | 0 | 0 | 0 |
| `:multiplatform:feat:savetrail:impl` | 5 | 22 | 0 | 0 | 0 |
| `:multiplatform:feat:filters:impl` | 1 | 1 | 0 | 0 | 0 |
| `:multiplatform:screen:explore:impl` | 2 | 4 | 0 | 0 | 0 |
| `:multiplatform:screen:traildetail:impl` | 1 | 1 | 0 | 0 | 0 |
| `:multiplatform:screen:saved:impl` | 1 | 2 | 0 | 0 | 0 |
| `:multiplatform:screen:navigate:impl` | 2 | 2 | 0 | 0 | 0 |
| `:multiplatform:screen:activity:impl` | 3 | 6 | 0 | 0 | 0 |
| `:multiplatform:screen:foryou:impl` | 2 | 7 | 0 | 0 | 0 |
| `:multiplatform:di:graph:active` | 1 | 8 | 0 | 0 | 0 |
| `:multiplatform:app:bootstrap:impl` | 1 | 2 | 0 | 0 | 0 |
| `:multiplatform:app:core` | 2 | 14 | 0 | 0 | 0 |
| **Total** | **37** | **116** | **0** | **0** | **0** |

The navigation correction subsequently ran `./gradlew :multiplatform:foundation:designsystem:jvmTest :multiplatform:foundation:designsystem:compileKotlinJvm :multiplatform:app:core:compileKotlinJvm :apps:android:compileDebugKotlin` at `6e56873fc298cf874cb68eb3b76cc4419026fd66`: **14 fresh design-system tests, zero failures/errors/skips**, BUILD SUCCESSFUL in 9s, 1,181 actionable tasks (63 executed, 1,118 up-to-date). Design-system JVM and Android Kotlin compilation executed freshly; app-core JVM and Android entry-point compilation were UP-TO-DATE. Its [passing XML](jvm/nav-fix/) and [gate log](logs/roots-task8-nav-fix-gate.log) are retained separately from the original gate. Replacing the original 11 design-system tests with those 14 yields **119 composite passing tests** across the two source revisions. The [historical composite count report](jvm/nav-fix/composite-counts.json) records the source revision for every module. This is not a fresh 119-test whole-branch execution.

Earlier first-red logs were copied unchanged from `/private/tmp/roots-task*-red.log` into [`logs/`](logs/). Those are development first failures, distinct from the successful fresh gate above. [First-red inventory](logs/first-red-inventory.json) records seven preserved earlier-task logs and their bytes/hashes. The navigation correction additionally retains its [first-red log](logs/roots-task8-nav-fix-red.log) and [XML](logs/roots-task8-nav-fix-red.xml), [default-layout diagnostic](logs/roots-task8-nav-fix-default-diagnostic.log), and [first implementation log](logs/roots-task8-nav-fix-green.log)/[XML](logs/roots-task8-nav-fix-first-green.xml). The first red independently reproduced broken whole labels at 200%; the diagnostic corrected a paragraph-width assertion, and the intermediate implementation failed the default one-row assertion before the final gate passed.

## Build and install

`./gradlew :apps:android:assembleDebug` completed **BUILD SUCCESSFUL in 10s**, 1,386 actionable tasks: 57 executed, 1,329 up-to-date. [Assembly log](logs/roots-assemble.log), also `/private/tmp/roots-assemble.log`.

Original APK from `dc354d1a0ef777452515bf763b887602bfb0f16a`: `apps/android/build/outputs/apk/debug/android-debug.apk`, **87,935,891 bytes**. Built and installed SHA-256: `ee50f2469ad7343d66645f7b65ec380b1bf6a9c2ec6fb009ed87920105310cd6`. [Identity report](build-install.json). `adb -s emulator-5554 install -r` returned **Success** ([install log](logs/install.log)).

Device: `emulator-5554`, `Trails_Preview_API_35`, Android 15/API35, 1280×2856 physical pixels, 480dpi (3px/dp). Only this emulator was used; `emulator-5556` was online and untouched. The existing installation was upgraded without wipe or app-data clearing. [Initial device settings and APK](database/01-device-before.json).

Corrected-build assembly at `6e56873fc298cf874cb68eb3b76cc4419026fd66` succeeded in 1s, 1,382 actionable tasks (61 executed, 1,321 up-to-date); [assembly log](logs/nav-fix-assemble.log). The controller's subsequent [assembly verification](logs/controller-nav-fix-assemble.log) succeeded in 489ms, 1,382 tasks (57 executed, 1,325 up-to-date), with APK packaging/assembly UP-TO-DATE. Corrected APK: **88,558,103 bytes**, built and installed SHA-256 **`d94158f5329c3cd3f653fb55bb2f3f3b7c7573ab628d034ed249dd8dd74e2138`**. Installation using `adb -P 5038 -s emulator-5554 install -r` returned **Success**; `pm path` followed by device `sha256sum` independently matched the built APK. [Identity report](nav-fix-build-install.json), [install log](logs/nav-fix-install.log).

The first controller install/shell clients stalled on the shared ADB server and were interrupted without a success claim. An isolated server on port 5038 recovered transport, with discovery restricted to emulator-5554; the shared server and emulator-5556 were untouched. Only captures 66–71 are accepted as the corrected installed build. No app-data clear or device wipe occurred. The isolated server was stopped after final verification. [Recovery chronology](logs/controller-adb-recovery.md).

## Checkpoint upgrade

Before installation, force-stopped the R2 app, copied `databases`, `files` and `shared_prefs` using `run-as … tar -cf -` to `/private/tmp/roots-01-pre-upgrade.tar`, then read the copied databases locally. After the first upgraded launch, repeated the stopped snapshot to `/private/tmp/roots-02-post-upgrade.tar` before exercising new roots. Raw archives stay outside Git; their SHA-256 values are in the reports.

- [Before checkpoint](database/01-pre-upgrade-snapshot.json): version1, selected Explore, only Explore/Saved route fields; all new-root fields absent.
- [After checkpoint](database/02-post-upgrade-snapshot.json): every pre-existing checkpoint field preserved exactly; three new root stacks default to their root screen. The inactive account checkpoint was unchanged.
- [Comparison](database/upgrade-comparison.json): backend seed **4→4** and all three prior memberships unchanged (`alpine-lake-loop`, `half-dome`, `trolltunga` → `weekend`). No sample fixture rows existed before or immediately after launch, before entering the new roots.
- [Cold-launch PNG](android/01-upgrade-cold-launch.png) and [tree](android/01-upgrade-cold-launch.xml): Explore default search/query, 50 trails, Most popular sort, five navigation labels. No `Couldn’t restore your place` line.

## Cells 10–12

Device acceptance is recorded by assertion below. No missing device assertion is inferred from JVM tests. [Persistent-shell action transcript](logs/persistent-session-actions.md) records actual mode changes, navigation and force-stop/start boundaries; [captured shell history](logs/roots-persistent-history.txt) is a partial native history export.

| Cell | Measured result | Evidence |
|---|---|---|
|10 — For You content|Feature photo/copy, `Based on your activity`, `More like Half Dome`; six named rows with rating, difficulty, distance and save hearts.|[02](android/02-foryou-initial.png), [08](android/08-foryou-all-six-rows.png), corresponding XML|
|10 — row and save|Mist Trail row opens its detail; Back returns; heart opens save sheet; selecting Weekend adventures and Save trail shows `Saved to Weekend adventures`.|[03](android/03-foryou-row-detail.png), [04](android/04-foryou-save-sheet.png), [05](android/05-foryou-saved-toast.png), corresponding XML|
|10 — Offline/restart|Pass: cached six-row feed remains without an error line, including after force-stop/start with For You selected and scroll restored. Database03 confirms OFFLINE and cached feed.|[21](android/21-foryou-offline.png), [22](android/22-foryou-offline-restart.png), [database03](database/03-foryou-offline-db.json)|
|11 — Navigate|Name pill opens matching detail; coming-soon toast has no tick and disappears between original captures 25/26. Corrected-build captures 70/71 add timed observations: visible during the PNG capture interval 0.353–1.293s after the tap and absent during 5.206–5.835s; after opening Half Dome from Explore, pill follows it. Selected root/Half Dome survives restart; first restart captures showed loading, later53 confirms readiness.|[23](android/23-navigate-initial.png), [24](android/24-navigate-detail.png), [25](android/25-navigate-recording-toast.png), [26](android/26-navigate-toast-gone.png), [49](android/49-explore-half-dome-detail.png), [50](android/50-navigate-follows-half-dome.png), [53](android/53-startup-settled.png), [70](android/70-recording-toast-timed.png), [71](android/71-recording-toast-after-five-seconds.png), [timing](logs/recording-toast-timing.json)|
|12 — Activity|Month46.9km/5trails/22h equals September backend rows46900m/1320min; hero copy correct; Mount Takao row opens trail; heart opens sheet and saves Weekend adventures; cached Offline retains content. Original probes43–46 did not exercise Retry: healthy cached UI exposed none and44 was bootstrap. The later authorized developer action87, actual tap, retained content88–90, positive online failure92 and recovery94 now verify this requirement.|[33](android/33-activity-online.png), [fixture rows/sum](database/04-activity-online-snapshot.json), [35](android/35-activity-row-detail.png), [38](android/38-activity-save-sheet.png), [39 screenshot only](android/39-activity-saved-toast-export-failure.png), [43](android/43-error-rate-full.png), [45](android/45-activity-offline-config.png), [46](android/46-activity-cached-offline.png), [database06](database/06-activity-offline-db.json), [measured Retry acceptance](retry-followup-acceptance.json)|
|New-root restart stacks|All three selected roots restore: For You root/scroll, Navigate root/Half Dome and Activity root. Pushed details also restore with their owning root selected: Activity/Mount Takao, Navigate/Half Dome and For You/Mist Trail. The For You archive pull stalled, so its pushed-stack proof is the captured UI, not a decoded archive.|[22](android/22-foryou-offline-restart.png), [34](android/34-activity-root-restart.png), [36](android/36-activity-detail-restart.png), [Activity checkpoint](database/activity-detail-checkpoint.json), [53](android/53-startup-settled.png), [54 before](android/54-navigate-half-dome-detail.png), [56 after](android/56-navigate-detail-ready.png), [Navigate checkpoint](database/navigate-pushed-checkpoint.json), [57 before](android/57-foryou-detail-before-restart.png), [58 after](android/58-foryou-detail-restored.png)|

## Paired comparisons

Figma source frames were exported by the controller from live file `4B7GK9ndPVQ1BFIKGg0Zqj` using screenshot base64 recovery; [manifest](screenshots/manifest.json). Each is 390×844. Stored Android captures are 378×844 from a 426.7×952dp device, so geometry differs. These pairs retain the initial APK at default font scale: F02 is capture02 and F25 is capture23 with gestural OS navigation; F07 is capture33 with temporary three-button OS navigation and its green system strip. Comparisons are visual observations, not pixel-diff results, and do not establish post-correction rendering.

| Frame | Figma | Android | Observations |
|---|---|---|---|
|F02 `148:55`|[Figma](screenshots/f02-figma.png)|[Android](screenshots/f02-android.png)|Real Trolltunga photo, Half Dome anchor and real catalog rows replace placeholders; avatar omitted by DEV-34; native platform proportions and icons differ. No default-scale clipping observed.|
|F07 `148:71`|[Figma](screenshots/f07-figma.png)|[Android](screenshots/f07-android.png)|September sample dates and measured fixture totals replace reference content. Near-black green month card differs from Figma forest green; taller hero and metadata flush to page margins rather than reference inset. Capture33 uses temporary three-button OS navigation/green system strip. No default-scale text clipping observed.|
|F25 `236:2258`|[Figma](screenshots/f25-figma.png)|[Android](screenshots/f25-android.png)|Plain deterministic schematic replaces basemap and route geometry, explicitly labelled; map circles omitted. Actual last-opened trail/name and native proportions differ. Sheet copy present. No default-scale text clipping observed.|

## Accessibility and 200 % font scale

Default-scale XML trees are preserved with captures. [Initial measured controls](accessibility.json): all five navigation items expose View semantics, selected state on For You, and 75.7–76.0×56dp bounds; all six fully visible recommendation hearts expose Button semantics and 48×48dp bounds. The partially scrolled Franconia heart in capture02 has a viewport-clipped 48×22.3dp tree rectangle; its fully visible control in capture08 is 48×48dp.

Additional original-APK measurements from the raw XML use the measured density of 3px/dp:

| Capture/control | Android tree role | Bounds px | Size dp |
|---|---|---|---|
|[23](android/23-navigate-initial.xml), Mist Trail name pill|Clickable `android.view.View`|[60,216][1220,360]|386.7×48|
|[23](android/23-navigate-initial.xml), Start recording|Clickable `android.view.View`|[60,2292][1220,2448]|386.7×52|
|[33](android/33-activity-online.xml), Half Dome heart|`android.widget.Button`|[1040,1279][1184,1423]|48×48|
|[33](android/33-activity-online.xml), Mount Takao heart|`android.widget.Button`|[1076,2143][1220,2287]|48×48|
|[61](android/61-foryou-root-font-200.xml), Mist Trail/Upper Yosemite/Angels Landing hearts at 200%|`android.widget.Button`|144×144 each|48×48 each|
|[63](android/63-navigate-font-200.xml), Trolltunga name pill at 200%|Clickable `android.view.View`|[60,216][1220,386]|386.7×56.7|
|[63](android/63-navigate-font-200.xml), Start recording at 200%|Clickable `android.view.View`|[60,2130][1220,2319]|386.7×63|

The partially visible Bondi heart in capture33 is viewport-clipped to 48×16.7dp; it is not a full-target acceptance measurement. Android View exposure for the name pill/recording action must not be described as a native Button role.

**Original 200% failure:** [61 PNG](android/61-foryou-root-font-200.png) and [XML](android/61-foryou-root-font-200.xml) show navigation labels breaking within words and clipping at the capsule boundary. [62](android/62-foryou-font-200-top.png) preserves the For You top state, and [63](android/63-navigate-font-200.png) preserves Navigate at 200%. Captures59/60 actually show Trolltunga detail, despite their attempted-state filenames, and are not For You root acceptance. No original Activity 200% capture was completed. The correction at `6e56873fc298cf874cb68eb3b76cc4419026fd66` has fresh Compose text-layout and 48dp regression coverage at 320/360dp and 200%; the corrected installed APK was then revalidated below.

**Corrected installed build:** all five navigation labels are fully legible in two rows at 200% on [Activity66](android/66-corrected-font-200-current.png), [For You67](android/67-corrected-foryou-font-200.png) and [Navigate68](android/68-corrected-navigate-font-200.png), each with paired XML. Activity month totals and labels fit; For You campaign copy and the visible recommendation remain readable; Navigate's name pill, complete sheet copy and Start recording action remain above the navigation. The larger navigation reduces the scrolling feeds' visible area, so content farther down is outside these top-state captures. [Capture69](android/69-corrected-default-nav.png) confirms the default font scale restores the single-row pill.

The `correctedBuild` section of [accessibility.json](accessibility.json) records **26 measured controls, all at least 48×48dp**: five navigation items on each of captures66–69, the visible Activity/For You hearts, and Navigate's name/action at both scales. At 200%, first-row nav targets are 126.0–126.3×67dp, second-row targets 189.3×67dp, visible hearts 48×48dp, the name pill 386.7×56.7dp and recording action 386.7×63dp. Selected destination state is exposed in each tree; selected items report `clickable=false`, while other destinations report `clickable=true`. This records tree semantics and target bounds without inferring a native Button role for View controls.

TalkBack was **not run**; initial accessibility_enabled was `0` and enabled_accessibility_services was `null`. Tree labels/roles/bounds do not prove spoken focus order, traversal or announcements.

## Deviations

DEV-31…DEV-37 remain the planned preview boundaries. Navigate coming-soon toast/schematic/omitted map controls (DEV-31–33), For You omitted avatar (DEV-34), Activity row-to-trail/heart save (DEV-35–36) are observed above. DEV-37 has seeded per-account database rows and sample dates; Welcome disclosure was source-inspected and compiled in Task 7; the Task 8 gate reused that compilation (UP-TO-DATE). No retained JVM test asserts the disclosure, and no installed Welcome assertion was performed on this retained signed-in account.

## Boundaries and first failures

[Controller first failures](logs/controller-first-failures.md) and [exact dependency source recovery](logs/dependency-audit.md) preserve the initial missing Store6 source and its authorized recovery. Store6 revision `582edfe86e64ddc71312ecd20a1895fc3de37b52`, Atom revision `05daa800ac3c6d0dc1f9538234b99061c8139e40`; pins and artifact bytes were unchanged. The successful restored candidate guard alone was structural evidence; this task separately ran the consumer JVM gate and installed the APK.

[Device-driving failures](logs/device-driving-first-failure.md) preserve the system Back interceptions and adb stalls. Failed captures are kept under their original names; `*-export-failure.png` has no valid paired XML and is never treated as a successful accessibility export. Capture07/09/10/13 show the launcher, despite their attempted-state names, and are not product acceptance captures. Capture06 still shows the save toast and does not prove lower rows; capture08 does.

No iOS/Web/desktop behavioral acceptance, production API, map provider, actual GPS/offline network, recorder, personalization, publication or release claim. Developer Offline is a fake-backend setting, distinct from airplane mode.

The initial [settings restoration check](logs/device-settings-restoration-check.json) predates resumed acceptance, and capture 47 later confirms Offline false/errorRate 0.00. After all corrected-build captures, the [final settings report](logs/final-device-settings.json) verifies `font_scale=1.0`, gestural navigation active, three-button navigation inactive, `accessibility_enabled=0` and no enabled accessibility services. The force-stopped [final database snapshot](database/07-final-controller-snapshot.json) preserves checkpoint/fixture evidence and backend seed 4. Stored backend configuration is `{}`; applying the documented `StoredBackendConfig` defaults resolves it to ONLINE, errorRate 0.0, latency 50–200ms and rateLimit 0. The isolated ADB 5038 server was stopped with exit 0; the shared server remained untouched.

The corrected-build [timed toast observations](logs/recording-toast-timing.json) establish the exact copy/no tick in70 and its absence in71 after five seconds. PNG acquisition intervals were 0.353–1.293s and 5.206–5.835s after the tap; XML exports finished later at 3.680s and8.202s. These observations do not identify the exact disappearance instant or prove absence at precisely 5.000s.

The original cached Offline Retry gap is now measured through the real installed action in captures87–90, with the separate online failure/recovery control92–94. Original failed probes are preserved; no acceptance waiver was used. Task and whole-branch review disposition is recorded in the execution ledger.

The refreshed [capture index](android/capture-index.json) records PNG dimensions/hashes, XML validity and APK provenance through 95; 64/65 remain source-unknown. [actions.jsonl](android/actions.jsonl) and the persistent-shell transcript preserve action times and the recovery sequence. Original failed captures and probe states remain unchanged. Capture names describe attempted states and are interpreted against their actual PNG/XML contents above.
