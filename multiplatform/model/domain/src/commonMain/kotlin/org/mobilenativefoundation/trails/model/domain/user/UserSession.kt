package org.mobilenativefoundation.trails.model.domain.user

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val sessionId: String,
    val token: String,
)