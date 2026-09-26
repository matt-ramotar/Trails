package org.mobilenativefoundation.trails.data.trail.recommendation

import kotlinx.serialization.Serializable

/** Per-account recommendation fixture: one featured trail and rows "more like" an anchor trail. */
@Serializable
data class ForYouFeed(
    val featuredTrailId: String,
    val headline: String,
    val subline: String,
    val anchorTrailId: String,
    val recommendedTrailIds: List<String>,
)
