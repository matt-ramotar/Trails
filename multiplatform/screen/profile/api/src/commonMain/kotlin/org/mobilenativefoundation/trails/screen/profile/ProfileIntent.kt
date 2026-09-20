package org.mobilenativefoundation.trails.screen.profile

import com.slack.circuit.runtime.CircuitUiEvent

sealed interface ProfileIntent : CircuitUiEvent {
    data object EditProfile : ProfileIntent
    data object ShareProfile : ProfileIntent
    data object OpenMenu : ProfileIntent
    data object OpenAvatarOptions : ProfileIntent

    data class SelectTab(val tab: ProfileTab) : ProfileIntent
    data class SelectViewMode(val mode: ClipViewMode) : ProfileIntent
    data class SelectFilter(val filter: ClipFilter) : ProfileIntent
    data class SelectClip(val clipId: String) : ProfileIntent
    data class SelectCount(val count: ProfileCountType) : ProfileIntent
}

enum class ProfileCountType {
    POSTS,
    FOLLOWERS,
    FOLLOWING
}
