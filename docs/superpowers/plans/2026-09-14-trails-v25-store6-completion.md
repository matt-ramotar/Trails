# Trails v2.5, Figma Design, and Store6 Implementation Plan

> **For agentic workers:** Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` when implementation is requested. Checkboxes track execution, not approval or current completeness. The September 15–16 execution implements M1; later milestones remain planned. User authorization in the current task controls execution.

**Goal:** Preserve the completed v2.5 checkpoint, then finish Trails as a polished hiking app using the current custom Figma design and Store6 in its real persisted read and mutation paths.

**Architecture:** Retain Compose Multiplatform, Circuit, Metro, TrailsTheme, API/implementation module boundaries, bootstrap and account lifetimes, and the fake-backend simulation tools. Replace the ski-feed domain with trail, saved-collection and activity contracts. Store6 owns persisted values and mutation recovery behind Trails repositories; Atom owns selected transient feature state.

**Tech stack:** Kotlin Multiplatform, Compose Multiplatform, Circuit, Metro, SQLDelight, and a reproducibly pinned Store6/repaired Atom consumer tuple. HeroUI is the Figma design basis and maps to Compose components; it is not a new React runtime.

**Updated:** September 18, 2026. Current design and scope are defined in [the completion contract](../../trails-completion-contract.md). The visual authority is design revision R2: the [Trails v4 · AllTrails redesign](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=153-97) section (`153:97`, 28 frames after the September 18 second pass), the [Components · Messaging](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=248-2777) page and the restored original compass. The accepted M1 implementation targets the September 15 snapshot; the [R2 alignment plan](2026-09-18-trails-design-revision-alignment.md) closes that gap before M2. AllTrails and other apps provide inspiration; an exact Mobbin collection census no longer blocks implementation.

**Status:** Tasks 1–2 are completed locally in `86209d7` and `2a41dd5`. The recorded checkpoint gate passed 57 tests, JVM compilation and Android assembly; see the [checkpoint report](/Users/matt/src/matt-ramotar/atom/docs/reviews/2026-09-14/TRAILS_V25_CHECKPOINT.md). Twenty Figma screen/state frames and the current original-compass master were inspected live for this revision. The checkpoint is preserved. The September 15–16 M1 execution below provides separate Store6, hiking-domain and installed Compose evidence; checkpoint checks are not reused as M1 proof.

**M1 execution, September 15–16 — accepted for Android:** [Execution evidence](../../evidence/m1/README.md) preserves the v2.5 baseline, completed custom Figma snapshot and bounded C3 JVM/Android gate (13 file-backed SQLDelight consumer tests, 288 repaired-Atom tests, installed consumer restart/upgrade proof). Production M1 is implemented and installed with 64 passing targeted tests. All nine installed acceptance cells pass with explicit per-build attribution. The final APK repeats actual TalkBack opener/focus-return checks (172 assertions), native drawer Back, and Applied Offline save/force-stop/relaunch/reconnect (269 persistence assertions). The original five server receipts remain byte-equivalent; reconnect adds one effect/receipt and retires sequence six. Earlier fault, account-isolation, separate fresh Offline install, large-text and route/query/scroll evidence remains preserved. First failures and their diagnosed corrections are retained. Changes remain uncommitted; no commit, push or release was requested. M2–M4 remain open.

**Design revision R2, September 18 — recorded, implementation planned:** The Figma received a second AllTrails-derived pass and two follow-up rules. Tokens moved to a white background with Ink `#171E14`, Muted `#545A52`, Border `#E6E8E4`, Soft `#F4F5F4` and Citron `#A9F184`; navigation became a floating five-tab pill (Explore, For You, Navigate, Saved, Activity) with Profile behind the avatar; the trail card gained a heart and download circle, a highlight chip and a structured facts row with coloured difficulty markers (Easy, Moderate, Hard, Strenuous); Explore chips became All / Difficulty / Length / Elevation gain with a Most popular sort; Filters gained Strenuous, Length 0–50 km, Elevation gain, Highest point, Distance away, Dog- and Kid-friendly switches and Activity chips; Trail detail gained share/more circles, an underlined rating row, divided facts, Show more, route-map overlays, Preview/Photo tour cards, Customize route/Get directions, Top sights and tags; Saved gained a Favorites placeholder tile and Recommended lists; the Map gained layers/3D/compass/location/download/draw controls; three frames were added (Trail detail · Route and sights, Filters · More options, Sight detail). Buttons now follow one rule (citron hero, dark commit, soft secondary, white icon circles; accent never a fill) and messaging uses a dark toast, a one-line status line and a decision sheet instead of card notices. The paywall was not adapted. The M1 evidence exports under `docs/evidence/m1` still show the September 15 design. Implementation of the M1-surface subset is specified task by task in [the R2 alignment plan](2026-09-18-trails-design-revision-alignment.md); elements that need maps, location, sharing or later roots are assigned to M2–M4 in Task 8 below.

## Consolidation authority

**September 16 continuation:** [Fresh verification](../../evidence/m1/continuation/README.md) preserves the prior 624-file source snapshot, refreshes live Figma and immutable dependency checks in parallel, and closes the previously omitted selected-trail review excerpt. The correction passes 20 affected data tests, detail JVM compilation and Android assembly. Installed APK `01f0dfee6057ba2f801ad3f7cdc8488bd3b3bb6b838569c6c8fa5a43e2d0a4ac` repeats the durable save/restart/reconnect journey with 199 passing receipt-conservation assertions and an added-review 200% text check. The AVD restored an older v2.5 snapshot, so this run has a new M1 installation identity: its existing first receipt survives the APK correction and Offline restart; reconnect adds exactly one second receipt/effect. Earlier nine-cell acceptance retains its per-build attribution, including fault, account, fresh-Offline and TalkBack checks that were not all rerun. Changes remain uncommitted and M2–M4 remain open.

The [unified Atom, Sower, and Store6 Trails plan](/Users/matt/src/matt-ramotar/atom/docs/superpowers/plans/2026-09-14-atom-sower-trails-plan.md) controls shared dependency ordering and Atom adoption. This plan and its completion contract supersede older exact-AllTrails scope/census instructions in linked planning/specification inputs. The change is limited to Trails product/design scope and its first journey. Preserve the joint compatibility gate C3, repaired-Atom prerequisites, account-owned Store6 durability, Sower gates, and separate public/release boundaries.

The first proposed implementation journey is **Explore → Trail detail → Save to list → Saved**, including search/filter and offline restart. It supplies the representative transient Atom feature through the save flow and the real Store6 data path. It is M1, not full Trails completion. M2–M4 retain maps, Moments, recording, activity, profile, remaining states, platform hosts and polish.

## 1. Completion contract and scope

The user's requirements are jointly necessary:

1. **Current Trails design:** Implement the Figma layouts, brand, original compass, navigation and intended controls through reusable Compose components. Preserve the design's intent while adapting system insets, keyboard/back, accessibility and platform services.
2. **Polished UX:** Complete journeys with truthful data, responsive interaction, useful loading/error/pending/permission states, accessibility and reviewed installed-app behavior.
3. **Store6 in the real app path:** Persisted trail/query/account/activity reads and applicable user mutations use Store6. No unused dependency or isolated demo screen satisfies this requirement; no Store5 runtime dependency remains at completion.

[The completion contract](../../trails-completion-contract.md) records the 20 current Figma frames, all four milestones, known state gaps, ownership, runtime decisions, asset handoff and the exact M1 acceptance script. Figma frames are visual evidence; static counts, prototype links and success copy are not domain truth or runtime proof. Derive counts, availability and saved/recording state from shared repositories and platform services.

Under design revision R2 the roots are Explore, For You, Navigate, Saved and Activity in a floating pill navigation, with Profile opened from the avatar; For You carries the former Moments content. Map and recording remain confirmed design scope, with capability proof still required. Offline basemap downloads (the R2 Downloads tab and download circles), custom route editing (the R2 Customize route button), subscriptions (the R2 paywall was deliberately not adapted), purchasing and full social/review-authoring systems are not implied by the AllTrails reference. Add them only through an explicit contract change.

