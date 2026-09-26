package org.mobilenativefoundation.trails.server.services

import org.mobilenativefoundation.trails.server.model.ProfileUpdate
import org.mobilenativefoundation.trails.server.model.UserAuthResponse
import org.mobilenativefoundation.trails.server.model.UserRecord
import org.mobilenativefoundation.trails.server.model.UserSummary
import org.mobilenativefoundation.trails.server.pagination.CursorPage
import org.mobilenativefoundation.trails.server.pagination.CursorRequest

interface UserService {
    // Authentication
    suspend fun signup(email: String, password: String): UserAuthResponse
    suspend fun login(email: String, password: String): UserAuthResponse
    suspend fun logout(token: String)
    suspend fun refreshToken(refreshToken: String): UserAuthResponse

    // Profile
    suspend fun getUser(userId: String): UserRecord
    suspend fun getCurrentUser(token: String): UserRecord
    suspend fun updateProfile(token: String, update: ProfileUpdate): UserRecord

    // Social (viewer-aware)
    suspend fun followUser(token: String, targetUserId: String)
    suspend fun unfollowUser(token: String, targetUserId: String)
    suspend fun getFollowers(
        userId: String,
        request: CursorRequest,
        viewerToken: String? = null,
    ): CursorPage<UserSummary>
    suspend fun getFollowing(
        userId: String,
        request: CursorRequest,
        viewerToken: String? = null,
    ): CursorPage<UserSummary>

    // Search (viewer-aware)
    suspend fun searchUsers(
        query: String,
        limit: Int = 10,
        viewerToken: String? = null,
    ): List<UserSummary>
}
