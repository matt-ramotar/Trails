package org.mobilenativefoundation.trails.server.fake.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.trails.server.BackendClock
import org.mobilenativefoundation.trails.server.BackendConfig
import org.mobilenativefoundation.trails.server.BackendConfigProvider
import org.mobilenativefoundation.trails.server.fake.internal.seed.SeedDataLoader
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ErrorSimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.LatencySimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.NetworkGate
import org.mobilenativefoundation.trails.server.fake.internal.simulation.SimulationRandomSource
import org.mobilenativefoundation.trails.server.fake.internal.storage.BackendTables
import org.mobilenativefoundation.trails.server.fake.internal.storage.InMemoryBackendStore
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredFollow
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredPost
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredResort
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredRun
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredSession
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredUser
import org.mobilenativefoundation.trails.server.model.BackgroundGradientId
import org.mobilenativefoundation.trails.server.model.EmojiId
import org.mobilenativefoundation.trails.server.model.FeedItemRecord
import org.mobilenativefoundation.trails.server.model.SkiRunPostRecord
import org.mobilenativefoundation.trails.server.model.TrailDifficulty
import org.mobilenativefoundation.trails.server.pagination.CursorPage
import org.mobilenativefoundation.trails.server.pagination.CursorRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class FakeFeedServiceNoRepeatTest {

    @Test
    fun authoredFeed_preservesEveryPostInChronologicalOrder() = runTest {
        val fixture = createFixture()

        val page = fixture.service.getUserFeed(USER_FEED_AUTHOR_ID, CursorRequest(limit = 10))

        assertEquals(listOf("post_4", "post_5", "post_6"), postIds(page))
        assertEquals(3, page.totalCount)
        assertEquals(null, page.nextCursor)
    }

    @Test
    fun allMixedFeedEndpoints_preserveMembershipAndCounts() = runTest {
        val fixture = createFixture()
        val request = CursorRequest(limit = 20)
        val pages = listOf(
            fixture.service.getHomeFeed(fixture.viewerToken, request),
            fixture.service.getTrendingFeed(request),
            fixture.service.getFollowingFeed(fixture.viewerToken, request),
            fixture.service.getResortFeed(fixture.resortId, request),
        )

        pages.forEach { page ->
            assertEquals(ALL_POST_IDS, postIds(page).toSet())
            assertEquals(ALL_POST_IDS.size, page.items.size)
            assertEquals(ALL_POST_IDS.size, page.totalCount)
            assertNoAdjacentAuthorOrEmojiRepeats(page.items.take(3))
        }
        assertEquals(postIds(pages.first()), postIds(fixture.service.getTrendingFeed(request)))
    }

    @Test
    fun forwardPagination_preservesEveryPostAcrossDifferentPageSizes() = runTest {
        val fixture = createFixture()
        val expected = postIds(fixture.service.getTrendingFeed(CursorRequest(limit = 20)))

        for (limit in 1..4) {
            val collected = mutableListOf<String>()
            var cursor: String? = null
            do {
                val page = fixture.service.getTrendingFeed(CursorRequest(cursor = cursor, limit = limit))
                assertEquals(ALL_POST_IDS.size, page.totalCount)
                assertTrue(page.items.isNotEmpty())
                collected += postIds(page)
                cursor = page.nextCursor
            } while (cursor != null)
            assertEquals(expected, collected)
            assertEquals(ALL_POST_IDS, collected.toSet())
        }
    }

    @Test
    fun backwardPagination_preservesEveryPostInTheSameOrder() = runTest {
        val fixture = createFixture()
        val expected = postIds(fixture.service.getTrendingFeed(CursorRequest(limit = 20)))
        val collected = mutableListOf<String>()
        var cursor: String? = null

        do {
            val page = fixture.service.getTrendingFeed(
                CursorRequest(cursor = cursor, limit = 2, direction = CursorRequest.Direction.BACKWARD),
            )
            assertEquals(ALL_POST_IDS.size, page.totalCount)
            collected.addAll(0, postIds(page))
            cursor = page.prevCursor
        } while (cursor != null)

        assertEquals(expected, collected)
        assertEquals(ALL_POST_IDS, collected.toSet())
    }

    @Test
    fun previousCursor_returnsThePreviousPageWithoutSkippingItsItems() = runTest {
        val fixture = createFixture()
        val first = fixture.service.getTrendingFeed(CursorRequest(limit = 2))
        val second = fixture.service.getTrendingFeed(CursorRequest(cursor = assertNotNull(first.nextCursor), limit = 2))
        val previous = fixture.service.getTrendingFeed(
            CursorRequest(
                cursor = assertNotNull(second.prevCursor),
                limit = 2,
                direction = CursorRequest.Direction.BACKWARD,
            ),
        )

        assertEquals(postIds(first), postIds(previous))
        assertNoAdjacentAuthorOrEmojiRepeats(first.items + second.items)
    }

    @Test
    fun authoredFeed_paginatedMembershipIncludesUnavoidableAuthorRepeats() = runTest {
        val fixture = createFixture()
        val first = fixture.service.getUserFeed(USER_FEED_AUTHOR_ID, CursorRequest(limit = 2))
        val second = fixture.service.getUserFeed(
            USER_FEED_AUTHOR_ID,
            CursorRequest(cursor = assertNotNull(first.nextCursor), limit = 2),
        )

        assertEquals(listOf("post_4", "post_5", "post_6"), postIds(first) + postIds(second))
        assertEquals(3, first.totalCount)
        assertEquals(3, second.totalCount)
        assertEquals(null, second.nextCursor)
    }

    private suspend fun createFixture(): FeedFixture {
        val configProvider = MutableConfigProvider(
            initial = BackendConfig(
                latencyRange = 0.milliseconds..0.milliseconds,
            ),
        )
        val randomSource = SimulationRandomSource(seed = 1337)
        val tables = BackendTables(InMemoryBackendStore())
        val service = FakeFeedService(
            tables = tables,
            networkGate = NetworkGate(configProvider),
            latencySimulator = LatencySimulator(configProvider, randomSource),
            errorSimulator = ErrorSimulator(configProvider, FixedClock, randomSource),
            seedLoader = SeedDataLoader(tables, seedDefaultData = false),
            configProvider = configProvider,
        )

        tables.resorts().put(
            StoredResort(
                id = RESORT_ID,
                name = "Resort One",
                city = "Aspen",
                state = "CO",
                country = "USA",
                latitude = 0.0,
                longitude = 0.0,
                elevation = 1000,
                runs = 7,
                lifts = 1,
                verticalFeet = 1000,
                skiableAcres = 100,
                snowfallAnnualInches = 200,
                amenities = emptyList(),
                isOpen = true,
                liftsOpen = 1,
                runsOpen = 7,
                imageUrl = null,
                version = 1,
            ),
        )

        tables.runs().putAll(
            listOf(
                storedRun("run_1"),
                storedRun("run_2"),
                storedRun("run_3"),
                storedRun("run_4"),
                storedRun("run_5"),
                storedRun("run_6"),
                storedRun("run_7"),
            ),
        )

        tables.users().putAll(
            listOf(
                storedUser(VIEWER_ID),
                storedUser("author_a"),
                storedUser("author_b"),
                storedUser(USER_FEED_AUTHOR_ID),
                storedUser("author_d"),
            ),
        )

        tables.sessions().put(
            StoredSession(
                token = VIEWER_TOKEN,
                refreshToken = "refresh_$VIEWER_TOKEN",
                userId = VIEWER_ID,
                expiresAt = Long.MAX_VALUE,
            ),
        )

        tables.follows().putAll(
            listOf(
                StoredFollow(VIEWER_ID, "author_a", createdAt = 1),
                StoredFollow(VIEWER_ID, "author_b", createdAt = 1),
                StoredFollow(VIEWER_ID, USER_FEED_AUTHOR_ID, createdAt = 1),
                StoredFollow(VIEWER_ID, "author_d", createdAt = 1),
            ),
        )

        tables.posts().putAll(
            listOf(
                storedPost(id = "post_1", authorId = "author_a", runId = "run_1", emoji = "emoji_1", createdAt = 7000),
                storedPost(id = "post_2", authorId = "author_a", runId = "run_2", emoji = "emoji_2", createdAt = 6000),
                storedPost(id = "post_3", authorId = "author_b", runId = "run_3", emoji = "emoji_2", createdAt = 5000),
                storedPost(id = "post_4", authorId = USER_FEED_AUTHOR_ID, runId = "run_4", emoji = "emoji_2", createdAt = 4000),
                storedPost(id = "post_5", authorId = USER_FEED_AUTHOR_ID, runId = "run_5", emoji = "emoji_3", createdAt = 3000),
                storedPost(id = "post_6", authorId = USER_FEED_AUTHOR_ID, runId = "run_6", emoji = "emoji_4", createdAt = 2000),
                storedPost(id = "post_7", authorId = "author_d", runId = "run_7", emoji = "emoji_4", createdAt = 1000),
            ),
        )

        return FeedFixture(
            service = service,
            viewerToken = VIEWER_TOKEN,
            resortId = RESORT_ID,
        )
    }

    private fun storedUser(id: String): StoredUser =
        StoredUser(
            id = id,
            email = "$id@example.com",
            passwordHash = "hashed_password",
            firstName = id,
            lastName = null,
            birthdate = null,
            displayName = id,
            username = id,
            avatarUrl = null,
            bio = null,
            location = null,
            verified = false,
            createdAt = 1,
            version = 1,
        )

    private fun storedRun(id: String): StoredRun =
        StoredRun(
            id = id,
            resortId = RESORT_ID,
            name = id,
            difficulty = TrailDifficulty.BLUE_SQUARE,
            lengthMiles = 1.0f,
            verticalFeet = 1000,
            averageGradePct = 10,
            maxGradePct = 20,
            isOpen = true,
            conditions = "Packed",
            lastGroomed = null,
            liftAccess = listOf("Lift 1"),
            features = emptyList(),
            version = 1,
        )

    private fun storedPost(
        id: String,
        authorId: String,
        runId: String,
        emoji: String,
        createdAt: Long,
    ): StoredPost =
        StoredPost(
            id = id,
            authorId = authorId,
            runId = runId,
            backgroundGradientId = BackgroundGradientId("gradient_1"),
            emojiId = EmojiId(emoji),
            caption = null,
            createdAt = createdAt,
            version = 1,
            likes = 0,
            comments = 0,
            shares = 0,
            views = 0,
        )

    private fun postIds(page: CursorPage<FeedItemRecord>): List<String> =
        page.items.filterIsInstance<SkiRunPostRecord>().map { it.id }

    private fun assertNoAdjacentAuthorOrEmojiRepeats(items: List<FeedItemRecord>) {
        val posts = items.filterIsInstance<SkiRunPostRecord>()
        for (index in 1 until posts.size) {
            val previous = posts[index - 1]
            val current = posts[index]
            assertNotEquals(previous.author.id, current.author.id)
            assertNotEquals(previous.style.emojiId, current.style.emojiId)
        }
    }

    private data class FeedFixture(
        val service: FakeFeedService,
        val viewerToken: String,
        val resortId: String,
    )

    private class MutableConfigProvider(initial: BackendConfig) : BackendConfigProvider {
        private val state = MutableStateFlow(initial)

        override val current: BackendConfig
            get() = state.value

        override val flow: StateFlow<BackendConfig> = state

        override fun update(config: BackendConfig) {
            state.value = config
        }
    }

    private object FixedClock : BackendClock {
        override fun nowMs(): Long = 0
    }

    private companion object {
        private const val VIEWER_ID = "viewer"
        private const val VIEWER_TOKEN = "viewer_token"
        private const val RESORT_ID = "resort_1"
        private const val USER_FEED_AUTHOR_ID = "author_c"
        private val ALL_POST_IDS = (1..7).map { "post_$it" }.toSet()
    }
}
