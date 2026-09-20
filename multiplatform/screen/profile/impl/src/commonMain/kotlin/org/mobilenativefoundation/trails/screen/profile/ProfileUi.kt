package org.mobilenativefoundation.trails.screen.profile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.jetbrains.compose.resources.painterResource
import org.mobilenativefoundation.trails.foundation.designsystem.ClipPlaceholders
import org.mobilenativefoundation.trails.foundation.designsystem.Emojis
import org.mobilenativefoundation.trails.foundation.designsystem.component.glassEffect
import org.mobilenativefoundation.trails.foundation.designsystem.component.glassCard
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icon as TrailsIcon
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.foundation.designsystem.util.CountFormatUtils.formatCount
import org.mobilenativefoundation.trails.foundation.scope.ActiveScope
import trails.multiplatform.screen.profile.impl.generated.resources.Res

@ContributesBinding(ActiveScope::class)
@Inject
class ProfileUi : Ui<ProfileState> {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content(state: ProfileState, modifier: Modifier) {
        val colorScheme = TrailsTheme.colorScheme
        val colors = TrailsTheme.colors
        val spacing = TrailsTheme.spacing

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
            ProfileTopBar(
                handle = state.header.handle,
                onShare = { state.send(ProfileIntent.ShareProfile) },
                onMenu = { state.send(ProfileIntent.OpenMenu) },
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = spacing.xxl)
            ) {
                item {
                    ProfileHero(
                        header = state.header,
                        counts = state.counts,
                        onAvatarClick = { state.send(ProfileIntent.OpenAvatarOptions) },
                        onCountClick = { state.send(ProfileIntent.SelectCount(it)) },
                        onEditClick = { state.send(ProfileIntent.EditProfile) },
                        onShareClick = { state.send(ProfileIntent.ShareProfile) }
                    )
                }

                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassEffect(
                                shape = RoundedCornerShape(0.dp),
                                backgroundColor = colors.glassBackground.copy(alpha = 0.9f),
                                borderColor = Color.Transparent
                            )
                            .padding(horizontal = spacing.lg, vertical = spacing.sm)
                    ) {
                        ProfileTabs(
                            selectedTab = state.selectedTab,
                            onTabSelected = { state.send(ProfileIntent.SelectTab(it)) }
                        )
                    }
                }

                if (state.selectedTab == ProfileTab.CLIPS) {
                    item {
                        ClipsHeader(
                            viewMode = state.viewMode,
                            onViewModeSelected = { state.send(ProfileIntent.SelectViewMode(it)) }
                        )
                    }

                    item {
                        ClipFiltersRow(
                            selectedFilter = state.selectedFilter,
                            onFilterSelected = { state.send(ProfileIntent.SelectFilter(it)) }
                        )
                    }

                    item {
                        ClipsGrid(
                            clips = state.clips,
                            viewMode = state.viewMode,
                            onClipSelected = { state.send(ProfileIntent.SelectClip(it)) },
                            modifier = Modifier.padding(horizontal = spacing.lg)
                        )
                    }
                } else {
                    item {
                        PlaceholderTabContent(selectedTab = state.selectedTab)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTopBar(
    handle: String,
    onShare: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TrailsTheme.colors
    val colorScheme = TrailsTheme.colorScheme
    val typography = TrailsTheme.typography
    val spacing = TrailsTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassEffect(
                shape = RoundedCornerShape(0.dp),
                backgroundColor = colors.glassBackground,
                borderColor = colors.glassBorder
            )
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = handle,
            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.onSurface
        )

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            GlassIconButton(
                icon = Icons.Outlined.Share,
                contentDescription = "Share profile",
                onClick = onShare
            )
            GlassIconButton(
                icon = Icons.Outlined.MoreHorizontal,
                contentDescription = "Profile menu",
                onClick = onMenu
            )
        }
    }
}

