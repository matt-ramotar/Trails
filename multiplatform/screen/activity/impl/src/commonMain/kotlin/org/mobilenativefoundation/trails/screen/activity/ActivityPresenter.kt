@file:OptIn(kotlin.time.ExperimentalTime::class)

package org.mobilenativefoundation.trails.screen.activity

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.feat.savetrail.SaveTrailFeature
import org.mobilenativefoundation.trails.screen.explore.M1Navigation

@Inject
class ActivityPresenter(
    private val activities: ActivityRepository,
    private val trails: TrailRepository,
    private val savedRepository: SavedRepository,
    private val navigation: M1Navigation,
    private val saves: SaveTrailFeature,
) : Presenter<ActivityState> {
    @Composable
    override fun present(): ActivityState {
        var retryAttempt by remember { mutableIntStateOf(0) }
        val history = remember(retryAttempt) {
            activities.observe().retainActivityDataOnFailure("Couldn’t load your activity")
        }.collectAsState(initial = LoadState()).value
        val catalog = remember(retryAttempt) {
            trails.observeQuery(TrailQuery()).retainActivityDataOnFailure("Couldn’t load this trail")
        }.collectAsState(initial = LoadState()).value
        val saved by savedRepository.state.collectAsState()
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val now by remember(lifecycle) { activityClock(lifecycle) }
            .collectAsState(initial = Clock.System.now().toEpochMilliseconds())
        val scope = rememberCoroutineScope()
        var refreshing by remember { mutableStateOf(false) }
        var refreshErrors by remember { mutableStateOf<List<String>>(emptyList()) }
        // History remains visible while the independently loaded catalog recovers its hearts.
        val catalogComplete = catalog.data?.let { cached ->
            history.data?.all { activity -> cached.any { it.id == activity.trailId } } == true
        } == true
        val root = history.copy(
            loading = history.loading || catalog.loading || refreshing,
            error = (listOfNotNull(history.visibleActivityError(), catalog.visibleActivityError(catalogComplete)) + refreshErrors).distinct().takeIf { it.isNotEmpty() }?.joinToString(" · "),
            offline = history.offline || catalog.offline,
        )
        return ActivityState(root, catalog.data.orEmpty().associateBy { it.id }, saved, now, navigation.scrollPosition("ACTIVITY/activity")) { intent ->
            when (intent) {
                is ActivityIntent.ScrollChanged -> navigation.checkpointScroll("ACTIVITY/activity", intent.position)
                is ActivityIntent.OpenTrail -> navigation.openTrail(intent.trailId)
                is ActivityIntent.SaveTrail -> saves.open(intent.trail, intent.onDismiss)
                ActivityIntent.Explore -> navigation.selectExplore()
                ActivityIntent.Retry -> if (!refreshing) scope.launch {
                    refreshing = true
                    refreshErrors = emptyList()
                    suspend fun refresh(action: suspend () -> Unit) {
                        try { action() }
                        catch (failure: Exception) {
                            if (failure is CancellationException) throw failure
                            refreshErrors = refreshErrors + (failure.message ?: "Couldn’t load your activity")
                        }
                    }
                    try {
                        refresh { activities.refresh() }
                        refresh { trails.refreshQuery(TrailQuery()) }
                    } finally {
                        // Reconnect after refresh so a terminated observer reads the recovered data.
                        retryAttempt++
                        refreshing = false
                    }
                }
            }
        }
    }
}

/** Resample on foreground resume and local midnight even when cached history is unchanged. */
internal fun activityClock(
    lifecycle: Lifecycle,
    clock: Clock = Clock.System,
    zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
): Flow<Long> = channelFlow {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        while (isActive) {
            val now = clock.now().toEpochMilliseconds()
            send(now)
            val currentZone = zone()
            val nextMidnight = localDate(now, currentZone).plus(1, DateTimeUnit.DAY)
                .atStartOfDayIn(currentZone).toEpochMilliseconds()
            delay((nextMidnight - now).coerceAtLeast(1L))
        }
    }
}

/** Keep the last payload and offline state if an observer terminates unexpectedly. */
private fun <T> Flow<LoadState<T>>.retainActivityDataOnFailure(fallback: String): Flow<LoadState<T>> {
    var latest = LoadState<T>()
    return onEach { latest = it }.catch { failure ->
        if (failure is CancellationException) throw failure
        emit(latest.copy(loading = false, error = failure.message ?: fallback))
    }
}

/**
 * LoadState currently carries flattened Store diagnostics. Recognize only the fake backend's
 * expected Offline fetch rejection, per source, with usable cached content. Other fetch,
 * persistence and catalog failures must continue to offer recovery, even while Offline.
 */
private fun LoadState<*>.visibleActivityError(cacheUsable: Boolean = data != null): String? =
    error?.takeUnless {
        offline && cacheUsable && it.startsWith("Fetch failed for key '") &&
            it.endsWith(": The fake backend Offline setting is applied. The fetcher threw; inspect the cause for the underlying failure.")
    }
