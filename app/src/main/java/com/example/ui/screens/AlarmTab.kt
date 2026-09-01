package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.ui.SoundHapticHelper
import com.example.HapticManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.AlarmEntity
import com.example.ui.AlarmScheduler
import java.util.Calendar
import com.example.ui.ClockViewModel
import com.example.ui.LiquidSoundSynth
import com.example.ui.theme.BodyLg
import com.example.ui.theme.BodySm
import com.example.ui.theme.DarkGrayBg
import com.example.ui.theme.DisplayTimerMobile
import com.example.ui.theme.HeadlineMd
import com.example.ui.theme.LabelCaps
import com.example.ui.theme.LiquidOrange
import com.example.ui.theme.OnSurfaceLight
import com.example.ui.theme.OnSurfaceMuted
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SecondaryRed
import com.example.ui.theme.SurfaceGray
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.glassCard
import com.example.ui.theme.glassPill
import com.example.ui.theme.glassStrongCard

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmTab(viewModel: ClockViewModel) {
    val context = LocalContext.current
    val alarms by viewModel.alarms.collectAsState()
    val isShowingAlarmSetup by viewModel.isShowingAlarmSetup.collectAsState()
    val editingAlarm by viewModel.editingAlarm.collectAsState()
    val ringingAlarm by viewModel.ringingAlarm.collectAsState()

    val sleepAlarms = remember(alarms) {
        alarms.filter { it.label.lowercase().contains("sleep") }
    }
    val otherAlarms = remember(alarms) {
        alarms.filter { !it.label.lowercase().contains("sleep") }
    }

    var triggerUpdate by remember { mutableStateOf(0) }

    LaunchedEffect(alarms) {
        triggerUpdate++
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            triggerUpdate++
        }
    }

    val countdownText = remember(alarms, triggerUpdate) {
        val now = Calendar.getInstance()
        val nextAlarmTime = alarms
            .filter { alarm: AlarmEntity -> alarm.isEnabled }
            .map { alarm: AlarmEntity -> AlarmScheduler.getNextTriggerMillis(alarm) }
            .minByOrNull { time: Long -> time }

        nextAlarmTime?.let { time: Long ->
            val diff = time - now.timeInMillis
            val hours = diff / (1000 * 60 * 60)
            val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
            "In ${hours}h ${minutes}m"
        } ?: "No alarms"
    }

    val bedtimeText = remember(alarms) {
        val morningAlarm = alarms
            .filter { it.isEnabled && it.isAm }
            .minByOrNull { 
                val h24 = if (it.hour == 12) 0 else it.hour
                h24 * 60 + it.minute
            }

        morningAlarm?.let {
            val hour = it.hour
            val minute = String.format("%02d", it.minute)
            val amPm = if (it.isAm) "AM" else "PM"
            "$hour:$minute $amPm"
        } ?: "--:--"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val blurRadius by animateDpAsState(
            targetValue = if (isShowingAlarmSetup || editingAlarm != null) 16.dp else 0.dp,
            animationSpec = tween(durationMillis = 300),
            label = "alarm_screen_blur"
        )

        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        // Main Listings View
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
                .padding(horizontal = 16.dp)
                .padding(
                    top = if (isLandscape) 8.dp else 56.dp,
                    bottom = if (isLandscape) 8.dp else 100.dp
                )
        ) {
            // Header Group (Exact layout parameters)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alarm",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight,
                    letterSpacing = (-0.5).sp
                )

                // '+' Rounded glass action button
                GlassIconBtn(
                    icon = Icons.Default.Add,
                    onClick = { viewModel.showAlarmSetup(true) },
                    tint = LiquidOrange
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sleep | Wake Up Section
                if (sleepAlarms.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = OnSurfaceMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SLEEP | WAKE UP",
                                    style = LabelCaps,
                                    color = OnSurfaceMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    items(
                        items = sleepAlarms,
                        key = { it.id }
                    ) { alarmObj ->
                        AlarmRowItem(
                            alarm = alarmObj,
                            viewModel = viewModel,
                            onDelete = { viewModel.deleteAlarm(alarmObj) },
                            onRowClick = { viewModel.showEditingAlarm(alarmObj) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }

                // Other Alarms Section
                if (otherAlarms.isNotEmpty()) {
                    if (sleepAlarms.isNotEmpty()) {
                        item {
                            Text(
                                text = "OTHER",
                                style = LabelCaps,
                                color = OnSurfaceMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    items(
                        items = otherAlarms,
                        key = { it.id }
                    ) { alarmObj ->
                        AlarmRowItem(
                            alarm = alarmObj,
                            viewModel = viewModel,
                            onDelete = { viewModel.deleteAlarm(alarmObj) },
                            onRowClick = { viewModel.showEditingAlarm(alarmObj) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }

                // Bento Style double stats blocks (Visual Decoration / Bento Style Info)
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // NEXT UP Bento Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(116.dp)
                                .glassCard(shape = RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "NEXT UP",
                                    style = LabelCaps,
                                    color = OnSurfaceMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = countdownText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryGreen
                                )
                            }
                            // Icon ghost watermark bottom right (10% opacity)
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = OnSurfaceMuted.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .size(72.dp)
                                    .align(Alignment.BottomEnd)
                             )
                        }

                        // BEDTIME Bento Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(116.dp)
                                .glassCard(shape = RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "BEDTIME",
                                    style = LabelCaps,
                                    color = OnSurfaceMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = bedtimeText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LiquidOrange
                                )
                            }
                            // Icon ghost watermark bottom right (10% opacity)
                            Icon(
                                imageVector = Icons.Default.Bed,
                                contentDescription = null,
                                tint = OnSurfaceMuted.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .size(72.dp)
                                    .align(Alignment.BottomEnd)
                            )
                        }
                    }
                }
            }
        }

        // Overlay Alarm Picker dialog
        AnimatedVisibility(
            visible = isShowingAlarmSetup,
            enter = fadeIn(tween(350)) + slideInVertically(animationSpec = tween(350)) { height -> (height * 0.15f).toInt() },
            exit = fadeOut(tween(250)) + slideOutVertically(animationSpec = tween(300)) { height -> (height * 0.15f).toInt() }
        ) {
            AlarmSetupDialog(
                onDismiss = { viewModel.showAlarmSetup(false) },
                onSave = { h, m, isAm, repeats, label, sound ->
                    viewModel.addNewAlarm(h, m, isAm, repeats, label, sound)
                }
            )
        }

        // Overlay Alarm Edit dialog
        AnimatedVisibility(
            visible = editingAlarm != null,
            enter = fadeIn(tween(350)) + slideInVertically(animationSpec = tween(350)) { height -> (height * 0.15f).toInt() },
            exit = fadeOut(tween(250)) + slideOutVertically(animationSpec = tween(300)) { height -> (height * 0.15f).toInt() }
        ) {
            AlarmSetupDialog(
                alarmToEdit = editingAlarm,
                onDismiss = { viewModel.showEditingAlarm(null) },
                onSave = { h, m, isAm, repeats, label, sound ->
                    editingAlarm?.let { currentEdit ->
                        viewModel.saveEditedAlarm(currentEdit.id, h, m, isAm, repeats, label, sound, currentEdit.isEnabled)
                    }
                }
            )
        }
    }
}

