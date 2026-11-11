package com.captain.kplayer.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

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

/**
 * Background gradient for the current theme. [top] / [bottom] are the
 * gradient ends; [mid] is an optional richer middle stop — when null
 * it is derived as the midpoint blend of [top] and [bottom], so the
 * two-argument construction keeps working unchanged.
 */
data class BackgroundColors(val top: Color, val bottom: Color, val mid: Color? = null) {
    /** Full multi-stop background gradient, top -> mid -> bottom. */
    val stops: List<Color> get() = listOf(top, midColor, bottom)

    /** The middle stop, explicit when given, otherwise a 50/50 blend. */
    val midColor: Color get() = mid ?: lerp(top, bottom, 0.5f)
}

/**
 * One switchable app theme: background gradient plus accent gradient.
 * Selecting a theme re-themes the whole app immediately.
 * [cardTint] is the theme's own tint washed into glass panels at very
 * low alpha, so cards follow the theme instead of looking identical
 * everywhere; null falls back to the accent glow color.
 * [isLight] marks a light theme (light surfaces, dark text). Light
 * themes re-theme the whole app: background gradient, card tints,
 * glows, the Material 3 color scheme and the text tokens below.
 */
data class ThemeDefinition(
    val id: String,
    val displayName: String,
    val description: String,
    val background: BackgroundColors,
    val accent: AccentColors,
    val cardTint: Color? = null,
    val isLight: Boolean = false
) {
    /** Tint color washed into glass surfaces for this theme. */
    val cardTintColor: Color get() = cardTint ?: accent.glowColor

    /** Primary text color for this theme: near-black on light themes, near-white on dark themes. */
    val textPrimary: Color get() = if (isLight) Color(0xFF101828) else TextPrimary

    /** Secondary text color for this theme. */
    val textSecondary: Color get() = if (isLight) Color(0xFF475467) else TextSecondary

    /** Muted text color for this theme. */
    val textMuted: Color get() = if (isLight) Color(0xFF667085) else TextMuted
}

