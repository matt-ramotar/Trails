package org.mobilenativefoundation.trails.data.trail

/** Null data means unknown/unavailable. A successful empty list is a different state. */
data class LoadState<out T>(
    val data: T? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val offline: Boolean = false,
)
