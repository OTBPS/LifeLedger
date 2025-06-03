package com.example.life_ledger.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.life_ledger.constants.AppConstants

/**
 * SharedPreferences工具类
 * 提供应用设置和数据存储相关方法
 */
object PreferenceUtils {
    
    private lateinit var sharedPreferences: SharedPreferences
    
    /**
     * 初始化SharedPreferences
     */
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(
            AppConstants.Preferences.PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }
    
    /**
     * 保存字符串值
     */
    fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }
    
    /**
     * 获取字符串值
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }
    
    /**
     * 保存布尔值
     */
    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }
    
    /**
     * 获取布尔值
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }
    
    /**
     * 保存整数值
     */
    fun putInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }
    
    /**
     * 获取整数值
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }
    
    /**
     * 保存长整数值
     */
    fun putLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }
    
    /**
     * 获取长整数值
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }
    
    /**
     * 保存浮点数值
     */
    fun putFloat(key: String, value: Float) {
        sharedPreferences.edit().putFloat(key, value).apply()
    }
    
    /**
     * 获取浮点数值
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }
    
    /**
     * 移除指定键
     */
    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }
    
    /**
     * 清除所有数据
     */
    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
    
    /**
     * 检查是否包含指定键
     */
    fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }
    
    // 应用特定设置方法
    
    /**
     * 是否首次启动
     */
    fun isFirstLaunch(): Boolean {
        return getBoolean(AppConstants.Preferences.KEY_FIRST_LAUNCH, true)
    }
    
    /**
     * 设置首次启动标记
     */
    fun setFirstLaunch(isFirstLaunch: Boolean) {
        putBoolean(AppConstants.Preferences.KEY_FIRST_LAUNCH, isFirstLaunch)
    }
    
    /**
     * 获取用户主题
     */
    fun getUserTheme(): String {
        return getString(AppConstants.Preferences.KEY_USER_THEME, "system")
    }
    
    /**
     * 设置用户主题
     */
    fun setUserTheme(theme: String) {
        putString(AppConstants.Preferences.KEY_USER_THEME, theme)
    }
    
    /**
     * 获取主题模式
     */
    fun getThemeMode(): String {
        return getString(AppConstants.Preferences.KEY_THEME_MODE, AppConstants.Theme.MODE_SYSTEM)
    }
    
    /**
     * 设置主题模式
     */
    fun setThemeMode(mode: String) {
        putString(AppConstants.Preferences.KEY_THEME_MODE, mode)
    }
    
    /**
     * 获取自定义主题
     */
    fun getCustomTheme(): String {
        return getString(AppConstants.Preferences.KEY_CUSTOM_THEME, AppConstants.Theme.THEME_DEFAULT)
    }
    
    /**
     * 设置自定义主题
     */
    fun setCustomTheme(theme: String) {
        putString(AppConstants.Preferences.KEY_CUSTOM_THEME, theme)
    }
    
    /**
     * 是否启用通知
     */
    fun isNotificationEnabled(): Boolean {
        return getBoolean(AppConstants.Preferences.KEY_NOTIFICATION_ENABLED, true)
    }
    
    /**
     * 设置通知开关
     */
    fun setNotificationEnabled(enabled: Boolean) {
        putBoolean(AppConstants.Preferences.KEY_NOTIFICATION_ENABLED, enabled)
    }
    
    /**
     * 获取用户货币
     */
    fun getUserCurrency(): String {
        return getString(AppConstants.Preferences.KEY_CURRENCY, AppConstants.Finance.DEFAULT_CURRENCY)
    }
    
    /**
     * 设置用户货币
     */
    fun setUserCurrency(currency: String) {
        putString(AppConstants.Preferences.KEY_CURRENCY, currency)
    }
    
    /**
     * 获取用户语言
     */
    fun getUserLanguage(): String {
        return getString(AppConstants.Preferences.KEY_LANGUAGE, "zh")
    }
    
    /**
     * 设置用户语言
     */
    fun setUserLanguage(language: String) {
        putString(AppConstants.Preferences.KEY_LANGUAGE, language)
    }
} 