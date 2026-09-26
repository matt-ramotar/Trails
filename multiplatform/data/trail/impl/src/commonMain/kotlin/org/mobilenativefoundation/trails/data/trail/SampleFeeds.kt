package org.mobilenativefoundation.trails.data.trail

private const val DAY_MILLIS = 86_400_000L

/** Sample history: catalog routes with their catalog distance, duration and gain, dated back from the first request. */
internal fun sampleActivities(nowEpochMillis: Long): List<CompletedActivity> = listOf(
    "half-dome" to 1,
    "mount-takao-trail-1" to 3,
    "bondi-to-coogee-coastal-walk" to 6,
    "diamond-head-summit-trail" to 9,
    "preikestolen" to 15,
    "lake-agnes-tea-house" to 24,
).map { (trailId, daysAgo) ->
    val trail = worldTrails.first { it.id == trailId }
    CompletedActivity(
        id = "sample-$trailId",
        trailId = trailId,
        trailName = trail.name,
        completedAtEpochMillis = nowEpochMillis - daysAgo * DAY_MILLIS,
        distanceMeters = trail.distanceMeters,
        durationMinutes = trail.durationMinutes,
        elevationMeters = trail.elevationMeters,
    )
}

/** Authored per-account recommendation fixture (DEV-37); nothing here is inferred from behaviour. */
internal val sampleForYou = ForYouFeed(
    featuredTrailId = "trolltunga",
    headline = "A weekend worth the walk",
    subline = "Discover a quieter side of outside.",
    anchorTrailId = "half-dome",
    recommendedTrailIds = listOf(
        "mist-trail-to-nevada-fall", "upper-yosemite-fall-trail", "angels-landing",
        "franconia-ridge-loop", "preikestolen", "ben-nevis-mountain-track",
    ),
)
