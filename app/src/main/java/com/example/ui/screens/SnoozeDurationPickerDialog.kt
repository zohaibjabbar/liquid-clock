package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.ui.SoundHapticHelper
import com.example.HapticManager
import androidx.compose.ui.window.DialogWindowProvider
import android.view.WindowManager
import com.example.ui.theme.LiquidOrange
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.OnSurfaceLight
import com.example.ui.theme.BodySm
import com.example.ui.theme.BodyLg
import com.example.ui.theme.glassCard
import com.example.ui.theme.glassStrongCard
import kotlinx.coroutines.launch

data class SnoozeOption(val minutes: Int, val label: String)

@Composable
fun SnoozeDurationPickerDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int, label: String) -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val options = remember {
        listOf(
            SnoozeOption(3, "3 Minutes"),
            SnoozeOption(5, "5 Minutes"),
            SnoozeOption(9, "9 Minutes"),
            SnoozeOption(10, "10 Minutes"),
            SnoozeOption(15, "15 Minutes"),
            SnoozeOption(30, "30 Minutes")
        )
    }

    var selectedOption by remember {
        mutableStateOf(options.find { it.minutes == currentMinutes } ?: options[2])
    }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {} // Absolutely block background interaction
            .background(TrueBlack)
    ) {
            // Centered Column containing the Title and Snooze Options
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 40.dp, bottom = 100.dp), // Extra bottom padding to ensure no overlap with bottom buttons
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Screen Title
                Text(
                    text = "Snooze Duration",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { item ->
                        val isSelected = item.minutes == selectedOption.minutes
                        val scale = remember { Animatable(1f) }
                        val itemModifier = Modifier
                            .fillMaxWidth()
                            .scale(scale.value)
                            .clip(RoundedCornerShape(12.dp))
                            .glassCard(shape = RoundedCornerShape(12.dp))

                        Row(
                            modifier = itemModifier
                                .clickable {
                                    HapticManager.light(context)
                                    scope.launch {
                                        scale.animateTo(0.95f, tween(80))
                                        scale.animateTo(1f, tween(80))
                                    }
                                    selectedOption = item
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.label,
                                style = BodySm,
                                color = if (isSelected) LiquidOrange else OnSurfaceLight,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Selected",
                                    tint = LiquidOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Actions Cancel / Select comfortably positioned at the bottom of the screen
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp), // Comfortable bottom margin from screen edge
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                 Button(
                    onClick = {
                        android.util.Log.d("HAPTIC_TEST", "triggered")
                        HapticManager.heavy(context)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundHapticHelper.playSound269(context)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Cancel", style = BodyLg, color = OnSurfaceLight)
                }

                Button(
                    onClick = {
                        android.util.Log.d("HAPTIC_TEST", "triggered")
                        HapticManager.medium(context)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundHapticHelper.playSound3124(context)
                        onConfirm(selectedOption.minutes, selectedOption.label)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .glassStrongCard(shape = RoundedCornerShape(12.dp))
                ) {
                    Text(text = "Select", style = BodyLg, color = LiquidOrange, fontWeight = FontWeight.Bold)
                }
            }
        }
}
