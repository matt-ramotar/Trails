# Trails Remaining Roots Preview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the floating pill navigation its three missing roots — Navigate (F25 route preview with a coming-soon recording action), Activity (F07 history and monthly totals from seeded sample activities) and For You (F02 feature card and recommendation rows from a seeded fixture) — with their own back stacks and checkpoints, without a map provider or a recorder.

**Architecture:** Models and repositories stay in `data/trail` (API additions with defaults; the fake backend seeds per-account fixtures into its unused `cache_row` table; each account caches both feeds through a small Store6 read on its value database). `M1NavigationController` grows to five roots with backward-compatible checkpoints. Three new Circuit screen modules (`screen/navigate`, `screen/activity`, `screen/foryou`) use presenters and the R2 design-system components. `M1Content` renders all five destinations.

**Tech Stack:** Kotlin Multiplatform 2.2.20, Compose Multiplatform 1.9.0 (Material 3), Circuit 0.30.0, Metro 0.6.7, SQLDelight 2.1.0, kotlinx.serialization, kotlinx-datetime 0.7.1 (already in the version catalog), the pinned Store6/repaired-Atom tuple (unchanged), desktop Compose UI tests.

## Global Constraints

- Design: spec `docs/superpowers/specs/2026-09-20-trails-remaining-roots-design.md`; Figma frames F02 `148:55`, F07 `148:71`, F25 `236:2258` in file `4B7GK9ndPVQ1BFIKGg0Zqj`. R2 tokens, button rule and messaging rule apply exactly as in `docs/superpowers/plans/2026-09-18-trails-design-revision-alignment.md` (citron = the single hero action, dark = commits and empty-state primaries, soft = secondary; one-line status lines beside their subject; the dark toast above the navigation; no titled cards with nested buttons; no hard-coded colours outside the token file and shadow tints).
- Copy (verbatim): `Ready when you are`; `Choose a trail or record your own route.`; `Start recording`; `Recording is coming soon`; `Route preview · schematic`; `Finding your trail…`; `Activity`; `A little progress. A lot of fresh air.`; `<MONTH> SO FAR`; `km walked` / `trail` / `trails` / `outside`; `Your recent adventures`; `Your hikes will show up here`; `Find a trail worth walking and it’ll be waiting for you here afterwards.`; `Explore trails`; `Offline · Activity isn’t on this device yet`; `Couldn’t load your activity`; `Couldn’t refresh · Showing this device’s copy`; `For you`; `Based on your activity`; `More like <anchor>`; `Offline · Picks aren’t on this device yet`; `Couldn’t load your picks`; `Trail details aren’t on this device yet`; `Not on this device yet`; `Couldn’t load this trail`; `Try again`; `Today`; `Yesterday`.
- No map provider, no location permission, no recorder, no activity-detail screen, no avatar, no personalization logic. Everything omitted is a ledger row (DEV-31…DEV-37), never a dead control.
- Backward compatibility: every new checkpoint field has a default; version-1 checkpoints written before this change decode; no SQL schema change (installed builds have no migration path); Store6 read and mutation paths of the save flow are untouched. Owner ruling during execution: use Task 2's code exactly, with required members for the new `TrailAccount` feeds and required fields in `CompletedActivity` and `ForYouFeed`; the defaults requirement is relaxed for those additions.
- Keep the API/impl module split, Metro `@Inject`, Circuit `Ui`/`Presenter`, `TrailsTheme` accessors. Tests use `kotlin.test` on JVM; desktop Compose UI tests need `implementation(compose.desktop.uiTestJUnit4)` and `runtimeOnly(compose.desktop.currentOs)` in the module's `jvmTest` dependencies. On this Compose version use `waitUntil(timeoutMillis = …)`, `performScrollTo()` before acting below the fold, and `assertExists()`/`assertDoesNotExist()` as member calls (not imports).
- Preserve the first failure (console log or XML) of every red step before fixing it; never rerun an unchanged failure to obtain green. One Gradle runner at a time.
- Commit messages end with `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`.

## Prerequisite

`main` holds the R2 alignment (`353d916`) and the working tree is clean apart from the untracked `docs/evidence/m1/` and `redesign/` folders:

```bash
git status --short | grep -v '^?? docs/evidence/m1/' | grep -v '^?? redesign/' | wc -l
```

Expected: `0`. Create the working branch first: `git checkout -b matt-ramotar/remaining-roots`.

## File Structure

| File | Responsibility |
| --- | --- |
| `docs/trails-completion-contract.md`, `docs/trails-m1-design-contract.md`, `docs/trails-reference-deviations.md` | Preview status of F02/F07/F25, the roots' control and copy table, DEV-31…DEV-37 (Task 1) |
| `multiplatform/data/trail/api/.../TrailData.kt` | `CompletedActivity`, `ForYouFeed`, `ActivityRepository`, `ForYouRepository`, `TrailAccount.activities/forYou` (Task 2) |
| `multiplatform/data/trail/impl/.../SampleFeeds.kt` (new), `M1Backend.kt`, `StoreLoadStates.kt` (new), `AccountFeed.kt` (new), `RealTrailAccount.kt`, `TrailCatalog.kt` | Fixtures, backend endpoints, shared Store result folding, per-account cached feeds (Task 2) |
| `multiplatform/app/bootstrap/impl/src/commonTest/.../BootstrapRecoveryTest.kt` | `TestAccount` fake gains the two members (Task 2) |
| `multiplatform/screen/{navigate,activity,foryou}/api` (new) | `Screen`, state and intents per root (Task 3) |
| `multiplatform/screen/explore/api/.../M1Navigation.kt`, `multiplatform/di/graph/active/.../M1NavigationController.kt`, `M1NavigationCheckpoint.kt`, `ActiveGraph.kt`, `settings.gradle.kts` | Five roots, checkpoints, repository providers (Task 3) |
| `…/designsystem/component/TrailRouteSchematic.kt` (new), `TrailsStatus.kt` | Schematic route, toast host `showCheck` (Task 4) |
| `multiplatform/screen/navigate/impl` (new) | `NavigatePresenter`, `NavigateUi`, trail choice (Task 4) |
| `multiplatform/screen/activity/impl` (new) | `ActivitySummary.kt`, `ActivityPresenter`, `ActivityUi` (Task 5) |
| `multiplatform/screen/foryou/impl` (new) | `ForYouPresenter`, `ForYouUi` (Task 6) |
| `multiplatform/app/core/.../M1Content.kt`, `multiplatform/screen/welcome/impl/.../WelcomeUi.kt` | Five-item pill, checkpoint observer, sample-data disclosure (Task 7) |
| `docs/evidence/roots/README.md` (new), `docs/trails-reference-deviations.md`, `docs/superpowers/plans/2026-09-14-trails-v25-store6-completion.md` | Verification record, evidence links, master plan tick (Task 8) |

`…` abbreviates `src/commonMain/kotlin/org/mobilenativefoundation/trails/<module package>`; test files use `src/jvmTest/kotlin/…`.

---

### Task 1: Record the preview scope in the contracts and ledger

**Files:**
- Modify: `docs/trails-completion-contract.md`
- Modify: `docs/trails-m1-design-contract.md`
- Modify: `docs/trails-reference-deviations.md`

**Interfaces:**
- Consumes: the design spec `docs/superpowers/specs/2026-09-20-trails-remaining-roots-design.md`.
- Produces: the copy table below (used verbatim by Tasks 4–7) and deviation IDs DEV-31…DEV-37 (cited by Tasks 4–8).

- [ ] **Step 1: Mark the three frames as previews in the completion contract**

In `docs/trails-completion-contract.md` replace the rows that begin `| F02 \`148:55\` |`, `| F07 \`148:71\` |` and `| F25 \`236:2258\` |` with:

```markdown
| F02 `148:55` | For You | M2 (preview shipped) | Feature card and "More like" rows come from a seeded per-account fixture; hearts share saved state; the avatar and any personalization wait for M4/M2. |
| F07 `148:71` | Activity | M3 (preview shipped) | History and monthly totals derive from seeded sample activities; rows open their trail; recorded history arrives with the M3 recorder. |
| F25 `236:2258` | Navigate | M3 (preview shipped) | Schematic route preview of the current trail; Start recording announces that recording is coming; the recorder is M3. |
```

- [ ] **Step 2: Add the roots to the M1 design contract**

Append to `docs/trails-m1-design-contract.md`:

```markdown
## Remaining roots preview (September 20, 2026)

The five-root pill is complete. For You, Navigate and Activity are previews: seeded data, no map provider, no recorder ([design](superpowers/specs/2026-09-20-trails-remaining-roots-design.md)).

| Control / Frame | Intent; owner | Data effect | Behaviour | Cells |
| --- | --- | --- | --- | --- |
| Feature card / F02 | `OpenTrail(featured)`; For You Presenter | None | Opens the featured trail's detail in the For You stack | 10 |
| Recommendation row / F02 | `OpenTrail(trail)`; For You Presenter | None | Rows exist only for trails cached in the catalog | 10 |
| Heart circle / F02, F07 | `SaveTrail`; save feature | Draft until Save | Same sheet, toast and status lines as Explore | 10, 12 |
| Trail name pill / F25 | `OpenTrail`; Navigate Presenter | None | The current trail: last opened, else first saved, else Half Dome | 11 |
| Start recording / F25 | `StartRecording`; Navigate Presenter | None | Dark toast `Recording is coming soon`, no tick, dismisses after 5 s (DEV-31) | 11 |
| Activity row / F07 | `OpenTrail(trailId)`; Activity Presenter | None | Opens the trail's detail in the Activity stack (DEV-35) | 12 |
| Explore trails / F07 empty | `Explore`; Activity Presenter | None | Selects the Explore root | 12 |
| Try again / F02, F07, F25 | `Retry`; each Presenter | Refreshes the feed or trail | Cached content stays visible; status line beside its subject | 10–12 |

Query and data contract: activity totals cover the device's current calendar month; the seven bars are the last seven local days ending today; dates read `Today`, `Yesterday`, else `Sep 10`. Fixtures are seeded once per account on first request and read verbatim afterwards; they never change on refresh. Welcome discloses that activity history and recommendations are sample data.
```

- [ ] **Step 3: Append deviations DEV-31…DEV-37**

Append rows to the ledger table in `docs/trails-reference-deviations.md`:

```markdown
| DEV-31 | Start recording on Navigate | Shows the toast `Recording is coming soon`; no recorder, no recording screens (F05, F11–F13, F15) | M3 recorder contract |
| DEV-32 | Navigate route over a basemap | Deterministic schematic polyline on a plain surface, labelled `Route preview · schematic`; no tiles, geometry or attribution | M2 map provider and geometry |
| DEV-33 | Navigate map settings and locate circles | Omitted | M2 map controls, M3 location |
| DEV-34 | For You header avatar | Omitted | M4 profile |
| DEV-35 | Activity entries open an activity detail | Rows open the trail's detail; Figma has no activity-detail frame | M3 recorded activities |
| DEV-36 | Activity row bookmark glyph | The R2 heart circle wired to the save flow | None required |
| DEV-37 | Activity history and recommendations | Seeded per-account sample fixtures, disclosed on Welcome | M3 recorder; M2 recommendation content |
```

- [ ] **Step 4: Verify and commit**

```bash
git diff --check && grep -c "preview shipped" docs/trails-completion-contract.md && grep -c "DEV-3[1-7]" docs/trails-reference-deviations.md
```

Expected: no whitespace errors; `3`; `7`.

