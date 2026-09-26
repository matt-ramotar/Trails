package org.mobilenativefoundation.trails.data.session.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val sessionId: String,
    val token: String,
)