package org.mobilenativefoundation.trails.app.scaffold

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.mobilenativefoundation.trails.foundation.scope.ActiveScope

@SingleIn(ActiveScope::class)
@ContributesBinding(ActiveScope::class)
@Inject
class RealScaffoldPresenter(
    private val router: ScaffoldRouter
) : ScaffoldPresenter {
    @Composable
    override fun present(): ScaffoldState {
        var selectedBottomNavItem by remember { mutableStateOf(TrailsBottomNavItem.HOME) }
        return ScaffoldState(
            selectedBottomNavItem = selectedBottomNavItem,
            bottomNavItems = listOf(
                TrailsBottomNavItem.HOME,
                TrailsBottomNavItem.PROFILE,
            )
        ) { intent ->
            when (intent) {
                is ScaffoldIntent.SelectBottomNavItem -> {
                    val selectedItem = intent.item
                    selectedBottomNavItem = selectedItem
                    when (selectedItem) {
                        TrailsBottomNavItem.HOME -> router.attachHome()
                        TrailsBottomNavItem.PROFILE -> router.attachProfile()
                    }
                }
            }
        }
    }
}