@Composable
private fun ProfileHero(
    header: ProfileHeader,
    counts: ProfileCounts,
    onAvatarClick: () -> Unit,
    onCountClick: (ProfileCountType) -> Unit,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TrailsTheme.spacing

    Box(modifier = modifier.fillMaxWidth()) {
        HeroBackground(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        )

        ProfileHeaderCard(
            header = header,
            counts = counts,
            onAvatarClick = onAvatarClick,
            onCountClick = onCountClick,
            onEditClick = onEditClick,
            onShareClick = onShareClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 146.dp, start = spacing.lg, end = spacing.lg, bottom = spacing.lg)
        )
    }
}

@Composable
private fun HeroBackground(modifier: Modifier = Modifier) {
    val colors = TrailsTheme.colors
    val colorScheme = TrailsTheme.colorScheme

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.gradientStart.copy(alpha = 0.45f),
                            colors.gradientEnd.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.gradientMid.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        radius = 420f
                    )
                )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.freshTrails.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(420f, 60f),
                        radius = 320f
                    )
                )
        )

        Image(
            painter = Emojis.SnowCappedMountain,
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .alpha(0.35f),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            colorScheme.background.copy(alpha = 0.95f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun ProfileHeaderCard(
    header: ProfileHeader,
    counts: ProfileCounts,
    onAvatarClick: () -> Unit,
    onCountClick: (ProfileCountType) -> Unit,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TrailsTheme.spacing
    val radii = TrailsTheme.radii
    val typography = TrailsTheme.typography
    val colorScheme = TrailsTheme.colorScheme
    val colors = TrailsTheme.colors

    Box(
        modifier = modifier
            .shadow(40.dp, RoundedCornerShape(radii.xl))
            .glassEffect(
                shape = RoundedCornerShape(radii.xl),
                backgroundColor = colors.glassBackground,
                borderColor = colors.glassBorder
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.gradientStart.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        radius = 320f
                    )
                )
        )

        Column(
            modifier = Modifier.padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.Top
            ) {
                ProfileAvatar(
                    initials = header.avatarInitials,
                    onClick = onAvatarClick
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = header.displayName,
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        if (header.isVerified) {
                            VerifiedBadge()
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = header.handle,
                            style = typography.labelMedium,
                            color = colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        header.statusLabel?.let { label ->
                            StatusPill(label = label)
                        }
                    }

                    Text(
                        text = header.bio,
                        style = typography.bodySmall,
                        color = colorScheme.onSurface.copy(alpha = 0.78f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = Icons.Outlined.Location.painter,
                            contentDescription = Icons.Outlined.Location.contentDescription,
                            tint = colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = header.location,
                            style = typography.labelSmall,
                            color = colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            ProfileCountsRow(counts = counts, onCountClick = onCountClick)

            ProfileActionRow(
                onEditClick = onEditClick,
                onShareClick = onShareClick
            )
        }
    }
}

@Composable
private fun ProfileAvatar(
    initials: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TrailsTheme.colors
    val colorScheme = TrailsTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .size(78.dp)
            .semantics { contentDescription = "Avatar $initials" }
            .shadow(18.dp, shape)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.primary.copy(alpha = 0.85f),
                        colors.freshTrails.copy(alpha = 0.62f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.58f),
                            Color.White.copy(alpha = 0.1f)
                        ),
                        center = Offset.Zero,
                        radius = 70f
                    )
                )
        ) {
            Image(ClipPlaceholders.Avatar, contentDescription = "Avatar", modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-6).dp, y = (-6).dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(colorScheme.primary)
                .border(2.dp, colorScheme.background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = Icons.Outlined.Camera.painter,
                contentDescription = Icons.Outlined.Camera.contentDescription,
                tint = colorScheme.onPrimary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun VerifiedBadge(modifier: Modifier = Modifier) {
    val colors = TrailsTheme.colors
    val colorScheme = TrailsTheme.colorScheme

    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(colors.badgeRare),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = Icons.Solid.Check.painter,
            contentDescription = Icons.Solid.Check.contentDescription,
            tint = colorScheme.onSurface,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun StatusPill(label: String, modifier: Modifier = Modifier) {
    val colorScheme = TrailsTheme.colorScheme
    val typography = TrailsTheme.typography

    GlassPill(
        modifier = modifier,
        backgroundColor = ChipBackground,
        borderColor = ChipBorder
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(TrailsTheme.colors.freshTrails, CircleShape)
        )
        Text(
            text = label,
            style = typography.labelSmall,
            color = colorScheme.onSurface.copy(alpha = 0.8f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProfileCountsRow(
    counts: ProfileCounts,
    onCountClick: (ProfileCountType) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TrailsTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        ProfileCountButton(
            value = counts.posts,
            label = "Posts",
            onClick = { onCountClick(ProfileCountType.POSTS) },
            modifier = Modifier.weight(1f)
        )
        ProfileCountButton(
            value = counts.followers,
            label = "Followers",
            onClick = { onCountClick(ProfileCountType.FOLLOWERS) },
            modifier = Modifier.weight(1f)
        )
        ProfileCountButton(
            value = counts.following,
            label = "Following",
            onClick = { onCountClick(ProfileCountType.FOLLOWING) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProfileCountButton(
    value: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TrailsTheme.spacing
    val radii = TrailsTheme.radii
    val colorScheme = TrailsTheme.colorScheme
    val typography = TrailsTheme.typography

    Column(
        modifier = modifier
            .height(59.dp)
            .glassCard(shape = RoundedCornerShape(radii.lg))
            .clickable(onClick = onClick)
            .padding(vertical = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = value.formatCount(),
            style = typography.titleSmall,
            color = colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label.uppercase(),
            style = typography.labelSmall,
            color = colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ProfileActionRow(
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TrailsTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        ProfileActionButton(
            label = "Edit",
            icon = Icons.Outlined.PencilEdit,
            isPrimary = true,
            onClick = onEditClick,
            modifier = Modifier.weight(1f)
        )
        ProfileActionButton(
            label = "Share",
            icon = Icons.Outlined.Share,
            isPrimary = false,
            onClick = onShareClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProfileActionButton(
    label: String,
    icon: TrailsIcon,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val radii = TrailsTheme.radii
    val colorScheme = TrailsTheme.colorScheme
    val typography = TrailsTheme.typography
    val shape = RoundedCornerShape(radii.lg)
    val buttonModifier = if (isPrimary) {
        modifier
            .height(40.dp)
            .shadow(20.dp, shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.primary,
                        colorScheme.primary.copy(alpha = 0.76f)
                    )
                ),
                shape
            )
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
    } else {
        modifier
            .height(40.dp)
            .glassEffect(
                shape = shape,
                backgroundColor = Color.White.copy(alpha = 0.06f),
                borderColor = Color.White.copy(alpha = 0.1f)
            )
    }
    val textColor = if (isPrimary) {
        colorScheme.onPrimary
    } else {
        colorScheme.onSurface
    }

    Row(
        modifier = buttonModifier.clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon.painter,
            contentDescription = icon.contentDescription,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = typography.labelLarge,
            color = textColor
        )
    }
}

@Composable
private fun ProfileTabs(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TrailsTheme.spacing
    val radii = TrailsTheme.radii
    val colorScheme = TrailsTheme.colorScheme
    val typography = TrailsTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        ProfileTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            val background = if (isSelected) {
                Color.White.copy(alpha = 0.14f)
            } else {
                Color.Transparent
            }
            val border = if (isSelected) {
                Color.White.copy(alpha = 0.14f)
            } else {
                Color.Transparent
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .shadow(if (isSelected) 10.dp else 0.dp, RoundedCornerShape(radii.md))
                    .glassEffect(
                        shape = RoundedCornerShape(radii.md),
                        backgroundColor = background,
                        borderColor = border
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = spacing.sm, horizontal = spacing.sm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = tabIcon(tab).painter,
                    contentDescription = tabIcon(tab).contentDescription,
                    tint = if (isSelected) {
                        colorScheme.onSurface
                    } else {
                        colorScheme.onSurface.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = tabLabel(tab),
                    style = typography.labelMedium,
                    color = if (isSelected) {
                        colorScheme.onSurface
                    } else {
                        colorScheme.onSurface.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}

@Composable
private fun ClipsHeader(
    viewMode: ClipViewMode,
    onViewModeSelected: (ClipViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TrailsTheme.spacing
    val colorScheme = TrailsTheme.colorScheme
    val typography = TrailsTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Clips",
                style = typography.titleMedium,
                color = colorScheme.onSurface
            )
            Text(
                text = "Your runs, drafts, and uploads — built to work offline.",
                style = typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        ViewToggle(
            viewMode = viewMode,
            onViewModeSelected = onViewModeSelected
        )
    }
}

@Composable
private fun ViewToggle(
    viewMode: ClipViewMode,
    onViewModeSelected: (ClipViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TrailsTheme.spacing
    val radii = TrailsTheme.radii

    Row(
        modifier = modifier
            .glassEffect(
                shape = RoundedCornerShape(radii.md),
                backgroundColor = Color.White.copy(alpha = 0.06f),
                borderColor = Color.White.copy(alpha = 0.1f)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        ToggleIconButton(
            icon = Icons.Outlined.Album,
            selected = viewMode == ClipViewMode.GRID,
            onClick = { onViewModeSelected(ClipViewMode.GRID) }
        )
        ToggleIconButton(
            icon = Icons.Outlined.MenuTwoLine,
            selected = viewMode == ClipViewMode.LIST,
            onClick = { onViewModeSelected(ClipViewMode.LIST) }
        )
    }
}

@Composable
private fun ToggleIconButton(
    icon: TrailsIcon,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val radii = TrailsTheme.radii
    val colorScheme = TrailsTheme.colorScheme

    val background = if (selected) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Transparent
    }
    val border = if (selected) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.White.copy(alpha = 0.1f)
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .glassEffect(
                shape = RoundedCornerShape(radii.md),
                backgroundColor = background,
                borderColor = border
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon.painter,
            contentDescription = icon.contentDescription,
            tint = if (selected) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ClipFiltersRow(
    selectedFilter: ClipFilter,
    onFilterSelected: (ClipFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TrailsTheme.spacing

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.sm)
    ) {
        items(ClipFilter.entries.toList()) { filter ->
            FilterChip(
                filter = filter,
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

@Composable
private fun FilterChip(
    filter: ClipFilter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val radii = TrailsTheme.radii
    val colorScheme = TrailsTheme.colorScheme
    val typography = TrailsTheme.typography
    val indicatorColor = filterIndicatorColor(filter)

    val background = if (selected) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.White.copy(alpha = 0.06f)
    }
    val border = if (selected) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.White.copy(alpha = 0.1f)
    }

    Row(
        modifier = modifier
            .glassEffect(
                shape = RoundedCornerShape(radii.pill),
                backgroundColor = background,
                borderColor = border
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        indicatorColor?.let { color ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
        }
        Text(
            text = filterLabel(filter),
            style = typography.labelMedium,
            color = colorScheme.onSurface.copy(alpha = if (selected) 0.9f else 0.75f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ClipsGrid(
    clips: List<ClipItem>,
    viewMode: ClipViewMode,
    onClipSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TrailsTheme.spacing
    val typography = TrailsTheme.typography
    val colorScheme = TrailsTheme.colorScheme

    if (clips.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = spacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No clips yet.",
                style = typography.bodyMedium,
                color = colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = if (viewMode == ClipViewMode.GRID) 3 else 1
        val horizontalSpacing = spacing.sm
        val verticalSpacing = spacing.sm
        val totalSpacing = horizontalSpacing * (columns - 1)
        val itemWidth = (maxWidth - totalSpacing) / columns
        val itemHeight = if (viewMode == ClipViewMode.GRID) itemWidth else 120.dp
        val rows = (clips.size + columns - 1) / columns
        val gridHeight = if (rows == 0) {
            0.dp
        } else {
            itemHeight * rows + verticalSpacing * (rows - 1)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.height(gridHeight),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            userScrollEnabled = false
        ) {
            gridItems(clips, key = { it.id }) { clip ->
                ClipCard(
                    clip = clip,
                    height = itemHeight,
                    viewMode = viewMode,
                    onClick = { onClipSelected(clip.id) },
                    modifier = Modifier.width(itemWidth)
                )
            }
        }
    }
}

@Composable
private fun ClipCard(
    clip: ClipItem,
    height: Dp,
    viewMode: ClipViewMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val radii = TrailsTheme.radii
    val spacing = TrailsTheme.spacing
    val backgroundBrush = clipBackgroundBrush(clip.background)
    val isGrid = viewMode == ClipViewMode.GRID
    val shape = RoundedCornerShape(radii.md)

    Box(
        modifier = modifier
            .height(height)
            .shadow(14.dp, shape)
            .clip(shape)
            .background(backgroundBrush)
            .clickable(onClick = onClick)
    ) {
        clip.thumbnail?.let { thumbnail ->
            Image(
                painter = clipThumbnailPainter(thumbnail, viewMode),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            clip.badges.forEach { badge ->
                GlassIconChip(icon = badgeIcon(badge))
            }
        }

        if (isGrid) {
            gridChipFor(clip)?.let { chip ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(spacing.xs)
                ) {
                    when (chip) {
                        is ClipGridChip.Like -> LikeChip(likes = chip.likes)
                        is ClipGridChip.Location -> TextChip(text = chip.location)
                        is ClipGridChip.Status -> StatusChip(status = chip.status)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(spacing.xs),
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalAlignment = Alignment.Bottom
                ) {
                    clip.likes?.let { likes ->
                        LikeChip(likes = likes)
                    }
                    clip.location?.let { location ->
                        TextChip(text = location)
                    }
                }

                clip.status?.let { status ->
                    Spacer(modifier = Modifier.width(spacing.xs))
                    StatusChip(status = status)
                }
            }
        }
    }
}

@Composable
private fun LikeChip(likes: Int, modifier: Modifier = Modifier) {
    val colorScheme = TrailsTheme.colorScheme
    val typography = TrailsTheme.typography

    GlassPill(
        modifier = modifier,
        backgroundColor = ChipBackground,
        borderColor = ChipBorder
    ) {
        Icon(
            painter = Icons.Solid.Favorite.painter,
            contentDescription = Icons.Solid.Favorite.contentDescription,
            tint = colorScheme.error,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = likes.formatCount(),
            style = typography.labelSmall,
            color = colorScheme.onSurface.copy(alpha = 0.85f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TextChip(text: String, modifier: Modifier = Modifier) {
    val colorScheme = TrailsTheme.colorScheme
    val typography = TrailsTheme.typography

    GlassPill(
        modifier = modifier,
        backgroundColor = ChipBackground,
        borderColor = ChipBorder
    ) {
        Text(
            text = text,
            style = typography.labelSmall,
            color = colorScheme.onSurface.copy(alpha = 0.85f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusChip(status: ClipStatus, modifier: Modifier = Modifier) {
    val colorScheme = TrailsTheme.colorScheme
    val typography = TrailsTheme.typography
    val (label, tone) = when (status) {
        is ClipStatus.Uploading -> {
            "Uploading ${status.progress}%" to colorScheme.tertiary
        }
        ClipStatus.Queued -> {
            "Queued" to colorScheme.tertiary
        }
        ClipStatus.Failed -> {
            "Failed" to colorScheme.error
        }
    }

    GlassPill(
        modifier = modifier,
        backgroundColor = ChipBackground,
        borderColor = ChipBorder
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(tone, CircleShape)
                .border(3.dp, tone.copy(alpha = 0.2f), CircleShape)
        )
        Text(
            text = label,
            style = typography.labelSmall,
            color = colorScheme.onSurface.copy(alpha = 0.85f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GlassIconChip(icon: TrailsIcon, modifier: Modifier = Modifier) {
    val colorScheme = TrailsTheme.colorScheme

    Box(
        modifier = modifier
            .size(28.dp)
            .glassEffect(
                shape = RoundedCornerShape(999.dp),
                backgroundColor = ChipBackground,
                borderColor = ChipBorder
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon.painter,
            contentDescription = icon.contentDescription,
            tint = colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun GlassIconButton(
    icon: TrailsIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = TrailsTheme.colorScheme

    Box(
        modifier = modifier
            .size(38.dp)
            .shadow(10.dp, RoundedCornerShape(14.dp))
            .glassEffect(
                shape = RoundedCornerShape(14.dp),
                backgroundColor = Color.White.copy(alpha = 0.06f),
                borderColor = Color.White.copy(alpha = 0.1f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon.painter,
            contentDescription = contentDescription,
            tint = colorScheme.onSurface.copy(alpha = 0.9f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun GlassPill(
    backgroundColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .glassEffect(
                shape = RoundedCornerShape(999.dp),
                backgroundColor = backgroundColor,
                borderColor = borderColor
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

private val ChipBackground = Color(0x52000000)
private val ChipBorder = Color(0x1FFFFFFF)

@Composable
private fun PlaceholderTabContent(selectedTab: ProfileTab, modifier: Modifier = Modifier) {
    val spacing = TrailsTheme.spacing
    val typography = TrailsTheme.typography
    val colorScheme = TrailsTheme.colorScheme
    val label = when (selectedTab) {
        ProfileTab.CLIPS -> "Clips"
        ProfileTab.STATS -> "Stats"
        ProfileTab.SAVED -> "Saved"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label content is coming soon.",
            style = typography.bodyMedium,
            color = colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun clipBackgroundBrush(background: ClipBackground): Brush {
    val gradients = TrailsTheme.gradients
    val colors = TrailsTheme.colors
    val colorScheme = TrailsTheme.colorScheme

    return when (background) {
        ClipBackground.OCEAN -> gradients.ocean
        ClipBackground.MINT -> gradients.mint
        ClipBackground.ROSE -> gradients.rose
        ClipBackground.DEEP -> gradients.deepPurple
        ClipBackground.SUNSET -> gradients.sunset
        ClipBackground.FROST -> Brush.linearGradient(
            colors = listOf(colors.gradientStart, colors.gradientMid, colors.gradientEnd)
        )
        ClipBackground.NIGHT -> Brush.linearGradient(
            colors = listOf(colorScheme.surfaceVariant, colorScheme.surface)
        )
    }
}

@Composable
private fun clipThumbnailPainter(
    thumbnail: ClipThumbnail,
    viewMode: ClipViewMode
): Painter {
    return if (viewMode == ClipViewMode.LIST) {
        when (thumbnail) {
            ClipThumbnail.CLIP_01 -> ClipPlaceholders.Clip01Landscape
            ClipThumbnail.CLIP_02 -> ClipPlaceholders.Clip02Landscape
            ClipThumbnail.CLIP_03 -> ClipPlaceholders.Clip03Landscape
            ClipThumbnail.CLIP_04 -> ClipPlaceholders.Clip04Landscape
            ClipThumbnail.CLIP_05 -> ClipPlaceholders.Clip05Landscape
            ClipThumbnail.CLIP_06 -> ClipPlaceholders.Clip06Landscape
            ClipThumbnail.CLIP_07 -> ClipPlaceholders.Clip07Landscape
            ClipThumbnail.CLIP_08 -> ClipPlaceholders.Clip08Landscape
            ClipThumbnail.CLIP_09 -> ClipPlaceholders.Clip09Landscape
            ClipThumbnail.CLIP_10 -> ClipPlaceholders.Clip10Landscape
            ClipThumbnail.CLIP_11 -> ClipPlaceholders.Clip11Landscape
            ClipThumbnail.CLIP_12 -> ClipPlaceholders.Clip12Landscape
        }
    } else {
        when (thumbnail) {
            ClipThumbnail.CLIP_01 -> ClipPlaceholders.Clip01
            ClipThumbnail.CLIP_02 -> ClipPlaceholders.Clip02
            ClipThumbnail.CLIP_03 -> ClipPlaceholders.Clip03
            ClipThumbnail.CLIP_04 -> ClipPlaceholders.Clip04
            ClipThumbnail.CLIP_05 -> ClipPlaceholders.Clip05
            ClipThumbnail.CLIP_06 -> ClipPlaceholders.Clip06
            ClipThumbnail.CLIP_07 -> ClipPlaceholders.Clip07
            ClipThumbnail.CLIP_08 -> ClipPlaceholders.Clip08
            ClipThumbnail.CLIP_09 -> ClipPlaceholders.Clip09
            ClipThumbnail.CLIP_10 -> ClipPlaceholders.Clip10
            ClipThumbnail.CLIP_11 -> ClipPlaceholders.Clip11
            ClipThumbnail.CLIP_12 -> ClipPlaceholders.Clip12
        }
    }
}

@Composable
private fun badgeIcon(badge: ClipBadge): TrailsIcon {
    return when (badge) {
        ClipBadge.PLAY -> Icons.Outlined.Play
        ClipBadge.CAMERA -> Icons.Outlined.Camera
        ClipBadge.AUDIO -> Icons.Outlined.AudioWave
        ClipBadge.MAP -> Icons.Outlined.Maps
    }
}

private sealed interface ClipGridChip {
    data class Like(val likes: Int) : ClipGridChip
    data class Location(val location: String) : ClipGridChip
    data class Status(val status: ClipStatus) : ClipGridChip
}

private fun gridChipFor(clip: ClipItem): ClipGridChip? {
    clip.status?.let { status -> return ClipGridChip.Status(status) }
    clip.likes?.let { likes -> return ClipGridChip.Like(likes) }
    clip.location?.let { location -> return ClipGridChip.Location(location) }
    return null
}

private fun tabLabel(tab: ProfileTab): String = when (tab) {
    ProfileTab.CLIPS -> "Clips"
    ProfileTab.STATS -> "Stats"
    ProfileTab.SAVED -> "Saved"
}

@Composable
private fun tabIcon(tab: ProfileTab): TrailsIcon = when (tab) {
    ProfileTab.CLIPS -> Icons.Outlined.Album
    ProfileTab.STATS -> Icons.Outlined.ChartIncrease
    ProfileTab.SAVED -> Icons.Outlined.Bookmark
}

private fun filterLabel(filter: ClipFilter): String = when (filter) {
    ClipFilter.ALL -> "All"
    ClipFilter.UPLOADS -> "Uploads"
    ClipFilter.DRAFTS -> "Drafts"
    ClipFilter.GREEN -> "Green"
    ClipFilter.BLUE -> "Blue"
    ClipFilter.BLACK -> "Black"
    ClipFilter.DOUBLE -> "Double"
}

@Composable
private fun filterIndicatorColor(filter: ClipFilter): Color? {
    val colors = TrailsTheme.colors
    return when (filter) {
        ClipFilter.GREEN -> colors.greenCircle
        ClipFilter.BLUE -> colors.blueSquare
        ClipFilter.BLACK -> colors.blackDiamond
        ClipFilter.DOUBLE -> colors.doubleBlack
        else -> null
    }
}