Android is the required first preview and runtime proof. Full iOS runtime parity remains proposed; desktop/Web runtime completion remains undecided. Task 3 must record the platform contract before declaring the sample finished. Existing targets should continue to compile where compatible, but neither their presence nor a compilation pass establishes a working host. Do not drop targets silently to accommodate a dependency.

The fake backend remains intentional. Its fixtures must make enabled controls work and preserve account/server authority across the restart demo. Sample entry must say what it does; unavailable external authentication or purchases must not masquerade as real services. Record material platform/provider/asset adaptations in the discrepancy ledger. Do not add Store6 internals to normal product flows.

## 2. Historical verified starting point (before M1)

Verified from the current checkout on September 15, 2026:

- Checkout `/Users/matt/src/mobilenativefoundation/Trails`, branch `matt-ramotar/v2.5`, HEAD `2a41dd56e514d34cf910e07679df7ca7791c5edf`.
- The previous code changes are committed. At the start of this revision, only `docs/superpowers/` and `docs/trails-design-inspiration.md` were untracked. This planning revision adds/updates documentation, not runtime code.
- Checkpoint implementation commit `86209d747fae68cc0f6081aa736a26b02b39a249`; repository housekeeping commit `2a41dd56e514d34cf910e07679df7ca7791c5edf`.
- Current consumed dependencies: Store5 `5.1.0-alpha07`, Atom `0.1.0-alpha02`, Kotlin `2.2.20`, Compose Multiplatform `1.9.0`, KSP `2.2.20-2.0.4`, Metro `0.6.7`, Circuit `0.30.0`, coroutines `1.10.2`, SQLDelight `2.1.0`, AGP `8.12.3`, Gradle `8.13`, JVM 17. These were the baseline catalog/wrapper values; the consumed M1 tuple is recorded in the integration report.
- The current app is still the ski/post sample. Active navigation registers Home/Profile; no new trail, saved, map or recording repository/screen implementation is present. Profile's saved content is mock state. The Figma implementation is new product behavior, not a rename of post bookmarks.
- The Android database is file-backed; the existing JVM factory uses `JdbcSqliteDriver.IN_MEMORY`. New persistence tests need file-backed reopened databases.
- Fake services run in process, gate availability through `BackendConfig.networkMode`/`NetworkGate`, and keep server tables in memory. Persist demo-server data/receipts or use a separate deterministic backend before claiming client-restart recovery.
- Store6 and repaired-Atom source/publication/toolchain readiness are unresolved. Task 4 records live authority instead of assuming a locally cached branch or dirty tree is a releasable dependency.

### Historical evidence and remaining findings

The pre-checkpoint snapshot was `3ec393a` with 18 modified tracked files and additional untracked/staged artifacts. Tasks 1–2 below retain its completed preservation and repair ledger. The feed-loss test, legacy conflict-setting translation, stale-version validation and drawer-shortcut defects were addressed by the checkpoint; do not re-open them solely because an old plan described them as current.

Earlier `:apps:android:assembleDebug` and the launch on `Trails_Preview_API_35` (`emulator-5554`, Android 35 Google APIs ARM64) demonstrated the old ski feed. The capture `/private/tmp/trails-preview.png` and APK `apps/android/build/outputs/apk/debug/android-debug.apk` are historical preview evidence; recheck their existence and build identity before reuse. The checkpoint APK was not installed as part of checkpoint acceptance.

An earlier `:server:fake:jvmTest` attempt failed before Gradle startup because access to `gradle-8.13-bin.zip.lck` was rejected. It executed **zero tests**; `/private/tmp/trails-v25-plan-backend-tests.log` records that attempt. Subsequent checkpoint results are separately recorded in the checkpoint report. Preserve first failures and distinguish executed, up-to-date, compiled and installed evidence.

Remaining work includes rendering/replacing the current scaffold, replacing hardcoded profile/entry behavior, modeling hiking data, restoring account lifetimes, proving map/location/recording services, implementing promised native/JVM/Web hosts, and adding active CI under `.github/workflows` rather than the existing `.github/logo` location. The current user-store local updater is not evidence of a real server profile write; keep local session transitions separate from server-owned profile mutations.

## 3. Delivery order

~~~text
Completed: Tasks 1–2 / C1: verified v2.5 checkpoint and local commits
Completed: Task 8g / R2: aligned tokens, components, M1 screens and messaging with design revision R2 (separate plan); evidence in docs/evidence/r2
Next, in parallel:
  Task 3 / C2: snapshot Figma, close M1 state/control contract, record platforms
  Task 4 / C3: pin and prove the joint Store6 + repaired Atom consumer
Then:
  Task 5 / C4: shared Compose shell/assets; independent map/location spikes
  Tasks 6–8 / M1: trail reads + durable saved membership + complete Android journey
  Tasks 5–8 / M2: map and Moments journeys
  Tasks 5–8 / M3: recorder, completed activities and activity history
  Tasks 6–9 / M4: profile/preferences, remaining controls, selected hosts, UX polish
  Task 10: clean-checkout, hosted verification, sample documentation and final review
~~~

Task 3's current-frame inventory is recorded; remaining state/asset/control decisions are closed per milestone. There is no all-Mobbin-census prerequisite. C3 remains mandatory before an accepted Store6/Atom consumer, and the shared repaired-Atom gate remains mandatory for its representative save flow. Unresolved later map/location or platform choices do not block independent M1 design/data work. Store6 is integrated per journey, not retrofitted after a UI rewrite.

## Task 1: Preserve the current work and prepare the index

**Owner:** Integration lead.

**Files:** Entire existing diff, .gitignore, and the index-only mockup.html entry.

- [x] Record HEAD, branch, staged diff, unstaged diff, and untracked file inventory in a local evidence directory. Preserve actual untracked Kotlin content as well as patches.
- [x] Verify no other task is editing these files before staging.
- [x] Remove only the empty staged mockup.html addition from the index. It contains no design content and is already absent from disk.
- [x] Keep .agents/developer_tools.html as local design context unless it is deliberately selected as a shared artifact.
- [x] Stage explicit files/hunks per the ledger below. Do not use a blanket git add -A.

**Exit:** Every existing change has an explicit destination; nothing is discarded accidentally or attributed to this planning task.

## Task 2: Stabilize and commit v2.5

**Recommended commit ledger**

| Commit | Scope |
| --- | --- |
| feat(simulation): add seeded network and conflict controls | Backend config/control/simulation changes, service conflict plumbing, developer-settings API and persistence, repository strategy handling, and the drawer changes that consume them |
| fix(feed): preserve complete feeds while improving variety | Corrected feed behavior and its focused tests; include the test fixture seeding seam here if it can be separated cleanly |
| chore(repo): ignore local agent tooling | Only the intended .gitignore policy and newline normalization |

The simulation API, enums, settings, services, and drawer are coupled. Keep them together rather than introduce temporary compatibility APIs just to make smaller commits. Split the drawer's presentation changes into a separate feat(devtools) commit only if both intermediate trees compile without scaffolding.

**Exact affected areas**

- server/api/src/commonMain/kotlin/org/mobilenativefoundation/trails/server/BackendConfig.kt
- server/fake/src/commonMain/kotlin/org/mobilenativefoundation/trails/server/fake/{BackendControl,FakeBackendServer}.kt
- server/fake/src/commonMain/kotlin/org/mobilenativefoundation/trails/server/fake/internal/simulation/{ConflictSimulator,ErrorSimulator,LatencySimulator,SimulationRandomSource}.kt
- server/fake/src/commonMain/kotlin/org/mobilenativefoundation/trails/server/fake/internal/seed/SeedDataLoader.kt
- server/fake/src/commonMain/kotlin/org/mobilenativefoundation/trails/server/fake/services/{BaseFakeService,FakeFeedService,FakePostService,FakeUserService}.kt
- multiplatform/data/devsettings/{api,impl}/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/devsettings/
- multiplatform/data/post/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/post/RealPostRepository.kt
- multiplatform/model/db/src/commonMain/sqldelight/org/mobilenativefoundation/trails/db/DeveloperSettings.sq
- multiplatform/app/core/src/commonMain/kotlin/org/mobilenativefoundation/trails/app/DeveloperToolsDrawerHost.kt

