# 知期 (ZhiQi)

知期是一款本地优先（Local-first）的经期与健康记录 Android 应用，聚焦「快速记录、周期预测、趋势洞察、隐私保护」。

当前仓库为持续迭代版本，核心流程已可用，界面与算法仍在优化中。

## 功能概览

### 1. 首页
- 周期环状图：展示当前周期阶段、距离下次月经天数、易孕窗口等信息。
- 今日记录卡片：未记录时支持「立即记录」，已记录时可在当前卡片内展开查看完整内容。
- 周期周历：显示近期日期与关键状态（实际/预测）。

### 2. 记录页
- 月历视图：支持左右滑动切换月份（手势翻页）。
- 月经状态快捷记录：支持“月经来了 / 月经走了”并可撤销。
- 指标录入面板：按指标类型提供对应选项或输入表单。
- 同房记录面板：记录行为时间、防护方式与备注。

### 3. 洞察页
- 最近周期的记录覆盖率与结构占比。
- 基于记录的阶段提示与健康建议（仅用于管理参考）。
- 科普卡片、风险信号与 FAQ 内容。

### 4. 我的
- 生理周期参数设置（周期长度、经期天数、最近一次开始日）。
- 密码能力（数字 PIN）、后台超时自动锁定。
- 每日提醒（WorkManager 定时）与通知敏感词隐藏。
- 本地数据导入/导出（JSON 备份）。

### 5. 桌面小组件
- 提供「今日周期提醒」Widget，展示阶段信息与建议文案。

## 支持记录的指标

- 性行为（爱爱）
- 流量
- 疼痛（症状）
- 情绪（心情）
- 白带
- 体温
- 体重
- 日记
- 睡眠（好习惯）
- 便便
- 药物（计划）

## 技术栈

- 语言：Kotlin 1.9.23
- 构建：AGP 8.3.2 + Gradle Kotlin DSL
- UI：Jetpack Compose + Material3 + Navigation Compose
- 异步：Kotlin Coroutines
- 存储：Room 2.6.1 + SQLCipher 4.5.4（数据库加密）
- 安全：AndroidX Security Crypto（EncryptedSharedPreferences）
- 调度：WorkManager 2.9.1
- 其他：AppWidget、Biometric

## 项目结构

```text
app/src/main/java/com/zhiqi/app
├── data       # Room 实体、DAO、Repository、数据库初始化
├── security   # PIN、数据库密钥、应用锁
├── ui         # Compose 页面、主题、业务 UI 组件
└── widget     # 周期提醒桌面小组件
```

## 开发环境

- Android Studio（建议近两年稳定版）
- JDK 17
- Android SDK：
  - `compileSdk = 34`
  - `targetSdk = 34`
  - `minSdk = 24`
- 本机可用 `gradle` 命令（仓库当前未提交 `gradlew`）

## 本地构建与运行

在项目根目录执行：

```bash
gradle -p . assembleDebug
```

APK 输出目录：

```text
app/build/outputs/apk/debug/
```

首次打开项目时请确保 `local.properties` 中已配置 Android SDK 路径。

## 数据与隐私说明

- 应用默认本地存储，不依赖云端服务。
- `AndroidManifest` 中 `android:allowBackup="false"`，系统级自动备份关闭。
- Room 数据库通过 SQLCipher 加密（数据库文件：`zhiqi.db`）。
- 数据库口令由 `CryptoManager` 生成并保存于加密偏好中。
- PIN 不明文保存，使用 PBKDF2（`PBKDF2WithHmacSHA256`）派生哈希。
- 应用退到后台超过 5 分钟会自动锁定（若密码功能开启）。
- 导出备份默认不包含 PIN；导入会覆盖本地记录与周期配置。

## 备份兼容性

- 当前备份版本：`v2`
- 导出内容：记录、日常指标、周期设置（默认不含密码快照）
- 导入策略：先清空本地后恢复备份内容

## 注意事项

- 项目仍在快速迭代，UI 细节与预测策略会持续调整。
- 健康建议仅作记录管理参考，不替代专业医疗意见。

## 许可证

本项目采用 [MIT License](LICENSE)。
