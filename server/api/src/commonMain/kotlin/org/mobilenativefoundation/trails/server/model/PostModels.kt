package org.mobilenativefoundation.trails.server.model

import kotlinx.serialization.Serializable

@Serializable
data class CreatePostRequest(
    val runId: String,
    val styleId: String?,
    val caption: String?,
)

@Serializable
data class UpdatePostRequest(
    val caption: String? = null,
    val styleId: String? = null,
    val version: Long,
)

@Serializable
data class CommentRecord(
    val id: String,
    val author: UserSummary,
    val body: String,
    val timestamp: String,
    val likes: Int,
    val isLiked: Boolean,
)
