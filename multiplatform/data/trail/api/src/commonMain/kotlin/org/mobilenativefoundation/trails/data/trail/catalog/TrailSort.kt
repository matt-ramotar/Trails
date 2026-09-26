package org.mobilenativefoundation.trails.data.trail.catalog

import kotlinx.serialization.Serializable

/** Most popular is the catalog's documented recommended order; the others reorder the same membership. */
@Serializable
enum class TrailSort { MOST_POPULAR, HIGHEST_RATED, SHORTEST, LONGEST }
