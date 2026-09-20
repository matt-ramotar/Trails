package org.mobilenativefoundation.trails.server.fake.services

import org.mobilenativefoundation.trails.server.BackendConfigProvider
import org.mobilenativefoundation.trails.server.error.ServerError
import org.mobilenativefoundation.trails.server.fake.internal.seed.SeedDataLoader
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ErrorSimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.LatencySimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.NetworkGate
import org.mobilenativefoundation.trails.server.fake.internal.storage.BackendTables
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredPost
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredRun
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredUser
import org.mobilenativefoundation.trails.server.model.EngagementRecord
import org.mobilenativefoundation.trails.server.model.FeedItemRecord
import org.mobilenativefoundation.trails.server.model.ResortSummary
import org.mobilenativefoundation.trails.server.model.RunSummary
import org.mobilenativefoundation.trails.server.model.SkiRunPostRecord
import org.mobilenativefoundation.trails.server.model.StyleRecord
import org.mobilenativefoundation.trails.server.model.UserSummary
import org.mobilenativefoundation.trails.server.model.WeatherSummary
import org.mobilenativefoundation.trails.server.pagination.CursorPage
import org.mobilenativefoundation.trails.server.pagination.CursorRequest
import org.mobilenativefoundation.trails.server.services.FeedService

internal class FakeFeedService(
    tables: BackendTables,
    networkGate: NetworkGate,
    latencySimulator: LatencySimulator,
    errorSimulator: ErrorSimulator,
    seedLoader: SeedDataLoader,
    configProvider: BackendConfigProvider,
) : BaseFakeService(tables, networkGate, latencySimulator, errorSimulator, seedLoader, configProvider), FeedService {

    override suspend fun getHomeFeed(
        token: String,
        request: CursorRequest,
    ): CursorPage<FeedItemRecord> = withSimulation {
        val viewerId = userIdForToken(token)
        val posts = tables.posts().getAll().sortedByDescending { it.createdAt }
        val orderedPosts = orderForVariety(posts)
        paginatePosts(orderedPosts, request, viewerId)
    }

    override suspend fun getUserFeed(
        userId: String,
        request: CursorRequest,
    ): CursorPage<FeedItemRecord> = withSimulation {
        val posts = tables.posts().query { it.authorId == userId }.sortedByDescending { it.createdAt }
        paginatePosts(posts, request, null)
    }

    override suspend fun getFollowingFeed(
        token: String,
        request: CursorRequest,
    ): CursorPage<FeedItemRecord> = withSimulation {
        val viewerId = userIdForToken(token)
        val followees = tables.follows().query { it.followerId == viewerId }.map { it.followeeId }.toSet()
        val posts = tables.posts().query { followees.contains(it.authorId) }.sortedByDescending { it.createdAt }
        val orderedPosts = orderForVariety(posts)
        paginatePosts(orderedPosts, request, viewerId)
    }

    override suspend fun getResortFeed(
        resortId: String,
        request: CursorRequest,
    ): CursorPage<FeedItemRecord> = withSimulation {
        val runs = tables.runs().query { it.resortId == resortId }.map { it.id }.toSet()
        val posts = tables.posts().query { runs.contains(it.runId) }.sortedByDescending { it.createdAt }
        val orderedPosts = orderForVariety(posts)
        paginatePosts(orderedPosts, request, null)
    }

    override suspend fun getTrendingFeed(request: CursorRequest): CursorPage<FeedItemRecord> = withSimulation {
        val posts = tables.posts().getAll().sortedByDescending { it.createdAt }
        val orderedPosts = orderForVariety(posts)
        paginatePosts(orderedPosts, request, null)
    }

    private suspend fun paginatePosts(
        posts: List<StoredPost>,
        request: CursorRequest,
        viewerId: String?,
    ): CursorPage<FeedItemRecord> {
        val page = paginate(posts, request)
        return CursorPage(
            items = page.items.map { postToRecord(it, viewerId) },
            nextCursor = page.nextCursor,
            prevCursor = page.prevCursor,
            totalCount = page.totalCount,
        )
    }

    private fun orderForVariety(posts: List<StoredPost>): List<StoredPost> {
        val remaining = posts.toMutableList()
        val ordered = ArrayList<StoredPost>(posts.size)

        while (remaining.isNotEmpty()) {
            val previous = ordered.lastOrNull()
            val nextIndex = if (previous == null) {
                0
            } else {
                remaining.indexOfFirst { post ->
                    post.authorId != previous.authorId && post.emojiId != previous.emojiId
                }.coerceAtLeast(0)
            }
            // Keep every post, even when no remaining item can avoid an adjacent repeat.
            ordered.add(remaining.removeAt(nextIndex))
        }

        return ordered
    }

    private fun <T> paginate(items: List<T>, request: CursorRequest): CursorPage<T> {
        val limit = request.limit.coerceAtLeast(1)
        val size = items.size
        val cursor = request.cursor?.toIntOrNull()
        return if (request.direction == CursorRequest.Direction.BACKWARD) {
            val end = (cursor ?: size).coerceIn(0, size)
            val start = (end - limit).coerceAtLeast(0)
            CursorPage(
                items = items.subList(start, end),
                nextCursor = if (end < size) end.toString() else null,
                prevCursor = if (start > 0) start.toString() else null,
                totalCount = size,
            )
        } else {
            val start = (cursor ?: 0).coerceIn(0, size)
            val end = (start + limit).coerceAtMost(size)
            CursorPage(
                items = items.subList(start, end),
                nextCursor = if (end < size) end.toString() else null,
                prevCursor = if (start > 0) start.toString() else null,
                totalCount = size,
            )
        }
    }

    private suspend fun userIdForToken(token: String): String {
        val session = tables.sessions().get(token) ?: throw ServerError.Unauthorized()
        return session.userId
    }

    private suspend fun postToRecord(post: StoredPost, viewerId: String?): SkiRunPostRecord {
        val author = tables.users().get(post.authorId) ?: throw ServerError.NotFound("user", post.authorId)
        val run = tables.runs().get(post.runId) ?: throw ServerError.NotFound("run", post.runId)
        val resort = tables.resorts().get(run.resortId) ?: throw ServerError.NotFound("resort", run.resortId)
        val weather = tables.weather().get(resort.id)
        val engagement = viewerId?.let { tables.postEngagements().get("${it}:${post.id}") }

        return SkiRunPostRecord(
            id = post.id,
            timestamp = formatTimestamp(post.createdAt),
            author = toUserSummary(author, viewerId),
            run = toRunSummary(run),
            resort = toResortSummary(resort),
            weather = weather?.let { toWeatherSummary(it) } ?: WeatherSummary(
                conditions = "Unknown",
                temperatureF = 0,
                snowfall24hIn = 0,
            ),
            engagement = EngagementRecord(
                views = post.views,
                likes = post.likes,
                comments = post.comments,
                shares = post.shares,
                isLiked = engagement?.isLiked ?: false,
                isBookmarked = engagement?.isBookmarked ?: false,
            ),
            style = StyleRecord(
                backgroundGradientId = post.backgroundGradientId,
                emojiId = post.emojiId,
            ),
            caption = post.caption,
            version = post.version,
        )
    }

    private suspend fun toUserSummary(user: StoredUser, viewerId: String?): UserSummary {
        val isFollowing = viewerId?.let { tables.follows().exists("${it}:${user.id}") } ?: false
        return UserSummary(
            id = user.id,
            username = user.username,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            verified = user.verified,
            isFollowing = isFollowing,
        )
    }

    private fun toRunSummary(run: StoredRun): RunSummary {
        val distance = formatMiles(run.lengthMiles)
        val liftAccess = run.liftAccess.firstOrNull().orEmpty()
        return RunSummary(
            id = run.id,
            resortId = run.resortId,
            name = run.name,
            difficulty = run.difficulty,
            verticalFeet = run.verticalFeet,
            distance = distance,
            liftAccess = liftAccess,
        )
    }

    private fun toResortSummary(resort: org.mobilenativefoundation.trails.server.fake.internal.storage.StoredResort): ResortSummary {
        val location = if (resort.state != null) {
            "${resort.city}, ${resort.state}"
        } else {
            "${resort.city}, ${resort.country}"
        }
        return ResortSummary(
            id = resort.id,
            name = resort.name,
            location = location,
            imageUrl = resort.imageUrl,
        )
    }

    private fun toWeatherSummary(weather: org.mobilenativefoundation.trails.server.fake.internal.storage.StoredWeather): WeatherSummary {
        return WeatherSummary(
            conditions = weather.conditions,
            temperatureF = weather.temperatureF,
            snowfall24hIn = weather.newSnow24h,
        )
    }

    private fun formatMiles(value: Float): String {
        return if (value % 1.0f == 0.0f) {
            "${value.toInt()} mi"
        } else {
            "${(value * 10).toInt() / 10.0f} mi"
        }
    }

    private fun formatTimestamp(createdAt: Long): String {
        val base = BASE_TIMESTAMP_MS
        val diff = base - createdAt
        if (diff <= 0) return "just now"
        val hour = 3_600_000L
        val day = 86_400_000L
        return when {
            diff >= 2 * day -> "2d ago"
            diff >= day -> "1d ago"
            diff >= 7 * hour -> "7h ago"
            diff >= 5 * hour -> "5h ago"
            diff >= 3 * hour -> "3h ago"
            diff >= 2 * hour -> "2h ago"
            diff >= hour -> "1h ago"
            else -> "just now"
        }
    }

    companion object {
        private const val BASE_TIMESTAMP_MS = 1736596800000L
    }
}
