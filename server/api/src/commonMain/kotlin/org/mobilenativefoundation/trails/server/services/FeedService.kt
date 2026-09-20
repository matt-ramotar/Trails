package org.mobilenativefoundation.trails.server.services

import org.mobilenativefoundation.trails.server.model.FeedItemRecord
import org.mobilenativefoundation.trails.server.pagination.CursorPage
import org.mobilenativefoundation.trails.server.pagination.CursorRequest

interface FeedService {
    suspend fun getHomeFeed(
        token: String,
        request: CursorRequest,
    ): CursorPage<FeedItemRecord>

    suspend fun getUserFeed(
        userId: String,
        request: CursorRequest,
    ): CursorPage<FeedItemRecord>

    suspend fun getFollowingFeed(
        token: String,
        request: CursorRequest,
    ): CursorPage<FeedItemRecord>

    suspend fun getResortFeed(
        resortId: String,
        request: CursorRequest,
    ): CursorPage<FeedItemRecord>

    suspend fun getTrendingFeed(
        request: CursorRequest,
    ): CursorPage<FeedItemRecord>
}
