package org.mobilenativefoundation.trails.screen.explore

/** The host preserves independent root back stacks and their saved view state. */
interface M1Navigation {
    fun openTrail(trailId: String)
    fun selectExplore()
    fun selectSaved(collectionId: String? = null)
    fun back()
    fun selectForYou() {}
    fun selectNavigate() {}
    fun selectActivity() {}
    /** The trail most recently opened from any root; Navigate previews it. */
    val lastOpenedTrailId: String? get() = null
    val exploreView: M1ExploreView get() = M1ExploreView()
    val savedAllTrails: Boolean get() = false
    fun checkpointExplore(value: M1ExploreView) {}
    fun checkpointSavedSegment(allTrails: Boolean) {}
    fun viewKey(route: String): String = route
    fun scrollPosition(key: String): M1ScrollPosition = M1ScrollPosition()
    fun checkpointScroll(key: String, position: M1ScrollPosition) {}
}
