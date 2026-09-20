package org.mobilenativefoundation.trails.app.bootstrap

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.trails.data.user.UserStateReader
import org.mobilenativefoundation.trails.foundation.scope.ActiveScope
import org.mobilenativefoundation.trails.model.domain.user.LoggedInUser
import org.mobilenativefoundation.trails.model.domain.user.LoggedOutUser

@Inject
@SingleIn(ActiveScope::class)
@ContributesBinding(ActiveScope::class)
class RealAuthTokenProvider(
    private val userStateReader: UserStateReader,
) : org.mobilenativefoundation.trails.app.bootstrap.AuthTokenProvider {

    override fun getToken(): String? {
        return when (val user = userStateReader.current) {
            is LoggedInUser -> user.node.properties.session.token
            is LoggedOutUser -> null
        }
    }

    override fun tokenStream(): Flow<String?> {
        return userStateReader.stream().map { user ->
            when (user) {
                is LoggedInUser -> user.node.properties.session.token
                is LoggedOutUser -> null
            }
        }
    }
}
