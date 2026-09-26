package org.mobilenativefoundation.trails.screen.activity

import org.mobilenativefoundation.trails.feature.developertools.*
import org.mobilenativefoundation.trails.feature.developertools.LocalActivityDeveloperActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
internal fun RegisterActivityDeveloperRetry(send: (ActivityIntent) -> Unit) {
    val actions = LocalActivityDeveloperActions.current
    val currentSend by rememberUpdatedState(send)
    DisposableEffect(actions) {
        val unregister = actions?.register { currentSend(ActivityIntent.Retry) }
        onDispose { unregister?.invoke() }
    }
}
