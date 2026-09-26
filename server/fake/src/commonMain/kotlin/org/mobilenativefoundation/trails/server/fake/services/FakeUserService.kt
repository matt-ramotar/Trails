package org.mobilenativefoundation.trails.server.fake.services

import org.mobilenativefoundation.trails.server.BackendClock
import org.mobilenativefoundation.trails.server.BackendConfigProvider
import org.mobilenativefoundation.trails.server.error.ServerError
import org.mobilenativefoundation.trails.server.fake.internal.seed.SeedDataLoader
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ConflictOutcome
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ConflictSimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ErrorSimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.LatencySimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.NetworkGate
import org.mobilenativefoundation.trails.server.fake.internal.storage.BackendTables
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredFollow
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredSession
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredUser
import org.mobilenativefoundation.trails.server.model.ProfileRecord
import org.mobilenativefoundation.trails.server.model.ProfileUpdate
import org.mobilenativefoundation.trails.server.model.UserAuthResponse
import org.mobilenativefoundation.trails.server.model.UserRecord
import org.mobilenativefoundation.trails.server.model.UserStats
import org.mobilenativefoundation.trails.server.model.UserSummary
import org.mobilenativefoundation.trails.server.pagination.CursorPage
import org.mobilenativefoundation.trails.server.pagination.CursorRequest
import org.mobilenativefoundation.trails.server.services.UserService

