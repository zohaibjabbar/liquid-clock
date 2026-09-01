package com.example.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import com.example.HapticManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DisplayTimer
import com.example.ui.theme.LabelCaps
import com.example.ui.theme.LiquidOrange
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.glassCard
import com.example.ui.screens.VolumeBarsVisualizer

class TimerRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force full screen and display on top of keyguard/lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MyApplicationTheme {
                TimerRingingScreen(
                    onDismiss = {
                        try {
                            val dismissIntent = Intent(this@TimerRingingActivity, TimerService::class.java).apply {
                                action = TimerService.ACTION_DISMISS
                            }
                            startService(dismissIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        try {
                            val mainIntent = Intent(this@TimerRingingActivity, com.example.MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("SELECT_TAB", 3)
                                putExtra("RESET_TIMER_TO_ZERO", true)
                            }
                            startActivity(mainIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun TimerRingingScreen(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Elegant infinite swing and scale animation for the bell icon
    val transition = rememberInfiniteTransition(label = "bell_ring_transition")
    
    val rotation by transition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(220, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell_rotation"
    )

    val scale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(440, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section - Spacer to align
            Spacer(modifier = Modifier.height(48.dp))

            // Middle Display with glassmorphic aesthetics
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Timer Finished".uppercase(),
                    style = LabelCaps.copy(
                        color = Color.White,
                        letterSpacing = 3.sp,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "00:00",
                    style = DisplayTimer.copy(
                        color = Color.White,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Light
                    ),
                    modifier = Modifier.padding(bottom = 48.dp)
                )

                // Animated Ringing Bell Icon and Volume Bars
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .glassCard(shape = RoundedCornerShape(32.dp), bgColor = Color.White.copy(alpha = 0.02f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlarmOn,
                            contentDescription = "Ringing Bell Icon",
                            tint = LiquidOrange,
                            modifier = Modifier
                                .size(68.dp)
                                .scale(scale)
                                .graphicsLayer(rotationZ = rotation)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        VolumeBarsVisualizer(
                            modifier = Modifier.height(36.dp).fillMaxWidth(0.85f),
                            barCount = 10,
                            color = LiquidOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Your countdown timer has completed.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom Section - Dismiss Button (match Alarm STOP styling exactly)
            Box(
                modifier = Modifier
                    .padding(bottom = 48.dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(LiquidOrange.copy(alpha = 0.15f))
                    .border(1.dp, LiquidOrange.copy(alpha = 0.3f), CircleShape)
                    .clickable {
                        HapticManager.heavy(context.applicationContext)
                        onDismiss()
                    }
                    .testTag("timer_dismiss_button"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(LiquidOrange.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DISMISS",
                        style = LabelCaps,
                        color = TrueBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }
    }
}
