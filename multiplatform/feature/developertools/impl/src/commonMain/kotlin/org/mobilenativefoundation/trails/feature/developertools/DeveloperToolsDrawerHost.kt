package org.mobilenativefoundation.trails.feature.developertools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.data.developersettings.BackendConflictMode
import org.mobilenativefoundation.trails.data.developersettings.BackendSimulationSeedPreset
import org.mobilenativefoundation.trails.data.developersettings.DeveloperSettings
import org.mobilenativefoundation.trails.data.developersettings.DeveloperSettingsRepository
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.data.session.UserRepository
import org.mobilenativefoundation.trails.data.session.SampleAccounts
import org.mobilenativefoundation.trails.data.session.model.LoggedOutUser
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

    var networkExpanded by remember { mutableStateOf(true) }
    var conflictExpanded by remember { mutableStateOf(true) }
    var seedExpanded by remember { mutableStateOf(false) }

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
    val statusColor = when (statusLabel) {
        "Failed" -> DRAWER_DANGER_COLOR
        "Applied" -> DRAWER_SUCCESS_COLOR
        else -> DRAWER_MUTED_TEXT_COLOR
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

    val actionUnavailableHint = "Runtime controls unavailable."

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        gesturesEnabled = true,
        scrimColor = DRAWER_SCRIM_COLOR,
        drawerContent = {
            val spacing = TrailsTheme.spacing
            val typography = TrailsTheme.typography
            val sectionShape = RoundedCornerShape(18.dp)

            ModalDrawerSheet(
                // The stateful overload owns native Back while open. The drawer is composed
                // after app content, so its handler takes precedence over the underlying route.
                drawerState = drawerState,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 360.dp)
                    .focusRequester(drawerFocusRequester)
                    .onPreviewKeyEvent { event ->
                        if (!drawerState.isOpen || event.type != KeyEventType.KeyDown) {
                            return@onPreviewKeyEvent false
                        }
                        when {
                            event.key == Key.Escape -> {
                                scope.launch { drawerState.close() }
                                true
                            }
                            else -> false
                        }
                    }
                    .focusable(enabled = drawerState.isOpen),
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                drawerContainerColor = DRAWER_GLASS_COLOR,
                drawerContentColor = DRAWER_TEXT_COLOR,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(DRAWER_OVERLAY_BLUE, Color.Transparent),
                                    center = Offset(120f, 80f),
                                    radius = 900f,
                                ),
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(DRAWER_OVERLAY_MINT, Color.Transparent),
                                    center = Offset(120f, 700f),
                                    radius = 760f,
                                ),
                            ),
                    )
                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.md, vertical = spacing.md),
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .size(width = 54.dp, height = 5.dp)
                                    .background(DRAWER_GRAB_COLOR, RoundedCornerShape(999.dp)),
                            )

                            Spacer(modifier = Modifier.height(spacing.sm))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = "Developer Tools",
                                        style = typography.titleLarge,
                                        color = DRAWER_TEXT_COLOR,
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .border(
                                                    width = 1.dp,
                                                    color = DRAWER_STROKE_COLOR,
                                                    shape = RoundedCornerShape(999.dp),
                                                )
                                                .background(
                                                    color = DRAWER_GLASS_SURFACE_COLOR,
                                                    shape = RoundedCornerShape(999.dp),
                                                )
                                                .padding(horizontal = spacing.sm, vertical = spacing.xs),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(color = statusColor, shape = RoundedCornerShape(999.dp)),
                                            )
                                            Text(
                                                text = statusLabel,
                                                style = typography.labelSmall,
                                                color = DRAWER_PILL_TEXT_COLOR,
                                            )
                                        }
                                        Text(
                                            text = "Local device settings",
                                            style = typography.bodySmall,
                                            color = DRAWER_MUTED_TEXT_COLOR,
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { scope.launch { drawerState.close() } },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .border(
                                            width = 1.dp,
                                            color = DRAWER_STROKE_COLOR,
                                            shape = RoundedCornerShape(14.dp),
                                        )
                                        .background(
                                            color = DRAWER_GLASS_SURFACE_COLOR,
                                            shape = RoundedCornerShape(14.dp),
                                        ),
                                ) {
                                    Icon(
                                        painter = Icons.Outlined.Cancel.painter,
                                        contentDescription = "Close developer tools",
                                        tint = DRAWER_ICON_COLOR,
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = DRAWER_DIVIDER_COLOR)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = spacing.sm, vertical = spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) {
                            if (activityActions.available) {
                                Text("Activity refresh", style = typography.titleSmall, color = DRAWER_TEXT_COLOR)
                                TextButton(onClick = activityActions::retry) {
                                    Text("Try again", color = DRAWER_TEXT_COLOR)
                                }
                            }
                            if (userRepository != null) {
                                Text("Sample account", style = typography.titleSmall, color = DRAWER_TEXT_COLOR)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = {
                                        launchOperation("Switch to Alex") { userRepository.persist(SampleAccounts.primary) }
                                    }) { Text("Alex", color = DRAWER_TEXT_COLOR) }
                                    TextButton(onClick = {
                                        launchOperation("Switch to Robin") { userRepository.persist(SampleAccounts.secondary) }
                                    }) { Text("Robin", color = DRAWER_TEXT_COLOR) }
                                    TextButton(onClick = {
                                        launchOperation("Sign out") { userRepository.persist(LoggedOutUser) }
                                    }) { Text("Sign out", color = DRAWER_TEXT_COLOR) }
                                }
                                Text("Pending saves stay with their account and resume when it returns.", style = typography.bodySmall, color = DRAWER_MUTED_TEXT_COLOR)
                            }
                            if (!controlsEnabled) {
                                Text(
                                    text = actionUnavailableHint,
                                    style = typography.bodySmall,
                                    color = DRAWER_HINT_TEXT_COLOR,
                                    modifier = Modifier.padding(horizontal = spacing.sm),
                                )
                            }

                            DrawerSectionCard(
                                title = "Network",
                                subtitle = "Latency, errors, offline",
                                icon = Icons.Outlined.FastWind,
                                expanded = networkExpanded,
                                onExpandedChange = { networkExpanded = it },
                                sectionShape = sectionShape,
                            ) {
                                ToggleRow(
                                    label = "Offline mode",
                                    hint = "Forces all requests to fail fast (useful for caching + retry flows).",
                                    checked = settings.offlineMode,
                                    enabled = controlsEnabled,
                                    onCheckedChange = { enabled ->
                                        launchOperation("Save settings", restoreDraftsOnFailure = true) { developerSettingsRepository.setOfflineMode(enabled) }
                                    },
                                )
                                SliderRow(
                                    label = "Latency min",
                                    valueText = "${latencyMinDraft.roundToInt()} ms",
                                    enabled = controlsEnabled,
                                ) {
                                    ModernSlider(
                                        label = "Latency min",
                                        valueDescription = "${latencyMinDraft.roundToInt()} milliseconds",
                                        value = latencyMinDraft,
                                        enabled = controlsEnabled,
                                        onValueChange = { value ->
                                            val snapped = snapToStep(value, LATENCY_STEP, LATENCY_RANGE_MIN, LATENCY_RANGE_MAX)
                                            latencyMinDraft = snapped
                                            if (latencyMaxDraft < snapped) latencyMaxDraft = snapped
                                        },
                                        valueRange = LATENCY_RANGE_MIN..LATENCY_RANGE_MAX,
                                        tone = SliderTone.ACCENT,
                                        onValueChangeFinished = {
                                            launchOperation("Save settings", restoreDraftsOnFailure = true) {
                                                developerSettingsRepository.setLatencyRange(
                                                    minMs = latencyMinDraft.roundToInt().toLong(),
                                                    maxMs = latencyMaxDraft.roundToInt().toLong(),
                                                )
                                            }
                                        },
                                    )
                                }
                                SliderRow(
                                    label = "Latency max",
                                    valueText = "${latencyMaxDraft.roundToInt()} ms",
                                    enabled = controlsEnabled,
                                ) {
                                    ModernSlider(
                                        label = "Latency max",
                                        valueDescription = "${latencyMaxDraft.roundToInt()} milliseconds",
                                        value = latencyMaxDraft,
                                        enabled = controlsEnabled,
                                        onValueChange = { value ->
                                            val snapped = snapToStep(value, LATENCY_STEP, LATENCY_RANGE_MIN, LATENCY_RANGE_MAX)
                                            latencyMaxDraft = snapped
                                            if (latencyMinDraft > snapped) latencyMinDraft = snapped
                                        },
                                        valueRange = LATENCY_RANGE_MIN..LATENCY_RANGE_MAX,
                                        tone = SliderTone.ACCENT,
                                        onValueChangeFinished = {
                                            launchOperation("Save settings", restoreDraftsOnFailure = true) {
                                                developerSettingsRepository.setLatencyRange(
                                                    minMs = latencyMinDraft.roundToInt().toLong(),
                                                    maxMs = latencyMaxDraft.roundToInt().toLong(),
                                                )
                                            }
                                        },
                                    )
                                }
                                SliderRow(
                                    label = "Error rate",
                                    valueText = toDecimalRateText(errorRateDraft),
                                    enabled = controlsEnabled,
                                ) {
                                    ModernSlider(
                                        label = "Error rate",
                                        valueDescription = "${errorRateDraft.roundToInt()} percent",
                                        value = errorRateDraft,
                                        enabled = controlsEnabled,
                                        onValueChange = { value ->
                                            errorRateDraft = snapToStep(value, PERCENT_STEP, PERCENT_RANGE_MIN, PERCENT_RANGE_MAX)
                                        },
                                        valueRange = PERCENT_RANGE_MIN..PERCENT_RANGE_MAX,
                                        tone = SliderTone.DANGER,
                                        onValueChangeFinished = {
                                            launchOperation("Save settings", restoreDraftsOnFailure = true) {
                                                developerSettingsRepository.setErrorRate((errorRateDraft / 100f).coerceIn(0f, 1f))
                                            }
                                        },
                                    )
                                }
                                SliderRow(
                                    label = "Rate limit",
                                    valueText = "${rateLimitDraft.roundToInt()}",
                                    enabled = controlsEnabled,
                                ) {
                                    ModernSlider(
                                        label = "Rate limit",
                                        valueDescription = if (rateLimitDraft.roundToInt() == 0) "No rate limit" else "${rateLimitDraft.roundToInt()} requests per minute",
                                        value = rateLimitDraft,
                                        enabled = controlsEnabled,
                                        onValueChange = { value ->
                                            rateLimitDraft = snapToStep(value, RATE_LIMIT_STEP, RATE_LIMIT_RANGE_MIN, RATE_LIMIT_RANGE_MAX)
                                        },
                                        valueRange = RATE_LIMIT_RANGE_MIN..RATE_LIMIT_RANGE_MAX,
                                        tone = SliderTone.SUCCESS,
                                        onValueChangeFinished = {
                                            launchOperation("Save settings", restoreDraftsOnFailure = true) { developerSettingsRepository.setRateLimitPerMinute(rateLimitDraft.roundToInt()) }
                                        },
                                    )
                                }
                            }

                            DrawerSectionCard(
                                title = "Conflict",
                                subtitle = "Sync + merge simulation",
                                icon = Icons.Outlined.Alert,
                                expanded = conflictExpanded,
                                onExpandedChange = { conflictExpanded = it },
                                sectionShape = sectionShape,
                            ) {
                                ToggleRow(
                                    label = "Conflicts enabled",
                                    hint = "Randomly introduces divergent states between client & server.",
                                    checked = settings.conflictsEnabled,
                                    enabled = controlsEnabled,
                                    onCheckedChange = { enabled ->
                                        launchOperation("Save settings", restoreDraftsOnFailure = true) { developerSettingsRepository.setConflictsEnabled(enabled) }
                                    },
                                )
                                SelectRow(
                                    label = "Backend conflict mode",
                                    hint = "How the backend responds when it detects a conflict.",
                                    selected = settings.backendConflictMode,
                                    options = BackendConflictMode.entries.toList(),
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
                                        launchOperation("Save settings", restoreDraftsOnFailure = true) { developerSettingsRepository.setBackendConflictMode(mode) }
                                    },
                                )
                                SliderRow(
                                    label = "Conflict probability",
                                    valueText = toDecimalRateText(conflictProbabilityDraft),
                                    enabled = controlsEnabled,
                                ) {
                                    ModernSlider(
                                        label = "Conflict probability",
                                        valueDescription = "${conflictProbabilityDraft.roundToInt()} percent",
                                        value = conflictProbabilityDraft,
                                        enabled = controlsEnabled,
                                        onValueChange = { value ->
                                            conflictProbabilityDraft =
                                                snapToStep(value, PERCENT_STEP, PERCENT_RANGE_MIN, PERCENT_RANGE_MAX)
                                        },
                                        valueRange = PERCENT_RANGE_MIN..PERCENT_RANGE_MAX,
                                        tone = SliderTone.ACCENT,
                                        onValueChangeFinished = {
                                            launchOperation("Save settings", restoreDraftsOnFailure = true) {
                                                developerSettingsRepository.setConflictProbability(
                                                    (conflictProbabilityDraft / 100f).coerceIn(0f, 1f),
                                                )
                                            }
                                        },
                                    )
                                }
                            }

                            DrawerSectionCard(
                                title = "Seed",
                                subtitle = "Deterministic simulations",
                                icon = Icons.Solid.Sparkles,
                                expanded = seedExpanded,
                                onExpandedChange = { seedExpanded = it },
                                sectionShape = sectionShape,
                            ) {
                                SelectRow(
                                    label = "Simulation seed",
                                    hint = "Locks randomness for consistent repro steps across sessions.",
                                    selected = settings.simulationSeedPreset,
                                    options = BackendSimulationSeedPreset.entries.toList(),
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
                                        launchOperation("Save settings", restoreDraftsOnFailure = true) { developerSettingsRepository.setSimulationSeedPreset(preset) }
                                    },
                                )

                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            DRAWER_FOOTER_GRADIENT_TOP,
                                            DRAWER_FOOTER_GRADIENT_BOTTOM,
                                        ),
                                    ),
                                )
                                .padding(spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(spacing.md),
                        ) {
                            HorizontalDivider(color = DRAWER_DIVIDER_COLOR)
                            actionError?.let { message ->
                                Text(text = message, style = typography.bodySmall, color = DRAWER_DANGER_COLOR)
                            }
                            applicationError?.let { message ->
                                Text(text = message, style = typography.bodySmall, color = DRAWER_DANGER_COLOR)
                                OutlinedButton(onClick = onRetryBackendConfig) {
                                    Text("Retry runtime apply")
                                }
                            }
                            Text(
                                text = "Reset restores saved defaults. Esc closes this drawer.",
                                style = typography.bodySmall,
                                color = DRAWER_HINT_TEXT_COLOR,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                OutlinedButton(
                                    onClick = {
                                        launchOperation("Reset settings", restoreDraftsOnFailure = true) {
                                            developerSettingsRepository.resetToDefaults()
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = DRAWER_GLASS_SURFACE_COLOR,
                                        contentColor = DRAWER_TEXT_COLOR,
                                        disabledContainerColor = DRAWER_GLASS_SURFACE_COLOR.copy(alpha = 0.4f),
                                        disabledContentColor = DRAWER_MUTED_TEXT_COLOR,
                                    ),
                                    border = BorderStroke(1.dp, DRAWER_STROKE_STRONG_COLOR),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Reset")
                                }
                                Button(
                                    onClick = {
                                        val payload = settings.toConfigJson()
                                        clipboardManager.setText(AnnotatedString(payload))
                                        scope.launch {
                                            copyButtonLabel = "Copied ✓"
                                            delay(1100)
                                            copyButtonLabel = "Copy config"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = DRAWER_TEXT_COLOR,
                                        disabledContainerColor = Color.Transparent,
                                        disabledContentColor = DRAWER_MUTED_TEXT_COLOR,
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    DRAWER_ACCENT_COLOR.copy(alpha = 0.90f),
                                                    DRAWER_ACCENT_COLOR.copy(alpha = 0.50f),
                                                ),
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = DRAWER_ACCENT_COLOR.copy(alpha = 0.38f),
                                            shape = RoundedCornerShape(16.dp),
                                        ),
                                ) {
                                    Text(copyButtonLabel, color = DRAWER_TEXT_COLOR)
                                }
                            }
                        }
                    }
                }
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

