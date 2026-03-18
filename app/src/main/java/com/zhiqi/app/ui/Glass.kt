package com.zhiqi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        ZhiQiTokens.BackgroundTop,
                        Color.White,
                        ZhiQiTokens.PrimarySoft.copy(alpha = 0.52f),
                        ZhiQiTokens.BackgroundBottom
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.9f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ZhiQiTokens.PrimarySoft.copy(alpha = 0.34f),
                            ZhiQiTokens.AccentStrongerSoft.copy(alpha = 0.34f),
                            Color(0xFFFFF5CC).copy(alpha = 0.28f),
                            Color(0xFFDDE8FF).copy(alpha = 0.24f),
                            Color.Transparent
                        ),
                        radius = 1180f
                    )
                )
        )
        content()
    }
}

fun Modifier.glassCard(): Modifier {
    val shape = RoundedCornerShape(34.dp)
    return this
        .shadow(
            elevation = 18.dp,
            shape = shape,
            ambientColor = ZhiQiTokens.Primary.copy(alpha = 0.08f),
            spotColor = Color.White.copy(alpha = 0.18f)
        )
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.82f),
                    ZhiQiTokens.Surface.copy(alpha = 0.74f)
                )
            )
        )
        .border(1.dp, Color.White.copy(alpha = 0.74f), shape)
}

@Composable
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

@Composable
fun SectionTitle(text: String) {
    androidx.compose.material3.Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}
