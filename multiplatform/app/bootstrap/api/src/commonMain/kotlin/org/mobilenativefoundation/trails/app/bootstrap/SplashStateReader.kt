package org.mobilenativefoundation.trails.app.bootstrap

import kotlinx.coroutines.flow.StateFlow

interface SplashStateReader {
    val state: StateFlow<org.mobilenativefoundation.trails.app.bootstrap.SplashState>
}