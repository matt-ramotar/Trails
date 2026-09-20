package org.mobilenativefoundation.trails.feat.filters

import androidx.compose.runtime.Composable
import org.mobilenativefoundation.trails.data.trail.TrailQuery

/** Sheet section an Explore chip asks the sheet to scroll to. */
enum class FilterSection { ALL, DIFFICULTY, LENGTH, ELEVATION }

interface FiltersFeature {
    /**
     * Returns only an explicitly applied draft; dismissal leaves the original query unchanged.
     * [onDismiss] is an ephemeral invoker callback, excluded when the caller is cancelled/replaced.
     */
    suspend fun show(query: TrailQuery, section: FilterSection = FilterSection.ALL, onDismiss: () -> Unit = {}): TrailQuery?
    @Composable fun Content()
}
