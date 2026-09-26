package org.mobilenativefoundation.trails.ui.trail

import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.catalog.TrailFeature
import org.mobilenativefoundation.trails.foundation.designsystem.component.DifficultyMarkerKind

fun TrailDifficulty.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
fun trailDistance(meters: Int): String = "${meters / 1000}.${(meters % 1000) / 100} km"
fun trailDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes < 1440 -> "${minutes / 60} h ${minutes % 60} min"
    else -> {
        val days = minutes / 1440
        val hours = (minutes % 1440) / 60
        val dayLabel = "$days ${if (days == 1) "day" else "days"}"
        if (hours == 0) dayLabel else "$dayLabel $hours h"
    }
}

fun TrailDifficulty.markerKind(): DifficultyMarkerKind = when (this) {
    TrailDifficulty.EASY -> DifficultyMarkerKind.EASY
    TrailDifficulty.MODERATE -> DifficultyMarkerKind.MODERATE
    TrailDifficulty.HARD -> DifficultyMarkerKind.HARD
    TrailDifficulty.STRENUOUS -> DifficultyMarkerKind.STRENUOUS
}

/** Derived from the catalog's feature tags and duration; nothing is authored per route. */
internal fun Trail.highlight(): String? = when {
    durationMinutes >= 1440 -> "Multi-day"
    TrailFeature.WATERFALL in features -> "Waterfall"
    TrailFeature.SUMMIT in features -> "Summit views"
    TrailFeature.LAKE in features -> "Lakeside"
    TrailFeature.FOREST in features -> "Forest"
    else -> null
}
