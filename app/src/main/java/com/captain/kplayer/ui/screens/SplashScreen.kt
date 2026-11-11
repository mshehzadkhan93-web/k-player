package com.captain.kplayer.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.captain.kplayer.ui.theme.GradientBackground
import com.captain.kplayer.ui.theme.LocalAccent
import com.captain.kplayer.ui.theme.LocalTextMuted
import com.captain.kplayer.ui.theme.LocalTextPrimary
import com.captain.kplayer.ui.theme.LocalTextSecondary
import kotlinx.coroutines.delay

/** One drifting glow particle on the splash screen. */
private data class SplashParticle(
    val xOffsetDp: Float,
    val sizeDp: Float,
    val durationMillis: Int,
    val delayMillis: Int,
    val useEndColor: Boolean
)

/** A small glowing dot drifting upward, fading in and out as it rises. */
@Composable
private fun SplashParticleDot(spec: SplashParticle) {
    val accent = LocalAccent.current
    val transition = rememberInfiniteTransition(label = "particle${spec.xOffsetDp}")
    val riseY by transition.animateFloat(
        initialValue = 340f,
        targetValue = -340f,
        animationSpec = infiniteRepeatable(
            tween(spec.durationMillis, delayMillis = spec.delayMillis, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "riseY"
    )
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            tween(spec.durationMillis / 2, delayMillis = spec.delayMillis),
            RepeatMode.Reverse
        ),
        label = "twinkle"
    )
    Box(
        modifier = Modifier
            .offset(x = spec.xOffsetDp.dp, y = riseY.dp)
            .size(spec.sizeDp.dp)
            .graphicsLayer { alpha = twinkle }
            .blur(1.dp)
            .background(
                if (spec.useEndColor) accent.end else accent.start,
                CircleShape
            )
    )
}

/** Splash text that fades in while sliding up, staggered by [delayMillis]. */
@Composable
private fun SplashFadeText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    delayMillis: Int
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(650, delayMillis = delayMillis, easing = FastOutSlowInEasing),
        label = "fadeIn"
    )
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 28.dp.toPx()
        }
    )
}

/**
 * Splash screen: fancy 3D animated glass K logo — one-shot entrance
 * scale, gentle floating, a slow 3D tilt, pulsing glow, a glossy
 * shimmer sweep across the tile, drifting glow blobs and rising
 * particles behind it, with the texts fading in one after another.
 * Shows for ~1.8s, then calls [onFinished].
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        started = true
        delay(1800)
        onFinished()
    }
    val accent = LocalAccent.current

    // One-shot entrance: logo scales up from 0.5x and fades in.
    val entranceScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.5f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "entranceScale"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(500),
        label = "entranceAlpha"
    )

    // Infinite motion: gentle float, slow 3D tilt, breathing glow,
    // and a slow drift for the background glow blobs.
    val motion = rememberInfiniteTransition(label = "splashMotion")
    val floatY by motion.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "floatY"
    )
    val tilt by motion.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            tween(3200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "tilt"
    )
    val glowAlpha by motion.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    val blobDrift by motion.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            tween(6000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "blobDrift"
    )

    // Glossy shimmer sweeping across the logo tile, again and again.
    val shimmer = rememberInfiniteTransition(label = "splashShimmer")
    val shimmerX by shimmer.animateFloat(
        initialValue = -170f,
        targetValue = 170f,
        animationSpec = infiniteRepeatable(
            tween(2800, delayMillis = 500, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    val particles = remember {
        listOf(
            SplashParticle(-150f, 5f, 5200, 0, false),
            SplashParticle(-110f, 3f, 6800, 900, true),
            SplashParticle(-70f, 6f, 6000, 400, false),
            SplashParticle(-35f, 3f, 7400, 1400, true),
            SplashParticle(0f, 4f, 5600, 200, false),
            SplashParticle(40f, 3f, 7000, 1100, true),
            SplashParticle(75f, 6f, 6200, 600, false),
            SplashParticle(115f, 4f, 7800, 1600, true),
            SplashParticle(150f, 5f, 5400, 300, false)
        )
    }

    GradientBackground {
        // Extra drifting glow blobs + rising particles behind the content.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = blobDrift.dp, y = 90.dp)
                .size(220.dp)
                .blur(70.dp)
                .background(accent.start.copy(alpha = 0.20f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = -blobDrift.dp, y = (-70).dp)
                .size(200.dp)
                .blur(70.dp)
                .background(accent.end.copy(alpha = 0.18f), CircleShape)
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            particles.forEach { spec -> SplashParticleDot(spec) }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = entranceScale
                    scaleY = entranceScale
                    alpha = entranceAlpha
                    translationY = floatY.dp.toPx()
                    rotationZ = tilt
                    rotationY = tilt * 2f
                    cameraDistance = 16f * density
                },
                contentAlignment = Alignment.Center
            ) {
                // Pulsing glow halo behind the tile.
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .blur(32.dp)
                        .graphicsLayer { alpha = glowAlpha }
                        .background(accent.brush, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.07f)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "K",
                        style = TextStyle(
                            brush = accent.brush,
                            fontSize = 68.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Canvas(
                        modifier = Modifier
                            .size(34.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        val w = size.width
                        val h = size.height
                        val triangle = Path().apply {
                            moveTo(w * 0.28f, h * 0.18f)
                            lineTo(w * 0.85f, h * 0.5f)
                            lineTo(w * 0.28f, h * 0.82f)
                            close()
                        }
                        drawPath(triangle, color = Color.White)
                    }
                    // Glossy shimmer stripe sweeping across the tile.
                    Box(
                        modifier = Modifier
                            .offset(x = shimmerX.dp)
                            .fillMaxHeight()
                            .width(56.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.22f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            SplashFadeText(
                text = "K-Player",
                color = LocalTextPrimary.current,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                delayMillis = 350
            )
            Spacer(modifier = Modifier.height(4.dp))
            SplashFadeText(
                text = "by Shehzad",
                color = LocalTextSecondary.current,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                delayMillis = 550
            )
            Spacer(modifier = Modifier.height(12.dp))
            SplashFadeText(
                text = "Play everything beautifully",
                color = LocalTextMuted.current,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                delayMillis = 750
            )
        }
    }
}
