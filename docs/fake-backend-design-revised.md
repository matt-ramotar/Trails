# Backend Server Architecture for Trails (Revised)

## Technical Design Document

**Version:** 1.2
**Date:** January 25, 2026
**Author:** Trails Architecture Team

---

## Issue-by-issue revisions (step by step)

This section documents how each critical, high, or medium issue from the prior review is addressed.

### Issue 1 (Critical): JVM-only time usage in commonMain

Step 1: Introduce a KMP-safe clock abstraction in `server:api`.
Step 2: Replace all uses of `System.currentTimeMillis()` with the clock.
Step 3: Default fake backend uses `kotlinx.datetime.Clock` so all targets compile.

### Issue 2 (High): Runtime config changes not applied

Step 1: Add a `BackendConfigProvider` with a `StateFlow` of the current config.
Step 2: Make simulators read the config per request (no lazy snapshot).
Step 3: `updateConfig()` updates the provider and reseeds when scenario changes.

### Issue 3 (High): `UserSummary.isFollowing` has no viewer context

Step 1: Require viewer context for any API that returns `UserSummary`.
Step 2: Add `viewerToken: String?` to `getFollowers`, `getFollowing`, and `searchUsers`.
Step 3: If `viewerToken` is null, `isFollowing` is false by definition.

### Issue 4 (High): Offline mode not wired to server errors

Step 1: Add `networkMode` to `BackendConfig`.
Step 2: Map `DeveloperSettings.offlineMode` to `BackendConfig.networkMode`.
Step 3: Add `NetworkGate` to fail fast with `ServerError.NetworkUnavailable`.

### Issue 5 (Medium): Error simulator is not thread-safe

Step 1: Guard rate limit state with a `Mutex`.
Step 2: Make `shouldError()` a suspend function.
Step 3: Use `ArrayDeque` and prune timestamps within the mutex.

### Issue 6 (Medium): `BackendServer` mixes control/session concerns

Step 1: Make `BackendServer` a pure service facade.
Step 2: Move session storage to a client-side `SessionStore`.
Step 3: Move control methods to a fake-only `BackendControl` interface.

### Issue 7 (Medium): server API depends on app domain models

Step 1: Define server-local enums/ids for gradients, emoji, and difficulty.
Step 2: Map server DTOs to app domain models in data layer adapters.
Step 3: Remove `projects.multiplatform.model.domain` from `server:api`.

### Issue 8 (Medium): `@Binds` methods have bodies

Step 1: Use `@Provides` for service accessors instead of `@Binds`.
Step 2: Keep `BackendServer` provider in the module companion.
Step 3: Add an optional `BackendControl` provider for dev tools.

---

## 1. Executive Summary

This revised document presents a backend server abstraction that cleanly separates server logic from client-side code and fixes eight critical/high/medium issues from the previous draft.

Key revisions include:
- KMP-safe time handling via a clock abstraction
- Runtime-configurable latency/errors/conflicts
- Viewer-aware user summaries
- Offline mode wired to `ServerError.NetworkUnavailable`
- Thread-safe error simulation
- Separation of server facade, control, and client session storage
- Server-local DTOs to decouple app domain models
- Correct DI bindings for services

---

## 2. Current State Analysis

Same as previous version. The pain points remain the same and are addressed below.

---

## 3. Proposed Architecture

### 3.1 Module Structure (Revised)

