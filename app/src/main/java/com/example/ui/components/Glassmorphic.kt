package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// AMOLED Colors
val AmoledBackground = Color(0xFF000000)
val DarkGreySurface = Color(0xFF0A0A0C)
val GlassSurface = Color(0x1CFFFFFF) // 11% white for glass look
val GlassBorderColor = Color(0x2BFFFFFF) // Transparent white

// RGB Neon Colors
val NeonCyan = Color(0xFF00E5FF)
val NeonMagenta = Color(0xFFF355DA)
val NeonYellow = Color(0xFFFFD600)
val NeonGreen = Color(0xFF2AFFB0)
val NeonRed = Color(0xFFFF3366)

// RGB Neon Border Brush modifier
fun Modifier.neonBorder(
    strokeWidth: Dp = 1.5.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: List<Color> = listOf(NeonCyan, NeonMagenta, NeonYellow, NeonCyan)
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "neon_transition")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neon_progress"
    )

    val brush = Brush.sweepGradient(
        colors = colors,
        center = Offset.Unspecified
    )
    this.border(strokeWidth, brush, shape)
}

// Subtly breathing background neon glow
fun Modifier.neonGlow(
    color: Color = NeonMagenta,
    alpha: Float = 0.15f
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "glow_transition")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    this.drawBehind {
        val radius = size.minDimension * 0.45f * scale
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    hasNeonBorder: Boolean = false,
    neonColors: List<Color> = listOf(NeonCyan, NeonMagenta, NeonGreen, NeonCyan),
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (hasNeonBorder) {
        modifier
            .clip(shape)
            .background(Color(0x12FFFFFF)) // glass translucent fill
            .neonBorder(strokeWidth = 1.5.dp, shape = shape, colors = neonColors)
    } else {
        modifier
            .clip(shape)
            .background(Color(0x0CFFFFFF)) // glass transparent fill
            .border(1.dp, Color(0x1FFFFFFF), shape)
    }

    Column(
        modifier = cardModifier.padding(16.dp),
        content = content
    )
}

@Composable
fun NeonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(NeonCyan, NeonMagenta),
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "button_neon")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "button_glow"
    )

    val gradientBrush = Brush.linearGradient(
        colors = colors,
        start = Offset(animatedProgress * 500f, 0f),
        end = Offset((animatedProgress + 1f) * 500f, 500f)
    )

    val bgModifier = if (enabled) {
        Modifier.background(gradientBrush)
    } else {
        Modifier.background(Color(0x22FFFFFF))
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp)) else Modifier)
            .then(bgModifier),
        color = Color.Transparent,
        contentColor = if (enabled) Color.White else Color.Gray
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
