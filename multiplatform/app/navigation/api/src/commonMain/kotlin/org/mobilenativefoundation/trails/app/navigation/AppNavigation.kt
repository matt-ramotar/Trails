package org.mobilenativefoundation.trails.app.navigation

/** The host preserves independent root back stacks and their saved view state. */
interface AppNavigation {
    fun openTrail(trailId: String)
    fun selectExplore()
    fun selectSaved(collectionId: String? = null)
    fun back()
    fun selectForYou() {}
    fun selectNavigate() {}
    fun selectActivity() {}
    /** The trail most recently opened from any root. Navigate previews it. */
    val lastOpenedTrailId: String? get() = null
    val exploreView: ExploreViewState get() = ExploreViewState()
    val savedAllTrails: Boolean get() = false
    fun checkpointExplore(value: ExploreViewState) {}
    fun checkpointSavedSegment(allTrails: Boolean) {}
    fun viewKey(route: String): String = route
    fun scrollPosition(key: String): ScrollPosition = ScrollPosition()
    fun checkpointScroll(key: String, position: ScrollPosition) {}
}
