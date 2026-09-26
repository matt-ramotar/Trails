package org.mobilenativefoundation.trails.data.trail.catalog

import kotlinx.serialization.Serializable

@Serializable
data class Trail(
    val id: String,
    val name: String,
    val region: String,
    val description: String,
    val difficulty: TrailDifficulty,
    val distanceMeters: Int,
    val elevationMeters: Int,
    val durationMinutes: Int,
    val rating: Double,
    val reviewCount: Int,
    val features: Set<TrailFeature>,
    val photoIndex: Int,
    val reviewExcerpt: String? = null,
    val activities: Set<TrailActivity> = emptySet(),
)
