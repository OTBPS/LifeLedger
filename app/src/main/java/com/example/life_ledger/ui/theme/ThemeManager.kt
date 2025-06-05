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
 * Theme Manager
 * Responsible for managing application theme mode and custom themes
 */
object ThemeManager {
    
    private val _currentTheme = MutableLiveData<String>()
    val currentTheme: LiveData<String> = _currentTheme
    
    private val _currentThemeMode = MutableLiveData<String>()
    val currentThemeMode: LiveData<String> = _currentThemeMode
    
    /**
     * Initialize theme manager
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
     * Set theme mode (light/dark/follow system)
     */
    fun setThemeMode(mode: String) {
        PreferenceUtils.putString(AppConstants.Preferences.KEY_THEME_MODE, mode)
        _currentThemeMode.value = mode
        applyThemeMode(mode)
    }
    
    /**
     * Set custom theme
     */
    fun setCustomTheme(theme: String) {
        PreferenceUtils.putString(AppConstants.Preferences.KEY_CUSTOM_THEME, theme)
        _currentTheme.value = theme
    }
    
    /**
     * Apply theme mode
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
     * Get current theme mode
     */
    fun getCurrentThemeMode(): String {
        return PreferenceUtils.getString(
            AppConstants.Preferences.KEY_THEME_MODE,
            AppConstants.Theme.MODE_SYSTEM
        )
    }
    
    /**
     * Get current custom theme
     */
    fun getCurrentCustomTheme(): String {
        return PreferenceUtils.getString(
            AppConstants.Preferences.KEY_CUSTOM_THEME,
            AppConstants.Theme.THEME_DEFAULT
        )
    }
    
    /**
     * Check if it's dark mode
     */
    fun isDarkMode(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
    
    /**
     * Get theme resource ID
     */
    fun getThemeResId(customTheme: String): Int {
        return when (customTheme) {
            AppConstants.Theme.THEME_BLUE -> R.style.Theme_LifeLedger_Blue
            AppConstants.Theme.THEME_PURPLE -> R.style.Theme_LifeLedger_Purple
            AppConstants.Theme.THEME_ORANGE -> R.style.Theme_LifeLedger_Orange
            AppConstants.Theme.THEME_RED -> R.style.Theme_LifeLedger_Red
            AppConstants.Theme.THEME_TEAL -> R.style.Theme_LifeLedger_Teal
            AppConstants.Theme.THEME_PINK -> R.style.Theme_Life_Ledger_Pink
            else -> R.style.Theme_Life_Ledger
        }
    }
    
    /**
     * Apply custom theme to Activity
     */
    fun applyCustomTheme(activity: Activity, customTheme: String) {
        val themeResId = getThemeResId(customTheme)
        activity.setTheme(themeResId)
    }
    
    /**
     * Get theme display name
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
     * Get theme mode display name
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
     * Get theme preview color
     */
    fun getThemePreviewColor(context: Context, theme: String): Int {
        return when (theme) {
            AppConstants.Theme.THEME_BLUE -> context.getColor(R.color.blue_primary)
            AppConstants.Theme.THEME_PURPLE -> context.getColor(R.color.purple_primary)
            AppConstants.Theme.THEME_ORANGE -> context.getColor(R.color.orange_primary)
            AppConstants.Theme.THEME_RED -> context.getColor(R.color.theme_red_primary)
            AppConstants.Theme.THEME_TEAL -> context.getColor(R.color.theme_teal_primary)
            AppConstants.Theme.THEME_PINK -> context.getColor(R.color.theme_pink_primary)
            else -> context.getColor(R.color.md_theme_primary)
        }
    }
} 