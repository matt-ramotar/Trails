package org.mobilenativefoundation.trails.data.session

import kotlinx.serialization.json.Json
import org.mobilenativefoundation.trails.data.session.model.User
import org.mobilenativefoundation.trails.data.session.model.UserSerializer

internal object UserCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(user: User): String = json.encodeToString(UserSerializer, user)
    fun decode(value: String): User = json.decodeFromString(UserSerializer, value)
}
