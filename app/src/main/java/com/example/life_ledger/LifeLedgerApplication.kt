package com.example.life_ledger

import android.app.Application
import com.example.life_ledger.utils.PreferenceUtils

/**
 * LifeLedger 应用程序类
 * 负责全局初始化和依赖管理
 */
class LifeLedgerApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化应用程序组件
        initializeComponents()
    }
    
    /**
     * 初始化应用程序组件
     */
    private fun initializeComponents() {
        // 初始化SharedPreferences工具类
        PreferenceUtils.init(this)
        
        // 初始化其他全局组件
        initializeNotificationChannels()
        
        // 如果是首次启动，进行初始化设置
        if (PreferenceUtils.isFirstLaunch()) {
            performFirstLaunchSetup()
        }
    }
    
    /**
     * 初始化通知渠道
     */
    private fun initializeNotificationChannels() {
        // 这里将在后续步骤中实现通知渠道创建
        // 暂时留空，等待通知功能实现
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
    }
} 