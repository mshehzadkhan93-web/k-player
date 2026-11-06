package com.captain.kplayer.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.captain.kplayer.ui.theme.LocalAccent
import com.captain.kplayer.ui.theme.LocalGlassAlpha
import com.captain.kplayer.ui.theme.TextMuted
import com.captain.kplayer.ui.theme.TextPrimary
import com.captain.kplayer.ui.theme.TextSecondary

/** Bottom navigation tabs on the Home screen. */
enum class HomeTab { VIDEOS, MUSIC, FOLDERS, NETWORK, SETTINGS }

/**
 * Clear-glass fill: low white alpha (about 8-12% at the default glass
 * setting) so content stays visible through the panel — no heavy frost.
 * The fill strength still follows the glass intensity from Settings >
 * Appearance, scaled down to the clear-glass range.
 */
private fun glassBrush(alpha: Float): Brush {
    return Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.08f + alpha * 0.06f),
            Color.White.copy(alpha = 0.02f + alpha * 0.03f)
        )
    )
}

/**
 * Clear-glass edge: a thin bright white border (~45% white) plus a
 * subtle top-light inner glow that fades toward the bottom edge.
 */
private fun Modifier.glassEdge(shape: RoundedCornerShape): Modifier {
    return this
        .border(1.dp, Color.White.copy(alpha = 0.45f), shape)
        .border(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.02f)
                )
            ),
            shape
        )
}

/**
 * Clear-glass panel: low-alpha white fill, thin bright white border
 * with a subtle inner glow, rounded corners and a soft shadow. Content
 * behind the panel stays faintly visible through it. The fill strength
 * follows the glass intensity from Settings > Appearance.
 * [opaque] renders a solid surface instead (used by sheets and dialogs
 * so nothing bleeds through from behind).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    opaque: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val alpha = LocalGlassAlpha.current
    val shape = RoundedCornerShape(radius)
    var cardModifier = modifier
        .shadow(10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.35f))
        .then(
            if (opaque) {
                Modifier
                    .background(MaterialTheme.colorScheme.surface, shape)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            } else {
                Modifier
                    .background(glassBrush(alpha), shape)
                    .glassEdge(shape)
            }
        )
        .clip(shape)
    if (onClick != null) {
        cardModifier = cardModifier.clickable(onClick = onClick)
    }
    Column(modifier = cardModifier, content = content)
}

/** Glass button with an icon and label, 48dp minimum height, press scale. */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f, label = "glassButtonPress"
    )
    val alpha = LocalGlassAlpha.current
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(48.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
            .background(glassBrush(alpha), RoundedCornerShape(24.dp))
            .glassEdge(RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Primary call-to-action button with the cyan-to-pink accent gradient. */
@Composable
fun AccentGradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f, label = "accentButtonPress"
    )
    val accent = LocalAccent.current
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(48.dp)
            .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = accent.start.copy(alpha = 0.4f))
            .background(accent.brush, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Small pill badge, used for duration and resolution labels. */
@Composable
fun Badge(
    text: String,
    gradient: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(8.dp)
    var badgeModifier = modifier
        .background(
            if (gradient) accent.brush
            else SolidColor(Color.White.copy(alpha = 0.10f)),
            shape
        )
    if (!gradient) {
        badgeModifier = badgeModifier.border(1.dp, Color.White.copy(alpha = 0.35f), shape)
    }
    Box(
        modifier = badgeModifier
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Rounded progress bar with a gradient fill. */
@Composable
fun GlassProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccent.current
    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.10f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(accent.brush, RoundedCornerShape(2.dp))
        )
    }
}

/** Glass seek bar with a 48dp touch target. */
@Composable
fun GlassSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccent.current
    Slider(
        value = value.coerceIn(0f, 1f),
        onValueChange = onValueChange,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = accent.start,
            inactiveTrackColor = Color.White.copy(alpha = 0.12f)
        ),
        modifier = modifier.height(48.dp)
    )
}

/** Glass text field used for search and the stream URL input. */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true
) {
    val accent = LocalAccent.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = placeholder, color = TextMuted, fontSize = 15.sp) },
        leadingIcon = leadingIcon,
        singleLine = singleLine,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.10f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
            focusedBorderColor = Color.White.copy(alpha = 0.50f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.40f),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = accent.start
        ),
        modifier = modifier.height(56.dp)
    )
}

/** Clear-glass bottom navigation bar with five tabs. Semi-transparent so the list scrolling underneath stays faintly visible through it. */
@Composable
fun GlassBottomBar(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccent.current
    val items = listOf(
        HomeTab.VIDEOS to "Videos",
        HomeTab.MUSIC to "Music",
        HomeTab.FOLDERS to "Folders",
        HomeTab.NETWORK to "Network",
        HomeTab.SETTINGS to "Settings"
    )
    val icons = mapOf(
        HomeTab.VIDEOS to Icons.Rounded.Movie,
        HomeTab.MUSIC to Icons.Rounded.MusicNote,
        HomeTab.FOLDERS to Icons.Rounded.Folder,
        HomeTab.NETWORK to Icons.Rounded.Public,
        HomeTab.SETTINGS to Icons.Filled.Settings
    )
    val alpha = LocalGlassAlpha.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.25f))
            .background(glassBrush(alpha), RoundedCornerShape(24.dp))
            .glassEdge(RoundedCornerShape(24.dp))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { (tab, label) ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(tab) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icons.getValue(tab),
                    contentDescription = label,
                    tint = if (isSelected) accent.start else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = label,
                    color = if (isSelected) TextPrimary else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(width = 16.dp, height = 3.dp)
                            .background(accent.brush, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

/** Bottom sheet with a solid opaque surface — content behind it must never show through. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.4f))
        }
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 20.dp),
            opaque = true,
            content = content
        )
    }
}

/** Dialog with a solid opaque surface — content behind it must never show through. */
@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            opaque = true,
            content = content
        )
    }
}

/** Section header used on the Settings screen. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp)
    )
}

/** One settings row: title, optional subtitle, trailing control. */
@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, color = TextSecondary, fontSize = 13.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

/** Three animated bars shown next to the now-playing queue item. */
@Composable
fun NowPlayingBars(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "nowPlaying")
    val heights = listOf(
        transition.animateFloat(
            initialValue = 4f, targetValue = 16f,
            animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
            label = "bar1"
        ),
        transition.animateFloat(
            initialValue = 14f, targetValue = 5f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "bar2"
        ),
        transition.animateFloat(
            initialValue = 6f, targetValue = 15f,
            animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
            label = "bar3"
        )
    )
    val accent = LocalAccent.current
    Row(
        modifier = modifier.height(18.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        heights.forEach { animated ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animated.value.dp)
                    .background(accent.brush, RoundedCornerShape(2.dp))
            )
        }
    }
}
