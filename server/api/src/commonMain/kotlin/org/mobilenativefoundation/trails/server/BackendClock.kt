package org.mobilenativefoundation.trails.server

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface BackendClock {
    fun nowMs(): Long
}

@OptIn(ExperimentalTime::class)
object SystemBackendClock : BackendClock {
    override fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()
}
