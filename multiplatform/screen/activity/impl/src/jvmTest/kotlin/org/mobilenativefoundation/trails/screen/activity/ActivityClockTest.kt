@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.mobilenativefoundation.trails.screen.activity

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.mobilenativefoundation.trails.data.trail.CompletedActivity

class ActivityClockTest {
    private val zone = TimeZone.of("Europe/Madrid")
    private val beforeMidnight = Instant.parse("2026-09-30T21:59:59Z")
    private val history = listOf(
        CompletedActivity("hike", "half-dome", "Half Dome", beforeMidnight.toEpochMilliseconds(), 22_700, 660, 1463),
    )

    @Test
    fun unchangedHistoryMovesIntoTheNextLocalDayAndMonth() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val owner = object : LifecycleOwner {
                override val lifecycle = LifecycleRegistry.createUnsafe(this)
            }
            val lifecycle = owner.lifecycle
            lifecycle.currentState = Lifecycle.State.RESUMED
            val clock = MutableClock(beforeMidnight)
            var now = 0L
            backgroundScope.launch { activityClock(lifecycle, clock) { zone }.collect { now = it } }
            runCurrent()
            assertEquals("September so far", monthSummary(history, now, zone).monthLabel)
            assertEquals(22_700, monthSummary(history, now, zone).distanceMeters)
            assertEquals("Today", activityDateLabel(history.single().completedAtEpochMillis, now, zone))

            clock.instant = Instant.parse("2026-09-30T22:00:00Z")
            advanceTimeBy(1_000)
            runCurrent()

            val summary = monthSummary(history, now, zone)
            assertEquals("October so far", summary.monthLabel)
            assertEquals(0, summary.distanceMeters)
            assertEquals(0, summary.trails)
            assertEquals(0, summary.minutesOutside)
            assertEquals(listOf(0, 0, 0, 0, 0, 22_700, 0), summary.lastSevenDaysMeters)
            assertEquals("Yesterday", activityDateLabel(history.single().completedAtEpochMillis, now, zone))
        } finally {
            backgroundScope.coroutineContext.cancelChildren()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun foregroundResumeResamplesTheClockWithoutAHistoryEmissionOrTimerAdvance() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val owner = object : LifecycleOwner {
                override val lifecycle = LifecycleRegistry.createUnsafe(this)
            }
            val lifecycle = owner.lifecycle
            lifecycle.currentState = Lifecycle.State.RESUMED
            val clock = MutableClock(beforeMidnight)
            var now = 0L
            backgroundScope.launch { activityClock(lifecycle, clock) { zone }.collect { now = it } }
            runCurrent()
            assertEquals(beforeMidnight.toEpochMilliseconds(), now)

            lifecycle.currentState = Lifecycle.State.CREATED
            runCurrent()
            clock.instant = Instant.parse("2026-10-02T10:00:00Z")
            lifecycle.currentState = Lifecycle.State.RESUMED
            runCurrent()

            assertEquals(clock.instant.toEpochMilliseconds(), now)
            assertEquals("October so far", monthSummary(history, now, zone).monthLabel)
            assertEquals("Sep 30", activityDateLabel(history.single().completedAtEpochMillis, now, zone))
        } finally {
            backgroundScope.coroutineContext.cancelChildren()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    private class MutableClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }
}
