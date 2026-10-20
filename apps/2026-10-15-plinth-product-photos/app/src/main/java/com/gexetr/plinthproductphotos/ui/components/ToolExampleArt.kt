package com.gexetr.plinthproductphotos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.gexetr.plinthproductphotos.ui.theme.PlinthPalette

internal enum class ToolExample {
    CUT_OUT, LISTING_CARD, SHADOW_FILL, LOCAL_BACKGROUND, GENERATED_SCENE,
    RELIGHT, EXPAND_CANVAS, RECOLOUR, UPSCALE, ERASE_REGION, GHOST_MANNEQUIN, VIRTUAL_MODEL,
}

/** Small, deliberately illustrative before-and-after studies; they never claim to be source photography. */
@Composable
internal fun ToolExampleArt(example: ToolExample, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        when (example) {
            ToolExample.CUT_OUT -> drawCutOut()
            ToolExample.LISTING_CARD -> drawListingCard()
            ToolExample.SHADOW_FILL -> drawShadowFill()
            ToolExample.LOCAL_BACKGROUND -> drawLocalBackground()
            ToolExample.GENERATED_SCENE -> drawGeneratedScene()
            ToolExample.RELIGHT -> drawRelight()
            ToolExample.EXPAND_CANVAS -> drawExpandedCanvas()
            ToolExample.RECOLOUR -> drawRecolour()
            ToolExample.UPSCALE -> drawUpscale()
            ToolExample.ERASE_REGION -> drawEraseRegion()
            ToolExample.GHOST_MANNEQUIN -> drawGhostMannequin()
            ToolExample.VIRTUAL_MODEL -> drawVirtualModel()
        }
    }
}

private fun DrawScope.drawStudioBase(generative: Boolean = false) {
    val w = size.width
    val h = size.height
    val backdrop = if (generative) {
        Brush.verticalGradient(listOf(PlinthPalette.Spark.copy(.44f), PlinthPalette.Glow.copy(.22f), PlinthPalette.InkSoft))
    } else {
        Brush.verticalGradient(listOf(PlinthPalette.Paper, PlinthPalette.AmberPale, PlinthPalette.InkSoft.copy(.92f)))
    }
    drawRoundRect(backdrop, cornerRadius = CornerRadius(w * .09f))
    drawOval(Brush.radialGradient(listOf(PlinthPalette.Ink.copy(.42f), Color.Transparent), center = Offset(w * .5f, h * .78f), radius = w * .38f), topLeft = Offset(w * .16f, h * .68f), size = Size(w * .68f, h * .18f))
    drawOval(PlinthPalette.Paper.copy(.42f), topLeft = Offset(w * .2f, h * .71f), size = Size(w * .6f, h * .12f))
}

private fun DrawScope.drawBottle(center: Offset, scale: Float = 1f, color: Color = PlinthPalette.BenchLight, rim: Color = PlinthPalette.Paper) {
    val w = size.width * scale
    val h = size.height * scale
    val body = Path().apply {
        moveTo(center.x - w * .13f, center.y + h * .25f)
        lineTo(center.x - w * .11f, center.y - h * .12f)
        quadraticTo(center.x - w * .08f, center.y - h * .2f, center.x - w * .05f, center.y - h * .21f)
        lineTo(center.x - w * .05f, center.y - h * .32f)
        lineTo(center.x + w * .05f, center.y - h * .32f)
        lineTo(center.x + w * .05f, center.y - h * .21f)
        quadraticTo(center.x + w * .1f, center.y - h * .19f, center.x + w * .11f, center.y - h * .12f)
        lineTo(center.x + w * .13f, center.y + h * .25f)
        close()
    }
    drawPath(body, Brush.verticalGradient(listOf(rim, color, PlinthPalette.InkLift), startY = center.y - h * .35f, endY = center.y + h * .3f))
    drawPath(body, rim.copy(.78f), style = Stroke(w * .012f))
    drawRoundRect(PlinthPalette.InkSoft, Offset(center.x - w * .045f, center.y - h * .37f), Size(w * .09f, h * .08f), CornerRadius(w * .022f))
    drawLine(rim.copy(.7f), Offset(center.x - w * .07f, center.y - h * .12f), Offset(center.x - w * .09f, center.y + h * .15f), w * .018f)
}

