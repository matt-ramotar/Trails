package org.mobilenativefoundation.trails.server.model

import kotlinx.serialization.Serializable

@Serializable
data class UserAuthResponse(
    val user: UserRecord,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
)

@Serializable
data class UserRecord(
    val id: String,
    val email: String,
    val profile: ProfileRecord?,
    val stats: UserStats,
    val createdAt: String,
    val version: Long,
)

@Serializable
data class ProfileRecord(
    val firstName: String,
    val lastName: String,
    val birthdate: String?,
    val displayName: String,
    val username: String,
    val avatarUrl: String?,
    val bio: String?,
    val location: String?,
    val verified: Boolean,
)

@Serializable
data class UserStats(
    val followers: Int,
    val following: Int,
    val posts: Int,
    val totalVerticalFeet: Int,
    val totalRuns: Int,
)

@Serializable
data class ProfileUpdate(
    val firstName: String? = null,
    val lastName: String? = null,
    val birthdate: String? = null,
    val displayName: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class UserSummary(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val verified: Boolean,
    val isFollowing: Boolean,
)
