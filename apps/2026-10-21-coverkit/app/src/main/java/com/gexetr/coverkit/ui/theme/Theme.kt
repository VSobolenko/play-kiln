package com.gexetr.coverkit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ColorSecondaryContainerLight = Color(0xFFC5D8E0)
private val SurfaceVariantLight = Color(0xFFD9E2E8)
private val OnSurfaceVariantDark = Color(0xFFB8C5CF)

private val LightColorScheme = lightColorScheme(
    primary = CyanDeep,
    onPrimary = Paper,
    primaryContainer = CyanPale,
    onPrimaryContainer = Ink,
    secondary = AccentDeep,
    onSecondary = Paper,
    secondaryContainer = ColorSecondaryContainerLight,
    onSecondaryContainer = Ink,
    tertiary = AlertAmber,
    onTertiary = Ink,
    tertiaryContainer = AlertAmberPale,
    onTertiaryContainer = AlertAmberDeep,
    error = Error40,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = InkLift,
    outline = Muted,
)

private val DarkColorScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = OnCyan,
    primaryContainer = CyanDeep,
    onPrimaryContainer = Paper,
    secondary = CyanPale,
    onSecondary = Ink,
    secondaryContainer = AccentDeep,
    onSecondaryContainer = CyanPale,
    tertiary = AlertAmber,
    onTertiary = Ink,
    tertiaryContainer = AlertAmberDeep,
    onTertiaryContainer = AlertAmberPale,
    error = Error80,
    background = Ink,
    onBackground = Paper,
    surface = InkSoft,
    onSurface = Paper,
    surfaceVariant = InkLift,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = Muted,
)

@Composable
fun PlayKilnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PlayKilnTypography,
        content = content,
    )
}