@Composable
private fun DrawerSectionCard(
    title: String,
    subtitle: String,
    icon: org.mobilenativefoundation.trails.foundation.designsystem.icon.Icon,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    sectionShape: RoundedCornerShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = TrailsTheme.spacing
    val typography = TrailsTheme.typography
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DRAWER_SECTION_BACKGROUND_COLOR, sectionShape)
            .border(1.dp, DRAWER_SECTION_BORDER_COLOR, sectionShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DRAWER_SECTION_HEADER_TOP,
                            DRAWER_SECTION_HEADER_BOTTOM,
                        ),
                    ),
                )
                .padding(horizontal = spacing.sm, vertical = spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(DRAWER_SECTION_ICON_BG, RoundedCornerShape(14.dp))
                        .border(1.dp, DRAWER_STROKE_COLOR, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = icon.painter,
                        contentDescription = icon.contentDescription,
                        tint = DRAWER_ICON_COLOR,
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = typography.titleSmall,
                        color = DRAWER_TEXT_COLOR,
                    )
                    Text(
                        text = subtitle,
                        style = typography.bodySmall,
                        color = DRAWER_SUBTITLE_COLOR,
                    )
                }
            }

            IconButton(
                onClick = { onExpandedChange(!expanded) },
                modifier = Modifier
                    .size(28.dp)
                    .background(DRAWER_SECTION_ICON_BG, RoundedCornerShape(12.dp))
                    .border(1.dp, DRAWER_STROKE_COLOR, RoundedCornerShape(12.dp)),
            ) {
                Icon(
                    painter = Icons.Outlined.ArrowUp.painter,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotation),
                    tint = DRAWER_ICON_MUTED_COLOR,
                )
            }
        }
        HorizontalDivider(color = DRAWER_DIVIDER_COLOR)

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                content = content,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    hint: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val spacing = TrailsTheme.spacing
    val typography = TrailsTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = typography.bodyMedium, color = DRAWER_TEXT_COLOR)
            Text(
                text = hint,
                style = typography.bodySmall,
                color = DRAWER_HINT_TEXT_COLOR,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            // Text stays on the native actionable node; Compose can expose contentDescription
            // as a separate child for a merging control that has internal semantics children.
            modifier = Modifier.semantics { text = AnnotatedString(label) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = DRAWER_SWITCH_CHECKED_THUMB,
                checkedTrackColor = DRAWER_SWITCH_CHECKED_TRACK,
                checkedBorderColor = DRAWER_SWITCH_CHECKED_BORDER,
                uncheckedThumbColor = DRAWER_SWITCH_UNCHECKED_THUMB,
                uncheckedTrackColor = DRAWER_SWITCH_UNCHECKED_TRACK,
                uncheckedBorderColor = DRAWER_SWITCH_UNCHECKED_BORDER,
                disabledCheckedThumbColor = DRAWER_SWITCH_CHECKED_THUMB.copy(alpha = 0.55f),
                disabledCheckedTrackColor = DRAWER_SWITCH_CHECKED_TRACK.copy(alpha = 0.45f),
                disabledCheckedBorderColor = DRAWER_SWITCH_CHECKED_BORDER.copy(alpha = 0.4f),
                disabledUncheckedThumbColor = DRAWER_SWITCH_UNCHECKED_THUMB.copy(alpha = 0.45f),
                disabledUncheckedTrackColor = DRAWER_SWITCH_UNCHECKED_TRACK.copy(alpha = 0.4f),
                disabledUncheckedBorderColor = DRAWER_SWITCH_UNCHECKED_BORDER.copy(alpha = 0.35f),
            ),
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val spacing = TrailsTheme.spacing
    val typography = TrailsTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = typography.bodyMedium, color = DRAWER_TEXT_COLOR)
            Text(
                text = valueText,
                style = typography.titleMedium,
                color = DRAWER_ACCENT2_COLOR,
            )
        }
        if (enabled) {
            content()
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernSlider(
    label: String,
    valueDescription: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    tone: SliderTone = SliderTone.ACCENT,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val thumbColor = if (enabled) DRAWER_SLIDER_THUMB_COLOR else DRAWER_SLIDER_THUMB_COLOR.copy(alpha = 0.45f)
    val inactiveTrackColor = if (enabled) DRAWER_SLIDER_INACTIVE_TRACK else DRAWER_SLIDER_INACTIVE_TRACK.copy(alpha = 0.55f)
    val trackBorderColor = if (enabled) DRAWER_SLIDER_TRACK_BORDER else DRAWER_SLIDER_TRACK_BORDER.copy(alpha = 0.45f)
    val activeBrush =
        when (tone) {
            SliderTone.ACCENT -> Brush.horizontalGradient(listOf(DRAWER_ACCENT_COLOR.copy(alpha = 0.95f), DRAWER_ACCENT2_COLOR.copy(alpha = 0.75f)))
            SliderTone.SUCCESS -> Brush.horizontalGradient(listOf(DRAWER_SUCCESS_COLOR.copy(alpha = 0.95f), DRAWER_SUCCESS_COLOR.copy(alpha = 0.55f)))
            SliderTone.DANGER -> Brush.horizontalGradient(listOf(DRAWER_DANGER_COLOR.copy(alpha = 0.95f), DRAWER_DANGER_COLOR.copy(alpha = 0.60f)))
        }

    Slider(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier.semantics {
            text = AnnotatedString(label)
            stateDescription = valueDescription
        },
        valueRange = valueRange,
        steps = 0,
        onValueChangeFinished = onValueChangeFinished,
        thumb = {
            Box(
                modifier = Modifier
                    .size(MODERN_SLIDER_THUMB_SIZE)
                    .background(color = thumbColor, shape = CircleShape)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = if (enabled) 0.28f else 0.12f),
                        shape = CircleShape,
                    ),
            )
        },
        track = { sliderState ->
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MODERN_SLIDER_TRACK_HEIGHT),
            ) {
                val cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                drawRoundRect(
                    color = inactiveTrackColor,
                    cornerRadius = cornerRadius,
                )
                drawRoundRect(
                    color = trackBorderColor,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 1.dp.toPx()),
                )

                val activeWidth = (size.width * sliderState.coercedValueAsFraction).coerceIn(0f, size.width)
                if (activeWidth > 0f) {
                    drawRoundRect(
                        brush = activeBrush,
                        size = Size(activeWidth, size.height),
                        cornerRadius = cornerRadius,
                    )
                }
            }
        },
    )
}

