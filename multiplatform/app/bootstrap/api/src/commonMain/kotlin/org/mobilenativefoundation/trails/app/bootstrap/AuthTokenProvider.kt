package org.mobilenativefoundation.trails.app.bootstrap

import kotlinx.coroutines.flow.Flow

interface AuthTokenProvider {
    fun getToken(): String?
    fun tokenStream(): Flow<String?>
}