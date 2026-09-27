package org.mobilenativefoundation.trails.feature.developertools

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.data.developersettings.BackendConflictMode
import org.mobilenativefoundation.trails.data.developersettings.BackendSimulationSeedPreset
import org.mobilenativefoundation.trails.data.developersettings.DeveloperSettings
import org.mobilenativefoundation.trails.data.developersettings.DeveloperSettingsRepository
import org.mobilenativefoundation.trails.data.session.UserRepository
import org.mobilenativefoundation.trails.data.session.SampleAccounts
import org.mobilenativefoundation.trails.data.session.model.LoggedOutUser
import org.mobilenativefoundation.trails.foundation.designsystem.component.ButtonTone
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsIconButton
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsBrand
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsModalDrawer
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsSeparator
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsButton
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import kotlin.math.roundToInt

// Suppress destination Back while this overlay owns it, even if an account switch recreates
// the destination's callback after the drawer's platform callback was registered.
val LocalDeveloperToolsDrawerOpen = compositionLocalOf { false }

@Composable
fun DeveloperToolsDrawerHost(
    developerSettingsRepository: DeveloperSettingsRepository,
    modifier: Modifier = Modifier,
    backendConfigStatus: BackendConfigSyncStatus? = null,
    onRetryBackendConfig: () -> Unit = {},
    userRepository: UserRepository? = null,
    content: @Composable () -> Unit,
) {
    val activityActions = remember { ActivityDeveloperActions() }
    val settings by developerSettingsRepository.stream().collectAsState(initial = developerSettingsRepository.current)
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val drawerFocusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val clipboardManager = LocalClipboardManager.current
    val edgeSwipeThresholdPx = with(density) { EDGE_SWIPE_OPEN_THRESHOLD.toPx() }

    var section by remember { mutableStateOf(DeveloperToolsSection.Network) }

    var latencyMinDraft by remember { mutableStateOf(settings.latencyMinMs.toFloat()) }
    var latencyMaxDraft by remember { mutableStateOf(settings.latencyMaxMs.toFloat()) }
    var errorRateDraft by remember { mutableStateOf(settings.errorRate * 100f) }
    var rateLimitDraft by remember { mutableStateOf(settings.rateLimitPerMinute.toFloat()) }
    var conflictProbabilityDraft by remember { mutableStateOf(settings.conflictProbability * 100f) }
    var copyButtonLabel by remember { mutableStateOf("Copy config") }
    val controlsEnabled = backendConfigStatus != BackendConfigSyncStatus.Unavailable
    var actionError by remember { mutableStateOf<String?>(null) }
    var latestOperationId by remember { mutableStateOf(0L) }
    val applicationError = (backendConfigStatus as? BackendConfigSyncStatus.Failed)?.message
    val runtimeApplied = (backendConfigStatus as? BackendConfigSyncStatus.Applied)?.settings == settings
    val statusLabel = when {
        actionError != null || applicationError != null -> "Failed"
        !controlsEnabled || backendConfigStatus == BackendConfigSyncStatus.Unavailable -> "Unavailable"
        runtimeApplied -> "Applied"
        backendConfigStatus != null -> "Applying"
        else -> "Settings"
    }

    fun launchOperation(
        label: String,
        restoreDraftsOnFailure: Boolean = false,
        operation: suspend () -> Unit,
    ) {
        val operationId = ++latestOperationId
        scope.launch {
            try {
                operation()
                if (operationId == latestOperationId) actionError = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                actionError = "$label failed: ${failure.message ?: "Unknown error"}"
                if (restoreDraftsOnFailure) {
                    val persisted = developerSettingsRepository.current
                    latencyMinDraft = persisted.latencyMinMs.toFloat()
                    latencyMaxDraft = persisted.latencyMaxMs.toFloat()
                    errorRateDraft = persisted.errorRate * 100f
                    rateLimitDraft = persisted.rateLimitPerMinute.toFloat()
                    conflictProbabilityDraft = persisted.conflictProbability * 100f
                }
            }
        }
    }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) drawerFocusRequester.requestFocus()
    }

    LaunchedEffect(settings.latencyMinMs) { latencyMinDraft = settings.latencyMinMs.toFloat() }
    LaunchedEffect(settings.latencyMaxMs) { latencyMaxDraft = settings.latencyMaxMs.toFloat() }
    LaunchedEffect(settings.errorRate) { errorRateDraft = settings.errorRate * 100f }
    LaunchedEffect(settings.rateLimitPerMinute) { rateLimitDraft = settings.rateLimitPerMinute.toFloat() }
    LaunchedEffect(settings.conflictProbability) { conflictProbabilityDraft = settings.conflictProbability * 100f }

    val colors = TrailsTheme.colors
    val spacing = TrailsTheme.spacing
    val typography = TrailsTheme.typography

    TrailsModalDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerModifier = Modifier
            .focusRequester(drawerFocusRequester)
            .onPreviewKeyEvent { event ->
                if (drawerState.isOpen && event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    scope.launch { drawerState.close() }
                    true
                } else false
            }
            .focusable(enabled = drawerState.isOpen),
        drawerContent = {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TrailsBrand(Modifier.weight(1f))
                    TrailsIconButton(Icons.Outlined.Cancel.painter, "Close developer tools", { scope.launch { drawerState.close() } })
                }
                // The entire form, including feedback and actions, scrolls at large text sizes.
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Text("Developer tools", style = typography.headlineMedium, modifier = Modifier.semantics { heading() })
                        Text("Set up the conditions you want to test.", style = typography.bodyMedium, color = colors.textSecondary)
                        DeveloperToolsStatus(statusLabel)
                    }
                    actionError?.let { DeveloperToolsNotice(it, isError = true) }
                    applicationError?.let {
                        DeveloperToolsNotice(it, isError = true)
                        TrailsButton("Retry runtime apply", onRetryBackendConfig, Modifier.fillMaxWidth(), tone = ButtonTone.Secondary)
                    }
                    if (!controlsEnabled) DeveloperToolsNotice("Runtime controls unavailable.")
                    DeveloperToolsTabs(section, onSelected = { section = it })
                    when (section) {
                        DeveloperToolsSection.Network -> {
                            SettingsGroup {
                                SettingsToggle(
                                    label = "Offline mode",
                                    hint = "Fail backend requests immediately.",
                                    checked = settings.offlineMode,
                                    enabled = controlsEnabled,
                                    onCheckedChange = { enabled ->
                                        launchOperation("Save settings", restoreDraftsOnFailure = true) {
                                            developerSettingsRepository.setOfflineMode(enabled)
                                        }
                                    },
                                )
                            }
                            SettingsGroup {
                                SettingsSlider(
                                    label = "Latency min", valueText = "${latencyMinDraft.roundToInt()} ms",
                                    valueDescription = "${latencyMinDraft.roundToInt()} milliseconds",
                                    value = latencyMinDraft, valueRange = 0f..5000f, step = 10f, enabled = controlsEnabled,
                                    onValueChange = { value ->
                                        latencyMinDraft = value
                                        if (latencyMaxDraft < value) latencyMaxDraft = value
                                    },
                                    onValueChangeFinished = {
                                        launchOperation("Save settings", restoreDraftsOnFailure = true) {
                                            developerSettingsRepository.setLatencyRange(latencyMinDraft.roundToInt().toLong(), latencyMaxDraft.roundToInt().toLong())
                                        }
                                    },
                                )
                                SettingsDivider()
                                SettingsSlider(
                                    label = "Latency max", valueText = "${latencyMaxDraft.roundToInt()} ms",
                                    valueDescription = "${latencyMaxDraft.roundToInt()} milliseconds",
                                    value = latencyMaxDraft, valueRange = 0f..5000f, step = 10f, enabled = controlsEnabled,
                                    onValueChange = { value ->
                                        latencyMaxDraft = value
                                        if (latencyMinDraft > value) latencyMinDraft = value
                                    },
                                    onValueChangeFinished = {
                                        launchOperation("Save settings", restoreDraftsOnFailure = true) {
                                            developerSettingsRepository.setLatencyRange(latencyMinDraft.roundToInt().toLong(), latencyMaxDraft.roundToInt().toLong())
                                        }
                                    },
                                )
                                SettingsDivider()
                                SettingsSlider(
                                    label = "Error rate", valueText = "${errorRateDraft.roundToInt()}%",
                                    valueDescription = "${errorRateDraft.roundToInt()} percent",
                                    value = errorRateDraft, valueRange = 0f..100f, step = 1f, enabled = controlsEnabled,
                                    onValueChange = { errorRateDraft = it },
                                    onValueChangeFinished = {
                                        launchOperation("Save settings", restoreDraftsOnFailure = true) {
                                            developerSettingsRepository.setErrorRate((errorRateDraft / 100f).coerceIn(0f, 1f))
                                        }
                                    },
                                )
                                SettingsDivider()
                                SettingsSlider(
                                    label = "Rate limit",
                                    valueText = if (rateLimitDraft.roundToInt() == 0) "Unlimited" else "${rateLimitDraft.roundToInt()}/min",
                                    valueDescription = if (rateLimitDraft.roundToInt() == 0) "No rate limit" else "${rateLimitDraft.roundToInt()} requests per minute",
                                    value = rateLimitDraft, valueRange = 0f..120f, step = 1f, enabled = controlsEnabled,
                                    onValueChange = { rateLimitDraft = it },
                                    onValueChangeFinished = {
                                        launchOperation("Save settings", restoreDraftsOnFailure = true) {
                                            developerSettingsRepository.setRateLimitPerMinute(rateLimitDraft.roundToInt())
                                        }
                                    },
                                )
                            }
                        }
                        DeveloperToolsSection.Sync -> SettingsGroup {
                            SettingsToggle(
                                label = "Conflicts enabled", hint = "Introduce conflicts in backend responses.",
                                checked = settings.conflictsEnabled, enabled = controlsEnabled,
                                onCheckedChange = { enabled ->
                                    launchOperation("Save settings", restoreDraftsOnFailure = true) {
                                        developerSettingsRepository.setConflictsEnabled(enabled)
                                    }
                                },
                            )
                            SettingsDivider()
                            SettingsSelect(
                                label = "Backend conflict mode", hint = "Choose how the backend resolves conflicts.",
                                selected = settings.backendConflictMode, options = BackendConflictMode.entries,
                                enabled = controlsEnabled,
                                optionLabel = { mode ->
                                    when (mode) {
                                        BackendConflictMode.DISABLED -> "Disabled"
                                        BackendConflictMode.HTTP_409 -> "409 Conflict"
                                        BackendConflictMode.AUTO_MERGE -> "Auto-merge"
                                        BackendConflictMode.LAST_WRITE_WINS -> "Last write wins"
                                    }
                                },
                                onSelected = { mode ->
                                    launchOperation("Save settings", restoreDraftsOnFailure = true) {
                                        developerSettingsRepository.setBackendConflictMode(mode)
                                    }
                                },
                            )
                            SettingsDivider()
                            SettingsSlider(
                                label = "Conflict probability", valueText = "${conflictProbabilityDraft.roundToInt()}%",
                                valueDescription = "${conflictProbabilityDraft.roundToInt()} percent",
                                value = conflictProbabilityDraft, valueRange = 0f..100f, step = 1f, enabled = controlsEnabled,
                                onValueChange = { conflictProbabilityDraft = it },
                                onValueChangeFinished = {
                                    launchOperation("Save settings", restoreDraftsOnFailure = true) {
                                        developerSettingsRepository.setConflictProbability((conflictProbabilityDraft / 100f).coerceIn(0f, 1f))
                                    }
                                },
                            )
                        }
                        DeveloperToolsSection.Session -> {
                            if (userRepository != null) SettingsGroup {
                                Column(Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                    Text("Sample account", style = typography.titleSmall)
                                    Text("Switch accounts while keeping each account’s saved work.", style = typography.bodySmall, color = colors.textSecondary)
                                    TrailsButton("Alex", {
                                        launchOperation("Switch to Alex") { userRepository.persist(SampleAccounts.primary) }
                                    }, Modifier.fillMaxWidth(), tone = ButtonTone.Secondary)
                                    TrailsButton("Robin", {
                                        launchOperation("Switch to Robin") { userRepository.persist(SampleAccounts.secondary) }
                                    }, Modifier.fillMaxWidth(), tone = ButtonTone.Secondary)
                                    TrailsButton("Sign out", {
                                        launchOperation("Sign out") { userRepository.persist(LoggedOutUser) }
                                    }, Modifier.fillMaxWidth(), tone = ButtonTone.Ghost)
                                }
                            }
                            SettingsGroup {
                                SettingsSelect(
                                    label = "Simulation seed", hint = "Repeat the same random sequence across sessions.",
                                    selected = settings.simulationSeedPreset, options = BackendSimulationSeedPreset.entries,
                                    enabled = controlsEnabled,
                                    optionLabel = { preset ->
                                        when (preset) {
                                            BackendSimulationSeedPreset.RANDOM -> "Random (recommended)"
                                            BackendSimulationSeedPreset.SEED_42 -> "42"
                                            BackendSimulationSeedPreset.SEED_1337 -> "1337"
                                            BackendSimulationSeedPreset.SEED_9001 -> "9001"
                                        }
                                    },
                                    onSelected = { preset ->
                                        launchOperation("Save settings", restoreDraftsOnFailure = true) {
                                            developerSettingsRepository.setSimulationSeedPreset(preset)
                                        }
                                    },
                                )
                            }
                            if (activityActions.available) SettingsGroup {
                                Column(Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                    Text("Activity refresh", style = typography.titleSmall)
                                    TrailsButton("Try again", activityActions::retry, Modifier.fillMaxWidth(), tone = ButtonTone.Secondary)
                                }
                            }
                        }
                    }
                    TrailsSeparator()
                    Text("Settings save automatically. Reset restores saved defaults.", style = typography.bodySmall, color = colors.textSecondary)
                    TrailsButton("Reset", {
                        launchOperation("Reset settings", restoreDraftsOnFailure = true) { developerSettingsRepository.resetToDefaults() }
                    }, Modifier.fillMaxWidth(), tone = ButtonTone.Secondary)
                    TrailsButton(copyButtonLabel, {
                        clipboardManager.setText(AnnotatedString(settings.toConfigJson()))
                        scope.launch {
                            copyButtonLabel = "Copied ✓"
                            delay(1100)
                            copyButtonLabel = "Copy config"
                        }
                    }, Modifier.fillMaxWidth())
                }
        },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            CompositionLocalProvider(
                LocalDeveloperToolsDrawerOpen provides drawerState.isOpen,
                LocalActivityDeveloperActions provides activityActions,
            ) {
                content()
            }

            if (drawerState.isClosed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(EDGE_SWIPE_HITBOX_WIDTH)
                        .pointerInput(edgeSwipeThresholdPx) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    if (dragAmount > 0f) {
                                        totalDrag += dragAmount
                                        change.consume()
                                        if (totalDrag >= edgeSwipeThresholdPx) {
                                            totalDrag = 0f
                                            scope.launch {
                                                if (drawerState.isClosed) {
                                                    drawerState.open()
                                                }
                                            }
                                        }
                                    } else {
                                        totalDrag = 0f
                                    }
                                },
                                onDragEnd = { totalDrag = 0f },
                                onDragCancel = { totalDrag = 0f },
                            )
                        },
                )
            }
        }
    }
}

