package com.gexetr.plinthproductphotos.w75

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.ui.card.CardScreen
import com.gexetr.plinthproductphotos.ui.card.CardUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CardViewerZoomTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun inspectionPinchAndPanCanCenterAnOffCenterImageDetail() {
        val bitmap = quadrantImage()
        composeRule.setContent {
            MaterialTheme {
                CardScreen(
                    state = CardUiState(
                        isLoading = false,
                        photoName = "Four corners",
                        original = bitmap,
                    ),
                    onBack = {},
                    onMakeCard = {},
                    onEdit = {},
                    onExport = {},
                )
            }
        }

        val inspect = composeRule.onNodeWithText(composeRule.activityString(R.string.card_inspect))
            .assertIsDisplayed()
        val close = composeRule.onNodeWithText(composeRule.activityString(R.string.card_close_inspection))
        for (xFraction in listOf(0.25f, 0.75f)) {
            for (yFraction in listOf(0.25f, 0.75f)) {
                inspect.performClick()
                val image = composeRule.onNodeWithTag("card-inspection-zoomable-image")
                    .assertIsDisplayed()
                var initialImageWidth = 0f
                var initialImageHeight = 0f
                image.performTouchInput {
                    val bitmapScale = minOf(
                        1f,
                        visibleSize.width.toFloat() / bitmap.width,
                        visibleSize.height.toFloat() / bitmap.height,
                    )
                    val imageWidth = bitmap.width * bitmapScale
                    val imageHeight = bitmap.height * bitmapScale
                    initialImageWidth = imageWidth
                    initialImageHeight = imageHeight
                    val imageLeft = (visibleSize.width - imageWidth) / 2f
                    val imageTop = (visibleSize.height - imageHeight) / 2f
                    val focus = Offset(
                        imageLeft + imageWidth * xFraction,
                        imageTop + imageHeight * yFraction,
                    )
                    down(0, focus + Offset(-20f, 0f))
                    down(1, focus + Offset(20f, 0f))
                    updatePointerTo(0, focus + Offset(-80f, 0f))
                    updatePointerTo(1, focus + Offset(80f, 0f))
                    move()
                    up(0)
                    up(1)
                }
                image.performTouchInput {
                    val bitmapScale = minOf(
                        1f,
                        visibleSize.width.toFloat() / bitmap.width,
                        visibleSize.height.toFloat() / bitmap.height,
                    )
                    val imageWidth = bitmap.width * bitmapScale
                    val imageHeight = bitmap.height * bitmapScale
                    val imageLeft = (visibleSize.width - imageWidth) / 2f
                    val imageTop = (visibleSize.height - imageHeight) / 2f
                    val start = Offset(
                        imageLeft + imageWidth * xFraction,
                        imageTop + imageHeight * yFraction,
                    )
                    down(start)
                    moveTo(Offset(visibleSize.width / 2f, visibleSize.height / 2f))
                    up()
                }
                composeRule.waitForIdle()

                val captured = image.captureToImage().toPixelMap()
                val centerX = captured.width / 2
                val centerY = captured.height / 2
                assertEquals(Color.Cyan, captured[centerX, centerY])
                var markerWidth = 1
                while (centerX - markerWidth / 2 >= 0 &&
                    centerX + markerWidth / 2 < captured.width &&
                    captured[centerX - markerWidth / 2, centerY] == Color.Cyan
                ) {
                    markerWidth += 2
                }
                assertTrue(
                    "off-center marker should grow beyond the old 2x viewer ceiling",
                    markerWidth > minOf(initialImageWidth, initialImageHeight) * 0.16f,
                )
                close.performClick()
            }
        }
    }

    private fun quadrantImage(): Bitmap {
        val bitmap = Bitmap.createBitmap(1200, 1200, Bitmap.Config.ARGB_8888)
        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val color = when {
                    (x in 262..337 || x in 862..937) &&
                        (y in 262..337 || y in 862..937) -> Color.Cyan
                    x < 600 && y < 600 -> Color.Red
                    x >= 600 && y < 600 -> Color.Green
                    x < 600 -> Color.Blue
                    else -> Color.Yellow
                }
                row[x] = color.toArgb()
            }
            bitmap.setPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
        }
        return bitmap
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.activityString(id: Int): String =
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(id)
}
