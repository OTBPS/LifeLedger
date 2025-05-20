package com.example.life_ledger.data.dao

import androidx.room.*
import com.example.life_ledger.data.model.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * 用户设置数据访问对象
 * 提供对UserSettings表的所有数据库操作
 */
@Dao
interface UserSettingsDao {
    
    // ==================== 基础CRUD操作 ====================
    
    /**
     * 插入用户设置
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userSettings: UserSettings): Long
    
    /**
     * 更新用户设置
     */
    @Update
    suspend fun update(userSettings: UserSettings): Int
    
    /**
     * 删除用户设置
     */
    @Delete
    suspend fun delete(userSettings: UserSettings): Int
    
    /**
     * 根据ID删除用户设置
     */
    @Query("DELETE FROM user_settings WHERE id = :id")
    suspend fun deleteById(id: String): Int
    
    /**
     * 根据用户ID删除设置
     */
    @Query("DELETE FROM user_settings WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String): Int
    
    /**
     * 删除所有用户设置
     */
    @Query("DELETE FROM user_settings")
    suspend fun deleteAll(): Int
    
    // ==================== 查询操作 ====================
    
    /**
     * 根据ID获取用户设置
     */
    @Query("SELECT * FROM user_settings WHERE id = :id")
    suspend fun getById(id: String): UserSettings?
    
    /**
     * 根据用户ID获取设置
     */
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): UserSettings?
    
    /**
     * 根据用户ID获取设置（Flow响应式）
     */
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    fun getByUserIdFlow(userId: String): Flow<UserSettings?>
    
    /**
     * 获取默认用户设置
     */
    @Query("SELECT * FROM user_settings WHERE userId = 'default_user' LIMIT 1")
    suspend fun getDefaultSettings(): UserSettings?
    
    /**
     * 获取默认用户设置（Flow）
     */
    @Query("SELECT * FROM user_settings WHERE userId = 'default_user' LIMIT 1")
    fun getDefaultSettingsFlow(): Flow<UserSettings?>
    
    /**
     * 获取所有用户设置
     */
    @Query("SELECT * FROM user_settings ORDER BY createdAt DESC")
    suspend fun getAll(): List<UserSettings>
    
