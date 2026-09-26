package org.mobilenativefoundation.trails.app.scaffold

import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons

enum class TrailsBottomNavItem(
    val label: String,
    val icons: BottomNavItemIcon,
) {


    HOME(
        label = "Home",
        icons = BottomNavItemIcon(
            selected = Icons.Solid.Home,
            unselected = Icons.Outlined.Home
        ),
    ),

    PROFILE(
        label = "Profile",
        icons = BottomNavItemIcon(
            selected = Icons.Solid.User3,
            unselected = Icons.Outlined.User3
        ),
    )

}
