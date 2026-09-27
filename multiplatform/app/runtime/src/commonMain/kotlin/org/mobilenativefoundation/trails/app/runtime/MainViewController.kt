package org.mobilenativefoundation.trails.app.runtime

import org.mobilenativefoundation.trails.feature.developertools.*

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
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsBrand
import org.mobilenativefoundation.trails.foundation.designsystem.component.ButtonTone
import org.mobilenativefoundation.trails.foundation.designsystem.component.StatusKind
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsButton
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsLoading
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsStatusLine
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsAlert
import org.mobilenativefoundation.trails.app.runtime.bootstrap.AppRoot
import org.mobilenativefoundation.trails.app.runtime.graph.app.AppGraph
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

internal class MainViewController(
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
                StartupFailureContent(current.message, bootstrapCoordinator::retry)
            }
            AppRoot.Splash -> TrailsTheme {
                StartupSplashContent(
                    failureMessage = (backendConfigStatus as? BackendConfigSyncStatus.Failed)?.message,
                    onRetry = backendConfigSynchronizer::retry,
                )
            }

            is AppRoot.Welcome -> {
                MainContent(circuit = current.graph.circuit) {
                    DeveloperToolsDrawerHost(
                        developerSettingsRepository = graph.developerSettingsRepository,
                        backendConfigStatus = backendConfigStatus,
                        onRetryBackendConfig = backendConfigSynchronizer::retry,
                        userRepository = graph.userRepository,
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
                        backendConfigStatus = backendConfigStatus,
                        onRetryBackendConfig = backendConfigSynchronizer::retry,
                        userRepository = graph.userRepository,
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
                        backendConfigStatus = backendConfigStatus,
                        onRetryBackendConfig = backendConfigSynchronizer::retry,
                        userRepository = graph.userRepository,
                    ) {
                        AccountContent(current.graph)
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

@Composable
internal fun StartupFailureContent(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(TrailsTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TrailsBrand()
        TrailsStatusLine(StatusKind.FAILED, message)
        TrailsButton("Try again", onRetry, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun StartupSplashContent(failureMessage: String?, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(TrailsTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TrailsBrand()
        Spacer(Modifier.height(24.dp))
        if (failureMessage == null) {
            TrailsLoading("Restoring your trails…")
        } else {
            TrailsAlert(failureMessage, StatusKind.FAILED, actionLabel = "Try again", onAction = onRetry)
        }
    }
}
