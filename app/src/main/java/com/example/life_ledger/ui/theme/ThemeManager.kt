package com.example.life_ledger.ui.theme

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.life_ledger.R
import com.example.life_ledger.constants.AppConstants
import com.example.life_ledger.utils.PreferenceUtils

/**
 * 主题管理器
 * 负责管理应用的主题模式和自定义主题
 */
object ThemeManager {
    
    private val _currentTheme = MutableLiveData<String>()
    val currentTheme: LiveData<String> = _currentTheme
    
    private val _currentThemeMode = MutableLiveData<String>()
    val currentThemeMode: LiveData<String> = _currentThemeMode
    
    /**
     * 初始化主题管理器
     */
    fun init(context: Context) {
        val savedThemeMode = PreferenceUtils.getString(
            AppConstants.Preferences.KEY_THEME_MODE,
            AppConstants.Theme.MODE_SYSTEM
        )
        val savedCustomTheme = PreferenceUtils.getString(
            AppConstants.Preferences.KEY_CUSTOM_THEME,
            AppConstants.Theme.THEME_DEFAULT
        )
        
        _currentThemeMode.value = savedThemeMode
        _currentTheme.value = savedCustomTheme
        
        applyThemeMode(savedThemeMode)
    }
    
    /**
     * 设置主题模式（浅色/深色/跟随系统）
     */
    fun setThemeMode(mode: String) {
        PreferenceUtils.putString(AppConstants.Preferences.KEY_THEME_MODE, mode)
        _currentThemeMode.value = mode
        applyThemeMode(mode)
    }
    
    /**
     * 设置自定义主题
     */
    fun setCustomTheme(theme: String) {
        PreferenceUtils.putString(AppConstants.Preferences.KEY_CUSTOM_THEME, theme)
        _currentTheme.value = theme
    }
    
    /**
     * 应用主题模式
     */
    private fun applyThemeMode(mode: String) {
        val nightMode = when (mode) {
            AppConstants.Theme.MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppConstants.Theme.MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppConstants.Theme.MODE_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
    
    /**
     * 获取当前主题模式
     */
    fun getCurrentThemeMode(): String {
        return PreferenceUtils.getString(
            AppConstants.Preferences.KEY_THEME_MODE,
            AppConstants.Theme.MODE_SYSTEM
        )
    }
    
    /**
     * 获取当前自定义主题
     */
    fun getCurrentCustomTheme(): String {
        return PreferenceUtils.getString(
            AppConstants.Preferences.KEY_CUSTOM_THEME,
            AppConstants.Theme.THEME_DEFAULT
        )
    }
    
    /**
     * 检查是否为深色模式
     */
    fun isDarkMode(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
    
    /**
     * 获取主题资源ID
     */
    fun getThemeResId(customTheme: String): Int {
        return when (customTheme) {
            AppConstants.Theme.THEME_BLUE -> R.style.Theme_Life_Ledger_Blue
            AppConstants.Theme.THEME_PURPLE -> R.style.Theme_Life_Ledger_Purple
            AppConstants.Theme.THEME_ORANGE -> R.style.Theme_Life_Ledger_Orange
            AppConstants.Theme.THEME_RED -> R.style.Theme_Life_Ledger_Red
            AppConstants.Theme.THEME_TEAL -> R.style.Theme_Life_Ledger_Teal
            AppConstants.Theme.THEME_PINK -> R.style.Theme_Life_Ledger_Pink
            else -> R.style.Theme_Life_Ledger
        }
    }
    
    /**
     * 应用自定义主题到Activity
     */
    fun applyCustomTheme(activity: Activity, customTheme: String) {
        val themeResId = getThemeResId(customTheme)
        activity.setTheme(themeResId)
    }
    
    /**
     * 获取主题显示名称
     */
    fun getThemeDisplayName(context: Context, theme: String): String {
        return when (theme) {
            AppConstants.Theme.THEME_DEFAULT -> context.getString(R.string.theme_default)
            AppConstants.Theme.THEME_BLUE -> context.getString(R.string.theme_blue)
            AppConstants.Theme.THEME_PURPLE -> context.getString(R.string.theme_purple)
            AppConstants.Theme.THEME_ORANGE -> context.getString(R.string.theme_orange)
            AppConstants.Theme.THEME_RED -> context.getString(R.string.theme_red)
            AppConstants.Theme.THEME_TEAL -> context.getString(R.string.theme_teal)
            AppConstants.Theme.THEME_PINK -> context.getString(R.string.theme_pink)
            else -> context.getString(R.string.theme_default)
        }
    }
    
    /**
     * 获取主题模式显示名称
     */
    fun getThemeModeDisplayName(context: Context, mode: String): String {
        return when (mode) {
            AppConstants.Theme.MODE_LIGHT -> context.getString(R.string.theme_mode_light)
            AppConstants.Theme.MODE_DARK -> context.getString(R.string.theme_mode_dark)
            AppConstants.Theme.MODE_SYSTEM -> context.getString(R.string.theme_mode_system)
            else -> context.getString(R.string.theme_mode_system)
        }
    }
    
    /**
     * 获取主题预览颜色
     */
    fun getThemePreviewColor(context: Context, theme: String): Int {
        return when (theme) {
            AppConstants.Theme.THEME_BLUE -> context.getColor(R.color.theme_blue_primary)
            AppConstants.Theme.THEME_PURPLE -> context.getColor(R.color.theme_purple_primary)
            AppConstants.Theme.THEME_ORANGE -> context.getColor(R.color.theme_orange_primary)
            AppConstants.Theme.THEME_RED -> context.getColor(R.color.theme_red_primary)
            AppConstants.Theme.THEME_TEAL -> context.getColor(R.color.theme_teal_primary)
            AppConstants.Theme.THEME_PINK -> context.getColor(R.color.theme_pink_primary)
            else -> context.getColor(R.color.md_theme_primary)
        }
    }
} 