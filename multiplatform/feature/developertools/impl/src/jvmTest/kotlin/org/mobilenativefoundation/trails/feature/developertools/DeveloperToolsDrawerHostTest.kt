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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.hasClickAction
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
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.flow.MutableStateFlow
import org.mobilenativefoundation.trails.data.developersettings.BackendConflictMode
import org.mobilenativefoundation.trails.data.developersettings.BackendSimulationSeedPreset
import org.mobilenativefoundation.trails.data.developersettings.DeveloperSettings
import org.mobilenativefoundation.trails.data.developersettings.DeveloperSettingsRepository
import org.mobilenativefoundation.trails.data.session.UserRepository
import org.mobilenativefoundation.trails.data.session.model.LoggedOutUser
import org.mobilenativefoundation.trails.data.session.model.User
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DeveloperToolsDrawerHostTest {
    @Test
    fun nativeTabsStartOnNetworkAndSelectSyncAndSession() = drawerTest {
        showDrawer()
        openDrawer()

        onNodeWithText("Network").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).assertIsSelected()
        onNodeWithText("Sync").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).assertIsNotSelected()
        onNodeWithText("Session").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).assertIsNotSelected()

        onNodeWithText("Sync").performScrollTo().performClick().assertIsSelected()
        onNodeWithText("Network").assertIsNotSelected()
        onNodeWithText("Session").performScrollTo().performClick().assertIsSelected()
        onNodeWithText("Sync").assertIsNotSelected()
    }

    @Test
    fun allControlsAndActionsRemainReachableAtDoubleFontScaleOnNarrowPhone() = drawerTest(width = 320, height = 720) {
        var activityRetries = 0
        var runtimeRetries = 0
        showDrawer(
            status = { BackendConfigSyncStatus.Failed("backend down") },
            retry = { runtimeRetries++ },
            fontScale = 2f,
            userRepository = RecordingUserRepository(),
            activityRetry = { activityRetries++ },
        )
        openDrawer()

        onNodeWithText("Developer tools").performScrollTo()
        onNodeWithText("Network").performScrollTo().assertIsDisplayed().assertIsSelected()
        onNode(hasText("Offline mode") and isToggleable()).performScrollTo().assertIsDisplayed().assertIsEnabled()
        listOf("Latency min", "Latency max", "Error rate", "Rate limit").forEach { label ->
            onNode(hasText(label) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).performScrollTo().assertIsDisplayed().assertIsEnabled().assertHeightIsAtLeast(48.dp)
        }

        selectSection("Sync")
        onNode(hasText("Conflicts enabled") and isToggleable()).performScrollTo().assertIsDisplayed().assertIsEnabled()
        onNode(hasText("Backend conflict mode") and hasClickAction()).performScrollTo().assertIsDisplayed().assertIsEnabled()
        onNode(hasText("Conflict probability") and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).performScrollTo().assertIsDisplayed().assertIsEnabled()

        selectSection("Session")
        listOf("Alex", "Robin", "Sign out").forEach { label ->
            onNodeWithText(label).performScrollTo().assertIsDisplayed().assertIsEnabled()
        }
        onNode(hasText("Simulation seed") and hasClickAction()).performScrollTo().assertIsDisplayed().assertIsEnabled()
        onNodeWithText("Try again").performScrollTo().assertIsDisplayed().performClick()
        onNodeWithText("backend down").performScrollTo().assertIsDisplayed()
        onNodeWithText("Retry runtime apply").performScrollTo().assertIsDisplayed().performClick()
        onNodeWithText("Reset").performScrollTo().assertIsDisplayed().assertIsEnabled()
        onNodeWithText("Copy config").performScrollTo().assertIsDisplayed().assertIsEnabled()
        runOnIdle {
            assertEquals(1, activityRetries)
            assertEquals(1, runtimeRetries)
        }
        onNodeWithContentDescription(CLOSE_DRAWER).assertIsDisplayed().performClick()
        onNodeWithContentDescription(CLOSE_DRAWER).assertIsNotDisplayed()
    }

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
        onNodeWithText("Session").performScrollTo().performClick()
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
        onNodeWithText("Session").performScrollTo().performClick()
        onNodeWithText("Try again").performScrollTo().performClick()
        runOnIdle { assertEquals(listOf("alice/1"), invocations); activityCurrent = false }
        onNodeWithText("Try again").assertDoesNotExist()
        runOnIdle { registrations.getValue("alice").retry(); assertEquals(1, invocations.size); activityCurrent = true }
        onNodeWithText("Try again").assertExists()
        runOnIdle { account = "bob" }
        runOnIdle { registrations.getValue("alice").retry(); assertEquals(1, invocations.size) }
        openDrawer()
        onNodeWithText("Session").performScrollTo().performClick()
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

        onNodeWithText("Reset").performScrollTo().performClick()

        onNodeWithText("Reset settings failed: disk full").performScrollTo().assertIsDisplayed()
        onNodeWithText("Live").assertDoesNotExist()
        runOnIdle { repository.writeFailure = null }
        onNodeWithText("Reset").performScrollTo().performClick()
        onNodeWithText("Reset settings failed: disk full").assertDoesNotExist()
        runOnIdle { assertEquals(2, repository.writeAttempts) }
    }

    @Test
    fun failedSettingsWriteIsVisibleWithoutClaimingItIsApplied() = drawerTest {
        val repository = RecordingSettingsRepository().apply { writeFailure = IllegalStateException("settings unavailable") }
        showDrawer(repository = repository)
        openDrawer()

        onNode(hasText("Offline mode") and isToggleable()).performScrollTo().performClick()

        onNodeWithText("Save settings failed: settings unavailable").performScrollTo().assertIsDisplayed()
        onNodeWithText("Live").assertDoesNotExist()
        runOnIdle { assertEquals(false, repository.current.offlineMode) }
    }

    @Test
    fun unavailableBackendDoesNotClaimToBeLive() = drawerTest {
        showDrawer(status = { BackendConfigSyncStatus.Unavailable })
        openDrawer()

        onNodeWithText("Runtime controls unavailable.").performScrollTo().assertIsDisplayed()
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
        onNodeWithText("backend down").performScrollTo().assertIsDisplayed()
        onNodeWithText("Applied").assertDoesNotExist()
        onNodeWithText("Retry runtime apply").performScrollTo().performClick()
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
    fun editingSettingsDoesNotReuseAppliedStatusForThePreviousConfiguration() = drawerTest {
        val repository = RecordingSettingsRepository()
        var status by mutableStateOf<BackendConfigSyncStatus>(BackendConfigSyncStatus.Applied(repository.current))
        showDrawer(repository, status = { status })
        openDrawer()
        onNodeWithText("Applied").assertIsDisplayed()

        onNode(hasText("Offline mode") and isToggleable()).performScrollTo().performClick()

        runOnIdle { assertEquals(true, repository.current.offlineMode) }
        onNodeWithText("Applied").assertDoesNotExist()
        onNodeWithText("Applying").assertIsDisplayed()
        runOnIdle { status = BackendConfigSyncStatus.Applied(repository.current) }
        onNodeWithText("Applied").assertIsDisplayed()
    }

    @Test
    fun unavailableControlsStayVisibleAndIgnoreInputWhileResetRemainsAvailable() = drawerTest {
        val repository = RecordingSettingsRepository()
        showDrawer(repository, status = { BackendConfigSyncStatus.Unavailable })
        openDrawer()

        onNode(hasText("Offline mode") and isToggleable())
            .performScrollTo().assertIsDisplayed().assertIsNotEnabled().performTouchInput { click() }
        listOf("Latency min", "Latency max", "Error rate", "Rate limit").forEach { label ->
            onNode(hasText(label) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
                .performScrollTo().assertIsDisplayed().assertIsNotEnabled().performTouchInput { click() }
        }
        onNodeWithText("Sync").performScrollTo().performClick()
        onNode(hasText("Conflicts enabled") and isToggleable())
            .performScrollTo().assertIsDisplayed().assertIsNotEnabled().performTouchInput { click() }
        onNode(hasText("Backend conflict mode") and hasClickAction())
            .performScrollTo().assertIsDisplayed().assertIsNotEnabled().performTouchInput { click() }
        onNodeWithText("409 Conflict").assertDoesNotExist()
        onNode(hasText("Conflict probability") and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .performScrollTo().assertIsDisplayed().assertIsNotEnabled().performTouchInput { click() }
        onNodeWithText("Session").performScrollTo().performClick()
        onNode(hasText("Simulation seed") and hasClickAction())
            .performScrollTo().assertIsDisplayed().assertIsNotEnabled().performTouchInput { click() }
        onNodeWithText("42").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, repository.writeAttempts)
            assertEquals(DeveloperSettings(), repository.current)
        }

        onNodeWithText("Reset").performScrollTo().assertIsEnabled().performClick()

        runOnIdle { assertEquals(1, repository.writeAttempts) }
    }

    @Test
    fun backendModeAndSeedSelectorsPersistEverySupportedEnumValue() = drawerTest {
        val repository = RecordingSettingsRepository()
        showDrawer(repository)
        openDrawer()
        onNodeWithText("Sync").performScrollTo().performClick()
        listOf(
            "409 Conflict" to BackendConflictMode.HTTP_409,
            "Auto-merge" to BackendConflictMode.AUTO_MERGE,
            "Last write wins" to BackendConflictMode.LAST_WRITE_WINS,
            "Disabled" to BackendConflictMode.DISABLED,
        ).forEach { (label, expected) ->
            onNode(hasText("Backend conflict mode") and hasClickAction()).performScrollTo().performClick()
            onNodeWithText(label).performClick()
            runOnIdle { assertEquals(expected, repository.current.backendConflictMode) }
        }

        onNodeWithText("Session").performScrollTo().performClick()
        listOf(
            "42" to BackendSimulationSeedPreset.SEED_42,
            "1337" to BackendSimulationSeedPreset.SEED_1337,
            "9001" to BackendSimulationSeedPreset.SEED_9001,
            "Random (recommended)" to BackendSimulationSeedPreset.RANDOM,
        ).forEach { (label, expected) ->
            onNode(hasText("Simulation seed") and hasClickAction()).performScrollTo().performClick()
            onNodeWithText(label).performClick()
            runOnIdle { assertEquals(expected, repository.current.simulationSeedPreset) }
        }
        runOnIdle { assertEquals(8, repository.writeAttempts) }
    }

    @Test
    fun latencySlidersClampSnapAndKeepMinimumAndMaximumOrdered() = drawerTest {
        val repository = RecordingSettingsRepository()
        showDrawer(repository)
        openDrawer()

        onNode(hasText("Latency min") and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(6500f) }
        runOnIdle {
            assertEquals(5000L, repository.current.latencyMinMs)
            assertEquals(5000L, repository.current.latencyMaxMs)
        }
        onNode(hasText("Latency max") and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(-100f) }
        runOnIdle {
            assertEquals(0L, repository.current.latencyMinMs)
            assertEquals(0L, repository.current.latencyMaxMs)
        }
        onNode(hasText("Latency min") and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(157f) }
        runOnIdle {
            assertEquals(160L, repository.current.latencyMinMs)
            assertEquals(160L, repository.current.latencyMaxMs)
            assertEquals(3, repository.writeAttempts)
        }
        onNode(hasText("Latency min") and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "160 milliseconds"))
        onNode(hasText("Latency max") and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "160 milliseconds"))
    }

    @Test
    fun failedSliderWriteRestoresItsPersistedValueAndAccessibleUnits() = drawerTest {
        val repository = RecordingSettingsRepository().apply { writeFailure = IllegalStateException("disk full") }
        showDrawer(repository)
        openDrawer()

        onNode(hasText("Error rate") and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(75f) }

        onNodeWithText("Save settings failed: disk full").performScrollTo().assertIsDisplayed()
        onNode(hasText("Error rate") and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "0 percent"))
        runOnIdle {
            assertEquals(0f, repository.current.errorRate)
            assertEquals(1, repository.writeAttempts)
        }
    }

    @Test
    fun copiedConfigContainsOnlySupportedBackendControls() {
        val config = DeveloperSettings().toConfigJson()
        kotlin.test.assertFalse(config.contains("clientStrategy"))
        kotlin.test.assertFalse(config.contains("resolvedSeed"))
        kotlin.test.assertTrue(config.contains("backendMode"))
        kotlin.test.assertTrue(config.contains("seed"))
    }

    private fun drawerTest(width: Int = 900, height: Int = 1800, block: ComposeUiTest.() -> Unit) =
        runDesktopComposeUiTest(width = width, height = height) { block() }

    private fun ComposeUiTest.showDrawer(
        repository: RecordingSettingsRepository = RecordingSettingsRepository(),
        status: () -> BackendConfigSyncStatus? = { null },
        onLetterKey: () -> Unit = {},
        retry: () -> Unit = {},
        fontScale: Float = 1f,
        userRepository: UserRepository? = null,
        activityRetry: (() -> Unit)? = null,
    ) {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = fontScale)) {
                TrailsTheme {
                    DeveloperToolsDrawerHost(
                        developerSettingsRepository = repository,
                        backendConfigStatus = status(),
                        onRetryBackendConfig = retry,
                        userRepository = userRepository,
                    ) {
                        if (activityRetry != null) {
                            ActivityUi().Content(
                                ActivityState(
                                    history = LoadState(emptyList(), loading = false),
                                    trails = emptyMap(),
                                    saved = LoadState(loading = false),
                                    nowEpochMillis = 0L,
                                ) { intent ->
                                    if (intent == ActivityIntent.Retry) activityRetry()
                                }, Modifier,
                            )
                        } else {
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
        }
    }

    private fun ComposeUiTest.openDrawer() {
        onRoot().performTouchInput {
            swipe(start = Offset(1f, height / 2f), end = Offset(width * 0.9f, height / 2f), durationMillis = 400)
        }
        onNodeWithContentDescription(CLOSE_DRAWER).assertIsDisplayed()
    }

    private fun ComposeUiTest.focusDrawerButton() {
        onNodeWithContentDescription(CLOSE_DRAWER).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
    }

    private fun ComposeUiTest.selectSection(label: String) {
        // Tabs scroll horizontally inside the vertically scrolling form.
        onNodeWithText("Developer tools").performScrollTo()
        onNodeWithText(label).performScrollTo().assertIsDisplayed().performClick().assertIsSelected()
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

    private class RecordingUserRepository : UserRepository {
        private val state = MutableStateFlow<User>(LoggedOutUser)
        override val current: User get() = state.value
        override fun stream() = state
        override suspend fun persist(user: User) { state.value = user }
    }

    private companion object {
        const val APP_INPUT = "app-input"
        const val CLOSE_DRAWER = "Close developer tools"
    }
}
