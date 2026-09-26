package org.mobilenativefoundation.trails.data.post

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreWriteRequest
import org.mobilenativefoundation.store.store5.StoreWriteResponse
import org.mobilenativefoundation.trails.data.devsettings.ConflictStrategy
import org.mobilenativefoundation.trails.data.devsettings.DeveloperSettingsRepository
import org.mobilenativefoundation.trails.foundation.logging.Logger
import org.mobilenativefoundation.trails.model.domain.feed.FeedPost
import org.mobilenativefoundation.trails.server.error.ServerError

@OptIn(ExperimentalStoreApi::class)
@ContributesBinding(AppScope::class)
@Inject
class RealPostRepository(
    private val postStore: PostStore,
    private val logger: Logger,
    private val developerSettingsRepository: DeveloperSettingsRepository,
) : PostRepository {
    override fun streamFeed(feedKey: FeedKey, refresh: Boolean): Flow<List<FeedPost>> {
        val request =
            if (refresh) StoreReadRequest.cached(feedKey, refresh = true)
            else StoreReadRequest.cached(feedKey, refresh = false)

        return postStore
            .stream<Unit>(request)
            .filterIsInstance<StoreReadResponse.Data<List<FeedPost>>>()
            .map { it.value }
            .catch { throwable ->
                logger.error(TAG, "Failed to read feed.", throwable)
                emit(emptyList())
            }
    }

    override suspend fun refreshFeed(feedKey: FeedKey) {
        refreshFromServer(feedKey)
    }

    private suspend fun refreshFromServer(feedKey: FeedKey): Boolean {
        val response =
            postStore
                .stream<Unit>(StoreReadRequest.freshWithFallBackToSourceOfTruth(feedKey))
                .first { it is StoreReadResponse.Data || it is StoreReadResponse.Error }

        when (response) {
            is StoreReadResponse.Error.Exception -> {
                if (response.error is CancellationException) throw response.error
                logger.error(TAG, "Failed to refresh feed.", response.error)
            }

            is StoreReadResponse.Error.Message ->
                logger.error(TAG, "Failed to refresh feed: ${response.message}", RuntimeException(response.message))

            else -> Unit
        }
        return response is StoreReadResponse.Data
    }

    override suspend fun setLiked(feedKey: FeedKey, postId: String, liked: Boolean) {
        updatePost(feedKey) { post ->
            if (post.id != postId || post.isLiked == liked) post
            else post.copy(isLiked = liked, likes = adjustCount(post.likes, liked))
        }
    }

    override suspend fun setBookmarked(feedKey: FeedKey, postId: String, bookmarked: Boolean) {
        updatePost(feedKey) { post ->
            if (post.id != postId) post
            else post.copy(isBookmarked = bookmarked)
        }
    }

    override suspend fun setFollowing(feedKey: FeedKey, username: String, following: Boolean) {
        updatePost(feedKey) { post ->
            if (post.username != username) post
            else post.copy(isFollowing = following)
        }
    }

    private suspend fun updatePost(
        feedKey: FeedKey,
        allowRetry: Boolean = true,
        transform: (FeedPost) -> FeedPost,
    ) {
        val current =
            postStore
                .stream<Unit>(StoreReadRequest.cached(feedKey, refresh = false))
                .first { it is StoreReadResponse.Data || it is StoreReadResponse.Error }

        val posts = when (current) {
            is StoreReadResponse.Data -> current.value
            else -> emptyList()
        }

        if (posts.isEmpty()) return

        val updated =
            posts.map { post ->
                val transformed = transform(post)
                if (transformed == post) post else transformed.copy(version = post.version + 1)
            }

        if (updated == posts) return

        writePosts(feedKey, updated, allowRetry, transform)
    }

    private suspend fun writePosts(
        feedKey: FeedKey,
        updated: List<FeedPost>,
        allowRetry: Boolean,
        transform: (FeedPost) -> FeedPost,
    ) {
        val response =
            postStore.write(
                StoreWriteRequest.of<FeedKey, List<FeedPost>, Unit>(feedKey, updated)
            )

        when (response) {
            // Store5's Unit updater retains the optimistic snapshot, so read back canonical server values.
            is StoreWriteResponse.Success -> refreshFromServer(feedKey)

            is StoreWriteResponse.Error.Exception -> handleWriteException(
                feedKey = feedKey,
                updated = updated,
                transform = transform,
                allowRetry = allowRetry,
                throwable = response.error,
            )

            is StoreWriteResponse.Error.Message ->
                logger.error(TAG, "Failed to update feed: ${response.message}", RuntimeException(response.message))

            else -> Unit
        }
    }

    private suspend fun handleWriteException(
        feedKey: FeedKey,
        updated: List<FeedPost>,
        transform: (FeedPost) -> FeedPost,
        allowRetry: Boolean,
        throwable: Throwable,
    ) {
        when (throwable) {
            is CancellationException -> throw throwable
            is PostConflictException -> resolveConflict(feedKey, updated, allowRetry, transform)
            ServerError.NetworkUnavailable -> logger.info(TAG, "Backend is offline, update queued locally.")
            else -> logger.error(TAG, "Failed to update feed.", throwable)
        }
    }

    private suspend fun resolveConflict(
        feedKey: FeedKey,
        updated: List<FeedPost>,
        allowRetry: Boolean,
        transform: (FeedPost) -> FeedPost,
    ) {
        when (developerSettingsRepository.current.conflictStrategy) {
            ConflictStrategy.SERVER_WINS -> refreshFeed(feedKey)
            ConflictStrategy.CLIENT_WINS -> {
                if (!allowRetry) return
                writePosts(feedKey, updated, allowRetry = false, transform = transform)
            }

            ConflictStrategy.MERGE -> {
                if (!allowRetry) return
                if (refreshFromServer(feedKey)) {
                    updatePost(feedKey, allowRetry = false, transform = transform)
                }
            }
        }
    }

    private fun adjustCount(current: Int, liked: Boolean): Int =
        when {
            liked && current == 0 -> 1
            liked -> current + 1
            current == 0 -> 0
            else -> current - 1
        }

    private companion object {
        private const val TAG = "RealPostRepository"
    }
}
