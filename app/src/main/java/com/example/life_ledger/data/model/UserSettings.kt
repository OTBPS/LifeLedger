package com.example.life_ledger.data.model

import androidx.room.*
import com.example.life_ledger.constants.AppConstants
import java.util.*

/**
 * 用户设置实体类
 * 用于存储用户的个人偏好和应用配置
 */
@Entity(
    tableName = AppConstants.Database.TABLE_USER_SETTINGS,
    indices = [
        Index(value = ["userId"], unique = true)
    ]
)
data class UserSettings(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "userId")
    val userId: String = "default_user", // 用户ID，支持多用户
    
    // 外观设置
    @ColumnInfo(name = "theme")
    val theme: AppTheme = AppTheme.SYSTEM, // 主题模式
    
    @ColumnInfo(name = "primaryColor")
    val primaryColor: String = "#2196F3", // 主色调
    
    @ColumnInfo(name = "accentColor")
    val accentColor: String = "#FF4081", // 强调色
    
    @ColumnInfo(name = "fontSize")
    val fontSize: FontSize = FontSize.MEDIUM, // 字体大小
    
    // 货币和地区设置
    @ColumnInfo(name = "currency")
    val currency: String = "CNY", // 默认货币
    
    @ColumnInfo(name = "currencySymbol")
    val currencySymbol: String = "¥", // 货币符号
    
    @ColumnInfo(name = "language")
    val language: String = "zh-CN", // 语言设置
    
    @ColumnInfo(name = "dateFormat")
    val dateFormat: String = "yyyy-MM-dd", // 日期格式
    
    @ColumnInfo(name = "timeFormat")
    val timeFormat: String = "HH:mm", // 时间格式
    
    @ColumnInfo(name = "firstDayOfWeek")
    val firstDayOfWeek: Int = Calendar.MONDAY, // 一周的第一天
    
    // 通知设置
    @ColumnInfo(name = "notificationEnabled")
    val notificationEnabled: Boolean = true, // 是否启用通知
    
    @ColumnInfo(name = "budgetAlertEnabled")
    val budgetAlertEnabled: Boolean = true, // 预算警告通知
    
    @ColumnInfo(name = "todoReminderEnabled")
    val todoReminderEnabled: Boolean = true, // 待办提醒通知
    
    @ColumnInfo(name = "dailyReportEnabled")
    val dailyReportEnabled: Boolean = false, // 每日报告
    
    @ColumnInfo(name = "reminderTime")
    val reminderTime: String = "09:00", // 默认提醒时间
    
    @ColumnInfo(name = "weeklyReportDay")
    val weeklyReportDay: Int = Calendar.SUNDAY, // 周报发送日
    
    // 预算设置
    @ColumnInfo(name = "budgetAlertThreshold")
    val budgetAlertThreshold: Double = 0.8, // 预算警告阈值（80%）
    
    @ColumnInfo(name = "defaultBudgetPeriod")
    val defaultBudgetPeriod: String = "MONTHLY", // 默认预算周期
    
    @ColumnInfo(name = "autoCreateBudget")
    val autoCreateBudget: Boolean = false, // 自动创建预算
    
    // 财务设置
    @ColumnInfo(name = "defaultTransactionType")
    val defaultTransactionType: String = "EXPENSE", // 默认交易类型
    
    @ColumnInfo(name = "enableQuickAdd")
    val enableQuickAdd: Boolean = true, // 快速添加功能
    
    @ColumnInfo(name = "defaultCategory")
    val defaultCategory: String? = null, // 默认分类
    
    @ColumnInfo(name = "enableReceiptScan")
    val enableReceiptScan: Boolean = true, // 票据扫描功能
    
    // 待办设置
    @ColumnInfo(name = "defaultTodoPriority")
    val defaultTodoPriority: String = "MEDIUM", // 默认待办优先级
    
    @ColumnInfo(name = "autoCompleteSubtasks")
    val autoCompleteSubtasks: Boolean = false, // 自动完成子任务
    
    @ColumnInfo(name = "showCompletedTodos")
    val showCompletedTodos: Boolean = true, // 显示已完成待办
    
    // 安全设置
    @ColumnInfo(name = "enableBiometric")
    val enableBiometric: Boolean = false, // 生物识别
    
    @ColumnInfo(name = "enablePinLock")
    val enablePinLock: Boolean = false, // PIN码锁定
    
    @ColumnInfo(name = "autoLockDuration")
    val autoLockDuration: Int = 300, // 自动锁定时间（秒）
    
    @ColumnInfo(name = "enableDataBackup")
    val enableDataBackup: Boolean = true, // 数据备份
    
    // 数据与同步
    @ColumnInfo(name = "autoBackupInterval")
    val autoBackupInterval: Int = 7, // 自动备份间隔（天）
    
    @ColumnInfo(name = "enableCloudSync")
    val enableCloudSync: Boolean = false, // 云端同步
    
    @ColumnInfo(name = "syncWifiOnly")
    val syncWifiOnly: Boolean = true, // 仅在WiFi下同步
    
    @ColumnInfo(name = "dataRetentionDays")
    val dataRetentionDays: Int = 365, // 数据保留天数
    
    // 统计和报告设置
    @ColumnInfo(name = "enableAdvancedStats")
    val enableAdvancedStats: Boolean = true, // 高级统计
    
    @ColumnInfo(name = "defaultChartType")
    val defaultChartType: String = "PIE", // 默认图表类型
    
    @ColumnInfo(name = "showTrends")
    val showTrends: Boolean = true, // 显示趋势
    
    @ColumnInfo(name = "enableExportData")
    val enableExportData: Boolean = true, // 数据导出
    
    // 隐私设置
    @ColumnInfo(name = "enableAnalytics")
    val enableAnalytics: Boolean = false, // 数据分析
    
    @ColumnInfo(name = "enableCrashReporting")
    val enableCrashReporting: Boolean = true, // 崩溃报告
    
    @ColumnInfo(name = "shareUsageData")
    val shareUsageData: Boolean = false, // 分享使用数据
    
    // 时间戳
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 应用主题枚举
     */
    enum class AppTheme(val displayName: String) {
        LIGHT("浅色"),
        DARK("深色"),
        SYSTEM("跟随系统")
    }
    
    /**
     * 字体大小枚举
     */
    enum class FontSize(val displayName: String, val scale: Float) {
        SMALL("小", 0.85f),
        MEDIUM("中", 1.0f),
        LARGE("大", 1.15f),
        EXTRA_LARGE("特大", 1.3f)
    }
    
    /**
     * 图表类型枚举
     */
    enum class ChartType(val displayName: String) {
        PIE("饼状图"),
        BAR("柱状图"),
        LINE("折线图"),
        DONUT("环形图")
    }
    
    /**
     * 获取提醒时间的小时和分钟
     */
    fun getReminderHourAndMinute(): Pair<Int, Int> {
        val parts = reminderTime.split(":")
        return try {
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            Pair(9, 0) // 默认9:00
        }
    }
    
    /**
     * 设置提醒时间
     */
    fun setReminderTime(hour: Int, minute: Int): UserSettings {
        val timeString = String.format("%02d:%02d", hour, minute)
        return this.copy(
            reminderTime = timeString,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 检查是否启用了安全功能
     */
    fun hasSecurityEnabled(): Boolean {
        return enableBiometric || enablePinLock
    }
    
    /**
     * 检查是否为深色主题
     */
    fun isDarkTheme(): Boolean {
        return theme == AppTheme.DARK
    }
    
    /**
     * 检查是否跟随系统主题
     */
    fun isSystemTheme(): Boolean {
        return theme == AppTheme.SYSTEM
    }
    
    /**
     * 获取自动锁定时间文本
     */
    fun getAutoLockDurationText(): String {
        return when {
            autoLockDuration < 60 -> "${autoLockDuration}秒"
            autoLockDuration < 3600 -> "${autoLockDuration / 60}分钟"
            else -> "${autoLockDuration / 3600}小时"
        }
    }
    
    /**
     * 更新设置
     */
    fun updateSettings(
        theme: AppTheme? = null,
        currency: String? = null,
        language: String? = null,
        notificationEnabled: Boolean? = null,
        budgetAlertThreshold: Double? = null
    ): UserSettings {
        return this.copy(
            theme = theme ?: this.theme,
            currency = currency ?: this.currency,
            language = language ?: this.language,
            notificationEnabled = notificationEnabled ?: this.notificationEnabled,
            budgetAlertThreshold = budgetAlertThreshold ?: this.budgetAlertThreshold,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 启用通知
     */
    fun enableNotifications(): UserSettings {
        return this.copy(
            notificationEnabled = true,
            budgetAlertEnabled = true,
            todoReminderEnabled = true,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 禁用通知
     */
    fun disableNotifications(): UserSettings {
        return this.copy(
            notificationEnabled = false,
            budgetAlertEnabled = false,
            todoReminderEnabled = false,
            dailyReportEnabled = false,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 重置为默认设置
     */
    fun resetToDefault(): UserSettings {
        return UserSettings(
            id = this.id,
            userId = this.userId,
            createdAt = this.createdAt
        )
    }
} 