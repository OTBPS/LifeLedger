package com.example.life_ledger.data.repository

import com.example.life_ledger.data.dao.*
import com.example.life_ledger.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.*

/**
 * LifeLedger应用统一数据仓库
 * 整合所有实体的数据访问功能
 */
class LifeLedgerRepository(
    private val transactionDao: TransactionDao,
    private val todoDao: TodoDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val userSettingsDao: UserSettingsDao
) {
    
    // ==================== Transaction相关操作 ====================
    
    // 基础CRUD
    suspend fun insertTransaction(transaction: Transaction) = transactionDao.insert(transaction)
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.update(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.delete(transaction)
    suspend fun getTransactionById(id: String) = transactionDao.getById(id)
    fun getAllTransactions() = transactionDao.getAllFlow()
    
    // 统计查询
    suspend fun getTotalIncome() = transactionDao.getTotalIncome()
    suspend fun getTotalExpense() = transactionDao.getTotalExpense()
    fun getIncomeTransactions() = transactionDao.getIncomeFlow()
    fun getExpenseTransactions() = transactionDao.getExpenseFlow()
    fun getTransactionsByCategory(categoryId: String) = transactionDao.getByCategoryFlow(categoryId)
    fun getTransactionsByDateRange(startDate: Long, endDate: Long) = transactionDao.getByDateRangeFlow(startDate, endDate)
    suspend fun getMonthlyStats() = transactionDao.getMonthlyStats()
    
    // 搜索
    fun searchTransactions(query: String) = transactionDao.searchTransactionsFlow(query)
    
    // ==================== TodoItem相关操作 ====================
    
    // 基础CRUD
    suspend fun insertTodo(todo: TodoItem) = todoDao.insert(todo)
    suspend fun updateTodo(todo: TodoItem) = todoDao.update(todo)
    suspend fun deleteTodo(todo: TodoItem) = todoDao.delete(todo)
    suspend fun getTodoById(id: String) = todoDao.getById(id)
    fun getAllTodos() = todoDao.getAllFlow()
    
    // 状态查询
    fun getPendingTodos() = todoDao.getPendingFlow()
    fun getCompletedTodos() = todoDao.getCompletedFlow()
    fun getHighPriorityTodos() = todoDao.getHighPriorityFlow()
    fun getTodosByCategory(categoryId: String) = todoDao.getByCategoryFlow(categoryId)
    
    // 时间相关
    fun getTodayDueTodos(): Flow<List<TodoItem>> {
        val calendar = java.util.Calendar.getInstance()
        val startOfDay = calendar.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
        return todoDao.getTodayDueFlow(startOfDay, endOfDay)
    }
    
    fun getOverdueTodos() = todoDao.getOverdueFlow(System.currentTimeMillis())
    
    // 统计
    suspend fun getTodoStats(): TodoStats {
        val total = todoDao.getTotalCount()
        val pending = todoDao.getPendingCount()
        val completed = todoDao.getCompletedCount()
        val overdue = todoDao.getOverdueCount(System.currentTimeMillis())
        
        return TodoStats(
            totalCount = total,
            pendingCount = pending,
            completedCount = completed,
            overdueCount = overdue,
            completionRate = if (total > 0) (completed.toDouble() / total * 100) else 0.0
        )
    }
    
    // 批量操作
    suspend fun markTodosAsCompleted(ids: List<String>) {
        val currentTime = System.currentTimeMillis()
        todoDao.markAsCompleted(ids, currentTime, currentTime)
    }
    
    // 搜索
    fun searchTodos(query: String) = todoDao.searchTodosFlow(query)
    
    // ==================== Category相关操作 ====================
    
    // 基础CRUD
    suspend fun insertCategory(category: Category) = categoryDao.insert(category)
    suspend fun updateCategory(category: Category) = categoryDao.update(category)
    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)
    suspend fun getCategoryById(id: String) = categoryDao.getById(id)
    fun getAllCategories() = categoryDao.getAllFlow()
    
    // 按类型查询
    fun getFinancialCategories() = categoryDao.getFinancialCategoriesFlow()
    fun getTodoCategories() = categoryDao.getTodoCategoriesFlow()
    fun getIncomeCategories() = categoryDao.getIncomeCategoriesFlow()
    fun getExpenseCategories() = categoryDao.getExpenseCategoriesFlow()
    
    // 默认分类
    suspend fun getDefaultFinancialCategory(subType: String) = categoryDao.getDefaultFinancialCategory(subType)
    suspend fun getDefaultTodoCategory() = categoryDao.getDefaultTodoCategory()
    
    // 使用统计
    suspend fun incrementCategoryUsage(categoryId: String) {
        val currentTime = System.currentTimeMillis()
        categoryDao.incrementUsage(categoryId, currentTime, currentTime)
    }
    
    // 搜索
    fun searchCategories(query: String) = categoryDao.searchCategoriesFlow(query)
    
    // ==================== Budget相关操作 ====================
    
    // 基础CRUD
    suspend fun insertBudget(budget: Budget) = budgetDao.insert(budget)
    suspend fun updateBudget(budget: Budget) = budgetDao.update(budget)
    suspend fun deleteBudget(budget: Budget) = budgetDao.delete(budget)
    suspend fun getBudgetById(id: String) = budgetDao.getById(id)
    fun getAllBudgets() = budgetDao.getAllFlow()
    suspend fun getCurrentBudgetsList(): List<Budget> = budgetDao.getAll()
    
    // 当前预算
    fun getCurrentBudgets() = budgetDao.getCurrentBudgetsFlow(System.currentTimeMillis())
    suspend fun getCurrentBudgetByCategory(categoryId: String) = 
        budgetDao.getCurrentBudgetByCategory(categoryId, System.currentTimeMillis())
    
    // 预算状态
    fun getOverspentBudgets() = budgetDao.getOverspentBudgetsFlow()
    fun getNearLimitBudgets() = budgetDao.getNearLimitBudgetsFlow()
    
    // 预算更新
    suspend fun updateBudgetSpent(budgetId: String, amount: Double) {
        budgetDao.updateSpent(budgetId, amount, System.currentTimeMillis())
    }
    
    suspend fun addBudgetSpent(budgetId: String, amount: Double) {
        budgetDao.addSpent(budgetId, amount, System.currentTimeMillis())
    }
    
    // 统计
    suspend fun getBudgetOverview() = budgetDao.getCurrentBudgetOverview(System.currentTimeMillis())
    
    // 按日期范围查询
    suspend fun getBudgetsByDateRange(startDate: Long, endDate: Long) = 
        budgetDao.getByDateRange(startDate, endDate)
    
    // 搜索
    fun searchBudgets(query: String) = budgetDao.searchBudgetsFlow(query)
    
    // ==================== UserSettings相关操作 ====================
    
    // 基础CRUD
    suspend fun insertUserSettings(settings: UserSettings) = userSettingsDao.insert(settings)
    suspend fun updateUserSettings(settings: UserSettings) = userSettingsDao.update(settings)
    suspend fun getUserSettings(userId: String) = userSettingsDao.getByUserId(userId)
    fun getUserSettingsFlow(userId: String) = userSettingsDao.getByUserIdFlow(userId)
    suspend fun getDefaultSettings() = userSettingsDao.getDefaultSettings()
    
    // 特定设置更新
    suspend fun updateTheme(userId: String, theme: UserSettings.AppTheme) {
        userSettingsDao.updateTheme(userId, theme, System.currentTimeMillis())
    }
    
    suspend fun updateCurrency(userId: String, currency: String, currencySymbol: String) {
        userSettingsDao.updateCurrency(userId, currency, currencySymbol, System.currentTimeMillis())
    }
    
    suspend fun updateNotificationEnabled(userId: String, enabled: Boolean) {
        userSettingsDao.updateNotificationEnabled(userId, enabled, System.currentTimeMillis())
    }
    
    suspend fun updateBiometric(userId: String, enabled: Boolean) {
        userSettingsDao.updateBiometric(userId, enabled, System.currentTimeMillis())
    }
    
    // ==================== 跨表查询和业务逻辑 ====================
    
    /**
     * 获取首页仪表板数据
     */
    suspend fun getDashboardData(): DashboardData {
        val todayStats = getTodayFinancialStats()
        val todoStats = getTodoStats()
        val budgetOverview = getBudgetOverview()
        val recentTransactions = transactionDao.getRecentTransactions(5)
        val todayTodos = todoDao.getTodayDueCount(getTodayStartEnd().first, getTodayStartEnd().second)
        val overdueTodos = todoDao.getOverdueCount(System.currentTimeMillis())
        
        return DashboardData(
            todayIncome = todayStats.income,
            todayExpense = todayStats.expense,
            todayBalance = todayStats.balance,
            totalBudget = budgetOverview.totalAmount,
            totalSpent = budgetOverview.totalSpent,
            budgetUsageRate = budgetOverview.totalUsageRate,
            pendingTodos = todoStats.pendingCount,
            todayDueTodos = todayTodos,
            overdueTodos = overdueTodos,
            recentTransactions = recentTransactions
        )
    }
    
    /**
     * 获取今日财务统计
     */
    private suspend fun getTodayFinancialStats(): DailyFinancialStats {
        val (startOfDay, endOfDay) = getTodayStartEnd()
        val income = transactionDao.getIncomeByDateRange(startOfDay, endOfDay)
        val expense = transactionDao.getExpenseByDateRange(startOfDay, endOfDay)
        
        return DailyFinancialStats(
            income = income,
            expense = expense,
            balance = income - expense
        )
    }
    
    /**
     * 获取今日开始和结束时间戳
     */
    private fun getTodayStartEnd(): Pair<Long, Long> {
        val calendar = java.util.Calendar.getInstance()
        val startOfDay = calendar.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
        return Pair(startOfDay, endOfDay)
    }
    
    /**
     * 添加交易记录并更新相关预算
     */
    suspend fun addTransactionWithBudgetUpdate(transaction: Transaction): Long {
        // 插入交易记录
        val transactionId = transactionDao.insert(transaction)
        
        // 如果是支出，更新相关预算
        if (transaction.type == Transaction.TransactionType.EXPENSE && transaction.categoryId != null) {
            val budget = budgetDao.getCurrentBudgetByCategory(
                transaction.categoryId, 
                System.currentTimeMillis()
            )
            budget?.let {
                budgetDao.addSpent(it.id, transaction.amount, System.currentTimeMillis())
            }
        }
        
        // 更新分类使用次数
        transaction.categoryId?.let { categoryId ->
            categoryDao.incrementUsage(categoryId, System.currentTimeMillis(), System.currentTimeMillis())
        }
        
        return transactionId
    }
    
    /**
     * 初始化默认数据
     */
    suspend fun initializeDefaultData() {
        // 检查是否已有数据
        if (categoryDao.getCount() == 0) {
            // 插入默认分类
            val financialCategories = Category.createDefaultFinancialCategories()
            categoryDao.insertAll(financialCategories)
            
            val todoCategories = Category.createDefaultTodoCategories()
            categoryDao.insertAll(todoCategories)
        }
        
        // 检查用户设置
        if (!userSettingsDao.hasAnySettings()) {
            val defaultSettings = UserSettings()
            userSettingsDao.insert(defaultSettings)
        }
        
        // 注释掉自动创建示例数据，让用户使用真实的交易记录
        // val transactionCount = transactionDao.getCount()
        // if (transactionCount == 0) {
        //     createSampleTransactionData()
        // }
    }
    
    /**
     * 创建示例交易数据（用于测试月度统计图表）
     */
    private suspend fun createSampleTransactionData() {
        try {
            val categories = categoryDao.getFinancialCategoriesFlow().firstOrNull()
            if (categories.isNullOrEmpty()) {
                android.util.Log.w("LifeLedgerRepository", "No categories available for sample data")
                return
            }

            val expenseCategories = categories.filter { it.subType == Category.FinancialSubType.EXPENSE }
            val incomeCategories = categories.filter { it.subType == Category.FinancialSubType.INCOME }

            val sampleTransactions = mutableListOf<Transaction>()

            // 生成最近12个月的示例数据
            for (monthOffset in 0..11) {
                val calendar = Calendar.getInstance()
                
                // 设置为指定月份的第一天
                calendar.add(Calendar.MONTH, -monthOffset)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                val monthStart = calendar.timeInMillis
                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                
                android.util.Log.d("LifeLedgerRepository", "生成 ${java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)} 的示例数据")

                // 每个月生成8-20笔交易
                val transactionCount = (8..20).random()
                repeat(transactionCount) {
                    val randomDay = (1..daysInMonth).random()
                    calendar.set(Calendar.DAY_OF_MONTH, randomDay)
                    calendar.set(Calendar.HOUR_OF_DAY, (8..22).random())
                    calendar.set(Calendar.MINUTE, (0..59).random())

                    val isIncome = kotlin.random.Random.nextDouble() < 0.25 // 25%概率为收入
                    
                    val transaction = if (isIncome && incomeCategories.isNotEmpty()) {
                        val category = incomeCategories.random()
                        Transaction(
                            amount = (2000..12000).random().toDouble(),
                            type = Transaction.TransactionType.INCOME,
                            categoryId = category.id,
                            title = getRandomIncomeTitle(),
                            description = "示例收入记录 - ${category.name}",
                            date = calendar.timeInMillis,
                            tags = "示例, 收入"
                        )
                    } else if (expenseCategories.isNotEmpty()) {
                        val category = expenseCategories.random()
                        Transaction(
                            amount = (20..800).random().toDouble(),
                            type = Transaction.TransactionType.EXPENSE,
                            categoryId = category.id,
                            title = getRandomExpenseTitle(category.name),
                            description = "示例支出记录 - ${category.name}",
                            date = calendar.timeInMillis,
                            tags = "示例, 支出"
                        )
                    } else {
                        // 如果没有合适的分类，创建一个通用支出
                        Transaction(
                            amount = (50..300).random().toDouble(),
                            type = Transaction.TransactionType.EXPENSE,
                            categoryId = null,
                            title = "其他支出",
                            description = "示例支出记录",
                            date = calendar.timeInMillis,
                            tags = "示例, 支出"
                        )
                    }
                    
                    sampleTransactions.add(transaction)
                }
            }

            // 批量插入示例数据
            transactionDao.insertAll(sampleTransactions)
            android.util.Log.d("LifeLedgerRepository", "Created ${sampleTransactions.size} sample transactions")

        } catch (e: Exception) {
            android.util.Log.e("LifeLedgerRepository", "Failed to create sample transaction data", e)
        }
    }

    /**
     * 获取随机收入标题
     */
    private fun getRandomIncomeTitle(): String {
        val titles = listOf("工资", "奖金", "兼职收入", "投资收益", "退款", "红包", "其他收入")
        return titles.random()
    }

    /**
     * 获取随机支出标题
     */
    private fun getRandomExpenseTitle(categoryName: String): String {
        return when (categoryName) {
            "餐饮" -> listOf("早餐", "午餐", "晚餐", "下午茶", "宵夜", "聚餐", "外卖").random()
            "交通" -> listOf("打车", "地铁", "公交", "加油", "停车费", "高速费", "机票").random()
            "购物" -> listOf("衣服", "鞋子", "化妆品", "电子产品", "书籍", "日用品", "礼物").random()
            "娱乐" -> listOf("电影", "KTV", "游戏", "演唱会", "旅游", "运动", "酒吧").random()
            "医疗" -> listOf("挂号费", "药费", "检查费", "治疗费", "体检", "保险", "康复").random()
            else -> categoryName
        }
    }
    
    /**
     * 清理过期数据
     */
    suspend fun cleanupExpiredData() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        val ninetyDaysAgo = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L)
        
        // 归档过期预算
        budgetDao.archiveExpiredBudgets(System.currentTimeMillis(), System.currentTimeMillis())
        
        // 删除旧的预算（保留90天）
        budgetDao.deleteOldBudgets(ninetyDaysAgo)
        
        // 删除已完成的旧待办事项（保留30天）
        // 注意：这里需要自定义SQL，暂时跳过
    }

    /**
     * 强制重新创建示例数据（用于调试）
     */
    suspend fun recreateSampleData() {
        try {
            // 删除所有现有交易数据
            transactionDao.deleteAll()
            android.util.Log.d("LifeLedgerRepository", "Deleted all existing transactions")
            
            // 重新创建示例数据
            createSampleTransactionData()
        } catch (e: Exception) {
            android.util.Log.e("LifeLedgerRepository", "Failed to recreate sample data", e)
        }
    }

    /**
     * 删除所有交易数据
     */
    suspend fun deleteAllTransactions() {
        try {
            transactionDao.deleteAll()
            android.util.Log.d("LifeLedgerRepository", "所有交易数据已删除")
        } catch (e: Exception) {
            android.util.Log.e("LifeLedgerRepository", "删除交易数据失败", e)
            throw e
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: LifeLedgerRepository? = null
        
        fun getInstance(database: com.example.life_ledger.data.database.AppDatabase): LifeLedgerRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = LifeLedgerRepository(
                    transactionDao = database.transactionDao(),
                    todoDao = database.todoDao(),
                    categoryDao = database.categoryDao(),
                    budgetDao = database.budgetDao(),
                    userSettingsDao = database.userSettingsDao()
                )
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==================== 数据类定义 ====================

/**
 * 待办事项统计数据
 */
data class TodoStats(
    val totalCount: Int,
    val pendingCount: Int,
    val completedCount: Int,
    val overdueCount: Int,
    val completionRate: Double
)

/**
 * 日财务统计数据
 */
data class DailyFinancialStats(
    val income: Double,
    val expense: Double,
    val balance: Double
)

/**
 * 仪表板数据
 */
data class DashboardData(
    val todayIncome: Double,
    val todayExpense: Double,
    val todayBalance: Double,
    val totalBudget: Double,
    val totalSpent: Double,
    val budgetUsageRate: Double,
    val pendingTodos: Int,
    val todayDueTodos: Int,
    val overdueTodos: Int,
    val recentTransactions: List<Transaction>
) 