```bash
git add docs/trails-completion-contract.md docs/trails-m1-design-contract.md docs/trails-reference-deviations.md
git commit -m "docs(design): record the remaining roots preview in the contracts

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

**Copy table (verbatim strings for Tasks 4–7):**

| Situation | String | Indicator and action |
| --- | --- | --- |
| Navigate sheet | `Ready when you are` / `Choose a trail or record your own route.` / `Start recording` | Hero button |
| Start recording | `Recording is coming soon` | Toast, no tick, no action |
| Route label | `Route preview · schematic` | Label pill |
| Navigate loading / unavailable | `Finding your trail…` / `Not on this device yet` / `Couldn’t load this trail` | Failed + `Try again` |
| Activity heading | `Activity` / `A little progress. A lot of fresh air.` | — |
| Activity month card | `<MONTH> SO FAR`, `km walked`, `trail`/`trails`, `outside` | — |
| Activity list | `Your recent adventures` | — |
| Activity empty | `Your hikes will show up here` / `Find a trail worth walking and it’ll be waiting for you here afterwards.` / `Explore trails` | Commit button |
| Activity offline, no cache | `Offline · Activity isn’t on this device yet` | Offline + `Try again` |
| Activity failed, no cache | `Couldn’t load your activity` | Failed + `Try again` |
| Refresh failed, content kept | `Couldn’t refresh · Showing this device’s copy` | Failed + `Try again` |
| For You heading | `For you` | — |
| For You section | `Based on your activity` / `More like <anchor>` | — |
| For You offline, no cache | `Offline · Picks aren’t on this device yet` | Offline + `Try again` |
| For You failed, no cache | `Couldn’t load your picks` | Failed + `Try again` |
| For You rows without cached trails | `Trail details aren’t on this device yet` | Info |

---

### Task 2: Models, fixtures and per-account feeds

**Files:**
- Modify: `multiplatform/data/trail/api/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/TrailData.kt`
- Create: `multiplatform/data/trail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/SampleFeeds.kt`
- Create: `multiplatform/data/trail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/StoreLoadStates.kt`
- Create: `multiplatform/data/trail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/AccountFeed.kt`
- Modify: `multiplatform/data/trail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/M1Backend.kt`
- Modify: `multiplatform/data/trail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/RealTrailAccount.kt`
- Modify: `multiplatform/data/trail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/trail/TrailCatalog.kt` (use the shared folding helper)
- Modify: `multiplatform/app/bootstrap/impl/src/commonTest/kotlin/org/mobilenativefoundation/trails/app/bootstrap/BootstrapRecoveryTest.kt` (`TestAccount` fake)
- Test: `multiplatform/data/trail/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/data/trail/SampleFeedsTest.kt`
- Test: `multiplatform/data/trail/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/data/trail/AccountFeedTest.kt`

**Interfaces:**
- Produces: `@Serializable data class CompletedActivity(id, trailId, trailName, completedAtEpochMillis: Long, distanceMeters: Int, durationMinutes: Int, elevationMeters: Int)`; `@Serializable data class ForYouFeed(featuredTrailId, headline, subline, anchorTrailId, recommendedTrailIds: List<String>)`; `interface ActivityRepository { fun observe(): Flow<LoadState<List<CompletedActivity>>>; suspend fun refresh() }`; `interface ForYouRepository { fun observe(): Flow<LoadState<ForYouFeed>>; suspend fun refresh() }`; `TrailAccount.activities: ActivityRepository`, `TrailAccount.forYou: ForYouRepository`; `internal fun sampleActivities(nowEpochMillis: Long): List<CompletedActivity>`; `internal val sampleForYou: ForYouFeed`; `internal fun <V : Any> Flow<StoreResult<V>>.loadStates(config: StateFlow<BackendConfig?>, onData: () -> Unit = {}): Flow<LoadState<V>>`.
- Fixtures are stored in the backend database's `cache_row` table (namespaces `backend-activities`, `backend-foryou`, canonical id = account) and cached per account in the value database's `cache_row` (namespaces `activities`, `foryou`). No schema change.

- [ ] **Step 1: Write the failing fixture test**

```kotlin
package org.mobilenativefoundation.trails.data.trail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleFeedsTest {
    private val now = 1_790_000_000_000L

    @Test
    fun sampleActivitiesUseCatalogRoutesAndCountBackFromNow() {
        val activities = sampleActivities(now)
        assertEquals(6, activities.size)
        assertEquals(6, activities.map { it.id }.toSet().size)
        activities.forEach { activity ->
            val trail = worldTrails.single { it.id == activity.trailId }
            assertEquals(trail.name, activity.trailName)
            assertEquals(trail.distanceMeters, activity.distanceMeters)
            assertEquals(trail.durationMinutes, activity.durationMinutes)
            assertEquals(trail.elevationMeters, activity.elevationMeters)
            assertTrue(activity.completedAtEpochMillis < now)
        }
        assertEquals("half-dome", activities.first().trailId)
        assertEquals(now - 86_400_000L, activities.first().completedAtEpochMillis)
        assertTrue(activities.zipWithNext().all { (newer, older) -> newer.completedAtEpochMillis > older.completedAtEpochMillis })
    }

    @Test
    fun forYouFixtureResolvesInTheCatalog() {
        val ids = worldTrails.map { it.id }.toSet()
        assertTrue(sampleForYou.featuredTrailId in ids)
        assertTrue(sampleForYou.anchorTrailId in ids)
        assertTrue(sampleForYou.recommendedTrailIds.all { it in ids })
        assertEquals(sampleForYou.recommendedTrailIds.size, sampleForYou.recommendedTrailIds.toSet().size)
        assertTrue(sampleForYou.anchorTrailId !in sampleForYou.recommendedTrailIds)
        assertTrue(sampleForYou.featuredTrailId != sampleForYou.anchorTrailId)
    }
}
```

- [ ] **Step 2: Write the failing account-feed test**

```kotlin
package org.mobilenativefoundation.trails.data.trail

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.mobilenativefoundation.trails.server.BackendConfig
import org.mobilenativefoundation.trails.server.NetworkMode
import kotlin.time.Duration.Companion.milliseconds

class AccountFeedTest {
    private val online = BackendConfig(latencyRange = 0.milliseconds..0.milliseconds)

    @Test
    fun feedsSeedOncePerAccountAndSurviveAnOfflineReopen() = runBlocking {
        val directory = Files.createTempDirectory("trails-feeds").toFile()
        try {
            val first = RealTrailDataFactory(PlatformM1DriverFactory(directory), this)
            first.applyBackendConfig(online)
            val account = first.open("alice")
            val activities = withTimeout(10_000) { account.activities.observe().first { it.data != null } }.data!!
            assertEquals(6, activities.size)
            assertEquals("half-dome", activities.first().trailId)
            val feed = withTimeout(10_000) { account.forYou.observe().first { it.data != null } }.data!!
            assertEquals("trolltunga", feed.featuredTrailId)
            first.applyBackendConfig(online.copy(networkMode = NetworkMode.OFFLINE))
            first.close()

            val reopened = RealTrailDataFactory(PlatformM1DriverFactory(directory), this)
            assertEquals(NetworkMode.OFFLINE, reopened.restoreBackendConfig().networkMode)
            val again = reopened.open("alice")
            val cached = withTimeout(10_000) { again.activities.observe().first { it.data != null } }
            assertEquals(activities, cached.data)
            assertTrue(cached.offline)
            assertEquals(feed, withTimeout(10_000) { again.forYou.observe().first { it.data != null } }.data)
            val cold = reopened.open("bob")
            val unavailable = withTimeout(10_000) { cold.activities.observe().first { it.error != null } }
            assertNull(unavailable.data)
            assertTrue(unavailable.offline)
            reopened.applyBackendConfig(online)
            cold.activities.refresh()
            val seeded = withTimeout(10_000) { cold.activities.observe().first { it.data != null } }.data!!
            assertNotNull(seeded.firstOrNull())
            assertTrue(seeded.first().completedAtEpochMillis >= activities.first().completedAtEpochMillis)
            reopened.close()
        } finally { directory.deleteRecursively() }
    }
}
```

- [ ] **Step 3: Run both and keep the failure**

```bash
./gradlew :multiplatform:data:trail:impl:jvmTest --tests "*SampleFeedsTest*" --tests "*AccountFeedTest*" 2>&1 | tee /private/tmp/roots-task2-red.log
```

Expected: compilation fails with `Unresolved reference 'sampleActivities'` (or `'CompletedActivity'`).

- [ ] **Step 4: Extend the API**

In `TrailData.kt` add after `TrailCollection`:

```kotlin
/** A finished hike. Until the recorder ships, the fake backend seeds sample rows per account (DEV-37). */
@Serializable
data class CompletedActivity(
    val id: String,
    val trailId: String,
    val trailName: String,
    val completedAtEpochMillis: Long,
    val distanceMeters: Int,
    val durationMinutes: Int,
    val elevationMeters: Int,
)

/** Per-account recommendation fixture: one featured trail and rows "more like" an anchor trail. */
@Serializable
data class ForYouFeed(
    val featuredTrailId: String,
    val headline: String,
    val subline: String,
    val anchorTrailId: String,
    val recommendedTrailIds: List<String>,
)

interface ActivityRepository {
    fun observe(): Flow<LoadState<List<CompletedActivity>>>
    suspend fun refresh()
}

interface ForYouRepository {
    fun observe(): Flow<LoadState<ForYouFeed>>
    suspend fun refresh()
}
```

Replace the `TrailAccount` interface with:

```kotlin
interface TrailAccount {
    val accountId: String
    val saved: SavedRepository
    val activities: ActivityRepository
    val forYou: ForYouRepository
    /** Retires the account owner, joins jobs, then closes its drivers. */
    suspend fun close()
}
```

In `BootstrapRecoveryTest.kt`, inside `private class TestAccount(...) : TrailAccount`, add after the `saved` line:

```kotlin
    override val activities: ActivityRepository get() = error("Not needed by bootstrap")
    override val forYou: ForYouRepository get() = error("Not needed by bootstrap")
```

with imports `org.mobilenativefoundation.trails.data.trail.ActivityRepository` and `org.mobilenativefoundation.trails.data.trail.ForYouRepository`.

- [ ] **Step 5: Create the fixtures**

`SampleFeeds.kt`:

```kotlin
package org.mobilenativefoundation.trails.data.trail

private const val DAY_MILLIS = 86_400_000L

/** Sample history: catalog routes with their catalog distance, duration and gain, dated back from the first request. */
internal fun sampleActivities(nowEpochMillis: Long): List<CompletedActivity> = listOf(
    "half-dome" to 1,
    "mount-takao-trail-1" to 3,
    "bondi-to-coogee-coastal-walk" to 6,
    "diamond-head-summit-trail" to 9,
    "preikestolen" to 15,
    "lake-agnes-tea-house" to 24,
).map { (trailId, daysAgo) ->
    val trail = worldTrails.first { it.id == trailId }
    CompletedActivity(
        id = "sample-$trailId",
        trailId = trailId,
        trailName = trail.name,
        completedAtEpochMillis = nowEpochMillis - daysAgo * DAY_MILLIS,
        distanceMeters = trail.distanceMeters,
        durationMinutes = trail.durationMinutes,
        elevationMeters = trail.elevationMeters,
    )
}

/** Authored per-account recommendation fixture (DEV-37); nothing here is inferred from behaviour. */
internal val sampleForYou = ForYouFeed(
    featuredTrailId = "trolltunga",
    headline = "A weekend worth the walk",
    subline = "Discover a quieter side of outside.",
    anchorTrailId = "half-dome",
    recommendedTrailIds = listOf(
        "mist-trail-to-nevada-fall", "upper-yosemite-fall-trail", "angels-landing",
        "franconia-ridge-loop", "preikestolen", "ben-nevis-mountain-track",
    ),
)
```

- [ ] **Step 6: Serve the fixtures from the fake backend**

In `M1Backend.kt` add the imports `kotlinx.serialization.KSerializer` and `kotlinx.serialization.builtins.ListSerializer`, and add inside `class M1Backend` after `suspend fun saved(...)`:

```kotlin
    suspend fun activities(account: String): List<CompletedActivity> {
        request()
        return gate.withLock {
            requireOnline()
            fixture("backend-activities", account, ListSerializer(CompletedActivity.serializer())) { sampleActivities(Clock.System.now().toEpochMilliseconds()) }
        }
    }
    suspend fun forYou(account: String): ForYouFeed {
        request()
        return gate.withLock { requireOnline(); fixture("backend-foryou", account, ForYouFeed.serializer()) { sampleForYou } }
    }
    /**
     * Per-account fixtures live in this database's otherwise-unused cache_row table, so installed
     * builds need no schema migration. A row is written on the first request and read verbatim after.
     */
    private fun <T> fixture(namespace: String, account: String, serializer: KSerializer<T>, seed: () -> T): T =
        sql.readCache(namespace, account).executeAsOneOrNull()?.let { Json.decodeFromString(serializer, it.payload.decodeToString()) }
            ?: seed().also { sql.writeCache(namespace, account, Json.encodeToString(serializer, it).encodeToByteArray()) }
```

- [ ] **Step 7: Share the Store result folding**

`StoreLoadStates.kt`:

```kotlin
@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class)

package org.mobilenativefoundation.trails.data.trail

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.mobilenativefoundation.store6.core.StoreResult
import org.mobilenativefoundation.trails.server.BackendConfig
import org.mobilenativefoundation.trails.server.NetworkMode

/** Folds Store results into LoadState; cached data stays visible through later failures. */
internal fun <V : Any> Flow<StoreResult<V>>.loadStates(config: StateFlow<BackendConfig?>, onData: () -> Unit = {}): Flow<LoadState<V>> =
    combine(flow {
        var latest = LoadState<V>()
        this@loadStates.collect { result ->
            latest = when (result) {
                is StoreResult.Loading -> latest.copy(loading = true)
                is StoreResult.Data -> { onData(); LoadState(result.value, result.refreshing) }
                is StoreResult.Error -> latest.copy(loading = false, error = result.error.messageText())
                is StoreResult.Revalidated -> latest.copy(loading = false, error = null)
            }
            emit(latest)
        }
    }, config) { state, configured -> state.copy(offline = configured?.networkMode == NetworkMode.OFFLINE) }
```

In `TrailCatalog.kt` replace the whole `private fun observe(key: CatalogKey) = combine(flow { … }, backend.config) { … }` function with:

```kotlin
    private fun observe(key: CatalogKey) = store.stream(key).loadStates(backend.config) { revision.update { it + 1 } }
```

and remove the now-unused imports `kotlinx.coroutines.flow.combine` and `kotlinx.coroutines.flow.flow` (keep `map` and `update`). If the compiler reports that `StoreResult` is not generic in the pinned Store6, change the helper's receiver to `Flow<StoreResult<V>>`'s actual declared shape as the compiler names it and report the adjustment.

- [ ] **Step 8: Add the account feed store**

`AccountFeed.kt`:

```kotlin
@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class)

