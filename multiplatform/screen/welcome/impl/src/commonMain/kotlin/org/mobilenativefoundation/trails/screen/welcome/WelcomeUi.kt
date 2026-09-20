package org.mobilenativefoundation.trails.screen.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailPhoto
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailPhotoCredit
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsBrand
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsM1Button
import org.mobilenativefoundation.trails.foundation.designsystem.component.StatusKind
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsStatusLine
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.foundation.scope.LoggedOutScope

@ContributesBinding(LoggedOutScope::class)
@Inject
class WelcomeUi : Ui<WelcomeState> {
    @Composable
    override fun Content(state: WelcomeState, modifier: Modifier) {
        val typography = TrailsTheme.typography
        val colors = TrailsTheme.colors
        val spacing = TrailsTheme.spacing
        val radii = TrailsTheme.radii

        Column(
            modifier.fillMaxSize().background(colors.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xl, vertical = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            TrailsBrand()
            Box(Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(radii.card))) {
                TrailPhoto("half-dome", modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, colors.dark.copy(alpha = 0.65f)))))
                Text(
                    "A little closer to outside.",
                    modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = spacing.xl, vertical = spacing.xxl),
                    style = typography.titleLarge,
                    color = colors.citron,
                )
            }
            TrailPhotoCredit("half-dome")
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Text("Your next trail starts here.", style = typography.displayLarge, color = colors.textPrimary)
                Text(
                    "Find a path that feels like you. Save it for the day you’re ready to go.",
                    style = typography.bodyLarge,
                    color = colors.textSecondary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Text("Explore a local sample", style = typography.titleMedium, color = colors.textPrimary)
                Text(
                    "Sample trail details and your saves are stored on this device, so you can return to them offline.",
                    style = typography.bodyMedium,
                    color = colors.textSecondary,
                )
                state.error?.let { message ->
                    TrailsStatusLine(
                        StatusKind.FAILED,
                        message,
                        modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    )
                }
                TrailsM1Button(
                    text = when {
                        state.isLoading -> "Opening sample trails…"
                        state.error != null -> "Try again"
                        else -> "Explore sample trails"
                    },
                    onClick = { state.eventSink(WelcomeIntent.ExploreSampleTrails) },
                    loading = state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Real trails and location photography. Ratings, reviews, activity history and recommendations are sample data.",
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
