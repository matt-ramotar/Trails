package org.mobilenativefoundation.trails.screen.home

import com.slack.circuit.runtime.CircuitUiState


sealed interface HomeState : CircuitUiState {
    val showCommentsModal: Boolean
    val showShareModal: Boolean
    val feed: FeedState?

    data object Initial : HomeState {
        override val showCommentsModal: Boolean = false
        override val showShareModal: Boolean = false
        override val feed: FeedState? = null
    }

    data class Data(
        override val showCommentsModal: Boolean,
        override val showShareModal: Boolean,
        override val feed: FeedState,
        val currentPostIndex: Int = 0,
        val isRefreshing: Boolean = false,
        val selectedPostIdForComments: String? = null,
        val send: (HomeIntent) -> Unit
    ) : HomeState
}

