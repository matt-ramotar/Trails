package org.mobilenativefoundation.trails.data.trail.catalog

import kotlinx.serialization.Serializable

@Serializable
data class TrailQuery(
    val text: String = "",
    val region: String? = null,
    val difficulties: Set<TrailDifficulty> = emptySet(),
    val minMeters: Int = 0,
    val maxMeters: Int? = null,
    val features: Set<TrailFeature> = emptySet(),
    val minElevationGain: Int = 0,
    val maxElevationGain: Int? = null,
    val dogFriendly: Boolean = false,
    val activities: Set<TrailActivity> = emptySet(),
    val sort: TrailSort = TrailSort.MOST_POPULAR,
) {
    fun normalized(): TrailQuery = copy(
        text = text.trim().replace(Regex("\\s+"), " ").lowercase(),
        region = region?.trim()?.lowercase()?.takeIf { it.isNotEmpty() },
        minMeters = minMeters.coerceAtLeast(0),
        maxMeters = maxMeters?.coerceAtLeast(minMeters.coerceAtLeast(0)),
        difficulties = difficulties.sortedBy { it.name }.toSet(),
        features = features.sortedBy { it.name }.toSet(),
        minElevationGain = minElevationGain.coerceAtLeast(0),
        maxElevationGain = maxElevationGain?.coerceAtLeast(minElevationGain.coerceAtLeast(0)),
        activities = activities.sortedBy { it.name }.toSet(),
    )
}