private fun DrawScope.drawCutOut() {
    val w = size.width; val h = size.height; val tile = w * .11f
    drawRoundRect(PlinthPalette.Paper, cornerRadius = CornerRadius(w * .09f))
    for (x in 0..9) for (y in 0..7) if ((x + y) % 2 == 0) {
        drawRect(PlinthPalette.ContainerHighLight, Offset(x * tile, y * tile), Size(tile, tile))
    }
    drawBottle(Offset(w * .53f, h * .57f), .88f, PlinthPalette.AmberDeep)
    val outline = Path().apply { addOval(androidx.compose.ui.geometry.Rect(w * .34f, h * .16f, w * .72f, h * .82f)) }
    drawPath(outline, PlinthPalette.Amber, style = Stroke(w * .012f))
}

private fun DrawScope.drawListingCard() {
    val w = size.width; val h = size.height
    drawRoundRect(PlinthPalette.InkSoft, cornerRadius = CornerRadius(w * .09f))
    drawRoundRect(PlinthPalette.Paper, Offset(w * .14f, h * .1f), Size(w * .72f, h * .8f), CornerRadius(w * .05f))
    drawBottle(Offset(w * .5f, h * .46f), .58f, PlinthPalette.AmberDeep)
    drawRoundRect(PlinthPalette.InkSoft, Offset(w * .24f, h * .68f), Size(w * .31f, h * .045f), CornerRadius(w * .02f))
    drawRoundRect(PlinthPalette.Amber, Offset(w * .61f, h * .66f), Size(w * .14f, h * .1f), CornerRadius(w * .04f))
}

private fun DrawScope.drawShadowFill() {
    val w = size.width; val h = size.height
    drawStudioBase()
    drawOval(Brush.radialGradient(listOf(PlinthPalette.Ink.copy(.48f), Color.Transparent), radius = w * .37f), Offset(w * .13f, h * .65f), Size(w * .74f, h * .18f))
    drawBottle(Offset(w * .5f, h * .55f), .86f, PlinthPalette.AmberDeep)
}

private fun DrawScope.drawLocalBackground() {
    val w = size.width; val h = size.height
    drawRoundRect(Brush.verticalGradient(listOf(PlinthPalette.AmberPale, PlinthPalette.Paper)), cornerRadius = CornerRadius(w * .09f))
    drawRect(PlinthPalette.BenchLight, Offset(0f, h * .58f), Size(w, h * .42f))
    drawLine(PlinthPalette.AmberDeep.copy(.6f), Offset(0f, h * .58f), Offset(w, h * .58f), w * .012f)
    drawOval(PlinthPalette.Ink.copy(.28f), Offset(w * .29f, h * .68f), Size(w * .42f, h * .12f))
    drawBottle(Offset(w * .5f, h * .5f), .83f, PlinthPalette.AmberDeep)
}

private fun DrawScope.drawGeneratedScene() {
    val w = size.width; val h = size.height
    drawStudioBase(generative = true)
    drawArc(PlinthPalette.Glow.copy(.82f), 205f, 130f, false, Offset(w * .16f, h * .12f), Size(w * .28f, h * .28f), style = Stroke(w * .035f))
    drawLine(PlinthPalette.AmberPale.copy(.7f), Offset(0f, h * .56f), Offset(w, h * .56f), w * .013f)
    listOf(.1f, .27f, .74f, .88f).forEachIndexed { index, x ->
        drawCircle(if (index % 2 == 0) PlinthPalette.Amber.copy(.38f) else PlinthPalette.Glow.copy(.27f), w * .12f, Offset(w * x, h * .53f))
    }
    drawBottle(Offset(w * .5f, h * .55f), .8f, PlinthPalette.Paper)
}

private fun DrawScope.drawRelight() {
    val w = size.width; val h = size.height
    drawStudioBase()
    drawPath(Path().apply { moveTo(0f, 0f); lineTo(w * .55f, 0f); lineTo(w * .42f, h * .78f); close() }, PlinthPalette.Paper.copy(.28f))
    drawBottle(Offset(w * .54f, h * .56f), .85f, PlinthPalette.AmberDeep)
    drawLine(PlinthPalette.Paper, Offset(w * .4f, h * .3f), Offset(w * .38f, h * .66f), w * .03f)
}