```
server/
  api/
    src/commonMain/kotlin/
      org/mobilenativefoundation/trails/server/
        BackendServer.kt            # Service-only facade
        BackendConfig.kt            # Configuration data class
        BackendClock.kt             # KMP-safe clock abstraction
        BackendConfigProvider.kt    # Runtime config provider
        services/
          UserService.kt
          FeedService.kt
          PostService.kt
          ResortService.kt
          RunService.kt
          WeatherService.kt
        model/
          UserModels.kt
          FeedModels.kt
          PostModels.kt
          ResortModels.kt
          RunModels.kt
          WeatherModels.kt
          ServerEnums.kt            # Server-local enums/ids
        error/
          ServerError.kt
        pagination/
          Pagination.kt
    build.gradle.kts

  fake/
    src/commonMain/kotlin/
      org/mobilenativefoundation/trails/server/fake/
        FakeBackendServer.kt        # Implements BackendServer + BackendControl
        BackendControl.kt           # Fake-only control interface
        internal/
          storage/
            InMemoryBackendStore.kt
            BackendTable.kt
            BackendTables.kt
            StorageModels.kt
          simulation/
            NetworkGate.kt
            LatencySimulator.kt
            ErrorSimulator.kt
            ConflictSimulator.kt
          seed/
            SeedDataLoader.kt
            DefaultSeedData.kt
            LargeDatasetGenerator.kt
        services/
          FakeUserService.kt
          FakeFeedService.kt
          FakePostService.kt
          FakeResortService.kt
          FakeRunService.kt
          FakeWeatherService.kt
    build.gradle.kts
```

### 3.2 Dependency Graph (Unchanged)

Same as previous version, but `BackendServer` is now service-only, and control/session are separate.

### 3.3 Build Configuration (Revised)

**server:api/build.gradle.kts**
```kotlin
plugins {
    id("plugin.trails.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.datetime)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.server.api"
}
```

**server:fake/build.gradle.kts**
```kotlin
plugins {
    id("plugin.trails.kotlin.multiplatform")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.server.api)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.server.fake"
}
```

---

## 4. API Contracts

### 4.1 Core Configuration (Revised)

```kotlin
// server:api - BackendConfig.kt
package org.mobilenativefoundation.trails.server

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

enum class NetworkMode { ONLINE, OFFLINE }

data class BackendConfig(
    // Network simulation
    val networkMode: NetworkMode = NetworkMode.ONLINE,
    val latencyRange: ClosedRange<Duration> = 50.milliseconds..200.milliseconds,
    val errorRate: Float = 0.0f, // 0.0 to 1.0
    val rateLimitRequestsPerMinute: Int = 0, // 0 = disabled

    // Conflict simulation
    val conflictMode: ConflictMode = ConflictMode.DISABLED,
    val conflictProbability: Float = 0.0f,

    // Seeding
    val seedScenario: SeedScenario = SeedScenario.DEFAULT,

    // Pagination defaults
    val defaultPageSize: Int = 20,
    val maxPageSize: Int = 100,
)
```

### 4.2 Clock Abstraction (New)

```kotlin
// server:api - BackendClock.kt
package org.mobilenativefoundation.trails.server

import kotlinx.datetime.Clock

interface BackendClock {
    fun nowMs(): Long
}

object SystemBackendClock : BackendClock {
    override fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()
}
```

### 4.3 Runtime Config Provider (New)

```kotlin
// server:api - BackendConfigProvider.kt
package org.mobilenativefoundation.trails.server

import kotlinx.coroutines.flow.StateFlow

interface BackendConfigProvider {
    val current: BackendConfig
    val flow: StateFlow<BackendConfig>
    fun update(config: BackendConfig)
}
```

### 4.4 Error Types (Unchanged)

Same as previous version.

### 4.5 Pagination (Unchanged)

Same as previous version.

### 4.6 Service Interfaces (Revised viewer context)

#### UserService

```kotlin
// server:api - services/UserService.kt
package org.mobilenativefoundation.trails.server.services

import org.mobilenativefoundation.trails.server.model.*
import org.mobilenativefoundation.trails.server.pagination.*

interface UserService {
    // Authentication
    suspend fun signup(email: String, password: String): UserAuthResponse
    suspend fun login(email: String, password: String): UserAuthResponse
    suspend fun logout(token: String)
    suspend fun refreshToken(refreshToken: String): UserAuthResponse

    // Profile
    suspend fun getUser(userId: String): UserRecord
    suspend fun getCurrentUser(token: String): UserRecord
    suspend fun updateProfile(token: String, update: ProfileUpdate): UserRecord

    // Social (viewer-aware)
    suspend fun followUser(token: String, targetUserId: String)
    suspend fun unfollowUser(token: String, targetUserId: String)
    suspend fun getFollowers(
        userId: String,
        request: CursorRequest,
        viewerToken: String? = null,
    ): CursorPage<UserSummary>
    suspend fun getFollowing(
        userId: String,
        request: CursorRequest,
        viewerToken: String? = null,
    ): CursorPage<UserSummary>

    // Search (viewer-aware)
    suspend fun searchUsers(
        query: String,
        limit: Int = 10,
        viewerToken: String? = null,
    ): List<UserSummary>
}
```

