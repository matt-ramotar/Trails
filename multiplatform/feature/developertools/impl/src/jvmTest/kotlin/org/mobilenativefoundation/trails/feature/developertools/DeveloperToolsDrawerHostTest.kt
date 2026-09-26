package org.mobilenativefoundation.trails.feature.developertools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import org.mobilenativefoundation.trails.screen.activity.ActivityUi
import org.mobilenativefoundation.trails.screen.activity.ActivityState
import org.mobilenativefoundation.trails.screen.activity.ActivityIntent
import org.mobilenativefoundation.trails.data.trail.LoadState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.withKeysDown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.mobilenativefoundation.trails.data.developersettings.BackendConflictMode
import org.mobilenativefoundation.trails.data.developersettings.BackendSimulationSeedPreset
import org.mobilenativefoundation.trails.data.developersettings.DeveloperSettings
import org.mobilenativefoundation.trails.data.developersettings.DeveloperSettingsRepository
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.data.backend.BackendConfig
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class DeveloperToolsDrawerHostTest {
    @Test
    fun healthyActivityExposesGenuineRetryAndDisposesItWithItsScreen() = drawerTest {
        var activityVisible by mutableStateOf(true)
        var retries = 0
        val repository = RecordingSettingsRepository()
        setContent {
            TrailsTheme {
                DeveloperToolsDrawerHost(repository) {
                    if (activityVisible) {
                        ActivityUi().Content(
                            ActivityState(
                                history = LoadState(emptyList(), loading = false),
                                trails = emptyMap(),
                                saved = LoadState(loading = false),
                                nowEpochMillis = 0L,
                            ) { intent ->
                                if (intent == ActivityIntent.Retry) retries++
                            }, Modifier,
                        )
                    }
                }
            }
        }
        openDrawer()
        onNodeWithText("Try again").performScrollTo().performClick()
        runOnIdle { assertEquals(1, retries); activityVisible = false }
        onNodeWithText("Try again").assertDoesNotExist()
        runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun retainedActivityLosesRetryWhenRouteOrAccountScopeChanges() = drawerTest {
        var account by mutableStateOf("alice")
        var activityCurrent by mutableStateOf(true)
        var sendRevision by mutableStateOf(0)
        val invocations = mutableListOf<String>()
        val registrations = mutableMapOf<String, ActivityDeveloperActions>()
        val repository = RecordingSettingsRepository()
        setContent {
            key(account) {
                TrailsTheme {
                    DeveloperToolsDrawerHost(repository) {
                        val actions = requireNotNull(LocalActivityDeveloperActions.current)
                        val owner = account
                        val revision = sendRevision
                        SideEffect { registrations[owner] = actions }
                        // Model Circuit's outgoing UI still being composed after the top route changed.
                        CompositionLocalProvider(LocalActivityDeveloperActions provides actions.takeIf { activityCurrent }) {
                            ActivityUi().Content(
                                ActivityState(
                                    history = LoadState(emptyList(), loading = false),
                                    trails = emptyMap(),
                                    saved = LoadState(loading = false),
                                    nowEpochMillis = 0L,
                                ) { intent ->
                                    if (intent == ActivityIntent.Retry) invocations += "$owner/$revision"
                                }, Modifier,
                            )
                        }
                    }
                }
            }
        }
        runOnIdle { sendRevision = 1 }
        openDrawer()
        onNodeWithText("Try again").performScrollTo().performClick()
        runOnIdle { assertEquals(listOf("alice/1"), invocations); activityCurrent = false }
        onNodeWithText("Try again").assertDoesNotExist()
        runOnIdle { registrations.getValue("alice").retry(); assertEquals(1, invocations.size); activityCurrent = true }
        onNodeWithText("Try again").assertExists()
        runOnIdle { account = "bob" }
        runOnIdle { registrations.getValue("alice").retry(); assertEquals(1, invocations.size) }
        openDrawer()
        onNodeWithText("Try again").performScrollTo().performClick()
        runOnIdle { assertEquals(listOf("alice/1", "bob/1"), invocations) }
    }

    @Test
    fun ordinaryTypingInAppContentIsDeliveredWithDrawerClosed() = drawerTest {
        var deliveredLetterKeys = 0
        showDrawer(onLetterKey = { deliveredLetterKeys++ })

        onNodeWithTag(APP_INPUT).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithTag(APP_INPUT).performKeyInput { pressKey(Key.R) }
        onNodeWithTag(APP_INPUT).performTextInput("trail")

        runOnIdle {
            assertEquals(1, deliveredLetterKeys, "The drawer must not consume the content's letter key")
        }
        onNodeWithTag(APP_INPUT).assertTextContains("trail", substring = true)
    }

    @Test
    fun escapeFromDrawerFocusClosesIt() = drawerTest {
        showDrawer()
        openDrawer()
        focusDrawerButton()

        onRoot().performKeyInput { pressKey(Key.Escape) }

        onNodeWithContentDescription(CLOSE_DRAWER).assertIsNotDisplayed()
    }

    @Test
    fun failedResetIsVisibleAndSuccessfulRetryClearsTheFailure() = drawerTest {
        val repository = RecordingSettingsRepository().apply { writeFailure = IllegalStateException("disk full") }
        showDrawer(repository = repository)
        openDrawer()

        onNodeWithText("Reset").performClick()

        onNodeWithText("Reset settings failed: disk full").assertIsDisplayed()
        onNodeWithText("Live").assertDoesNotExist()
        runOnIdle { repository.writeFailure = null }
        onNodeWithText("Reset").performClick()
        onNodeWithText("Reset settings failed: disk full").assertDoesNotExist()
        runOnIdle { assertEquals(2, repository.writeAttempts) }
    }

    @Test
    fun failedSettingsWriteIsVisibleWithoutClaimingItIsApplied() = drawerTest {
        val repository = RecordingSettingsRepository().apply { writeFailure = IllegalStateException("settings unavailable") }
        showDrawer(repository = repository)
        openDrawer()

        // Target the named native Switch, rather than its separate visible caption.
        onNode(hasText("Offline mode") and isToggleable()).performScrollTo().performClick()

        onNodeWithText("Save settings failed: settings unavailable").assertIsDisplayed()
        onNodeWithText("Live").assertDoesNotExist()
        runOnIdle { assertEquals(false, repository.current.offlineMode) }
    }

    @Test
    fun unavailableBackendDoesNotClaimToBeLive() = drawerTest {
        showDrawer(status = { BackendConfigSyncStatus.Unavailable })
        openDrawer()

        onNodeWithText("Runtime controls unavailable.").assertIsDisplayed()
        onNodeWithText("Live").assertDoesNotExist()
        focusDrawerButton()
    }

    @Test
    fun failedRuntimeApplicationOffersRetryAndThenShowsApplied() = drawerTest {
        val repository = RecordingSettingsRepository()
        var status by mutableStateOf<BackendConfigSyncStatus>(BackendConfigSyncStatus.Failed("backend down"))
        var retries = 0
        showDrawer(repository, status = { status }, retry = {
            retries++
            status = BackendConfigSyncStatus.Applied(repository.current)
        })
        openDrawer()
        onNodeWithText("backend down").assertIsDisplayed()
        onNodeWithText("Applied").assertDoesNotExist()
        onNodeWithText("Retry runtime apply").performClick()
        onNodeWithText("Applied").assertIsDisplayed()
        runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun pendingApplicationDoesNotClaimSuccess() = drawerTest {
        var status by mutableStateOf<BackendConfigSyncStatus>(BackendConfigSyncStatus.Applying)
        val repository = RecordingSettingsRepository()
        showDrawer(repository, status = { status })
        openDrawer()
        onNodeWithText("Applying").assertIsDisplayed()
        onNodeWithText("Applied").assertDoesNotExist()
        runOnIdle { status = BackendConfigSyncStatus.Applied(repository.current) }
        onNodeWithText("Applied").assertIsDisplayed()
    }

    @Test
    fun copiedConfigContainsOnlySupportedBackendControls() {
        val config = DeveloperSettings().toConfigJson()
        kotlin.test.assertFalse(config.contains("clientStrategy"))
        kotlin.test.assertFalse(config.contains("resolvedSeed"))
        kotlin.test.assertTrue(config.contains("backendMode"))
        kotlin.test.assertTrue(config.contains("seed"))
    }

    private fun drawerTest(block: ComposeUiTest.() -> Unit) =
        runDesktopComposeUiTest(width = 900, height = 1800) { block() }

    private fun ComposeUiTest.showDrawer(
        repository: RecordingSettingsRepository = RecordingSettingsRepository(),
        status: () -> BackendConfigSyncStatus? = { null },
        onLetterKey: () -> Unit = {},
        retry: () -> Unit = {},
    ) {
        setContent {
            TrailsTheme {
                DeveloperToolsDrawerHost(
                    developerSettingsRepository = repository,
                    backendConfigStatus = status(),
                    onRetryBackendConfig = retry,
                ) {
                    var input by remember { mutableStateOf("") }
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxSize().testTag(APP_INPUT).onPreviewKeyEvent { event ->
                            if (event.key == Key.R && event.type == KeyEventType.KeyDown) onLetterKey()
                            false
                        },
                    )
                }
            }
        }
    }

    private fun ComposeUiTest.openDrawer() {
        onRoot().performTouchInput {
            swipe(start = Offset(1f, height / 2f), end = Offset(650f, height / 2f), durationMillis = 400)
        }
        onNodeWithContentDescription(CLOSE_DRAWER).assertIsDisplayed()
    }

    private fun ComposeUiTest.focusDrawerButton() {
        onNodeWithContentDescription(CLOSE_DRAWER).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
    }

    private class RecordingSettingsRepository : DeveloperSettingsRepository {
        private val state = MutableStateFlow(DeveloperSettings())
        var writeFailure: Exception? = null
        var writeAttempts = 0
        override val current: DeveloperSettings get() = state.value
        override fun stream() = state
        override suspend fun update(settings: DeveloperSettings) {
            writeAttempts++
            writeFailure?.let { throw it }
            state.value = settings
        }
        override suspend fun setOfflineMode(enabled: Boolean) = update(current.copy(offlineMode = enabled))
        override suspend fun setConflictsEnabled(enabled: Boolean) = update(current.copy(conflictsEnabled = enabled))
        override suspend fun setLatencyRange(minMs: Long, maxMs: Long) = update(current.copy(latencyMinMs = minMs, latencyMaxMs = maxMs))
        override suspend fun setErrorRate(rate: Float) = update(current.copy(errorRate = rate))
        override suspend fun setRateLimitPerMinute(limit: Int) = update(current.copy(rateLimitPerMinute = limit))
        override suspend fun setBackendConflictMode(mode: BackendConflictMode) = update(current.copy(backendConflictMode = mode))
        override suspend fun setConflictProbability(probability: Float) = update(current.copy(conflictProbability = probability))
        override suspend fun setSimulationSeedPreset(seedPreset: BackendSimulationSeedPreset) = update(current.copy(simulationSeedPreset = seedPreset))
        override suspend fun resetToDefaults() = update(DeveloperSettings())
    }

    private companion object {
        const val APP_INPUT = "app-input"
        const val CLOSE_DRAWER = "Close developer tools"
    }
}
