package org.mobilenativefoundation.trails.data.post

internal data class FeedPostEntity(
    val id: String,
    val authorId: String,
    val runId: String,
    val runResortId: String,
    val resortId: String,
    val weatherId: String,
    val weatherResortId: String,
    val json: String,
)