Other service interfaces remain the same.

---

## 5. Model Definitions (Revised to remove domain dependency)

### 5.1 Server-local enums

```kotlin
// server:api - model/ServerEnums.kt
package org.mobilenativefoundation.trails.server.model

import kotlinx.serialization.Serializable

@Serializable
enum class TrailDifficulty {
    GREEN_CIRCLE,
    BLUE_SQUARE,
    BLACK_DIAMOND,
    DOUBLE_BLACK,
}

@Serializable
@JvmInline
value class BackgroundGradientId(val value: String)

@Serializable
@JvmInline
value class EmojiId(val value: String)
```

### 5.2 Feed models (updated)

```kotlin
@Serializable
data class StyleRecord(
    val backgroundGradientId: BackgroundGradientId,
    val emojiId: EmojiId,
)
```

### 5.3 Run models (updated)

```kotlin
@Serializable
data class RunRecord(
    val id: String,
    val resortId: String,
    val name: String,
    val difficulty: TrailDifficulty,
    val stats: RunStats,
    val status: RunStatus,
    val liftAccess: List<String>,
    val features: List<String>,
    val version: Long,
)
```

Mapping to app domain enums now occurs in data layer adapters.

---

## 6. Behavior Simulation (Revised)

### 6.1 Network Gate (New)

```kotlin
// server:fake - internal/simulation/NetworkGate.kt
package org.mobilenativefoundation.trails.server.fake.internal.simulation

import org.mobilenativefoundation.trails.server.BackendConfigProvider
import org.mobilenativefoundation.trails.server.NetworkMode
import org.mobilenativefoundation.trails.server.error.ServerError

class NetworkGate(
    private val configProvider: BackendConfigProvider,
) {
    fun ensureOnline(): ServerError? {
        return if (configProvider.current.networkMode == NetworkMode.OFFLINE) {
            ServerError.NetworkUnavailable
        } else null
    }
}
```

### 6.2 Latency Simulator (Dynamic config)

```kotlin
// server:fake - internal/simulation/LatencySimulator.kt
class LatencySimulator(
    private val configProvider: BackendConfigProvider,
) {
    suspend fun <T> withLatency(block: suspend () -> T): T {
        val range = configProvider.current.latencyRange
        // same delay logic as before using range
        return block()
    }
}
```

### 6.3 Error Simulator (Thread-safe, dynamic)

```kotlin
// server:fake - internal/simulation/ErrorSimulator.kt
class ErrorSimulator(
    private val configProvider: BackendConfigProvider,
    private val clock: BackendClock,
) {
    private val mutex = Mutex()
    private val requestTimestamps = ArrayDeque<Long>()

    suspend fun shouldError(): ServerError? = mutex.withLock {
        val config = configProvider.current

        // Rate limit
        if (config.rateLimitRequestsPerMinute > 0) {
            val now = clock.nowMs()
            val oneMinuteAgo = now - 60_000
            while (requestTimestamps.isNotEmpty() && requestTimestamps.first() < oneMinuteAgo) {
                requestTimestamps.removeFirst()
            }
            if (requestTimestamps.size >= config.rateLimitRequestsPerMinute) {
                return ServerError.RateLimited(retryAfterSeconds = 60)
            }
            requestTimestamps.addLast(now)
        }

        // Random error
        if (config.errorRate > 0 && Random.nextFloat() < config.errorRate) {
            return randomError()
        }

        return null
    }
}
```

