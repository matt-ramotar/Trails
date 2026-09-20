package org.mobilenativefoundation.trails.app.bootstrap

enum class SplashState {
    NOT_READY,
    READY;

    fun isReady(): Boolean = this == READY
}