@Composable
private fun <T : Enum<T>> SelectRow(
    label: String,
    hint: String,
    selected: T,
    options: List<T>,
    enabled: Boolean,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
) {
    val spacing = TrailsTheme.spacing
    val typography = TrailsTheme.typography
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = typography.bodyMedium, color = DRAWER_TEXT_COLOR)
            Text(
                text = hint,
                style = typography.bodySmall,
                color = DRAWER_HINT_TEXT_COLOR,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = DRAWER_GLASS_SURFACE_COLOR,
                    contentColor = DRAWER_TEXT_COLOR,
                    disabledContainerColor = DRAWER_GLASS_SURFACE_COLOR.copy(alpha = 0.4f),
                    disabledContentColor = DRAWER_MUTED_TEXT_COLOR,
                ),
                border = BorderStroke(1.dp, DRAWER_STROKE_STRONG_COLOR),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(optionLabel(selected))
            }
            DropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false },
                containerColor = DRAWER_MENU_BG_COLOR,
                modifier = Modifier.border(1.dp, DRAWER_STROKE_COLOR, RoundedCornerShape(14.dp)),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        colors = MenuDefaults.itemColors(
                            textColor = DRAWER_TEXT_COLOR,
                            disabledTextColor = DRAWER_MUTED_TEXT_COLOR,
                        ),
                        text = { Text(optionLabel(option), color = DRAWER_TEXT_COLOR) },
                        onClick = {
                            expanded = false
                            onSelected(option)
                        },
                    )
                }
            }
        }
    }
}

