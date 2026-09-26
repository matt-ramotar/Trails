package org.mobilenativefoundation.trails.screen.explore

import org.mobilenativefoundation.trails.data.trail.TrailQuery

/** View preferences only. Canonical trails, memberships and pending work remain repository data. */
data class M1ExploreView(val text: String = "", val query: TrailQuery = TrailQuery())

data class M1ScrollPosition(val index: Int = 0, val offset: Int = 0) {
    init { require(index >= 0 && offset >= 0) }
}