- [x] Fix feed completeness first. Preserve all authored posts and stable pagination; curate seed ordering or use a non-dropping mixed-feed ordering policy for visual variety.
- [x] Replace tests that assert one surviving authored post with tests for complete authored feeds, stable membership/counts, mixed-feed variety where possible, and page-boundary correctness.
- [x] Add deterministic tests for fixed seeds, reseeding, error/latency injection, forced conflicts, and settings replay. Test only the promised replay scope; settings replay does not rewind server state.
- [x] Preserve the old refresh-then-retry behavior when translating persisted LAST_WRITE_WINS, unless an intentional compatibility change is recorded. Test all legacy enum translations and defaults.
- [x] Keep stale-version rejection independent from whether extra conflicts are injected. Test server-wins, client intent retry, and refresh/reapply semantics with final server and client values.
- [x] Scope R/Esc handling to appropriate drawer focus and open state; ensure ordinary text entry is unaffected.
- [x] Review reset semantics and persistence failures. Controls must not report successful application when persistence or backend application fails.
- [x] Run the narrow tests and compile/assembly gates below in an environment with permitted Gradle access.
- [x] Commit each reviewed tree using the ledger, recording which checks cover that tree. The baseline is complete only when all checkpoint commits pass together.

Suggested test files:

- server/fake/src/commonTest/kotlin/org/mobilenativefoundation/trails/server/fake/services/FakeFeedServiceNoRepeatTest.kt
- server/fake/src/commonTest/kotlin/org/mobilenativefoundation/trails/server/fake/SimulationContractTest.kt
- multiplatform/data/devsettings/impl/src/commonTest/kotlin/org/mobilenativefoundation/trails/data/devsettings/DeveloperSettingsCompatibilityTest.kt
- multiplatform/data/post/impl/src/commonTest/kotlin/org/mobilenativefoundation/trails/data/post/PostConflictResolutionTest.kt

~~~bash
./gradlew :server:fake:jvmTest
./gradlew :multiplatform:data:devsettings:impl:jvmTest :multiplatform:data:post:impl:jvmTest
./gradlew :multiplatform:app:core:compileKotlinJvm :apps:android:assembleDebug
git diff --check
~~~

**Exit:** A reproducible Store5-based v2.5 checkpoint with no known regressions from the uncommitted work. Store6 migration has not been mixed into these commits.

**Completed evidence:** `86209d747fae68cc0f6081aa736a26b02b39a249` combines the simulation and feed ledger entries with their shared APIs and fixtures; `2a41dd56e514d34cf910e07679df7ca7791c5edf` contains only `.gitignore` housekeeping. All 31 committed code/build/test blobs match the verified source snapshot. Separate specification and code-quality reviews passed. Final versus earlier executed test results, original failure evidence, source hashes, preserved local artifacts, and remaining optimistic-state limitations are in the checkpoint report. The current APK has not been installed or exercised as part of this checkpoint.


## Task 3: Snapshot the Figma design and close the implementation contract

**Owners:** Design lead and integration lead. **Shared gate:** C2. **Sources:** [completion contract](../../trails-completion-contract.md), [Trails v3](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=153-97), [brand/icon source](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=204-1685).

**Files:** Maintain `docs/trails-completion-contract.md`; create `docs/trails-reference-inventory.md` for versioned source/implementation evidence and `docs/trails-reference-deviations.md` for material discrepancies and temporary milestone omissions. Keep `docs/trails-design-inspiration.md` as historical source observations.

- [x] Record the current user-selected custom Figma direction and restored original compass, superseding strict AllTrails collection fidelity.
- [x] Inspect the current 20 Figma frames and record node IDs, milestone coverage and known state gaps in the completion contract.
- [x] Export/capture the M1 reference snapshot, semantic tokens, component variants, typography, permitted imagery and compass SVG; record export date and hashes. Do not use expiring links or local-only asset directories as build inputs.
- [x] Map every visible M1 control to an event, state owner, data operation and return/error behavior. Close focused search, filter drafts/counts, sheet cancel/save, saved collection detail/management and enabled navigation semantics before implementation.
- [x] Design M1 first-load/refresh/empty/offline/cache-miss states, local-save admission failure, pending work, retry/terminal handling, session restoration and account retirement. Treat Figma counts and success copy as illustrative until derived from real data.
- [x] Record temporary M1-only preview omissions of later controls as described in the contract. Do not leave enabled dead controls or route them into unrelated ski screens. Final scope still includes all M2–M4 flows.
- [x] Map HeroUI designs to reusable Compose/TrailsTheme components and Manrope/Inter assets, preserving brand colors and the original compass. Record native accessibility, insets, keyboard/back and platform icon adaptations.
- [x] Record the current runtime boundary: Android required; full iOS runtime proposed; desktop/Web undecided. Existing targets are retained.
- [ ] Select the final iOS, desktop and Web runtime acceptance scope and prove those hosts independently; do not silently drop existing targets.
- [x] Assign all 20 original rows and the added M1 reliability states to milestone owners; retain later required states in the inventory.
- [ ] Complete M2–M4 state/control specifications before their affected milestone, without inventing an exact-Mobbin access gate.
- [ ] Re-snapshot design revision R2 (September 18): the 28 frames, the Components · Messaging components, live token values and the button and messaging rules into the reference inventory, superseding the September 15 token/component rows without deleting their evidence (R2 plan, Task 1).
- [ ] Revise the completion contract, the M1 interaction contract and the deviation ledger for R2: five-root navigation, the Explore chip set and sort, extended filters, structured card facts, status line / toast / decision sheet copy, and the controls omitted from the M1 preview (R2 plan, Task 1).

**Exit for M1:** Reference snapshot and assets are reproducible; every enabled M1 control/state and data boundary is implementable; temporary omissions are explicit; later milestones remain recorded. **Exit for full design acceptance:** All inventory rows and required states have implementation and review evidence. A Figma frame/link is not proof of a working interaction.

**Completed M1 design evidence:** [Reference inventory](../../trails-reference-inventory.md), [interaction contract](../../trails-m1-design-contract.md), [deviation ledger](../../trails-reference-deviations.md), and [independent review](../../evidence/m1/design-review.md). The snapshot preserves 20 original frames, exports 11 M1 originals and 16 additive reliability states, and includes the original compass, generated landscape atlas, and licensed local fonts. All 45 asset/evidence hashes passed. Final platform selection remains open. Current Android behavior is tracked in the acceptance ledger; the static design review alone did not establish it.

## Task 4: Establish a reproducible Store6 consumer

**Owner:** Integration lead.

**Files:** gradle/libs.versions.toml, settings.gradle.kts, tooling/plugins/, affected module build.gradle.kts files; new docs/store6-integration.md.

Historical source research on September 14 (these statements describe the inspected revision; Task 4 must revalidate them):

- The local Store6 checkout currently has a Store5 branch checked out. Inspect immutable Store6 objects with git show; do not switch or modify that checkout.
- Inspected upstream/store6 revision: 582edfe86e64ddc71312ecd20a1895fc3de37b52. Local store6 revision: 7b190c14f6936ca53849a8aebcb215b5a280457e. Recheck live upstream authority before selecting the consumer pin.
- Inspected Store6 source uses Kotlin 2.3.20; Trails uses Kotlin 2.2.20. Compiler/plugin compatibility is a gate, not an assumed version-only change.
- Artifact names in source are under org.mobilenativefoundation.store and include core, sqldelight, mutations, mutations-sqldelight, mutations-conflicts, compose, and paging-androidx. Do not invent store6-prefixed coordinates. Core is stable-track but not frozen; the selected adapters and mutation APIs are experimental.
- The inspected source documentation described alpha01 as unpublished; artifact resolution was not verified then. A SNAPSHOT label alone is not a reproducible source identity.
- The inspected SQLDelight value and mutation adapters require synchronous drivers. Trails Web uses WebWorkerDriver; those cannot be assumed compatible.
- The inspected paging-androidx artifact excludes iosX64. Use iosArm64 and iosSimulatorArm64 for the proposed iOS release; either remove incompatible Intel target edges or choose another proven paging approach if Intel simulators are required.
- Background-drain/Meeseeks and devtools artifacts are outside the inspected alpha01 roster. They are not required dependencies for this plan.
- The installed Store6 documentation helper rejected the inspected revision with VERSION_MISMATCH. Matching checked-in documentation was inspected instead. Obtain a matching helper bundle or continue with exact-revision checked-in docs; do not suppress the mismatch or silently update the skill.

