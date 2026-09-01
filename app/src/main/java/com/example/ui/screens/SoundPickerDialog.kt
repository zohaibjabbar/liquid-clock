package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.ui.SoundHapticHelper
import com.example.HapticManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import android.os.Build
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.LiquidSoundSynth
import com.example.ui.theme.LiquidOrange
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.OnSurfaceLight
import com.example.ui.theme.OnSurfaceMuted
import com.example.ui.theme.BodySm
import com.example.ui.theme.BodyLg
import com.example.ui.theme.LabelCaps
import com.example.ui.theme.glassCard
import com.example.ui.theme.glassStrongCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import org.json.JSONArray
import org.json.JSONObject

data class SoundItem(
    val name: String,
    val value: String,
    val isCustom: Boolean = false
)

fun cleanSoundName(rawName: String): String {
    // Decode URL percent-encoding (like %20 to space)
    var name = try {
        android.net.Uri.decode(rawName)
    } catch (e: Exception) {
        rawName
    }
    
    // Remove any path parts if it's a full path
    name = name.substringAfterLast("/")
    
    // Remove leading timestamp (like System.currentTimeMillis()_)
    name = name.replaceFirst(Regex("^\\d+[_\\-]+"), "")
    
    // Remove common media suffixes/extensions (e.g., .mp3, .wav, .ogg, .m4a, _mp3, _wav, -mp3, etc.)
    name = name.replace(Regex("(?i)[_\\-\\s\\.]+(mp3|wav|ogg|m4a|aac|flac|wma)$"), "")
    name = name.replace(Regex("(?i)(mp3|wav|ogg|m4a|aac|flac|wma)$"), "")
    
    // Remove any remaining file extensions like .mp3 or similar
    val lastDot = name.lastIndexOf('.')
    if (lastDot > 0 && lastDot >= name.length - 5) {
        name = name.substring(0, lastDot)
    }
    
    // Remove any leading symbols, dots, numbers, spaces at the start (e.g. "12. ", ". ", "_", "- ", "01 - ")
    name = name.replaceFirst(Regex("^[^a-zA-Z]+"), "")
    
    // Replace underscores, hashes, dashes with spaces
    val cleaned = name.replace(Regex("[_#\\-\\.]+"), " ")
    
    // Split into words, trim them, and filter out any empty parts
    val words = cleaned.split(Regex("\\s+"))
    val cleanWords = words.map { it.trim() }.filter { it.isNotEmpty() }
    
    // Rejoin and capitalize words for a highly polished look
    val result = cleanWords.joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }.trim()
    
    return if (result.isEmpty()) "Custom Sound" else result
}

fun getSharedSoundList(context: Context): List<SoundItem> {
    val list = mutableListOf<SoundItem>()
    
    // 1. Built-in sounds
    val builtIn = listOf(
        "Dewdrop Serenade",
        "Bubble Resonance",
        "Ocean Breeze",
        "Liquid Echo",
        "Rainfall Melody"
    )
    for (name in builtIn) {
        list.add(SoundItem(name = name, value = name, isCustom = false))
    }
    
    // 2. Custom sounds from SharedPreferences
    val prefs = context.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString("custom_ringtones_json", "[]") ?: "[]"
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.getString("name")
            val uri = obj.getString("uri")
            list.add(SoundItem(name = cleanSoundName(name), value = uri, isCustom = true))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    return list
}

fun getTimerSoundList(context: Context): List<SoundItem> {
    val list = mutableListOf<SoundItem>()
    
    // 1. Built-in sounds
    val builtIn = listOf(
        "Dewdrop Serenade",
        "Bubble Resonance",
        "Ocean Breeze",
        "Liquid Echo",
        "Rainfall Melody"
    )
    for (name in builtIn) {
        list.add(SoundItem(name = name, value = name, isCustom = false))
    }
    
    // 2. Custom sounds from SharedPreferences
    val prefs = context.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString("custom_timer_sounds", "[]") ?: "[]"
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.getString("name")
            val uri = obj.getString("uri")
            list.add(SoundItem(name = cleanSoundName(name), value = uri, isCustom = true))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    return list
}

