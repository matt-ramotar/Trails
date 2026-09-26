package org.mobilenativefoundation.trails.screen.home.child.post

import com.slack.circuit.runtime.CircuitUiEvent
import dev.mattramotar.atom.runtime.fsm.Intent

sealed interface PostIntent : CircuitUiEvent, Intent {
    data class ToggleLike(val like: Boolean) : PostIntent
    data class ToggleRepost(val repost: Boolean) : PostIntent
    data class ToggleBookmark(val bookmark: Boolean) : PostIntent
    data object Share : PostIntent
    data object Report : PostIntent
    data object Refresh : PostIntent
}
