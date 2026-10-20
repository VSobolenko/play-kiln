package com.gexetr.plinthproductphotos.ui.theme

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.unit.IntOffset

internal object PlinthMotion {
    const val TabSwitchDurationMillis = 160
    const val PushPopDurationMillis = 180
    const val EnterStaggerMillis = 40

    val tabFadeIn = tween<Float>(
        durationMillis = TabSwitchDurationMillis,
        easing = LinearOutSlowInEasing,
    )
    val pushFadeIn = tween<Float>(
        durationMillis = PushPopDurationMillis,
        easing = LinearOutSlowInEasing,
    )
    val pushSlideIn = tween<IntOffset>(
        durationMillis = PushPopDurationMillis,
        easing = LinearOutSlowInEasing,
    )
    val popFadeOut = tween<Float>(
        durationMillis = PushPopDurationMillis,
        easing = FastOutLinearInEasing,
    )
    val popSlideOut = tween<IntOffset>(
        durationMillis = PushPopDurationMillis,
        easing = FastOutLinearInEasing,
    )
    fun enterStagger(index: Int) = tween<Float>(
        durationMillis = 260,
        delayMillis = index.coerceAtLeast(0) * EnterStaggerMillis,
        easing = LinearOutSlowInEasing,
    )
    val pressScale = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)
    val shimmer = tween<Float>(durationMillis = 1400, easing = LinearEasing)
    val morph = tween<Float>(durationMillis = 220, easing = LinearOutSlowInEasing)
    val fadeThrough = tween<Float>(durationMillis = 180, easing = LinearOutSlowInEasing)
}

internal fun plinthPhotoSharedElementKey(photoId: String): String = "plinth-photo:$photoId"
