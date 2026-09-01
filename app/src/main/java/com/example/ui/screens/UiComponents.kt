package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.ui.SoundHapticHelper
import com.example.HapticManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkGrayBg
import com.example.ui.theme.GlassBg
import com.example.ui.theme.GlassBgStrong
import com.example.ui.theme.HeadlineMd
import com.example.ui.theme.LabelCaps
import com.example.ui.theme.LiquidOrange
import com.example.ui.theme.OnSurfaceLight
import com.example.ui.theme.OnSurfaceMuted
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.glassCard
import com.example.ui.theme.glassPill
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders beautiful, blurred drift ambient background blobs matching the liquid design.
 */
@Composable
fun AtmosphericLiquidBg(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "drift")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = Math.PI.toFloat() * 2,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    Box(modifier = modifier.fillMaxSize().background(TrueBlack)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Slow drifting positions for background glow blobs
            val blob1X = w * 0.25f + cos(angle) * (w * 0.08f)
            val blob1Y = h * 0.35f + sin(angle) * (h * 0.08f)
            val blob2X = w * 0.75f - sin(angle) * (w * 0.08f)
            val blob2Y = h * 0.65f - cos(angle * 0.8f) * (h * 0.08f)

            // Orange liquid blob
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LiquidOrange.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(blob1X, blob1Y),
                    radius = w * 0.5f
                ),
                center = Offset(blob1X, blob1Y),
                radius = w * 0.5f
            )

            // Green liquid blob
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF55EE71).copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(blob2X, blob2Y),
                    radius = w * 0.45f
                ),
                center = Offset(blob2X, blob2Y),
                radius = w * 0.45f
            )

            // Subtle Fluid-Motion wave 1: Bottom Green wave
            val wave1Path = Path().apply {
                moveTo(0f, h)
                for (x in 0..w.toInt() step 8) {
                    val xF = x.toFloat()
                    val sine = sin(xF * 0.004f + phase1) * cos(xF * 0.002f + phase2 * 0.5f)
                    val y = h * 0.82f + sine * 25.dp.toPx()
                    lineTo(xF, y)
                }
                lineTo(w, h)
                close()
            }
            drawPath(
                path = wave1Path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PrimaryGreen.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = h * 0.7f,
                    endY = h
                )
            )

            // Subtle Fluid-Motion wave 2: Bottom Orange wave, slightly offset
            val wave2Path = Path().apply {
                moveTo(0f, h)
                for (x in 0..w.toInt() step 8) {
                    val xF = x.toFloat()
                    val sine = sin(xF * 0.003f - phase2) * cos(xF * 0.004f + phase1 * 0.4f)
                    val y = h * 0.85f + sine * 30.dp.toPx()
                    lineTo(xF, y)
                }
                lineTo(w, h)
                close()
            }
            drawPath(
                path = wave2Path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        LiquidOrange.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    startY = h * 0.72f,
                    endY = h
                )
            )

            // Subtle Fluid-Motion wave 3: Middle flowing current (very subtle)
            val wave3Path = Path().apply {
                moveTo(0f, h)
                for (x in 0..w.toInt() step 8) {
                    val xF = x.toFloat()
                    val sine = sin(xF * 0.002f + phase2 * 0.7f) * sin(xF * 0.003f - phase1 * 0.3f)
                    val y = h * 0.5f + sine * 20.dp.toPx()
                    lineTo(xF, y)
                }
                lineTo(w, h)
                close()
            }
            drawPath(
                path = wave3Path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.02f), // Cyan current
                        Color.Transparent
                    ),
                    startY = h * 0.45f,
                    endY = h * 0.75f
                )
            )
        }
    }
}

/**
 * Customizable navigation structure that precisely mirrors the bottom bar from screenshots.
 */