package org.mobilenativefoundation.trails.data.trail

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.mobilenativefoundation.store6.core.*
import org.mobilenativefoundation.store6.sqldelight.SqlDelightBookkeeper
import org.mobilenativefoundation.store6.sqldelight.SqlDelightSourceOfTruth
import org.mobilenativefoundation.trails.data.trail.db.M1Database

/** One account-scoped read, cached in the account's value database under its own cache_row namespace. */
internal class AccountFeed<T : Any>(
    kind: String,
    accountId: String,
    driver: SqlDriver,
    database: M1Database,
    private val backend: M1Backend,
    storageLifetime: StorageLifetime,
    private val serializer: KSerializer<T>,
    fetch: suspend () -> T,
) {
    private val queries = database.m1Queries
    private val key = CatalogKey(kind, accountId)
    private val source = storageLifetime.source(SqlDelightSourceOfTruth<CatalogKey, T>(
        driver, database,
        readQuery = { k -> queries.readCache(k.kind, k.id) { _, _, bytes -> Json.decodeFromString(serializer, bytes.decodeToString()) } },
        writeRow = { k, value -> queries.writeCache(k.kind, k.id, Json.encodeToString(serializer, value).encodeToByteArray()) },
        deleteRow = { queries.deleteCache(it.kind, it.id) },
        deleteNamespaceRows = { queries.deleteNamespace(it.value) },
        // The saved rows share this database; a clear-all for this feed only clears its namespace.
        deleteAllRows = { queries.deleteNamespace(kind) },
    ))
    private val store = store<CatalogKey, T> {
        fetcher { fetch() }
        persistence(source)
        bookkeeper(storageLifetime.bookkeeper(SqlDelightBookkeeper(driver, database)))
    }

    fun observe(): Flow<LoadState<T>> = store.stream(key).loadStates(backend.config)

    /** Invalidation keeps cached content visible; the stream reports a failed fetch. */
    suspend fun refresh() {
        store.invalidate(key)
        try { store.get(key, Freshness.MustBeFresh) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* The stream reports the structured read failure. */ }
    }

    suspend fun close() { store.close() }
}
```

- [ ] **Step 9: Expose the feeds on the account**

In `RealTrailAccount.kt` add the import `kotlinx.serialization.builtins.ListSerializer`, then add after the `private val journal = …` declaration:

```kotlin
    private val activityFeed = AccountFeed("activities", accountId, valueDriver, values, backend, storageLifetime, ListSerializer(CompletedActivity.serializer())) { backend.activities(accountId) }
    private val forYouFeed = AccountFeed("foryou", accountId, valueDriver, values, backend, storageLifetime, ForYouFeed.serializer()) { backend.forYou(accountId) }
    override val activities: ActivityRepository = object : ActivityRepository {
        override fun observe() = activityFeed.observe()
        override suspend fun refresh() = activityFeed.refresh()
    }
    override val forYou: ForYouRepository = object : ForYouRepository {
        override fun observe() = forYouFeed.observe()
        override suspend fun refresh() = forYouFeed.refresh()
    }
```

In `close()` add `activityFeed.close()` and `forYouFeed.close()` on the two lines before `store.close()`.

- [ ] **Step 10: Run the data suite and the bootstrap tests**

```bash
./gradlew :multiplatform:data:trail:impl:jvmTest :multiplatform:app:bootstrap:impl:jvmTest
```

Expected: all pass, including the two new suites and every earlier persistence, reseed, sort-cache, fault and lifetime test.

- [ ] **Step 11: Commit**

```bash
git add multiplatform/data/trail multiplatform/app/bootstrap
git commit -m "feat(trail): add seeded activity and recommendation feeds per account

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 3: Screen contracts and the five-root navigation

**Files:**
- Create: `multiplatform/screen/navigate/api/build.gradle.kts`, `…/screen/navigate/NavigateScreen.kt`, `…/screen/navigate/NavigateState.kt`
- Create: `multiplatform/screen/activity/api/build.gradle.kts`, `…/screen/activity/ActivityScreen.kt`, `…/screen/activity/ActivityState.kt`
- Create: `multiplatform/screen/foryou/api/build.gradle.kts`, `…/screen/foryou/ForYouScreen.kt`, `…/screen/foryou/ForYouState.kt`
- Modify: `settings.gradle.kts`
- Modify: `multiplatform/screen/explore/api/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/explore/M1Navigation.kt`
- Modify: `multiplatform/di/graph/active/build.gradle.kts`
- Modify: `multiplatform/di/graph/active/src/commonMain/kotlin/org/mobilenativefoundation/trails/di/graph/active/M1NavigationController.kt`
- Modify: `multiplatform/di/graph/active/src/commonMain/kotlin/org/mobilenativefoundation/trails/di/graph/active/M1NavigationCheckpoint.kt`
- Modify: `multiplatform/di/graph/active/src/commonMain/kotlin/org/mobilenativefoundation/trails/di/graph/active/ActiveGraph.kt`
- Test: `multiplatform/di/graph/active/src/jvmTest/kotlin/org/mobilenativefoundation/trails/di/graph/active/M1NavigationRecoveryTest.kt` (extend)

**Interfaces:**
- Produces: `NavigateScreen`, `ActivityScreen`, `ForYouScreen` (`@Parcelize data object … : Screen`); the state and intent types below; `M1Navigation.selectForYou()`, `selectNavigate()`, `selectActivity()`, `lastOpenedTrailId: String?`; `M1NavigationController.Root { EXPLORE, FOR_YOU, NAVIGATE, SAVED, ACTIVITY }`, `allStacks: List<SaveableBackStack>`; checkpoint fields `forYouRoutes`, `navigateRoutes`, `activityRoutes`, `lastTrail`; `ActiveGraph` providers for `ActivityRepository` and `ForYouRepository`.
- Route names: `foryou`, `navigate`, `activity`; scroll keys `FOR_YOU/foryou`, `ACTIVITY/activity`.

- [ ] **Step 1: Create the three API modules**

Each `api/build.gradle.kts` is identical to `multiplatform/screen/saved/api/build.gradle.kts` except the namespace (`org.mobilenativefoundation.trails.screen.navigate.api`, `…activity.api`, `…foryou.api`). In `settings.gradle.kts` add after `include(":multiplatform:screen:collection:impl")`:

```kotlin
include(":multiplatform:screen:navigate:api")
include(":multiplatform:screen:navigate:impl")
include(":multiplatform:screen:activity:api")
include(":multiplatform:screen:activity:impl")
include(":multiplatform:screen:foryou:api")
include(":multiplatform:screen:foryou:impl")
```

(The impl modules are created in Tasks 4–6; Gradle tolerates an included path without a build file only if the directory exists, so create the three `impl` directories now with an empty `build.gradle.kts` containing `plugins { id("plugin.trails.feature") }` and `android { namespace = "org.mobilenativefoundation.trails.screen.<root>.impl" }`.)

`NavigateScreen.kt`:

```kotlin
package org.mobilenativefoundation.trails.screen.navigate

import com.slack.circuit.runtime.screen.Screen
import org.mobilenativefoundation.trails.foundation.parcel.Parcelize

@Parcelize
data object NavigateScreen : Screen
```

`NavigateState.kt`:

```kotlin
package org.mobilenativefoundation.trails.screen.navigate

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.Trail

data class NavigateState(
    val trail: LoadState<Trail>,
    val toast: String? = null,
    val send: (NavigateIntent) -> Unit,
) : CircuitUiState

sealed interface NavigateIntent : CircuitUiEvent {
    data object OpenTrail : NavigateIntent
    data object StartRecording : NavigateIntent
    data object DismissToast : NavigateIntent
    data object Retry : NavigateIntent
}
```

`ActivityScreen.kt` mirrors `NavigateScreen.kt` with `data object ActivityScreen : Screen` in package `org.mobilenativefoundation.trails.screen.activity`. `ActivityState.kt`:

```kotlin
package org.mobilenativefoundation.trails.screen.activity

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.screen.explore.M1ScrollPosition

data class ActivityState(
    val history: LoadState<List<CompletedActivity>>,
    val trails: Map<String, Trail>,
    val saved: LoadState<SavedSnapshot>,
    val nowEpochMillis: Long,
    val initialScroll: M1ScrollPosition = M1ScrollPosition(),
    val send: (ActivityIntent) -> Unit,
) : CircuitUiState

sealed interface ActivityIntent : CircuitUiEvent {
    data class ScrollChanged(val position: M1ScrollPosition) : ActivityIntent
    data class OpenTrail(val trailId: String) : ActivityIntent
    data class SaveTrail(val trail: Trail, val onDismiss: () -> Unit) : ActivityIntent
    data object Explore : ActivityIntent
    data object Retry : ActivityIntent
}
```

`ForYouScreen.kt` mirrors it with `data object ForYouScreen : Screen` in package `org.mobilenativefoundation.trails.screen.foryou`. `ForYouState.kt`:

```kotlin
package org.mobilenativefoundation.trails.screen.foryou

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.screen.explore.M1ScrollPosition

data class ForYouState(
    val feed: LoadState<ForYouFeed>,
    val trails: Map<String, Trail>,
    val saved: LoadState<SavedSnapshot>,
    val initialScroll: M1ScrollPosition = M1ScrollPosition(),
    val send: (ForYouIntent) -> Unit,
) : CircuitUiState

sealed interface ForYouIntent : CircuitUiEvent {
    data class ScrollChanged(val position: M1ScrollPosition) : ForYouIntent
    data class OpenTrail(val trail: Trail) : ForYouIntent
    data class SaveTrail(val trail: Trail, val onDismiss: () -> Unit) : ForYouIntent
    data object Retry : ForYouIntent
}
```

- [ ] **Step 2: Write the failing navigation tests**

Append to `M1NavigationRecoveryTest` (add imports for `ActivityScreen`, `ForYouScreen`, `NavigateScreen`, `kotlin.test.assertNull` if missing, and `java.nio.file.Files`):

```kotlin
    @Test
    fun checkpointWrittenBeforeTheNewRootsRestoresDefaultsForThem() = withDirectory { directory ->
        val path = directory.resolve("legacy.json")
        Files.writeString(path, """{"version":1,"root":"SAVED","exploreRoutes":[{"name":"explore"}],"savedRoutes":[{"name":"saved"},{"name":"collection","id":"weekend"}],"text":"","query":{},"allTrails":false,"scroll":{}}""")
        val restored = M1NavigationController(FileStorage(path))
        assertNull(restored.persistenceError)
        assertEquals(M1NavigationController.Root.SAVED, restored.selectedRoot)
        assertEquals(listOf(CollectionScreen("weekend"), SavedScreen), restored.savedStack.map { it.screen })
        assertEquals(listOf(ForYouScreen), restored.forYouStack.map { it.screen })
        assertEquals(listOf(NavigateScreen), restored.navigateStack.map { it.screen })
        assertEquals(listOf(ActivityScreen), restored.activityStack.map { it.screen })
        assertNull(restored.lastOpenedTrailId)
    }

    @Test
    fun newRootsRestoreTheirStacksAndTheLastOpenedTrail() = withDirectory { directory ->
        val path = directory.resolve("roots.json")
        val first = M1NavigationController(FileStorage(path))
        first.selectActivity()
        first.openTrail("half-dome")
        first.selectForYou()
        first.openTrail("trolltunga")
        first.checkpointScroll(first.viewKey("foryou"), M1ScrollPosition(2, 9))
        first.selectNavigate()

        val restored = M1NavigationController(FileStorage(path))
        assertNull(restored.persistenceError)
        assertEquals(M1NavigationController.Root.NAVIGATE, restored.selectedRoot)
        assertEquals(listOf(TrailDetailScreen("half-dome"), ActivityScreen), restored.activityStack.map { it.screen })
        assertEquals(listOf(TrailDetailScreen("trolltunga"), ForYouScreen), restored.forYouStack.map { it.screen })
        assertEquals(listOf(NavigateScreen), restored.navigateStack.map { it.screen })
        assertEquals("trolltunga", restored.lastOpenedTrailId)
        assertEquals(M1ScrollPosition(2, 9), restored.scrollPosition("FOR_YOU/foryou"))
        restored.selectSavedTab()
        assertEquals(SavedScreen, restored.backStack.topRecord?.screen)
    }
```

- [ ] **Step 3: Run and keep the failure**

```bash
./gradlew :multiplatform:di:graph:active:jvmTest --tests "*M1NavigationRecoveryTest*" 2>&1 | tee /private/tmp/roots-task3-red.log
```

Expected: compilation fails with `Unresolved reference 'ForYouScreen'` (or `'selectActivity'`).

- [ ] **Step 4: Extend the navigation interface**

In `M1Navigation.kt` add inside the interface after `fun back()`:

```kotlin
    fun selectForYou() {}
    fun selectNavigate() {}
    fun selectActivity() {}
    /** The trail most recently opened from any root; Navigate previews it. */
    val lastOpenedTrailId: String? get() = null
```

- [ ] **Step 5: Wire the modules**