Live local source check on September 15: the Store6 directory is clean on Store5 `main` at `74a5988ff1c998055455b42509763e933a66da79`; local `store6` is `7b190c14f6936ca53849a8aebcb215b5a280457e`, and cached `upstream/store6` is `582edfe86e64ddc71312ecd20a1895fc3de37b52`. These are locally available refs, not a fresh remote/publication check. At the local Store6 ref, `VERSION_NAME=6.0.0-SNAPSHOT`; a catalog entry named `store` for Store5 is not the Store6 artifact version. Inspect immutable objects or isolated source checkouts; do not switch the shared working checkout.

The Atom checkout was actively changing on `matt-ramotar/atom-task4-optional-adapters` at `cf98ac7986c1774d08e1f9fd02b41a857fc40029`. Neither its dirty optional-adapter split nor that HEAD alone is the consumed candidate. Coordinate with its owner for an immutable repaired runtime/adapters revision and evidence; recheck status when implementation starts.

**Bounded proof files (13 JVM tests and Android integration executed):** `integration-tests/store6-consumer/build.gradle.kts`, `integration-tests/store6-consumer/src/commonMain/kotlin/org/mobilenativefoundation/trails/integration/Store6Consumer.kt`, and `integration-tests/store6-consumer/src/jvmTest/kotlin/org/mobilenativefoundation/trails/integration/Store6PersistenceTest.kt`. Include generated SQLDelight queries, real disk-backed value/journal adapters, a finite Atom command, and consuming Compose/Circuit/Metro wiring. Pure dependency resolution or independent library builds do not pass C3. Core, SQLDelight, mutations and persistent mutation storage are required for M1; prove conflicts/paging adapters when selected, without adding deferred background/devtools artifacts as prerequisites.

- [x] Verify upstream revision, module API tiers, and the actual published artifacts/variants available for the chosen release. Record exact coordinates, version, source SHA, repository, and resolution evidence.
- [x] Prefer published artifacts with proven required variants. If unavailable, use an explicitly documented immutable source checkout and verified composite substitution or isolated local artifact repository for development. A clean checkout and CI must reproduce it; no dependency on the developer's existing Maven local cache.
- [x] Treat publishing Store6 as separate work if a public dependency is required. Do not add that publication to the Trails execution scope implicitly.
- [x] Retrieve matching Store6 migration, read-contract, persistence, mutation, and lifecycle documentation. Record source/docs identity and behavioral limits.
- [x] Validate one joint Kotlin/Compose compiler/KSP/Metro/Circuit/Atom/Store6/SQLDelight/AGP/Gradle target tuple through unified gate C3. Use an actual combined consumer; independent library builds on incompatible tuples do not satisfy this gate. Select only required changes and keep initial Atom wiring manual.
- [x] Create the bounded joint consumer, run file-backed JVM persistence tests, and assemble/install its actual Android integration. M1 core/SQLDelight/journal and generated task names are recorded.
- [ ] Compile and verify retained Apple variants separately; prove paging adapters when selected for a later journey.
- [ ] If Web is required, prove a supported async persistence/journal solution before promising offline/restart parity. If desktop is required, include its launcher and driver in Task 9.
- [x] Record the selected dependency mechanism in docs/store6-integration.md and the build configuration.

**Current C3 evidence:** [Source/toolchain report](../../store6-integration.md) and [execution recipe](../../evidence/m1/dependencies/REPRODUCTION.md). Clean immutable Store6/Atom publications feed a hash-verified isolated repository. Thirteen actual SQLDelight consumer tests and 288 Atom revalidation tests pass. The Android dependency fixture consumed Circuit/Compose/Metro and preserved Offline pending work across force-stop and schema upgrade, then settled with one backend receipt/effect. Production pins now use that tuple. This passes the bounded JVM/Android prerequisite; the production M1 installed script and later platform/release gates remain separate.

**Exit:** A fresh checkout can resolve and compile the exact Store6 dependency on the release targets. If this gate is blocked, checkpoint work and UX design can continue, but migration and release claims remain blocked.

## Task 5: Prove platform capabilities and establish the reference shell

**Owners:** Platform lead and design-system owner, with disjoint files.

**Areas:** app/scaffold, foundation/designsystem, platform adapters as required by the inventory, and app/core host wiring. Proposed modules below are destinations for planning, not claims that these files already exist.

- [ ] Create the minimal selected-platform host, framework, and graph wiring needed to install and exercise capability and shell spikes. Reuse the Android host. For selected iOS runtime scope, add an apps/ios Xcode app and shared framework target, native graph initialization, app-context construction, and an actual Compose UIViewController entry point. The existing common MainViewController class is not itself a UIKit host. Complete production lifecycle and packaging acceptance in Task 9.
- [ ] For each required capability, identify an implementation compatible with the selected KMP targets, licensing/configuration needs, emulator constraints, and actual integration effort. Prove the riskiest paths with a small installed-app spike before promising parity.
- [ ] For the Figma Map journey in M2, prove map rendering, route geometry, markers/selection, camera-to-results behavior, and appropriate attribution. Record the provider/style choice and visible differences from the reference. Separate map presentation from trail-data fetching.
- [ ] For the Figma recording journey in M3, prove permission states, location acquisition, lifecycle transitions, pause/resume/finish behavior, and durable recording recovery. Define platform ownership of location sampling and recording storage; Store6 mutation journaling is not a high-frequency GPS recorder.
- [ ] If offline basemap downloads are subsequently selected in the contract, distinguish saved trail metadata, route geometry, and offline basemap coverage. Prove actual downloaded availability, progress, failure, cancellation, and storage behavior. A Store6 cache hit does not establish offline map coverage.
- [ ] Prove other integrations actually required by the Figma control inventory, such as sharing or external directions, through their real platform interfaces or explicitly designed sample service behavior.
- [ ] Build shared components and design tokens from Task 3's captured Figma design using TrailsTheme. Use the existing Hugeicons source where appropriate; record meaningful icon substitutions instead of asserting an exact match.
- [ ] Replace the existing ski-feed navigation shell with the R2 floating pill navigation (Explore, For You, Navigate, Saved, Activity; Profile from the avatar). Keep destination API/impl separation and Circuit registration; place reusable sheets/overlays under feat and platform services under suitable foundation/API boundaries.
- [ ] Exercise safe areas, Android system back, iOS dismissal, keyboard, selected tabs, and modal return state in the installed shell. Add no placeholder destination that appears complete but has dead controls.

**Exit:** Required platform capabilities have working evidence or a concrete unresolved blocker, and the shell/design system can support faithful reference journeys. A material capability gap must be reflected in scope and completion status.

**M1 progress:** TrailsTheme tokens, bundled assets, the original compass, and the functional Explore/Saved shell are implemented and installed against the September 15 design. The R2 alignment plan (Tasks 2 and 12) moves the tokens to R2 and restyles the two-root M1 shell as the floating pill. The full five-root shell, map/location capabilities and other hosts in this task remain open.

## Task 6: Build the trail domain, persisted reads, and account lifecycle

**Owner:** Data lead, with shared schema/DI integration serialized by the integration lead.

**Areas:** New API/impl repositories for the hiking domain, model/domain, model/network, model/db, server/api and fake services, account graph lifetimes, bootstrap. M1 proposes data/trail and data/saved; M3 adds data/activity. Confirm complete milestone file maps and ownership before creating them.