Conflict simulator remains mostly unchanged, but is updated when config changes.

---

## 7. Seeding Strategy

Same as previous version, but all time-based fields use `BackendClock` for KMP safety.

---

## 8. Configuration Integration (Revised)

```kotlin
// data:devsettings:api - DeveloperSettings.kt (extended)
package org.mobilenativefoundation.trails.data.devsettings

import org.mobilenativefoundation.trails.server.BackendConfig
import org.mobilenativefoundation.trails.server.NetworkMode
import kotlin.time.Duration.Companion.milliseconds

// Existing fields unchanged

data class BackendSettings(
    val offlineMode: Boolean = false,
    val latencyMinMs: Long = 50,
    val latencyMaxMs: Long = 200,
    val errorRate: Float = 0.0f,
    val rateLimitPerMinute: Int = 0,
    val conflictMode: String = "DISABLED",
    val conflictProbability: Float = 0.0f,
    val seedScenario: String = "DEFAULT",
) {
    fun toConfig(): BackendConfig = BackendConfig(
        networkMode = if (offlineMode) NetworkMode.OFFLINE else NetworkMode.ONLINE,
        latencyRange = latencyMinMs.milliseconds..latencyMaxMs.milliseconds,
        errorRate = errorRate,
        rateLimitRequestsPerMinute = rateLimitPerMinute,
        conflictMode = ConflictMode.valueOf(conflictMode),
        conflictProbability = conflictProbability,
        seedScenario = SeedScenario.valueOf(seedScenario),
    )
}
```

---

## 9. Client Integration (Revised)

### 9.1 BackendServer Facade (Service-only)

```kotlin
// server:api - BackendServer.kt
package org.mobilenativefoundation.trails.server

import org.mobilenativefoundation.trails.server.services.*

interface BackendServer {
    val userService: UserService
    val feedService: FeedService
    val postService: PostService
    val resortService: ResortService
    val runService: RunService
    val weatherService: WeatherService
}
```

### 9.2 Fake Control Interface (Fake-only)

```kotlin
// server:fake - BackendControl.kt
package org.mobilenativefoundation.trails.server.fake

import org.mobilenativefoundation.trails.server.BackendConfig

interface BackendControl {
    suspend fun reset()
    fun triggerConflict()
    suspend fun updateConfig(config: BackendConfig)
}
```

### 9.3 Session Storage (Client-side)

Tokens are stored in a client-side `SessionStore` owned by the data/auth layer.

```kotlin
// data:auth:api - SessionStore.kt (new)
interface SessionStore {
    suspend fun currentToken(): String?
    suspend fun setCurrentToken(token: String?)
}
```

`UserAuthApi` sets the token in `SessionStore`. APIs read the token from `SessionStore` and pass it explicitly to service calls.

### 9.4 Fake Backend Implementation (Revised)

```kotlin
class FakeBackendServer(
    initialConfig: BackendConfig,
    private val clock: BackendClock = SystemBackendClock,
) : BackendServer, BackendControl {

    private val configFlow = MutableStateFlow(initialConfig)
    private val configProvider = object : BackendConfigProvider {
        override val current: BackendConfig get() = configFlow.value
        override val flow: StateFlow<BackendConfig> = configFlow
        override fun update(config: BackendConfig) { configFlow.value = config }
    }

    private val store = InMemoryBackendStore()
    private val tables = BackendTables(store)
    private val seedLoader = SeedDataLoader(tables, clock)

    private val networkGate = NetworkGate(configProvider)
    private val latencySimulator = LatencySimulator(configProvider)
    private val errorSimulator = ErrorSimulator(configProvider, clock)
    private val conflictSimulator = ConflictSimulator(
        mode = configProvider.current.conflictMode,
        probability = configProvider.current.conflictProbability,
    )

    override val userService: UserService by lazy {
        FakeUserService(tables, networkGate, latencySimulator, errorSimulator, seedLoader, configProvider, clock)
    }

    // Other services constructed similarly.

    override suspend fun reset() {
        store.clear()
        conflictSimulator.reset()
        errorSimulator.reset()
        seedLoader.reset()
    }

    override fun triggerConflict() {
        conflictSimulator.triggerNextConflict()
    }

    override suspend fun updateConfig(config: BackendConfig) {
        configProvider.update(config)
        conflictSimulator.updateMode(config.conflictMode, config.conflictProbability)
        seedLoader.ensureSeeded(config.seedScenario)
    }
}
```

