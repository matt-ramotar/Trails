package org.mobilenativefoundation.trails.model.network.feed

import kotlinx.serialization.Serializable
import org.mobilenativefoundation.trails.model.domain.feed.BackgroundGradient
import org.mobilenativefoundation.trails.model.domain.feed.Emoji
import org.mobilenativefoundation.trails.model.domain.feed.TrailDifficulty

@Serializable
data class FeedPostRecord(
    val id: String,
    val author: FeedPostAuthorRecord,
    val run: FeedPostRunRecord,
    val resort: FeedPostResortRecord,
    val weather: FeedPostWeatherRecord,
    val engagement: FeedPostEngagementRecord,
    val style: FeedPostStyleRecord,
    val timestamp: String,
    val version: Long = 0,
)

@Serializable
data class FeedPostAuthorRecord(
    val id: String = "",
    val username: String,
    val displayName: String,
    val avatar: String,
    val verified: Boolean,
    val isFollowing: Boolean = false,
)

@Serializable
data class FeedPostRunRecord(
    val id: String = "",
    val resortId: String = "",
    val name: String,
    val distance: String,
    val vertical: String,
    val duration: String,
    val topSpeed: String,
    val difficulty: TrailDifficulty,
    val liftAccess: String,
)

@Serializable
data class FeedPostResortRecord(
    val id: String = "",
    val name: String,
    val location: String,
)

@Serializable
data class FeedPostWeatherRecord(
    val id: String = "",
    val resortId: String = "",
    val observedAt: String = "",
    val source: String = "",
    val conditions: String,
    val temperature: String,
    val temperatureF: Int? = null,
    val feelsLikeF: Int? = null,
    val windMph: Int? = null,
    val snowfall24hIn: Int? = null,
    val visibility: String = "",
    val summary: String = "",
)

@Serializable
data class FeedPostEngagementRecord(
    val views: Int,
    val likes: Int,
    val comments: Int,
    val shares: Int,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
)

@Serializable
data class FeedPostStyleRecord(
    val backgroundGradient: BackgroundGradient,
    val emoji: Emoji = Emoji.Skier,
)
