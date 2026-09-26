package org.mobilenativefoundation.trails.app.navigation

import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery

/** View preferences only. Canonical trails, memberships and pending work remain repository data. */
data class ExploreViewState(val text: String = "", val query: TrailQuery = TrailQuery())

data class ScrollPosition(val index: Int = 0, val offset: Int = 0) {
    init { require(index >= 0 && offset >= 0) }
}