In `multiplatform/di/graph/active/build.gradle.kts` add after `api(projects.multiplatform.screen.collection.impl)`:

```kotlin
                api(projects.multiplatform.screen.navigate.api)
                api(projects.multiplatform.screen.activity.api)
                api(projects.multiplatform.screen.foryou.api)
```

(Tasks 4–6 switch each line to the `impl` module when it exists.)

- [ ] **Step 6: Five roots in the controller**

Replace `M1NavigationController.kt` with:

```kotlin
package org.mobilenativefoundation.trails.di.graph.active

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.foundation.Navigator
import com.slack.circuit.runtime.screen.Screen
import org.mobilenativefoundation.trails.screen.activity.ActivityScreen
import org.mobilenativefoundation.trails.screen.collection.CollectionScreen
import org.mobilenativefoundation.trails.screen.explore.ExploreScreen
import org.mobilenativefoundation.trails.screen.explore.M1Navigation
import org.mobilenativefoundation.trails.screen.explore.M1ExploreView
import org.mobilenativefoundation.trails.screen.explore.M1ScrollPosition
import org.mobilenativefoundation.trails.screen.foryou.ForYouScreen
import org.mobilenativefoundation.trails.screen.navigate.NavigateScreen
import org.mobilenativefoundation.trails.screen.saved.SavedScreen
import org.mobilenativefoundation.trails.screen.traildetail.TrailDetailScreen

/** Account-owned independent roots. A tab change never pushes another root destination. */
class M1NavigationController(private val storage: M1NavigationStorage = InMemoryM1NavigationStorage()) : M1Navigation {
    enum class Root { EXPLORE, FOR_YOU, NAVIGATE, SAVED, ACTIVITY }
    var persistenceError by mutableStateOf<String?>(null)
        private set
    private var restoreFailed = false
    private var lastWritten: String? = null
    private var view = restore()
    var selectedRoot by mutableStateOf(view.root)
        private set
    val exploreStack = restoreStack(view.exploreRoutes)
    val forYouStack = restoreStack(view.forYouRoutes)
    val navigateStack = restoreStack(view.navigateRoutes)
    val savedStack = restoreStack(view.savedRoutes)
    val activityStack = restoreStack(view.activityRoutes)
    /** Root order matches the pill: Explore, For You, Navigate, Saved, Activity. */
    val allStacks: List<SaveableBackStack> get() = listOf(exploreStack, forYouStack, navigateStack, savedStack, activityStack)
    private val navigators = Root.entries.associateWith { root -> Navigator(stack(root)) {} }
    val backStack get() = stack(selectedRoot)
    val navigator get() = navigators.getValue(selectedRoot)

    override val exploreView get() = view.explore
    override val savedAllTrails get() = view.allTrails
    override val lastOpenedTrailId: String? get() = view.lastTrail

    override fun selectExplore() { selectedRoot = Root.EXPLORE; changed() }
    override fun selectForYou() { selectedRoot = Root.FOR_YOU; changed() }
    override fun selectNavigate() { selectedRoot = Root.NAVIGATE; changed() }
    override fun selectActivity() { selectedRoot = Root.ACTIVITY; changed() }
    fun selectSavedTab() { selectedRoot = Root.SAVED; changed() }
    override fun selectSaved(collectionId: String?) {
        selectedRoot = Root.SAVED
        // Explicit success actions open the named destination; tab selection preserves its stack.
        while (savedStack.size > 1) navigator.pop()
        if (collectionId != null) navigator.goTo(CollectionScreen(collectionId))
        changed()
    }
    override fun openTrail(trailId: String) {
        navigator.goTo(TrailDetailScreen(trailId))
        if (view.lastTrail != trailId) view = view.copy(lastTrail = trailId)
        changed()
    }
    override fun back() { navigator.pop(); changed() }

    override fun checkpointExplore(value: M1ExploreView) {
        if (view.explore == value) return
        val queryChanged = view.explore.query != value.query
        view = view.copy(explore = value, scroll = if (queryChanged) view.scroll + ("EXPLORE/explore" to M1ScrollPosition()) else view.scroll)
        changed()
    }

    override fun checkpointSavedSegment(allTrails: Boolean) {
        if (view.allTrails == allTrails) return
        view = view.copy(allTrails = allTrails)
        changed()
    }

    /** Capture this key before an asynchronous UI callback so tab switches cannot redirect it. */
    override fun viewKey(route: String): String = "${selectedRoot.name}/$route"
    override fun scrollPosition(key: String): M1ScrollPosition = view.scroll[key] ?: M1ScrollPosition()
    override fun checkpointScroll(key: String, position: M1ScrollPosition) {
        if (scrollPosition(key) == position) return
        view = view.copy(scroll = (view.scroll - key + (key to position)).entries.toList().takeLast(64).associate { it.toPair() })
        changed()
    }

    /** Also called by the host after platform-driven Back changes its Circuit stack. */
    fun checkpoint() {
        if (restoreFailed) return // Preserve an unreadable prior snapshot until an explicit action.
        try {
            val encoded = view.copy(
                root = selectedRoot,
                exploreRoutes = routes(exploreStack), forYouRoutes = routes(forYouStack), navigateRoutes = routes(navigateStack),
                savedRoutes = routes(savedStack), activityRoutes = routes(activityStack),
            ).encode()
            if (encoded != lastWritten) { storage.write(encoded); lastWritten = encoded }
            persistenceError = null
        } catch (failure: Exception) {
            persistenceError = "Couldn’t save your place. Your saved trails are kept."
        }
    }

    /** Explicitly saves the current place after a read/write error; never changes domain data. */
    fun retryCheckpoint() { changed() }
    private fun changed() { restoreFailed = false; checkpoint() }

    private fun stack(root: Root): SaveableBackStack = when (root) {
        Root.EXPLORE -> exploreStack
        Root.FOR_YOU -> forYouStack
        Root.NAVIGATE -> navigateStack
        Root.SAVED -> savedStack
        Root.ACTIVITY -> activityStack
    }

    private fun restore(): M1NavigationCheckpoint = try {
        storage.read()?.let { encoded -> M1NavigationCheckpoint.decode(encoded).also { lastWritten = encoded } } ?: M1NavigationCheckpoint()
    } catch (failure: Exception) {
        restoreFailed = true
        persistenceError = "Couldn’t restore your place. Your saved trails are kept."
        M1NavigationCheckpoint()
    }

    private fun routes(stack: SaveableBackStack): List<M1Route> = stack.map { record ->
        when (val screen = record.screen) {
            ExploreScreen -> M1Route("explore")
            ForYouScreen -> M1Route("foryou")
            NavigateScreen -> M1Route("navigate")
            SavedScreen -> M1Route("saved")
            ActivityScreen -> M1Route("activity")
            is TrailDetailScreen -> M1Route("trail", screen.trailId)
            is CollectionScreen -> M1Route("collection", screen.collectionId)
            else -> error("Unsupported M1 screen")
        }
    }.reversed()

    private fun restoreStack(routes: List<M1Route>): SaveableBackStack = SaveableBackStack(routes.first().screen()).also { stack ->
        routes.drop(1).forEach { stack.push(it.screen()) }
    }

    private fun M1Route.screen(): Screen = when (name) {
        "explore" -> ExploreScreen
        "foryou" -> ForYouScreen
        "navigate" -> NavigateScreen
        "saved" -> SavedScreen
        "activity" -> ActivityScreen
        "trail" -> TrailDetailScreen(requireNotNull(id))
        "collection" -> CollectionScreen(requireNotNull(id))
        else -> error("Unsupported M1 route")
    }
}
```

- [ ] **Step 7: Backward-compatible checkpoint**

In `M1NavigationCheckpoint.kt` replace the data class header and `encode()`/`decode()` bodies so the class reads:

```kotlin
internal data class M1NavigationCheckpoint(
    val root: M1NavigationController.Root = M1NavigationController.Root.EXPLORE,
    val exploreRoutes: List<M1Route> = listOf(M1Route("explore")),
    val savedRoutes: List<M1Route> = listOf(M1Route("saved")),
    val forYouRoutes: List<M1Route> = listOf(M1Route("foryou")),
    val navigateRoutes: List<M1Route> = listOf(M1Route("navigate")),
    val activityRoutes: List<M1Route> = listOf(M1Route("activity")),
    val lastTrail: String? = null,
    val explore: M1ExploreView = M1ExploreView(),
    val allTrails: Boolean = false,
    val scroll: Map<String, M1ScrollPosition> = emptyMap(),
) {
    fun encode(): String = buildJsonObject {
        put("version", 1)
        put("root", root.name)
        put("exploreRoutes", encodeRoutes(exploreRoutes))
        put("savedRoutes", encodeRoutes(savedRoutes))
        put("forYouRoutes", encodeRoutes(forYouRoutes))
        put("navigateRoutes", encodeRoutes(navigateRoutes))
        put("activityRoutes", encodeRoutes(activityRoutes))
        lastTrail?.let { put("lastTrail", it) }
        put("text", explore.text)
        put("query", Json.encodeToJsonElement(TrailQuery.serializer(), explore.query))
        put("allTrails", allTrails)
        putJsonObject("scroll") {
            scroll.forEach { (key, value) -> putJsonObject(key) { put("index", value.index); put("offset", value.offset) } }
        }
    }.toString().also { require(it.length <= MAX_CHECKPOINT_CHARACTERS) { "Navigation checkpoint is too large" } }

    companion object {
        private const val MAX_CHECKPOINT_CHARACTERS = 128_000
        fun decode(value: String): M1NavigationCheckpoint {
            require(value.length <= MAX_CHECKPOINT_CHARACTERS) { "Navigation checkpoint is too large" }
            val json = Json.parseToJsonElement(value).jsonObject
            require(json.getValue("version").jsonPrimitive.int == 1) { "Unsupported navigation checkpoint version" }
            val scroll = json.getValue("scroll").jsonObject
            require(scroll.size <= 64)
            // Roots added after the first checkpoints are optional so earlier snapshots still restore.
            fun optionalRoutes(field: String, root: String) = json[field]?.let { decodeRoutes(it, root) } ?: listOf(M1Route(root))
            return M1NavigationCheckpoint(
                root = M1NavigationController.Root.valueOf(json.getValue("root").jsonPrimitive.content),
                exploreRoutes = decodeRoutes(json.getValue("exploreRoutes"), "explore"),
                savedRoutes = decodeRoutes(json.getValue("savedRoutes"), "saved"),
                forYouRoutes = optionalRoutes("forYouRoutes", "foryou"),
                navigateRoutes = optionalRoutes("navigateRoutes", "navigate"),
                activityRoutes = optionalRoutes("activityRoutes", "activity"),
                lastTrail = json["lastTrail"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
                explore = M1ExploreView(json.getValue("text").jsonPrimitive.content, Json.decodeFromJsonElement(TrailQuery.serializer(), json.getValue("query")).normalized()),
                allTrails = json.getValue("allTrails").jsonPrimitive.boolean,
                scroll = scroll.mapValues { (_, entry) -> entry.jsonObject.let { M1ScrollPosition(it.getValue("index").jsonPrimitive.int, it.getValue("offset").jsonPrimitive.int) } },
            )
        }
```

Keep `encodeRoutes` and `decodeRoutes` unchanged.

- [ ] **Step 8: Provide the repositories**

In `ActiveGraph.kt` add after the `savedRepository` provider:

```kotlin
    @Provides fun activityRepository(account: TrailAccount): ActivityRepository = account.activities
    @Provides fun forYouRepository(account: TrailAccount): ForYouRepository = account.forYou
```

- [ ] **Step 9: Run the navigation tests, the app-core tests and the compile**

```bash
./gradlew :multiplatform:di:graph:active:jvmTest :multiplatform:app:core:jvmTest :multiplatform:app:core:compileKotlinJvm
```

Expected: `M1NavigationRecoveryTest` passes with the two new tests; `M1Content` still compiles (it uses `Root.EXPLORE`/`Root.SAVED` and the two stacks until Task 7).

- [ ] **Step 10: Commit**

