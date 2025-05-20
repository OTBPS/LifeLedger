package com.example.life_ledger.data.database

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.life_ledger.constants.AppConstants
import com.example.life_ledger.data.dao.*
import com.example.life_ledger.data.model.Transaction
import com.example.life_ledger.data.model.TodoItem
import com.example.life_ledger.data.model.Category
import com.example.life_ledger.data.model.Budget
import com.example.life_ledger.data.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * LifeLedger应用主数据库
 * 使用Room框架管理SQLite数据库
 */
@Database(
    entities = [
        Transaction::class,
        TodoItem::class,
        Category::class,
        Budget::class,
        UserSettings::class
    ],
    version = AppConstants.Database.VERSION,
    exportSchema = false
)
@TypeConverters(DatabaseTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    
    // ==================== DAO 接口 ====================
    
    abstract fun transactionDao(): TransactionDao
    abstract fun todoDao(): TodoDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun userSettingsDao(): UserSettingsDao
    
    // ==================== 数据库实例管理 ====================
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 获取数据库实例（单例模式）
         */
        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    AppConstants.Database.NAME
                )
                    .addCallback(DatabaseCallback())
                    .addMigrations(/* 未来的数据库迁移 */)
                    .fallbackToDestructiveMigration() // 开发阶段使用，生产环境需要移除
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * 关闭数据库实例
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
    
    /**
     * 数据库回调，用于初始化默认数据
     */
    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            
            // 在后台线程初始化默认数据
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    populateDatabase(database)
                }
            }
        }
        
        /**
         * 填充默认数据
         */
        private suspend fun populateDatabase(database: AppDatabase) {
            try {
                // 插入默认分类
                val categoryDao = database.categoryDao()
                
                // 插入默认财务分类
                val financialCategories = Category.createDefaultFinancialCategories()
                categoryDao.insertAll(financialCategories)
                
                // 插入默认待办分类
                val todoCategories = Category.createDefaultTodoCategories()
                categoryDao.insertAll(todoCategories)
                
                // 创建默认用户设置
                val userSettingsDao = database.userSettingsDao()
                val defaultSettings = UserSettings()
                userSettingsDao.insert(defaultSettings)
                
            } catch (e: Exception) {
                // 记录错误，但不阻塞应用启动
                e.printStackTrace()
            }
        }
    }
}

/**
 * 数据库类型转换器
 * 用于Room无法直接处理的数据类型转换
 */
class DatabaseTypeConverters {
    
    // ==================== 枚举类型转换器 ====================
    
    @TypeConverter
    fun fromTransactionType(type: Transaction.TransactionType): String {
        return type.name
    }
    
    @TypeConverter
    fun toTransactionType(type: String): Transaction.TransactionType {
        return Transaction.TransactionType.valueOf(type)
    }
    
    @TypeConverter
    fun fromPaymentMethod(method: Transaction.PaymentMethod): String {
        return method.name
    }
    
    @TypeConverter
    fun toPaymentMethod(method: String): Transaction.PaymentMethod {
        return Transaction.PaymentMethod.valueOf(method)
    }
    
    @TypeConverter
    fun fromTodoPriority(priority: TodoItem.Priority): String {
        return priority.name
    }
    
    @TypeConverter
    fun toTodoPriority(priority: String): TodoItem.Priority {
        return TodoItem.Priority.valueOf(priority)
    }
    
    @TypeConverter
    fun fromCategoryType(type: Category.CategoryType): String {
        return type.name
    }
    
    @TypeConverter
    fun toCategoryType(type: String): Category.CategoryType {
        return Category.CategoryType.valueOf(type)
    }
    
    @TypeConverter
    fun fromBudgetPeriod(period: Budget.BudgetPeriod): String {
        return period.name
    }
    
    @TypeConverter
    fun toBudgetPeriod(period: String): Budget.BudgetPeriod {
        return Budget.BudgetPeriod.valueOf(period)
    }
    
    @TypeConverter
    fun fromAppTheme(theme: UserSettings.AppTheme): String {
        return theme.name
    }
    
    @TypeConverter
    fun toAppTheme(theme: String): UserSettings.AppTheme {
        return UserSettings.AppTheme.valueOf(theme)
    }
    
    @TypeConverter
    fun fromFontSize(fontSize: UserSettings.FontSize): String {
        return fontSize.name
    }
    
    @TypeConverter
    fun toFontSize(fontSize: String): UserSettings.FontSize {
        return UserSettings.FontSize.valueOf(fontSize)
    }
    
    // ==================== 集合类型转换器 ====================
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
    
    @TypeConverter
    fun fromLongList(value: List<Long>): String {
        return value.joinToString(",")
    }
    
    @TypeConverter
    fun toLongList(value: String): List<Long> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            value.split(",").mapNotNull { it.toLongOrNull() }
        }
    }
    
    // ==================== 特殊数据类型转换器 ====================
    
    /**
     * Map<String, String> 转换器
     * 用于存储键值对数据
     */
    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return value.entries.joinToString(";") { "${it.key}:${it.value}" }
    }
    
    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        if (value.isEmpty()) return emptyMap()
        return value.split(";").associate { pair ->
            val (key, value) = pair.split(":", limit = 2)
            key to value
        }
    }
    
    /**
     * JSON字符串处理
     * 用于复杂对象的序列化存储
     */
    @TypeConverter
    fun fromJson(json: String?): Map<String, Any>? {
        return if (json.isNullOrEmpty()) {
            null
        } else {
            try {
                // 这里可以使用Gson或其他JSON解析库
                // 为简化示例，使用简单的键值对解析
                parseSimpleJson(json)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    @TypeConverter
    fun toJson(data: Map<String, Any>?): String? {
        return if (data == null || data.isEmpty()) {
            null
        } else {
            try {
                // 简单的JSON序列化
                buildSimpleJson(data)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * 简单的JSON解析（仅支持字符串值）
     */
    private fun parseSimpleJson(json: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        // 移除大括号
        val content = json.trim().removeSurrounding("{", "}")
        if (content.isNotEmpty()) {
            content.split(",").forEach { pair ->
                val parts = pair.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().removeSurrounding("\"")
                    val value = parts[1].trim().removeSurrounding("\"")
                    result[key] = value
                }
            }
        }
        return result
    }
    
    /**
     * 简单的JSON构建（仅支持字符串值）
     */
    private fun buildSimpleJson(data: Map<String, Any>): String {
        val pairs = data.entries.joinToString(",") { (key, value) ->
            "\"$key\":\"$value\""
        }
        return "{$pairs}"
    }
} 