### 9.5 Updated PostApi Example (Token from SessionStore)

```kotlin
class RealPostApi(
    private val backendServer: BackendServer,
    private val sessionStore: SessionStore,
) : PostApi {

    override suspend fun fetchFeed(feedKey: FeedKey): List<FeedPostRecord> {
        val token = sessionStore.currentToken()
        val page = if (token != null) {
            backendServer.feedService.getHomeFeed(
                token = token,
                request = CursorRequest(limit = 50),
            )
        } else {
            backendServer.feedService.getTrendingFeed(
                request = CursorRequest(limit = 50),
            )
        }
        return page.items.mapNotNull { item ->
            (item as? SkiRunPostRecord)?.toFeedPostRecord()
        }
    }
}
```

Mapping from server enums to app domain enums now happens in the adapter layer.

---

## 10. DI Bindings (Revised)

```kotlin
// di:graph:app - BackendModule.kt
@Module
interface BackendModule {

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideBackendServer(
            developerSettingsRepository: DeveloperSettingsRepository,
        ): BackendServer {
            return FakeBackendServer(
                initialConfig = developerSettingsRepository.current.backend.toConfig()
            )
        }

        @Provides
        fun provideBackendControl(server: BackendServer): BackendControl? {
            return server as? BackendControl
        }

        @Provides
        fun provideUserService(server: BackendServer): UserService = server.userService

        @Provides
        fun provideFeedService(server: BackendServer): FeedService = server.feedService

        @Provides
        fun providePostService(server: BackendServer): PostService = server.postService

        @Provides
        fun provideResortService(server: BackendServer): ResortService = server.resortService

        @Provides
        fun provideRunService(server: BackendServer): RunService = server.runService

        @Provides
        fun provideWeatherService(server: BackendServer): WeatherService = server.weatherService
    }
}
```

---

## 11. Migration Plan (Adjusted)

### Phase 1: Create New Modules (Week 1)

1. Create `server/api` module
2. Create `server/fake` module
3. Add new server-local enums and clock/config provider

### Phase 2: Implement Core Infrastructure (Week 1-2)

1. Implement `BackendClock` and replace time calls
2. Add `BackendConfigProvider` and update simulators
3. Add `NetworkGate`
4. Update `ErrorSimulator` to be thread-safe

### Phase 3: Implement Services (Week 2-3)

1. Implement `FakeUserService`
2. Implement `FakeFeedService`
3. Implement `FakePostService`
4. Implement `FakeResortService`
5. Implement `FakeRunService`
6. Implement `FakeWeatherService`

### Phase 4: Client Integration (Week 3)

1. Introduce `SessionStore` in data/auth
2. Refactor APIs to read token from `SessionStore`
3. Update `DeveloperSettings` mapping to `BackendConfig`
4. Add DI bindings for `BackendServer` and optional `BackendControl`

### Phase 5: Cleanup (Week 4)

Same as previous version.

---

## 12. Testing Strategy (Adjusted)

Add tests for:
- Offline mode returns `ServerError.NetworkUnavailable`
- Runtime config updates affect latency and error injection
- Viewer-aware search/follow lists return correct `isFollowing`

---

## Appendix: File Inventory (Updated Highlights)

New additions:
- `server/api/BackendClock.kt`
- `server/api/BackendConfigProvider.kt`
- `server/api/model/ServerEnums.kt`
- `server/fake/BackendControl.kt`
- `server/fake/internal/simulation/NetworkGate.kt`
- `data/auth/api/SessionStore.kt`

Existing files updated to remove domain model coupling and add viewer context.

---

*End of Document*