internal fun DeveloperSettings.toConfigJson(): String {
    val backendMode = when (backendConflictMode) {
        BackendConflictMode.DISABLED -> "disabled"
        BackendConflictMode.HTTP_409 -> "409"
        BackendConflictMode.AUTO_MERGE -> "merge"
        BackendConflictMode.LAST_WRITE_WINS -> "lastwrite"
    }
    val seed = when (simulationSeedPreset) {
        BackendSimulationSeedPreset.RANDOM -> "random"
        BackendSimulationSeedPreset.SEED_42 -> "42"
        BackendSimulationSeedPreset.SEED_1337 -> "1337"
        BackendSimulationSeedPreset.SEED_9001 -> "9001"
    }

    return buildString {
        appendLine("{")
        appendLine("  \"network\": {")
        appendLine("    \"offline\": $offlineMode,")
        appendLine("    \"latencyMs\": { \"min\": ${latencyMinMs.coerceAtLeast(0)}, \"max\": ${latencyMaxMs.coerceAtLeast(0)} },")
        appendLine("    \"errorRatePct\": ${(errorRate.coerceIn(0f, 1f) * 100f).roundToInt()},")
        appendLine("    \"rateLimitPerMinute\": ${rateLimitPerMinute.coerceAtLeast(0)}")
        appendLine("  },")
        appendLine("  \"conflict\": {")
        appendLine("    \"enabled\": $conflictsEnabled,")
        appendLine("    \"backendMode\": \"$backendMode\",")
        appendLine("    \"probabilityPct\": ${(conflictProbability.coerceIn(0f, 1f) * 100f).roundToInt()}")
        appendLine("  },")
        appendLine("  \"seed\": \"$seed\"")
        appendLine("}")
    }
}

private val EDGE_SWIPE_HITBOX_WIDTH = 28.dp
private val EDGE_SWIPE_OPEN_THRESHOLD = 14.dp