private fun DrawScope.drawExpandedCanvas() {
    val w = size.width; val h = size.height
    drawRoundRect(PlinthPalette.Paper, cornerRadius = CornerRadius(w * .09f))
    drawRoundRect(PlinthPalette.AmberPale, Offset(w * .09f, h * .1f), Size(w * .82f, h * .8f), CornerRadius(w * .04f))
    val stroke = w * .012f
    listOf(Offset(w * .18f, h * .23f) to Offset(w * .82f, h * .23f), Offset(w * .18f, h * .77f) to Offset(w * .82f, h * .77f), Offset(w * .18f, h * .23f) to Offset(w * .18f, h * .77f), Offset(w * .82f, h * .23f) to Offset(w * .82f, h * .77f)).forEach { (start, end) ->
        val segments = 6
        repeat(segments) { i ->
            val a = i / segments.toFloat(); val b = (i + .55f) / segments
            drawLine(PlinthPalette.AmberDeep, Offset(start.x + (end.x - start.x) * a, start.y + (end.y - start.y) * a), Offset(start.x + (end.x - start.x) * b, start.y + (end.y - start.y) * b), stroke)
        }
    }
    drawBottle(Offset(w * .5f, h * .53f), .52f, PlinthPalette.AmberDeep)
}

private fun DrawScope.drawRecolour() {
    val w = size.width; val h = size.height
    drawStudioBase(generative = true)
    drawBottle(Offset(w * .41f, h * .56f), .63f, PlinthPalette.AmberDeep)
    drawBottle(Offset(w * .66f, h * .56f), .63f, PlinthPalette.Glow)
    drawCircle(PlinthPalette.Glow, w * .055f, Offset(w * .25f, h * .2f))
    drawCircle(PlinthPalette.Spark, w * .055f, Offset(w * .39f, h * .2f), style = Stroke(w * .014f))
}

private fun DrawScope.drawUpscale() {
    val w = size.width; val h = size.height
    drawStudioBase(generative = true)
    drawRect(PlinthPalette.InkSoft.copy(.62f), Offset(0f, 0f), Size(w * .48f, h))
    repeat(4) { i -> repeat(4) { j -> drawRect(if ((i + j) % 2 == 0) PlinthPalette.Glow.copy(.55f) else PlinthPalette.Spark.copy(.48f), Offset(w * (.09f + i * .085f), h * (.27f + j * .09f)), Size(w * .078f, h * .082f)) } }
    drawLine(PlinthPalette.Paper.copy(.85f), Offset(w * .5f, h * .12f), Offset(w * .5f, h * .88f), w * .014f)
    drawBottle(Offset(w * .73f, h * .55f), .62f, PlinthPalette.Paper)
}

private fun DrawScope.drawEraseRegion() {
    val w = size.width; val h = size.height
    drawStudioBase()
    drawBottle(Offset(w * .5f, h * .56f), .85f, PlinthPalette.AmberDeep)
    drawCircle(PlinthPalette.Paper.copy(.92f), w * .105f, Offset(w * .55f, h * .53f))
    drawCircle(PlinthPalette.Amber.copy(.72f), w * .105f, Offset(w * .55f, h * .53f), style = Stroke(w * .014f))
    drawCircle(PlinthPalette.Paper, w * .035f, Offset(w * .28f, h * .24f), style = Stroke(w * .018f))
}

private fun DrawScope.drawGhostMannequin() {
    val w = size.width; val h = size.height
    drawStudioBase()
    drawCircle(PlinthPalette.Paper.copy(.38f), w * .1f, Offset(w * .5f, h * .23f))
    drawRoundRect(PlinthPalette.Paper.copy(.28f), Offset(w * .33f, h * .33f), Size(w * .34f, h * .44f), CornerRadius(w * .15f))
    drawBottle(Offset(w * .5f, h * .57f), .68f, PlinthPalette.AmberDeep)
}

private fun DrawScope.drawVirtualModel() {
    val w = size.width; val h = size.height
    drawStudioBase()
    drawCircle(PlinthPalette.InkSoft, w * .09f, Offset(w * .39f, h * .23f))
    drawRoundRect(PlinthPalette.InkSoft, Offset(w * .29f, h * .33f), Size(w * .21f, h * .43f), CornerRadius(w * .1f))
    drawLine(PlinthPalette.InkSoft, Offset(w * .47f, h * .43f), Offset(w * .67f, h * .57f), w * .06f)
    drawBottle(Offset(w * .66f, h * .58f), .49f, PlinthPalette.AmberDeep)
}
