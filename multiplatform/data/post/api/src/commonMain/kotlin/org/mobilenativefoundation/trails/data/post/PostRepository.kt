package org.mobilenativefoundation.trails.data.post

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.trails.model.domain.feed.FeedPost

interface PostRepository {
    fun streamFeed(feedKey: FeedKey, refresh: Boolean = false): Flow<List<FeedPost>>

    suspend fun refreshFeed(feedKey: FeedKey)

    suspend fun setLiked(feedKey: FeedKey, postId: String, liked: Boolean)

    suspend fun setBookmarked(feedKey: FeedKey, postId: String, bookmarked: Boolean)

    suspend fun setFollowing(feedKey: FeedKey, username: String, following: Boolean)
}
