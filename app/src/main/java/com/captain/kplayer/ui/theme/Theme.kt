package com.captain.kplayer.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Dark Material 3 scheme built from the selected theme, so primary,
 * secondary and tertiary always follow the theme's accent colors.
 */
private fun kPlayerColorScheme(theme: ThemeDefinition) = darkColorScheme(
    primary = theme.accent.start,
    secondary = theme.accent.end,
    tertiary = theme.accent.glowColor,
    background = theme.background.top,
    surface = theme.background.top,
    surfaceVariant = theme.background.midColor,
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
 * [themeName] picks one of the eight themes (Nebula, Emerald, Sunset,
 * Crimson, Ocean, Aurora, Golden Hour, Cyberpunk); the whole app
 * re-themes the moment it changes.
 */
@Composable
fun KPlayerTheme(
    themeName: String = "nebula",
    blurIntensity: Float = 0.12f,
    content: @Composable () -> Unit
) {
    val theme = themeDefinitionFor(themeName)
    val accent = theme.accent
    val background = theme.background
    val scheme = kPlayerColorScheme(theme)
    CompositionLocalProvider(
        LocalAccent provides accent,
        LocalBackground provides background,
        LocalTheme provides theme,
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
 * theme's accent colors, shared by every screen. The base is a rich
 * multi-stop vertical gradient (top -> mid -> bottom) from the theme,
 * and the glow blobs slowly drift and breathe so the background feels
 * alive instead of flat.
 */
@Composable
fun GradientBackground(content: @Composable BoxScope.() -> Unit) {
    val background = LocalBackground.current
    val accent = LocalAccent.current
    val drift = rememberInfiniteTransition(label = "backgroundDrift")
    val driftX by drift.animateFloat(
        initialValue = -28f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            tween(9000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "driftX"
    )
    val driftY by drift.animateFloat(
        initialValue = 18f,
        targetValue = -18f,
        animationSpec = infiniteRepeatable(
            tween(11000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "driftY"
    )
    val breathe by drift.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            tween(5000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "breathe"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(background.stops)
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = driftX.dp, y = driftY.dp)
                .fillMaxSize(0.55f)
                .blur(90.dp)
                .background(accent.start.copy(alpha = breathe), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = -driftX.dp, y = -driftY.dp)
                .fillMaxSize(0.5f)
                .blur(90.dp)
                .background(accent.end.copy(alpha = breathe * 0.8f), CircleShape)
        )
        if (accent.hasGlow) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = driftY.dp, y = driftX.dp)
                    .fillMaxSize(0.45f)
                    .blur(90.dp)
                    .background(accent.glowColor.copy(alpha = breathe * 0.7f), CircleShape)
            )
        }
        content()
    }
}
