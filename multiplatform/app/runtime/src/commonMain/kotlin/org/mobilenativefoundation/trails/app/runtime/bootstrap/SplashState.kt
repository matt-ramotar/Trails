package org.mobilenativefoundation.trails.app.runtime.bootstrap

internal enum class SplashState {
    NOT_READY,
    READY;

    fun isReady(): Boolean = this == READY
}
