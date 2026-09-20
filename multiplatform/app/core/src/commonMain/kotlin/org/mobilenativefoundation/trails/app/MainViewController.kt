package org.mobilenativefoundation.trails.app

import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsBrand
import org.mobilenativefoundation.trails.foundation.designsystem.component.ButtonTone
import org.mobilenativefoundation.trails.foundation.designsystem.component.StatusKind
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsButton
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsStatusLine
import org.mobilenativefoundation.trails.di.graph.app.BackendConfigSyncStatus
import org.mobilenativefoundation.trails.app.bootstrap.AppRoot
import org.mobilenativefoundation.trails.di.graph.app.AppGraph
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

class MainViewController(
    private val graph: AppGraph
) {
    private val backendConfigSynchronizer = graph.backendConfigSynchronizer
    val bootstrapCoordinator = graph.bootstrapCoordinator

    init {
        bootstrapCoordinator.start()
    }

    @Composable
    fun Content() {
        val root by bootstrapCoordinator.route.collectAsState()
        val backendConfigStatus by backendConfigSynchronizer.status.collectAsState()

        when (val current = root) {
            is AppRoot.Failed -> TrailsTheme {
                Column(
                    Modifier.fillMaxSize().background(TrailsTheme.colors.background)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TrailsBrand()
                    TrailsStatusLine(StatusKind.FAILED, current.message)
                    TrailsButton("Try again", bootstrapCoordinator::retry, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())
                }
            }
            AppRoot.Splash -> TrailsTheme {
                Column(
                    Modifier.fillMaxSize().background(TrailsTheme.colors.background)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TrailsBrand()
                    Spacer(Modifier.height(24.dp))
                    val failure = backendConfigStatus as? BackendConfigSyncStatus.Failed
                    if (failure == null) {
                        CircularProgressIndicator()
                        Text("Restoring your trails…", style = TrailsTheme.typography.bodyMedium)
                    } else {
                        Text(failure.message, style = TrailsTheme.typography.bodyMedium)
                        Button(onClick = backendConfigSynchronizer::retry) { Text("Try again") }
                    }
                }
            }

            is AppRoot.Welcome -> {
                MainContent(circuit = current.graph.circuit) {
                    DeveloperToolsDrawerHost(
                        developerSettingsRepository = graph.developerSettingsRepository,
                        backendControl = graph.backendControl,
                        backendConfigStatus = backendConfigStatus,
                        onRetryBackendConfig = backendConfigSynchronizer::retry,
                        userRepository = graph.userRepository,
                        legacyReplayEnabled = false,
                    ) {
                        NavigableCircuitContent(
                            navigator = current.graph.navigator,
                            backStack = current.graph.backStack
                        )
                    }
                }
            }

            is AppRoot.PreLanding -> {
                MainContent(circuit = current.graph.circuit) {
                    DeveloperToolsDrawerHost(
                        developerSettingsRepository = graph.developerSettingsRepository,
                        backendControl = graph.backendControl,
                        backendConfigStatus = backendConfigStatus,
                        onRetryBackendConfig = backendConfigSynchronizer::retry,
                        userRepository = graph.userRepository,
                        legacyReplayEnabled = false,
                    ) {
                        NavigableCircuitContent(
                            navigator = current.graph.navigator,
                            backStack = current.graph.backStack
                        )
                    }
                }
            }

            is AppRoot.Main -> key(current.graph) {
                MainContent(circuit = current.graph.circuit) {
                    DeveloperToolsDrawerHost(
                        developerSettingsRepository = graph.developerSettingsRepository,
                        backendControl = graph.backendControl,
                        backendConfigStatus = backendConfigStatus,
                        onRetryBackendConfig = backendConfigSynchronizer::retry,
                        userRepository = graph.userRepository,
                        legacyReplayEnabled = false,
                    ) {
                        M1Content(current.graph)
                    }
                }
            }
        }
    }

    @Composable
    private fun MainContent(
        circuit: Circuit,
        content: @Composable () -> Unit
    ) {
        TrailsTheme {
            CircuitCompositionLocals(circuit) {
                content()
            }
        }
    }
}
