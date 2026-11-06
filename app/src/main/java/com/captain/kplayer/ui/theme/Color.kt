package com.captain.kplayer.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val MidnightBlue = Color(0xFF0B1026)
val VioletDeep = Color(0xFF2A1B4E)
val VioletMid = Color(0xFF1B1440)
val CyanAccent = Color(0xFF22D3EE)
val PinkAccent = Color(0xFFF472B6)
val VioletAccent = Color(0xFFA78BFA)
val BlueAccent = Color(0xFF38BDF8)
val EmeraldAccent = Color(0xFF34D399)

val TextPrimary = Color(0xFFF2F4FF)
val TextSecondary = Color(0xFFB9C0E0)
val TextMuted = Color(0xFF7E87A8)

/**
 * Accent gradient pair for the current theme, plus an optional third
 * glow color used for the extra GradientBackground blob. When [glow]
 * is null the end color doubles as the glow color, so older themes
 * keep working unchanged.
 */
data class AccentColors(val start: Color, val end: Color, val glow: Color? = null) {
    val brush: Brush get() = Brush.horizontalGradient(listOf(start, end))
    val verticalBrush: Brush get() = Brush.verticalGradient(listOf(start, end))
    val glowColor: Color get() = glow ?: end
    val hasGlow: Boolean get() = glow != null
}

/** Background gradient pair for the current theme. */
data class BackgroundColors(val top: Color, val bottom: Color)

/**
 * One switchable app theme: background gradient plus accent gradient.
 * Selecting a theme re-themes the whole app immediately.
 */
data class ThemeDefinition(
    val id: String,
    val displayName: String,
    val description: String,
    val background: BackgroundColors,
    val accent: AccentColors
)

val THEMES: Map<String, ThemeDefinition> = mapOf(
    "nebula" to ThemeDefinition(
        id = "nebula",
        displayName = "Nebula",
        description = "Midnight blue to violet",
        background = BackgroundColors(Color(0xFF0B1026), Color(0xFF2A1B4E)),
        accent = AccentColors(Color(0xFF22D3EE), Color(0xFFF472B6))
    ),
    "emerald" to ThemeDefinition(
        id = "emerald",
        displayName = "Emerald",
        description = "Dark emerald green to black",
        background = BackgroundColors(Color(0xFF022C22), Color(0xFF000000)),
        accent = AccentColors(Color(0xFF10B981), Color(0xFFF59E0B))
    ),
    "sunset" to ThemeDefinition(
        id = "sunset",
        displayName = "Sunset",
        description = "Charcoal plum to black",
        background = BackgroundColors(Color(0xFF2D1B2E), Color(0xFF000000)),
        accent = AccentColors(Color(0xFFFB923C), Color(0xFFFBBF24))
    ),
    "crimson" to ThemeDefinition(
        id = "crimson",
        displayName = "Crimson",
        description = "Near-black to deep maroon",
        background = BackgroundColors(Color(0xFF0A0A0A), Color(0xFF450A0A)),
        accent = AccentColors(Color(0xFFEF4444), Color(0xFFFB7185))
    ),
    "ocean" to ThemeDefinition(
        id = "ocean",
        displayName = "Ocean",
        description = "Deep navy to black",
        background = BackgroundColors(Color(0xFF0C1B3D), Color(0xFF000000)),
        accent = AccentColors(Color(0xFF2DD4BF), Color(0xFF38BDF8))
    ),
    "aurora" to ThemeDefinition(
        id = "aurora",
        displayName = "Aurora",
        description = "Deep teal-black with aurora green to purple",
        background = BackgroundColors(Color(0xFF02141A), Color(0xFF000000)),
        accent = AccentColors(
            Color(0xFF34D399),
            Color(0xFFA78BFA),
            glow = Color(0xFFF472B6)
        )
    ),
    "golden" to ThemeDefinition(
        id = "golden",
        displayName = "Golden Hour",
        description = "Black with gold to champagne",
        background = BackgroundColors(Color(0xFF0A0A0A), Color(0xFF000000)),
        accent = AccentColors(Color(0xFFF59E0B), Color(0xFFFDE68A))
    ),
    "cyberpunk" to ThemeDefinition(
        id = "cyberpunk",
        displayName = "Cyberpunk",
        description = "Near-black with hot pink to electric cyan",
        background = BackgroundColors(Color(0xFF0A0A12), Color(0xFF000000)),
        accent = AccentColors(Color(0xFFEC4899), Color(0xFF22D3EE))
    )
)

fun themeDefinitionFor(name: String): ThemeDefinition {
    return THEMES[name] ?: THEMES.getValue("nebula")
}

/** Current accent gradient, provided by KPlayerTheme. */
val LocalAccent = compositionLocalOf { AccentColors(CyanAccent, PinkAccent) }

/** Current background gradient, provided by KPlayerTheme. */
val LocalBackground = compositionLocalOf { BackgroundColors(MidnightBlue, VioletDeep) }

/** Glass blur intensity 0..1, provided by KPlayerTheme from settings. Defaults low for clear glass. */
val LocalGlassAlpha = compositionLocalOf { 0.12f }
