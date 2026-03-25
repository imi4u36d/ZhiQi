package com.zhiqi.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhiqi.app.data.RecordEntity
import com.zhiqi.app.data.RecordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    repository: RecordRepository,
    recordId: Long,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    var record by remember { mutableStateOf<RecordEntity?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(recordId) {
        record = repository.getById(recordId)
    }

    GlassBackground {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("记录详情", style = MaterialTheme.typography.titleMedium)
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "菜单",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.noRippleClickable { onOpenDrawer() }
                )
            }
            if (record == null) {
                Text("未找到记录")
            } else {
                Column(modifier = Modifier.glassCard().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (record!!.type == "同房") "爱爱" else record!!.type,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (record!!.protections.isBlank()) {
                        DetailLine("防护措施", "未记录措施")
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(record!!.protections.split("|").filter { it.isNotBlank() }) { tag ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text(tag) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    )
                                )
                            }
                        }
                    }
                    if (!record!!.otherProtection.isNullOrBlank()) {
                        DetailLine("其他防护", record!!.otherProtection!!)
                    }
                    DetailLine(
                        "记录时间",
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record!!.timeMillis))
                    )
                    if (!record!!.note.isNullOrBlank()) {
                        DetailLine("备注", record!!.note!!)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { if (record != null) showEdit = true },
                    modifier = Modifier.weight(1f)
                ) { Text("编辑") }
                Button(
                    onClick = { if (record != null) showDelete = true },
                    modifier = Modifier.weight(1f)
                ) { Text("删除") }
            }
        }
    }

    if (showEdit && record != null) {
        ZhiQiModalSheet(onDismissRequest = { showEdit = false }) {
            RecordSheet(
                initialRecord = record,
                onSave = { updated ->
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.update(updated)
                    }
                    record = updated
                    showEdit = false
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
                CoroutineScope(Dispatchers.IO).launch {
                    repository.delete(record!!)
                }
                showDelete = false
                onBack()
            },
            confirmText = "删除",
            destructive = true
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.secondary)
        Text(value)
    }
}
