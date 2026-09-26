package org.mobilenativefoundation.trails.data.post

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.data.auth.SessionStore
import org.mobilenativefoundation.trails.model.domain.feed.BackgroundGradient
import org.mobilenativefoundation.trails.model.domain.feed.Emoji
import org.mobilenativefoundation.trails.model.domain.feed.TrailDifficulty
import org.mobilenativefoundation.trails.model.network.feed.FeedPostAuthorRecord
import org.mobilenativefoundation.trails.model.network.feed.FeedPostEngagementRecord
import org.mobilenativefoundation.trails.model.network.feed.FeedPostRecord
import org.mobilenativefoundation.trails.model.network.feed.FeedPostResortRecord
import org.mobilenativefoundation.trails.model.network.feed.FeedPostRunRecord
import org.mobilenativefoundation.trails.model.network.feed.FeedPostStyleRecord
import org.mobilenativefoundation.trails.model.network.feed.FeedPostWeatherRecord
import org.mobilenativefoundation.trails.server.BackendServer
import org.mobilenativefoundation.trails.server.error.ServerError
import org.mobilenativefoundation.trails.server.model.SkiRunPostRecord
import org.mobilenativefoundation.trails.server.pagination.CursorRequest
import kotlin.math.roundToInt

@ContributesBinding(AppScope::class)
@Inject
class RealPostApi(
    private val backendServer: BackendServer,
    private val sessionStore: SessionStore,
) : PostApi {

    override suspend fun fetchFeed(feedKey: FeedKey): List<FeedPostRecord> = withTranslatedErrors {
        val request = CursorRequest(limit = MAX_FEED_PAGE_SIZE)
        val token = sessionStore.currentToken()
        fetchServerFeed(feedKey, token, request)
            .items
            .mapNotNull { it as? SkiRunPostRecord }
            .map { it.toNetworkRecord() }
    }

    override suspend fun updateFeed(feedKey: FeedKey, posts: List<FeedPostRecord>): List<FeedPostRecord> =
        withTranslatedErrors {
            val token = sessionStore.currentToken() ?: throw ServerError.Unauthorized()
            val currentPosts = fetchFeed(feedKey)
            val currentById = currentPosts.associateBy { it.id }

            posts.forEach { desired ->
                val existing = currentById[desired.id] ?: return@forEach
                if (desired.engagement.isLiked != existing.engagement.isLiked) {
                    if (desired.engagement.isLiked) {
                        backendServer.postService.likePost(token, desired.id)
                    } else {
                        backendServer.postService.unlikePost(token, desired.id)
                    }
                }

                if (desired.engagement.isBookmarked != existing.engagement.isBookmarked) {
                    if (desired.engagement.isBookmarked) {
                        backendServer.postService.bookmarkPost(token, desired.id)
                    } else {
                        backendServer.postService.unbookmarkPost(token, desired.id)
                    }
                }
            }

            val currentByAuthorId = currentPosts.associateBy { it.author.id }
            val desiredByAuthorId = posts.associateBy { it.author.id }
            desiredByAuthorId.forEach { (authorId, desiredPost) ->
                if (authorId.isBlank()) return@forEach
                val existing = currentByAuthorId[authorId] ?: return@forEach
                if (desiredPost.author.isFollowing != existing.author.isFollowing) {
                    if (desiredPost.author.isFollowing) {
                        backendServer.userService.followUser(token, authorId)
                    } else {
                        backendServer.userService.unfollowUser(token, authorId)
                    }
                }
            }

            fetchFeed(feedKey)
        }

    private suspend fun fetchServerFeed(
        feedKey: FeedKey,
        token: String?,
        request: CursorRequest,
    ) = when {
        feedKey.value == FeedKey.Home.value && token != null ->
            backendServer.feedService.getHomeFeed(token, request)

        feedKey.value == FeedKey.Home.value ->
            backendServer.feedService.getTrendingFeed(request)

        feedKey.value == FEED_KEY_FOLLOWING && token != null ->
            backendServer.feedService.getFollowingFeed(token, request)

        feedKey.userIdOrNull() != null ->
            backendServer.feedService.getUserFeed(feedKey.userIdOrNull().orEmpty(), request)

        feedKey.resortIdOrNull() != null ->
            backendServer.feedService.getResortFeed(feedKey.resortIdOrNull().orEmpty(), request)

        else -> backendServer.feedService.getTrendingFeed(request)
    }

    private suspend fun SkiRunPostRecord.toNetworkRecord(): FeedPostRecord {
        val runDistanceMiles = parseMiles(run.distance)
        val topSpeed = estimateTopSpeed(run.difficulty)
        val durationMinutes = estimateDurationMinutes(runDistanceMiles)

        return FeedPostRecord(
            id = id,
            author = FeedPostAuthorRecord(
                id = author.id,
                username = author.username,
                displayName = author.displayName,
                avatar = author.avatarUrl ?: DEFAULT_AVATAR_URL,
                verified = author.verified,
                isFollowing = author.isFollowing,
            ),
            run = FeedPostRunRecord(
                id = run.id,
                resortId = run.resortId,
                name = run.name,
                distance = run.distance,
                vertical = "${run.verticalFeet} ft",
                duration = "${durationMinutes}m",
                topSpeed = "${topSpeed} mph",
                difficulty = run.difficulty.toDomainDifficulty(),
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
                source = "server",
                conditions = weather.conditions,
                temperature = "${weather.temperatureF}F",
                temperatureF = weather.temperatureF,
                feelsLikeF = weather.temperatureF,
                windMph = null,
                snowfall24hIn = weather.snowfall24hIn,
                visibility = "",
                summary = weather.conditions,
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
                backgroundGradient = style.backgroundGradientId.value.toBackgroundGradient(),
                emoji = style.emojiId.value.toEmoji(),
            ),
            timestamp = timestamp,
            version = version,
        )
    }

    private fun org.mobilenativefoundation.trails.server.model.TrailDifficulty.toDomainDifficulty(): TrailDifficulty =
        when (this) {
            org.mobilenativefoundation.trails.server.model.TrailDifficulty.GREEN_CIRCLE -> TrailDifficulty.GREEN_CIRCLE
            org.mobilenativefoundation.trails.server.model.TrailDifficulty.BLUE_SQUARE -> TrailDifficulty.BLUE_SQUARE
            org.mobilenativefoundation.trails.server.model.TrailDifficulty.BLACK_DIAMOND -> TrailDifficulty.BLACK_DIAMOND
            org.mobilenativefoundation.trails.server.model.TrailDifficulty.DOUBLE_BLACK -> TrailDifficulty.DOUBLE_BLACK_DIAMOND
        }

    private fun String.toBackgroundGradient(): BackgroundGradient =
        runCatching { BackgroundGradient.valueOf(this) }
            .getOrDefault(BackgroundGradient.INDIGO_BLUE_CYAN)

    private fun String.toEmoji(): Emoji =
        runCatching { Emoji.valueOf(this) }.getOrDefault(Emoji.Skier)

    private fun parseMiles(distance: String): Float? =
        distance
            .substringBefore("mi")
            .trim()
            .toFloatOrNull()

    private fun estimateDurationMinutes(distanceMiles: Float?): Int {
        val miles = distanceMiles ?: DEFAULT_DISTANCE_MILES
        return (miles * 9f).roundToInt().coerceAtLeast(8)
    }

    private fun estimateTopSpeed(difficulty: org.mobilenativefoundation.trails.server.model.TrailDifficulty): Int =
        when (difficulty) {
            org.mobilenativefoundation.trails.server.model.TrailDifficulty.GREEN_CIRCLE -> 25
            org.mobilenativefoundation.trails.server.model.TrailDifficulty.BLUE_SQUARE -> 35
            org.mobilenativefoundation.trails.server.model.TrailDifficulty.BLACK_DIAMOND -> 45
            org.mobilenativefoundation.trails.server.model.TrailDifficulty.DOUBLE_BLACK -> 55
        }

    private fun FeedKey.userIdOrNull(): String? =
        value.removePrefix("user:")
            .takeIf { value.startsWith("user:") && it.isNotBlank() }

    private fun FeedKey.resortIdOrNull(): String? =
        value.removePrefix("resort:")
            .takeIf { value.startsWith("resort:") && it.isNotBlank() }

    private suspend fun <T> withTranslatedErrors(block: suspend () -> T): T =
        try {
            block()
        } catch (error: ServerError.Conflict) {
            throw PostConflictException(
                "Conflict for ${error.resourceType}:${error.id} " +
                    "(client=${error.clientVersion}, server=${error.serverVersion})",
            )
        }

    private companion object {
        private const val FEED_KEY_FOLLOWING = "following"
        private const val MAX_FEED_PAGE_SIZE = 100
        private const val DEFAULT_AVATAR_URL = "https://images.unsplash.com/photo-1565992441121-4367c2967103?w=800&auto=format&fit=crop&q=60"
        private const val DEFAULT_DISTANCE_MILES = 3.0f
    }
}
