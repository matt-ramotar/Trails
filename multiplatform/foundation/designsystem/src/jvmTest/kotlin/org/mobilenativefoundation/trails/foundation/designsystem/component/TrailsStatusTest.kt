package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class TrailsStatusTest {
    @Test
    fun statusLineShowsMessageAndRunsItsAction() = runDesktopComposeUiTest {
        var retried = false
        setContent { TrailsTheme { TrailsStatusLine(StatusKind.FAILED, "Couldn’t refresh · Showing saved trails", actionLabel = "Try again", onAction = { retried = true }) } }
        onNodeWithText("Couldn’t refresh · Showing saved trails").assertIsDisplayed()
        onNodeWithText("Try again").performClick()
        assertTrue(retried)
    }

    @Test
    fun toastBodyDoesNotPassTapsToTheContentBeneathIt() = runDesktopComposeUiTest {
        var beneath = 0
        setContent {
            TrailsTheme {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().clickable { beneath++ })
                    TrailsToast(TrailsToastData("Saved to Weekend adventures", "View") {}, Modifier.align(Alignment.BottomCenter))
                }
            }
        }
        onNodeWithText("Saved to Weekend adventures").performClick()
        assertEquals(0, beneath, "The toast body must consume its own taps")
    }

    @Test
    fun toastHostClearsItselfAfterFiveSeconds() = runDesktopComposeUiTest {
        mainClock.autoAdvance = false
        var toast by mutableStateOf<TrailsToastData?>(TrailsToastData("Saved to Weekend adventures", "View") {})
        setContent { TrailsTheme { TrailsToastHost(toast, onDismissed = { toast = null }) } }
        mainClock.advanceTimeByFrame()
        onNodeWithText("Saved to Weekend adventures").assertIsDisplayed()
        mainClock.advanceTimeBy(5_100)
        assertEquals(null, toast, "Dismissal callback still runs at five seconds")
        mainClock.advanceTimeBy(350) // Allow the Native exit and content-size transition to finish.
        mainClock.advanceTimeByFrame()
        onNodeWithText("Saved to Weekend adventures").assertDoesNotExist()
    }
}
