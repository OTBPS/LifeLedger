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
                try {
                    android.util.Log.d("AppDatabase", "Creating database instance...")
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        AppConstants.Database.NAME
                    )
                        .addCallback(DatabaseCallback())
                        .fallbackToDestructiveMigration() // 开发阶段允许重新创建数据库
                        .build()
                    INSTANCE = instance
                    android.util.Log.d("AppDatabase", "Database instance created successfully")
                    instance
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Failed to create database instance", e)
                    // 如果创建失败，尝试删除数据库文件并重新创建
                    try {
                        context.deleteDatabase(AppConstants.Database.NAME)
                        android.util.Log.d("AppDatabase", "Deleted corrupted database, retrying...")
                        val instance = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            AppConstants.Database.NAME
                        )
                            .addCallback(DatabaseCallback())
                            .fallbackToDestructiveMigration()
                            .build()
                        INSTANCE = instance
                        android.util.Log.d("AppDatabase", "Database recreated successfully")
                        instance
                    } catch (retryException: Exception) {
                        android.util.Log.e("AppDatabase", "Failed to recreate database", retryException)
                        throw retryException
                    }
                }
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
            android.util.Log.d("AppDatabase", "Database created successfully")
            
            // 在后台线程初始化默认数据
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    populateDatabase(database)
                }
            }
        }
        
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            android.util.Log.d("AppDatabase", "Database opened successfully")
        }
        
        /**
         * 填充默认数据
         */
        private suspend fun populateDatabase(database: AppDatabase) {
            try {
                android.util.Log.d("AppDatabase", "Starting to populate database with default data")
                
                // 插入默认分类
                val categoryDao = database.categoryDao()
                
                // 强制清理所有现有数据以确保使用新的英文类别
                try {
                    categoryDao.deleteAll()
                    android.util.Log.d("AppDatabase", "Cleared all existing categories")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Failed to clear categories, continuing anyway", e)
                }
                
                // 插入默认财务分类（英文版）
                val financialCategories = Category.createDefaultFinancialCategories()
                categoryDao.insertAll(financialCategories)
                android.util.Log.d("AppDatabase", "Inserted ${financialCategories.size} English financial categories")
                
                // 插入默认待办分类（英文版）
                val todoCategories = Category.createDefaultTodoCategories()
                categoryDao.insertAll(todoCategories)
                android.util.Log.d("AppDatabase", "Inserted ${todoCategories.size} English todo categories")
                
                // 验证插入的数据
                val totalCategories = categoryDao.getAll()
                android.util.Log.d("AppDatabase", "Total categories after insertion: ${totalCategories.size}")
                totalCategories.forEach { category ->
                    android.util.Log.d("AppDatabase", "Category: ${category.name} (${category.type})")
                }
                
                // 创建默认用户设置
                val userSettingsDao = database.userSettingsDao()
                val existingSettings = userSettingsDao.getByUserId("default_user")
                if (existingSettings == null) {
                    val defaultSettings = UserSettings()
                    userSettingsDao.insert(defaultSettings)
                    android.util.Log.d("AppDatabase", "Inserted default user settings")
                } else {
                    android.util.Log.d("AppDatabase", "User settings already exist, skipping insertion")
                }
                
                android.util.Log.d("AppDatabase", "Database population completed successfully")
                
            } catch (e: Exception) {
                // 记录错误，但不阻塞应用启动
                android.util.Log.e("AppDatabase", "Error populating database", e)
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
} 