@Composable
private fun AlarmRowItem(
    alarm: AlarmEntity,
    viewModel: ClockViewModel,
    onDelete: (AlarmEntity) -> Unit,
    onRowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val rowAlpha by animateFloatAsState(
        targetValue = if (alarm.isEnabled) 1.0f else 0.5f,
        animationSpec = tween(300),
        label = "alarm_row_alpha"
    )

    SwipeToRevealItem(
        onDelete = { onDelete(alarm) },
        modifier = modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(24.dp))
                .clickable {
                    android.util.Log.d("HAPTIC_TEST", "triggered")
                    HapticManager.light(context.applicationContext)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    SoundHapticHelper.playSound269(context)
                    onRowClick()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${alarm.hour}:${String.format("%02d", alarm.minute)}",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Light,
                        color = OnSurfaceLight,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (alarm.isAm) "AM" else "PM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = OnSurfaceLight
                    )
                }
                Text(
                    text = "${alarm.label} • ${getRepeatLabel(alarm.repeatDays)}",
                    style = BodySm,
                    color = OnSurfaceMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                LiquidGlassToggle(
                    checked = alarm.isEnabled,
                    onCheckedChange = { newValue ->
                        HapticManager.doubleClick(context.applicationContext)
                        viewModel.toggleAlarm(alarm.id, newValue)
                    }
                )
            }
        }
    }
}

