package org.mobilenativefoundation.trails.screen.activity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.TimeZone
import org.mobilenativefoundation.trails.data.trail.activity.CompletedActivity

class ActivitySummaryTest {
    private val zone = TimeZone.UTC
    private val now = 1_789_905_600_000L // 2026-09-20T12:00:00Z
    private val day = 86_400_000L
    private fun activity(id: String, daysAgo: Int, meters: Int, minutes: Int) =
        CompletedActivity(id, id, id, now - daysAgo * day, meters, minutes, 100)

    @Test
    fun monthTotalsAndBarsFollowTheCalendarMonthAndTheLastSevenDays() {
        val summary = monthSummary(listOf(activity("a", 1, 22_700, 660), activity("b", 3, 7_600, 180), activity("c", 40, 9_000, 200)), now, zone)
        assertEquals("September so far", summary.monthLabel)
        assertEquals(30_300, summary.distanceMeters)
        assertEquals(2, summary.trails)
        assertEquals(840, summary.minutesOutside)
        assertEquals(listOf(0, 0, 0, 7_600, 0, 22_700, 0), summary.lastSevenDaysMeters)
    }

    @Test
    fun dateLabelsAndSummaryLines() {
        assertEquals("Today", activityDateLabel(now - 3_600_000L, now, zone))
        assertEquals("Yesterday", activityDateLabel(now - day, now, zone))
        assertEquals("Sep 17", activityDateLabel(now - 3 * day, now, zone))
        assertEquals("Aug 11", activityDateLabel(now - 40 * day, now, zone))
        assertEquals("Yesterday · 22.7 km · 11 h 0 min", activitySummaryLine(activity("a", 1, 22_700, 660), now, zone))
        assertEquals("30.3", kilometres(30_300))
    }
}
