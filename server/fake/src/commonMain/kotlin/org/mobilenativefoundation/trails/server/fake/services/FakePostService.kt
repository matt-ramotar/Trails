package org.mobilenativefoundation.trails.server.fake.services

import org.mobilenativefoundation.trails.server.BackendClock
import org.mobilenativefoundation.trails.server.BackendConfigProvider
import org.mobilenativefoundation.trails.server.error.ServerError
import org.mobilenativefoundation.trails.server.fake.internal.seed.SeedDataLoader
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ConflictOutcome
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ConflictSimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ErrorSimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.LatencySimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.NetworkGate
import org.mobilenativefoundation.trails.server.fake.internal.storage.BackendTables
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredComment
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredPost
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredPostEngagement
import org.mobilenativefoundation.trails.server.model.CommentRecord
import org.mobilenativefoundation.trails.server.model.CreatePostRequest
import org.mobilenativefoundation.trails.server.model.EngagementRecord
import org.mobilenativefoundation.trails.server.model.ResortSummary
import org.mobilenativefoundation.trails.server.model.RunSummary
import org.mobilenativefoundation.trails.server.model.SkiRunPostRecord
import org.mobilenativefoundation.trails.server.model.StyleRecord
import org.mobilenativefoundation.trails.server.model.UpdatePostRequest
import org.mobilenativefoundation.trails.server.model.UserSummary
import org.mobilenativefoundation.trails.server.model.WeatherSummary
import org.mobilenativefoundation.trails.server.pagination.CursorPage
import org.mobilenativefoundation.trails.server.pagination.CursorRequest
import org.mobilenativefoundation.trails.server.services.PostService