```bash
git add settings.gradle.kts multiplatform/screen/navigate multiplatform/screen/activity multiplatform/screen/foryou multiplatform/screen/explore/api multiplatform/di/graph/active
git commit -m "feat(navigation): add the For You, Navigate and Activity roots with compatible checkpoints

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 4: Navigate root

**Files:**
- Create: `multiplatform/foundation/designsystem/src/commonMain/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/component/TrailRouteSchematic.kt`
- Modify: `…/designsystem/component/TrailsStatus.kt` (`TrailsToastHost` gains `showCheck`)
- Create: `multiplatform/screen/navigate/impl/build.gradle.kts`, `…/screen/navigate/NavigatePresenter.kt`, `…/screen/navigate/NavigateUi.kt`
- Modify: `multiplatform/di/graph/active/build.gradle.kts`, `…/ActiveGraph.kt`
- Test: `multiplatform/foundation/designsystem/src/jvmTest/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/component/TrailRouteSchematicTest.kt`
- Test: `multiplatform/screen/navigate/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/screen/navigate/NavigateTrailChoiceTest.kt`
- Test: `multiplatform/screen/navigate/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/screen/navigate/NavigateUiTest.kt`

**Interfaces:**
- Produces: `fun schematicRoute(seed: String, loop: Boolean, points: Int = 14): List<Pair<Float, Float>>`; `@Composable fun TrailRouteSchematic(seed: String, loop: Boolean, modifier: Modifier = Modifier)`; `TrailsToastHost(toast, onDismissed, modifier, durationMillis, showCheck: Boolean = true)`; `internal fun navigateTrailId(lastOpened: String?, saved: SavedSnapshot?): String`; `const val RECORDING_COMING_SOON = "Recording is coming soon"`.
- Consumes: Task 3 contracts and navigation; `TrailsButton`, `TrailsStatusLine`, `M1Loading`, `Icons.Outlined.Location`.

- [ ] **Step 1: Write the failing schematic test**

```kotlin
package org.mobilenativefoundation.trails.foundation.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrailRouteSchematicTest {
    @Test
    fun routesAreDeterministicBoundedAndLoopsClose() {
        val loop = schematicRoute("half-dome", loop = true)
        assertEquals(loop, schematicRoute("half-dome", loop = true))
        assertEquals(15, loop.size)
        assertEquals(loop.first(), loop.last())
        val line = schematicRoute("trolltunga", loop = false)
        assertEquals(14, line.size)
        assertTrue(line.zipWithNext().all { (a, b) -> b.first > a.first })
        (loop + line).forEach { (x, y) ->
            assertTrue(x in 0.05f..0.95f, "x=$x")
            assertTrue(y in 0.05f..0.95f, "y=$y")
        }
        assertTrue(schematicRoute("a", loop = true) != schematicRoute("b", loop = true))
    }
}
```

- [ ] **Step 2: Write the failing trail-choice test**

```kotlin
package org.mobilenativefoundation.trails.screen.navigate

import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.data.trail.*

class NavigateTrailChoiceTest {
    private val trail = Trail("preikestolen", "Preikestolen", "Lysefjord, Norway", "Cliff.", TrailDifficulty.MODERATE, 8000, 350, 240, 4.8, 900, setOf(TrailFeature.SUMMIT), 2)
    private val snapshot = SavedSnapshot(
        collections = listOf(TrailCollection("weekend", "Weekend adventures")),
        memberships = mapOf("preikestolen" to setOf("weekend"), "half-dome" to emptySet()),
        trails = listOf(trail), syncByTrail = emptyMap(),
    )

    @Test
    fun lastOpenedWinsThenFirstSavedThenHalfDome() {
        assertEquals("trolltunga", navigateTrailId("trolltunga", snapshot))
        assertEquals("preikestolen", navigateTrailId(null, snapshot))
        assertEquals("half-dome", navigateTrailId(null, snapshot.copy(memberships = emptyMap())))
        assertEquals("half-dome", navigateTrailId(null, null))
    }
}
```

- [ ] **Step 3: Write the failing UI test**

```kotlin
package org.mobilenativefoundation.trails.screen.navigate

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class NavigateUiTest {
    private val trail = Trail("half-dome", "Half Dome", "Yosemite National Park, USA", "Granite summit.", TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486, setOf(TrailFeature.WATERFALL, TrailFeature.SUMMIT), 2)

    @Test
    fun sheetActionsSendTheirIntentsAndTheToastRenders() = runDesktopComposeUiTest {
        val sent = mutableListOf<NavigateIntent>()
        setContent { TrailsTheme { NavigateUi().Content(NavigateState(LoadState(trail, loading = false), toast = "Recording is coming soon") { sent += it }, Modifier) } }
        onNodeWithText("Ready when you are").assertIsDisplayed()
        onNodeWithText("Route preview · schematic").assertIsDisplayed()
        onNodeWithText("Recording is coming soon").assertIsDisplayed()
        onNodeWithText("Start recording").performClick()
        onNodeWithText("Half Dome").performClick()
        assertEquals(listOf(NavigateIntent.StartRecording, NavigateIntent.OpenTrail), sent)
    }
}
```

- [ ] **Step 4: Create the impl module and run to keep the failure**

`multiplatform/screen/navigate/impl/build.gradle.kts`:

```kotlin
plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.screen.navigate.api)
                implementation(projects.multiplatform.foundation.designsystem)
                implementation(libs.kotlinx.coroutines.core)
                implementation(projects.multiplatform.feat.savetrail.api)
                implementation(projects.multiplatform.feat.savetrail.impl)
            }
        }

        jvmTest {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                runtimeOnly(compose.desktop.currentOs)
            }
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.screen.navigate.impl" }
```

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest --tests "*TrailRouteSchematicTest*" :multiplatform:screen:navigate:impl:jvmTest 2>&1 | tee /private/tmp/roots-task4-red.log
```

Expected: compilation fails with `Unresolved reference 'schematicRoute'`.

- [ ] **Step 5: Create the schematic component**

`TrailRouteSchematic.kt`:

```kotlin
package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** Normalized (0..1) route polyline. Deterministic per seed; a loop repeats its first point last. */
fun schematicRoute(seed: String, loop: Boolean, points: Int = 14): List<Pair<Float, Float>> {
    require(points >= 8)
    val random = Random(seed.hashCode())
    return if (loop) {
        val ring = List(points) { index ->
            val angle = 2.0 * PI * index / points
            val radius = 0.30 + random.nextDouble(-0.06, 0.08)
            (0.5 + radius * cos(angle)).toFloat() to (0.5 + radius * 0.85 * sin(angle)).toFloat()
        }
        ring + ring.first()
    } else List(points) { index ->
        val progress = index / (points - 1).toDouble()
        val wander = random.nextDouble(-0.10, 0.10)
        (0.12 + 0.76 * progress).toFloat() to (0.82 - 0.62 * progress + wander).coerceIn(0.08, 0.92).toFloat()
    }
}

/** Route preview without a basemap: halo, route line, start marker and (for non-loops) a citron end marker. Decorative; the screen labels it schematic (DEV-32). */
@Composable
fun TrailRouteSchematic(seed: String, loop: Boolean, modifier: Modifier = Modifier) {
    val colors = TrailsTheme.colors
    val points = remember(seed, loop) { schematicRoute(seed, loop) }
    Canvas(modifier) {
        val path = Path()
        points.forEachIndexed { index, (x, y) ->
            val point = Offset(x * size.width, y * size.height)
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, colors.surface, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, colors.dark, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val start = Offset(points.first().first * size.width, points.first().second * size.height)
        drawCircle(colors.surface, radius = 10.dp.toPx(), center = start)
        drawCircle(colors.dark, radius = 7.dp.toPx(), center = start)
        if (!loop) {
            val end = Offset(points.last().first * size.width, points.last().second * size.height)
            drawCircle(colors.surface, radius = 9.dp.toPx(), center = end)
            drawCircle(colors.citron, radius = 6.dp.toPx(), center = end)
        }
    }
}
```

In `TrailsStatus.kt` change `TrailsToastHost` to:

```kotlin
@Composable
fun TrailsToastHost(toast: TrailsToastData?, onDismissed: () -> Unit, modifier: Modifier = Modifier, durationMillis: Long = 5_000, showCheck: Boolean = true) {
    LaunchedEffect(toast) { if (toast != null) { delay(durationMillis); onDismissed() } }
    if (toast != null) Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        TrailsToast(toast, Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp), showCheck = showCheck)
    }
}
```

- [ ] **Step 6: Presenter**

`NavigatePresenter.kt`:

```kotlin
package org.mobilenativefoundation.trails.screen.navigate

import androidx.compose.runtime.*
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.screen.explore.M1Navigation

const val RECORDING_COMING_SOON = "Recording is coming soon"
internal const val DEFAULT_NAVIGATE_TRAIL = "half-dome"

/** The trail Navigate previews: last opened, else the first saved trail in saved order, else Half Dome. */
internal fun navigateTrailId(lastOpened: String?, saved: SavedSnapshot?): String =
    lastOpened
        ?: saved?.let { snapshot -> snapshot.trails.firstOrNull { snapshot.memberships[it.id]?.isNotEmpty() == true }?.id }
        ?: DEFAULT_NAVIGATE_TRAIL

@Inject
class NavigatePresenter(
    private val repository: TrailRepository,
    private val savedRepository: SavedRepository,
    private val navigation: M1Navigation,
) : Presenter<NavigateState> {
    @Composable
    override fun present(): NavigateState {
        val saved by savedRepository.state.collectAsState()
        val trailId = navigateTrailId(navigation.lastOpenedTrailId, saved.data)
        val trail = key(trailId) {
            remember(trailId) { repository.observeTrail(trailId).catch { failure ->
                if (failure is CancellationException) throw failure
                emit(LoadState(loading = false, error = failure.message ?: "Couldn’t load this trail"))
            } }.collectAsState(initial = LoadState()).value
        }
        var toast by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        var refreshing by remember(trailId) { mutableStateOf(false) }
        return NavigateState(trail.copy(loading = trail.loading || refreshing), toast) { intent ->
            when (intent) {
                NavigateIntent.OpenTrail -> navigation.openTrail(trailId)
                NavigateIntent.StartRecording -> toast = RECORDING_COMING_SOON
                NavigateIntent.DismissToast -> toast = null
                NavigateIntent.Retry -> if (!refreshing) scope.launch {
                    refreshing = true
                    try { repository.refreshTrail(trailId) } finally { refreshing = false }
                }
            }
        }
    }
}
```

- [ ] **Step 7: UI**

`NavigateUi.kt`:

```kotlin
package org.mobilenativefoundation.trails.screen.navigate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.data.trail.TrailFeature
import org.mobilenativefoundation.trails.feat.savetrail.M1Loading
import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@Inject
class NavigateUi : Ui<NavigateState> {
    @Composable
    override fun Content(state: NavigateState, modifier: Modifier) {
        val colors = TrailsTheme.colors
        val typography = TrailsTheme.typography
        val trail = state.trail.data
        var sheetHeight by remember { mutableStateOf(0) }
        val sheetPadding = with(LocalDensity.current) { sheetHeight.toDp() }
        Box(modifier.fillMaxSize().background(colors.soft).semantics { paneTitle = "Navigate" }) {
            if (trail != null) TrailRouteSchematic(trail.id, TrailFeature.LOOP in trail.features, Modifier.fillMaxSize().padding(bottom = sheetPadding))
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    trail != null -> TrailNamePill(trail.name) { state.send(NavigateIntent.OpenTrail) }
                    state.trail.loading -> M1Loading("Finding your trail…")
                    else -> TrailsStatusLine(
                        StatusKind.FAILED, if (state.trail.offline) "Not on this device yet" else "Couldn’t load this trail",
                        actionLabel = "Try again", onAction = { state.send(NavigateIntent.Retry) },
                    )
                }
                Text(
                    "Route preview · schematic", style = typography.labelMedium, color = colors.textSecondary,
                    modifier = Modifier.background(colors.surface, RoundedCornerShape(TrailsTheme.radii.pill)).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { sheetHeight = it.height }
                    .clip(RoundedCornerShape(topStart = TrailsTheme.radii.sheet, topEnd = TrailsTheme.radii.sheet))
                    .background(colors.surface).padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Ready when you are", style = typography.headlineMedium, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                Text("Choose a trail or record your own route.", style = typography.bodyLarge, color = colors.textSecondary)
                TrailsButton("Start recording", { state.send(NavigateIntent.StartRecording) }, Modifier.fillMaxWidth().padding(top = 8.dp), tone = ButtonTone.Hero)
            }
            TrailsToastHost(
                state.toast?.let { TrailsToastData(it) },
                onDismissed = { state.send(NavigateIntent.DismissToast) },
                modifier = Modifier.padding(bottom = sheetPadding),
                showCheck = false,
            )
        }
    }
}

@Composable
private fun TrailNamePill(name: String, onClick: () -> Unit) {
    val colors = TrailsTheme.colors
    val shape = RoundedCornerShape(TrailsTheme.radii.md)
    Row(
        Modifier.fillMaxWidth().shadow(4.dp, shape).clip(shape).background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick).heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Location.painter, contentDescription = null, Modifier.size(20.dp), tint = colors.textPrimary)
        Text(name, style = TrailsTheme.typography.titleSmall, color = colors.textPrimary)
    }
}
```

- [ ] **Step 8: Register the screen**

In `multiplatform/di/graph/active/build.gradle.kts` change `api(projects.multiplatform.screen.navigate.api)` to `api(projects.multiplatform.screen.navigate.impl)`. In `ActiveGraph.kt` add the import `org.mobilenativefoundation.trails.screen.navigate.*`, add the parameter `navigateUi: NavigateUi` to `provideCircuit`, and inside the builder:

```kotlin
        addUi<NavigateScreen, NavigateState> { state, modifier -> navigateUi.Content(state, modifier) }
        addPresenter<NavigateScreen, NavigateState> { _, _, _ -> NavigatePresenter(trails, saved, navigation) }
```

- [ ] **Step 9: Run the tests and compile the app**

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest :multiplatform:screen:navigate:impl:jvmTest :multiplatform:app:core:compileKotlinJvm
```

Expected: the schematic test, the trail-choice test and the UI test pass; the existing design-system tests stay green; the app compiles.

- [ ] **Step 10: Commit**

```bash
git add multiplatform/foundation/designsystem multiplatform/screen/navigate multiplatform/di/graph/active
git commit -m "feat(navigate): preview the current trail's route with a coming-soon recording action

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 5: Activity root

