package org.mobilenativefoundation.trails.screen.profile

import com.slack.circuit.runtime.CircuitUiState

data class ProfileState(
    val header: ProfileHeader,
    val counts: ProfileCounts,
    val selectedTab: ProfileTab,
    val viewMode: ClipViewMode,
    val selectedFilter: ClipFilter,
    val clips: List<ClipItem>,
    val send: (ProfileIntent) -> Unit
) : CircuitUiState

data class ProfileHeader(
    val displayName: String,
    val handle: String,
    val bio: String,
    val location: String,
    val isVerified: Boolean,
    val statusLabel: String?,
    val avatarInitials: String
)

data class ProfileCounts(
    val posts: Int,
    val followers: Int,
    val following: Int
)

enum class ProfileTab {
    CLIPS,
    STATS,
    SAVED
}

enum class ClipViewMode {
    GRID,
    LIST
}

enum class ClipFilter {
    ALL,
    UPLOADS,
    DRAFTS,
    GREEN,
    BLUE,
    BLACK,
    DOUBLE
}

data class ClipItem(
    val id: String,
    val likes: Int?,
    val location: String?,
    val status: ClipStatus?,
    val badges: List<ClipBadge>,
    val background: ClipBackground,
    val filter: ClipFilter,
    val thumbnail: ClipThumbnail?
)

enum class ClipBadge {
    PLAY,
    CAMERA,
    AUDIO,
    MAP
}

sealed interface ClipStatus {
    data class Uploading(val progress: Int) : ClipStatus
    data object Queued : ClipStatus
    data object Failed : ClipStatus
}

enum class ClipBackground {
    OCEAN,
    MINT,
    ROSE,
    DEEP,
    SUNSET,
    FROST,
    NIGHT
}

enum class ClipThumbnail {
    CLIP_01,
    CLIP_02,
    CLIP_03,
    CLIP_04,
    CLIP_05,
    CLIP_06,
    CLIP_07,
    CLIP_08,
    CLIP_09,
    CLIP_10,
    CLIP_11,
    CLIP_12
}
