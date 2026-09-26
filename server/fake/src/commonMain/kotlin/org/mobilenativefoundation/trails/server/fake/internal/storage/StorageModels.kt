package org.mobilenativefoundation.trails.server.fake.internal.storage

import org.mobilenativefoundation.trails.server.model.BackgroundGradientId
import org.mobilenativefoundation.trails.server.model.EmojiId
import org.mobilenativefoundation.trails.server.model.TrailDifficulty

internal data class StoredUser(
    val id: String,
    val email: String,
    val passwordHash: String,
    val firstName: String?,
    val lastName: String?,
    val birthdate: String?,
    val displayName: String,
    val username: String,
    val avatarUrl: String?,
    val bio: String?,
    val location: String?,
    val verified: Boolean,
    val createdAt: Long,
    val version: Long,
)

internal data class StoredSession(
    val token: String,
    val refreshToken: String,
    val userId: String,
    val expiresAt: Long,
)

internal data class StoredFollow(
    val followerId: String,
    val followeeId: String,
    val createdAt: Long,
)

internal data class StoredPost(
    val id: String,
    val authorId: String,
    val runId: String,
    val backgroundGradientId: BackgroundGradientId,
    val emojiId: EmojiId,
    val caption: String?,
    val createdAt: Long,
    val version: Long,
    val likes: Int,
    val comments: Int,
    val shares: Int,
    val views: Int,
)

internal data class StoredPostEngagement(
    val userId: String,
    val postId: String,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
)

internal data class StoredComment(
    val id: String,
    val postId: String,
    val authorId: String,
    val body: String,
    val createdAt: Long,
    val likes: Int,
)

internal data class StoredResort(
    val id: String,
    val name: String,
    val city: String,
    val state: String?,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Int,
    val runs: Int,
    val lifts: Int,
    val verticalFeet: Int,
    val skiableAcres: Int,
    val snowfallAnnualInches: Int,
    val amenities: List<String>,
    val isOpen: Boolean,
    val liftsOpen: Int,
    val runsOpen: Int,
    val imageUrl: String?,
    val version: Long,
)

internal data class StoredRun(
    val id: String,
    val resortId: String,
    val name: String,
    val difficulty: TrailDifficulty,
    val lengthMiles: Float,
    val verticalFeet: Int,
    val averageGradePct: Int,
    val maxGradePct: Int,
    val isOpen: Boolean,
    val conditions: String,
    val lastGroomed: String?,
    val liftAccess: List<String>,
    val features: List<String>,
    val version: Long,
)

internal data class StoredWeather(
    val resortId: String,
    val observedAt: Long,
    val conditions: String,
    val temperatureF: Int,
    val feelsLikeF: Int,
    val windMph: Int,
    val windDirection: String,
    val humidity: Int,
    val visibility: String,
    val uvIndex: Int,
    val newSnow24h: Int,
    val newSnow48h: Int,
    val newSnow7d: Int,
    val baseDepth: Int,
    val seasonTotal: Int,
)

internal data class StoredUserFavorite(
    val userId: String,
    val resortId: String,
    val createdAt: Long,
)