**Files:**
- Create: `multiplatform/screen/activity/impl/build.gradle.kts`, `…/screen/activity/ActivitySummary.kt`, `…/screen/activity/ActivityPresenter.kt`, `…/screen/activity/ActivityUi.kt`
- Modify: `multiplatform/di/graph/active/build.gradle.kts`, `…/ActiveGraph.kt`
- Test: `multiplatform/screen/activity/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/screen/activity/ActivitySummaryTest.kt`
- Test: `multiplatform/screen/activity/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/screen/activity/ActivityUiTest.kt`

**Interfaces:**
- Produces: `data class MonthSummary(monthLabel: String, distanceMeters: Int, trails: Int, minutesOutside: Int, lastSevenDaysMeters: List<Int>)`; `fun monthSummary(activities: List<CompletedActivity>, nowEpochMillis: Long, zone: TimeZone): MonthSummary`; `fun activityDateLabel(completedAtEpochMillis: Long, nowEpochMillis: Long, zone: TimeZone): String`; `fun activitySummaryLine(activity: CompletedActivity, nowEpochMillis: Long, zone: TimeZone): String`; `fun kilometres(meters: Int): String`.
- Consumes: Task 2 `ActivityRepository`, Task 3 contracts, `TrailPhoto`, `TrailBookmark`, `TrailsButton`, `TrailsStatusLine`, `M1Loading`, `rememberCheckpointedListState`, `trailDistance`, `trailDuration`.

- [ ] **Step 1: Create the impl module**

`multiplatform/screen/activity/impl/build.gradle.kts` is the Navigate module file with `api(projects.multiplatform.screen.activity.api)`, the added line `implementation(libs.kotlinx.datetime)` in `commonMain`, and namespace `org.mobilenativefoundation.trails.screen.activity.impl`.

- [ ] **Step 2: Write the failing summary test**

```kotlin
package org.mobilenativefoundation.trails.screen.activity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.TimeZone
import org.mobilenativefoundation.trails.data.trail.CompletedActivity

class ActivitySummaryTest {
    private val zone = TimeZone.UTC
    private val now = 1_789_905_600_000L // 2026-09-20T12:00:00Z
    private val day = 86_400_000L
    private fun activity(id: String, daysAgo: Int, meters: Int, minutes: Int) =
        CompletedActivity(id, id, id, now - daysAgo * day, meters, minutes, 100)

    @Test
    fun monthTotalsAndBarsFollowTheCalendarMonthAndTheLastSevenDays() {
        val summary = monthSummary(listOf(activity("a", 1, 22_700, 660), activity("b", 3, 7_600, 180), activity("c", 40, 9_000, 200)), now, zone)
        assertEquals("September so far", summary.monthLabel)
        assertEquals(30_300, summary.distanceMeters)
        assertEquals(2, summary.trails)
        assertEquals(840, summary.minutesOutside)
        assertEquals(listOf(0, 0, 0, 7_600, 0, 22_700, 0), summary.lastSevenDaysMeters)
    }

    @Test
    fun dateLabelsAndSummaryLines() {
        assertEquals("Today", activityDateLabel(now - 3_600_000L, now, zone))
        assertEquals("Yesterday", activityDateLabel(now - day, now, zone))
        assertEquals("Sep 17", activityDateLabel(now - 3 * day, now, zone))
        assertEquals("Aug 11", activityDateLabel(now - 40 * day, now, zone))
        assertEquals("Yesterday · 22.7 km · 11 h 0 min", activitySummaryLine(activity("a", 1, 22_700, 660), now, zone))
        assertEquals("30.3", kilometres(30_300))
    }
}
```

- [ ] **Step 3: Write the failing UI test**

```kotlin
package org.mobilenativefoundation.trails.screen.activity

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class ActivityUiTest {
    private val now = 1_789_905_600_000L
    private val halfDome = Trail("half-dome", "Half Dome", "Yosemite National Park, USA", "Granite.", TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486, setOf(TrailFeature.SUMMIT), 2)
    private val activities = listOf(
        CompletedActivity("sample-half-dome", "half-dome", "Half Dome", now - 86_400_000L, 22700, 660, 1463),
        CompletedActivity("sample-takao", "mount-takao-trail-1", "Mount Takao Trail 1", now - 3 * 86_400_000L, 7600, 180, 411),
    )

    @Test
    fun historyRendersTotalsHeroAndRowsAndRowsOpenTheirTrail() = runDesktopComposeUiTest {
        val sent = mutableListOf<ActivityIntent>()
        val state = ActivityState(LoadState(activities, loading = false), mapOf(halfDome.id to halfDome), LoadState(loading = false), now) { sent += it }
        setContent { TrailsTheme { ActivityUi().Content(state, Modifier) } }
        onNodeWithText("Activity").assertIsDisplayed()
        onNodeWithText("Your recent adventures").assertIsDisplayed()
        onNodeWithText("Half Dome").assertIsDisplayed()
        onNodeWithText("Mount Takao Trail 1").performScrollTo().performClick()
        assertEquals(ActivityIntent.OpenTrail("mount-takao-trail-1"), sent.filterIsInstance<ActivityIntent.OpenTrail>().single())
    }

    @Test
    fun emptyHistoryOffersExplore() = runDesktopComposeUiTest {
        val sent = mutableListOf<ActivityIntent>()
        setContent { TrailsTheme { ActivityUi().Content(ActivityState(LoadState(emptyList(), loading = false), emptyMap(), LoadState(loading = false), now) { sent += it }, Modifier) } }
        onNodeWithText("Your hikes will show up here").assertIsDisplayed()
        onNodeWithText("Explore trails").performScrollTo().performClick()
        assertEquals(ActivityIntent.Explore, sent.filterIsInstance<ActivityIntent.Explore>().single())
    }
}
```

- [ ] **Step 4: Run and keep the failure**

```bash
./gradlew :multiplatform:screen:activity:impl:jvmTest 2>&1 | tee /private/tmp/roots-task5-red.log
```

Expected: compilation fails with `Unresolved reference 'monthSummary'`.

- [ ] **Step 5: Summary functions**

`ActivitySummary.kt`:

```kotlin
@file:OptIn(kotlin.time.ExperimentalTime::class)

package org.mobilenativefoundation.trails.screen.activity

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.mobilenativefoundation.trails.data.trail.CompletedActivity
import org.mobilenativefoundation.trails.feat.savetrail.trailDistance
import org.mobilenativefoundation.trails.feat.savetrail.trailDuration

data class MonthSummary(val monthLabel: String, val distanceMeters: Int, val trails: Int, val minutesOutside: Int, val lastSevenDaysMeters: List<Int>)

internal fun localDate(epochMillis: Long, zone: TimeZone): LocalDate = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date

private fun monthName(date: LocalDate): String = date.month.name.lowercase().replaceFirstChar { it.uppercase() }

/** Totals for the calendar month of [nowEpochMillis]; the bars are the last seven local days ending today. */
fun monthSummary(activities: List<CompletedActivity>, nowEpochMillis: Long, zone: TimeZone): MonthSummary {
    val today = localDate(nowEpochMillis, zone)
    val dated = activities.map { it to localDate(it.completedAtEpochMillis, zone) }
    val thisMonth = dated.filter { (_, date) -> date.year == today.year && date.month == today.month }
    return MonthSummary(
        monthLabel = "${monthName(today)} so far",
        distanceMeters = thisMonth.sumOf { it.first.distanceMeters },
        trails = thisMonth.map { it.first.trailId }.distinct().size,
        minutesOutside = thisMonth.sumOf { it.first.durationMinutes },
        lastSevenDaysMeters = (6 downTo 0).map { back -> dated.filter { (_, date) -> date.daysUntil(today) == back }.sumOf { it.first.distanceMeters } },
    )
}

fun activityDateLabel(completedAtEpochMillis: Long, nowEpochMillis: Long, zone: TimeZone): String {
    val date = localDate(completedAtEpochMillis, zone)
    return when (date.daysUntil(localDate(nowEpochMillis, zone))) {
        0 -> "Today"
        1 -> "Yesterday"
        else -> "${monthName(date).take(3)} ${date.day}"
    }
}

fun activitySummaryLine(activity: CompletedActivity, nowEpochMillis: Long, zone: TimeZone): String =
    "${activityDateLabel(activity.completedAtEpochMillis, nowEpochMillis, zone)} · ${trailDistance(activity.distanceMeters)} · ${trailDuration(activity.durationMinutes)}"

/** Whole kilometres with one decimal, unit supplied by the caller's label. */
fun kilometres(meters: Int): String = "${meters / 1000}.${(meters % 1000) / 100}"
```

- [ ] **Step 6: Presenter**

`ActivityPresenter.kt`:

```kotlin
@file:OptIn(kotlin.time.ExperimentalTime::class)

package org.mobilenativefoundation.trails.screen.activity

import androidx.compose.runtime.*
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.feat.savetrail.SaveTrailFeature
import org.mobilenativefoundation.trails.screen.explore.M1Navigation

@Inject
class ActivityPresenter(
    private val activities: ActivityRepository,
    private val trails: TrailRepository,
    private val savedRepository: SavedRepository,
    private val navigation: M1Navigation,
    private val saves: SaveTrailFeature,
) : Presenter<ActivityState> {
    @Composable
    override fun present(): ActivityState {
        val history = remember { activities.observe().catch { failure ->
            if (failure is CancellationException) throw failure
            emit(LoadState(loading = false, error = failure.message ?: "Couldn’t load your activity"))
        } }.collectAsState(initial = LoadState()).value
        val catalog = remember { trails.observeQuery(TrailQuery()).catch { failure ->
            if (failure is CancellationException) throw failure
            emit(LoadState(loading = false))
        } }.collectAsState(initial = LoadState()).value
        val saved by savedRepository.state.collectAsState()
        val now = remember(history.data) { Clock.System.now().toEpochMilliseconds() }
        val scope = rememberCoroutineScope()
        var refreshing by remember { mutableStateOf(false) }
        return ActivityState(history.copy(loading = history.loading || refreshing), catalog.data.orEmpty().associateBy { it.id }, saved, now, navigation.scrollPosition("ACTIVITY/activity")) { intent ->
            when (intent) {
                is ActivityIntent.ScrollChanged -> navigation.checkpointScroll("ACTIVITY/activity", intent.position)
                is ActivityIntent.OpenTrail -> navigation.openTrail(intent.trailId)
                is ActivityIntent.SaveTrail -> saves.open(intent.trail, intent.onDismiss)
                ActivityIntent.Explore -> navigation.selectExplore()
                ActivityIntent.Retry -> if (!refreshing) scope.launch {
                    refreshing = true
                    try { activities.refresh() } finally { refreshing = false }
                }
            }
        }
    }
}
```

- [ ] **Step 7: UI**

`ActivityUi.kt`:

