package org.mobilenativefoundation.trails.app.scaffold

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.runtime.Navigator

interface TrailsScaffold {
    @Composable
    fun Content(
        state: ScaffoldState,
        backStack: SaveableBackStack,
        navigator: Navigator,
        modifier: Modifier = Modifier
    )
}




