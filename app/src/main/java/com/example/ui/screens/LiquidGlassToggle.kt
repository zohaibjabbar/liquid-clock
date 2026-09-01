package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LiquidGlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "thumbProgress"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFFF5A623) else Color(0xFF3A3A3A),
        animationSpec = tween(durationMillis = 200),
        label = "trackColor"
    )

    val trackWidth  = 52.dp
    val trackHeight = 28.dp
    val thumbSize   = 22.dp
    val thumbPadding = 3.dp

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        val maxOffset = trackWidth - thumbSize - (thumbPadding * 2)
        Box(
            modifier = Modifier
                .padding(start = thumbPadding + (maxOffset * thumbProgress))
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
