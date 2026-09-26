package org.mobilenativefoundation.trails.data.trail.saved

enum class TrailSyncStatus { SYNCED, PENDING, SYNCING, FINISHING, PARKED }

/** Why parked work cannot proceed. [TrailSyncCause.INCOMPATIBLE] means this build cannot read the stored work, so only an app update resolves it. */
enum class TrailSyncCause { INCOMPATIBLE, OTHER }

data class TrailSync(
    val status: TrailSyncStatus,
    val pendingCount: Int = 0,
    val reason: String? = null,
    val canRetry: Boolean = false,
    val cause: TrailSyncCause = TrailSyncCause.OTHER,
)
