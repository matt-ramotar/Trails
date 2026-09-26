package org.mobilenativefoundation.trails.screen.home.child.posts

import com.slack.circuit.runtime.CircuitUiState
import kotlinx.serialization.Serializable
import org.mobilenativefoundation.trails.model.domain.post.Post

@Serializable
sealed interface PostsState : CircuitUiState {

    val posts: List<Post.Ref>?
    val loading: LoadingState

    data class Data(
        override val posts: List<Post.Ref>?,
        override val loading: LoadingState
    ) : PostsState
}


