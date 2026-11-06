package com.captain.kplayer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.captain.kplayer.player.EqualizerManager
import com.captain.kplayer.ui.components.GlassButton
import com.captain.kplayer.ui.components.GlassDialog
import com.captain.kplayer.ui.theme.LocalAccent
import com.captain.kplayer.ui.theme.TextPrimary
import com.captain.kplayer.ui.theme.TextSecondary
import com.captain.kplayer.viewmodel.PlayerViewModel
import kotlin.math.atan2

/**
 * Glass equalizer dialog: five vertical sliders (60Hz, 230Hz, 910Hz,
 * 3.6kHz, 14kHz), a presets dropdown, a radial bass-boost knob and an
 * on/off switch. Levels are applied live to the real audio effects.
 */
@Composable
fun EqualizerDialog(
    playerVm: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val accent = LocalAccent.current
    val manager by playerVm.equalizerManager.collectAsState()
    val eqEnabled by playerVm.equalizerEnabled.collectAsState()
    val preset by playerVm.equalizerPreset.collectAsState()
    var showPresets by remember { mutableStateOf(false) }
    var bandLevels by remember { mutableStateOf(listOf(0f, 0f, 0f, 0f, 0f)) }
    var bassStrength by remember { mutableStateOf(0) }

    GlassDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Equalizer",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = eqEnabled,
                    onCheckedChange = { playerVm.setEqualizerEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accent.start
                    )
                )
            }
            if (manager == null || manager?.supported == false) {
                Text(
                    text = "Audio effects are not available on this device.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                val mgr = manager!!
                // Presets.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preset",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        GlassButton(
                            onClick = { showPresets = true },
                            text = preset
                        )
                        DropdownMenu(
                            expanded = showPresets,
                            onDismissRequest = { showPresets = false }
                        ) {
                            EqualizerManager.PRESETS.keys.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        playerVm.applyEqualizerPreset(name)
                                        bandLevels =
                                            EqualizerManager.PRESETS.getValue(name)
                                        showPresets = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Five vertical sliders.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    mgr.targetFrequencies.forEachIndexed { index, freqHz ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(width = 48.dp, height = 150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Slider(
                                    value = bandLevels[index],
                                    onValueChange = { value ->
                                        bandLevels = bandLevels.toMutableList().also {
                                            it[index] = value
                                        }
                                        playerVm.setEqualizerBand(freqHz, value)
                                    },
                                    valueRange = -15f..15f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = accent.start,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .rotate(-90f)
                                        .width(150.dp)
                                )
                            }
                            Text(
                                text = if (bandLevels[index] > 0) {
                                    "+${bandLevels[index].toInt()} dB"
                                } else {
                                    "${bandLevels[index].toInt()} dB"
                                },
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = freqLabel(freqHz),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // Bass boost knob.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bass Boost",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Drag the knob to boost low frequencies",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    BassKnob(
                        strength = bassStrength,
                        onStrengthChange = { value ->
                            bassStrength = value
                            playerVm.setBassBoost(value)
                        }
                    )
                }
            }
            GlassButton(
                onClick = onDismiss,
                text = "Done",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private fun freqLabel(freqHz: Int): String = when {
    freqHz >= 1000 -> "${freqHz / 1000}.${(freqHz % 1000) / 100}kHz"
    else -> "${freqHz}Hz"
}

/**
 * Radial bass-boost knob: a 270-degree arc dial. Dragging around the dial
 * maps the pointer angle to 0..100 strength.
 */
@Composable
fun BassKnob(
    strength: Int,
    onStrengthChange: (Int) -> Unit
) {
    val accent = LocalAccent.current
    val sweep = 270f
    val startAngle = 135f
    Box(
        modifier = Modifier
            .size(96.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = change.position.x - center.x
                    val dy = change.position.y - center.y
                    val degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    // Angle measured clockwise from the dial start (135 deg).
                    var relative = (degrees - startAngle + 360f) % 360f
                    if (relative > sweep) {
                        // Inside the bottom dead zone: snap to the nearest end.
                        relative = if (relative > sweep + (360f - sweep) / 2f) 0f else sweep
                    }
                    onStrengthChange((relative / sweep * 100f).toInt().coerceIn(0, 100))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(96.dp)) {
            drawArc(
                color = Color.White.copy(alpha = 0.2f),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
            drawArc(
                color = accent.start,
                startAngle = startAngle,
                sweepAngle = sweep * strength / 100f,
                useCenter = false,
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$strength%",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
