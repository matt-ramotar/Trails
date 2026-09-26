package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo

/** Ephemeral identity of one actual sheet invoker. Never store this in navigation checkpoints. */
@Stable
class TrailsFocusReturnTarget internal constructor(internal val requester: FocusRequester) {
    internal var pending by mutableStateOf(false)
    internal var active = false
    // Native buttons use SystemDefined focusability, which excludes touch mode. Override that
    // property on the same target so a programmatic return is allowed without another focus node.
    val modifier: Modifier get() = Modifier.focusRequester(requester).focusProperties { canFocus = true }

    /** Called after a modal request is removed; native window focus determines when to restore. */
    fun restore() { if (active) pending = true }
}

@Composable
fun rememberTrailsFocusReturnTarget(): TrailsFocusReturnTarget {
    val target = remember { TrailsFocusReturnTarget(FocusRequester()) }
    val window = LocalWindowInfo.current
    val focusManager = LocalFocusManager.current
    DisposableEffect(target) {
        target.active = true
        onDispose { target.active = false; target.pending = false }
    }
    LaunchedEffect(target.pending, window.isWindowFocused) {
        if (target.pending && window.isWindowFocused) {
            // The sheet owns a separate window. Request only after the invoker's window is
            // focused again, and create an input-focus transition even if it retained focus.
            focusManager.clearFocus(force = true)
            try { target.requester.requestFocus() }
            catch (_: IllegalStateException) { /* The invoker may have left composition. */ }
            target.pending = false
        }
    }
    return target
}
