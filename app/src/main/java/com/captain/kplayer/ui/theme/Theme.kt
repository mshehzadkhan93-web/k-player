package com.captain.kplayer.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val KPlayerColorScheme = darkColorScheme(
    primary = CyanAccent,
    secondary = PinkAccent,
    tertiary = VioletAccent,
    background = MidnightBlue,
    surface = Color(0xFF151B3D),
    surfaceVariant = Color(0xFF1E2547),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

private val KPlayerTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium)
)

/**
 * App theme: dark Material 3 scheme plus the K-Player design tokens.
 * [themeName] picks one of the five themes (Nebula, Emerald, Sunset,
 * Crimson, Ocean); the whole app re-themes the moment it changes.
 */
@Composable
fun KPlayerTheme(
    themeName: String = "nebula",
    blurIntensity: Float = 0.6f,
    content: @Composable () -> Unit
) {
    val theme = themeDefinitionFor(themeName)
    val accent = theme.accent
    val background = theme.background
    val scheme = KPlayerColorScheme.copy(
        primary = accent.start,
        secondary = accent.end,
        background = background.top,
        surface = background.top
    )
    CompositionLocalProvider(
        LocalAccent provides accent,
        LocalBackground provides background,
        LocalGlassAlpha provides blurIntensity.coerceIn(0f, 1f)
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = KPlayerTypography,
            content = content
        )
    }
}

/**
 * Full-screen gradient background with soft glow blobs in the current
 * theme's accent colors, shared by every screen.
 */
@Composable
fun GradientBackground(content: @Composable BoxScope.() -> Unit) {
    val background = LocalBackground.current
    val accent = LocalAccent.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(background.top, background.top, background.bottom)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxSize(0.55f)
                .blur(90.dp)
                .background(accent.start.copy(alpha = 0.16f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxSize(0.5f)
                .blur(90.dp)
                .background(accent.end.copy(alpha = 0.13f), CircleShape)
        )
        content()
    }
}
