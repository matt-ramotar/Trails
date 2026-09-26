# Backend Server Architecture for Trails

## Technical Design Document

**Version:** 1.1
**Date:** January 2026
**Author:** Trails Architecture Team

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current State Analysis](#2-current-state-analysis)
3. [Proposed Architecture](#3-proposed-architecture)
4. [API Contracts](#4-api-contracts)
5. [Data Layer Design](#5-data-layer-design)
6. [Behavior Simulation](#6-behavior-simulation)
7. [Seeding Strategy](#7-seeding-strategy)
8. [Configuration Integration](#8-configuration-integration)
9. [Client Integration](#9-client-integration)
10. [Architecture Diagram](#10-architecture-diagram)
11. [Migration Plan](#11-migration-plan)
12. [Testing Strategy](#12-testing-strategy)

---

## 1. Executive Summary

This document presents a comprehensive architecture for a backend server abstraction that cleanly separates server logic from client-side code in the Trails sample application. The design addresses current pain points where `FakePostApi` writes to client-side SQLDelight tables, creating tight coupling between "backend simulation" and client code.

### Design Principles

1. **Clean API contracts**: Service interfaces (`UserService`, `PostService`, etc.) have no implementation details - they work for both fake and real backends
2. **Implementation flexibility**: `FakeUserService` implements `UserService`; future `RealUserService` can implement the same interface
3. **Location**: `server/` directory at the project root, not in `multiplatform/`

### Goals

1. **Clean separation**: Backend as its own module in `server/`, not part of `data/post/impl`
2. **Multi-service**: Support all services (users, posts, feeds, resorts, runs, weather)
3. **Realistic simulation**: Latency, pagination, errors, rate limits
4. **Configurable**: Developer settings control behavior at runtime
5. **In-memory storage**: Backend maintains its own state separate from client cache
6. **REST-like contracts**: Clear API interfaces replaceable with real HTTP later
7. **Conflict simulation**: Rich conflict scenarios for Store5 demonstrations
8. **Kotlin Multiplatform**: Works on Android, iOS, JVM, and Web

---

## 2. Current State Analysis

### 2.1 Pain Points Identified

After analyzing the codebase, the following architectural issues were identified:

#### Mixed Concerns in `FakePostApi`

**File:** `multiplatform/data/post/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/data/post/FakePostApi.kt`

- Directly uses `TrailsDatabaseQueries` to write to `backend_posts` and `backend_feed_posts` tables
- These "backend" tables live in the same SQLDelight database as client tables
- No clear boundary between what is "server state" vs "client cache"

#### Single-Shot Conflict Simulation

- `conflictInjected` boolean fires once per session only
- No way to reset or configure different conflict scenarios

#### Limited Data Coverage

- Only `FakePostDatabase` exists for posts
- No fake data for users (profiles), resorts, runs, weather as independent services
- `FakeUserAuthApi` exists but stores user state in the same client database

#### No Realistic Network Behavior

- No latency simulation
- No pagination support
- Limited error injection (only `OfflineModeException`)
- No rate limiting

#### Static Configuration

- `DeveloperSettings` only controls `offlineMode`, `conflictsEnabled`, `conflictStrategy`
- No way to configure latency ranges, error rates, or seed data scenarios

### 2.2 Current Data Flow

```
FakePostApi.fetchFeed(feedKey)
    |
    +-- ensureOnline() // throws if offline mode enabled
    |
    +-- seedIfNeeded(feedKey)
    |       |
    |       +-- Check if backend_posts table is empty
    |       +-- If empty: persistFeed(feedKey, FakePostDataFactory.homeFeed())
    |               +-- FakePostDataFactory.homeFeed() -> FakePostDatabase.homeFeed()
    |               +-- Saves to backend_posts + backend_feed_posts tables (CLIENT DB!)
    |
    +-- readFeed(feedKey)
            +-- Queries backend_posts + backend_feed_posts tables
            +-- Returns List<FeedPostRecord>
```

The problem: "backend" tables are in the client's SQLDelight database, blurring the architectural boundary.

---

## 3. Proposed Architecture

### 3.1 Module Structure

```
server/
  api/                             # Public contracts only (clean names, no "Fake" prefix)
    src/commonMain/kotlin/
      org/mobilenativefoundation/trails/server/
        BackendServer.kt           # Main facade interface
        BackendConfig.kt           # Configuration data class
        services/
          UserService.kt           # Interface (clean name)
          FeedService.kt           # Interface (clean name)
          PostService.kt           # Interface (clean name)
          ResortService.kt         # Interface (clean name)
          RunService.kt            # Interface (clean name)
          WeatherService.kt        # Interface (clean name)
        model/
          UserModels.kt            # DTOs for user service
          FeedModels.kt            # DTOs for feed service
          PostModels.kt            # DTOs for post service
          ResortModels.kt          # DTOs for resort service
          RunModels.kt             # DTOs for run service
          WeatherModels.kt         # DTOs for weather service
        error/
          ServerError.kt           # Sealed class for error types
        pagination/
          CursorPage.kt            # Cursor-based pagination
          OffsetPage.kt            # Offset-based pagination
    build.gradle.kts

  fake/                            # Fake implementation (uses "Fake" prefix)
    src/commonMain/kotlin/
      org/mobilenativefoundation/trails/server/fake/
        FakeBackendServer.kt       # Implements BackendServer
        internal/
          storage/
            InMemoryBackendStore.kt      # Thread-safe in-memory storage
            BackendTable.kt              # Generic table abstraction
            BackendTables.kt             # All table definitions
            StorageModels.kt             # Internal storage models
          simulation/
            LatencySimulator.kt          # Configurable delay injection
            ErrorSimulator.kt            # Random error injection
            ConflictSimulator.kt         # Version conflict scenarios
          seed/
            SeedDataLoader.kt            # Lazy data seeding
            DefaultSeedData.kt           # Default ski resort data
            LargeDatasetGenerator.kt     # For pagination testing
        services/
          FakeUserService.kt       # Implements UserService
          FakeFeedService.kt       # Implements FeedService
          FakePostService.kt       # Implements PostService
          FakeResortService.kt     # Implements ResortService
          FakeRunService.kt        # Implements RunService
          FakeWeatherService.kt    # Implements WeatherService
    build.gradle.kts
```

### 3.2 Dependency Graph

```
                    +------------------+
                    |    server:api    |  <- Clean contracts (UserService, PostService, etc.)
                    +------------------+
                            ^
                            |
                    +------------------+
                    |    server:fake   |  <- Fake implementations (FakeUserService, etc.)
                    +------------------+
                            ^
                            |
              +-------------+-------------+
              |                           |
    +------------------+        +------------------+
    |  data:post:impl  |        |  data:user:impl  |
    +------------------+        +------------------+
              ^                           ^
              |                           |
              +-----------+---------------+
                          |
                  +---------------+
                  | di:graph:app  |
                  +---------------+
```

### 3.3 Build Configuration

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
                api(projects.multiplatform.model.domain) // For TrailDifficulty, etc.
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

### 4.1 Core Configuration

```kotlin
// server:api - BackendConfig.kt
package org.mobilenativefoundation.trails.server

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class BackendConfig(
    // Network simulation
    val latencyRange: ClosedRange<Duration> = 50.milliseconds..200.milliseconds,
    val errorRate: Float = 0.0f, // 0.0 to 1.0
    val rateLimitRequestsPerMinute: Int = 0, // 0 = disabled

    // Conflict simulation
    val conflictMode: ConflictMode = ConflictMode.DISABLED,
    val conflictProbability: Float = 0.0f, // For RANDOM mode

    // Seeding
    val seedScenario: SeedScenario = SeedScenario.DEFAULT,

    // Pagination defaults
    val defaultPageSize: Int = 20,
    val maxPageSize: Int = 100,
)

enum class ConflictMode {
    DISABLED,           // No conflicts injected
    FIRST_WRITE,        // Conflict on first write only (current behavior)
    EVERY_WRITE,        // Conflict on every write attempt
    RANDOM,             // Random based on probability
    MANUAL,             // Only when explicitly triggered via triggerConflict()
}

enum class SeedScenario {
    DEFAULT,            // Standard ski resort data (17 resorts, 18 runs)
    EMPTY,              // No data (test empty states)
    LARGE_DATASET,      // 1000+ items for pagination testing
    CONFLICT_TESTING,   // Prepared conflict scenarios
    ERROR_STATES,       // Data that triggers edge cases
}
```

### 4.2 Error Types

```kotlin
// server:api - error/ServerError.kt
package org.mobilenativefoundation.trails.server.error

sealed class ServerError : Exception() {
    // 4xx Client Errors
    data class BadRequest(override val message: String) : ServerError()

    data class Unauthorized(
        val reason: String = "Invalid or expired token"
    ) : ServerError()

    data class Forbidden(val resource: String) : ServerError()

    data class NotFound(
        val resourceType: String,
        val id: String
    ) : ServerError()

    data class Conflict(
        val resourceType: String,
        val id: String,
        val clientVersion: Long,
        val serverVersion: Long,
    ) : ServerError()

    data class RateLimited(val retryAfterSeconds: Int) : ServerError()

    // 5xx Server Errors
    data class InternalError(override val message: String) : ServerError()
    data object ServiceUnavailable : ServerError()
    data class Timeout(val operationName: String) : ServerError()

    // Network-level
    data object NetworkUnavailable : ServerError() // Simulates offline
}
```

### 4.3 Pagination

```kotlin
// server:api - pagination/Pagination.kt
package org.mobilenativefoundation.trails.server.pagination

import kotlinx.serialization.Serializable

// Cursor-based pagination (preferred for feeds)
@Serializable
data class CursorPage<T>(
    val items: List<T>,
    val nextCursor: String?, // null = no more pages
    val prevCursor: String?, // for bidirectional navigation
    val totalCount: Int? = null, // optional total
)

@Serializable
data class CursorRequest(
    val cursor: String? = null,
    val limit: Int = 20,
    val direction: Direction = Direction.FORWARD,
) {
    enum class Direction { FORWARD, BACKWARD }
}

// Offset-based pagination (for lists with stable ordering)
@Serializable
data class OffsetPage<T>(
    val items: List<T>,
    val offset: Int,
    val limit: Int,
    val totalCount: Int,
    val hasMore: Boolean,
)

@Serializable
data class OffsetRequest(
    val offset: Int = 0,
    val limit: Int = 20,
)
```

### 4.4 Service Interfaces

All service interfaces use **clean names without "Fake" prefix**. This allows the same contract to be implemented by both fake (for testing/demo) and real (production) implementations.

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

    // Social
    suspend fun followUser(token: String, targetUserId: String)
    suspend fun unfollowUser(token: String, targetUserId: String)
    suspend fun getFollowers(userId: String, request: CursorRequest): CursorPage<UserSummary>
    suspend fun getFollowing(userId: String, request: CursorRequest): CursorPage<UserSummary>

    // Search
    suspend fun searchUsers(query: String, limit: Int = 10): List<UserSummary>
}
```

#### FeedService

```kotlin
// server:api - services/FeedService.kt
package org.mobilenativefoundation.trails.server.services

import org.mobilenativefoundation.trails.server.model.*
import org.mobilenativefoundation.trails.server.pagination.*

interface FeedService {
    // Home feed (algorithmic, cursor-based)
    suspend fun getHomeFeed(
        token: String,
        request: CursorRequest,
    ): CursorPage<FeedItemRecord>

    // User's own posts
    suspend fun getUserFeed(
        userId: String,
        request: CursorRequest,
    ): CursorPage<FeedItemRecord>

    // Posts from followed users
    suspend fun getFollowingFeed(
        token: String,
        request: CursorRequest,
    ): CursorPage<FeedItemRecord>

    // Resort-specific feed
    suspend fun getResortFeed(
        resortId: String,
        request: CursorRequest,
    ): CursorPage<FeedItemRecord>

    // Trending posts
    suspend fun getTrendingFeed(
        request: CursorRequest,
    ): CursorPage<FeedItemRecord>
}
```

#### PostService

```kotlin
// server:api - services/PostService.kt
package org.mobilenativefoundation.trails.server.services

import org.mobilenativefoundation.trails.server.model.*
import org.mobilenativefoundation.trails.server.pagination.*

interface PostService {
    // CRUD
    suspend fun createPost(token: String, create: CreatePostRequest): SkiRunPostRecord
    suspend fun getPost(postId: String): SkiRunPostRecord
    suspend fun getPost(postId: String, token: String): SkiRunPostRecord // With user engagement state
    suspend fun updatePost(token: String, postId: String, update: UpdatePostRequest): SkiRunPostRecord
    suspend fun deletePost(token: String, postId: String)

    // Engagement
    suspend fun likePost(token: String, postId: String)
    suspend fun unlikePost(token: String, postId: String)
    suspend fun bookmarkPost(token: String, postId: String)
    suspend fun unbookmarkPost(token: String, postId: String)

    // Comments
    suspend fun getComments(postId: String, request: CursorRequest): CursorPage<CommentRecord>
    suspend fun addComment(token: String, postId: String, body: String): CommentRecord
    suspend fun deleteComment(token: String, postId: String, commentId: String)

    // Bookmarks (for current user)
    suspend fun getBookmarkedPosts(token: String, request: CursorRequest): CursorPage<SkiRunPostRecord>

    // Liked posts (for current user)
    suspend fun getLikedPosts(token: String, request: CursorRequest): CursorPage<SkiRunPostRecord>
}
```

#### ResortService

```kotlin
// server:api - services/ResortService.kt
package org.mobilenativefoundation.trails.server.services

import org.mobilenativefoundation.trails.server.model.*
import org.mobilenativefoundation.trails.server.pagination.*

interface ResortService {
    suspend fun listResorts(
        request: OffsetRequest,
        filter: ResortFilter? = null,
    ): OffsetPage<ResortRecord>

    suspend fun getResort(resortId: String): ResortRecord

    suspend fun searchResorts(query: String, limit: Int = 10): List<ResortSummary>

    // Favorites
    suspend fun favoriteResort(token: String, resortId: String)
    suspend fun unfavoriteResort(token: String, resortId: String)
    suspend fun getFavoriteResorts(token: String): List<ResortSummary>
}
```

#### RunService

```kotlin
// server:api - services/RunService.kt
package org.mobilenativefoundation.trails.server.services

import org.mobilenativefoundation.trails.server.model.*
import org.mobilenativefoundation.trails.server.pagination.*

interface RunService {
    suspend fun listRuns(
        resortId: String,
        request: OffsetRequest,
        filter: RunFilter? = null,
    ): OffsetPage<RunRecord>

    suspend fun getRun(runId: String): RunRecord

    suspend fun searchRuns(
        query: String,
        resortId: String? = null,
        limit: Int = 10,
    ): List<RunSummary>

    // User's run history
    suspend fun getRunHistory(
        token: String,
        request: CursorRequest,
    ): CursorPage<RunHistoryEntry>
}
```

#### WeatherService

```kotlin
// server:api - services/WeatherService.kt
package org.mobilenativefoundation.trails.server.services

import org.mobilenativefoundation.trails.server.model.*

interface WeatherService {
    suspend fun getCurrentConditions(resortId: String): WeatherRecord

    suspend fun getForecast(resortId: String, days: Int = 5): List<ForecastRecord>

    suspend fun getSnowReport(resortId: String): SnowReportRecord

    // Bulk operations for efficiency
    suspend fun getConditionsForResorts(resortIds: List<String>): Map<String, WeatherRecord>
}
```

### 4.5 Model Definitions

```kotlin
// server:api - model/UserModels.kt
package org.mobilenativefoundation.trails.server.model

import kotlinx.serialization.Serializable

@Serializable
data class UserAuthResponse(
    val user: UserRecord,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long, // epoch millis
)

@Serializable
data class UserRecord(
    val id: String,
    val email: String,
    val profile: ProfileRecord?,
    val stats: UserStats,
    val createdAt: String,
    val version: Long,
)

@Serializable
data class ProfileRecord(
    val firstName: String,
    val lastName: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String?,
    val bio: String?,
    val location: String?,
    val verified: Boolean,
)

@Serializable
data class UserStats(
    val followers: Int,
    val following: Int,
    val posts: Int,
    val totalVerticalFeet: Int,
    val totalRuns: Int,
)

@Serializable
data class ProfileUpdate(
    val firstName: String? = null,
    val lastName: String? = null,
    val displayName: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class UserSummary(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val verified: Boolean,
    val isFollowing: Boolean, // Relative to requesting user
)
```

```kotlin
// server:api - model/FeedModels.kt
package org.mobilenativefoundation.trails.server.model

import kotlinx.serialization.Serializable
import org.mobilenativefoundation.trails.model.domain.feed.BackgroundGradient
import org.mobilenativefoundation.trails.model.domain.feed.Emoji

@Serializable
sealed interface FeedItemRecord {
    val id: String
    val type: String
    val timestamp: String
}

@Serializable
data class SkiRunPostRecord(
    override val id: String,
    override val type: String = "ski_run_post",
    override val timestamp: String,
    val author: UserSummary,
    val run: RunSummary,
    val resort: ResortSummary,
    val weather: WeatherSummary,
    val engagement: EngagementRecord,
    val style: StyleRecord,
    val caption: String?,
    val version: Long,
) : FeedItemRecord

@Serializable
data class EngagementRecord(
    val views: Int,
    val likes: Int,
    val comments: Int,
    val shares: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
)

@Serializable
data class StyleRecord(
    val backgroundGradient: BackgroundGradient,
    val emoji: Emoji,
)

@Serializable
data class WeatherSummary(
    val conditions: String,
    val temperatureF: Int,
    val snowfall24hIn: Int,
)
```

```kotlin
// server:api - model/PostModels.kt
package org.mobilenativefoundation.trails.server.model

import kotlinx.serialization.Serializable

@Serializable
data class CreatePostRequest(
    val runId: String,
    val styleId: String?,
    val caption: String?,
)

@Serializable
data class UpdatePostRequest(
    val caption: String? = null,
    val styleId: String? = null,
    val version: Long, // Required for optimistic locking
)

@Serializable
data class CommentRecord(
    val id: String,
    val author: UserSummary,
    val body: String,
    val timestamp: String,
    val likes: Int,
    val isLiked: Boolean,
)
```

```kotlin
// server:api - model/ResortModels.kt
package org.mobilenativefoundation.trails.server.model

import kotlinx.serialization.Serializable

@Serializable
data class ResortRecord(
    val id: String,
    val name: String,
    val location: LocationRecord,
    val stats: ResortStats,
    val status: ResortStatus,
    val amenities: List<String>,
    val imageUrl: String?,
    val version: Long,
)

@Serializable
data class LocationRecord(
    val city: String,
    val state: String?,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Int, // base elevation in feet
)

@Serializable
data class ResortStats(
    val runs: Int,
    val lifts: Int,
    val verticalFeet: Int,
    val skiableAcres: Int,
    val snowfallAnnualInches: Int,
)

@Serializable
data class ResortStatus(
    val isOpen: Boolean,
    val liftsOpen: Int,
    val liftsTotal: Int,
    val runsOpen: Int,
    val runsTotal: Int,
    val lastUpdated: String,
)

@Serializable
data class ResortSummary(
    val id: String,
    val name: String,
    val location: String, // "City, State" or "City, Country"
    val imageUrl: String?,
)

@Serializable
data class ResortFilter(
    val country: String? = null,
    val state: String? = null,
    val minVertical: Int? = null,
    val hasNightSkiing: Boolean? = null,
    val isOpen: Boolean? = null,
)
```

```kotlin
// server:api - model/RunModels.kt
package org.mobilenativefoundation.trails.server.model

import kotlinx.serialization.Serializable
import org.mobilenativefoundation.trails.model.domain.feed.TrailDifficulty

@Serializable
data class RunRecord(
    val id: String,
    val resortId: String,
    val name: String,
    val difficulty: TrailDifficulty,
    val stats: RunStats,
    val status: RunStatus,
    val liftAccess: List<String>,
    val features: List<String>, // "glades", "moguls", "groomed", etc.
    val version: Long,
)

@Serializable
data class RunStats(
    val lengthMiles: Float,
    val verticalFeet: Int,
    val averageGradePct: Int,
    val maxGradePct: Int,
)

@Serializable
data class RunStatus(
    val isOpen: Boolean,
    val conditions: String, // "Groomed", "Powder", etc.
    val lastGroomed: String?,
)

@Serializable
data class RunSummary(
    val id: String,
    val resortId: String,
    val name: String,
    val difficulty: TrailDifficulty,
    val verticalFeet: Int,
    val distance: String, // Formatted, e.g., "2.3 mi"
    val liftAccess: String, // Primary lift
)

@Serializable
data class RunFilter(
    val difficulty: List<TrailDifficulty>? = null,
    val isOpen: Boolean? = null,
    val minVertical: Int? = null,
    val hasNightSkiing: Boolean? = null,
)

@Serializable
data class RunHistoryEntry(
    val id: String,
    val run: RunSummary,
    val resort: ResortSummary,
    val timestamp: String,
    val duration: String,
    val topSpeed: String,
)
```

```kotlin
// server:api - model/WeatherModels.kt
package org.mobilenativefoundation.trails.server.model

import kotlinx.serialization.Serializable

@Serializable
data class WeatherRecord(
    val id: String,
    val resortId: String,
    val observedAt: String,
    val conditions: String, // "Snowing", "Clear", "Overcast"
    val temperatureF: Int,
    val feelsLikeF: Int,
    val windMph: Int,
    val windDirection: String,
    val humidity: Int,
    val visibility: String, // "good", "moderate", "poor"
    val uvIndex: Int,
    val summary: String,
)

@Serializable
data class ForecastRecord(
    val date: String,
    val highF: Int,
    val lowF: Int,
    val conditions: String,
    val precipProbability: Int,
    val snowfallInches: Int,
    val windMph: Int,
)

@Serializable
data class SnowReportRecord(
    val resortId: String,
    val reportedAt: String,
    val newSnow24h: Int, // inches
    val newSnow48h: Int,
    val newSnow7d: Int,
    val baseDepth: Int,
    val seasonTotal: Int,
    val surfaceConditions: String,
)
```

---

## 5. Data Layer Design

### 5.1 In-Memory Storage Architecture

The fake backend uses a thread-safe in-memory storage system rather than SQLDelight, ensuring complete separation from client state.

```kotlin
// server:fake - internal/storage/InMemoryBackendStore.kt
package org.mobilenativefoundation.trails.server.fake.internal.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

class InMemoryBackendStore {
    private val tables = mutableMapOf<KClass<*>, BackendTable<*, *>>()
    private val mutex = Mutex()

    @Suppress("UNCHECKED_CAST")
    suspend fun <K : Any, V : Any> getTable(
        type: KClass<V>,
        keyExtractor: (V) -> K,
    ): BackendTable<K, V> = mutex.withLock {
        tables.getOrPut(type) {
            BackendTable(keyExtractor)
        } as BackendTable<K, V>
    }

    suspend fun clear() = mutex.withLock {
        tables.values.forEach { it.clear() }
    }

    suspend fun snapshot(): Map<String, List<Any>> = mutex.withLock {
        tables.map { (type, table) ->
            type.simpleName.orEmpty() to table.getAll()
        }.toMap()
    }
}
```

### 5.2 Generic Table Abstraction

```kotlin
// server:fake - internal/storage/BackendTable.kt
package org.mobilenativefoundation.trails.server.fake.internal.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BackendTable<K : Any, V : Any>(
    private val keyExtractor: (V) -> K,
) {
    private val data = mutableMapOf<K, V>()
    private val mutex = Mutex()

    suspend fun get(key: K): V? = mutex.withLock { data[key] }

    suspend fun getAll(): List<V> = mutex.withLock { data.values.toList() }

    suspend fun put(value: V): V = mutex.withLock {
        val key = keyExtractor(value)
        data[key] = value
        value
    }

    suspend fun putAll(values: List<V>) = mutex.withLock {
        values.forEach { value ->
            data[keyExtractor(value)] = value
        }
    }

    suspend fun remove(key: K): V? = mutex.withLock { data.remove(key) }

    suspend fun clear() = mutex.withLock { data.clear() }

    suspend fun count(): Int = mutex.withLock { data.size }

    suspend fun query(predicate: (V) -> Boolean): List<V> = mutex.withLock {
        data.values.filter(predicate)
    }

    suspend fun update(key: K, transform: (V) -> V): V? = mutex.withLock {
        data[key]?.let { existing ->
            val updated = transform(existing)
            data[key] = updated
            updated
        }
    }

    suspend fun updateOrCreate(key: K, create: () -> V, transform: (V) -> V): V = mutex.withLock {
        val existing = data[key]
        val updated = if (existing != null) transform(existing) else create()
        data[key] = updated
        updated
    }

    suspend fun exists(key: K): Boolean = mutex.withLock { data.containsKey(key) }
}
```

### 5.3 Table Definitions

```kotlin
// server:fake - internal/storage/BackendTables.kt
package org.mobilenativefoundation.trails.server.fake.internal.storage

class BackendTables(private val store: InMemoryBackendStore) {

    // Users
    suspend fun users() = store.getTable(
        type = StoredUser::class,
        keyExtractor = { it.id }
    )

    // Sessions (token -> user mapping)
    suspend fun sessions() = store.getTable(
        type = StoredSession::class,
        keyExtractor = { it.token }
    )

    // Follow relationships
    suspend fun follows() = store.getTable(
        type = StoredFollow::class,
        keyExtractor = { "${it.followerId}:${it.followeeId}" }
    )

    // Posts
    suspend fun posts() = store.getTable(
        type = StoredPost::class,
        keyExtractor = { it.id }
    )

    // Post engagements (user-specific)
    suspend fun postEngagements() = store.getTable(
        type = StoredPostEngagement::class,
        keyExtractor = { "${it.userId}:${it.postId}" }
    )

    // Comments
    suspend fun comments() = store.getTable(
        type = StoredComment::class,
        keyExtractor = { it.id }
    )

    // Resorts
    suspend fun resorts() = store.getTable(
        type = StoredResort::class,
        keyExtractor = { it.id }
    )

    // Runs
    suspend fun runs() = store.getTable(
        type = StoredRun::class,
        keyExtractor = { it.id }
    )

    // Weather
    suspend fun weather() = store.getTable(
        type = StoredWeather::class,
        keyExtractor = { it.resortId }
    )

    // User favorites (resorts)
    suspend fun userFavorites() = store.getTable(
        type = StoredUserFavorite::class,
        keyExtractor = { "${it.userId}:${it.resortId}" }
    )
}
```

### 5.4 Storage Models

```kotlin
// server:fake - internal/storage/StorageModels.kt
package org.mobilenativefoundation.trails.server.fake.internal.storage

import org.mobilenativefoundation.trails.model.domain.feed.BackgroundGradient
import org.mobilenativefoundation.trails.model.domain.feed.Emoji
import org.mobilenativefoundation.trails.model.domain.feed.TrailDifficulty

// Storage models (internal, not exposed via API)

internal data class StoredUser(
    val id: String,
    val email: String,
    val passwordHash: String, // simulated
    val firstName: String?,
    val lastName: String?,
    val displayName: String,
    val username: String,
    val avatarUrl: String?,
    val bio: String?,
    val location: String?,
    val verified: Boolean,
    val createdAt: Long,
    val version: Long,
)

internal data class StoredSession(
    val token: String,
    val refreshToken: String,
    val userId: String,
    val expiresAt: Long,
)

internal data class StoredFollow(
    val followerId: String,
    val followeeId: String,
    val createdAt: Long,
)

internal data class StoredPost(
    val id: String,
    val authorId: String,
    val runId: String,
    val backgroundGradient: BackgroundGradient,
    val emoji: Emoji,
    val caption: String?,
    val createdAt: Long,
    val version: Long,
    // Denormalized engagement counts
    val likes: Int,
    val comments: Int,
    val shares: Int,
    val views: Int,
)

internal data class StoredPostEngagement(
    val userId: String,
    val postId: String,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
)

internal data class StoredComment(
    val id: String,
    val postId: String,
    val authorId: String,
    val body: String,
    val createdAt: Long,
    val likes: Int,
)

internal data class StoredResort(
    val id: String,
    val name: String,
    val city: String,
    val state: String?,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Int,
    val runs: Int,
    val lifts: Int,
    val verticalFeet: Int,
    val skiableAcres: Int,
    val snowfallAnnualInches: Int,
    val amenities: List<String>,
    val isOpen: Boolean,
    val liftsOpen: Int,
    val runsOpen: Int,
    val imageUrl: String?,
    val version: Long,
)

internal data class StoredRun(
    val id: String,
    val resortId: String,
    val name: String,
    val difficulty: TrailDifficulty,
    val lengthMiles: Float,
    val verticalFeet: Int,
    val averageGradePct: Int,
    val maxGradePct: Int,
    val isOpen: Boolean,
    val conditions: String,
    val lastGroomed: String?,
    val liftAccess: List<String>,
    val features: List<String>,
    val version: Long,
)

internal data class StoredWeather(
    val resortId: String,
    val observedAt: Long,
    val conditions: String,
    val temperatureF: Int,
    val feelsLikeF: Int,
    val windMph: Int,
    val windDirection: String,
    val humidity: Int,
    val visibility: String,
    val uvIndex: Int,
    val newSnow24h: Int,
    val newSnow48h: Int,
    val newSnow7d: Int,
    val baseDepth: Int,
    val seasonTotal: Int,
)

internal data class StoredUserFavorite(
    val userId: String,
    val resortId: String,
    val createdAt: Long,
)
```

---

## 6. Behavior Simulation

### 6.1 Latency Simulation

```kotlin
// server:fake - internal/simulation/LatencySimulator.kt
package org.mobilenativefoundation.trails.server.fake.internal.simulation

import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration

class LatencySimulator(
    private val range: ClosedRange<Duration>,
) {
    suspend fun <T> withLatency(block: suspend () -> T): T {
        if (range.start > Duration.ZERO || range.endInclusive > Duration.ZERO) {
            val delayMs = Random.nextLong(
                range.start.inWholeMilliseconds,
                range.endInclusive.inWholeMilliseconds + 1
            )
            delay(delayMs)
        }
        return block()
    }

    companion object {
        val INSTANT = LatencySimulator(Duration.ZERO..Duration.ZERO)
    }
}
```

### 6.2 Error Simulation

```kotlin
// server:fake - internal/simulation/ErrorSimulator.kt
package org.mobilenativefoundation.trails.server.fake.internal.simulation

import org.mobilenativefoundation.trails.server.error.ServerError
import kotlin.random.Random

class ErrorSimulator(
    private val errorRate: Float,
    private val rateLimitPerMinute: Int,
) {
    private val requestTimestamps = mutableListOf<Long>()

    fun shouldError(): ServerError? {
        // Check rate limit first
        if (rateLimitPerMinute > 0) {
            val now = currentTimeMillis()
            val oneMinuteAgo = now - 60_000
            requestTimestamps.removeAll { it < oneMinuteAgo }

            if (requestTimestamps.size >= rateLimitPerMinute) {
                return ServerError.RateLimited(retryAfterSeconds = 60)
            }
            requestTimestamps.add(now)
        }

        // Random error injection
        if (errorRate > 0 && Random.nextFloat() < errorRate) {
            return randomError()
        }

        return null
    }

    private fun randomError(): ServerError {
        val errors = listOf(
            ServerError.InternalError("Simulated server error"),
            ServerError.ServiceUnavailable,
            ServerError.Timeout("Simulated timeout"),
        )
        return errors.random()
    }

    fun reset() {
        requestTimestamps.clear()
    }

    private fun currentTimeMillis(): Long = System.currentTimeMillis()
}
```

### 6.3 Conflict Simulation

```kotlin
// server:fake - internal/simulation/ConflictSimulator.kt
package org.mobilenativefoundation.trails.server.fake.internal.simulation

import org.mobilenativefoundation.trails.server.ConflictMode
import org.mobilenativefoundation.trails.server.error.ServerError
import kotlin.random.Random

class ConflictSimulator(
    private var mode: ConflictMode,
    private var probability: Float,
) {
    private var firstWriteConflicted = false
    private var manualConflictPending = false

    fun updateMode(mode: ConflictMode, probability: Float = 0.0f) {
        this.mode = mode
        this.probability = probability
    }

    fun checkForConflict(
        resourceType: String,
        id: String,
        clientVersion: Long,
        serverVersion: Long,
    ): ServerError.Conflict? {
        // Always check real version mismatch first
        if (clientVersion < serverVersion) {
            return ServerError.Conflict(resourceType, id, clientVersion, serverVersion)
        }

        // Then check simulation modes
        val shouldConflict = when (mode) {
            ConflictMode.DISABLED -> false
            ConflictMode.FIRST_WRITE -> {
                if (!firstWriteConflicted) {
                    firstWriteConflicted = true
                    true
                } else false
            }
            ConflictMode.EVERY_WRITE -> true
            ConflictMode.RANDOM -> Random.nextFloat() < probability
            ConflictMode.MANUAL -> {
                if (manualConflictPending) {
                    manualConflictPending = false
                    true
                } else false
            }
        }

        return if (shouldConflict) {
            // Simulate a "stale read" scenario by incrementing server version
            ServerError.Conflict(resourceType, id, clientVersion, serverVersion + 1)
        } else null
    }

    fun triggerNextConflict() {
        manualConflictPending = true
    }

    fun reset() {
        firstWriteConflicted = false
        manualConflictPending = false
    }
}
```

---

## 7. Seeding Strategy

### 7.1 Seed Data Loader

```kotlin
// server:fake - internal/seed/SeedDataLoader.kt
package org.mobilenativefoundation.trails.server.fake.internal.seed

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.trails.server.SeedScenario
import org.mobilenativefoundation.trails.server.fake.internal.storage.BackendTables

class SeedDataLoader(
    private val tables: BackendTables,
) {
    private var currentScenario: SeedScenario? = null
    private val mutex = Mutex()

    suspend fun ensureSeeded(scenario: SeedScenario) = mutex.withLock {
        if (currentScenario == scenario) return@withLock

        // Clear existing data if switching scenarios
        if (currentScenario != null) {
            clearAllTables()
        }

        currentScenario = scenario

        when (scenario) {
            SeedScenario.DEFAULT -> loadDefaultSeed()
            SeedScenario.EMPTY -> { /* no-op */ }
            SeedScenario.LARGE_DATASET -> loadLargeDatasetSeed()
            SeedScenario.CONFLICT_TESTING -> loadConflictTestingSeed()
            SeedScenario.ERROR_STATES -> loadErrorStatesSeed()
        }
    }

    private suspend fun clearAllTables() {
        tables.users().clear()
        tables.sessions().clear()
        tables.follows().clear()
        tables.posts().clear()
        tables.postEngagements().clear()
        tables.comments().clear()
        tables.resorts().clear()
        tables.runs().clear()
        tables.weather().clear()
        tables.userFavorites().clear()
    }

    private suspend fun loadDefaultSeed() {
        val seedData = DefaultSeedData()
        tables.users().putAll(seedData.users)
        tables.resorts().putAll(seedData.resorts)
        tables.runs().putAll(seedData.runs)
        tables.weather().putAll(seedData.weather)
        tables.posts().putAll(seedData.posts)
        tables.follows().putAll(seedData.follows)
    }

    private suspend fun loadLargeDatasetSeed() {
        val generator = LargeDatasetGenerator()
        tables.users().putAll(generator.users)
        tables.resorts().putAll(generator.resorts)
        tables.runs().putAll(generator.runs)
        tables.posts().putAll(generator.generatePosts(count = 1000))
        tables.weather().putAll(generator.weather)
    }

    private suspend fun loadConflictTestingSeed() {
        // Load default seed with specific versions for conflict testing
        val seedData = DefaultSeedData()
        tables.users().putAll(seedData.users)
        tables.resorts().putAll(seedData.resorts)
        tables.runs().putAll(seedData.runs)
        tables.weather().putAll(seedData.weather)
        // Posts with version = 5 to make conflicts more visible
        tables.posts().putAll(seedData.posts.map { it.copy(version = 5) })
    }

    private suspend fun loadErrorStatesSeed() {
        // Minimal data to test error conditions
        val seedData = DefaultSeedData()
        tables.users().putAll(seedData.users.take(1))
        tables.resorts().putAll(seedData.resorts.take(1))
        tables.runs().putAll(seedData.runs.take(1))
    }

    suspend fun reset() = mutex.withLock {
        clearAllTables()
        currentScenario = null
    }
}
```

### 7.2 Default Seed Data

```kotlin
// server:fake - internal/seed/DefaultSeedData.kt
package org.mobilenativefoundation.trails.server.fake.internal.seed

import org.mobilenativefoundation.trails.server.fake.internal.storage.*
import org.mobilenativefoundation.trails.model.domain.feed.BackgroundGradient
import org.mobilenativefoundation.trails.model.domain.feed.Emoji
import org.mobilenativefoundation.trails.model.domain.feed.TrailDifficulty
import kotlin.math.absoluteValue

class DefaultSeedData {

    // Authors (migrated from FakePostDatabase.Authors)
    val users: List<StoredUser> = listOf(
        StoredUser(
            id = "author_matt",
            email = "matt@example.com",
            passwordHash = "hashed_password",
            firstName = "Matt",
            lastName = null,
            displayName = "Matt",
            username = "matt",
            avatarUrl = "https://images.unsplash.com/photo-1565992441121-4367c2967103?w=800",
            bio = "Skiing enthusiast",
            location = "Colorado",
            verified = true,
            createdAt = 1704067200000, // Jan 1, 2024
            version = 1,
        ),
        StoredUser(
            id = "author_lily",
            email = "lily@example.com",
            passwordHash = "hashed_password",
            firstName = "Lily",
            lastName = null,
            displayName = "Lily",
            username = "lily",
            avatarUrl = "https://images.unsplash.com/photo-1486684338211-1a7ced564b0d?w=800",
            bio = null,
            location = "Utah",
            verified = false,
            createdAt = 1704067200000,
            version = 1,
        ),
        StoredUser(
            id = "author_ty",
            email = "ty@example.com",
            passwordHash = "hashed_password",
            firstName = "Ty",
            lastName = null,
            displayName = "Ty",
            username = "ty",
            avatarUrl = "https://images.unsplash.com/photo-1551524559-8af4e6624178?w=800",
            bio = "Powder chaser",
            location = "Wyoming",
            verified = true,
            createdAt = 1704067200000,
            version = 1,
        ),
        StoredUser(
            id = "author_bryce",
            email = "bryce@example.com",
            passwordHash = "hashed_password",
            firstName = "Bryce",
            lastName = null,
            displayName = "Bryce",
            username = "bryce",
            avatarUrl = "https://images.unsplash.com/photo-1596473537047-50758f115d04?w=800",
            bio = null,
            location = "California",
            verified = false,
            createdAt = 1704067200000,
            version = 1,
        ),
    )

    // Resorts (migrated from FakePostDatabase.CatalogResorts)
    val resorts: List<StoredResort> = listOf(
        StoredResort(
            id = "resort_killington",
            name = "Killington",
            city = "Killington",
            state = "VT",
            country = "USA",
            latitude = 43.6045,
            longitude = -72.8201,
            elevation = 4241,
            runs = 155,
            lifts = 22,
            verticalFeet = 3050,
            skiableAcres = 1509,
            snowfallAnnualInches = 250,
            amenities = listOf("Night skiing", "Terrain park", "Snow tubing"),
            isOpen = true,
            liftsOpen = 18,
            runsOpen = 140,
            imageUrl = null,
            version = 1,
        ),
        StoredResort(
            id = "resort_beaver_creek",
            name = "Beaver Creek",
            city = "Avon",
            state = "CO",
            country = "USA",
            latitude = 39.6042,
            longitude = -106.5165,
            elevation = 8100,
            runs = 150,
            lifts = 25,
            verticalFeet = 4040,
            skiableAcres = 1832,
            snowfallAnnualInches = 310,
            amenities = listOf("Luxury lodging", "Gourmet dining", "Ski-in/ski-out"),
            isOpen = true,
            liftsOpen = 22,
            runsOpen = 145,
            imageUrl = null,
            version = 1,
        ),
        StoredResort(
            id = "resort_vail",
            name = "Vail",
            city = "Vail",
            state = "CO",
            country = "USA",
            latitude = 39.6403,
            longitude = -106.3742,
            elevation = 8120,
            runs = 195,
            lifts = 31,
            verticalFeet = 3450,
            skiableAcres = 5317,
            snowfallAnnualInches = 350,
            amenities = listOf("Back bowls", "Blue Sky Basin", "Adventure Ridge"),
            isOpen = true,
            liftsOpen = 28,
            runsOpen = 185,
            imageUrl = null,
            version = 1,
        ),
        // ... additional resorts (Zermatt, Jackson Hole, Chamonix, etc.)
    )

    // Runs (migrated from FakePostDatabase.CatalogRuns)
    val runs: List<StoredRun> = listOf(
        StoredRun(
            id = "run_killington_superstar",
            resortId = "resort_killington",
            name = "Superstar",
            difficulty = TrailDifficulty.BLACK_DIAMOND,
            lengthMiles = 1.0f,
            verticalFeet = 1500,
            averageGradePct = 28,
            maxGradePct = 35,
            isOpen = true,
            conditions = "Groomed",
            lastGroomed = "2025-01-11T06:00:00Z",
            liftAccess = listOf("Superstar Express"),
            features = listOf("moguls", "steep"),
            version = 1,
        ),
        StoredRun(
            id = "run_beaver_creek_birds_of_prey",
            resortId = "resort_beaver_creek",
            name = "Birds of Prey",
            difficulty = TrailDifficulty.BLACK_DIAMOND,
            lengthMiles = 2.0f,
            verticalFeet = 2470,
            averageGradePct = 32,
            maxGradePct = 40,
            isOpen = true,
            conditions = "Packed",
            lastGroomed = "2025-01-10T18:00:00Z",
            liftAccess = listOf("Centennial Express"),
            features = listOf("world cup course", "steep"),
            version = 1,
        ),
        StoredRun(
            id = "run_vail_riva_ridge",
            resortId = "resort_vail",
            name = "Riva Ridge",
            difficulty = TrailDifficulty.BLUE_SQUARE,
            lengthMiles = 4.0f,
            verticalFeet = 3050,
            averageGradePct = 18,
            maxGradePct = 25,
            isOpen = true,
            conditions = "Groomed",
            lastGroomed = "2025-01-11T05:00:00Z",
            liftAccess = listOf("Riva Bahn Express"),
            features = listOf("cruiser", "scenic"),
            version = 1,
        ),
        // ... additional runs
    )

    // Weather (generated per resort)
    val weather: List<StoredWeather> by lazy {
        resorts.map { resort ->
            val seed = resort.id.hashCode().absoluteValue
            val conditionsOptions = listOf("Powder", "Packed", "Groomed", "Wind buff", "Icy")
            StoredWeather(
                resortId = resort.id,
                observedAt = System.currentTimeMillis(),
                conditions = conditionsOptions[seed % conditionsOptions.size],
                temperatureF = 8 + (seed % 25),
                feelsLikeF = 5 + (seed % 20),
                windMph = 4 + (seed % 22),
                windDirection = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[seed % 8],
                humidity = 30 + (seed % 50),
                visibility = if (seed % 3 == 0) "moderate" else "good",
                uvIndex = 2 + (seed % 6),
                newSnow24h = seed % 10,
                newSnow48h = seed % 15,
                newSnow7d = seed % 30,
                baseDepth = 40 + (seed % 60),
                seasonTotal = 150 + (seed % 200),
            )
        }
    }

    // Posts (generated from runs)
    val posts: List<StoredPost> by lazy {
        val gradients = BackgroundGradient.entries
        val emojis = Emoji.entries

        runs.mapIndexed { index, run ->
            val seed = run.id.hashCode().absoluteValue
            val author = users[seed % users.size]
            StoredPost(
                id = "post_${run.id}",
                authorId = author.id,
                runId = run.id,
                backgroundGradient = gradients[seed % gradients.size],
                emoji = emojis[seed % emojis.size],
                caption = null,
                createdAt = System.currentTimeMillis() - (index * 3600000L), // Stagger by 1 hour
                version = 1,
                likes = 0,
                comments = 0,
                shares = 0,
                views = 0,
            )
        }
    }

    // Follow relationships (users follow each other)
    val follows: List<StoredFollow> by lazy {
        listOf(
            StoredFollow("author_matt", "author_lily", System.currentTimeMillis()),
            StoredFollow("author_lily", "author_matt", System.currentTimeMillis()),
            StoredFollow("author_ty", "author_matt", System.currentTimeMillis()),
        )
    }
}
```

### 7.3 Large Dataset Generator

```kotlin
// server:fake - internal/seed/LargeDatasetGenerator.kt
package org.mobilenativefoundation.trails.server.fake.internal.seed

import org.mobilenativefoundation.trails.server.fake.internal.storage.*
import org.mobilenativefoundation.trails.model.domain.feed.BackgroundGradient
import org.mobilenativefoundation.trails.model.domain.feed.Emoji
import org.mobilenativefoundation.trails.model.domain.feed.TrailDifficulty

class LargeDatasetGenerator {

    private val defaultSeed = DefaultSeedData()

    val users: List<StoredUser> = defaultSeed.users + generateAdditionalUsers(100)
    val resorts: List<StoredResort> = defaultSeed.resorts
    val runs: List<StoredRun> = defaultSeed.runs
    val weather: List<StoredWeather> = defaultSeed.weather

    fun generatePosts(count: Int): List<StoredPost> {
        val gradients = BackgroundGradient.entries
        val emojis = Emoji.entries

        return (0 until count).map { index ->
            val run = runs[index % runs.size]
            val author = users[index % users.size]
            StoredPost(
                id = "post_generated_$index",
                authorId = author.id,
                runId = run.id,
                backgroundGradient = gradients[index % gradients.size],
                emoji = emojis[index % emojis.size],
                caption = if (index % 3 == 0) "Great conditions today!" else null,
                createdAt = System.currentTimeMillis() - (index * 60000L), // 1 minute apart
                version = 1,
                likes = index % 100,
                comments = index % 20,
                shares = index % 10,
                views = index * 5,
            )
        }
    }

    private fun generateAdditionalUsers(count: Int): List<StoredUser> {
        return (0 until count).map { index ->
            StoredUser(
                id = "user_generated_$index",
                email = "user$index@example.com",
                passwordHash = "hashed",
                firstName = "User",
                lastName = "$index",
                displayName = "User $index",
                username = "user$index",
                avatarUrl = null,
                bio = null,
                location = null,
                verified = index % 10 == 0,
                createdAt = System.currentTimeMillis(),
                version = 1,
            )
        }
    }
}
```

---

## 8. Configuration Integration

### 8.1 Extended Developer Settings

```kotlin
// data:devsettings:api - DeveloperSettings.kt (extended)
package org.mobilenativefoundation.trails.data.devsettings

import org.mobilenativefoundation.trails.server.ConflictMode
import org.mobilenativefoundation.trails.server.BackendConfig
import org.mobilenativefoundation.trails.server.SeedScenario
import kotlin.time.Duration.Companion.milliseconds

data class DeveloperSettings(
    // Existing fields
    val offlineMode: Boolean = false,
    val conflictsEnabled: Boolean = false,
    val conflictStrategy: ConflictStrategy = ConflictStrategy.SERVER_WINS,

    // New: Backend configuration
    val backend: BackendSettings = BackendSettings(),
)

data class BackendSettings(
    val latencyMinMs: Long = 50,
    val latencyMaxMs: Long = 200,
    val errorRate: Float = 0.0f,
    val rateLimitPerMinute: Int = 0,
    val conflictMode: String = "DISABLED",
    val conflictProbability: Float = 0.0f,
    val seedScenario: String = "DEFAULT",
) {
    fun toConfig(): BackendConfig = BackendConfig(
        latencyRange = latencyMinMs.milliseconds..latencyMaxMs.milliseconds,
        errorRate = errorRate,
        rateLimitRequestsPerMinute = rateLimitPerMinute,
        conflictMode = ConflictMode.valueOf(conflictMode),
        conflictProbability = conflictProbability,
        seedScenario = SeedScenario.valueOf(seedScenario),
    )
}
```

### 8.2 Developer Settings Repository Extension

```kotlin
// data:devsettings:api - DeveloperSettingsRepository.kt (extended)
package org.mobilenativefoundation.trails.data.devsettings

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.trails.server.ConflictMode
import org.mobilenativefoundation.trails.server.SeedScenario

interface DeveloperSettingsRepository {
    // Existing methods
    val current: DeveloperSettings
    fun stream(): Flow<DeveloperSettings>
    suspend fun setOfflineMode(enabled: Boolean)
    suspend fun setConflictsEnabled(enabled: Boolean)
    suspend fun setConflictStrategy(strategy: ConflictStrategy)

    // New methods for backend control
    suspend fun setLatencyRange(minMs: Long, maxMs: Long)
    suspend fun setErrorRate(rate: Float)
    suspend fun setRateLimitPerMinute(limit: Int)
    suspend fun setConflictMode(mode: ConflictMode)
    suspend fun setConflictProbability(probability: Float)
    suspend fun setSeedScenario(scenario: SeedScenario)

    // Control methods
    suspend fun resetBackend()
    suspend fun triggerConflict()
}
```

---

## 9. Client Integration

### 9.1 BackendServer Facade

```kotlin
// server:api - BackendServer.kt
package org.mobilenativefoundation.trails.server

import org.mobilenativefoundation.trails.server.services.*

interface BackendServer {
    // Services (clean interface names)
    val userService: UserService
    val feedService: FeedService
    val postService: PostService
    val resortService: ResortService
    val runService: RunService
    val weatherService: WeatherService

    // Session management
    suspend fun currentToken(): String?
    suspend fun setCurrentToken(token: String?)

    // Control methods
    suspend fun reset()
    fun triggerConflict()
    suspend fun updateConfig(config: BackendConfig)
}
```

### 9.2 Fake Implementation

```kotlin
// server:fake - FakeBackendServer.kt
package org.mobilenativefoundation.trails.server.fake

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.trails.server.BackendConfig
import org.mobilenativefoundation.trails.server.BackendServer
import org.mobilenativefoundation.trails.server.fake.internal.seed.SeedDataLoader
import org.mobilenativefoundation.trails.server.fake.internal.simulation.*
import org.mobilenativefoundation.trails.server.fake.internal.storage.*
import org.mobilenativefoundation.trails.server.services.*

class FakeBackendServer(
    private val configProvider: () -> BackendConfig,
) : BackendServer {

    private val store = InMemoryBackendStore()
    private val tables = BackendTables(store)
    private var currentTokenValue: String? = null
    private val tokenMutex = Mutex()

    private val config: BackendConfig
        get() = configProvider()

    private val latencySimulator: LatencySimulator
        get() = LatencySimulator(config.latencyRange)

    private val errorSimulator: ErrorSimulator by lazy {
        ErrorSimulator(config.errorRate, config.rateLimitRequestsPerMinute)
    }

    private val conflictSimulator = ConflictSimulator(
        mode = config.conflictMode,
        probability = config.conflictProbability,
    )

    private val seedLoader = SeedDataLoader(tables)

    // Services (implementations of clean interfaces)
    override val userService: UserService by lazy {
        FakeUserService(tables, latencySimulator, errorSimulator, seedLoader, config)
    }

    override val feedService: FeedService by lazy {
        FakeFeedService(tables, latencySimulator, errorSimulator, seedLoader, config)
    }

    override val postService: PostService by lazy {
        FakePostService(tables, latencySimulator, errorSimulator, conflictSimulator, seedLoader, config)
    }

    override val resortService: ResortService by lazy {
        FakeResortService(tables, latencySimulator, errorSimulator, seedLoader, config)
    }

    override val runService: RunService by lazy {
        FakeRunService(tables, latencySimulator, errorSimulator, seedLoader, config)
    }

    override val weatherService: WeatherService by lazy {
        FakeWeatherService(tables, latencySimulator, errorSimulator, seedLoader, config)
    }

    // Token management
    override suspend fun currentToken(): String? = tokenMutex.withLock { currentTokenValue }

    override suspend fun setCurrentToken(token: String?) = tokenMutex.withLock {
        currentTokenValue = token
    }

    // Control methods
    override suspend fun reset() {
        store.clear()
        conflictSimulator.reset()
        errorSimulator.reset()
        seedLoader.reset()
        setCurrentToken(null)
    }

    override fun triggerConflict() {
        conflictSimulator.triggerNextConflict()
    }

    override suspend fun updateConfig(config: BackendConfig) {
        conflictSimulator.updateMode(config.conflictMode, config.conflictProbability)
        // Re-seed if scenario changed
        seedLoader.ensureSeeded(config.seedScenario)
    }
}
```

### 9.3 Updated PostApi Implementation

```kotlin
// data:post:impl - RealPostApi.kt (refactored to use server API)
package org.mobilenativefoundation.trails.data.post

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.server.BackendServer
import org.mobilenativefoundation.trails.server.pagination.CursorRequest
import org.mobilenativefoundation.trails.server.model.SkiRunPostRecord
import org.mobilenativefoundation.trails.model.network.feed.FeedPostRecord

@ContributesBinding(AppScope::class)
@Inject
class RealPostApi(
    private val backendServer: BackendServer,
) : PostApi {

    override suspend fun fetchFeed(feedKey: FeedKey): List<FeedPostRecord> {
        val token = backendServer.currentToken()
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

    override suspend fun updateFeed(
        feedKey: FeedKey,
        posts: List<FeedPostRecord>
    ): List<FeedPostRecord> {
        val token = backendServer.currentToken()
            ?: throw IllegalStateException("Not authenticated")

        posts.forEach { post ->
            backendServer.postService.updatePost(
                token = token,
                postId = post.id,
                update = UpdatePostRequest(version = post.version),
            )
        }
        return fetchFeed(feedKey)
    }
}

// Extension to convert from backend model to network model
private fun SkiRunPostRecord.toFeedPostRecord(): FeedPostRecord {
    return FeedPostRecord(
        id = id,
        author = FeedPostAuthorRecord(
            id = author.id,
            username = author.username,
            displayName = author.displayName,
            avatar = author.avatarUrl.orEmpty(),
            verified = author.verified,
            isFollowing = author.isFollowing,
        ),
        run = FeedPostRunRecord(
            id = run.id,
            resortId = run.resortId,
            name = run.name,
            distance = run.distance,
            vertical = "${run.verticalFeet} ft",
            duration = "N/A", // Calculated client-side
            topSpeed = "N/A",
            difficulty = run.difficulty,
            liftAccess = run.liftAccess,
        ),
        resort = FeedPostResortRecord(
            id = resort.id,
            name = resort.name,
            location = resort.location,
        ),
        weather = FeedPostWeatherRecord(
            id = "weather_${resort.id}",
            resortId = resort.id,
            observedAt = "",
            source = "resort_report",
            conditions = weather.conditions,
            temperatureF = weather.temperatureF,
            feelsLikeF = weather.temperatureF - 5,
            windMph = 10,
            snowfall24hIn = weather.snowfall24hIn,
            visibility = "good",
            summary = "${weather.conditions} conditions",
        ),
        engagement = FeedPostEngagementRecord(
            views = engagement.views,
            likes = engagement.likes,
            comments = engagement.comments,
            shares = engagement.shares,
            isLiked = engagement.isLiked,
            isBookmarked = engagement.isBookmarked,
        ),
        style = FeedPostStyleRecord(
            backgroundGradient = style.backgroundGradient,
            emoji = style.emoji,
        ),
        timestamp = timestamp,
        version = version,
    )
}
```

### 9.4 DI Bindings

```kotlin
// di:graph:app - BackendModule.kt (new file)
package org.mobilenativefoundation.trails.di.graph.app

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Module
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.mobilenativefoundation.trails.data.devsettings.DeveloperSettingsRepository
import org.mobilenativefoundation.trails.server.BackendServer
import org.mobilenativefoundation.trails.server.fake.FakeBackendServer
import org.mobilenativefoundation.trails.server.services.*

@Module
interface BackendModule {

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideBackendServer(
            developerSettingsRepository: DeveloperSettingsRepository,
        ): BackendServer {
            // Fake implementation - could be swapped for real implementation
            return FakeBackendServer(
                configProvider = { developerSettingsRepository.current.backend.toConfig() }
            )
        }
    }

    @Binds
    fun bindUserService(server: BackendServer): UserService = server.userService

    @Binds
    fun bindFeedService(server: BackendServer): FeedService = server.feedService

    @Binds
    fun bindPostService(server: BackendServer): PostService = server.postService

    @Binds
    fun bindResortService(server: BackendServer): ResortService = server.resortService

    @Binds
    fun bindRunService(server: BackendServer): RunService = server.runService

    @Binds
    fun bindWeatherService(server: BackendServer): WeatherService = server.weatherService
}
```

---

## 10. Architecture Diagram

```
+------------------------------------------------------------------+
|                         Client Layer                              |
|  +------------------+  +------------------+  +------------------+  |
|  |  HomePresenter   |  |  ProfilePresenter|  |  ResortPresenter |  |
|  +--------+---------+  +--------+---------+  +--------+---------+  |
|           |                     |                     |           |
|  +--------v---------+  +--------v---------+  +--------v---------+  |
|  |  PostRepository  |  |  UserRepository  |  | ResortRepository |  |
|  +--------+---------+  +--------+---------+  +--------+---------+  |
|           |                     |                     |           |
|  +--------v---------+  +--------v---------+  +--------v---------+  |
|  |  Store<Post>     |  |  Store<User>     |  |  Store<Resort>   |  |
|  |  (Client Cache)  |  |  (Client Cache)  |  |  (Client Cache)  |  |
|  +--------+---------+  +--------+---------+  +--------+---------+  |
+-----------|---------------------|---------------------|------------+
            |                     |                     |
            v                     v                     v
+------------------------------------------------------------------+
|                         API Layer                                 |
|  +------------------+  +------------------+  +------------------+  |
|  |   RealPostApi    |  |  RealUserAuthApi |  |  RealResortApi   |  |
|  |  (implements     |  |  (implements     |  |  (implements     |  |
|  |   PostApi)       |  |   UserAuthApi)   |  |   ResortApi)     |  |
|  +--------+---------+  +--------+---------+  +--------+---------+  |
+-----------|---------------------|---------------------|------------+
            |                     |                     |
            +---------------------+---------------------+
                                  |
                                  v
+------------------------------------------------------------------+
|               server:api - Clean Interfaces                       |
|  +------------------------------------------------------------+  |
|  |               BackendServer (interface)                     |  |
|  |  +------------+  +------------+  +------------+             |  |
|  |  | UserService|  | FeedService|  | PostService|             |  |
|  |  +------------+  +------------+  +------------+             |  |
|  |  +------------+  +------------+  +-------------+            |  |
|  |  |ResortService| | RunService |  |WeatherService|           |  |
|  |  +------------+  +------------+  +-------------+            |  |
|  +------------------------------------------------------------+  |
+------------------------------------------------------------------+
            |
            | DI binds FakeBackendServer → BackendServer
            v
+------------------------------------------------------------------+
|               server:fake - Fake Implementations                  |
|  +------------------------------------------------------------+  |
|  |           FakeBackendServer (implements BackendServer)      |  |
|  |  +--------------------+  +--------------------+             |  |
|  |  | LatencySimulator   |  | ErrorSimulator     |             |  |
|  |  +--------------------+  +--------------------+             |  |
|  |  +--------------------+  +--------------------+             |  |
|  |  | ConflictSimulator  |  | SeedDataLoader     |             |  |
|  |  +--------------------+  +--------------------+             |  |
|  +------------------------------------------------------------+  |
|                                                                   |
|  +------------------------------------------------------------+  |
|  |                    Service Implementations                  |  |
|  |  +---------------+  +---------------+  +---------------+    |  |
|  |  | FakeUserSvc   |  | FakeFeedSvc   |  | FakePostSvc   |    |  |
|  |  | (→ UserService)  (→ FeedService)   (→ PostService)  |    |  |
|  |  +---------------+  +---------------+  +---------------+    |  |
|  |  +---------------+  +---------------+  +---------------+    |  |
|  |  | FakeResortSvc |  | FakeRunSvc    |  | FakeWeatherSvc|    |  |
|  |  | (→ ResortSvc) |  | (→ RunService)|  |(→ WeatherSvc) |    |  |
|  |  +---------------+  +---------------+  +---------------+    |  |
|  +------------------------------------------------------------+  |
+------------------------------------------------------------------+
            |
            v
+------------------------------------------------------------------+
|                      Storage Layer                                |
|  +----------------------------------------------------------+    |
|  |                 InMemoryBackendStore                      |    |
|  |  +-------+  +-------+  +-------+  +-------+  +-------+   |    |
|  |  | Users |  | Posts |  |Resorts|  | Runs  |  |Weather|   |    |
|  |  +-------+  +-------+  +-------+  +-------+  +-------+   |    |
|  |  +-------+  +-------+  +-------+  +---------+            |    |
|  |  |Follows|  |Comments| |Sessions| |Favorites|            |    |
|  |  +-------+  +-------+  +-------+  +---------+            |    |
|  +----------------------------------------------------------+    |
+------------------------------------------------------------------+
```

---

## 11. Migration Plan

### Phase 1: Create New Modules (Week 1)

1. Create `server/api` module
   - Add `build.gradle.kts`
   - Define all interfaces and models (clean names: `UserService`, `PostService`, etc.)
   - Add module to `settings.gradle.kts`

2. Create `server/fake` module
   - Add `build.gradle.kts`
   - Implement storage layer and fake services (`FakeUserService`, `FakePostService`, etc.)
   - Add module to `settings.gradle.kts`

### Phase 2: Implement Core Infrastructure (Week 1-2)

1. Implement `InMemoryBackendStore` and `BackendTable`
2. Implement simulation classes:
   - `LatencySimulator`
   - `ErrorSimulator`
   - `ConflictSimulator`
3. Implement `SeedDataLoader` with `DefaultSeedData`
4. Migrate data from `FakePostDatabase` to `DefaultSeedData`

### Phase 3: Implement Services (Week 2-3)

1. Implement `RealFakeUserService`
2. Implement `RealFakeFeedService`
3. Implement `RealFakePostService`
4. Implement `RealFakeResortService`
5. Implement `RealFakeRunService`
6. Implement `RealFakeWeatherService`

### Phase 4: Client Integration (Week 3)

1. Refactor `FakePostApi` to use `FakeBackendServer`
2. Refactor `FakeUserAuthApi` to use `FakeBackendServer`
3. Add DI bindings in `AppGraph`
4. Update `DeveloperSettings` with `FakeBackendSettings`

### Phase 5: Cleanup (Week 4)

1. Remove `backend_posts`, `backend_feed_posts`, `backend_user_state` tables from client SQLDelight
2. Delete deprecated fake data code:
   - `FakePostDatabase.kt` (keep reference for migration)
   - `FakePostDataFactory.kt`
   - `FakePostRecordFactories.kt`
3. Update `PostStoreFactory` to remove backend table references

### Phase 6: Documentation & Testing (Week 4)

1. Add unit tests for fake backend services
2. Add integration tests for client integration
3. Update `AGENTS.md` with new module documentation
4. Create developer guide for using fake backend

---

## 12. Testing Strategy

### 12.1 Unit Tests for Services

```kotlin
// server:fake - test/PostServiceTest.kt
class PostServiceTest {

    private val server: BackendServer = FakeBackendServer {
        BackendConfig(
            latencyRange = Duration.ZERO..Duration.ZERO, // No latency in tests
            conflictMode = ConflictMode.DISABLED,
            seedScenario = SeedScenario.DEFAULT,
        )
    }

    @BeforeTest
    fun setup() = runTest {
        // Authenticate
        val response = server.userService.login("matt@example.com", "password")
        server.setCurrentToken(response.accessToken)
    }

    @Test
    fun `like post increments like count`() = runTest {
        val token = server.currentToken()!!

        val feed = server.feedService.getHomeFeed(token, CursorRequest())
        val post = feed.items.first() as SkiRunPostRecord
        val initialLikes = post.engagement.likes

        server.postService.likePost(token, post.id)

        val updated = server.postService.getPost(post.id, token)
        assertEquals(initialLikes + 1, updated.engagement.likes)
        assertTrue(updated.engagement.isLiked)
    }

    @Test
    fun `unlike post decrements like count`() = runTest {
        val token = server.currentToken()!!

        val feed = server.feedService.getHomeFeed(token, CursorRequest())
        val post = feed.items.first() as SkiRunPostRecord

        // Like first
        server.postService.likePost(token, post.id)
        val likedPost = server.postService.getPost(post.id, token)

        // Then unlike
        server.postService.unlikePost(token, post.id)
        val unlikedPost = server.postService.getPost(post.id, token)

        assertEquals(likedPost.engagement.likes - 1, unlikedPost.engagement.likes)
        assertFalse(unlikedPost.engagement.isLiked)
    }

    @Test
    fun `bookmark post adds to bookmarks list`() = runTest {
        val token = server.currentToken()!!

        val feed = server.feedService.getHomeFeed(token, CursorRequest())
        val post = feed.items.first() as SkiRunPostRecord

        server.postService.bookmarkPost(token, post.id)

        val bookmarks = server.postService.getBookmarkedPosts(token, CursorRequest())
        assertTrue(bookmarks.items.any { it.id == post.id })
    }
}
```

### 12.2 Conflict Simulation Tests

```kotlin
// server:fake - test/ConflictSimulatorTest.kt
class ConflictSimulatorTest {

    @Test
    fun `FIRST_WRITE mode triggers conflict on first write only`() = runTest {
        val server: BackendServer = FakeBackendServer {
            BackendConfig(conflictMode = ConflictMode.FIRST_WRITE)
        }

        val response = server.userService.login("matt@example.com", "password")
        server.setCurrentToken(response.accessToken)

        val feed = server.feedService.getHomeFeed(response.accessToken, CursorRequest())
        val post = feed.items.first() as SkiRunPostRecord

        // First write should throw conflict
        assertFailsWith<ServerError.Conflict> {
            server.postService.updatePost(
                token = response.accessToken,
                postId = post.id,
                update = UpdatePostRequest(version = post.version)
            )
        }

        // Second write should succeed
        val updated = server.postService.updatePost(
            token = response.accessToken,
            postId = post.id,
            update = UpdatePostRequest(version = post.version + 1)
        )
        assertNotNull(updated)
    }

    @Test
    fun `MANUAL mode triggers conflict only when triggered`() = runTest {
        val server: BackendServer = FakeBackendServer {
            BackendConfig(conflictMode = ConflictMode.MANUAL)
        }

        val response = server.userService.login("matt@example.com", "password")
        server.setCurrentToken(response.accessToken)

        val feed = server.feedService.getHomeFeed(response.accessToken, CursorRequest())
        val post = feed.items.first() as SkiRunPostRecord

        // Should not conflict initially
        val updated1 = server.postService.updatePost(
            token = response.accessToken,
            postId = post.id,
            update = UpdatePostRequest(version = post.version)
        )
        assertNotNull(updated1)

        // Trigger conflict for next write
        server.triggerConflict()

        // Now should throw conflict
        assertFailsWith<ServerError.Conflict> {
            server.postService.updatePost(
                token = response.accessToken,
                postId = updated1.id,
                update = UpdatePostRequest(version = updated1.version)
            )
        }
    }
}
```

### 12.3 Pagination Tests

```kotlin
// server:fake - test/PaginationTest.kt
class PaginationTest {

    @Test
    fun `cursor pagination returns correct pages`() = runTest {
        val server: BackendServer = FakeBackendServer {
            BackendConfig(seedScenario = SeedScenario.LARGE_DATASET)
        }

        val response = server.userService.login("matt@example.com", "password")

        // First page
        val page1 = server.feedService.getHomeFeed(
            token = response.accessToken,
            request = CursorRequest(limit = 10)
        )
        assertEquals(10, page1.items.size)
        assertNotNull(page1.nextCursor)

        // Second page
        val page2 = server.feedService.getHomeFeed(
            token = response.accessToken,
            request = CursorRequest(cursor = page1.nextCursor, limit = 10)
        )
        assertEquals(10, page2.items.size)

        // Pages should have different items
        val page1Ids = page1.items.map { it.id }.toSet()
        val page2Ids = page2.items.map { it.id }.toSet()
        assertTrue(page1Ids.intersect(page2Ids).isEmpty())
    }

    @Test
    fun `offset pagination returns correct totals`() = runTest {
        val server: BackendServer = FakeBackendServer {
            BackendConfig(seedScenario = SeedScenario.DEFAULT)
        }

        val page = server.resortService.listResorts(
            request = OffsetRequest(offset = 0, limit = 5)
        )

        assertEquals(5, page.items.size)
        assertTrue(page.totalCount > 0)
        assertEquals(page.hasMore, page.offset + page.items.size < page.totalCount)
    }
}
```

---

## Appendix A: File Inventory

### New Files to Create

```
server/
├── api/                                    # Clean API contracts (no "Fake" prefix)
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/org/mobilenativefoundation/trails/server/
│       ├── BackendConfig.kt               # Configuration data class
│       ├── BackendServer.kt               # Main facade interface
│       ├── services/
│       │   ├── UserService.kt             # Clean interface
│       │   ├── FeedService.kt             # Clean interface
│       │   ├── PostService.kt             # Clean interface
│       │   ├── ResortService.kt           # Clean interface
│       │   ├── RunService.kt              # Clean interface
│       │   └── WeatherService.kt          # Clean interface
│       ├── model/
│       │   ├── UserModels.kt
│       │   ├── FeedModels.kt
│       │   ├── PostModels.kt
│       │   ├── ResortModels.kt
│       │   ├── RunModels.kt
│       │   └── WeatherModels.kt
│       ├── error/
│       │   └── ServerError.kt             # Clean error types
│       └── pagination/
│           └── Pagination.kt
└── fake/                                   # Fake implementations (uses "Fake" prefix)
    ├── build.gradle.kts
    └── src/
        ├── commonMain/kotlin/org/mobilenativefoundation/trails/server/fake/
        │   ├── FakeBackendServer.kt       # Implements BackendServer
        │   ├── internal/
        │   │   ├── storage/
        │   │   │   ├── InMemoryBackendStore.kt
        │   │   │   ├── BackendTable.kt
        │   │   │   ├── BackendTables.kt
        │   │   │   └── StorageModels.kt
        │   │   ├── simulation/
        │   │   │   ├── LatencySimulator.kt
        │   │   │   ├── ErrorSimulator.kt
        │   │   │   └── ConflictSimulator.kt
        │   │   └── seed/
        │   │       ├── SeedDataLoader.kt
        │   │       ├── DefaultSeedData.kt
        │   │       └── LargeDatasetGenerator.kt
        │   └── services/
        │       ├── FakeUserService.kt     # Implements UserService
        │       ├── FakeFeedService.kt     # Implements FeedService
        │       ├── FakePostService.kt     # Implements PostService
        │       ├── FakeResortService.kt   # Implements ResortService
        │       ├── FakeRunService.kt      # Implements RunService
        │       └── FakeWeatherService.kt  # Implements WeatherService
        └── commonTest/kotlin/org/mobilenativefoundation/trails/server/fake/
            ├── PostServiceTest.kt
            ├── ConflictSimulatorTest.kt
            └── PaginationTest.kt
```

### Files to Modify

```
multiplatform/data/post/impl/src/commonMain/kotlin/.../FakePostApi.kt
multiplatform/data/user/impl/src/commonMain/kotlin/.../FakeUserAuthApi.kt
multiplatform/data/devsettings/api/src/commonMain/kotlin/.../DeveloperSettings.kt
multiplatform/data/devsettings/api/src/commonMain/kotlin/.../DeveloperSettingsRepository.kt
multiplatform/di/graph/app/src/commonMain/kotlin/.../AppGraph.kt
settings.gradle.kts
```

### Files to Delete (after migration)

```
multiplatform/data/post/impl/src/commonMain/kotlin/.../FakePostDatabase.kt
multiplatform/data/post/impl/src/commonMain/kotlin/.../FakePostDataFactory.kt
multiplatform/data/post/impl/src/commonMain/kotlin/.../FakePostRecordFactories.kt
multiplatform/model/db/src/commonMain/sqldelight/.../BackendPosts.sq
multiplatform/model/db/src/commonMain/sqldelight/.../BackendUserState.sq
```

---

## Appendix B: Glossary

| Term | Definition |
|------|------------|
| **BackendServer** | Main facade interface providing access to all services (clean API contract) |
| **FakeBackendServer** | Fake implementation of BackendServer for testing/demo |
| **UserService** | Clean interface for user/auth operations |
| **FakeUserService** | Fake implementation of UserService |
| **Store5** | MobileNativeFoundation caching library with Fetcher/SourceOfTruth/Updater |
| **ConflictMode** | Enum controlling how version conflicts are simulated |
| **SeedScenario** | Enum selecting which test data set to load |
| **CursorPage** | Pagination response using opaque cursor tokens |
| **OffsetPage** | Pagination response using numeric offset/limit |
| **InMemoryBackendStore** | Thread-safe in-memory storage separate from client SQLDelight |
| **BackendTable** | Generic CRUD abstraction for in-memory storage |

---

*End of Document*
