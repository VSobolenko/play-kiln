package com.gexetr.plinthproductphotos.ui.components

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.ui.theme.PlinthPalette

@Composable
fun AuroraField(modifier: Modifier = Modifier, intensity: Float = 1f) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "aurora-phase").value
    Canvas(modifier = modifier.drawWithCache {
        val teal = PlinthPalette.Amber.copy(alpha = .18f * intensity)
        val spark = PlinthPalette.Spark.copy(alpha = .16f * intensity)
        val glow = PlinthPalette.Glow.copy(alpha = .13f * intensity)
        onDrawBehind {
            drawRect(Color.Transparent)
            drawCircle(Brush.radialGradient(listOf(teal, Color.Transparent), Offset(size.width * (.12f + phase * .08f), size.height * .12f), size.maxDimension * .6f), radius = size.maxDimension * .6f, center = Offset(size.width * (.12f + phase * .08f), size.height * .12f))
            drawCircle(Brush.radialGradient(listOf(spark, Color.Transparent), Offset(size.width * .82f, size.height * (.20f + phase * .09f)), size.maxDimension * .52f), radius = size.maxDimension * .52f, center = Offset(size.width * .82f, size.height * (.20f + phase * .09f)))
            drawCircle(Brush.radialGradient(listOf(glow, Color.Transparent), Offset(size.width * .52f, size.height * .84f), size.maxDimension * .48f), radius = size.maxDimension * .48f, center = Offset(size.width * .52f, size.height * .84f))
        }
    }) {}
}

@Composable
fun AuroraRim(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRoundRect(Brush.linearGradient(listOf(PlinthPalette.Amber, PlinthPalette.Spark, PlinthPalette.Glow)), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
    }
}
