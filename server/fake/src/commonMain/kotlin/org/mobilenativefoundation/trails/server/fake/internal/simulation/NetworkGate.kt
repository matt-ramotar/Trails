package org.mobilenativefoundation.trails.server.fake.internal.simulation

import org.mobilenativefoundation.trails.server.BackendConfigProvider
import org.mobilenativefoundation.trails.server.NetworkMode
import org.mobilenativefoundation.trails.server.error.ServerError

class NetworkGate(
    private val configProvider: BackendConfigProvider,
) {
    fun ensureOnline(): ServerError? {
        return if (configProvider.current.networkMode == NetworkMode.OFFLINE) {
            ServerError.NetworkUnavailable
        } else null
    }
}
