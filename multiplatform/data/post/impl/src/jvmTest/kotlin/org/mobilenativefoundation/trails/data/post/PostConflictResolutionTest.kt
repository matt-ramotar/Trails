package org.mobilenativefoundation.trails.data.post

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.trails.data.devsettings.ConflictStrategy
import org.mobilenativefoundation.trails.data.devsettings.DeveloperSettings
import org.mobilenativefoundation.trails.data.devsettings.RealDeveloperSettingsRepository
import org.mobilenativefoundation.trails.db.TrailsDatabase
import org.mobilenativefoundation.trails.db.TrailsDatabaseQueries
import org.mobilenativefoundation.trails.foundation.logging.Logger
import org.mobilenativefoundation.trails.model.domain.feed.BackgroundGradient
import org.mobilenativefoundation.trails.model.domain.feed.FeedPost
import org.mobilenativefoundation.trails.model.domain.feed.TrailDifficulty
import org.mobilenativefoundation.trails.model.network.feed.FeedPostRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class PostConflictResolutionTest {
    @Test
    fun serverWinsReplacesOptimisticValuesWithServerValues() = runTest {
        fixture(ConflictStrategy.SERVER_WINS).use { fixture ->
            fixture.repository.setLiked(FeedKey.Home, "post", true)

            assertEquals(fixture.api.concurrent, fixture.api.server)
            assertEquals(fixture.api.server, fixture.local())
            assertEquals(1, fixture.api.writes.size)
        }
    }

    @Test
    fun clientWinsRetriesTheOriginalLikeWithoutIncrementingTwice() = runTest {
        fixture(ConflictStrategy.CLIENT_WINS).use { fixture ->
            fixture.repository.setLiked(FeedKey.Home, "post", true)

            val expected = fixture.api.initial.copy(isLiked = true, likes = 11, version = 2)
            assertEquals(expected, fixture.api.server)
            assertEquals(expected, fixture.local())
            assertEquals(listOf(expected, expected), fixture.api.writes)
        }
    }

    @Test
    fun clientWinsRetriesABookmarkAlreadyWrittenOptimistically() = runTest {
        fixture(ConflictStrategy.CLIENT_WINS).use { fixture ->
            fixture.repository.setBookmarked(FeedKey.Home, "post", true)

            val expected = fixture.api.initial.copy(isBookmarked = true, version = 2)
            assertEquals(expected, fixture.api.server)
            assertEquals(expected, fixture.local())
            assertEquals(listOf(expected, expected), fixture.api.writes)
        }
    }

    @Test
    fun mergeRefreshesAndReappliesIntentToCurrentServerValues() = runTest {
        fixture(ConflictStrategy.MERGE).use { fixture ->
            fixture.repository.setLiked(FeedKey.Home, "post", true)

            val expected = fixture.api.concurrent.copy(isLiked = true, likes = 21, version = 3)
            assertEquals(expected, fixture.api.server)
            assertEquals(expected, fixture.local())
            assertEquals(2, fixture.api.writes.size)
        }
    }

    @Test
    fun mergeDoesNotRetryWhenFreshServerValuesCannotBeRead() = runTest {
        fixture(ConflictStrategy.MERGE).use { fixture ->
            fixture.api.failConflictRefresh = true

            fixture.repository.setLiked(FeedKey.Home, "post", true)

            assertEquals(1, fixture.api.writes.size)
            assertEquals(fixture.api.concurrent, fixture.api.server)
        }
    }

    @Test
    fun repeatedLikeIntentDoesNotChangeTheCountAgain() = runTest {
        fixture(ConflictStrategy.CLIENT_WINS).use { fixture ->
            fixture.api.conflictsRemaining = 0
            fixture.repository.setLiked(FeedKey.Home, "post", true)
            fixture.repository.setLiked(FeedKey.Home, "post", true)

            assertEquals(11, fixture.api.server.likes)
            assertEquals(fixture.api.server, fixture.local())
            assertEquals(1, fixture.api.writes.size)
        }
    }

    @Test
    fun clientWinsRetriesAtMostOnce() = runTest {
        fixture(ConflictStrategy.CLIENT_WINS).use { fixture ->
            fixture.api.conflictsRemaining = 3

            fixture.repository.setBookmarked(FeedKey.Home, "post", true)

            assertEquals(2, fixture.api.writes.size)
            assertEquals(fixture.api.concurrent, fixture.api.server)
        }
    }

    @Test
    fun successfulWriteAdoptsCanonicalServerCountsAndVersion() = runTest {
        fixture(ConflictStrategy.CLIENT_WINS).use { fixture ->
            val canonical = fixture.api.initial.copy(isLiked = true, likes = 21, comments = 7, version = 8)
            fixture.api.conflictsRemaining = 0
            fixture.api.successfulServerValue = canonical

            fixture.repository.setLiked(FeedKey.Home, "post", true)

            assertEquals(canonical, fixture.api.server)
            assertEquals(canonical, fixture.local())
            assertEquals(1, fixture.api.writes.size)
        }
    }

    @Test
    fun writeCancellationPropagatesThroughStoreAndRepository() = runTest {
        fixture(ConflictStrategy.CLIENT_WINS).use { fixture ->
            val cancellation = CancellationException("Update cancelled")
            fixture.api.writeFailure = cancellation

            assertSame(cancellation, assertFailsWith<CancellationException> {
                fixture.repository.setLiked(FeedKey.Home, "post", true)
            })
        }
    }

    private suspend fun TestScope.fixture(strategy: ConflictStrategy): Fixture {
        val fixture = Fixture(this)
        fixture.settings.update(DeveloperSettings(conflictStrategy = strategy))
        runCurrent()
        fixture.repository.refreshFeed(FeedKey.Home)
        assertEquals(fixture.api.initial, fixture.local())
        return fixture
    }

    private class Fixture(scope: TestScope) : AutoCloseable {
        private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { TrailsDatabase.Schema.create(it) }
        private val queries = TrailsDatabaseQueries(driver)
        private val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val settings = RealDeveloperSettingsRepository(scope.backgroundScope, queries, Logger(), dispatcher)
        val api = ConflictingPostApi()
        private val store = RealPostStore(PostStoreFactory(queries, api, dispatcher))
        val repository = RealPostRepository(store, Logger(), settings)

        suspend fun local(): FeedPost = repository.streamFeed(FeedKey.Home).first().single()

        override fun close() = driver.close()
    }

    // The network boundary deliberately rejects the first update while another writer changes
    // the server. SQLDelight, Store5, mappings, optimistic writes and repository policy are real.
    private class ConflictingPostApi : PostApi {
        val initial = FeedPost(
            id = "post", authorId = "author", username = "skier", displayName = "Skier", avatar = "avatar",
            verified = false, runId = "run", runName = "Run", resort = "Resort", resortId = "resort",
            runResortId = "resort", location = "Location", weatherId = "weather", weatherResortId = "resort",
            distance = "1 mi", vertical = "100 ft", duration = "2m", topSpeed = "10 mph",
            difficulty = TrailDifficulty.BLUE_SQUARE, views = 0, likes = 10, comments = 0, shares = 0,
            conditions = "Packed", temperature = "", backgroundGradient = BackgroundGradient.CYAN_BLUE_INDIGO,
            timestamp = "now", liftAccess = "Lift", version = 1,
        )
        val concurrent = initial.copy(likes = 20, comments = 7, version = 2)
        var server = initial
        var conflictsRemaining = 1
        var failConflictRefresh = false
        var writeFailure: Throwable? = null
        var successfulServerValue: FeedPost? = null
        val writes = mutableListOf<FeedPost>()

        override suspend fun fetchFeed(feedKey: FeedKey): List<FeedPostRecord> {
            if (failConflictRefresh && writes.isNotEmpty()) error("Fresh read failed")
            return listOf(server.toRecord())
        }

        override suspend fun updateFeed(feedKey: FeedKey, posts: List<FeedPostRecord>): List<FeedPostRecord> {
            val desired = posts.single().toDomain()
            writes += desired
            writeFailure?.let { throw it }
            if (conflictsRemaining > 0) {
                conflictsRemaining--
                server = concurrent
                throw PostConflictException("Concurrent update")
            }
            server = successfulServerValue ?: desired
            return listOf(server.toRecord())
        }
    }
}
