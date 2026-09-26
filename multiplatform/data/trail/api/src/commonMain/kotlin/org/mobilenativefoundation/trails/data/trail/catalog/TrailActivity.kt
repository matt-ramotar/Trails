package org.mobilenativefoundation.trails.data.trail.catalog

import kotlinx.serialization.Serializable

/** Derived from route facts, never authored per route: every route is hiked; multi-day treks are backpacked. */
@Serializable
enum class TrailActivity { HIKING, BACKPACKING }
