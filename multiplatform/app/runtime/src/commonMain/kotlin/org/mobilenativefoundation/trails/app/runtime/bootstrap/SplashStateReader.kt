package org.mobilenativefoundation.trails.app.runtime.bootstrap

import kotlinx.coroutines.flow.StateFlow

internal interface SplashStateReader {
    val state: StateFlow<SplashState>
}
