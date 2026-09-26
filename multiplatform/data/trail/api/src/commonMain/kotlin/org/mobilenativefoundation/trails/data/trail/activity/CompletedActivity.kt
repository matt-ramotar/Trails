package org.mobilenativefoundation.trails.data.trail.activity

import kotlinx.serialization.Serializable

/** A completed hike. The sample backend seeds activities per account. */
@Serializable
data class CompletedActivity(
    val id: String,
    val trailId: String,
    val trailName: String,
    val completedAtEpochMillis: Long,
    val distanceMeters: Int,
    val durationMinutes: Int,
    val elevationMeters: Int,
)
