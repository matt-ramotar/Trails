package org.mobilenativefoundation.trails.data.auth

interface SessionStore {
    suspend fun currentToken(): String?

    suspend fun setCurrentToken(token: String?)
}
