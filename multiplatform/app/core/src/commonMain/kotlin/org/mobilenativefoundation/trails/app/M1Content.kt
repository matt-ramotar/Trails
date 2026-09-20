package org.mobilenativefoundation.trails.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import org.mobilenativefoundation.trails.screen.activity.ActivityScreen
import org.mobilenativefoundation.trails.screen.activity.LocalActivityDeveloperActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collect
import org.mobilenativefoundation.trails.foundation.designsystem.component.StatusKind
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsStatusLine
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import org.mobilenativefoundation.trails.di.graph.active.ActiveGraph
import org.mobilenativefoundation.trails.di.graph.active.M1NavigationController.Root
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsDestination
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsFloatingNav
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@Composable
internal fun M1Content(graph: ActiveGraph) = key(graph.account) {
    val navigation = graph.navigation
    val developerToolsOpen = LocalDeveloperToolsDrawerOpen.current
    LaunchedEffect(navigation) {
        snapshotFlow { navigation.selectedRoot to navigation.allStacks.map { stack -> stack.map { it.screen } } }
            .collect { navigation.checkpoint() }
    }
    val colors = TrailsTheme.colors
    val savedState = rememberSaveableStateHolder()
    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            TrailsFloatingNav(
                items = TrailsDestination.entries,
                selected = navigation.selectedRoot.destination(),
                onSelect = { destination ->
                    when (destination) {
                        TrailsDestination.EXPLORE -> navigation.selectExplore()
                        TrailsDestination.FOR_YOU -> navigation.selectForYou()
                        TrailsDestination.NAVIGATE -> navigation.selectNavigate()
                        TrailsDestination.SAVED -> navigation.selectSavedTab()
                        TrailsDestination.ACTIVITY -> navigation.selectActivity()
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                navigation.persistenceError?.let {
                    TrailsStatusLine(StatusKind.FAILED, "Couldn’t save your place", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), actionLabel = "Retry", onAction = navigation::retryCheckpoint)
                }
                savedState.SaveableStateProvider(navigation.selectedRoot.name) {
                    val platformNavigator = rememberCircuitNavigator(
                        backStack = navigation.backStack,
                        onRootPop = {},
                        enableBackHandler = navigation.backStack.size > 1 && !developerToolsOpen,
                    )
                    // Circuit may retain an outgoing composition during its transition. Only the
                    // actual top route may register a developer action with this account's host.
                    val activityActions = LocalActivityDeveloperActions.current
                        .takeIf { navigation.backStack.topRecord?.screen == ActivityScreen }
                    CompositionLocalProvider(LocalActivityDeveloperActions provides activityActions) {
                        NavigableCircuitContent(backStack = navigation.backStack, navigator = platformNavigator, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            graph.saves.Toast(Modifier.align(Alignment.BottomCenter))
        }
    }
    graph.filters.Content()
    graph.saves.Content(onViewSaved = navigation::selectSaved)
}

private fun Root.destination(): TrailsDestination = when (this) {
    Root.EXPLORE -> TrailsDestination.EXPLORE
    Root.FOR_YOU -> TrailsDestination.FOR_YOU
    Root.NAVIGATE -> TrailsDestination.NAVIGATE
    Root.SAVED -> TrailsDestination.SAVED
    Root.ACTIVITY -> TrailsDestination.ACTIVITY
}
