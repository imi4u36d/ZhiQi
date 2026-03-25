package com.zhiqi.app.ui

import android.content.Context
import android.net.Uri
import com.zhiqi.app.data.DailyIndicatorEntity
import com.zhiqi.app.data.DailyIndicatorRepository
import com.zhiqi.app.data.RecordEntity
import com.zhiqi.app.data.RecordRepository
import com.zhiqi.app.security.PinManager
import com.zhiqi.app.security.PinSnapshot
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(
    private val context: Context,
    private val repository: RecordRepository,
    private val indicatorRepository: DailyIndicatorRepository,
    private val cycleSettingsManager: CycleSettingsManager,
    private val pinManager: PinManager
) {
    suspend fun exportTo(uri: Uri): BackupSummary {
        val records = repository.getAll()
        val indicators = indicatorRepository.getAll()
        // 导出默认不包含 PIN 明文快照，避免备份文件本身成为敏感数据载体。
        val payload = BackupPayload(
            version = BACKUP_VERSION,
            exportedAtMillis = System.currentTimeMillis(),
            records = records,
            cycleSettings = cycleSettingsManager.exportSnapshot(),
            indicators = indicators,
            pin = null
        )
        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8).use { writer ->
            requireNotNull(writer) { "无法创建导出文件" }
            writer.write(payload.toJson().toString())
            writer.flush()
        }
        return BackupSummary(
            recordCount = records.size,
            indicatorCount = indicators.size,
            cycleConfigured = payload.cycleSettings.configured,
            pinConfigured = pinManager.isPinSet()
        )
    }

    suspend fun importFrom(uri: Uri): BackupSummary {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8).use { reader ->
            requireNotNull(reader) { "无法读取备份文件" }
            reader.readText()
        }
        val payload = BackupPayload.fromJson(JSONObject(content))
        repository.clearAll()
        indicatorRepository.clearAll()
        payload.records.forEach { repository.add(it) }
        payload.indicators.forEach { indicatorRepository.save(it) }
        cycleSettingsManager.restoreSnapshot(payload.cycleSettings)
        if (payload.pin != null) {
            pinManager.restoreSnapshot(payload.pin)
        }
        return BackupSummary(
            recordCount = payload.records.size,
            indicatorCount = payload.indicators.size,
            cycleConfigured = payload.cycleSettings.configured,
            pinConfigured = payload.pin?.hashBase64?.isNotBlank() == true
        )
    }

    companion object {
        // 当前版本同时兼容旧导出：v1 没有显式 pin 字段，v2 才开始携带更多状态。
        private const val BACKUP_VERSION = 2
    }
}

data class BackupSummary(
    val recordCount: Int,
    val indicatorCount: Int,
    val cycleConfigured: Boolean,
    val pinConfigured: Boolean
)

