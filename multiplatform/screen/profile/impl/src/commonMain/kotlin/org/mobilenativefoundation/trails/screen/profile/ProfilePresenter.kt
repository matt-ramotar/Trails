package org.mobilenativefoundation.trails.screen.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.foundation.scope.ActiveScope

@ContributesBinding(ActiveScope::class)
@Inject
class ProfilePresenter : Presenter<ProfileState> {
    @Composable
    override fun present(): ProfileState {
        var selectedTab by remember { mutableStateOf(ProfileTab.CLIPS) }
        var viewMode by remember { mutableStateOf(ClipViewMode.GRID) }
        var selectedFilter by remember { mutableStateOf(ClipFilter.ALL) }

        val header = remember { createProfileHeader() }
        val counts = remember { createProfileCounts() }
        val allClips = remember { createMockClips() }

        val visibleClips = remember(selectedTab, selectedFilter, allClips) {
            if (selectedTab != ProfileTab.CLIPS) {
                emptyList()
            } else if (selectedFilter == ClipFilter.ALL) {
                allClips
            } else {
                allClips.filter { it.filter == selectedFilter }
            }
        }

        val send: (ProfileIntent) -> Unit = { intent ->
            when (intent) {
                ProfileIntent.EditProfile -> {
                    // TODO: Navigate to edit profile screen.
                }
                ProfileIntent.ShareProfile -> {
                    // TODO: Trigger share flow.
                }
                ProfileIntent.OpenMenu -> {
                    // TODO: Open profile menu.
                }
                ProfileIntent.OpenAvatarOptions -> {
                    // TODO: Open avatar options.
                }
                is ProfileIntent.SelectTab -> {
                    selectedTab = intent.tab
                }
                is ProfileIntent.SelectViewMode -> {
                    viewMode = intent.mode
                }
                is ProfileIntent.SelectFilter -> {
                    selectedFilter = intent.filter
                }
                is ProfileIntent.SelectClip -> {
                    // TODO: Open clip details.
                }
                is ProfileIntent.SelectCount -> {
                    // TODO: Navigate to selected count list.
                }
            }
        }

        return ProfileState(
            header = header,
            counts = counts,
            selectedTab = selectedTab,
            viewMode = viewMode,
            selectedFilter = selectedFilter,
            clips = visibleClips,
            send = send
        )
    }

    private fun createProfileHeader() = ProfileHeader(
        displayName = "Matt Ramotar",
        handle = "@matt",
        bio = "Staff Engineer • Building Store, Atom, Meeseeks 🧊 Chasing powder days ❄️ + clean architecture.",
        location = "West Village, NYC • Vail, CO",
        isVerified = true,
        statusLabel = "Synced",
        avatarInitials = "MR"
    )

    private fun createProfileCounts() = ProfileCounts(
        posts = 142,
        followers = 12400,
        following = 847
    )

    private fun createMockClips() = listOf(
        ClipItem(
            id = "clip_1",
            likes = 52000,
            location = null,
            status = null,
            badges = listOf(ClipBadge.PLAY, ClipBadge.AUDIO),
            background = ClipBackground.OCEAN,
            filter = ClipFilter.GREEN,
            thumbnail = ClipThumbnail.CLIP_01
        ),
        ClipItem(
            id = "clip_2",
            likes = 28000,
            location = null,
            status = null,
            badges = listOf(ClipBadge.CAMERA, ClipBadge.PLAY),
            background = ClipBackground.MINT,
            filter = ClipFilter.BLUE,
            thumbnail = ClipThumbnail.CLIP_02
        ),
        ClipItem(
            id = "clip_3",
            likes = 9000,
            location = null,
            status = null,
            badges = listOf(ClipBadge.PLAY, ClipBadge.AUDIO),
            background = ClipBackground.ROSE,
            filter = ClipFilter.BLACK,
            thumbnail = ClipThumbnail.CLIP_03
        ),
        ClipItem(
            id = "clip_4",
            likes = 14000,
            location = null,
            status = null,
            badges = listOf(ClipBadge.CAMERA, ClipBadge.AUDIO),
            background = ClipBackground.DEEP,
            filter = ClipFilter.GREEN,
            thumbnail = ClipThumbnail.CLIP_04
        ),
        ClipItem(
            id = "clip_5",
            likes = 8100,
            location = null,
            status = null,
            badges = listOf(ClipBadge.PLAY, ClipBadge.CAMERA),
            background = ClipBackground.SUNSET,
            filter = ClipFilter.BLUE,
            thumbnail = ClipThumbnail.CLIP_05
        ),
        ClipItem(
            id = "clip_6",
            likes = 4000,
            location = null,
            status = null,
            badges = listOf(ClipBadge.CAMERA, ClipBadge.AUDIO),
            background = ClipBackground.NIGHT,
            filter = ClipFilter.BLACK,
            thumbnail = ClipThumbnail.CLIP_06
        ),
        ClipItem(
            id = "clip_7",
            likes = null,
            location = "Snowbird",
            status = ClipStatus.Uploading(progress = 64),
            badges = listOf(ClipBadge.PLAY, ClipBadge.MAP),
            background = ClipBackground.FROST,
            filter = ClipFilter.UPLOADS,
            thumbnail = ClipThumbnail.CLIP_07
        ),
        ClipItem(
            id = "clip_8",
            likes = null,
            location = "Aspen Highlands",
            status = ClipStatus.Queued,
            badges = listOf(ClipBadge.CAMERA, ClipBadge.MAP),
            background = ClipBackground.OCEAN,
            filter = ClipFilter.DRAFTS,
            thumbnail = ClipThumbnail.CLIP_08
        ),
        ClipItem(
            id = "clip_9",
            likes = null,
            location = "Alta",
            status = ClipStatus.Queued,
            badges = listOf(ClipBadge.PLAY, ClipBadge.MAP),
            background = ClipBackground.DEEP,
            filter = ClipFilter.UPLOADS,
            thumbnail = ClipThumbnail.CLIP_09
        ),
        ClipItem(
            id = "clip_10",
            likes = 2100,
            location = null,
            status = null,
            badges = listOf(ClipBadge.CAMERA, ClipBadge.AUDIO),
            background = ClipBackground.MINT,
            filter = ClipFilter.GREEN,
            thumbnail = ClipThumbnail.CLIP_10
        ),
        ClipItem(
            id = "clip_11",
            likes = 1700,
            location = null,
            status = null,
            badges = listOf(ClipBadge.PLAY, ClipBadge.AUDIO),
            background = ClipBackground.ROSE,
            filter = ClipFilter.BLUE,
            thumbnail = ClipThumbnail.CLIP_11
        ),
        ClipItem(
            id = "clip_12",
            likes = null,
            location = "Palisades",
            status = ClipStatus.Failed,
            badges = listOf(ClipBadge.PLAY, ClipBadge.MAP),
            background = ClipBackground.NIGHT,
            filter = ClipFilter.DOUBLE,
            thumbnail = ClipThumbnail.CLIP_12
        )
    )
}