val THEMES: Map<String, ThemeDefinition> = mapOf(
    "system" to ThemeDefinition(
        id = "system",
        displayName = "System default",
        description = "Follows your device light / dark mode",
        background = BackgroundColors(
            top = Color(0xFF050505),
            mid = Color(0xFF0E0E12),
            bottom = Color(0xFF17171C)
        ),
        accent = AccentColors(
            start = Color(0xFF22D3EE),
            end = Color(0xFFF472B6),
            glow = Color(0xFFA78BFA)
        ),
        cardTint = Color(0xFF22D3EE)
    ),
    "mx_dark" to ThemeDefinition(
        id = "mx_dark",
        displayName = "MX Dark",
        description = "Near-black MX-style dark layout, K-Player cyan to pink accent",
        background = BackgroundColors(
            top = Color(0xFF050505),
            mid = Color(0xFF0E0E12),
            bottom = Color(0xFF17171C)
        ),
        accent = AccentColors(
            start = Color(0xFF22D3EE),
            end = Color(0xFFF472B6),
            glow = Color(0xFFA78BFA)
        ),
        cardTint = Color(0xFF22D3EE)
    ),
    "light" to ThemeDefinition(
        id = "light",
        displayName = "Light",
        description = "White and light-grey surfaces with dark text",
        background = BackgroundColors(
            top = Color(0xFFF7F8FA),
            mid = Color(0xFFEDEFF3),
            bottom = Color(0xFFE2E5EC)
        ),
        accent = AccentColors(
            start = Color(0xFF0891B2),
            end = Color(0xFFDB2777),
            glow = Color(0xFF7C3AED)
        ),
        cardTint = Color(0xFF0891B2),
        isLight = true
    ),
    "nebula" to ThemeDefinition(
        id = "nebula",
        displayName = "Nebula",
        description = "Deep space indigo to violet nebula",
        background = BackgroundColors(
            top = Color(0xFF060A1E),
            mid = Color(0xFF141038),
            bottom = Color(0xFF2E1A5E)
        ),
        accent = AccentColors(
            start = Color(0xFF22D3EE),
            end = Color(0xFFF472B6),
            glow = Color(0xFFA78BFA)
        ),
        cardTint = Color(0xFF7C6CFF)
    ),
    "emerald" to ThemeDefinition(
        id = "emerald",
        displayName = "Emerald",
        description = "Deep forest emerald to jade glow",
        background = BackgroundColors(
            top = Color(0xFF010F0A),
            mid = Color(0xFF032E1F),
            bottom = Color(0xFF064E33)
        ),
        accent = AccentColors(
            start = Color(0xFF34D399),
            end = Color(0xFFA7F3D0),
            glow = Color(0xFF10B981)
        ),
        cardTint = Color(0xFF10B981)
    ),
    "sunset" to ThemeDefinition(
        id = "sunset",
        displayName = "Sunset",
        description = "Dusky plum to ember orange",
        background = BackgroundColors(
            top = Color(0xFF160A1E),
            mid = Color(0xFF3B1226),
            bottom = Color(0xFF7C2D12)
        ),
        accent = AccentColors(
            start = Color(0xFFF97316),
            end = Color(0xFFEC4899),
            glow = Color(0xFFFBBF24)
        ),
        cardTint = Color(0xFFF97316)
    ),
    "crimson" to ThemeDefinition(
        id = "crimson",
        displayName = "Crimson",
        description = "Black velvet to deep ruby",
        background = BackgroundColors(
            top = Color(0xFF0D0205),
            mid = Color(0xFF2A060B),
            bottom = Color(0xFF4C0519)
        ),
        accent = AccentColors(
            start = Color(0xFFE11D48),
            end = Color(0xFFFB7185),
            glow = Color(0xFFF43F5E)
        ),
        cardTint = Color(0xFFE11D48)
    ),
    "ocean" to ThemeDefinition(
        id = "ocean",
        displayName = "Ocean",
        description = "Midnight sea to deep teal",
        background = BackgroundColors(
            top = Color(0xFF020617),
            mid = Color(0xFF0A2540),
            bottom = Color(0xFF0C4A6E)
        ),
        accent = AccentColors(
            start = Color(0xFF2DD4BF),
            end = Color(0xFF38BDF8),
            glow = Color(0xFF0EA5E9)
        ),
        cardTint = Color(0xFF0EA5E9)
    ),
    "aurora" to ThemeDefinition(
        id = "aurora",
        displayName = "Aurora",
        description = "Polar night with aurora green to violet",
        background = BackgroundColors(
            top = Color(0xFF01161A),
            mid = Color(0xFF02292B),
            bottom = Color(0xFF134E4A)
        ),
        accent = AccentColors(
            Color(0xFF34D399),
            Color(0xFFA78BFA),
            glow = Color(0xFFF472B6)
        ),
        cardTint = Color(0xFF2DD4BF)
    ),
    "golden" to ThemeDefinition(
        id = "golden",
        displayName = "Golden Hour",
        description = "Espresso black to warm amber gold",
        background = BackgroundColors(
            top = Color(0xFF120C02),
            mid = Color(0xFF2A1A05),
            bottom = Color(0xFF451A03)
        ),
        accent = AccentColors(
            start = Color(0xFFF59E0B),
            end = Color(0xFFFDE68A),
            glow = Color(0xFFF97316)
        ),
        cardTint = Color(0xFFF59E0B)
    ),
    "cyberpunk" to ThemeDefinition(
        id = "cyberpunk",
        displayName = "Cyberpunk",
        description = "Neon night city, hot pink to electric cyan",
        background = BackgroundColors(
            top = Color(0xFF0A0618),
            mid = Color(0xFF1E0A2E),
            bottom = Color(0xFF2D0A4E)
        ),
        accent = AccentColors(
            start = Color(0xFFEC4899),
            end = Color(0xFF22D3EE),
            glow = Color(0xFFA855F7)
        ),
        cardTint = Color(0xFFA855F7)
    )
)

fun themeDefinitionFor(name: String): ThemeDefinition {
    return THEMES[name] ?: THEMES.getValue("nebula")
}

/**
 * Resolves a stored theme id to the definition the app actually uses.
 * "system" follows the device: the MX Dark definition in dark mode,
 * the Light definition in light mode. Any other id resolves to its own
 * definition; unknown ids fall back to Nebula.
 */
fun resolveTheme(name: String, systemDark: Boolean): ThemeDefinition {
    if (name == "system") {
        return if (systemDark) THEMES.getValue("mx_dark") else THEMES.getValue("light")
    }
    return THEMES[name] ?: THEMES.getValue("nebula")
}

/** Current accent gradient, provided by KPlayerTheme. */
val LocalAccent = compositionLocalOf { AccentColors(CyanAccent, PinkAccent) }

/** Current background gradient, provided by KPlayerTheme. */
val LocalBackground = compositionLocalOf { BackgroundColors(MidnightBlue, VioletDeep) }

/** Current full theme definition, provided by KPlayerTheme. */
val LocalTheme = compositionLocalOf { THEMES.getValue("nebula") }

/** Current primary text color, provided by KPlayerTheme from the resolved theme. */
val LocalTextPrimary = compositionLocalOf { TextPrimary }

/** Current secondary text color, provided by KPlayerTheme from the resolved theme. */
val LocalTextSecondary = compositionLocalOf { TextSecondary }

/** Current muted text color, provided by KPlayerTheme from the resolved theme. */
val LocalTextMuted = compositionLocalOf { TextMuted }

/** Glass blur intensity 0..1, provided by KPlayerTheme from settings. Defaults low for clear glass. */
val LocalGlassAlpha = compositionLocalOf { 0.12f }