@Composable
fun LiquidGlassBottomNav(
    activeTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val indicatorOffset by animateDpAsState(
        targetValue = when (activeTab) {
            0 -> 0.dp
            1 -> 56.dp
            2 -> 112.dp
            3 -> 168.dp
            else -> 0.dp
        },
        animationSpec = tween(durationMillis = 300),
        label = "navIndicator"
    )

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(80.dp)
            .glassCard(shape = RoundedCornerShape(36.dp))
            .clip(RoundedCornerShape(36.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(288.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            // Draw the floating white pill behind the selected tab
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(120.dp)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp, horizontal = 4.dp)
                    .glassCard(shape = RoundedCornerShape(50))
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                NavItem(
                    label = "WORLD",
                    iconFilled = Icons.Filled.Public,
                    iconOutlined = Icons.Outlined.Public,
                    isActive = activeTab == 0,
                    onClick = { onTabSelected(0) }
                )
                NavItem(
                    label = "ALARMS",
                    iconFilled = Icons.Filled.Alarm,
                    iconOutlined = Icons.Outlined.Alarm,
                    isActive = activeTab == 1,
                    onClick = { onTabSelected(1) }
                )
                NavItem(
                    label = "STOPWATCH",
                    iconFilled = Icons.Filled.Timer,
                    iconOutlined = Icons.Outlined.Timer,
                    isActive = activeTab == 2,
                    onClick = { onTabSelected(2) }
                )
                NavItem(
                    label = "TIMERS",
                    iconFilled = Icons.Filled.HourglassEmpty,
                    iconOutlined = Icons.Outlined.HourglassEmpty,
                    isActive = activeTab == 3,
                    onClick = { onTabSelected(3) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    iconFilled: ImageVector,
    iconOutlined: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val tabWidth by animateDpAsState(
        targetValue = if (isActive) 120.dp else 56.dp,
        animationSpec = tween(durationMillis = 300),
        label = "tabWidth"
    )

    Box(
        modifier = Modifier
            .width(tabWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .clickable {
                Log.d("HAPTIC_TEST", "triggered")
                HapticManager.light(context)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                SoundHapticHelper.playSound269(context)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isActive) iconFilled else iconOutlined,
                contentDescription = label,
                tint = if (isActive) Color.White else Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(20.dp)
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(150))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Large glowing action circle for clock operations.
 */
@Composable
fun GlassIconBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LiquidOrange,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable {
                Log.d("HAPTIC_TEST", "triggered")
                HapticManager.light(context)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                SoundHapticHelper.playSound269(context)
                onClick()
            }
            .glassCard(shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Animated liquid-bouncing volume/equalizer bars visualizer.
 */
@Composable
fun VolumeBarsVisualizer(
    modifier: Modifier = Modifier,
    barCount: Int = 12,
    color: Color = LiquidOrange
) {
    val infiniteTransition = rememberInfiniteTransition(label = "volume_bars")
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val duration = 400 + (i * 70) % 350
            val targetHeightScale by infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue = 0.95f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(targetHeightScale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.9f),
                                color.copy(alpha = 0.3f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun LiquidSoundWaveformOverlay(
    modifier: Modifier = Modifier,
    soundName: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_overlay")

    // General continuous phase animation
    val globalPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * 3.1415927f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "global_phase"
    )

    // Pulse animation for general scaling/brightness
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width == 0f || height == 0f) return@Canvas

        when (soundName) {
            "Dewdrop Serenade" -> {
                // 1. "Dewdrop Serenade": Calm, undulating neon blue/teal curves + rising glowing circles (dewdrops)
                val waveColors = listOf(
                    Color(0xFF00E5FF), // Neon cyan
                    Color(0xFF00BFA5)  // Teal
                )
                
                // Draw 2 layered waves with different phases
                for (layer in 0..1) {
                    val path = Path()
                    val centerY = height * (0.6f + layer * 0.1f)
                    val amplitude = height * 0.12f
                    val phaseOffset = layer * (3.1415927f / 2f)
                    val frequency = 2 * 3.1415927f / width * 1.5f

                    path.moveTo(0f, centerY)
                    for (x in 0..width.toInt() step 6) {
                        val xF = x.toFloat()
                        val y = centerY + sin(xF * frequency + globalPhase + phaseOffset) * amplitude
                        path.lineTo(xF, y)
                    }
                    path.lineTo(width, height)
                    path.lineTo(0f, height)
                    path.close()

                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                waveColors[layer].copy(alpha = 0.25f - (layer * 0.1f)),
                                waveColors[layer].copy(alpha = 0.01f)
                            ),
                            startY = centerY - amplitude,
                            endY = height
                        )
                    )
                }

                // Draw floating dewdrops
                val dropCount = 6
                for (i in 0 until dropCount) {
                    // Staggered vertical progress for each drop
                    val progress = ((globalPhase / (2 * 3.1415927f)) + (i.toFloat() / dropCount)) % 1f
                    val x = width * (0.15f + 0.7f * ((i * 7) % 10 / 10f))
                    // Start from wave region and float up
                    val startY = height * 0.7f
                    val y = startY - (progress * (height * 0.4f))
                    val radius = 6.dp.toPx() * (1f - progress * 0.5f)
                    val alpha = (1f - progress) * 0.5f * glowPulse

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF00E5FF).copy(alpha = alpha), Color.Transparent),
                            center = Offset(x, y),
                            radius = radius * 2
                        ),
                        center = Offset(x, y),
                        radius = radius * 2
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        center = Offset(x, y),
                        radius = radius,
                        style = Fill
                    )
                }
            }
            "Bubble Resonance" -> {
                // 2. "Bubble Resonance": energetic springy vertical bars + cute floating circles
                // Let's draw some bouncy bubbly columns in the lower half of the screen
                val waveColors = listOf(
                    Color(0xFFFF5252), // Coral pink
                    Color(0xFF9C27B0)  // Purple
                )
                
                val barWidth = 14.dp.toPx()
                val gap = 10.dp.toPx()
                val startX = (width - (10 * barWidth + 9 * gap)) / 2f

                for (i in 0 until 10) {
                    val barPhase = globalPhase * 2f + i * 0.6f
                    val dynamicHeight = height * 0.15f + sin(barPhase).coerceAtLeast(-1f) * height * 0.1f
                    val x = startX + i * (barWidth + gap)
                    val y = height * 0.85f - dynamicHeight

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(waveColors[i % 2].copy(alpha = 0.4f), waveColors[i % 2].copy(alpha = 0.05f)),
                            startY = y,
                            endY = height * 0.85f
                        ),
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, dynamicHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                }

                // Rising/popping bubbles
                val bubbleCount = 8
                for (i in 0 until bubbleCount) {
                    val progress = ((globalPhase / (2 * 3.1415927f)) + (i.toFloat() / bubbleCount)) % 1f
                    val baseScale = (i * 3) % 5 + 1
                    val baseSpeedX = sin(globalPhase + i) * 15.dp.toPx()
                    val x = width * (0.1f + 0.8f * ((i * 13) % 17 / 17f)) + baseSpeedX
                    val y = height * 0.9f - progress * (height * 0.6f)
                    
                    val radius = (10 + baseScale * 4).dp.toPx() * (0.5f + 0.5f * sin(progress * 3.1415927f))
                    val alpha = (1f - progress) * 0.4f

                    // Bubble outline
                    drawCircle(
                        color = Color(0xFFFF5252).copy(alpha = alpha),
                        center = Offset(x, y),
                        radius = radius,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Inner glow dot
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.6f),
                        center = Offset(x - radius * 0.3f, y - radius * 0.3f),
                        radius = radius * 0.2f
                    )
                }
            }
            "Ocean Breeze" -> {
                // 3. "Ocean Breeze": Mesmerizing deep rolling oceanic waves with shifting phase
                val deepOceanColors = listOf(
                    Color(0xFF0D47A1), // Deep Blue
                    Color(0xFF00ACC1), // Deep Cyan
                    Color(0xFF00BFA5)  // Teal
                )

                for (layer in 0..2) {
                    val path = Path()
                    val centerY = height * (0.55f + layer * 0.08f)
                    val amplitude = height * (0.14f - layer * 0.03f)
                    val waveSpeed = globalPhase * (1.2f - layer * 0.2f)
                    val frequency = 2 * 3.1415927f / width * (1.2f + layer * 0.3f)

                    path.moveTo(0f, centerY)
                    for (x in 0..width.toInt() step 8) {
                        val xF = x.toFloat()
                        val y = centerY + sin(xF * frequency + waveSpeed) * amplitude
                        path.lineTo(xF, y)
                    }
                    path.lineTo(width, height)
                    path.lineTo(0f, height)
                    path.close()

                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                deepOceanColors[layer].copy(alpha = 0.3f - layer * 0.08f),
                                Color.Transparent
                            ),
                            startY = centerY - amplitude,
                            endY = height
                        )
                    )
                }
            }
            "Liquid Echo" -> {
                // 4. "Liquid Echo": dynamic concentric expanding ripples from center + polar radial bars
                val echoColor = Color(0xFF00E676) // Emerald green
                val centerOffset = Offset(width / 2f, height * 0.55f)

                // Draw expanding echo rings
                for (i in 0..2) {
                    val progress = ((globalPhase / (2 * 3.1415927f)) + (i.toFloat() / 3f)) % 1f
                    val radius = width * 0.45f * progress
                    val alpha = (1f - progress) * 0.35f

                    drawCircle(
                        color = echoColor.copy(alpha = alpha),
                        center = centerOffset,
                        radius = radius,
                        style = Stroke(width = (2.dp.toPx() + progress * 4.dp.toPx()))
                    )
                }

                // Radial polar bars vibrating in echo
                val rayCount = 36
                val minRadius = width * 0.12f
                val maxRadius = width * 0.22f
                for (r in 0 until rayCount) {
                    val angle = r * (2 * 3.1415927f / rayCount)
                    val vibration = sin(globalPhase * 3f + r * 1.5f) * 12.dp.toPx() * glowPulse
                    val currentMaxRadius = maxRadius + vibration

                    val startX = centerOffset.x + minRadius * cos(angle)
                    val startY = centerOffset.y + minRadius * sin(angle)
                    val endX = centerOffset.x + currentMaxRadius * cos(angle)
                    val endY = centerOffset.y + currentMaxRadius * sin(angle)

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(echoColor.copy(alpha = 0.5f), Color.Transparent),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
            "Rainfall Melody" -> {
                // 5. "Rainfall Melody": elegant vertical rain streaks + splashing bottom waves + jittery oscilloscope baseline
                val rainColor = Color(0xFF80DEEA) // Ice Blue

                // Oscilloscope jitter waveform at bottom
                val path = Path()
                val centerY = height * 0.82f
                path.moveTo(0f, centerY)
                for (x in 0..width.toInt() step 4) {
                    val xF = x.toFloat()
                    // Create high-frequency noise-like jitter
                    val noise = sin(xF * 0.15f + globalPhase * 12f) * sin(xF * 0.04f)
                    val y = centerY + noise * 18.dp.toPx()
                    path.lineTo(xF, y)
                }
                path.lineTo(width, height)
                path.lineTo(0f, height)
                path.close()

                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(rainColor.copy(alpha = 0.25f), Color.Transparent),
                        startY = centerY - 20.dp.toPx(),
                        endY = height
                    )
                )

                // Dropping neon lines
                val rainCount = 15
                for (r in 0 until rainCount) {
                    val progress = ((globalPhase * 1.5f / (2 * PI).toFloat()) + (r.toFloat() / rainCount)) % 1f
                    val x = width * (0.05f + 0.9f * ((r * 29) % 31 / 31f))
                    val startY = progress * height * 0.8f
                    val lineLength = 22.dp.toPx()
                    val endY = startY + lineLength
                    
                    if (endY < centerY) {
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, rainColor.copy(alpha = 0.35f)),
                                startY = startY,
                                endY = endY
                            ),
                            start = Offset(x, startY),
                            end = Offset(x, endY),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    } else {
                        // Splashing effect if rain hits baseline
                        val splashProgress = (endY - centerY) / 40.dp.toPx()
                        if (splashProgress in 0f..1f) {
                            val splashRadius = 8.dp.toPx() * splashProgress
                            drawCircle(
                                color = rainColor.copy(alpha = (1f - splashProgress) * 0.4f),
                                center = Offset(x, centerY),
                                radius = splashRadius,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }
            }
            else -> {
                // Default / Normal Ringtone: Floating organic morphing blob in center background
                val blobColor = com.example.ui.theme.LiquidOrange
                val centerOffset = Offset(width / 2f, height * 0.55f)

                val path = Path()
                val baseRadius = width * 0.25f
                val vertexCount = 48

                for (i in 0 until vertexCount) {
                    val angle = i * (2 * 3.1415927f / vertexCount)
                    // Morphing radius using trigonometric harmony
                    val waveOffset1 = sin(angle * 3f + globalPhase) * 22.dp.toPx()
                    val waveOffset2 = cos(angle * 5f - globalPhase * 1.5f) * 14.dp.toPx()
                    val r = baseRadius + waveOffset1 + waveOffset2

                    val x = centerOffset.x + r * cos(angle)
                    val y = centerOffset.y + r * sin(angle)

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                path.close()

                // Draw the morphing blob as glass-like glow fill
                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            blobColor.copy(alpha = 0.15f * glowPulse),
                            blobColor.copy(alpha = 0.01f)
                        ),
                        center = centerOffset,
                        radius = baseRadius * 1.4f
                    )
                )

                // Stroke outline
                drawPath(
                    path = path,
                    color = blobColor.copy(alpha = 0.25f * glowPulse),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun LiquidGlassNavRail(
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val tabs = listOf(
        Triple(Icons.Filled.Public,        Icons.Outlined.Public,        "WORLD"),
        Triple(Icons.Filled.Alarm,         Icons.Outlined.Alarm,         "ALARMS"),
        Triple(Icons.Filled.Timer,         Icons.Outlined.Timer,         "STOP"),
        Triple(Icons.Filled.HourglassEmpty,Icons.Outlined.HourglassEmpty,"TIMERS"),
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 12.dp, top = 24.dp, bottom = 24.dp)
            .width(80.dp)
            .glassCard(
                shape = RoundedCornerShape(28.dp),
                bgColor = GlassBg
            )
            .padding(vertical = 16.dp, horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        tabs.forEachIndexed { index, (iconFilled, iconOutlined, label) ->
            val isActive = activeTab == index
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isActive)
                            Modifier.glassCard(
                                shape = RoundedCornerShape(20.dp),
                                bgColor = PrimaryGreen.copy(alpha = 0.12f)
                            )
                        else
                            Modifier
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        HapticManager.light(context)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundHapticHelper.playSound269(context)
                        onTabSelected(index)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isActive) iconFilled else iconOutlined,
                        contentDescription = label,
                        tint = if (isActive) PrimaryGreen else Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(22.dp)
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 9.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                color = PrimaryGreen
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

