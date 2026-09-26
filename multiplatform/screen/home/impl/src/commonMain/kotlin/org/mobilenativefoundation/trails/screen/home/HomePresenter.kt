package org.mobilenativefoundation.trails.screen.home

import androidx.compose.runtime.*
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.data.post.FeedKey
import org.mobilenativefoundation.trails.data.post.PostRepository
import org.mobilenativefoundation.trails.foundation.scope.ActiveScope

@ContributesBinding(ActiveScope::class)
@Inject
class HomePresenter(
    private val postRepository: PostRepository,
) : Presenter<HomeState> {
    @Composable
    override fun present(): HomeState {
        val scope = rememberCoroutineScope()
        val feedKey = remember { FeedKey.Home }
        var showCommentsModal by remember { mutableStateOf(false) }
        var showShareModal by remember { mutableStateOf(false) }
        var selectedPostIdForComments by remember { mutableStateOf<String?>(null) }
        var isRefreshing by remember { mutableStateOf(false) }
        var currentPostIndex by remember { mutableStateOf(0) }

        val posts by postRepository.streamFeed(feedKey).collectAsState(emptyList())
        val hasAttemptedInitialRefresh = remember { mutableStateOf(false) }

        LaunchedEffect(posts) {
            if (!hasAttemptedInitialRefresh.value && posts.isEmpty()) {
                hasAttemptedInitialRefresh.value = true
                postRepository.refreshFeed(feedKey)
            }
        }

        val send = { intent: HomeIntent ->
            when (intent) {
                is HomeIntent.ToggleLike -> {
                    val post = posts.firstOrNull { it.id == intent.postId }
                    if (post != null) {
                        scope.launch {
                            postRepository.setLiked(feedKey, intent.postId, !post.isLiked)
                        }
                        Unit
                    }
                }

                is HomeIntent.ToggleBookmark -> {
                    val post = posts.firstOrNull { it.id == intent.postId }
                    if (post != null) {
                        scope.launch {
                            postRepository.setBookmarked(feedKey, intent.postId, !post.isBookmarked)
                        }
                        Unit
                    }
                }

                is HomeIntent.ToggleFollow -> {
                    val post = posts.firstOrNull { it.username == intent.username }
                    if (post != null) {
                        scope.launch {
                            postRepository.setFollowing(feedKey, intent.username, !post.isFollowing)
                        }
                        Unit
                    }
                }

                is HomeIntent.ShowComments -> {
                    selectedPostIdForComments = intent.postId
                    showCommentsModal = true
                }

                is HomeIntent.DismissComments -> {
                    showCommentsModal = false
                    selectedPostIdForComments = null
                }

                is HomeIntent.ShowShare -> {
                    showShareModal = true
                }

                is HomeIntent.DismissShare -> {
                    showShareModal = false
                }

                is HomeIntent.PostScrolled -> {
                    currentPostIndex = intent.postIndex
                }

                is HomeIntent.Refresh -> {
                    isRefreshing = true
                    scope.launch {
                        postRepository.refreshFeed(feedKey)
                        isRefreshing = false
                    }
                    Unit
                }

                is HomeIntent.OpenNotifications -> {
                    // TODO: Navigate to notifications
                }
            }
        }

        val feedItems = posts.map { post ->
            FeedItem.SkiRunPost(post)
        }

        return HomeState.Data(
            showCommentsModal = showCommentsModal,
            showShareModal = showShareModal,
            feed = FeedState(items = feedItems),
            currentPostIndex = currentPostIndex,
            isRefreshing = isRefreshing,
            selectedPostIdForComments = selectedPostIdForComments,
            send = send
        )
    }
}
