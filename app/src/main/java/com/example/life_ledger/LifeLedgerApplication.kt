package com.example.life_ledger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import com.example.life_ledger.constants.AppConstants
import com.example.life_ledger.data.database.AppDatabase
import com.example.life_ledger.service.BudgetNotificationService
import com.example.life_ledger.ui.theme.ThemeManager
import com.example.life_ledger.utils.PreferenceUtils
import java.io.File

/**
 * LifeLedger 应用程序类
 * 负责全局初始化和依赖管理
 */
class LifeLedgerApplication : Application() {
    
    // 数据库实例
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppConstants.Database.NAME
        )
        .fallbackToDestructiveMigration() // 开发阶段允许破坏性迁移
        .build()
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 清理旧的数据库文件（开发阶段使用）
        clearOldDatabase()
        
        // 初始化应用程序组件
        initializeComponents()
    }
    
    /**
     * 清理旧的数据库文件以解决schema变更问题
     * 仅在开发阶段使用
     */
    private fun clearOldDatabase() {
        try {
            val dbFile = File(this.getDatabasePath("life_ledger_db").absolutePath)
            if (dbFile.exists()) {
                val deleted = dbFile.delete()
                android.util.Log.d("LifeLedgerApp", "Old database deleted: $deleted")
            }
            
            // 也删除相关的文件
            val shmFile = File(this.getDatabasePath("life_ledger_db-shm").absolutePath)
            val walFile = File(this.getDatabasePath("life_ledger_db-wal").absolutePath)
            
            if (shmFile.exists()) {
                shmFile.delete()
                android.util.Log.d("LifeLedgerApp", "SHM file deleted")
            }
            
            if (walFile.exists()) {
                walFile.delete()
                android.util.Log.d("LifeLedgerApp", "WAL file deleted")
            }
        } catch (e: Exception) {
            android.util.Log.e("LifeLedgerApp", "Error clearing database", e)
        }
    }
    
    /**
     * 初始化应用程序组件
     */
    private fun initializeComponents() {
        // 初始化SharedPreferences工具类
        PreferenceUtils.init(this)
        
        // 初始化主题管理器
        ThemeManager.init(this)
        
        // 初始化其他全局组件
        initializeNotificationChannels()
        
        // 启动预算通知服务
        startBudgetNotificationService()
        
        // 如果是首次启动，进行初始化设置
        if (PreferenceUtils.isFirstLaunch()) {
            performFirstLaunchSetup()
        }
    }
    
    /**
     * 初始化通知渠道
     */
    private fun initializeNotificationChannels() {
        // 预算通知服务会自动创建通知渠道
        // 这里可以添加其他通知渠道的创建
        
        // 创建通知渠道
        createNotificationChannels()
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // 提醒通知渠道
            val reminderChannel = NotificationChannel(
                AppConstants.Notification.CHANNEL_ID_REMINDERS,
                "Reminder notification",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "To dos and financial reminders"
            }
            
            // 预算警告通知渠道
            val budgetChannel = NotificationChannel(
                AppConstants.Notification.CHANNEL_ID_BUDGET,
                "Budget Warning",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Budget overrun warning"
            }
            
            // 一般通知渠道
            val generalChannel = NotificationChannel(
                AppConstants.Notification.CHANNEL_ID_GENERAL,
                "General Notice",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Application General Notification"
            }
            
            notificationManager.createNotificationChannels(
                listOf(reminderChannel, budgetChannel, generalChannel)
            )
        }
    }
    
    /**
     * 启动预算通知服务
     */
    private fun startBudgetNotificationService() {
        // 只有在用户启用通知的情况下才启动服务
        if (PreferenceUtils.isNotificationEnabled()) {
            BudgetNotificationService.startBudgetNotificationWork(this)
        }
    }
    
    /**
     * 首次启动设置
     */
    private fun performFirstLaunchSetup() {
        // 设置默认主题
        PreferenceUtils.setUserTheme("system")
        
        // 启用通知
        PreferenceUtils.setNotificationEnabled(true)
        
        // 设置默认货币
        PreferenceUtils.setUserCurrency("CNY")
        
        // 设置默认语言
        PreferenceUtils.setUserLanguage("zh")
        
        // 标记已完成首次启动
        PreferenceUtils.setFirstLaunch(false)
        
        // 启动预算通知服务
        startBudgetNotificationService()
    }
    
    /**
     * 设置全局异常处理器
     */
    private fun setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            // 记录异常信息
            android.util.Log.e("LifeLedger", "Uncaught exception in thread ${thread.name}", exception)
            
            // 这里可以添加崩溃报告逻辑
            // 例如发送到崩溃分析服务
            
            // 调用系统默认处理器
            System.exit(1)
        }
    }
    
    companion object {
        /**
         * 获取应用实例
         */
        fun getInstance(context: Context): LifeLedgerApplication {
            return context.applicationContext as LifeLedgerApplication
        }
    }
} 