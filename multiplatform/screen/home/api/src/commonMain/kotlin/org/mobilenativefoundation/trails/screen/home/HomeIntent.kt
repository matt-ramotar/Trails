package org.mobilenativefoundation.trails.screen.home

import com.slack.circuit.runtime.CircuitUiEvent

sealed interface HomeIntent : CircuitUiEvent {
    data class ToggleLike(val postId: String) : HomeIntent

    data class ToggleBookmark(val postId: String) : HomeIntent

    data class ToggleFollow(val username: String) : HomeIntent

    data class ShowComments(val postId: String) : HomeIntent

    data object DismissComments : HomeIntent

    data class ShowShare(val postId: String) : HomeIntent

    data object DismissShare : HomeIntent

    data class PostScrolled(val postIndex: Int) : HomeIntent

    data object Refresh : HomeIntent

    data object OpenNotifications : HomeIntent
}