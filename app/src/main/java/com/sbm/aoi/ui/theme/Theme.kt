package com.sbm.aoi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    error = Error,
    onPrimary = OnPrimary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onError = OnError,
)

private val DeeperDarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = BackgroundDeeper,
    surface = SurfaceDeeper,
    surfaceVariant = SurfaceVariantDeeper,
    error = Error,
    onPrimary = OnPrimary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onError = OnError,
)

@Composable
fun SbmAoiTheme(
    useDeeperDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDeeperDark) DeeperDarkColorScheme else DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