    /**
     * 获取所有用户设置（Flow）
     */
    @Query("SELECT * FROM user_settings ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<UserSettings>>
    
    // ==================== 检查操作 ====================
    
    /**
     * 检查用户设置是否存在
     */
    @Query("SELECT COUNT(*) > 0 FROM user_settings WHERE userId = :userId")
    suspend fun exists(userId: String): Boolean
    
    /**
     * 检查是否有任何设置
     */
    @Query("SELECT COUNT(*) > 0 FROM user_settings")
    suspend fun hasAnySettings(): Boolean
    
    /**
     * 获取用户数量
     */
    @Query("SELECT COUNT(DISTINCT userId) FROM user_settings")
    suspend fun getUserCount(): Int
    
    /**
     * 获取设置总数
     */
    @Query("SELECT COUNT(*) FROM user_settings")
    suspend fun getCount(): Int
    
    // ==================== 设置分类查询 ====================
    
    /**
     * 获取主题设置
     */
    @Query("SELECT theme FROM user_settings WHERE userId = :userId")
    suspend fun getTheme(userId: String): UserSettings.AppTheme?
    
    /**
     * 获取货币设置
     */
    @Query("SELECT currency, currencySymbol FROM user_settings WHERE userId = :userId")
    suspend fun getCurrencySettings(userId: String): CurrencySettings?
    
    /**
     * 获取通知设置
     */
    @Query("""
        SELECT notificationEnabled, budgetAlertEnabled, todoReminderEnabled, 
               dailyReportEnabled, reminderTime 
        FROM user_settings WHERE userId = :userId
    """)
    suspend fun getNotificationSettings(userId: String): NotificationSettings?
    
    /**
     * 获取安全设置
     */
    @Query("""
        SELECT enableBiometric, enablePinLock, autoLockDuration 
        FROM user_settings WHERE userId = :userId
    """)
    suspend fun getSecuritySettings(userId: String): SecuritySettings?
    
    // ==================== 批量更新操作 ====================
    
    /**
     * 更新主题
     */
    @Query("UPDATE user_settings SET theme = :theme, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateTheme(userId: String, theme: UserSettings.AppTheme, updatedAt: Long): Int
    
    /**
     * 更新货币设置
     */
    @Query("""
        UPDATE user_settings 
        SET currency = :currency, currencySymbol = :currencySymbol, updatedAt = :updatedAt 
        WHERE userId = :userId
    """)
    suspend fun updateCurrency(userId: String, currency: String, currencySymbol: String, updatedAt: Long): Int
    
    /**
     * 更新语言设置
     */
    @Query("UPDATE user_settings SET language = :language, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateLanguage(userId: String, language: String, updatedAt: Long): Int
    
    /**
     * 更新通知总开关
     */
    @Query("UPDATE user_settings SET notificationEnabled = :enabled, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateNotificationEnabled(userId: String, enabled: Boolean, updatedAt: Long): Int
    
    /**
     * 更新预算警告设置
     */
    @Query("UPDATE user_settings SET budgetAlertEnabled = :enabled, budgetAlertThreshold = :threshold, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateBudgetAlert(userId: String, enabled: Boolean, threshold: Double, updatedAt: Long): Int
    
    /**
     * 更新待办提醒设置
     */
    @Query("UPDATE user_settings SET todoReminderEnabled = :enabled, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateTodoReminder(userId: String, enabled: Boolean, updatedAt: Long): Int
    
    /**
     * 更新提醒时间
     */
    @Query("UPDATE user_settings SET reminderTime = :reminderTime, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateReminderTime(userId: String, reminderTime: String, updatedAt: Long): Int
    
    /**
     * 更新生物识别设置
     */
    @Query("UPDATE user_settings SET enableBiometric = :enabled, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateBiometric(userId: String, enabled: Boolean, updatedAt: Long): Int
    
    /**
     * 更新PIN锁设置
     */
    @Query("UPDATE user_settings SET enablePinLock = :enabled, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updatePinLock(userId: String, enabled: Boolean, updatedAt: Long): Int
    
    /**
     * 更新自动锁定时间
     */
    @Query("UPDATE user_settings SET autoLockDuration = :duration, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateAutoLockDuration(userId: String, duration: Int, updatedAt: Long): Int
    
    /**
     * 更新数据备份设置
     */
    @Query("UPDATE user_settings SET enableDataBackup = :enabled, autoBackupInterval = :interval, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateBackupSettings(userId: String, enabled: Boolean, interval: Int, updatedAt: Long): Int
    
    /**
     * 更新云同步设置
     */
    @Query("UPDATE user_settings SET enableCloudSync = :enabled, syncWifiOnly = :wifiOnly, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateCloudSync(userId: String, enabled: Boolean, wifiOnly: Boolean, updatedAt: Long): Int
    
    /**
     * 更新默认分类设置
     */
    @Query("UPDATE user_settings SET defaultCategory = :categoryId, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateDefaultCategory(userId: String, categoryId: String?, updatedAt: Long): Int
    
    /**
     * 更新默认交易类型
     */
    @Query("UPDATE user_settings SET defaultTransactionType = :type, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateDefaultTransactionType(userId: String, type: String, updatedAt: Long): Int
    
    /**
     * 更新默认待办优先级
     */
    @Query("UPDATE user_settings SET defaultTodoPriority = :priority, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateDefaultTodoPriority(userId: String, priority: String, updatedAt: Long): Int
    
    // ==================== 特殊功能操作 ====================
    
    /**
     * 启用所有通知
     */
    @Query("""
        UPDATE user_settings 
        SET notificationEnabled = 1, budgetAlertEnabled = 1, todoReminderEnabled = 1, updatedAt = :updatedAt 
        WHERE userId = :userId
    """)
    suspend fun enableAllNotifications(userId: String, updatedAt: Long): Int
    
    /**
     * 禁用所有通知
     */
    @Query("""
        UPDATE user_settings 
        SET notificationEnabled = 0, budgetAlertEnabled = 0, todoReminderEnabled = 0, dailyReportEnabled = 0, updatedAt = :updatedAt 
        WHERE userId = :userId
    """)
    suspend fun disableAllNotifications(userId: String, updatedAt: Long): Int
    
    /**
     * 启用所有安全功能
     */
    @Query("""
        UPDATE user_settings 
        SET enableBiometric = 1, enablePinLock = 1, updatedAt = :updatedAt 
        WHERE userId = :userId
    """)
    suspend fun enableAllSecurity(userId: String, updatedAt: Long): Int
    
    /**
     * 禁用所有安全功能
     */
    @Query("""
        UPDATE user_settings 
        SET enableBiometric = 0, enablePinLock = 0, updatedAt = :updatedAt 
        WHERE userId = :userId
    """)
    suspend fun disableAllSecurity(userId: String, updatedAt: Long): Int
    
    /**
     * 重置为默认设置（保留用户ID和创建时间）
     */
    @Query("""
        UPDATE user_settings 
        SET theme = 'SYSTEM', currency = 'CNY', currencySymbol = '¥', language = 'zh-CN',
            notificationEnabled = 1, budgetAlertEnabled = 1, todoReminderEnabled = 1,
            reminderTime = '09:00', enableBiometric = 0, enablePinLock = 0,
            updatedAt = :updatedAt
        WHERE userId = :userId
    """)
    suspend fun resetToDefaults(userId: String, updatedAt: Long): Int
    
    // ==================== 数据维护操作 ====================
    
    /**
     * 更新最后修改时间
     */
    @Query("UPDATE user_settings SET updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateLastModified(userId: String, updatedAt: Long): Int
    
    /**
     * 获取最近修改的设置
     */
    @Query("SELECT * FROM user_settings ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentlyModified(limit: Int = 5): List<UserSettings>
    
    /**
     * 获取过期的设置（超过指定时间未更新）
     */
    @Query("SELECT * FROM user_settings WHERE updatedAt < :cutoffTime")
    suspend fun getOutdatedSettings(cutoffTime: Long): List<UserSettings>
}

/**
 * 货币设置数据类
 */
data class CurrencySettings(
    val currency: String,
    val currencySymbol: String
)

/**
 * 通知设置数据类
 */
data class NotificationSettings(
    val notificationEnabled: Boolean,
    val budgetAlertEnabled: Boolean,
    val todoReminderEnabled: Boolean,
    val dailyReportEnabled: Boolean,
    val reminderTime: String
)

/**
 * 安全设置数据类
 */
data class SecuritySettings(
    val enableBiometric: Boolean,
    val enablePinLock: Boolean,
    val autoLockDuration: Int
) 