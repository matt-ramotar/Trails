package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** Native Dialog/Surface treatment with side placement and Compose's drawer focus/Back ownership. */
@Composable
fun TrailsModalDrawer(
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    drawerModifier: Modifier = Modifier,
    drawerContent: @Composable ColumnScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = TrailsTheme.colors
    val shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        scrimColor = Color.Black.copy(alpha = 0.2f),
        drawerContent = {
            // The state-taking overload keeps modal Back ownership through account replacement.
            ModalDrawerSheet(
                drawerState = drawerState,
                modifier = drawerModifier.fillMaxHeight().widthIn(max = 360.dp).trailsShadow(shape, TrailsShadow.Overlay),
                drawerShape = shape,
                drawerContainerColor = colors.surface,
                drawerContentColor = colors.textPrimary,
                drawerTonalElevation = 0.dp,
                content = drawerContent,
            )
        },
        content = content,
    )
}
