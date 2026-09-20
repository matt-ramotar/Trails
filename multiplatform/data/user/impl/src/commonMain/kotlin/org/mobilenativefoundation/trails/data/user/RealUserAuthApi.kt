package org.mobilenativefoundation.trails.data.user

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.datetime.LocalDate
import org.mobilenativefoundation.trails.data.auth.SessionStore
import org.mobilenativefoundation.trails.model.domain.user.ActiveUser
import org.mobilenativefoundation.trails.model.domain.user.InactiveUser
import org.mobilenativefoundation.trails.model.domain.user.LoggedInUser
import org.mobilenativefoundation.trails.model.domain.user.OnboardingStatus
import org.mobilenativefoundation.trails.model.domain.user.UserProfile
import org.mobilenativefoundation.trails.model.domain.user.UserSession
import org.mobilenativefoundation.trails.server.BackendServer
import org.mobilenativefoundation.trails.server.error.ServerError
import org.mobilenativefoundation.trails.server.model.ProfileRecord
import org.mobilenativefoundation.trails.server.model.ProfileUpdate
import org.mobilenativefoundation.trails.server.model.UserRecord

@ContributesBinding(AppScope::class)
@Inject
class RealUserAuthApi(
    private val backendServer: BackendServer,
    private val sessionStore: SessionStore,
) : UserAuthApi {

    override suspend fun signup(email: String, password: String): InactiveUser.Composite {
        val response = backendServer.userService.signup(email, password)
        sessionStore.setCurrentToken(response.accessToken)
        return response.user.toInactiveUser(response.accessToken)
    }

    override suspend fun login(email: String, password: String): LoggedInUser {
        val response = backendServer.userService.login(email, password)
        sessionStore.setCurrentToken(response.accessToken)
        return response.user.toLoggedInUser(response.accessToken)
    }

    override suspend fun completeOnboarding(
        firstName: String,
        lastName: String,
        birthdate: LocalDate,
    ): ActiveUser.Composite {
        val token = requireToken()
        val user = backendServer.userService.updateProfile(
            token = token,
            update = ProfileUpdate(
                firstName = firstName,
                lastName = lastName,
                birthdate = birthdate.toString(),
            ),
        )
        return user.toActiveUser(token)
    }

    override suspend fun logout() {
        val token = sessionStore.currentToken()
        if (token != null) {
            runCatching { backendServer.userService.logout(token) }
        }
        sessionStore.setCurrentToken(null)
    }

    override suspend fun getLoggedInUser(): LoggedInUser {
        val token = sessionStore.currentToken() ?: ensureDemoSession()
        val user = backendServer.userService.getCurrentUser(token)
        return user.toLoggedInUser(token)
    }

    private suspend fun requireToken(): String =
        sessionStore.currentToken() ?: throw ServerError.Unauthorized()

    private suspend fun ensureDemoSession(): String {
        val existingToken = sessionStore.currentToken()
        if (existingToken != null) return existingToken

        val authResponse = runCatching {
            backendServer.userService.login(DEFAULT_EMAIL, DEFAULT_PASSWORD)
        }.getOrElse {
            backendServer.userService.signup(DEFAULT_EMAIL, DEFAULT_PASSWORD)
        }

        sessionStore.setCurrentToken(authResponse.accessToken)
        backendServer.userService.updateProfile(
            token = authResponse.accessToken,
            update = ProfileUpdate(
                firstName = DEFAULT_FIRST_NAME,
                lastName = DEFAULT_LAST_NAME,
                birthdate = DEFAULT_BIRTHDATE.toString(),
            ),
        )

        return sessionStore.currentToken() ?: authResponse.accessToken
    }

    private fun UserRecord.toInactiveUser(token: String): InactiveUser.Composite {
        val session = sessionFor(token)
        return InactiveUser.Composite(
            node = InactiveUser.Node(
                id = id,
                properties = InactiveUser.Properties(
                    session = session,
                    onboardingStatus = OnboardingStatus.Incomplete,
                    profile = null,
                ),
            ),
            edges = InactiveUser.Edges,
        )
    }

    private fun UserRecord.toActiveUser(token: String): ActiveUser.Composite {
        val session = sessionFor(token)
        val profileRecord = requireNotNull(profile)
        val birthdate = requireNotNull(profileRecord.birthdate)
        return ActiveUser.Composite(
            node = ActiveUser.Node(
                id = id,
                properties = ActiveUser.Properties(
                    session = session,
                    onboardingStatus = OnboardingStatus.Complete,
                    profile = profileRecord.toDomainProfile(id, email, birthdate),
                ),
            ),
            edges = ActiveUser.Edges,
        )
    }

    private fun UserRecord.toLoggedInUser(token: String): LoggedInUser {
        val profileRecord = profile
        val birthdate = profileRecord?.birthdate?.let(::parseBirthdate)
        return if (profileRecord != null && birthdate != null) {
            ActiveUser.Composite(
                node = ActiveUser.Node(
                    id = id,
                    properties = ActiveUser.Properties(
                        session = sessionFor(token),
                        onboardingStatus = OnboardingStatus.Complete,
                        profile = profileRecord.toDomainProfile(id, email, birthdate.toString()),
                    ),
                ),
                edges = ActiveUser.Edges,
            )
        } else {
            toInactiveUser(token)
        }
    }

    private fun ProfileRecord.toDomainProfile(
        userId: String,
        email: String,
        birthdateRaw: String,
    ): UserProfile.Node {
        val birthdate = parseBirthdate(birthdateRaw) ?: DEFAULT_BIRTHDATE
        return UserProfile.Node(
            id = UserProfile.Id("profile_$userId"),
            properties = UserProfile.Properties(
                email = email,
                firstName = firstName,
                lastName = lastName,
                birthdate = birthdate,
                profileImageUrl = avatarUrl,
                bio = bio,
                location = location,
                verified = verified,
            ),
        )
    }

    private fun sessionFor(token: String): UserSession =
        UserSession(
            sessionId = "session_$token",
            token = token,
        )

    private fun parseBirthdate(raw: String): LocalDate? =
        runCatching { LocalDate.parse(raw) }.getOrNull()

    private companion object {
        private const val DEFAULT_EMAIL = "demo@trails.app"
        private const val DEFAULT_PASSWORD = "password"
        private const val DEFAULT_FIRST_NAME = "Morgan"
        private const val DEFAULT_LAST_NAME = "Ridge"
        private val DEFAULT_BIRTHDATE = LocalDate(1992, 2, 12)
    }
}
