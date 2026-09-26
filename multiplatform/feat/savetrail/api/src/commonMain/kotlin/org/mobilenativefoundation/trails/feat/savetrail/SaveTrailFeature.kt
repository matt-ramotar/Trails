package org.mobilenativefoundation.trails.feat.savetrail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.mobilenativefoundation.trails.data.trail.Trail

interface SaveTrailFeature {
    /** [onDismiss] returns focus to the invoker; explicit navigation and account disposal skip it. */
    fun open(trail: Trail, onDismiss: () -> Unit = {})
    /** Host once per account. A sole collection routes there; several/zero route to Saved. */
    @Composable fun Content(onViewSaved: (collectionId: String?) -> Unit)
    /** Hosts the completion toast; the app places it inside the scaffold content so it sits above the navigation. */
    @Composable fun Toast(modifier: Modifier = Modifier)
}