fun getSoundDisplayName(context: Context, soundValue: String): String {
    if (soundValue.isBlank()) {
        return "Starlight"
    }
    
    // Check built-in sounds
    val builtIn = listOf("Dewdrop Serenade", "Bubble Resonance", "Ocean Breeze", "Liquid Echo", "Rainfall Melody")
    if (soundValue in builtIn) {
        return soundValue
    }
    
    // Check if it matches any custom sound value
    val prefs = context.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString("custom_ringtones_json", "[]") ?: "[]"
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.getString("name")
            val uri = obj.getString("uri")
            if (uri == soundValue) {
                return cleanSoundName(name)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    // Check if it's the default alarm sound uri
    val defaultUri = prefs.getString("default_alarm_sound_uri", "") ?: ""
    val defaultName = prefs.getString("default_alarm_sound_name", "Starlight") ?: "Starlight"
    if (soundValue == defaultUri && defaultUri.isNotEmpty()) {
        return cleanSoundName(defaultName)
    }
    
    // If it's a file path, we can clean up any raw file name with underscores or hashes
    if (soundValue.startsWith("content://") || soundValue.startsWith("file://") || soundValue.contains("/")) {
        return cleanSoundName(soundValue)
    }
    
    return cleanSoundName(soundValue)
}

fun getFileNameFromUri(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "custom_ringtone.mp3"
}

fun copyUriToInternalStorage(context: Context, uri: Uri, displayName: String): java.io.File? {
    try {
        val destDir = java.io.File(context.filesDir, "custom_ringtones")
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        val safeName = displayName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val destFile = java.io.File(destDir, "${System.currentTimeMillis()}_$safeName")
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            java.io.FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return destFile
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundPickerDialog(
    currentSoundValue: String,
    onDismiss: () -> Unit,
    onConfirm: (SoundItem) -> Unit,
    onCustomSoundsChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var soundList by remember { mutableStateOf(getSharedSoundList(context)) }
    
    // Find matching sound or default to first
    var selectedSound by remember {
        mutableStateOf(soundList.find { it.value == currentSoundValue } ?: soundList.first())
    }
    
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val scope = rememberCoroutineScope()
    var previewJob by remember { mutableStateOf<Job?>(null) }
    var previewingSoundValue by remember { mutableStateOf<String?>(null) }
    var soundToDelete by remember { mutableStateOf<SoundItem?>(null) }
    
    fun stopPreview() {
        LiquidSoundSynth.stopPlaying()
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPreviewWithJob() {
        previewJob?.cancel()
        previewJob = null
        stopPreview()
        previewingSoundValue = null
    }

    BackHandler {
        stopPreviewWithJob()
        onDismiss()
    }

    fun startPreview(item: SoundItem) {
        previewJob?.cancel()
        stopPreview()
        
        previewingSoundValue = item.value
        
        if (!item.isCustom) {
            LiquidSoundSynth.startPlaying(item.value)
        } else {
            try {
                val player = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(item.value))
                    prepare()
                    start()
                }
                previewPlayer = player
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        previewJob = scope.launch {
            delay(5000L)
            stopPreview()
            previewingSoundValue = null
            previewJob = null
        }
    }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val originalName = getFileNameFromUri(context, uri)
            val localFile = copyUriToInternalStorage(context, uri, originalName)
            if (localFile != null) {
                val rawName = originalName.substringBeforeLast(".")
                val displayName = cleanSoundName(rawName)
                
                // Save to SharedPreferences custom list
                val prefs = context.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
                val jsonStr = prefs.getString("custom_ringtones_json", "[]") ?: "[]"
                val array = try {
                    JSONArray(jsonStr)
                } catch (e: Exception) {
                    JSONArray()
                }
                
                val obj = JSONObject()
                obj.put("name", displayName)
                obj.put("uri", localFile.absolutePath)
                array.put(obj)
                
                prefs.edit().putString("custom_ringtones_json", array.toString()).apply()
                
                // Reload sound list dynamically
                soundList = getSharedSoundList(context)
                
                // Set newly added sound as selected
                val newItem = SoundItem(name = displayName, value = localFile.absolutePath, isCustom = true)
                selectedSound = newItem
                
                // Notify custom callback if provided
                onCustomSoundsChanged()
                
                android.widget.Toast.makeText(context, "Added: $displayName", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Failed to copy audio file", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            stopPreviewWithJob()
        }
    }

    if (soundToDelete != null) {
        Dialog(
            onDismissRequest = { soundToDelete = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .glassCard(shape = RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Screen Title
                Text(
                    text = "Delete Custom Sound",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )

                Text(
                    text = "Are you sure you want to delete \"${soundToDelete?.name}\"?",
                    style = BodyLg,
                    color = OnSurfaceLight,
                    fontWeight = FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Actions Cancel / Delete buttons - matching styling of main picker dialog buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            android.util.Log.d("HAPTIC_TEST", "triggered")
                            HapticManager.heavy(context)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            soundToDelete = null
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
                            HapticManager.heavy(context)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            soundToDelete?.let { item ->
                                stopPreviewWithJob()
                                
                                val file = java.io.File(item.value)
                                if (file.exists()) {
                                    file.delete()
                                }
                                
                                val prefs = context.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
                                val jsonStr = prefs.getString("custom_ringtones_json", "[]") ?: "[]"
                                try {
                                    val array = JSONArray(jsonStr)
                                    val newArray = JSONArray()
                                    for (i in 0 until array.length()) {
                                        val obj = array.getJSONObject(i)
                                        val uri = obj.getString("uri")
                                        if (uri != item.value) {
                                            newArray.put(obj)
                                        }
                                    }
                                    prefs.edit().putString("custom_ringtones_json", newArray.toString()).apply()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                
                                soundList = getSharedSoundList(context)
                                if (selectedSound.value == item.value) {
                                    selectedSound = soundList.firstOrNull() ?: SoundItem("Starlight", "")
                                }
                                onCustomSoundsChanged()
                                android.widget.Toast.makeText(context, "Deleted ${item.name}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            soundToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .glassStrongCard(shape = RoundedCornerShape(12.dp))
                    ) {
                        Text(text = "Delete", style = BodyLg, color = LiquidOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    
    val view = LocalView.current
        val density = LocalDensity.current
        val isBlurred = soundToDelete != null

        DisposableEffect(isBlurred, view) {
            if (isBlurred) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val radiusPx = with(density) { 10.dp.toPx() }
                    try {
                        view.setRenderEffect(
                            android.graphics.RenderEffect.createBlurEffect(
                                radiusPx,
                                radiusPx,
                                android.graphics.Shader.TileMode.CLAMP
                            )
                        )
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        view.setRenderEffect(null)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
            onDispose {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        view.setRenderEffect(null)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {} // Absolutely block background interaction
                .background(TrueBlack)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(top = 40.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header Screen Title
                Text(
                    text = "Select Alarm Sound",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(soundList) { item ->
                        val isSelected = item.value == selectedSound.value
                        val scale = remember { Animatable(1f) }
                        val itemModifier = Modifier
                            .fillMaxWidth()
                            .scale(scale.value)
                            .clip(RoundedCornerShape(12.dp))
                            .glassCard(shape = RoundedCornerShape(12.dp))
                        
                        Row(
                            modifier = itemModifier
                                .combinedClickable(
                                    onClick = {
                                        HapticManager.light(context)
                                        scope.launch {
                                            scale.animateTo(0.95f, tween(80))
                                            scale.animateTo(1f, tween(80))
                                        }
                                        selectedSound = item
                                        stopPreviewWithJob()
                                        if (!item.isCustom) {
                                            LiquidSoundSynth.startPlaying(item.value)
                                        } else {
                                            try {
                                                val player = MediaPlayer().apply {
                                                    setDataSource(context, Uri.parse(item.value))
                                                    prepare()
                                                    start()
                                                }
                                                previewPlayer = player
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        if (item.isCustom) {
                                            HapticManager.heavy(context)
                                            soundToDelete = item
                                        }
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isPreviewing = previewingSoundValue == item.value
                                IconButton(
                                    onClick = {
                                        HapticManager.light(context)
                                        if (isPreviewing) {
                                            stopPreviewWithJob()
                                        } else {
                                            startPreview(item)
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewing) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = if (isPreviewing) "Stop Preview" else "Play Preview",
                                        tint = if (isPreviewing) LiquidOrange else OnSurfaceLight.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Text(
                                    text = item.name,
                                    style = BodySm,
                                    color = if (isSelected) LiquidOrange else OnSurfaceLight,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                            
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
                
                // Add From Storage - styled exactly like the list rows
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .glassCard(shape = RoundedCornerShape(12.dp))
                        .clickable {
                            HapticManager.light(context)
                            filePickerLauncher.launch("audio/*")
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add From Storage",
                        tint = LiquidOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Add From Storage",
                        style = BodySm,
                        color = LiquidOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Actions Cancel / Select - styled exactly like Cancel / Save on New Alarm screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            android.util.Log.d("HAPTIC_TEST", "triggered")
                            HapticManager.heavy(context)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            stopPreview()
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
                            stopPreview()
                            onConfirm(selectedSound)
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

            // Fallback dimming overlay for Android versions below 12
            if (isBlurred && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
            }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerSoundPickerDialog(
    currentSoundValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onSoundSelected: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("clock_settings", Context.MODE_PRIVATE) }
    val customUri = remember { prefs.getString("timer_custom_sound_uri", null) }
    
    var soundList by remember { mutableStateOf(getTimerSoundList(context)) }
    
    var selectedSound by remember {
        mutableStateOf(
            if (!customUri.isNullOrEmpty()) {
                soundList.find { it.isCustom && it.value == customUri } ?: soundList.first()
            } else {
                soundList.find { !it.isCustom && (it.value == currentSoundValue || it.name == currentSoundValue) } ?: soundList.first()
            }
        )
    }

    val storagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            val fileName = getFileNameFromUri(context, it)
            val rawName = fileName.substringBeforeLast(".")
            val displayName = cleanSoundName(rawName)

            // Save the selected URI as the timer sound
            prefs.edit().putString("timer_custom_sound_uri", it.toString()).apply()

            // Save to "custom_timer_sounds" JSON list in SharedPreferences
            val jsonStr = prefs.getString("custom_timer_sounds", "[]") ?: "[]"
            val array = try {
                JSONArray(jsonStr)
            } catch (e: Exception) {
                JSONArray()
            }

            // Check for duplicates
            var exists = false
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("uri") == it.toString()) {
                    exists = true
                    break
                }
            }

            if (!exists) {
                val obj = JSONObject()
                obj.put("name", displayName)
                obj.put("uri", it.toString())
                array.put(obj)
                prefs.edit().putString("custom_timer_sounds", array.toString()).apply()
            }

            soundList = getTimerSoundList(context)

            // Update the displayed sound name
            onSoundSelected(displayName, it.toString())
            onDismiss()
        }
    }

    var previewingSoundValue by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var previewJob by remember { mutableStateOf<Job?>(null) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var soundToDelete by remember { mutableStateOf<SoundItem?>(null) }

    fun stopPreview() {
        LiquidSoundSynth.stopPlaying()
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPreviewWithJob() {
        previewJob?.cancel()
        previewJob = null
        stopPreview()
        previewingSoundValue = null
    }

    BackHandler {
        stopPreviewWithJob()
        onDismiss()
    }

    fun startPreview(item: SoundItem) {
        previewJob?.cancel()
        stopPreview()
        previewingSoundValue = item.value
        
        if (!item.isCustom) {
            LiquidSoundSynth.startPlaying(item.value)
        } else {
            try {
                val player = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(item.value))
                    prepare()
                    start()
                }
                previewPlayer = player
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        previewJob = scope.launch {
            delay(5000L)
            stopPreview()
            previewingSoundValue = null
            previewJob = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPreviewWithJob()
        }
    }

    if (soundToDelete != null) {
        Dialog(
            onDismissRequest = { soundToDelete = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .glassCard(shape = RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Delete Custom Sound",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )

                Text(
                    text = "Are you sure you want to delete \"${soundToDelete?.name}\"?",
                    style = BodyLg,
                    color = OnSurfaceLight,
                    fontWeight = FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            HapticManager.heavy(context)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            soundToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Cancel", style = BodyLg, color = OnSurfaceLight)
                    }

                    Button(
                        onClick = {
                            HapticManager.heavy(context)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            soundToDelete?.let { item ->
                                stopPreviewWithJob()
                                
                                val prefs = context.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
                                val jsonStr = prefs.getString("custom_timer_sounds", "[]") ?: "[]"
                                try {
                                    val array = JSONArray(jsonStr)
                                    val newArray = JSONArray()
                                    for (i in 0 until array.length()) {
                                        val obj = array.getJSONObject(i)
                                        val uri = obj.getString("uri")
                                        if (uri != item.value) {
                                            newArray.put(obj)
                                        }
                                    }
                                    prefs.edit().putString("custom_timer_sounds", newArray.toString()).apply()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                
                                val currentActiveUri = prefs.getString("timer_custom_sound_uri", null)
                                if (currentActiveUri == item.value) {
                                    prefs.edit().remove("timer_custom_sound_uri").apply()
                                }
                                
                                soundList = getTimerSoundList(context)
                                if (selectedSound.value == item.value) {
                                    selectedSound = soundList.firstOrNull() ?: SoundItem("Dewdrop Serenade", "Dewdrop Serenade")
                                }
                                android.widget.Toast.makeText(context, "Deleted ${item.name}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            soundToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .glassStrongCard(shape = RoundedCornerShape(12.dp))
                    ) {
                        Text(text = "Delete", style = BodyLg, color = LiquidOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    val view = LocalView.current
        val density = LocalDensity.current
        val isBlurred = soundToDelete != null

        DisposableEffect(isBlurred, view) {
            if (isBlurred) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val radiusPx = with(density) { 10.dp.toPx() }
                    try {
                        view.setRenderEffect(
                            android.graphics.RenderEffect.createBlurEffect(
                                radiusPx,
                                radiusPx,
                                android.graphics.Shader.TileMode.CLAMP
                            )
                        )
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        view.setRenderEffect(null)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
            onDispose {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        view.setRenderEffect(null)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {}
                .background(TrueBlack)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(top = 40.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header Screen Title
                Text(
                    text = "Select Timer Sound",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(soundList) { item ->
                        val isSelected = item.value == selectedSound.value
                        val scale = remember { Animatable(1f) }
                        val itemModifier = Modifier
                            .fillMaxWidth()
                            .scale(scale.value)
                            .clip(RoundedCornerShape(12.dp))
                            .glassCard(shape = RoundedCornerShape(12.dp))

                        Row(
                            modifier = itemModifier
                                .combinedClickable(
                                    onClick = {
                                        HapticManager.light(context)
                                        scope.launch {
                                            scale.animateTo(0.95f, tween(80))
                                            scale.animateTo(1f, tween(80))
                                        }
                                        selectedSound = item
                                        stopPreviewWithJob()
                                        startPreview(item)
                                    },
                                    onLongClick = {
                                        if (item.isCustom) {
                                            HapticManager.heavy(context)
                                            soundToDelete = item
                                        }
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isPreviewing = previewingSoundValue == item.value
                                IconButton(
                                    onClick = {
                                        HapticManager.light(context)
                                        if (isPreviewing) {
                                            stopPreviewWithJob()
                                        } else {
                                            startPreview(item)
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewing) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = if (isPreviewing) "Stop Preview" else "Play Preview",
                                        tint = if (isPreviewing) LiquidOrange else OnSurfaceLight.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = item.name,
                                    style = BodySm,
                                    color = if (isSelected) LiquidOrange else OnSurfaceLight,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }

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

                // Add From Storage - styled exactly like the list rows
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .glassCard(shape = RoundedCornerShape(12.dp))
                        .clickable {
                            HapticManager.light(context)
                            storagePickerLauncher.launch(arrayOf("audio/*"))
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add From Storage",
                        tint = LiquidOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Add From Storage",
                        style = BodySm,
                        color = LiquidOrange,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Actions Cancel / Select - styled exactly like the alarm dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            android.util.Log.d("HAPTIC_TEST", "triggered")
                            HapticManager.heavy(context)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            stopPreview()
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
                            stopPreview()
                            
                            if (selectedSound.isCustom) {
                                prefs.edit().putString("timer_custom_sound_uri", selectedSound.value).apply()
                                onSoundSelected(selectedSound.name, selectedSound.value)
                            } else {
                                prefs.edit().remove("timer_custom_sound_uri").apply()
                                onConfirm(selectedSound.value)
                            }
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

            // Fallback dimming overlay for Android versions below 12
            if (isBlurred && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
            }
        }
}

