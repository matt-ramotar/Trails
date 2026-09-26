package org.mobilenativefoundation.trails.data.trail.catalog

import kotlinx.serialization.Serializable

/** Activities are derived from route facts, never authored per route. Every route is hiked, and multi-day treks are backpacked. */
@Serializable
enum class TrailActivity { HIKING, BACKPACKING }
