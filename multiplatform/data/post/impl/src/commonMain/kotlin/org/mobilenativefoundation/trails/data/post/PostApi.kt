package org.mobilenativefoundation.trails.data.post

import org.mobilenativefoundation.trails.model.network.feed.FeedPostRecord

interface PostApi {
    suspend fun fetchFeed(feedKey: FeedKey): List<FeedPostRecord>

    suspend fun updateFeed(feedKey: FeedKey, posts: List<FeedPostRecord>): List<FeedPostRecord>
}
