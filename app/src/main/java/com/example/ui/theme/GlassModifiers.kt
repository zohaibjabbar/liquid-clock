package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates a beautiful liquid frosted glass card background with exact dual-tone gradient border highlights.
 */
fun Modifier.glassCard(
    shape: Shape = RoundedCornerShape(16.dp),
    bgColor: Color = GlassBgCard,
    borderWidth: Dp = 1.dp
): Modifier {
    val glassBorderBrush = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = 0.45f),
        0.2f to Color.White.copy(alpha = 0.15f),
        0.4f to Color.Transparent,
        0.6f to Color.Transparent,
        0.8f to Color.White.copy(alpha = 0.15f),
        1.0f to Color.White.copy(alpha = 0.45f)
    )
    return this
        .background(bgColor, shape)
        .border(borderWidth, glassBorderBrush, shape)
}

/**
 * Creates a sleek frosted glass pill-shape for toggle options, select labels and small badges.
 */
fun Modifier.glassPill(
    bgColor: Color = GlassBg
): Modifier {
    val pillShape = RoundedCornerShape(9999.dp)
    val pillBorderBrush = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = 0.25f),
        0.5f to Color.White.copy(alpha = 0.05f),
        1.0f to Color.White.copy(alpha = 0.10f)
    )
    return this
        .background(bgColor, pillShape)
        .border(1.dp, pillBorderBrush, pillShape)
}

/**
 * Creates a strong frosted card glass highlight with heavier opacities for prominent buttons or high-contrast states.
 */
fun Modifier.glassStrongCard(
    shape: Shape = RoundedCornerShape(16.dp),
    bgColor: Color = Color(0x22FFFFFF) // More opaque white (13%)
): Modifier {
    val strongBorderBrush = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = 0.60f),
        0.2f to Color.White.copy(alpha = 0.25f),
        0.4f to Color.Transparent,
        0.6f to Color.Transparent,
        0.8f to Color.White.copy(alpha = 0.25f),
        1.0f to Color.White.copy(alpha = 0.60f)
    )
    return this
        .background(bgColor, shape)
        .border(1.4.dp, strongBorderBrush, shape)
}
