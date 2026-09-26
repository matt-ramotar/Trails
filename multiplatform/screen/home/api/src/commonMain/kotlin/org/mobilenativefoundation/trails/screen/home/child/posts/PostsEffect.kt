package org.mobilenativefoundation.trails.screen.home.child.posts

import dev.mattramotar.atom.runtime.fsm.SideEffect
import kotlinx.serialization.Serializable

@Serializable
sealed interface PostsEffect : SideEffect {
    @Serializable
    data object LoadMoreForward : PostsEffect
}