package org.mobilenativefoundation.trails.screen.home

import kotlinx.serialization.Serializable
import org.mobilenativefoundation.trails.model.domain.feed.FeedPost
import org.mobilenativefoundation.trails.model.domain.post.Post.Ref

@Serializable
sealed interface FeedItem {
    data class Post(val ref: Ref) : FeedItem
    data class SkiRunPost(val post: FeedPost) : FeedItem
}
