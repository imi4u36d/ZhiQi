package com.zhiqi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhiqi.app.ui.designsystem.AppRadii

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZhiQiModalSheet(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White.copy(alpha = 0.985f),
        scrimColor = ZhiQiTokens.TextPrimary.copy(alpha = 0.16f),
        shape = RoundedCornerShape(topStart = AppRadii.sheet, topEnd = AppRadii.sheet),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .background(ZhiQiTokens.BorderStrong, RoundedCornerShape(999.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.9f),
                            ZhiQiTokens.SurfaceSoft.copy(alpha = 0.98f)
                        )
                    )
                )
                .navigationBarsPadding()
        ) {
            content()
        }
    }
}

@Composable
fun ZhiQiConfirmDialog(
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "确定",
    dismissText: String? = "取消",
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White.copy(alpha = 0.98f),
        shape = RoundedCornerShape(AppRadii.large),
        title = {
            Text(
                text = title,
                color = ZhiQiTokens.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                color = ZhiQiTokens.TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (destructive) ZhiQiTokens.Danger else ZhiQiTokens.PrimaryStrong,
                    contentColor = Color.White
                )
            ) {
                Text(text = confirmText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            if (dismissText != null) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        ZhiQiTokens.BorderStrong.copy(alpha = 0.9f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.72f),
                        contentColor = ZhiQiTokens.TextSecondary
                    )
                ) {
                    Text(text = dismissText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    )
}
