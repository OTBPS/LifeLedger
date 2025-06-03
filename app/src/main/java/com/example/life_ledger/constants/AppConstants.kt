package com.example.life_ledger.constants

/**
 * 应用程序常量类
 * 定义全局使用的常量值
 */
object AppConstants {
    
    // 数据库相关常量
    object Database {
        const val NAME = "life_ledger_db_v2"  // 修改数据库名称以创建全新数据库
        const val VERSION = 1  // 重置版本号
        
        // 表名
        const val TABLE_TRANSACTIONS = "transactions"
        const val TABLE_TODOS = "todo_items"
        const val TABLE_CATEGORIES = "categories"
        const val TABLE_BUDGETS = "budgets"
        const val TABLE_USER_SETTINGS = "user_settings"
        
        // 查询限制
        const val DEFAULT_LIMIT = 50
        const val MAX_LIMIT = 1000
    }
    
    // 网络请求相关常量
    object Network {
        const val BASE_URL = "https://api.deepseek.com/"
        const val API_KEY = "sk-7b6e2e43870740758764e747e76aae2d"
        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 30L
        const val WRITE_TIMEOUT = 30L
    }
    
    // SharedPreferences相关常量
    object Preferences {
        const val PREFS_NAME = "life_ledger_prefs"
        const val KEY_FIRST_LAUNCH = "first_launch"
        const val KEY_USER_THEME = "user_theme"
        const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        const val KEY_CURRENCY = "currency"
        const val KEY_LANGUAGE = "language"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_CUSTOM_THEME = "custom_theme"
    }
    
    // 主题相关常量
    object Theme {
        const val MODE_SYSTEM = "system"
        const val MODE_LIGHT = "light"
        const val MODE_DARK = "dark"
        
        // 自定义主题
        const val THEME_DEFAULT = "default"
        const val THEME_BLUE = "blue"
        const val THEME_PURPLE = "purple"
        const val THEME_ORANGE = "orange"
        const val THEME_RED = "red"
        const val THEME_TEAL = "teal"
        const val THEME_PINK = "pink"
        
        val AVAILABLE_THEMES = listOf(
            THEME_DEFAULT,
            THEME_BLUE,
            THEME_PURPLE,
            THEME_ORANGE,
            THEME_RED,
            THEME_TEAL,
            THEME_PINK
        )
    }
    
    // 日期格式常量
    object DateFormat {
        const val DATE_FORMAT_DISPLAY = "yyyy-MM-dd"
        const val DATE_FORMAT_FULL = "yyyy-MM-dd HH:mm:ss"
        const val DATE_FORMAT_MONTH = "yyyy-MM"
        const val DATE_FORMAT_YEAR = "yyyy"
        const val TIME_FORMAT_12H = "hh:mm a"
        const val TIME_FORMAT_24H = "HH:mm"
    }
    
    // 财务相关常量
    object Finance {
        const val DEFAULT_CURRENCY = "CNY"
        val SUPPORTED_CURRENCIES = listOf("CNY", "USD", "EUR", "JPY", "GBP")
        const val MAX_AMOUNT = 999999999.99
        const val MIN_AMOUNT = 0.01
    }
    
    // 待办事项相关常量
    object Todo {
        const val HIGH_PRIORITY = 3
        const val MEDIUM_PRIORITY = 2
        const val LOW_PRIORITY = 1
        const val DEFAULT_PRIORITY = MEDIUM_PRIORITY
    }
    
    // 通知相关常量
    object Notification {
        const val CHANNEL_ID_REMINDERS = "reminders"
        const val CHANNEL_ID_BUDGET = "budget_alerts"
        const val CHANNEL_ID_GENERAL = "general"
        const val NOTIFICATION_ID_BUDGET_ALERT = 1001
        const val NOTIFICATION_ID_TODO_REMINDER = 1002
    }
    
    // 图表相关常量
    object Chart {
        const val CHART_ANIMATION_DURATION = 1000
        const val PIE_CHART_HOLE_RADIUS = 45f
        const val PIE_CHART_TRANSPARENT_CIRCLE_RADIUS = 50f
        const val LINE_CHART_LINE_WIDTH = 2f
    }
    
    // Intent Extra键值
    object IntentExtra {
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_CATEGORY_ID = "category_id"
        const val EXTRA_DATE = "date"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_TYPE = "type"
    }
    
    // 错误码
    object ErrorCode {
        const val NETWORK_ERROR = 1001
        const val DATABASE_ERROR = 1002
        const val VALIDATION_ERROR = 1003
        const val PERMISSION_ERROR = 1004
        const val UNKNOWN_ERROR = 1999
    }
} 