- [ ] Model stable trail identity and observed attributes, route geometry, query results, and account-owned records. Keep public trail facts separate from viewer-specific saved/list membership, activity, review, or profile state. Do not force the new domain into FeedPost or ski-specific RunRecord fields.
- [ ] Define query identity from actual result selectors: normalized search, filters, sort, paging cursor, and map bounds where those determine results. Persist ordered membership separately from entity records. Keep transient camera movement, filter drafts, and local UI selection out of durable domain state unless restoration requires them.
- [ ] Provide deterministic, representative hiking fixtures and fake service responses whose names, units, counts, images, route geometry, and relationships remain consistent across screens. Preserve useful seeded latency/error/conflict controls from v2.5; distinguish settings replay from full scenario reset.
- [ ] Build the new Store6 read/write slice alongside the old app until it passes, then switch its repository and UI bindings together. Do not run Store5 and Store6 as concurrent writers of the same cache or ship a migration that breaks corresponding actions.
- [ ] Keep repository APIs owned by Trails. Map Store6 Loading, Data, Error, and Revalidated behavior into the Task 3 presentation contract, preserving content during refresh and separating a valid empty result from a missing cache.
- [ ] Use canonical entity keys and persisted query/page membership. Scope account-dependent keys and data to the account and selectors, never credentials. Public cache reuse must not expose private membership or overwrite pending account state.
- [ ] Adapt SQLDelight deliberately: the inspected row adapter uses a Query<V> read as one row; a multi-row result query cannot be plugged into it unchanged. Persist query/page membership as one row or use a documented custom source of truth with behavioral tests.
- [ ] Persist freshness/validator metadata with values according to the selected adapter contract. Install the value source of truth and SqlDelightBookkeeper with the same driver/transacter. Define refresh with explicit invalidation or the selected policy; MustBeFresh does not force an unconditional fetch.
- [ ] Specify an existing-v2.5 database upgrade path. Preserve session and developer settings where compatible; deliberately retire obsolete ski/post caches. Add numbered migrations for actual schema changes; the current seed-scenario alias rename alone does not change the physical column.
- [ ] Migrate server user/profile reads to Store6 while keeping local session/bootstrap state distinct. The existing user updater reports success without a remote write; do not reproduce a fictitious acknowledgement. Handle the fake backend's unauthorized type directly rather than relying only on obsolete HTTP-client exceptions.
- [ ] Own stores, collectors, and journals at deliberate account lifetimes. Sign-out cancels old collection/draining, retires the old scope, clears or isolates data, and prevents late responses from entering another account.
- [ ] For search, filter, and map queries, cancel or ignore stale requests by query identity. A slow earlier search must not replace newer results; selection and camera/list relationships must use stable trail IDs.
- [ ] Test cold/warm/empty/error reads, query isolation, stale refresh, page membership, account projections, cache/database restoration, unauthorized refresh, and sign-out during a fetch. Use reopened file-backed value and journal databases for restart tests; the existing JVM in-memory driver cannot prove durability.
- [ ] Retire obsolete post store/repository/model/schema usage after replacement journeys pass. Do not migrate PostStore only to discard it; preserve the verified v2.5 history and remove abandoned code through explicit follow-up commits.

**Exit:** The first complete reference journey uses Store6-backed trail/account data with truthful state and an intentional database upgrade path. Later journeys reuse these contracts and add only the domain capabilities they need.

**M1 progress:** `data/trail` implements canonical hiking facts, persisted ordered query membership, account-owned saved projection, file-backed drivers and guarded retirement. The upgraded main installation preserves its existing session/settings in the legacy database and adds separate M1 databases. M1 data/lifetime tests and installed restart/query/account evidence pass. Geometry, paging, remote profile/user migration and obsolete Store5 removal remain later work; the broad checklist above retains those requirements.

## Task 7: Implement mutations, conflict handling, and durable recovery

**Owners:** Data lead and backend owner with agreed service contracts.

**Areas:** New typed mutations in the relevant data implementation modules, server/api and fake services, mutation journal schema/driver wiring, account lifecycle, and developer drawer diagnostics.

- [ ] Derive mutations from the approved journeys: desired saved/list membership, enabled collection edits, profile/preferences and completed-activity submission. Review authoring and ski-feed like/follow commands are not implied; add such operations only if the current contract explicitly includes them.
- [ ] Use desired state rather than blind toggles for repeatable changes. Define entity/key resolution, a MutatorRegistry, versioned durable payload codecs, and the selected mutationStore APIs from matching Store6 documentation.
- [ ] Implement MutationServer push/retire and acknowledgement semantics, stable operation identity, and idempotent backend application. Duplicate delivery must not double counts, create duplicate lists/activities/reviews, or erase a later user intent.
- [ ] Reflect one logical change in all relevant search/detail/saved/profile projections. Refreshing public trail facts must not overwrite account-owned membership or a pending overlay; test both refresh orders.
- [ ] Use persistent Store6 mutation storage and expose real pending, success, retry, and conflict state. Observe stream for optimistic values; get exposes committed truth only. Derive pending feedback from overlay origin and stale feedback from staleness, never from one interchangeable “sync” flag.
- [ ] Respect SQLDelight driver ownership: the inspected journal has a separate gate from value/bookkeeper storage. Use a dedicated account-scoped journal driver/database unless all shared-driver access is explicitly serialized and tested.
- [ ] Define app-owned foreground/startup/reconnect draining. Enqueueing a mutation does not push it; one drain pass neither retries/backoffs nor fetches. Implement bounded retry timing, cancellation, concurrency ownership, and required refresh explicitly. Deferred background-drain/Meeseeks modules are not prerequisites, and OS background completion is not promised without platform proof.
- [ ] Apply persisted developer Offline settings before the first fetch/drain on cold start; use the drawer applied-state signal in the demo, since airplane mode does not gate in-process fake services. Preserve fake-server authority across client restarts. Persist demo server/account state and applied-operation receipts, or use a separately running deterministic test backend. Client restart must not silently reset the server during durability demonstrations.
- [ ] Test acknowledgement windows precisely: server acceptance before durable acknowledgement may redeliver the same operation ID; durable ACKED recovery must finish local adoption without pushing it again. Inject failures around journal persistence, server acceptance, acknowledgement, and adoption.
- [ ] Define coherent reset behavior across backend, account/session, value cache, journal, downloads/recording where applicable, and UI. Clearly distinguish settings replay from full-demo reset.
- [ ] Implement selected conflict policies with verified final values and truthful user feedback. Keep retryable pending work distinct from terminal/parked failures. The inspected alpha exposes pendingWrites/deadLetters inspection but no discard/requeue API for parked intents: do not invent recovery buttons the integration cannot perform.
- [ ] Test offline enqueue, reconnect, retry limits, duplicate delivery, rapid save/unsave or membership changes, conflicts, process restart, sign-out with pending work, and cross-account isolation. Any terminal recovery through application migration or full-demo reset must be deliberate and documented.
- [ ] Switch each complete read/write slice's production DI bindings together. Remove the remaining Store5 classes, dependencies, and imports after the new trail and user paths replace them. Inspect resolved runtime dependencies as well as source.

**Exit:** The design's applicable actions respond immediately, survive restart as promised, settle without duplicate side effects, and communicate actual outcomes. Store6 owns their real read and mutation lifecycle.

**M1 progress:** Desired collection sets, persistent admission/journal records, stable operation identities, idempotent persistent fake authority, account-owned draining, startup Offline ordering and failure-window recovery are implemented. Installed save/unsave/save, lost acknowledgement, ACKED adoption and account isolation pass. No full-demo reset is enabled in M1. Profile/activity commands, selected conflict-policy expansion and final legacy removal remain open.

## Task 8: Deliver M1, then complete the remaining Figma journeys

**Owners:** Disjoint screen/feature owners with serialized schema/DI/build integration. **Dependencies:** M1 contract from Task 3, joint consumer/repaired-Atom acceptance from Task 4 and the unified plan, and the needed Task 5 shell. Tasks 6–7 execute with this task, per journey.

### M1 implementation units and file ownership

