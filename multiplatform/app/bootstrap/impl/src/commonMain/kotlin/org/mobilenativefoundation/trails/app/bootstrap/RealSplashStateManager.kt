package org.mobilenativefoundation.trails.app.bootstrap

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@OptIn(ExperimentalTime::class)
@Inject
class RealSplashStateManager : SplashStateManager {
    private val _state = MutableStateFlow(_root_ide_package_.org.mobilenativefoundation.trails.app.bootstrap.SplashState.NOT_READY)
    private val earliestEnd = Clock.System.now() + 300.milliseconds

    override val state: StateFlow<org.mobilenativefoundation.trails.app.bootstrap.SplashState> = _state.asStateFlow()

    override fun write(value: org.mobilenativefoundation.trails.app.bootstrap.SplashState) {
        if (value == _root_ide_package_.org.mobilenativefoundation.trails.app.bootstrap.SplashState.READY) {
            while (Clock.System.now() < earliestEnd) {
                // Loop
            }
        }
        _state.value = value
    }
}