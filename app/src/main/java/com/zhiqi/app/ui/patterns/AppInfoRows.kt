package com.zhiqi.app.ui.patterns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.zhiqi.app.ui.ZhiQiTokens
import com.zhiqi.app.ui.components.AppIconBadge
import com.zhiqi.app.ui.designsystem.AppSpacing

@Composable
fun AppInfoRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
        verticalAlignment = Alignment.Top
    ) {
        AppIconBadge(
            icon = icon,
            contentDescription = label,
            tint = iconTint
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xSmall)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = ZhiQiTokens.TextMuted,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = ZhiQiTokens.TextPrimary
            )
        }
    }
}