The actual M1 implementation map below replaces the original proposed file map. Data types and repositories share the Trails-owned `data/trail` API; the finite Atom sits within its reusable save feature. Screens and features retain API/implementation separation, Android namespaces and explicit settings registration.

| Unit | Implemented paths | Proof |
| --- | --- | --- |
| Models/repository contracts | `multiplatform/data/trail/api/.../TrailData.kt` | Stable IDs, normalized selectors, saved snapshot and desired-set command contracts |
| Trail reads | `multiplatform/data/trail/impl/.../TrailCatalog.kt`, `RealTrailDataFactory.kt` | Canonical Store6 values, persisted ordered membership and file-backed query/restart tests |
| Saved account/recovery | `multiplatform/data/trail/impl/.../RealTrailAccount.kt`, `SavedStorage.kt`, `StorageLifetime.kt`, `AccountRecoveryPolicy.kt` | 18 data/lifetime tests plus installed fault-window/account checks |
| SQL and drivers | `multiplatform/data/trail/impl/src/commonMain/sqldelight/.../M1.sq`, platform `PlatformM1DriverFactory` files | Separate value/journal storage, persistent server receipts; Android/JVM proven, other hosts not claimed |
| Fake trail authority | `multiplatform/data/trail/impl/.../M1Backend.kt` | Durable server state and receipts; mapped existing developer network controls |
| Screens | `multiplatform/screen/{explore,traildetail,saved,collection}/{api,impl}` | Real record routing and shared saved projections through Circuit |
| Reusable sheets | `multiplatform/feat/{filters,savetrail}/{api,impl}` | Draft/apply/cancel, native sliders, finite save admission/error flow |
| Atom | `multiplatform/feat/savetrail/impl/.../SaveTrailAtom.kt`, `src/jvmTest/.../SaveTrailAtomTest.kt` | 7 finite-flow tests; no Atom durable mutation queue |
| Integration/navigation | Existing app/bootstrap and DI graphs; `M1Content.kt`, `M1NavigationController.kt`, `M1NavigationCheckpoint.kt`, Android navigation storage | 6 navigation, 2 bootstrap and 14 app-core tests; installed Back/query/scroll recovery |
| Settings and acceptance | Developer settings/synchronizer, Android debug receiver, `apps/android/src/androidTest`, `scripts/m1-*.py` | Applied Offline, explicit faults, native accessibility actions and preserved process/database evidence |

`...` within a module's Kotlin path denotes `src/commonMain/kotlin/org/mobilenativefoundation/trails/<module package>` unless a platform/test source set is named. The [changed-file inventory](../../evidence/m1/changed-files.md) and JSON list record complete paths. No parallel Store5/Store6 writer shares an M1 cache. The old unused generated PostAtom module was retired; legacy session/profile migration remains a later requirement.

### M1 execution order