internal class FakePostService(
    tables: BackendTables,
    networkGate: NetworkGate,
    latencySimulator: LatencySimulator,
    errorSimulator: ErrorSimulator,
    private val conflictSimulator: ConflictSimulator,
    seedLoader: SeedDataLoader,
    configProvider: BackendConfigProvider,
    private val clock: BackendClock,
) : BaseFakeService(tables, networkGate, latencySimulator, errorSimulator, seedLoader, configProvider), PostService {

    override suspend fun createPost(token: String, create: CreatePostRequest): SkiRunPostRecord = withSimulation {
        val userId = userIdForToken(token)
        val run = tables.runs().get(create.runId) ?: throw ServerError.NotFound("run", create.runId)
        val postId = "post_${create.runId}_${clock.nowMs()}"
        val post = StoredPost(
            id = postId,
            authorId = userId,
            runId = run.id,
            backgroundGradientId = org.mobilenativefoundation.trails.server.model.BackgroundGradientId(
                create.styleId ?: "INDIGO_BLUE_CYAN"
            ),
            emojiId = org.mobilenativefoundation.trails.server.model.EmojiId("Skier"),
            caption = create.caption,
            createdAt = clock.nowMs(),
            version = 1,
            likes = 0,
            comments = 0,
            shares = 0,
            views = 0,
        )
        tables.posts().put(post)
        getPost(postId, token)
    }

    override suspend fun getPost(postId: String): SkiRunPostRecord = withSimulation {
        val post = tables.posts().get(postId) ?: throw ServerError.NotFound("post", postId)
        toRecord(post, null)
    }

    override suspend fun getPost(postId: String, token: String): SkiRunPostRecord = withSimulation {
        val post = tables.posts().get(postId) ?: throw ServerError.NotFound("post", postId)
        val viewerId = userIdForToken(token)
        toRecord(post, viewerId)
    }

    override suspend fun updatePost(token: String, postId: String, update: UpdatePostRequest): SkiRunPostRecord = withSimulation {
        val viewerId = userIdForToken(token)
        val post = tables.posts().get(postId) ?: throw ServerError.NotFound("post", postId)
        if (post.authorId != viewerId) throw ServerError.Forbidden("post")

        val conflictOutcome = conflictSimulator.evaluate(
            resourceType = "post",
            id = postId,
            clientVersion = update.version,
            serverVersion = post.version,
        )
        if (conflictOutcome is ConflictOutcome.Reject) {
            throw conflictOutcome.error
        }

        val updated = post.copy(
            caption = when (conflictOutcome) {
                ConflictOutcome.AutoMerge -> mergeCaption(post.caption, update.caption)
                else -> update.caption ?: post.caption
            },
            backgroundGradientId = when (conflictOutcome) {
                // Preserve server style in auto-merge mode to model non-destructive merge behavior.
                ConflictOutcome.AutoMerge -> post.backgroundGradientId
                else -> update.styleId?.let { org.mobilenativefoundation.trails.server.model.BackgroundGradientId(it) }
                    ?: post.backgroundGradientId
            },
            version = post.version + 1,
        )
        tables.posts().put(updated)
        toRecord(updated, viewerId)
    }

    override suspend fun deletePost(token: String, postId: String) = withSimulation {
        val viewerId = userIdForToken(token)
        val post = tables.posts().get(postId) ?: throw ServerError.NotFound("post", postId)
        if (post.authorId != viewerId) throw ServerError.Forbidden("post")
        tables.posts().remove(postId)
        Unit
    }

    override suspend fun likePost(token: String, postId: String) = withSimulation {
        val viewerId = userIdForToken(token)
        updateEngagement(viewerId, postId, like = true)
    }

    override suspend fun unlikePost(token: String, postId: String) = withSimulation {
        val viewerId = userIdForToken(token)
        updateEngagement(viewerId, postId, like = false)
    }

    override suspend fun bookmarkPost(token: String, postId: String) = withSimulation {
        val viewerId = userIdForToken(token)
        updateBookmark(viewerId, postId, bookmarked = true)
    }

    override suspend fun unbookmarkPost(token: String, postId: String) = withSimulation {
        val viewerId = userIdForToken(token)
        updateBookmark(viewerId, postId, bookmarked = false)
    }

    override suspend fun getComments(postId: String, request: CursorRequest): CursorPage<CommentRecord> = withSimulation {
        val comments = tables.comments().query { it.postId == postId }.sortedByDescending { it.createdAt }
        val page = paginate(comments, request)
        CursorPage(
            items = page.items.map { toCommentRecord(it) },
            nextCursor = page.nextCursor,
            prevCursor = page.prevCursor,
            totalCount = page.totalCount,
        )
    }

    override suspend fun addComment(token: String, postId: String, body: String): CommentRecord = withSimulation {
        val viewerId = userIdForToken(token)
        val post = tables.posts().get(postId) ?: throw ServerError.NotFound("post", postId)
        val commentId = "comment_${postId}_${clock.nowMs()}"
        val comment = StoredComment(
            id = commentId,
            postId = postId,
            authorId = viewerId,
            body = body,
            createdAt = clock.nowMs(),
            likes = 0,
        )
        tables.comments().put(comment)
        tables.posts().put(post.copy(comments = post.comments + 1))
        toCommentRecord(comment)
    }

    override suspend fun deleteComment(token: String, postId: String, commentId: String) = withSimulation {
        val viewerId = userIdForToken(token)
        val comment = tables.comments().get(commentId) ?: throw ServerError.NotFound("comment", commentId)
        if (comment.authorId != viewerId) throw ServerError.Forbidden("comment")
        tables.comments().remove(commentId)
        val post = tables.posts().get(postId)
        if (post != null) {
            tables.posts().put(post.copy(comments = (post.comments - 1).coerceAtLeast(0)))
        }
    }

    override suspend fun getBookmarkedPosts(token: String, request: CursorRequest): CursorPage<SkiRunPostRecord> = withSimulation {
        val viewerId = userIdForToken(token)
        val bookmarks = tables.postEngagements().query { it.userId == viewerId && it.isBookmarked }
        val posts = tables.posts().query { post -> bookmarks.any { it.postId == post.id } }
            .sortedByDescending { it.createdAt }
        val page = paginate(posts, request)
        CursorPage(
            items = page.items.map { toRecord(it, viewerId) },
            nextCursor = page.nextCursor,
            prevCursor = page.prevCursor,
            totalCount = page.totalCount,
        )
    }

    override suspend fun getLikedPosts(token: String, request: CursorRequest): CursorPage<SkiRunPostRecord> = withSimulation {
        val viewerId = userIdForToken(token)
        val likes = tables.postEngagements().query { it.userId == viewerId && it.isLiked }
        val posts = tables.posts().query { post -> likes.any { it.postId == post.id } }
            .sortedByDescending { it.createdAt }
        val page = paginate(posts, request)
        CursorPage(
            items = page.items.map { toRecord(it, viewerId) },
            nextCursor = page.nextCursor,
            prevCursor = page.prevCursor,
            totalCount = page.totalCount,
        )
    }

    private suspend fun userIdForToken(token: String): String {
        val session = tables.sessions().get(token) ?: throw ServerError.Unauthorized()
        return session.userId
    }

    private suspend fun updateEngagement(userId: String, postId: String, like: Boolean) {
        val post = tables.posts().get(postId) ?: throw ServerError.NotFound("post", postId)
        conflictSimulator.evaluate(
            resourceType = "post_engagement",
            id = "$userId:$postId",
            clientVersion = post.version,
            serverVersion = post.version,
        ).let { outcome ->
            if (outcome is ConflictOutcome.Reject) throw outcome.error
        }
        val key = "${userId}:${postId}"
        val table = tables.postEngagements()
        val existing = table.get(key)
        val wasLiked = existing?.isLiked ?: false
        val updated = StoredPostEngagement(
            userId = userId,
            postId = postId,
            isLiked = like,
            isBookmarked = existing?.isBookmarked ?: false,
        )
        table.put(updated)
        if (like && !wasLiked) {
            tables.posts().put(post.copy(likes = post.likes + 1))
        } else if (!like && wasLiked) {
            tables.posts().put(post.copy(likes = (post.likes - 1).coerceAtLeast(0)))
        }
    }

    private suspend fun updateBookmark(userId: String, postId: String, bookmarked: Boolean) {
        val post = tables.posts().get(postId) ?: throw ServerError.NotFound("post", postId)
        conflictSimulator.evaluate(
            resourceType = "post_engagement",
            id = "$userId:$postId",
            clientVersion = post.version,
            serverVersion = post.version,
        ).let { outcome ->
            if (outcome is ConflictOutcome.Reject) throw outcome.error
        }
        val key = "${userId}:${postId}"
        val table = tables.postEngagements()
        val existing = table.get(key)
        val updated = StoredPostEngagement(
            userId = userId,
            postId = postId,
            isLiked = existing?.isLiked ?: false,
            isBookmarked = bookmarked,
        )
        table.put(updated)
    }

    private suspend fun toRecord(post: StoredPost, viewerId: String?): SkiRunPostRecord {
        val author = tables.users().get(post.authorId) ?: throw ServerError.NotFound("user", post.authorId)
        val run = tables.runs().get(post.runId) ?: throw ServerError.NotFound("run", post.runId)
        val resort = tables.resorts().get(run.resortId) ?: throw ServerError.NotFound("resort", run.resortId)
        val weather = tables.weather().get(resort.id)
        val engagement = viewerId?.let { tables.postEngagements().get("${it}:${post.id}") }

        return SkiRunPostRecord(
            id = post.id,
            timestamp = formatTimestamp(post.createdAt),
            author = UserSummary(
                id = author.id,
                username = author.username,
                displayName = author.displayName,
                avatarUrl = author.avatarUrl,
                verified = author.verified,
                isFollowing = viewerId?.let { tables.follows().exists("${it}:${author.id}") } ?: false,
            ),
            run = RunSummary(
                id = run.id,
                resortId = run.resortId,
                name = run.name,
                difficulty = run.difficulty,
                verticalFeet = run.verticalFeet,
                distance = formatMiles(run.lengthMiles),
                liftAccess = run.liftAccess.firstOrNull().orEmpty(),
            ),
            resort = ResortSummary(
                id = resort.id,
                name = resort.name,
                location = if (resort.state != null) "${resort.city}, ${resort.state}" else "${resort.city}, ${resort.country}",
                imageUrl = resort.imageUrl,
            ),
            weather = weather?.let {
                WeatherSummary(
                    conditions = it.conditions,
                    temperatureF = it.temperatureF,
                    snowfall24hIn = it.newSnow24h,
                )
            } ?: WeatherSummary("Unknown", 0, 0),
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

    private fun mergeCaption(current: String?, incoming: String?): String? {
        if (incoming == null) return current
        if (current.isNullOrBlank()) return incoming
        if (current == incoming) return current
        return "$current | $incoming"
    }

    private suspend fun toCommentRecord(comment: StoredComment): CommentRecord {
        val author = tables.users().get(comment.authorId) ?: throw ServerError.NotFound("user", comment.authorId)
        return CommentRecord(
            id = comment.id,
            author = UserSummary(
                id = author.id,
                username = author.username,
                displayName = author.displayName,
                avatarUrl = author.avatarUrl,
                verified = author.verified,
                isFollowing = false,
            ),
            body = comment.body,
            timestamp = formatTimestamp(comment.createdAt),
            likes = comment.likes,
            isLiked = false,
        )
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
                prevCursor = if (start > 0) (start - limit).coerceAtLeast(0).toString() else null,
                totalCount = size,
            )
        }
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
