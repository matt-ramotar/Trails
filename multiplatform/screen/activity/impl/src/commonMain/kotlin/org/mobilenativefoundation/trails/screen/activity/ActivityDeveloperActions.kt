package org.mobilenativefoundation.trails.screen.activity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Optional developer host bridge. Only the currently composed Activity UI registers an action. */
class ActivityDeveloperActions {
    private var retry by mutableStateOf<(() -> Unit)?>(null)
    val available: Boolean get() = retry != null

    fun retry() { retry?.invoke() }

    internal fun register(action: () -> Unit): () -> Unit {
        retry = action
        return { if (retry === action) retry = null }
    }
}

/** Absent in ordinary screen hosts; the app supplies it only for the current Activity route. */
val LocalActivityDeveloperActions = staticCompositionLocalOf<ActivityDeveloperActions?> { null }

@Composable
internal fun RegisterActivityDeveloperRetry(send: (ActivityIntent) -> Unit) {
    val actions = LocalActivityDeveloperActions.current
    val currentSend by rememberUpdatedState(send)
    DisposableEffect(actions) {
        val unregister = actions?.register { currentSend(ActivityIntent.Retry) }
        onDispose { unregister?.invoke() }
    }
}
