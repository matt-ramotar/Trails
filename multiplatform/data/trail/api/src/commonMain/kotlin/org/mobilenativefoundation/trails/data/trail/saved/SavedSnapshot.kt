package org.mobilenativefoundation.trails.data.trail.saved

import org.mobilenativefoundation.trails.data.trail.catalog.Trail

data class SavedSnapshot(
    val collections: List<TrailCollection>,
    val memberships: Map<String, Set<String>>,
    val trails: List<Trail>,
    val syncByTrail: Map<String, TrailSync>,
    val syncing: Boolean = false,
    val offline: Boolean = false,
)