@Composable
fun DrumTimeWheel(
    value: Int,
    isHour: Boolean,
    onValueChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var accumulatedDrag by remember { mutableStateOf(0f) }
    val dragThreshold = 35f // sensitiveness for smooth high-fidelity response

    fun adjustValue(delta: Int) {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            SoundHapticHelper.playSound269(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (isHour) {
            var newVal = value + delta
            while (newVal < 1) newVal += 12
            while (newVal > 12) newVal -= 12
            onValueChange(newVal)
        } else {
            var newVal = value + delta
            while (newVal < 0) newVal += 60
            while (newVal > 59) newVal -= 60
            onValueChange(newVal)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(90.dp, 172.dp)
            .glassCard(shape = RoundedCornerShape(20.dp))
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // PLUS Button (+)
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Increment",
            tint = OnSurfaceMuted,
            modifier = Modifier
                .size(28.dp)
                .clickable {
                    HapticManager.light(context.applicationContext)
                    adjustValue(1)
                }
                .padding(4.dp)
        )

        // Drum Roller / Swipe area
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(value) {
                    detectVerticalDragGestures(
                        onDragStart = { accumulatedDrag = 0f },
                        onDragEnd = { accumulatedDrag = 0f },
                        onDragCancel = { accumulatedDrag = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDrag += dragAmount
                            if (accumulatedDrag > dragThreshold) {
                                adjustValue(-1)
                                accumulatedDrag = 0f
                            } else if (accumulatedDrag < -dragThreshold) {
                                adjustValue(1)
                                accumulatedDrag = 0f
                            }
                        }
                    )
                }
        ) {
            val prevVal = if (isHour) {
                if (value == 1) 12 else value - 1
            } else {
                if (value == 0) 59 else value - 1
            }
            val nextVal = if (isHour) {
                if (value == 12) 1 else value + 1
            } else {
                if (value == 59) 0 else value + 1
            }

            // Top item
            Text(
                text = String.format("%02d", prevVal),
                fontSize = 15.sp,
                color = OnSurfaceMuted.copy(alpha = 0.35f),
                fontWeight = FontWeight.Normal
            )

            // Center item (Active)
            Text(
                text = String.format("%02d", value),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Bottom item
            Text(
                text = String.format("%02d", nextVal),
                fontSize = 15.sp,
                color = OnSurfaceMuted.copy(alpha = 0.35f),
                fontWeight = FontWeight.Normal
            )
        }

        // MINUS Button (-)
        Icon(
            imageVector = Icons.Default.Remove,
            contentDescription = "Decrement",
            tint = OnSurfaceMuted,
            modifier = Modifier
                .size(28.dp)
                .clickable {
                    HapticManager.light(context.applicationContext)
                    adjustValue(-1)
                }
                .padding(4.dp)
        )
    }
}

@Composable
fun AlarmSetupDialog(
    alarmToEdit: AlarmEntity? = null,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Boolean, Set<Int>, String, String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("clock_settings", android.content.Context.MODE_PRIVATE) }
    
    val defaultAlarmSoundName = remember(alarmToEdit) {
        prefs.getString("default_alarm_sound_name", "Starlight") ?: "Starlight"
    }
    val defaultAlarmSoundUri = remember(alarmToEdit) {
        prefs.getString("default_alarm_sound_uri", "") ?: ""
    }
    
    var hour by remember { mutableStateOf(alarmToEdit?.hour ?: 6) }
    var minute by remember { mutableStateOf(alarmToEdit?.minute ?: 45) }
    var isAm by remember { mutableStateOf(alarmToEdit?.isAm ?: true) }
    var label by remember { mutableStateOf(alarmToEdit?.label ?: "Morning Routine") }
    var sound by remember { mutableStateOf(alarmToEdit?.sound ?: defaultAlarmSoundName) }
    var soundChanged by remember { mutableStateOf(false) }

    fun getSoundDisplayName(soundValue: String): String {
        return com.example.ui.screens.getSoundDisplayName(context, soundValue)
    }

    var showLabelDialog by remember { mutableStateOf(false) }
    var tempLabel by remember { mutableStateOf("") }
    var showSoundDialog by remember { mutableStateOf(false) }

    // Repeat Days selection (Glows amber when active)
    val selectedDays = remember(alarmToEdit) {
        mutableStateOf(alarmToEdit?.repeatDays ?: setOf(0, 1, 2, 3, 4))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {} // Absolutely block background interaction
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(top = 40.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Screen Title
            Text(
                text = if (alarmToEdit != null) "Edit Alarm" else "New Alarm",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Pickers wrappers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour Container
                DrumTimeWheel(
                    value = hour,
                    isHour = true,
                    onValueChange = { hour = it }
                )

                // Orange Colon
                Text(
                    text = ":",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = LiquidOrange,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                // Minute Container
                DrumTimeWheel(
                    value = minute,
                    isHour = false,
                    onValueChange = { minute = it }
                )
            }

            // AM/PM Toggle Capsule (Matches Screen 3)
            Row(
                modifier = Modifier
                    .glassPill(bgColor = Color.White.copy(alpha = 0.04f))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val amBgColor by animateColorAsState(
                    targetValue = if (isAm) LiquidOrange.copy(alpha = 0.2f) else Color.Transparent,
                    animationSpec = tween(200),
                    label = "am_bg"
                )
                val amTextColor by animateColorAsState(
                    targetValue = if (isAm) LiquidOrange else OnSurfaceMuted,
                    animationSpec = tween(200),
                    label = "am_text"
                )
                val pmBgColor by animateColorAsState(
                    targetValue = if (!isAm) LiquidOrange.copy(alpha = 0.2f) else Color.Transparent,
                    animationSpec = tween(200),
                    label = "pm_bg"
                )
                val pmTextColor by animateColorAsState(
                    targetValue = if (!isAm) LiquidOrange else OnSurfaceMuted,
                    animationSpec = tween(200),
                    label = "pm_text"
                )

                Box(
                    modifier = Modifier
                        .size(80.dp, 36.dp)
                        .clip(CircleShape)
                        .background(amBgColor)
                        .clickable {
                            android.util.Log.d("HAPTIC_TEST", "triggered")
                            HapticManager.light(context.applicationContext)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            isAm = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AM",
                        style = LabelCaps,
                        color = amTextColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(80.dp, 36.dp)
                        .clip(CircleShape)
                        .background(pmBgColor)
                        .clickable {
                            android.util.Log.d("HAPTIC_TEST", "triggered")
                            HapticManager.light(context.applicationContext)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            isAm = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PM",
                        style = LabelCaps,
                        color = pmTextColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // REPEAT DAYS Section with Amber glowing circles
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "REPEAT DAYS",
                    style = LabelCaps,
                    color = OnSurfaceMuted.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    days.forEachIndexed { idx, day ->
                        val selected = selectedDays.value.contains(idx)

                        val animatedDayColor by animateColorAsState(
                            targetValue = if (selected) LiquidOrange else Color.Transparent,
                            animationSpec = tween(250),
                            label = "day_color_glow"
                        )
                        val animatedTextCol by animateColorAsState(
                            targetValue = if (selected) TrueBlack else OnSurfaceLight.copy(alpha = 0.6f),
                            animationSpec = tween(250),
                            label = "day_text_glow"
                        )

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(animatedDayColor)
                                .clickable {
                                    android.util.Log.d("HAPTIC_TEST", "triggered")
                                    HapticManager.light(context.applicationContext)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    SoundHapticHelper.playSound269(context)
                                    if (selectedDays.value.contains(idx)) {
                                        selectedDays.value = selectedDays.value - idx
                                    } else {
                                        selectedDays.value = selectedDays.value + idx
                                    }
                                }
                                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = animatedTextCol
                            )
                        }
                    }
                }
            }

            // Text Inputs Area
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(shape = RoundedCornerShape(12.dp))
                        .clickable {
                            android.util.Log.d("HAPTIC_TEST", "triggered")
                            HapticManager.light(context.applicationContext)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            tempLabel = label
                            showLabelDialog = true
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Label", style = BodySm, color = OnSurfaceMuted)
                    Text(text = label, style = BodySm, color = OnSurfaceLight)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(shape = RoundedCornerShape(12.dp))
                        .clickable {
                            android.util.Log.d("HAPTIC_TEST", "triggered")
                            HapticManager.light(context.applicationContext)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            showSoundDialog = true
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Sound", style = BodySm, color = OnSurfaceMuted)
                    Text(text = getSoundDisplayName(sound), style = BodySm, color = LiquidOrange)
                }
            }

            // Actions Cancel / Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        android.util.Log.d("HAPTIC_TEST", "triggered")
                        HapticManager.heavy(context.applicationContext)
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
                        HapticManager.medium(context.applicationContext)
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        SoundHapticHelper.playSound3124(context)
                        
                        val finalSound = if (alarmToEdit == null) {
                            if (!soundChanged) {
                                if (defaultAlarmSoundUri.isNotEmpty()) defaultAlarmSoundUri else sound
                            } else {
                                sound
                            }
                        } else {
                            sound
                        }
                        
                        onSave(hour, minute, isAm, selectedDays.value, label, finalSound)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .glassStrongCard(shape = RoundedCornerShape(12.dp))
                ) {
                    Text(text = "Save", style = BodyLg, color = LiquidOrange, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Label Editing Interactive Dialog ---
        if (showLabelDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showLabelDialog = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .glassCard(shape = RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Edit Label",
                            style = BodyLg,
                            color = OnSurfaceLight,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard(shape = RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(12.dp)
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = tempLabel,
                                onValueChange = { tempLabel = it },
                                textStyle = BodySm.copy(color = Color.White),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    android.util.Log.d("HAPTIC_TEST", "triggered")
                                    HapticManager.heavy(context.applicationContext)
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    SoundHapticHelper.playSound269(context)
                                    showLabelDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = OnSurfaceLight, style = BodySm)
                            }

                            Button(
                                onClick = {
                                    HapticManager.medium(context.applicationContext)
                                    label = tempLabel
                                    showLabelDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LiquidOrange),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("OK", color = TrueBlack, style = BodySm, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- Sound Selection Interactive Dialog ---
        if (showSoundDialog) {
            SoundPickerDialog(
                currentSoundValue = sound,
                onDismiss = { showSoundDialog = false },
                onConfirm = { selected ->
                    sound = selected.value
                    soundChanged = true
                    showSoundDialog = false
                }
            )
        }
    }
}

/**
 * Screen 5 active alarm ringing view featuring ambient glow loops
 */
@Composable
fun ActiveAlarmRingingOverlay(
    alarm: AlarmEntity,
    onStop: () -> Unit,
    onSnooze: () -> Unit
) {
    val context = LocalContext.current
    // Tapping stopwatch or alarms pulse
    val scalePulse = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        scalePulse.animateTo(
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
    ) {
        // Cyan and Violet atmospheres
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(w * 0.2f, h * 0.15f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.2f, h * 0.15f),
                radius = w * 0.65f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFA144FF).copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(w * 0.8f, h * 0.85f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.8f, h * 0.85f),
                radius = w * 0.65f
            )
        }

        LiquidSoundWaveformOverlay(
            modifier = Modifier.fillMaxSize(),
            soundName = alarm.sound
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.alpha(0.7f)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = alarm.label.uppercase(),
                    style = LabelCaps.copy(color = Color.White, letterSpacing = 2.sp, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                )
            }

            // Main Display (Wrapped in frosted card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(shape = RoundedCornerShape(24.dp), bgColor = Color.White.copy(alpha = 0.03f))
                    .padding(vertical = 40.dp)
                    .alpha(scalePulse.value),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${alarm.hour}:${String.format("%02d", alarm.minute)}",
                        style = DisplayTimerMobile.copy(color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Light)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (alarm.isAm) "AM" else "PM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Rise and shine! Your productive day starts now.",
                        style = BodyLg,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(240.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    VolumeBarsVisualizer(
                        modifier = Modifier.height(36.dp).width(160.dp),
                        barCount = 12,
                        color = LiquidOrange
                    )
                }
            }

            // Actions Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // STOP central circle
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(LiquidOrange.copy(alpha = 0.15f))
                        .border(1.dp, LiquidOrange.copy(alpha = 0.3f), CircleShape)
                        .clickable { 
                            HapticManager.heavy(context.applicationContext)
                            onStop() 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(LiquidOrange.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "STOP",
                            style = LabelCaps,
                            color = TrueBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Snooze for 9m button
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(64.dp)
                        .glassPill(bgColor = Color.White.copy(alpha = 0.05f))
                        .clickable { 
                            HapticManager.medium(context.applicationContext)
                            onSnooze() 
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Snooze for 9m",
                        style = BodyLg,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Swipe-up/Dismiss indicators
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.alpha(0.3f)
                ) {
                    Box(modifier = Modifier.size(40.dp, 4.dp).clip(CircleShape).background(Color.White))
                    Text(
                        text = "SWIPE UP TO DISMISS",
                        style = LabelCaps,
                        color = Color.White,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

fun getRepeatLabel(repeatDays: Set<Int>): String {
    if (repeatDays.isEmpty()) return "Once-off"
    if (repeatDays.size == 7) return "Every day"
    if (repeatDays.size == 5 && !repeatDays.contains(5) && !repeatDays.contains(6)) return "Weekdays"
    if (repeatDays.size == 2 && repeatDays.contains(5) && repeatDays.contains(6)) return "Weekends"
    
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return repeatDays.sorted().map { days[it] }.joinToString(", ")
}