- [x] **8a — Fixture and contracts:** Add stable hiking fixtures, normalized query selectors and account-owned membership models. Provide a documented debug launch/test preset for fake-service Offline before bootstrap on clean app data. First test query identity, real counts, per-trail detail routing and desired-state membership semantics. Ensure enabled collection controls have complete behavior; keep later controls out of the M1 preview per the discrepancy ledger.
- [x] **8b — Persisted reads:** Implement the Trail repository/Store6 source of truth and ordered result membership. Test cold/warm/empty/error, stale refresh and late-result suppression using real adapters. Close/reopen file-backed storage and verify the same data returns.
- [x] **8c — Durable membership:** Implement the Saved repository, versioned mutation payload/key resolver, persistent journal, idempotent backend receipts and account drainer. Test local admission, rollback/failure visibility, rapid save/unsave/save, duplicate pushes, durable ACKED adoption and restored pending work before binding UI. Keep public facts separate from membership overlays.
- [x] **8d — Compose and transient state:** Build the M1 Figma components/screens/sheets and save-flow Atom using finite commands. Test draft dismissal, admission failure, query/back/selection restoration and account retirement. Surface real repository state; do not use fixed success delays or hardcoded counts.
- [x] **8e — Integrate and install:** Wire the completed read/write slice and the functional M1 preview entry together. Run targeted repository/presenter/Atom tests, app JVM compilation and Android assembly. Install the newly built APK and execute every step of the [M1 acceptance script](../../trails-completion-contract.md#m1-acceptance-script), including drawer-controlled Offline and force-stop without data clearing.
- [x] **8f — Review and record M1 evidence:** Capture paired Figma/app images and flow evidence, close material M1 issues, and record the source/APK/dependency identities. M1 is accepted separately from full completion; M2–M4 remain future execution.
- [ ] **Optional commit checkpoint:** Create coherent implementation commits when requested. The existing `86209d7` and `2a41dd5` commits remain intact; this execution leaves the verified changes uncommitted.
- [x] **8g — Design revision R2 alignment:** After the commit checkpoint, execute [the R2 alignment plan](2026-09-18-trails-design-revision-alignment.md), rebased September 18 onto the world-trail catalog (`481346e`) and the bundled photography (`6435f57`): contract updates, R2 tokens and heading sizes, the button hierarchy, difficulty markers and the Strenuous fixture, the R2 trail card, the extended query model and filter sheet, the Explore header and sort, the R2 trail detail, toast / status line / decision layout messaging with the R2 copy table, the Saved tiles, the floating navigation, then rerun acceptance cells 1, 3, 5 and 9 on the new APK and record paired R2 captures under `docs/evidence/r2`. Done: the eleven-target JVM gate passed with 88 executed tests, the APK `726b3753…` was installed on `Trails_Preview_API_35`, the catalog reseeded from seed version 2 to 4 with the eight Strenuous treks and every saved membership intact, and cells 1, 3, 5 and 9 were rerun — [R2 evidence](../../evidence/r2/README.md). Cells 2, 4, 6, 7 and 8 keep their M1 APK attribution and TalkBack traversal remains open.
- [x] **8h — Remaining roots preview (For You, Navigate, Activity):** Completed [the remaining roots plan](2026-09-20-trails-remaining-roots.md) from its [design](../specs/2026-09-20-trails-remaining-roots-design.md): five roots with compatible checkpoints, seeded per-account feeds and a schematic Navigate preview. The map provider (M2) and recorder (M3) remain future milestones. [Measured verification](../../evidence/roots/README.md) records 137 passing JVM tests across 13 modules (fresh whole gate plus a clean replacement Activity gate after a test-only logging correction), the preserved R2 checkpoint upgrade, root/detail restarts, totals, saves and cached Offline feeds. All three roots passed installed 200% navigation checks on the attributed corrected build. The authorized follow-up exercised the genuine cached Activity Offline Try again action without losing content or showing a failure line, and verified genuine online failure/recovery. The final production APK is independently device-hashed; original settings, memberships, fixtures and seed4 are preserved. Task8 spec-plus-quality review approved; whole-branch review is the final execution gate.

For each behavior change, write the meaningful failing test, capture its failure, implement the smallest complete behavior, run the affected checks and commit the verified unit. Preserve the first failure; rerun after a fix, not unchanged until green. Do not add tests that only mirror harmless visual implementation details.

Expected new task paths, **only after the proposed projects exist**:

~~~bash
./gradlew :multiplatform:data:trail:impl:jvmTest :multiplatform:data:saved:impl:jvmTest
./gradlew :multiplatform:screen:explore:impl:jvmTest :multiplatform:screen:traildetail:impl:jvmTest :multiplatform:screen:saved:impl:jvmTest
./gradlew :multiplatform:atom:jvmTest
./gradlew :multiplatform:app:core:compileKotlinJvm :apps:android:assembleDebug
./gradlew :apps:android:installDebug
~~~

Record actual generated task names for the pinned tuple, journal fixture and Android instrumentation runner before invoking them. Expected results are passing contract tests, successful compilation/assembly, and the observed installed M1 behavior; none is marked executed by this plan. For M2–M4, repeat the unit/test/commit sequence with complete milestone file and state maps before implementation.

R2 elements that the M1 alignment omits are owned by later milestones; each needs its state/control contract before implementation:

| R2 element | Frame | Milestone | Prerequisite |
| --- | --- | --- | --- |
| For You root: feature card, Based on your activity rows, heart saves | F02 `148:55` | M2 | Persisted recommendation fixture and the Moments content decision |
| Map controls (layers, 3D, compass, my location, scale bar), Map pill on Explore, mini route thumbnails on cards | F01, F04 `148:61` | M2 | Task 5 map provider, geometry and location proof |
| Download map and draw-route controls, Downloads tab | F04, F23 `236:2210` | Excluded | Explicit contract change (offline maps, custom routes) |
| Trail detail share and more circles | F03 `148:58` | M2 | Task 5 platform share proof; the more menu needs a defined action list |
| On-trail directions, expand map, Get directions, Preview / Photo tour cards | F26 `264:3418` | M2 | Route geometry, external directions integration, media assets |
| Customize route | F26 | Excluded | Explicit contract change |
| Top sights list and Sight detail | F26, F28 `263:3756` | M2 | POI fixture with photos, distance from trailhead and elevation |
| Distance away filter | F27 `264:3966` | M2 | Location fix |
| Navigate root, Start recording | F25 `236:2258` | M3 | Recorder contract |
| Activity root | F07 `148:71` | M3 | Completed-activity submission |
| Saved quick actions (+, search, avatar), Recommended lists, Invites tab | F06 `148:68`, F24 `236:2234` | M4 (Invites needs a sharing contract) | Collection management, curated public lists, profile routing |
| Profile from the avatar | F08 `148:74` | M4 | Account/profile persistence |

- [ ] Assign every inventory row to a slice, including flows not covered by the milestone table in the completion contract. Adjust sequencing to dependencies without removing source coverage.
- [ ] Use real cursor paging where required by observed collections/results. Persist membership, deduplicate by stable ID, preserve scroll/selection, and provide append retry and terminal states. Verify filtering, paging, refresh, and map selection together.
- [ ] Keep trail metadata, units, route shape, saved state, and account identity consistent from discovery through detail and saved/profile surfaces. Prevent screens from maintaining competing hardcoded copies.
- [ ] Complete every visible control, including reference sheets/dialogs and secondary actions. Map and recording controls require their scoped models/services/platform adapters. Sharing or other added controls require corresponding contracts; no silent TODO handlers or fabricated progress.
- [x] Deliver the M1 save-flow Atom+Store6 feature under unified Task 6. Store6 and account-owned services manage durable mutations, draining, and recovery; Atom owns the finite transient save flow. File-backed tests and installed Offline/restart/reconnect evidence establish this bounded Trails result; the broader shared Sower/value gate remains separate.
- [ ] For later journeys, use a Presenter for simple state and Atom where its transient feature/lifecycle contracts add value. Atom must not own persistent effects, await remote mutation settlement or collect an infinite repository stream inside its sequential interpreter. Keep each module's Intent/Event pattern internally consistent.
- [ ] Register navigation destinations through Circuit with API/impl modules. Inject reusable feature triggers for cross-screen sheets and overlays; retain TrailsTheme throughout.
- [ ] Compare each normal state with its captured Figma state at matched content and dimensions. Exercise the corresponding interaction path, not only isolated previews. Record discrepancies and close them or identify them explicitly in the deviation ledger.
- [ ] Exercise the slice against the real Store6 layer and fake services under latency, offline, error, and conflict conditions relevant to its behavior.

**Exit:** Every screen/flow in the approved design inventory has an implemented path and evidence, or an explicit unresolved deviation that keeps Trails from being declared complete. New states added for reliability and accessibility are coherent with the reference.


## Task 9: Complete platform hosts, fidelity review, and device polish

**Owners:** Platform integration lead and UX lead.

**Android:** apps/android and app/core Android implementation. Reuse the working Trails_Preview_API_35 AVD for repeatable preview; retain the current-app capture as a before image.

**Proposed iOS runtime:** Complete the minimal apps/ios host, shared framework, native graph wiring, and Compose UIViewController entry point established in Task 5. Finish bootstrap, lifecycle, persistence, packaging, and installed-app acceptance under the platform contract from Task 3. The existing common MainViewController class is not itself a UIKit host and cannot substitute for that entry point.

**Desktop/Web if selected:** Add launchers and graph creation, driver initialization, lifecycle, navigation/input, and the persistence solution proved in Task 4.

- [ ] Build and launch every promised platform. Verify bootstrap, lifecycle cancellation/resumption, keyboard/back handling, account changes, and persistence with the actual Store6 integration.
- [ ] Capture paired source/Trails screenshots for every inventory screen and material state at comparable dimensions, scale, content, and scroll position. Review composition, typography, spacing, colors, icons, imagery/crop, overlays, and system insets; investigate image-diff findings rather than treating a numeric score as sufficient.
- [ ] Capture short interaction evidence for each flow: taps, navigation, sheets, transitions, gesture behavior, and resulting data. Still images cannot establish interaction parity.
- [ ] Keep a discrepancy log linked to source and implementation captures. Fix material visual/behavioral differences or record reviewed deviations with their reasons; record substantive changes to the approved Figma design instead of silently dropping its behavior.
- [ ] Verify small and large phones, landscape where supported, safe areas, keyboard, and enlarged text without clipped controls or unreadable overlays. Accessibility adaptations may differ from static reference captures and must preserve usable behavior.
- [ ] Make interactive targets accessible and provide meaningful toggle, selection, progress, permission, and status semantics. Verify TalkBack and, for iOS scope, VoiceOver traversal and labels.
- [ ] Respect reduced motion. Preserve responsive scrolling and stable content through refresh/append; animation must not obscure errors, pending actions, or recording state.
- [ ] Test required map/location/download/recording behavior on devices as well as emulator/simulator scenarios where their limitations matter. Record what was exercised, especially background/lifecycle claims.
- [ ] Verify developer-drawer discovery/dismissal with touch, supported keyboard input, and screen readers. Its shortcuts must not hijack search, forms, or normal gestures.
- [ ] Record representative OS/device versions, layout sizes, visual baselines, startup and frame-time measurements. Investigate observed jank without inventing performance guarantees.

**Exit:** Installed apps pass the reference, state, and interaction contracts with paired visual evidence. Outstanding material deviations, runtime platforms, or capability gaps remain visible in completion status.

## Task 10: CI, sample documentation, and final review

**Files:** Create `.github/workflows/verify.yml`, `docs/store6-integration.md`, `docs/trails-reference-inventory.md`, `docs/trails-reference-deviations.md`, and `docs/trails-demo.md` as their implementation stages require; update `README.md` and the existing `docs/trails-completion-contract.md`.

- [ ] Add hosted backend/repository tests, Android build/tests, and Apple build/simulator checks for the actual supported runtime targets. Use the exact PR commit and archive reports.
- [ ] Replace README's license-only content with prerequisites, dependency provenance, reproducible launch instructions, runtime platform support, Figma design source, and the intentional fake-backend/sample service scope.
- [ ] Explain Store6 reads, persistence, mutation, conflict, paging, and account lifetimes with links to actual source. State selected API tiers and limitations. Distinguish Store6 persistence from offline map and recording capabilities.
- [ ] Document deterministic reference scenarios so another developer can reproduce comparison captures and core journeys. Use durable fixture/configuration identity; settings and RNG replay alone are not full scenario replay.
- [ ] Write and execute a short Store6 demonstration through an actual reference journey: find/open a trail -> disconnect -> save/change membership -> restart -> reconnect -> observe settlement -> inject a relevant conflict -> recover. Adapt to confirmed actions without inventing product controls solely for the demo.
- [ ] Execute the matrix below, recording revision, command, outcome, source/implementation evidence, and actual coverage. Local compilation, behavioral tests, device UX, and hosted checks are separate evidence.
- [ ] Reconcile every reference inventory row and every deviation. Review for dead handlers, false statuses, inconsistent domain values, abandoned ski-feed UI, leftover Store5 imports/runtime dependencies, and unintended local artifacts.
- [ ] Prepare coherent reviewable commits, the PR description, and comparison images. Preserve the initial v2.5 checkpoint separately from design/domain/Store6 work. Push/open/merge/release actions follow the user's execution authorization; this plan itself performs none.

**Exit:** Another developer can clone, resolve the exact Store6 dependency, launch Trails, and reproduce its advertised reference journeys. The agreed platform contract, all reference coverage, and UX/data acceptance gates pass on the reviewed tree.

## 4. Release acceptance matrix

The Figma inventory and completion contract determine required coverage. Maps and recording are required in M2/M3; offline basemap downloads require separate selection. Unknown or unimplemented required states are not passing results.

| Scenario | Required observable result |
| --- | --- |
| Reference screen coverage | Every screen/variant has paired source and Trails evidence; material differences are resolved or explicitly recorded |
| Reference flow coverage | Every flow has the expected navigation, intermediate states, controls, and functional result; no silently missing flows |
| Design revision R2 alignment | M1 surfaces match the September 18 frames at matched content and size: R2 tokens, button fills by rule, structured card facts with markers, the R2 filter set, status line / toast messaging; omitted controls appear only in the deviation ledger |
| Fresh install online | Truthful sample entry and any required onboarding/permission route into populated discovery |
| Fresh install offline | Useful offline/error state and retry; no false empty collection or implied downloaded content |
| Warm start offline | Applied fake-backend Offline state and account restore before fetch/drain; cached trail/account content appears with honest availability; navigation and selection restore as promised |
| Search/filter/map query changes | Latest query controls results; stable IDs connect list, marker, detail, and return state where applicable |
| Refresh fails | Existing content remains; progress stops and recovery feedback is usable |
| Save/list change offline | Immediate intended state and honest pending feedback across applicable screens |
| Reconnect/retry/duplicate delivery | One logical effect; no duplicate records/counts or lost later intent |
| Restart while pending | Durable work and documented server state recover; acknowledgement/adoption windows behave correctly |
| Conflict preset | Final data and user message agree with the selected policy |
| Paging/refresh overlap | Stable IDs and query membership, no duplicate/missing results, append retry and terminal state |
| Maps; downloads only if selected | Displayed route/selection is correct; claimed offline coverage, progress, failure and cancellation reflect actual availability |
| Recording | Permission, pause/resume/finish, lifecycle and recovery behavior matches the agreed contract; completed activity is durable |
| Provider/sample adaptations | Authentication, entitlement, sharing or other adapted flows do what their visible copy promises and appear in the deviation ledger |
| Sign-out/account switch during work | Old account data and mutations never appear under the new account |
| Repeated taps/navigation/lifecycle | No duplicate root stack, stuck indicators, accidental resets, or lost selection |
| Accessibility/layout | Controls remain reachable and meaningful with larger text, screen readers, safe areas, keyboard and reduced motion |
| Clean checkout | Exact dependencies resolve from documented sources; reference scenario and launch instructions work |

Run behavioral repository tests on JVM and persistence/lifecycle tests on the selected mobile targets. Installed-app journeys must exercise Store6 with the fake backend; mocked UI state alone is insufficient.

Known checkpoint tasks remain:

~~~bash
./gradlew :server:fake:jvmTest
./gradlew :multiplatform:data:devsettings:impl:jvmTest :multiplatform:data:post:impl:jvmTest
./gradlew :multiplatform:app:core:compileKotlinJvm :apps:android:assembleDebug
./gradlew :apps:android:installDebug
~~~

During implementation, add and record the exact test/compile tasks for new trail, saved, activity, and screen modules actually created. Retire obsolete post/home tasks when those modules are removed; do not claim currently nonexistent tasks pass. Once iOS hosting exists, record exact framework, Xcode project, scheme, simulator destination, build, and test commands. Add connected Android tests when their test source set and runner are present.

## 5. Ownership and evidence rules

- One integration lead owns settings.gradle.kts, the version catalog/toolchain, shared DB schema, DI integration, git staging/commits, and the Gradle execution lease.
- One design-system owner maintains shared tokens/components and reference mappings. Screen workers own disjoint slices; backend/data/platform owners agree contracts before parallel implementation.
- Keep the reference inventory and deviation ledger current through implementation. An unspecified required state is a design task to resolve before its affected slice, not permission to omit its behavior or wait for an exact Mobbin capture.
- Use one Gradle runner. Preserve the first failure's logs/XML before fixes and subsequent runs; do not rerun unchanged failures just to obtain green.
- A wrapper or sandbox failure before startup means zero tests executed. Do not bypass it through cache or lock manipulation.
- Record local compile, executed tests, installed-device UX, reference comparisons, and hosted checks separately. No percentage-complete estimate substitutes for the acceptance matrix.
- Do not mark Trails finished while a material reference deviation, promised platform, durable behavior, visible control, or dependency reproducibility gate remains unresolved.

## 6. Source anchors

- [Current Trails completion contract](../../trails-completion-contract.md) — user decisions, live Figma inventory, milestone boundaries and M1 acceptance.
- [Trails Figma implementation reference](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=153-97).
- [Original compass production master](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=204-1685).
- [Components · Messaging page](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=248-2777) — R2 toast, status line, status indicator and decision sheet components.
- [R2 alignment plan](2026-09-18-trails-design-revision-alignment.md) and [R2 design spec](../specs/2026-09-18-alltrails-redesign-second-pass-design.md).

- [Original AllTrails inspiration collection](https://mobbin.com/apps/all-trails-ios-66ac61f7-f9ea-4c45-b512-b2fadf7e027b/caf6da24-bc01-47b9-a571-0f4db9d55dfe/screens) — historical inspiration; exact collection coverage is not a completion gate.
- [Inspected Mobbin design references](/Users/matt/src/mobilenativefoundation/Trails/docs/trails-design-inspiration.md) — screen observations and saving/search flow previews, with explicit evidence limits.
- [Current Store5 feed construction](/Users/matt/src/mobilenativefoundation/Trails/multiplatform/data/post/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/post/PostStoreFactory.kt:36)
- [Current user store](/Users/matt/src/mobilenativefoundation/Trails/multiplatform/data/user/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/user/UserStoreFactory.kt:66)
- [Checkpoint feed-filtering implementation](/Users/matt/src/mobilenativefoundation/Trails/server/fake/src/commonMain/kotlin/org/mobilenativefoundation/trails/server/fake/services/FakeFeedService.kt:44)
- [Persisted conflict-setting translation](/Users/matt/src/mobilenativefoundation/Trails/multiplatform/data/devsettings/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/devsettings/RealDeveloperSettingsRepository.kt:131)
- [Unrendered navigation](/Users/matt/src/mobilenativefoundation/Trails/multiplatform/app/scaffold/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/app/scaffold/RealTrailsScaffold.kt:47)
- [Welcome's shared demo-account handler](/Users/matt/src/mobilenativefoundation/Trails/multiplatform/screen/welcome/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/welcome/WelcomePresenter.kt:26)
- [Profile mock state and dead actions](/Users/matt/src/mobilenativefoundation/Trails/multiplatform/screen/profile/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/profile/ProfilePresenter.kt:22)
- [Native startup placeholder](/Users/matt/src/mobilenativefoundation/Trails/multiplatform/app/core/src/nativeMain/kotlin/org/mobilenativefoundation/trails/app/TrailsApp.native.kt:5)

Historical Store6 source statements above were checked against immutable Git objects in /Users/matt/src/matt-ramotar/Store6, not its currently checked-out Store5 files. Revalidate revision authority and publication at Task 4.

- [Inspected Store6 API tiers and release roster](https://github.com/MobileNativeFoundation/Store/blob/582edfe86e64ddc71312ecd20a1895fc3de37b52/STABILITY.md)
- [Inspected Store6 platform and verification matrix](https://github.com/MobileNativeFoundation/Store/blob/582edfe86e64ddc71312ecd20a1895fc3de37b52/docs/store6/platforms.md)
- [Inspected Store5-to-Store6 mutation contract](https://github.com/MobileNativeFoundation/Store/blob/582edfe86e64ddc71312ecd20a1895fc3de37b52/plugins/store/skills/migrating-to-store6/references/mutations.md)
