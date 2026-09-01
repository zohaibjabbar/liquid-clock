package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.ui.SoundHapticHelper
import com.example.HapticManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ClockViewModel
import com.example.ui.theme.BodyLg
import com.example.ui.theme.DisplayTimerMobile
import com.example.ui.theme.OnSurfaceLight
import com.example.ui.theme.OnSurfaceMuted
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SecondaryRed
import com.example.ui.theme.SurfaceGray
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.glassCard
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun StopwatchTab(viewModel: ClockViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val totalTimeMs by viewModel.stopwatchTimeMs.collectAsState()
    val isRunning by viewModel.stopwatchIsRunning.collectAsState()
    val laps by viewModel.stopwatchLaps.collectAsState()

    // Find the min/max lap values to highlight fastest/slowest dynamically
    val fastestLapIdx = if (laps.size > 1) laps.indexOf(laps.minOrNull() ?: 0L) else -1
    val slowestLapIdx = if (laps.size > 1) laps.indexOf(laps.maxOrNull() ?: 0L) else -1

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val isLandscape = maxHeight < 400.dp

        // Ambient procedural liquid wave background running dynamically
        SubtleLiquidWaveBackground(
            elapsedMs = totalTimeMs,
            isRunning = isRunning,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(
                    top = if (isLandscape) 8.dp else 56.dp,
                    bottom = if (isLandscape) 16.dp else 100.dp
                )
        ) {
        // App header (Stopwatch, triple dot context button menu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stopwatch",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight,
                letterSpacing = (-0.5).sp
            )
        }

        // Digital display panel (Matches 00:17.26 big spacing on top third axis)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (isLandscape) 8.dp else 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatStopwatchTime(totalTimeMs),
                style = DisplayTimerMobile.copy(
                    fontSize = if (isLandscape) 48.sp else 72.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-2).sp
                ),
                color = OnSurfaceLight,
                textAlign = TextAlign.Center
            )
        }

        // Physical concentric circular badges (Start/Stop, Reset/Lap controls)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset / Lap Circle Button (Secondary neutral background)
            val resetText = if (isRunning) "Lap" else "Reset"
            val buttonSize = if (isLandscape) 56.dp else 80.dp
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .clickable {
                        android.util.Log.d("HAPTIC_TEST", "triggered")
                        if (isRunning) {
                            HapticManager.medium(context.applicationContext)
                        } else {
                            HapticManager.heavy(context.applicationContext)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundHapticHelper.playSound269(context)
                        if (isRunning) {
                            viewModel.lapStopwatch()
                        } else {
                            viewModel.resetStopwatch()
                        }
                    }
                    .background(Color.White.copy(alpha = 0.08f))
                    .glassCard(shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = resetText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceLight
                )
            }

            // Swipable navigation dots (Exactly matching visual markers from Stopwatch mockup)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(OnSurfaceLight)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                )
            }

            // Start / Stop Circle Button (Primary high chroma green/red state toggling)
            val startText = if (isRunning) "Stop" else "Start"
            val textCol by animateColorAsState(
                targetValue = if (isRunning) SecondaryRed else PrimaryGreen,
                animationSpec = tween(350),
                label = "stopwatch_btn_text_color"
            )
            val btnBgAlpha by animateFloatAsState(
                targetValue = if (isRunning) 0.16f else 0.08f,
                animationSpec = tween(350),
                label = "stopwatch_btn_bg_alpha"
            )
            val btnBgColor = if (isRunning) SecondaryRed.copy(alpha = btnBgAlpha) else PrimaryGreen.copy(alpha = btnBgAlpha)

            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .clickable {
                        android.util.Log.d("HAPTIC_TEST", "triggered")
                        HapticManager.medium(context.applicationContext)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundHapticHelper.playSound3124(context)
                        viewModel.startStopwatch()
                    }
                    .background(Color.White.copy(alpha = 0.04f))
                    .background(btnBgColor)
                    .glassCard(shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = startText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = textCol
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // History Laps Scroll Layout (Fastest Green, Slowest Red indicators)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = laps,
                key = { index, _ -> laps.size - index }
            ) { idx, lapTime ->
                val displayLapIndex = laps.size - idx
                val rowColor = when (idx) {
                    fastestLapIdx -> PrimaryGreen
                    slowestLapIdx -> SecondaryRed
                    else -> OnSurfaceLight
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .animateItem()
                        .glassCard(shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lap $displayLapIndex",
                        style = BodyLg,
                        color = rowColor,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = formatLapTime(lapTime),
                        style = BodyLg.copy(fontFamily = FontFamily.Monospace),
                        color = rowColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
}

@Composable
fun SubtleLiquidWaveBackground(
    elapsedMs: Long,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_ambient")

    // Smooth continuous loop of animation offsets matching fluid motion
    val waveOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset_1"
    )

    val waveOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -(2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset_2"
    )

    // Compute active wave intensity: gets higher and more active as elapsed time advances!
    // We let the wave amplitude and speed swell smoothly based on the stopwatch time.
    val progressPeriod = 60000f // resets/cycles visual swell every 60 seconds
    val baseIntensity = if (isRunning) {
        // Ranges between 0.35f and 1.1f smoothly as timer travels up to 60s
        0.35f + 0.75f * ((elapsedMs % progressPeriod) / progressPeriod)
    } else if (elapsedMs > 0L) {
        // Preservation intensity level when paused
        0.3f + 0.3f * ((elapsedMs % progressPeriod) / progressPeriod)
    } else {
        // Silent beautiful background ripple in standby
        0.18f
    }

    val animatedIntensity by animateFloatAsState(
        targetValue = baseIntensity,
        animationSpec = tween(durationMillis = 600),
        label = "wave_intensity"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Fill level scales slightly: wave rises slowly as time progresses (up to 3 minutes)
        val fillFraction = if (isRunning || elapsedMs > 0) {
            0.87f - 0.15f * minOf(1.0f, elapsedMs / 180000f)
        } else {
            0.89f
        }

        val baselineY = height * fillFraction

        // Wave 1: primary flowing wave
        val wavePath1 = Path()
        val amplitude1 = 16.dp.toPx() * animatedIntensity
        val wavelength1 = width * 1.25f

        wavePath1.moveTo(0f, baselineY)
        var x = 0f
        val step = 12f
        while (x <= width) {
            val relativeX = x / wavelength1
            val y = baselineY + amplitude1 * sin(relativeX * 2 * PI.toFloat() + waveOffset1)
            wavePath1.lineTo(x, y)
            x += step
        }
        wavePath1.lineTo(width, height)
        wavePath1.lineTo(0f, height)
        wavePath1.close()

        // Wave 2: secondary visual counter-wave
        val wavePath2 = Path()
        val amplitude2 = 10.dp.toPx() * (animatedIntensity * 0.75f + 0.15f)
        val wavelength2 = width * 0.85f

        wavePath2.moveTo(0f, baselineY)
        x = 0f
        while (x <= width) {
            val relativeX = x / wavelength2
            val y = baselineY + amplitude2 * sin(relativeX * 2 * PI.toFloat() + waveOffset2 + (PI / 3).toFloat())
            wavePath2.lineTo(x, y)
            x += step
        }
        wavePath2.lineTo(width, height)
        wavePath2.lineTo(0f, height)
        wavePath2.close()

        // Paint gorgeous translucent PrimaryGreen waves in the background
        drawPath(
            path = wavePath1,
            color = PrimaryGreen.copy(alpha = 0.08f * animatedIntensity + 0.02f)
        )

        drawPath(
            path = wavePath2,
            color = PrimaryGreen.copy(alpha = 0.12f * animatedIntensity + 0.03f)
        )
    }
}

// Stopwatch formatting utils (Centisecond accuracy)
private fun formatStopwatchTime(elapsedMs: Long): String {
    val hrs = (elapsedMs / (1000 * 60 * 60)) % 24
    val mins = (elapsedMs / (1000 * 60)) % 60
    val secs = (elapsedMs / 1000) % 60
    val micro = (elapsedMs / 10) % 100

    return if (hrs > 0) {
        String.format("%02d:%02d:%02d.%02d", hrs, mins, secs, micro)
    } else {
        String.format("%02d:%02d.%02d", mins, secs, micro)
    }
}

private fun formatLapTime(lapMs: Long): String {
    val mins = (lapMs / (1000 * 60)) % 60
    val secs = (lapMs / 1000) % 60
    val micro = (lapMs / 10) % 100
    return String.format("%02d:%02d.%02d", mins, secs, micro)
}
