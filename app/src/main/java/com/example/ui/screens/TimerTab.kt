package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ClockViewModel
import com.example.ui.theme.BodySm
import com.example.ui.theme.DarkGrayBg
import com.example.ui.theme.DisplayTimer
import com.example.ui.theme.HeadlineMd
import com.example.ui.theme.LabelCaps
import com.example.ui.theme.LiquidOrange
import com.example.ui.theme.OnSurfaceLight
import com.example.ui.theme.OnSurfaceMuted
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.glassCard
import com.example.ui.theme.glassPill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.ui.SoundHapticHelper
import com.example.HapticManager
import android.content.Intent

@Composable
fun TimerTab(viewModel: ClockViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hrs by viewModel.timerHrs.collectAsState()
    val min by viewModel.timerMin.collectAsState()
    val sec by viewModel.timerSec.collectAsState()
    val isRunning by viewModel.timerIsRunning.collectAsState()
    val remainingMs by viewModel.timerRemainingMs.collectAsState()
    val totalMs by viewModel.timerTotalMs.collectAsState()
    val activePresetVal by viewModel.selectedPresetMin.collectAsState()
    val selectedTimerSound by viewModel.selectedTimerSound.collectAsState()
    val timerSounds = viewModel.timerSounds

    val presets = listOf(1, 3, 5, 10, 15, 30)

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(
                top = if (isLandscape) 8.dp else 56.dp,
                bottom = if (isLandscape) 8.dp else 100.dp
            )
    ) {
        // App header (Clock title, gear settings icon button top right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Clock",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight,
                letterSpacing = (-0.5).sp
            )

            // Dynamic setup settings top right
            GlassIconBtn(
                icon = Icons.Default.Settings,
                onClick = {
                    viewModel.showSettings(true)
                },
                tint = OnSurfaceLight
            )
        }

        val isTimerActive = isRunning || remainingMs < totalMs && remainingMs > 0

        AnimatedContent(
            targetState = isTimerActive,
            transitionSpec = {
                (fadeIn(animationSpec = tween(400)) + scaleIn(animationSpec = tween(400), initialScale = 0.95f))
                    .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(animationSpec = tween(300), targetScale = 0.95f))
            },
            label = "timer_view_switcher",
            modifier = Modifier.weight(1f)
        ) { active ->
            if (active) {
                // Live countdown view (if running)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "TIMER RUNNING",
                        style = LabelCaps,
                        color = OnSurfaceMuted,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = formatRemainingTime(remainingMs),
                        style = DisplayTimer.copy(
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-0.5).sp
                        ),
                        color = LiquidOrange
                    )
                }
            } else {
                // Traditional Column selections (Hours / Minutes / Seconds grid Cards)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // HRS COLUMNCARD
                    ColumnCard(
                        label = "HRS",
                        value = hrs,
                        onIncrement = { viewModel.setTimerHrs(hrs + 1) },
                        onDecrement = { viewModel.setTimerHrs(hrs - 1) }
                    )

                    // MIN COLUMNCARD
                    ColumnCard(
                        label = "MIN",
                        value = min,
                        onIncrement = { viewModel.setTimerMin(min + 1) },
                        onDecrement = { viewModel.setTimerMin(min - 1) },
                        showDotOverlay = true
                    )

                    // SEC COLUMNCARD
                    ColumnCard(
                        label = "SEC",
                        value = sec,
                        onIncrement = { viewModel.setTimerSec(sec + 1) },
                        onDecrement = { viewModel.setTimerSec(sec - 1) }
                    )
                }
            }
        }

        // QUICK PRESETS SECTION
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "QUICK PRESETS",
                color = OnSurfaceMuted.copy(alpha = 0.6f),
                style = LabelCaps,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 2.dp)
            )

            // Presets Chips horizontal row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(presets) { presetMin ->
                    val isActive = presetMin == activePresetVal
                    val bgAlpha by animateFloatAsState(
                        targetValue = if (isActive) 0.2f else 0.05f,
                        animationSpec = tween(250),
                        label = "preset_bg_alpha"
                    )
                    val bg = if (isActive) LiquidOrange.copy(alpha = bgAlpha) else Color.White.copy(alpha = bgAlpha)
                    val tint by animateColorAsState(
                        targetValue = if (isActive) LiquidOrange else OnSurfaceLight,
                        animationSpec = tween(250),
                        label = "preset_tint"
                    )

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .glassPill(bgColor = bg)
                            .clickable {
                                HapticManager.light(context.applicationContext)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                SoundHapticHelper.playSound269(context)
                                viewModel.selectPreset(presetMin)
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$presetMin Min",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = tint
                        )
                    }
                }
            }
        }

        // TIMER SOUND SELECTOR ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .glassCard(shape = RoundedCornerShape(12.dp))
                .clickable {
                    HapticManager.light(context.applicationContext)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    SoundHapticHelper.playSound269(context)
                    viewModel.showTimerSoundPicker(true)
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = OnSurfaceMuted,
                    modifier = Modifier.size(18.dp)
                )
                Text(text = "Timer Sound", style = BodySm, color = OnSurfaceLight)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rawName = selectedTimerSound
                val timerSoundDisplay = when {
                    rawName.length > 25 -> rawName.take(22) + "..."
                    rawName.contains("content://") -> "Custom Sound"
                    rawName.any { it == '-' } && rawName.length > 20 -> "Custom Sound"
                    else -> rawName
                }
                Text(text = timerSoundDisplay, style = BodySm, color = LiquidOrange)
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = OnSurfaceMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Circular START Button & auxiliary sub-actions (New, Reset)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val buttonText = if (isRunning) "PAUSE" else "START"

            // Central Ring Button
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .glassCard(shape = CircleShape)
                    .border(2.dp, LiquidOrange.copy(alpha = 0.4f), CircleShape)
                    .clickable {
                        HapticManager.medium(context.applicationContext)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundHapticHelper.playSound3124(context)
                        viewModel.startTimer()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!isRunning) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = LiquidOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = buttonText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = LiquidOrange,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sub control links (Reset, New Buttons exactly from Screenshot 7)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // RESET LINK CONTROLLER
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .alpha(0.6f)
                        .clickable {
                            android.util.Log.d("HAPTIC_TEST", "triggered")
                            HapticManager.heavy(context.applicationContext)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            viewModel.resetTimer()
                        }
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "ResetTimer",
                                tint = OnSurfaceLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Reset",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // NEW CONTROLLER
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .alpha(0.6f)
                        .clickable {
                            android.util.Log.d("HAPTIC_TEST", "triggered")
                            HapticManager.heavy(context.applicationContext)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            viewModel.resetTimer()
                        }
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "NewTimer",
                                tint = OnSurfaceLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Add Timer",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.ColumnCard(
    label: String,
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    showDotOverlay: Boolean = false
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .weight(1f)
            .height(132.dp)
            .glassCard(shape = RoundedCornerShape(16.dp))
    ) {
        if (showDotOverlay) {
            // Underlay yellow highlights
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LiquidOrange.copy(alpha = 0.04f))
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = LabelCaps,
                color = OnSurfaceMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = OnSurfaceMuted.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            HapticManager.light(context.applicationContext)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            onDecrement()
                        }
                )
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(durationMillis = 350, easing = EaseInOutCubic)
                        ).togetherWith(
                            fadeOut(
                                animationSpec = tween(durationMillis = 250, easing = EaseInOutCubic)
                            )
                        )
                    },
                    label = "column_card_value"
                ) { targetValue ->
                    Text(
                        text = String.format("%02d", targetValue),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Medium,
                        color = LiquidOrange,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandLess,
                    contentDescription = null,
                    tint = OnSurfaceMuted.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            HapticManager.light(context.applicationContext)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            onIncrement()
                        }
                )
            }
        }
    }
}

private fun formatRemainingTime(remainingMs: Long): String {
    val hrs = (remainingMs / (1000 * 60 * 60)) % 24
    val mins = (remainingMs / (1000 * 60)) % 60
    val secs = (remainingMs / 1000) % 60
    return String.format("%02d:%02d:%02d", hrs, mins, secs)
}