private data class BackupPayload(
    val version: Int,
    val exportedAtMillis: Long,
    val records: List<RecordEntity>,
    val cycleSettings: CycleSettingsSnapshot,
    val indicators: List<DailyIndicatorEntity>,
    val pin: PinSnapshot?
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("version", version)
            .put("exportedAtMillis", exportedAtMillis)
            .put("records", JSONArray().apply {
                records.forEach { record ->
                    put(
                        JSONObject()
                            .put("id", record.id)
                            .put("type", record.type)
                            .put("protections", record.protections)
                            .put("otherProtection", record.otherProtection)
                            .put("timeMillis", record.timeMillis)
                            .put("note", record.note)
                    )
                }
            })
            .put("indicators", JSONArray().apply {
                indicators.forEach { indicator ->
                    put(
                        JSONObject()
                            .put("id", indicator.id)
                            .put("dateKey", indicator.dateKey)
                            .put("metricKey", indicator.metricKey)
                            .put("optionValue", indicator.optionValue)
                            .put("displayLabel", indicator.displayLabel)
                            .put("updatedAt", indicator.updatedAt)
                    )
                }
            })
            .put(
                "cycleSettings",
                JSONObject()
                    .put("configured", cycleSettings.configured)
                    .put("cycleLengthDays", cycleSettings.cycleLengthDays)
                    .put("periodLengthDays", cycleSettings.periodLengthDays)
                    .put("lastPeriodStartMillis", cycleSettings.lastPeriodStartMillis)
            )
            .apply {
                if (pin != null) {
                    put(
                        "pin",
                        JSONObject()
                            .put("hashBase64", pin.hashBase64)
                            .put("saltBase64", pin.saltBase64)
                            .put("failCount", pin.failCount)
                            .put("hidden", pin.hidden)
                            .put("biometricEnabled", pin.biometricEnabled)
                            .put("passwordEnabled", pin.passwordEnabled)
                    )
                }
            }
    }

    companion object {
        fun fromJson(json: JSONObject): BackupPayload {
            val version = json.optInt("version", 0)
            require(version in 1..2) { "不支持的备份版本：$version" }

            val recordsArray = json.optJSONArray("records") ?: JSONArray()
            val records = buildList(recordsArray.length()) {
                for (index in 0 until recordsArray.length()) {
                    val item = recordsArray.getJSONObject(index)
                    add(
                        RecordEntity(
                            id = item.optLong("id", 0L),
                            type = item.optString("type"),
                            protections = item.optString("protections"),
                            otherProtection = item.optNullableString("otherProtection"),
                            timeMillis = item.optLong("timeMillis", 0L),
                            note = item.optNullableString("note")
                        )
                    )
                }
            }

            val indicatorsArray = json.optJSONArray("indicators") ?: JSONArray()
            val indicators = buildList(indicatorsArray.length()) {
                for (index in 0 until indicatorsArray.length()) {
                    val item = indicatorsArray.getJSONObject(index)
                    add(
                        DailyIndicatorEntity(
                            id = item.optLong("id", 0L),
                            dateKey = item.optString("dateKey", ""),
                            metricKey = item.optString("metricKey", ""),
                            optionValue = item.optString("optionValue", ""),
                            displayLabel = item.optString("displayLabel", ""),
                            updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }.filter {
                it.dateKey.isNotBlank() && it.metricKey.isNotBlank()
            }

            val cycleJson = json.optJSONObject("cycleSettings") ?: JSONObject()
            val pinJson = json.optJSONObject("pin") ?: JSONObject()
            // 旧版本备份可能没有 pin 块；缺省时按“无密码快照”处理。
            val pin = if (pinJson.length() == 0) {
                null
            } else {
                val passwordEnabled = if (pinJson.has("passwordEnabled")) {
                    pinJson.optBoolean("passwordEnabled", false)
                } else {
                    pinJson.optNullableString("hashBase64")?.isNotBlank() == true
                }
                PinSnapshot(
                    hashBase64 = pinJson.optNullableString("hashBase64"),
                    saltBase64 = pinJson.optNullableString("saltBase64"),
                    failCount = pinJson.optInt("failCount", 0),
                    hidden = pinJson.optBoolean("hidden", false),
                    biometricEnabled = pinJson.optBoolean("biometricEnabled", false),
                    passwordEnabled = passwordEnabled
                )
            }

            return BackupPayload(
                version = version,
                exportedAtMillis = json.optLong("exportedAtMillis", System.currentTimeMillis()),
                records = records,
                cycleSettings = CycleSettingsSnapshot(
                    configured = cycleJson.optBoolean("configured", false),
                    cycleLengthDays = cycleJson.optInt("cycleLengthDays", 28),
                    periodLengthDays = cycleJson.optInt("periodLengthDays", 5),
                    lastPeriodStartMillis = cycleJson.optLong("lastPeriodStartMillis", 0L)
                ),
                indicators = indicators,
                pin = pin
            )
        }
    }
}

private fun JSONObject.optNullableString(key: String): String? {
    if (isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() && it != "null" }
}
