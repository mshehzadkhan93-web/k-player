package com.captain.kplayer.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.captain.kplayer.ui.theme.GradientBackground
import com.captain.kplayer.ui.theme.LocalAccent
import com.captain.kplayer.ui.theme.TextMuted
import com.captain.kplayer.ui.theme.TextPrimary
import com.captain.kplayer.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/** Splash screen: glass K logo with a play triangle, soft glow, app name. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500)
        onFinished()
    }
    val accent = LocalAccent.current
    val glow = rememberInfiniteTransition(label = "splashGlow")
    val glowAlpha by glow.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
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
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "K-Player",
                color = TextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "by Shehzad",
                color = TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Play everything beautifully",
                color = TextMuted,
                fontSize = 14.sp
            )
        }
    }
}