```kotlin
package org.mobilenativefoundation.trails.screen.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import kotlinx.datetime.TimeZone
import org.mobilenativefoundation.trails.data.trail.CompletedActivity
import org.mobilenativefoundation.trails.feat.savetrail.*
import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.screen.explore.M1ScrollPosition

@Inject
class ActivityUi : Ui<ActivityState> {
    @Composable
    override fun Content(state: ActivityState, modifier: Modifier) {
        val colors = TrailsTheme.colors
        val typography = TrailsTheme.typography
        val zone = remember { TimeZone.currentSystemDefault() }
        val history = state.history.data
        val scroll = rememberCheckpointedListState(state.initialScroll.index, state.initialScroll.offset, history != null) { index, offset ->
            state.send(ActivityIntent.ScrollChanged(M1ScrollPosition(index, offset)))
        }
        LazyColumn(
            modifier.fillMaxSize().background(colors.background).semantics { paneTitle = "Activity" }, state = scroll,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "heading") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Activity", style = typography.displayMedium, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                    Text("A little progress. A lot of fresh air.", style = typography.bodyLarge, color = colors.textSecondary)
                }
            }
            if (history == null && state.history.loading) item(key = "loading") { M1Loading("Opening your activity…") }
            if (history == null && !state.history.loading) item(key = "unavailable") {
                TrailsStatusLine(
                    if (state.history.offline) StatusKind.OFFLINE else StatusKind.FAILED,
                    if (state.history.offline) "Offline · Activity isn’t on this device yet" else "Couldn’t load your activity",
                    actionLabel = "Try again", onAction = { state.send(ActivityIntent.Retry) },
                )
            }
            if (history != null) {
                item(key = "month") { MonthCard(monthSummary(history, state.nowEpochMillis, zone)) }
                if (state.history.error != null) item(key = "refresh-error") {
                    TrailsStatusLine(StatusKind.FAILED, "Couldn’t refresh · Showing this device’s copy", actionLabel = if (state.history.loading) null else "Try again", onAction = { state.send(ActivityIntent.Retry) })
                }
                item(key = "recent") { Text("Your recent adventures", style = typography.titleLarge, color = colors.textPrimary, modifier = Modifier.semantics { heading() }) }
                if (history.isEmpty()) item(key = "empty") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Your hikes will show up here", style = typography.titleSmall, color = colors.textPrimary)
                        Text("Find a trail worth walking and it’ll be waiting for you here afterwards.", style = typography.bodyLarge, color = colors.textSecondary)
                        TrailsButton("Explore trails", { state.send(ActivityIntent.Explore) }, Modifier.fillMaxWidth(), tone = ButtonTone.Commit)
                    }
                }
                val sorted = history.sortedByDescending { it.completedAtEpochMillis }
                sorted.firstOrNull()?.let { latest -> item(key = "hero-${latest.id}") { ActivityHero(latest, state, zone) } }
                items(sorted.drop(1), key = { it.id }) { activity -> ActivityRow(activity, state, zone) }
            }
        }
    }
}

@Composable
private fun MonthCard(summary: MonthSummary) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(TrailsTheme.radii.card)).background(colors.dark).padding(20.dp).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(summary.monthLabel.uppercase(), style = typography.labelMedium, color = colors.citron)
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            MonthStat(kilometres(summary.distanceMeters), "km walked")
            MonthStat("${summary.trails}", if (summary.trails == 1) "trail" else "trails")
            MonthStat("${summary.minutesOutside / 60}h", "outside")
        }
        LastSevenDaysBars(summary.lastSevenDaysMeters)
    }
}

@Composable
private fun MonthStat(value: String, label: String) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = typography.headlineMedium, color = colors.onDark)
        Text(label, style = typography.bodySmall, color = colors.onDark.copy(alpha = 0.72f))
    }
}

/** Seven bars, oldest first; the most recent day with activity is citron. Decorative: the stats above carry the numbers. */
@Composable
private fun LastSevenDaysBars(values: List<Int>) {
    val colors = TrailsTheme.colors
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val highlight = values.indexOfLast { it > 0 }
    Canvas(Modifier.fillMaxWidth().height(56.dp)) {
        val gap = 8.dp.toPx()
        val width = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { index, meters ->
            val height = size.height * (0.16f + 0.84f * meters / max.toFloat())
            val color = when {
                index == highlight -> colors.citron
                meters > 0 -> colors.onDark.copy(alpha = 0.55f)
                else -> colors.onDark.copy(alpha = 0.18f)
            }
            drawRoundRect(color, topLeft = Offset(index * (width + gap), size.height - height), size = Size(width, height), cornerRadius = CornerRadius(6.dp.toPx()))
        }
    }
}

@Composable
private fun ActivityHero(activity: CompletedActivity, state: ActivityState, zone: TimeZone) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val trail = state.trails[activity.trailId]
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { state.send(ActivityIntent.OpenTrail(activity.trailId)) }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(TrailsTheme.radii.lg))) {
            TrailPhoto(activity.trailId, Modifier.fillMaxSize())
            if (trail != null) TrailBookmark(trail.name, state.saved.data?.memberships?.get(trail.id)?.isNotEmpty(), { onDismiss -> state.send(ActivityIntent.SaveTrail(trail, onDismiss)) }, Modifier.align(Alignment.TopEnd).padding(12.dp))
        }
        Text(activity.trailName, style = typography.titleSmall.copy(fontSize = 18.sp, lineHeight = 24.sp), color = colors.textPrimary)
        Text(activitySummaryLine(activity, state.nowEpochMillis, zone), style = typography.bodyMedium, color = colors.textSecondary)
    }
}

@Composable
private fun ActivityRow(activity: CompletedActivity, state: ActivityState, zone: TimeZone) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val trail = state.trails[activity.trailId]
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { state.send(ActivityIntent.OpenTrail(activity.trailId)) }.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            TrailPhoto(activity.trailId, Modifier.size(64.dp).clip(RoundedCornerShape(TrailsTheme.radii.md)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(activity.trailName, style = typography.titleSmall, color = colors.textPrimary)
                Text(activitySummaryLine(activity, state.nowEpochMillis, zone), style = typography.bodyMedium, color = colors.textSecondary)
            }
            if (trail != null) TrailBookmark(trail.name, state.saved.data?.memberships?.get(trail.id)?.isNotEmpty(), { onDismiss -> state.send(ActivityIntent.SaveTrail(trail, onDismiss)) })
        }
        HorizontalDivider(color = colors.border)
    }
}
```

- [ ] **Step 8: Register the screen**

In `multiplatform/di/graph/active/build.gradle.kts` change `api(projects.multiplatform.screen.activity.api)` to `api(projects.multiplatform.screen.activity.impl)`. In `ActiveGraph.kt` add the import `org.mobilenativefoundation.trails.screen.activity.*`, the parameters `activities: ActivityRepository` and `activityUi: ActivityUi` to `provideCircuit`, and:

```kotlin
        addUi<ActivityScreen, ActivityState> { state, modifier -> activityUi.Content(state, modifier) }
        addPresenter<ActivityScreen, ActivityState> { _, _, _ -> ActivityPresenter(activities, trails, saved, navigation, saves) }
```

- [ ] **Step 9: Run the tests and compile the app**

```bash
./gradlew :multiplatform:screen:activity:impl:jvmTest :multiplatform:app:core:compileKotlinJvm
```

Expected: 4 tests pass; the app compiles.

- [ ] **Step 10: Commit**

```bash
git add multiplatform/screen/activity multiplatform/di/graph/active
git commit -m "feat(activity): show sample history with monthly totals and recent adventures

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 6: For You root

**Files:**
- Create: `multiplatform/screen/foryou/impl/build.gradle.kts`, `…/screen/foryou/ForYouPresenter.kt`, `…/screen/foryou/ForYouUi.kt`
- Modify: `multiplatform/di/graph/active/build.gradle.kts`, `…/ActiveGraph.kt`
- Test: `multiplatform/screen/foryou/impl/src/jvmTest/kotlin/org/mobilenativefoundation/trails/screen/foryou/ForYouUiTest.kt`

**Interfaces:**
- Consumes: Task 2 `ForYouRepository`, Task 3 contracts, `TrailPhoto`, `TrailFactsRow(showCount = false)`, `TrailBookmark`, `TrailsCompass`, `TrailsStatusLine`, `M1Loading`.

- [ ] **Step 1: Create the impl module**

`multiplatform/screen/foryou/impl/build.gradle.kts` is the Navigate module file with `api(projects.multiplatform.screen.foryou.api)` and namespace `org.mobilenativefoundation.trails.screen.foryou.impl`.

- [ ] **Step 2: Write the failing UI test**

```kotlin
package org.mobilenativefoundation.trails.screen.foryou

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class ForYouUiTest {
    private val trolltunga = Trail("trolltunga", "Trolltunga", "Hardangerfjord, Norway", "Ledge.", TrailDifficulty.HARD, 27000, 800, 600, 4.9, 1964, setOf(TrailFeature.LAKE), 2)
    private val halfDome = Trail("half-dome", "Half Dome", "Yosemite National Park, USA", "Granite.", TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486, setOf(TrailFeature.SUMMIT), 2)
    private val mist = Trail("mist-trail-to-nevada-fall", "Mist Trail to Nevada Fall", "Yosemite National Park, USA", "Falls.", TrailDifficulty.HARD, 8700, 610, 330, 4.8, 1200, setOf(TrailFeature.WATERFALL), 0)
    private val feed = ForYouFeed("trolltunga", "A weekend worth the walk", "Discover a quieter side of outside.", "half-dome", listOf("mist-trail-to-nevada-fall", "missing-trail"))

    @Test
    fun featureCardSectionAndRowsRenderAndOpenTheirTrails() = runDesktopComposeUiTest {
        val sent = mutableListOf<ForYouIntent>()
        val state = ForYouState(LoadState(feed, loading = false), listOf(trolltunga, halfDome, mist).associateBy { it.id }, LoadState(loading = false)) { sent += it }
        setContent { TrailsTheme { ForYouUi().Content(state, Modifier) } }
        onNodeWithText("For you").assertIsDisplayed()
        onNodeWithText("A weekend worth the walk").assertIsDisplayed()
        onNodeWithText("More like Half Dome").assertIsDisplayed()
        onNodeWithText("Mist Trail to Nevada Fall").performScrollTo().performClick()
        onNodeWithText("A weekend worth the walk").performScrollTo().performClick()
        assertEquals(listOf(mist, trolltunga), sent.filterIsInstance<ForYouIntent.OpenTrail>().map { it.trail })
    }
}
```

- [ ] **Step 3: Run and keep the failure**

```bash
./gradlew :multiplatform:screen:foryou:impl:jvmTest 2>&1 | tee /private/tmp/roots-task6-red.log
```

Expected: compilation fails with `Unresolved reference 'ForYouUi'`.

- [ ] **Step 4: Presenter**

`ForYouPresenter.kt`:

```kotlin
package org.mobilenativefoundation.trails.screen.foryou

import androidx.compose.runtime.*
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.feat.savetrail.SaveTrailFeature
import org.mobilenativefoundation.trails.screen.explore.M1Navigation

@Inject
class ForYouPresenter(
    private val feed: ForYouRepository,
    private val trails: TrailRepository,
    private val savedRepository: SavedRepository,
    private val navigation: M1Navigation,
    private val saves: SaveTrailFeature,
) : Presenter<ForYouState> {
    @Composable
    override fun present(): ForYouState {
        val content = remember { feed.observe().catch { failure ->
            if (failure is CancellationException) throw failure
            emit(LoadState(loading = false, error = failure.message ?: "Couldn’t load your picks"))
        } }.collectAsState(initial = LoadState()).value
        val catalog = remember { trails.observeQuery(TrailQuery()).catch { failure ->
            if (failure is CancellationException) throw failure
            emit(LoadState(loading = false))
        } }.collectAsState(initial = LoadState()).value
        val saved by savedRepository.state.collectAsState()
        val scope = rememberCoroutineScope()
        var refreshing by remember { mutableStateOf(false) }
        return ForYouState(content.copy(loading = content.loading || refreshing), catalog.data.orEmpty().associateBy { it.id }, saved, navigation.scrollPosition("FOR_YOU/foryou")) { intent ->
            when (intent) {
                is ForYouIntent.ScrollChanged -> navigation.checkpointScroll("FOR_YOU/foryou", intent.position)
                is ForYouIntent.OpenTrail -> navigation.openTrail(intent.trail.id)
                is ForYouIntent.SaveTrail -> saves.open(intent.trail, intent.onDismiss)
                ForYouIntent.Retry -> if (!refreshing) scope.launch {
                    refreshing = true
                    try { feed.refresh() } finally { refreshing = false }
                }
            }
        }
    }
}
```

- [ ] **Step 5: UI**

`ForYouUi.kt`:

```kotlin
package org.mobilenativefoundation.trails.screen.foryou

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.data.trail.ForYouFeed
import org.mobilenativefoundation.trails.data.trail.Trail
import org.mobilenativefoundation.trails.feat.savetrail.*
import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.screen.explore.M1ScrollPosition

@Inject
class ForYouUi : Ui<ForYouState> {
    @Composable
    override fun Content(state: ForYouState, modifier: Modifier) {
        val colors = TrailsTheme.colors
        val typography = TrailsTheme.typography
        val feed = state.feed.data
        val scroll = rememberCheckpointedListState(state.initialScroll.index, state.initialScroll.offset, feed != null) { index, offset ->
            state.send(ForYouIntent.ScrollChanged(M1ScrollPosition(index, offset)))
        }
        LazyColumn(
            modifier.fillMaxSize().background(colors.background).semantics { paneTitle = "For you" }, state = scroll,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "heading") { Text("For you", style = typography.displayMedium, color = colors.textPrimary, modifier = Modifier.semantics { heading() }) }
            if (feed == null && state.feed.loading) item(key = "loading") { M1Loading("Finding your picks…") }
            if (feed == null && !state.feed.loading) item(key = "unavailable") {
                TrailsStatusLine(
                    if (state.feed.offline) StatusKind.OFFLINE else StatusKind.FAILED,
                    if (state.feed.offline) "Offline · Picks aren’t on this device yet" else "Couldn’t load your picks",
                    actionLabel = "Try again", onAction = { state.send(ForYouIntent.Retry) },
                )
            }
            if (feed != null) {
                item(key = "feature") { FeatureCard(feed, state.trails[feed.featuredTrailId]) { trail -> state.send(ForYouIntent.OpenTrail(trail)) } }
                if (state.feed.error != null) item(key = "refresh-error") {
                    TrailsStatusLine(StatusKind.FAILED, "Couldn’t refresh · Showing this device’s copy", actionLabel = if (state.feed.loading) null else "Try again", onAction = { state.send(ForYouIntent.Retry) })
                }
                item(key = "section") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Based on your activity", style = typography.titleLarge, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                        Text("More like ${state.trails[feed.anchorTrailId]?.name ?: "your last hike"}", style = typography.bodyMedium, color = colors.textSecondary)
                    }
                }
                val rows = feed.recommendedTrailIds.mapNotNull { state.trails[it] }
                if (rows.isEmpty()) item(key = "rows-missing") { TrailsStatusLine(StatusKind.INFO, "Trail details aren’t on this device yet") }
                items(rows, key = { it.id }) { trail ->
                    RecommendationRow(trail, state.saved.data?.memberships?.get(trail.id)?.isNotEmpty(),
                        onOpen = { state.send(ForYouIntent.OpenTrail(trail)) }, onSave = { onDismiss -> state.send(ForYouIntent.SaveTrail(trail, onDismiss)) })
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(feed: ForYouFeed, trail: Trail?, onOpen: (Trail) -> Unit) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Box(
        Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(TrailsTheme.radii.card))
            .clickable(enabled = trail != null, role = Role.Button) { trail?.let(onOpen) }
            .semantics(mergeDescendants = true) {},
    ) {
        TrailPhoto(feed.featuredTrailId, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.dark.copy(alpha = 0.10f), colors.dark.copy(alpha = 0.60f)))))
        Row(Modifier.align(Alignment.TopStart).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            TrailsCompass(Modifier.size(22.dp), tint = colors.onDark)
            Text("trails", style = typography.displayLarge.copy(fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.96).sp), color = colors.onDark)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(feed.headline, style = typography.headlineMedium, color = colors.onDark)
            Text(feed.subline, style = typography.bodyMedium, color = colors.onDark.copy(alpha = 0.78f))
        }
    }
}

