package org.mobilenativefoundation.trails.model.domain.feed

import kotlinx.serialization.Serializable

@Serializable
data class FeedPost(
    val id: String,
    val authorId: String = "",
    val username: String,
    val displayName: String,
    val avatar: String,
    val verified: Boolean,
    val runId: String = "",
    val runName: String,
    val resort: String,
    val resortId: String = "",
    val runResortId: String = "",
    val location: String,
    val weatherId: String = "",
    val weatherResortId: String = "",
    val weatherObservedAt: String = "",
    val weatherSource: String = "",
    val weatherTemperatureF: Int? = null,
    val weatherFeelsLikeF: Int? = null,
    val weatherWindMph: Int? = null,
    val weatherSnowfall24hIn: Int? = null,
    val weatherVisibility: String = "",
    val weatherSummary: String = "",
    val distance: String,
    val vertical: String,
    val duration: String,
    val topSpeed: String,
    val difficulty: TrailDifficulty,
    val views: Int,
    val likes: Int,
    val comments: Int,
    val shares: Int, // TODO: We need "bookmarks" not shares
    val conditions: String,
    val temperature: String,
    val backgroundGradient: BackgroundGradient,
    val timestamp: String,
    val liftAccess: String,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val isFollowing: Boolean = false,
    val emoji: Emoji = Emoji.Skier,
    val version: Long = 0,
)

@Serializable
enum class TrailDifficulty {
    GREEN_CIRCLE,
    BLUE_SQUARE,
    BLACK_DIAMOND,
    DOUBLE_BLACK_DIAMOND
}

@Serializable
enum class BackgroundGradient {
    CYAN_BLUE_INDIGO,
    BLUE_SLATE_GRAY,
    SKY_CYAN_BLUE,
    BLUE_INDIGO_PURPLE,
    SLATE_BLUE_CYAN,
    INDIGO_BLUE_CYAN
}

@Serializable
enum class Emoji {
    Skier,
    AerialTramway,
    SnowCappedMountain
}
