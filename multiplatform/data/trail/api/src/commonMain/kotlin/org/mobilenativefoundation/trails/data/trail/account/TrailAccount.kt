package org.mobilenativefoundation.trails.data.trail.account

import org.mobilenativefoundation.trails.data.trail.activity.ActivityRepository
import org.mobilenativefoundation.trails.data.trail.recommendation.ForYouRepository
import org.mobilenativefoundation.trails.data.trail.saved.SavedRepository

interface TrailAccount {
    val accountId: String
    val saved: SavedRepository
    val activities: ActivityRepository
    val forYou: ForYouRepository
    /** Retires the account owner, joins jobs, then closes its drivers. */
    suspend fun close()
}
