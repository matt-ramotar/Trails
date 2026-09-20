package org.mobilenativefoundation.trails.screen.home.child.posts

import dev.mattramotar.atom.runtime.fsm.Event
import kotlinx.serialization.Serializable
import org.mobilenativefoundation.trails.model.domain.post.Post

@Serializable
sealed interface PostsEvent : Event {
    @Serializable
    data class DataUpdated(val posts: List<Post.Composite>) : PostsEvent

    @Serializable
    data object RequestLoadMoreForward : PostsEvent
}