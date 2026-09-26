package org.mobilenativefoundation.trails.screen.home.child.posts

import dev.mattramotar.atom.runtime.fsm.Intent
import kotlinx.serialization.Serializable

@Serializable
sealed interface PostsIntent : Intent {
    @Serializable
    data class ScrollForward(val lastVisibleIndex: Int) : PostsIntent

    @Serializable
    data object PullToRefresh : PostsIntent

    @Serializable
    data class Retry(val direction: LoadDirection) : PostsIntent

    @Serializable
    data object LoadMore : PostsIntent
}