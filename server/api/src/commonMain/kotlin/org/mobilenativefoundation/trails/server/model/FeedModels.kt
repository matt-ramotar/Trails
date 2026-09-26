package org.mobilenativefoundation.trails.server.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface FeedItemRecord {
    val id: String
    val type: String
    val timestamp: String
}

@Serializable
data class SkiRunPostRecord(
    override val id: String,
    override val type: String = "ski_run_post",
    override val timestamp: String,
    val author: UserSummary,
    val run: RunSummary,
    val resort: ResortSummary,
    val weather: WeatherSummary,
    val engagement: EngagementRecord,
    val style: StyleRecord,
    val caption: String?,
    val version: Long,
) : FeedItemRecord

@Serializable
data class EngagementRecord(
    val views: Int,
    val likes: Int,
    val comments: Int,
    val shares: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
)

@Serializable
data class StyleRecord(
    val backgroundGradientId: BackgroundGradientId,
    val emojiId: EmojiId,
)

@Serializable
data class WeatherSummary(
    val conditions: String,
    val temperatureF: Int,
    val snowfall24hIn: Int,
)
