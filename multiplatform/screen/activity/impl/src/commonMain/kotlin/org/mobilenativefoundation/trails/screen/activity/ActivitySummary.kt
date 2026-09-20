@file:OptIn(kotlin.time.ExperimentalTime::class)

package org.mobilenativefoundation.trails.screen.activity

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.mobilenativefoundation.trails.data.trail.CompletedActivity
import org.mobilenativefoundation.trails.feat.savetrail.trailDistance
import org.mobilenativefoundation.trails.feat.savetrail.trailDuration

data class MonthSummary(val monthLabel: String, val distanceMeters: Int, val trails: Int, val minutesOutside: Int, val lastSevenDaysMeters: List<Int>)

internal fun localDate(epochMillis: Long, zone: TimeZone): LocalDate = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date

private fun monthName(date: LocalDate): String = date.month.name.lowercase().replaceFirstChar { it.uppercase() }

/** Totals for the calendar month of [nowEpochMillis]; the bars are the last seven local days ending today. */
fun monthSummary(activities: List<CompletedActivity>, nowEpochMillis: Long, zone: TimeZone): MonthSummary {
    val today = localDate(nowEpochMillis, zone)
    val dated = activities.map { it to localDate(it.completedAtEpochMillis, zone) }
    val thisMonth = dated.filter { (_, date) -> date.year == today.year && date.month == today.month }
    return MonthSummary(
        monthLabel = "${monthName(today)} so far",
        distanceMeters = thisMonth.sumOf { it.first.distanceMeters },
        trails = thisMonth.map { it.first.trailId }.distinct().size,
        minutesOutside = thisMonth.sumOf { it.first.durationMinutes },
        lastSevenDaysMeters = (6 downTo 0).map { back -> dated.filter { (_, date) -> date.daysUntil(today) == back }.sumOf { it.first.distanceMeters } },
    )
}

fun activityDateLabel(completedAtEpochMillis: Long, nowEpochMillis: Long, zone: TimeZone): String {
    val date = localDate(completedAtEpochMillis, zone)
    return when (date.daysUntil(localDate(nowEpochMillis, zone))) {
        0 -> "Today"
        1 -> "Yesterday"
        else -> "${monthName(date).take(3)} ${date.day}"
    }
}

fun activitySummaryLine(activity: CompletedActivity, nowEpochMillis: Long, zone: TimeZone): String =
    "${activityDateLabel(activity.completedAtEpochMillis, nowEpochMillis, zone)} · ${trailDistance(activity.distanceMeters)} · ${trailDuration(activity.durationMinutes)}"

/** Whole kilometres with one decimal, unit supplied by the caller's label. */
fun kilometres(meters: Int): String = "${meters / 1000}.${(meters % 1000) / 100}"