private fun snapToStep(
    value: Float,
    step: Float,
    min: Float,
    max: Float,
): Float {
    val clamped = value.coerceIn(min, max)
    val snappedSteps = ((clamped - min) / step).roundToInt()
    return (min + snappedSteps * step).coerceIn(min, max)
}

private fun toDecimalRateText(percentDraft: Float): String {
    val normalized = (percentDraft / 100f).coerceIn(0f, 1f)
    val scaled = (normalized * 100).roundToInt()
    val whole = scaled / 100
    val fractional = (scaled % 100).toString().padStart(2, '0')
    return "$whole.$fractional"
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

private const val LATENCY_RANGE_MIN = 0f
private const val LATENCY_RANGE_MAX = 5000f
private const val LATENCY_STEP = 10f

private const val PERCENT_RANGE_MIN = 0f
private const val PERCENT_RANGE_MAX = 100f
private const val PERCENT_STEP = 1f

private const val RATE_LIMIT_RANGE_MIN = 0f
private const val RATE_LIMIT_RANGE_MAX = 120f
private const val RATE_LIMIT_STEP = 1f

private enum class SliderTone {
    ACCENT,
    SUCCESS,
    DANGER,
}

private val DRAWER_SCRIM_COLOR = Color.Black.copy(alpha = 0.55f)
private val DRAWER_GLASS_COLOR = Color(0xB80A1224)
private val DRAWER_GLASS_SURFACE_COLOR = Color.White.copy(alpha = 0.06f)

private val DRAWER_OVERLAY_BLUE = Color(0x294D8DFF)
private val DRAWER_OVERLAY_MINT = Color(0x1A39E0B2)

private val DRAWER_TEXT_COLOR = Color.White.copy(alpha = 0.92f)
private val DRAWER_MUTED_TEXT_COLOR = Color.White.copy(alpha = 0.62f)
private val DRAWER_SUBTITLE_COLOR = Color.White.copy(alpha = 0.56f)
private val DRAWER_HINT_TEXT_COLOR = Color.White.copy(alpha = 0.55f)
private val DRAWER_PILL_TEXT_COLOR = Color.White.copy(alpha = 0.75f)

private val DRAWER_ICON_COLOR = Color.White.copy(alpha = 0.90f)
private val DRAWER_ICON_MUTED_COLOR = Color.White.copy(alpha = 0.80f)
private val DRAWER_GRAB_COLOR = Color.White.copy(alpha = 0.18f)

private val DRAWER_STROKE_COLOR = Color.White.copy(alpha = 0.10f)
private val DRAWER_STROKE_STRONG_COLOR = Color.White.copy(alpha = 0.12f)
private val DRAWER_DIVIDER_COLOR = Color.White.copy(alpha = 0.08f)

private val DRAWER_SECTION_BACKGROUND_COLOR = Color.White.copy(alpha = 0.04f)
private val DRAWER_SECTION_BORDER_COLOR = Color.White.copy(alpha = 0.08f)
private val DRAWER_SECTION_HEADER_TOP = Color.White.copy(alpha = 0.05f)
private val DRAWER_SECTION_HEADER_BOTTOM = Color.White.copy(alpha = 0.03f)
private val DRAWER_SECTION_ICON_BG = Color.White.copy(alpha = 0.06f)

private val DRAWER_ACCENT_COLOR = Color(0xFF4D8DFF)
private val DRAWER_ACCENT2_COLOR = Color(0xFF6AA8FF)
private val DRAWER_SUCCESS_COLOR = Color(0xFF39E0B2)
private val DRAWER_DANGER_COLOR = Color(0xFFFF6B7A)

private val DRAWER_SWITCH_CHECKED_TRACK = DRAWER_ACCENT_COLOR.copy(alpha = 0.65f)
private val DRAWER_SWITCH_CHECKED_THUMB = Color.White.copy(alpha = 0.92f)
private val DRAWER_SWITCH_CHECKED_BORDER = DRAWER_ACCENT_COLOR.copy(alpha = 0.55f)
private val DRAWER_SWITCH_UNCHECKED_TRACK = Color.White.copy(alpha = 0.06f)
private val DRAWER_SWITCH_UNCHECKED_THUMB = Color.White.copy(alpha = 0.82f)
private val DRAWER_SWITCH_UNCHECKED_BORDER = Color.White.copy(alpha = 0.14f)

private val MODERN_SLIDER_TRACK_HEIGHT: Dp = 8.dp
private val MODERN_SLIDER_THUMB_SIZE: Dp = 22.dp
private val DRAWER_SLIDER_THUMB_COLOR = Color.White.copy(alpha = 0.92f)
private val DRAWER_SLIDER_INACTIVE_TRACK = Color.White.copy(alpha = 0.10f)
private val DRAWER_SLIDER_TRACK_BORDER = Color.White.copy(alpha = 0.10f)

private val DRAWER_SEGMENT_SELECTED_BG = Color.White.copy(alpha = 0.10f)
private val DRAWER_MENU_BG_COLOR = Color(0xF20A1224)

private val DRAWER_FOOTER_GRADIENT_TOP = Color(0x000A1224)
private val DRAWER_FOOTER_GRADIENT_BOTTOM = Color(0x730A1224)

private val EDGE_SWIPE_HITBOX_WIDTH = 28.dp
private val EDGE_SWIPE_OPEN_THRESHOLD = 14.dp
