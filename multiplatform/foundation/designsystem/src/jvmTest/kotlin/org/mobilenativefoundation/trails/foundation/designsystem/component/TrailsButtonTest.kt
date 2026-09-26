package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class TrailsButtonTest {
    private fun fillOf(tone: ButtonTone): Color {
        var pixel = Color.Unspecified
        runDesktopComposeUiTest {
            setContent { TrailsTheme { TrailsButton("Save trail", onClick = {}, tone = tone, modifier = Modifier.testTag("button")) } }
            val image = onNodeWithTag("button").captureToImage()
            pixel = image.toPixelMap()[6, image.height / 2]
        }
        return pixel
    }

    /** A loading button is disabled, so its spinner has to read against the soft disabled container. */
    @Test
    fun loadingSpinnerDrawsInTheDisabledContentColourNotTheTonesOwn() = runDesktopComposeUiTest {
        mainClock.autoAdvance = false
        // No label, so the only content colour in the capture is the spinner's.
        setContent { TrailsTheme { TrailsButton("", onClick = {}, tone = ButtonTone.Hero, loading = true, modifier = Modifier.testTag("button")) } }
        mainClock.advanceTimeBy(400)
        val pixels = onNodeWithTag("button").captureToImage().toPixelMap()
        val drawn = buildSet { for (x in 0 until pixels.width) for (y in 0 until pixels.height) add(pixels[x, y]) }
        assertTrue(Color(0xFF545A52) in drawn, "The spinner must be drawn in the disabled content colour")
        assertFalse(Color(0xFF171E14) in drawn, "The spinner must not use the Hero tone's own content colour")
    }

    @Test fun heroToneFillsCitron() = assertEquals(Color(0xFFA9F184), fillOf(ButtonTone.Hero))
    @Test fun commitToneFillsDark() = assertEquals(Color(0xFF0D1F18), fillOf(ButtonTone.Commit))
    @Test fun secondaryToneFillsSoft() = assertEquals(Color(0xFFF4F5F4), fillOf(ButtonTone.Secondary))
}