@Composable
private fun RecommendationRow(trail: Trail, saved: Boolean?, onOpen: () -> Unit, onSave: (() -> Unit) -> Unit) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            TrailPhoto(trail.id, Modifier.size(64.dp).clip(RoundedCornerShape(TrailsTheme.radii.md)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(trail.name, style = typography.titleSmall, color = colors.textPrimary)
                TrailFactsRow(trail, showCount = false)
            }
            TrailBookmark(trail.name, saved, onSave)
        }
        HorizontalDivider(color = colors.border)
    }
}
```

- [ ] **Step 6: Register the screen**

In `multiplatform/di/graph/active/build.gradle.kts` change `api(projects.multiplatform.screen.foryou.api)` to `api(projects.multiplatform.screen.foryou.impl)`. In `ActiveGraph.kt` add the import `org.mobilenativefoundation.trails.screen.foryou.*`, the parameters `forYou: ForYouRepository` and `forYouUi: ForYouUi` to `provideCircuit`, and:

```kotlin
        addUi<ForYouScreen, ForYouState> { state, modifier -> forYouUi.Content(state, modifier) }
        addPresenter<ForYouScreen, ForYouState> { _, _, _ -> ForYouPresenter(forYou, trails, saved, navigation, saves) }
```

- [ ] **Step 7: Run the tests and compile the app**

```bash
./gradlew :multiplatform:screen:foryou:impl:jvmTest :multiplatform:app:core:compileKotlinJvm
```

Expected: 1 test passes; the app compiles.

- [ ] **Step 8: Commit**

```bash
git add multiplatform/screen/foryou multiplatform/di/graph/active
git commit -m "feat(foryou): render the feature card and recommendation rows from the seeded fixture

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 7: Five-root shell and the sample-data disclosure

**Files:**
- Modify: `multiplatform/app/core/src/commonMain/kotlin/org/mobilenativefoundation/trails/app/M1Content.kt`
- Modify: `multiplatform/screen/welcome/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/welcome/WelcomeUi.kt`

**Interfaces:**
- Consumes: Task 3 `Root`, `allStacks`, `selectForYou/selectNavigate/selectActivity`; `TrailsDestination.entries` (order EXPLORE, FOR_YOU, NAVIGATE, SAVED, ACTIVITY).

- [ ] **Step 1: Render all five destinations and checkpoint every stack**

In `M1Content.kt` (the `Root` import already exists) replace the `LaunchedEffect` with:

```kotlin
    LaunchedEffect(navigation) {
        snapshotFlow { navigation.selectedRoot to navigation.allStacks.map { stack -> stack.map { it.screen } } }
            .collect { navigation.checkpoint() }
    }
```

replace the `bottomBar` argument with:

```kotlin
        bottomBar = {
            TrailsFloatingNav(
                items = TrailsDestination.entries,
                selected = navigation.selectedRoot.destination(),
                onSelect = { destination ->
                    when (destination) {
                        TrailsDestination.EXPLORE -> navigation.selectExplore()
                        TrailsDestination.FOR_YOU -> navigation.selectForYou()
                        TrailsDestination.NAVIGATE -> navigation.selectNavigate()
                        TrailsDestination.SAVED -> navigation.selectSavedTab()
                        TrailsDestination.ACTIVITY -> navigation.selectActivity()
                    }
                },
            )
        },
```

and add at the bottom of the file:

```kotlin
private fun Root.destination(): TrailsDestination = when (this) {
    Root.EXPLORE -> TrailsDestination.EXPLORE
    Root.FOR_YOU -> TrailsDestination.FOR_YOU
    Root.NAVIGATE -> TrailsDestination.NAVIGATE
    Root.SAVED -> TrailsDestination.SAVED
    Root.ACTIVITY -> TrailsDestination.ACTIVITY
}
```

- [ ] **Step 2: Disclose the sample data on Welcome**

In `WelcomeUi.kt` replace the string `Real trails and location photography. Ratings and reviews are sample data.` with `Real trails and location photography. Ratings, reviews, activity history and recommendations are sample data.`

- [ ] **Step 3: Run the app-core tests, the whole JVM gate and the Android assembly**

```bash
./gradlew :multiplatform:app:core:jvmTest :multiplatform:app:core:compileKotlinJvm :multiplatform:screen:welcome:impl:compileKotlinJvm :apps:android:assembleDebug 2>&1 | tee /private/tmp/roots-task7-gate.log
```

Expected: `BUILD SUCCESSFUL`; `M1ScrollCheckpointTest` and `DeveloperToolsDrawerHostTest` still pass; the APK assembles with the five-root shell.

- [ ] **Step 4: Commit**

```bash
git add multiplatform/app/core multiplatform/screen/welcome
git commit -m "feat(app): render all five roots in the floating navigation

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 8: Verification, installed evidence and ledger

**Execution amendment approved September 20, 2026:** The owner authorized one additional bounded fix-and-review round after final review F-I3. The original cached Activity Offline `Try again` requirement remains binding. Add developer-only refresh tooling as needed to reach the genuine Activity retry/read path, measure the actual installed action, and resolve any mismatch with cached content retention and the no-failure-line expectation. This authorization does not waive the acceptance check or permit fabricated failures, data clearing, or reseeding.

**Files:**
- Create: `docs/evidence/roots/README.md` (plus captures under `docs/evidence/roots/`)
- Modify: `docs/trails-reference-deviations.md` (evidence column of DEV-31…DEV-37)
- Modify: `docs/superpowers/plans/2026-09-14-trails-v25-store6-completion.md` (tick 8h)

**Interfaces:** Consumes every earlier task; produces the roots acceptance record referenced by the master plan.

- [ ] **Step 1: Run the complete JVM gate**

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest :multiplatform:data:trail:impl:jvmTest :multiplatform:feat:savetrail:impl:jvmTest :multiplatform:feat:filters:impl:jvmTest :multiplatform:screen:explore:impl:jvmTest :multiplatform:screen:traildetail:impl:jvmTest :multiplatform:screen:saved:impl:jvmTest :multiplatform:screen:navigate:impl:jvmTest :multiplatform:screen:activity:impl:jvmTest :multiplatform:screen:foryou:impl:jvmTest :multiplatform:di:graph:active:jvmTest :multiplatform:app:bootstrap:impl:jvmTest :multiplatform:app:core:jvmTest :multiplatform:app:core:compileKotlinJvm 2>&1 | tee /private/tmp/roots-jvm-gate.log
```

Expected: `BUILD SUCCESSFUL`; record the executed test count per module from `build/test-results/jvmTest/*.xml`.

- [ ] **Step 2: Assemble, install and record the APK identity**

```bash
./gradlew :apps:android:assembleDebug 2>&1 | tee /private/tmp/roots-assemble.log
shasum -a 256 apps/android/build/outputs/apk/debug/android-debug.apk
$HOME/Library/Android/sdk/platform-tools/adb devices
$HOME/Library/Android/sdk/platform-tools/adb -s emulator-5554 install -r apps/android/build/outputs/apk/debug/android-debug.apk
```

Do not wipe data: the emulator carries the R2 installation, whose checkpoint predates the new roots and must restore (Task 3's compatibility claim, proven on device).

- [ ] **Step 3: Exercise the three roots on the installed build**

Follow the capture process of `docs/evidence/r2/README.md` (the helper `docs/evidence/r2/tools/r2-emulator.py`, copied to `docs/evidence/roots/tools/`, writing under `docs/evidence/roots/android/`). Record, with numbered PNG/XML pairs and the first failure of anything that breaks:

1. Cold launch after the upgrade: the pill shows five items; Explore restores its previous state; the previous `docs/evidence/r2` checkpoint restored without a `Couldn’t restore your place` line.
2. For You (cell 10): feature card, `More like Half Dome`, six rows with facts and hearts; tap a row → its detail; Back; heart on a row → save sheet → `Saved to Weekend adventures` toast; Offline in the developer drawer → For You still shows the cached feed with no error line; restart the app offline → the feed is still there.
3. Navigate (cell 11): the name pill shows the last opened trail; `Route preview · schematic` visible; tap the pill → detail; Back; tap `Start recording` → `Recording is coming soon` toast, no tick, gone after 5 s; open a different trail from Explore, return to Navigate → the pill follows it.
4. Activity (cell 12): month card numbers equal the fixture's current-month sum (compute them from `database/` evidence of the seeded rows: read the backend database's `cache_row` with namespace `backend-activities`); hero shows `Yesterday · 22.7 km · 11 h 0 min`; rows open their trail; heart works; Offline → cached; `Try again` while offline keeps the content and shows no failure for the cached feed.
5. Restart with each new root selected: the selected root and its stack (a pushed detail) restore.
6. 200 % font scale on all three roots; uiautomator trees showing labels, roles and 48 dp bounds for the name pill, Start recording, the hearts and the five pill items. State plainly that TalkBack was not run if it was not.

- [ ] **Step 4: Paired captures**

Export F02 `148:55`, F07 `148:71` and F25 `236:2258` at 390 × 844 (Figma MCP `get_screenshot`, or `pending controller export` if no Figma access) to `docs/evidence/roots/screenshots/f02-figma.png`, `f07-figma.png`, `f25-figma.png`, and capture the app in the same states as `f02-android.png`, `f07-android.png`, `f25-android.png`. Note the observed differences per pair (the schematic route, the omitted avatar and map circles, the sample dates).

- [ ] **Step 5: Write the evidence README**

Create `docs/evidence/roots/README.md` with the sections `Branch and commits`, `JVM gate` (command, per-module counts, first-failure logs `/private/tmp/roots-task*-red.log`), `Build and install` (APK SHA-256, device, upgrade without wipe), `Checkpoint upgrade` (the pre-existing checkpoint restored), `Cells 10–12` (captures and results), `Paired comparisons`, `Accessibility and 200 % font scale`, `Deviations` (DEV-31…DEV-37 evidence), `Boundaries and first failures`. Every value is measured in this task; leave nothing unfilled except `pending controller export`.

- [ ] **Step 6: Update the ledger and the master plan**

Fill the evidence column of DEV-31…DEV-37 in `docs/trails-reference-deviations.md` with links into `docs/evidence/roots/`. In the master plan tick `8h` and link the README.

- [ ] **Step 7: Commit**

```bash
git add docs/evidence/roots docs/trails-reference-deviations.md docs/superpowers/plans/2026-09-14-trails-v25-store6-completion.md
git commit -m "docs(evidence): record the remaining roots preview verification

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

## Self-review

- **Spec coverage:** Navigate F25 with schematic route, name pill, sheet copy and the coming-soon toast (Task 4); Activity F07 month card, bars, hero, rows, empty and status states (Task 5); For You F02 feature card, section, rows, hearts (Task 6); seeded per-account fixtures with no schema change (Task 2); five roots with compatible checkpoints and `lastOpenedTrailId` (Task 3); the five-item pill and the Welcome disclosure (Task 7); contracts, DEV-31…DEV-37 and evidence (Tasks 1, 8). Out of scope stays out: no map provider, recorder, avatar, activity detail or personalization.
- **Placeholder scan:** every code step is complete; the only deferred values are the evidence README's measurements.
- **Type consistency:** `CompletedActivity`/`ForYouFeed`/`ActivityRepository`/`ForYouRepository` (Task 2) are consumed by the states (Task 3) and presenters (Tasks 5, 6) with the same names; `navigateTrailId(lastOpened, saved)` matches its test; `monthSummary`/`activityDateLabel`/`activitySummaryLine`/`kilometres` match their tests and the UI; `TrailsToastHost(..., showCheck)` (Task 4) is called with the named argument; `Root` has five values everywhere `when` is exhaustive (controller `stack()`, `M1Content.destination()`); `M1Navigation` defaults keep every existing fake compiling; `loadStates` (Task 2) is used by both `RealTrailRepository` and `AccountFeed`.

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-09-20-trails-remaining-roots.md`. Two execution options: **Subagent-Driven** (fresh subagent per task with review between tasks, via superpowers:subagent-driven-development) or **Inline** (superpowers:executing-plans with checkpoints). Both require the Prerequisite check to pass first.
