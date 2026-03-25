package com.zhiqi.app.ui

import androidx.compose.ui.graphics.Color

data class IndicatorOption(
    val value: String,
    val label: String
)

// metricKey 是持久化用的原始键名，label 只是界面展示文案；不要随意重命名存储键。
fun metricTitle(metricKey: String): String = when (metricKey) {
    "爱爱" -> "爱爱"
    "流量" -> "流量"
    "症状" -> "疼痛"
    "心情" -> "情绪"
    "白带" -> "白带"
    "体温" -> "体温"
    "体重" -> "体重"
    "日记" -> "日记"
    "好习惯" -> "睡眠"
    "便便" -> "便便"
    "计划" -> "药物"
    else -> metricKey
}

fun metricAccent(metricKey: String): Color = when (metricKey) {
    "爱爱" -> ZhiQiTokens.PrimaryStrong
    "流量" -> ZhiQiTokens.Primary
    "症状" -> Color(0xFF58A8D8)
    "心情" -> Color(0xFFE2B866)
    "白带" -> Color(0xFFA177D9)
    "体温" -> Color(0xFF8E79DC)
    "体重" -> Color(0xFFB389D5)
    "日记" -> Color(0xFFD69A60)
    "好习惯" -> Color(0xFF59AFC9)
    "便便" -> Color(0xFFCCA667)
    "计划" -> Color(0xFF6FA8D8)
    else -> ZhiQiTokens.Primary
}

fun metricOptions(metricKey: String): List<IndicatorOption> = when (metricKey) {
    "流量" -> listOf(
        IndicatorOption("spotting", "点滴"),
        IndicatorOption("light", "少量"),
        IndicatorOption("medium", "中等"),
        IndicatorOption("heavy", "偏多"),
        IndicatorOption("very_heavy", "大量")
    )
    "症状" -> listOf(
        IndicatorOption("none", "无疼痛"),
        IndicatorOption("light_pain", "轻微疼痛"),
        IndicatorOption("moderate_pain", "中度疼痛"),
        IndicatorOption("severe_pain", "重度疼痛"),
        IndicatorOption("cramp", "绞痛"),
        IndicatorOption("dull_ache", "持续隐痛")
    )
    "心情" -> listOf(
        IndicatorOption("happy", "开心"),
        IndicatorOption("calm", "平静"),
        IndicatorOption("sensitive", "敏感"),
        IndicatorOption("irritable", "烦躁"),
        IndicatorOption("sad", "难过")
    )
    "白带" -> listOf(
        IndicatorOption("clear", "透明"),
        IndicatorOption("stretchy", "拉丝"),
        IndicatorOption("milky", "乳白"),
        IndicatorOption("yellow", "偏黄"),
        IndicatorOption("odor", "异味")
    )
    "体温" -> listOf(
        IndicatorOption("low", "偏低"),
        IndicatorOption("normal", "正常"),
        IndicatorOption("high", "偏高")
    )
    "体重" -> listOf(
        IndicatorOption("stable", "稳定"),
        IndicatorOption("up", "略增"),
        IndicatorOption("down", "略降"),
        IndicatorOption("fluctuate", "波动")
    )
    "日记" -> listOf(
        IndicatorOption("body", "身体"),
        IndicatorOption("work", "工作"),
        IndicatorOption("emotion", "情绪"),
        IndicatorOption("relation", "关系"),
        IndicatorOption("other", "其他")
    )
    "好习惯" -> listOf(
        IndicatorOption("sleep_lt5", "少于5小时"),
        IndicatorOption("sleep_5_6", "5-6小时"),
        IndicatorOption("sleep_6_8", "6-8小时"),
        IndicatorOption("sleep_gt8", "8小时以上"),
        IndicatorOption("sleep_poor", "入睡困难"),
        IndicatorOption("sleep_broken", "易醒多梦")
    )
    "便便" -> listOf(
        IndicatorOption("normal", "正常"),
        IndicatorOption("dry", "偏干"),
        IndicatorOption("loose", "偏稀"),
        IndicatorOption("hard", "困难"),
        IndicatorOption("many", "多次")
    )
    "计划" -> listOf(
        IndicatorOption("none", "未用药"),
        IndicatorOption("painkiller", "止痛药"),
        IndicatorOption("antispasmodic", "解痉药"),
        IndicatorOption("hormone", "激素类"),
        IndicatorOption("supplement", "补充剂"),
        IndicatorOption("other_medicine", "其他药物")
    )
    else -> emptyList()
}
