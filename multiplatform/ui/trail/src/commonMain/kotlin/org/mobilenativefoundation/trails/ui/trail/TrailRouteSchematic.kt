package org.mobilenativefoundation.trails.ui.trail

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** Normalized (0..1) route polyline. Deterministic per seed; a loop repeats its first point last. */
internal fun schematicRoute(seed: String, loop: Boolean, points: Int = 14): List<Pair<Float, Float>> {
    require(points >= 8)
    val random = Random(seed.hashCode())
    return if (loop) {
        val ring = List(points) { index ->
            val angle = 2.0 * PI * index / points
            val radius = 0.30 + random.nextDouble(-0.06, 0.08)
            (0.5 + radius * cos(angle)).toFloat() to (0.5 + radius * 0.85 * sin(angle)).toFloat()
        }
        ring + ring.first()
    } else List(points) { index ->
        val progress = index / (points - 1).toDouble()
        val wander = random.nextDouble(-0.10, 0.10)
        (0.12 + 0.76 * progress).toFloat() to (0.82 - 0.62 * progress + wander).coerceIn(0.08, 0.92).toFloat()
    }
}

/** Route preview without a basemap: halo, route line, start marker and (for non-loops) a citron end marker. Decorative; the screen labels it schematic (DEV-32). */
@Composable
fun TrailRouteSchematic(seed: String, loop: Boolean, modifier: Modifier = Modifier) {
    val colors = TrailsTheme.colors
    val points = remember(seed, loop) { schematicRoute(seed, loop) }
    Canvas(modifier) {
        val path = Path()
        points.forEachIndexed { index, (x, y) ->
            val point = Offset(x * size.width, y * size.height)
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, colors.surface, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, colors.dark, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val start = Offset(points.first().first * size.width, points.first().second * size.height)
        drawCircle(colors.surface, radius = 10.dp.toPx(), center = start)
        drawCircle(colors.dark, radius = 7.dp.toPx(), center = start)
        if (!loop) {
            val end = Offset(points.last().first * size.width, points.last().second * size.height)
            drawCircle(colors.surface, radius = 9.dp.toPx(), center = end)
            drawCircle(colors.citron, radius = 6.dp.toPx(), center = end)
        }
    }
}
