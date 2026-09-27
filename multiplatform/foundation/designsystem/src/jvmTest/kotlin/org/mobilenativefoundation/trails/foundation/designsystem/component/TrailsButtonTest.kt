package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun loadingButtonAnnouncesItsStateAndCannotDispatch() = runDesktopComposeUiTest {
        var clicks = 0
        setContent { TrailsTheme { TrailsButton("Save trail", onClick = { clicks++ }, tone = ButtonTone.Hero, loading = true, modifier = Modifier.testTag("button")) } }
        onNodeWithTag("button").assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Loading"))
            .performTouchInput { click() }
        assertEquals(0, clicks)
    }

    @Test fun heroToneFillsCitron() = assertEquals(Color(0xFFA9F184), fillOf(ButtonTone.Hero))
    @Test fun commitToneFillsDark() = assertEquals(Color(0xFF0D1F18), fillOf(ButtonTone.Commit))
    @Test fun secondaryToneFillsSoft() = assertEquals(Color(0xFFF4F5F4), fillOf(ButtonTone.Secondary))
}