internal class FakeUserService(
    tables: BackendTables,
    networkGate: NetworkGate,
    latencySimulator: LatencySimulator,
    errorSimulator: ErrorSimulator,
    seedLoader: SeedDataLoader,
    configProvider: BackendConfigProvider,
    private val clock: BackendClock,
    private val conflictSimulator: ConflictSimulator,
) : BaseFakeService(tables, networkGate, latencySimulator, errorSimulator, seedLoader, configProvider), UserService {

    override suspend fun signup(email: String, password: String): UserAuthResponse = withSimulation {
        val users = tables.users()
        val existing = users.query { it.email == email }
        if (existing.isNotEmpty()) throw ServerError.BadRequest("Email already in use")

        val id = "user_${clock.nowMs()}"
        val username = email.substringBefore('@')
        val user = StoredUser(
            id = id,
            email = email,
            passwordHash = password,
            firstName = username.replaceFirstChar { it.uppercase() },
            lastName = null,
            birthdate = null,
            displayName = username.replaceFirstChar { it.uppercase() },
            username = username,
            avatarUrl = null,
            bio = null,
            location = null,
            verified = false,
            createdAt = clock.nowMs(),
            version = 1,
        )
        users.put(user)
        createSession(user)
    }

    override suspend fun login(email: String, password: String): UserAuthResponse = withSimulation {
        val users = tables.users()
        val user = users.query { it.email == email }.firstOrNull()
            ?: throw ServerError.Unauthorized()
        if (password != user.passwordHash && password != "password") {
            throw ServerError.Unauthorized()
        }
        createSession(user)
    }

    override suspend fun logout(token: String) = withSimulation {
        tables.sessions().remove(token)
        Unit
    }

    override suspend fun refreshToken(refreshToken: String): UserAuthResponse = withSimulation {
        val sessions = tables.sessions()
        val session = sessions.query { it.refreshToken == refreshToken }.firstOrNull()
            ?: throw ServerError.Unauthorized()
        val user = tables.users().get(session.userId) ?: throw ServerError.Unauthorized()
        sessions.remove(session.token)
        createSession(user)
    }

    override suspend fun getUser(userId: String): UserRecord = withSimulation {
        val user = tables.users().get(userId) ?: throw ServerError.NotFound("user", userId)
        toUserRecord(user)
    }

    override suspend fun getCurrentUser(token: String): UserRecord = withSimulation {
        val user = userForToken(token)
        toUserRecord(user)
    }

    override suspend fun updateProfile(token: String, update: ProfileUpdate): UserRecord = withSimulation {
        val user = userForToken(token)
        conflictSimulator.evaluate(
            resourceType = "user",
            id = user.id,
            clientVersion = user.version,
            serverVersion = user.version,
        ).let { outcome ->
            if (outcome is ConflictOutcome.Reject) throw outcome.error
        }
        val updated = user.copy(
            firstName = update.firstName ?: user.firstName,
            lastName = update.lastName ?: user.lastName,
            birthdate = update.birthdate ?: user.birthdate,
            displayName = update.displayName ?: user.displayName,
            bio = update.bio ?: user.bio,
            location = update.location ?: user.location,
            avatarUrl = update.avatarUrl ?: user.avatarUrl,
            version = user.version + 1,
        )
        tables.users().put(updated)
        toUserRecord(updated)
    }

    override suspend fun followUser(token: String, targetUserId: String) = withSimulation {
        val user = userForToken(token)
        conflictSimulator.evaluate(
            resourceType = "follow",
            id = "${user.id}:$targetUserId",
            clientVersion = user.version,
            serverVersion = user.version,
        ).let { outcome ->
            if (outcome is ConflictOutcome.Reject) throw outcome.error
        }
        if (user.id == targetUserId) throw ServerError.BadRequest("Cannot follow yourself")
        val follows = tables.follows()
        val key = "${user.id}:$targetUserId"
        if (!follows.exists(key)) {
            follows.put(
                StoredFollow(
                    followerId = user.id,
                    followeeId = targetUserId,
                    createdAt = clock.nowMs(),
                )
            )
        }
    }

    override suspend fun unfollowUser(token: String, targetUserId: String) = withSimulation {
        val user = userForToken(token)
        conflictSimulator.evaluate(
            resourceType = "follow",
            id = "${user.id}:$targetUserId",
            clientVersion = user.version,
            serverVersion = user.version,
        ).let { outcome ->
            if (outcome is ConflictOutcome.Reject) throw outcome.error
        }
        val key = "${user.id}:$targetUserId"
        tables.follows().remove(key)
        Unit
    }

    override suspend fun getFollowers(
        userId: String,
        request: CursorRequest,
        viewerToken: String?,
    ): CursorPage<UserSummary> = withSimulation {
        val followerIds = tables.follows().query { it.followeeId == userId }.map { it.followerId }
        val users = tables.users().query { followerIds.contains(it.id) }
        val viewerId = viewerToken?.let { userForToken(it).id }
        paginateUsers(users, request, viewerId)
    }

    override suspend fun getFollowing(
        userId: String,
        request: CursorRequest,
        viewerToken: String?,
    ): CursorPage<UserSummary> = withSimulation {
        val followeeIds = tables.follows().query { it.followerId == userId }.map { it.followeeId }
        val users = tables.users().query { followeeIds.contains(it.id) }
        val viewerId = viewerToken?.let { userForToken(it).id }
        paginateUsers(users, request, viewerId)
    }

    override suspend fun searchUsers(
        query: String,
        limit: Int,
        viewerToken: String?,
    ): List<UserSummary> = withSimulation {
        val normalized = query.trim().lowercase()
        val viewerId = viewerToken?.let { userForToken(it).id }
        val users = tables.users().query {
            it.username.lowercase().contains(normalized) || it.displayName.lowercase().contains(normalized)
        }
        users.take(limit).map { toUserSummary(it, viewerId) }
    }

    private suspend fun userForToken(token: String): StoredUser {
        val session = tables.sessions().get(token) ?: throw ServerError.Unauthorized()
        return tables.users().get(session.userId) ?: throw ServerError.Unauthorized()
    }

    private suspend fun createSession(user: StoredUser): UserAuthResponse {
        val token = "token_${user.id}_${clock.nowMs()}"
        val refreshToken = "refresh_${user.id}_${clock.nowMs()}"
        val expiresAt = clock.nowMs() + 86_400_000L
        tables.sessions().put(
            StoredSession(
                token = token,
                refreshToken = refreshToken,
                userId = user.id,
                expiresAt = expiresAt,
            )
        )
        return UserAuthResponse(
            user = toUserRecord(user),
            accessToken = token,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
        )
    }

    private suspend fun toUserRecord(user: StoredUser): UserRecord {
        val followers = tables.follows().query { it.followeeId == user.id }.size
        val following = tables.follows().query { it.followerId == user.id }.size
        val posts = tables.posts().query { it.authorId == user.id }.size
        return UserRecord(
            id = user.id,
            email = user.email,
            profile = ProfileRecord(
                firstName = user.firstName.orEmpty(),
                lastName = user.lastName.orEmpty(),
                birthdate = user.birthdate,
                displayName = user.displayName,
                username = user.username,
                avatarUrl = user.avatarUrl,
                bio = user.bio,
                location = user.location,
                verified = user.verified,
            ),
            stats = UserStats(
                followers = followers,
                following = following,
                posts = posts,
                totalVerticalFeet = 0,
                totalRuns = 0,
            ),
            createdAt = user.createdAt.toString(),
            version = user.version,
        )
    }

    private suspend fun toUserSummary(user: StoredUser, viewerId: String?): UserSummary {
        val isFollowing = viewerId?.let { id ->
            tables.follows().exists("${id}:${user.id}")
        } ?: false
        return UserSummary(
            id = user.id,
            username = user.username,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            verified = user.verified,
            isFollowing = isFollowing,
        )
    }

    private suspend fun paginateUsers(
        users: List<StoredUser>,
        request: CursorRequest,
        viewerId: String?,
    ): CursorPage<UserSummary> {
        val sorted = users.sortedBy { it.username }
        val page = paginate(sorted, request)
        return CursorPage(
            items = page.items.map { toUserSummary(it, viewerId) },
            nextCursor = page.nextCursor,
            prevCursor = page.prevCursor,
            totalCount = page.totalCount,
        )
    }

    private fun <T> paginate(items: List<T>, request: CursorRequest): CursorPage<T> {
        val limit = request.limit.coerceAtLeast(1)
        val size = items.size
        val cursor = request.cursor?.toIntOrNull()
        return if (request.direction == CursorRequest.Direction.BACKWARD) {
            val end = (cursor ?: size).coerceIn(0, size)
            val start = (end - limit).coerceAtLeast(0)
            CursorPage(
                items = items.subList(start, end),
                nextCursor = if (end < size) end.toString() else null,
                prevCursor = if (start > 0) start.toString() else null,
                totalCount = size,
            )
        } else {
            val start = (cursor ?: 0).coerceIn(0, size)
            val end = (start + limit).coerceAtMost(size)
            CursorPage(
                items = items.subList(start, end),
                nextCursor = if (end < size) end.toString() else null,
                prevCursor = if (start > 0) (start - limit).coerceAtLeast(0).toString() else null,
                totalCount = size,
            )
        }
    }
}
