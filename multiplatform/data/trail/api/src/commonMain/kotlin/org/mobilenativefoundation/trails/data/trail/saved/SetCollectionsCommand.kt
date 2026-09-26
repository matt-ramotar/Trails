package org.mobilenativefoundation.trails.data.trail.saved

/** This immutable identity always describes the entire desired set for one account trail. */
data class SetCollectionsCommand(val id: String, val trailId: String, val collectionIds: Set<String>)
