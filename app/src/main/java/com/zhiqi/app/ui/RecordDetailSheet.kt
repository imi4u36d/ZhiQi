package com.zhiqi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zhiqi.app.data.RecordEntity
import com.zhiqi.app.data.RecordRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailSheet(
    repository: RecordRepository,
    recordId: Long,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var record by remember { mutableStateOf<RecordEntity?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(recordId) {
        record = repository.getById(recordId)
    }

    Column(
        modifier = modifier
            .fillMaxHeight(0.82f)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (record == null) {
            Text("未找到记录", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClose) { Text("关闭") }
        } else {
            val current = record!!
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (current.type == "同房") "爱爱" else current.type,
                    style = MaterialTheme.typography.titleLarge,
                    color = ZhiQiTokens.TextPrimary
                )
                Text(
                    text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(current.timeMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZhiQiTokens.TextSecondary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (current.protections.isNotBlank()) {
                    DetailItem("防护") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(current.protections.split("|").filter { it.isNotBlank() }) { tag ->
                                BoxTag(tag)
                            }
                        }
                    }
                } else {
                    DetailItem("防护") { Text("未记录措施") }
                }

                if (!current.otherProtection.isNullOrBlank()) {
                    DetailItem("其他") { Text(current.otherProtection) }
                }

                if (!current.note.isNullOrBlank()) {
                    DetailItem("备注") { Text(current.note) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    text = "编辑",
                    icon = Icons.Filled.Edit,
                    color = ZhiQiTokens.Primary
                ) {
                    showEdit = true
                }
                ActionButton(
                    modifier = Modifier.weight(1f),
                    text = "删除",
                    icon = Icons.Filled.DeleteOutline,
                    color = ZhiQiTokens.Danger
                ) {
                    showDelete = true
                }
            }
        }
    }

    if (showEdit && record != null) {
        ZhiQiModalSheet(onDismissRequest = { showEdit = false }) {
            RecordSheet(
                initialRecord = record,
                onSave = { updated ->
                    scope.launch {
                        repository.update(updated)
                        record = updated
                        showEdit = false
                    }
                },
                onCancel = { showEdit = false }
            )
        }
    }

    if (showDelete && record != null) {
        ZhiQiConfirmDialog(
            title = "确认删除记录？",
            message = "删除后无法恢复。",
            onDismissRequest = { showDelete = false },
            onConfirm = {
                scope.launch {
                    repository.delete(record!!)
                    showDelete = false
                    onClose()
                }
            },
            confirmText = "删除",
            destructive = true
        )
    }
}

@Composable
private fun DetailItem(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = ZhiQiTokens.TextSecondary)
        content()
    }
}

@Composable
private fun BoxTag(tag: String) {
    Row(
        modifier = Modifier
            .background(ZhiQiTokens.AccentSoft, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(tag, style = MaterialTheme.typography.labelMedium, color = ZhiQiTokens.Primary)
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .glassCard()
            .noRippleClickable(onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, tint = color, modifier = Modifier.size(18.dp))
        Text(
            text = text,
            color = color,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.titleSmall
        )
    }
}
