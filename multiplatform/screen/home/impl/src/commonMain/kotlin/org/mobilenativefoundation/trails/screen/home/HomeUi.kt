package org.mobilenativefoundation.trails.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.foundation.designsystem.component.CommentsModal
import org.mobilenativefoundation.trails.foundation.designsystem.component.ShareModal
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.foundation.scope.ActiveScope

@ContributesBinding(ActiveScope::class)
@Inject
class HomeUi : Ui<HomeState> {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content(state: HomeState, modifier: Modifier) {
        when (state) {
            is HomeState.Initial -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // TODO
                    Text(
                        text = "Loading feed...",
                        color = Color.White,
                        style = TrailsTheme.typography.bodyLarge
                    )
                }
            }

            is HomeState.Data -> {
                val posts = state.feed.items.filterIsInstance<FeedItem.SkiRunPost>()
                    .map { it.post }

                if (posts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // TODO
                        Text(
                            text = "No posts yet",
                            color = Color.White,
                            style = TrailsTheme.typography.bodyLarge
                        )
                    }
                    return
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0a1628))
                ) {
                    val pagerState = rememberPagerState(
                        initialPage = state.currentPostIndex,
                        pageCount = { posts.size }
                    )

                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }.collect { page ->
                            state.send(HomeIntent.PostScrolled(page))
                        }
                    }

                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { page ->
                        val post = posts[page]

                        PostCard(
                            post = post,
                            onLikeClick = { state.send(HomeIntent.ToggleLike(post.id)) },
                            onCommentClick = { state.send(HomeIntent.ShowComments(post.id)) },
                            onShareClick = { state.send(HomeIntent.ShowShare(post.id)) },
                            onFollowClick = { state.send(HomeIntent.ToggleFollow(post.username)) }
                        )
                    }

                    if (state.showCommentsModal && state.selectedPostIdForComments != null) {
                        val selectedPost = posts.find { it.id == state.selectedPostIdForComments }
                        if (selectedPost != null) {
                            CommentsModal(
                                commentCount = selectedPost.comments,
                                onDismiss = { state.send(HomeIntent.DismissComments) }
                            )
                        }
                    }

                    if (state.showShareModal) {
                        ShareModal(
                            username = "tag",
                            onCopyLink = { /* TODO: Copy link */ },
                            onShare = { /* TODO: Share */ },
                            onDismiss = { state.send(HomeIntent.DismissShare) }
                        )
                    }
                }
            }
        }
    }
}