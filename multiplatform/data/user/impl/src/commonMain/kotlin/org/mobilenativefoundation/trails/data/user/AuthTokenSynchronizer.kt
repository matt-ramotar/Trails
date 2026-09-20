package org.mobilenativefoundation.trails.data.user

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.trails.data.auth.SessionStore
import org.mobilenativefoundation.trails.foundation.logging.Logger
import org.mobilenativefoundation.trails.foundation.networking.AuthTokenHolder
import org.mobilenativefoundation.trails.model.domain.user.LoggedInUser
import org.mobilenativefoundation.trails.model.domain.user.LoggedOutUser
import org.mobilenativefoundation.trails.model.domain.user.User

/**
 * Keeps [AuthTokenHolder] in sync with the current [User] stream.
 *
 * This component is side-effecting and should live in [AppScope].
 *
 * @param appScope Application scope used for collection; must be non-null
 * @param userStore Store emitting user updates; must be non-null
 * @param sessionStore Session token cache used by server-backed data APIs; must be non-null
 * @param logger Logger used for non-fatal errors; must be non-null
 * @see UserRepository for the user state source
 */
@OptIn(ExperimentalStoreApi::class)
@SingleIn(AppScope::class)
@Inject
class AuthTokenSynchronizer(
    @param:Named("AppCoroutineScope") private val appScope: CoroutineScope,
    private val userStore: UserStore,
    private val sessionStore: SessionStore,
    private val logger: Logger,
) {
    init {
        appScope.launch {
            userStore
                .stream<Unit>(StoreReadRequest.cached(CurrentUserKey, refresh = false))
                .filterIsInstance<StoreReadResponse.Data<User>>()
                .map { it.value.sessionTokenOrNull() }
                .distinctUntilChanged()
                .catch { throwable ->
                    logger.error(TAG, "Failed to sync auth token.", throwable)
                }
                .collect { token ->
                    AuthTokenHolder.token = token
                    sessionStore.setCurrentToken(token)
                }
        }
    }

    private fun User.sessionTokenOrNull(): String? =
        when (this) {
            is LoggedInUser -> node.properties.session.token
            is LoggedOutUser -> null
        }

    private companion object {
        private const val TAG = "AuthTokenSynchronizer"
    }
}
