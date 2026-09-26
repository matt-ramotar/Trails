package org.mobilenativefoundation.trails.app.runtime.bootstrap

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
internal class RealSplashStateManager : SplashStateManager {
    private val _state = MutableStateFlow(SplashState.NOT_READY)
    private val earliestEnd = Clock.System.now() + 300.milliseconds

    override val state: StateFlow<SplashState> = _state.asStateFlow()

    override fun write(value: SplashState) {
        if (value == SplashState.READY) {
            while (Clock.System.now() < earliestEnd) {
            }
        }
        _state.value = value
    }
}
