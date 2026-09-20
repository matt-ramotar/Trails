package org.mobilenativefoundation.trails.server.pagination

import kotlinx.serialization.Serializable

@Serializable
data class CursorPage<T>(
    val items: List<T>,
    val nextCursor: String?,
    val prevCursor: String?,
    val totalCount: Int? = null,
)

@Serializable
data class CursorRequest(
    val cursor: String? = null,
    val limit: Int = 20,
    val direction: Direction = Direction.FORWARD,
) {
    enum class Direction { FORWARD, BACKWARD }
}

@Serializable
data class OffsetPage<T>(
    val items: List<T>,
    val offset: Int,
    val limit: Int,
    val totalCount: Int,
    val hasMore: Boolean,
)

@Serializable
data class OffsetRequest(
    val offset: Int = 0,
    val limit: Int = 20,
)
