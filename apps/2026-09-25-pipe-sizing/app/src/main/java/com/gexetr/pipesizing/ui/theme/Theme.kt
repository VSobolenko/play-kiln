package com.gexetr.pipesizing.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Navy40,
    onPrimary = Neutral99,
    primaryContainer = Navy90,
    onPrimaryContainer = Navy10,
    secondary = Slate40,
    onSecondary = Neutral99,
    secondaryContainer = Slate90,
    onSecondaryContainer = Slate20,
    tertiary = Amber40,
    onTertiary = Neutral99,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber30,
    error = Error40,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
)

private val DarkColorScheme = darkColorScheme(
    primary = Navy80,
    onPrimary = Navy20,
    primaryContainer = Navy30,
    onPrimaryContainer = Navy90,
    secondary = Slate80,
    onSecondary = Slate20,
    secondaryContainer = Slate30,
    onSecondaryContainer = Slate90,
    tertiary = Amber80,
    onTertiary = Amber30,
    tertiaryContainer = Amber30,
    onTertiaryContainer = Amber90,
    error = Error80,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant80,
)

@Composable
fun PlayKilnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default, deliberately. Dynamic colour hands the app's palette to the device
    // wallpaper, which is pleasant for a general-purpose app and wrong for a paid specialist tool:
    // the branded navy/amber scheme is part of what the user is buying, and it has to match the
    // launcher icon and the store listing. Callers can still opt in explicitly.
    dynamicColor: Boolean = false,
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
