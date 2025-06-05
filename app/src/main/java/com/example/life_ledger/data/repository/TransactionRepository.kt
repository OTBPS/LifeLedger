package com.example.life_ledger.data.repository

import com.example.life_ledger.data.dao.TransactionDao
import com.example.life_ledger.data.dao.BudgetDao
import com.example.life_ledger.data.dao.MonthlyStats
import com.example.life_ledger.data.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * 财务数据仓库
 * 封装Transaction相关的数据访问逻辑
 */
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) {
    
    // ==================== 基础CRUD操作 ====================
    
    /**
     * 添加交易记录
     */
    suspend fun insertTransaction(transaction: Transaction): Long {
        val result = transactionDao.insert(transaction)
        
        // 如果是支出记录，更新相关预算
        if (transaction.type == Transaction.TransactionType.EXPENSE) {
            updateRelatedBudgets(transaction)
        }
        
        return result
    }
    
    /**
     * 批量添加交易记录
     */
    suspend fun insertTransactions(transactions: List<Transaction>): List<Long> {
        return transactionDao.insertAll(transactions)
    }
    
    /**
     * 更新交易记录
     */
    suspend fun updateTransaction(transaction: Transaction): Int {
        val result = transactionDao.update(transaction)
        
        // 如果是支出记录，重新计算相关预算
        if (transaction.type == Transaction.TransactionType.EXPENSE) {
            recalculateRelatedBudgets(transaction.categoryId)
        }
        
        return result
    }
    
    /**
     * 删除交易记录
     */
    suspend fun deleteTransaction(transaction: Transaction): Int {
        val result = transactionDao.delete(transaction)
        
        // 如果是支出记录，重新计算相关预算
        if (transaction.type == Transaction.TransactionType.EXPENSE) {
            recalculateRelatedBudgets(transaction.categoryId)
        }
        
        return result
    }
    
    /**
     * 根据ID删除交易记录
     */
    suspend fun deleteTransactionById(id: String): Int {
        return transactionDao.deleteById(id)
    }
    
    /**
     * 删除所有交易记录
     */
    suspend fun deleteAllTransactions(): Int {
        return transactionDao.deleteAll()
    }
    
    // ==================== 查询操作 ====================
    
    /**
     * 根据ID获取交易记录
     */
    suspend fun getTransactionById(id: String): Transaction? {
        return transactionDao.getById(id)
    }
    
    /**
     * 获取所有交易记录（Flow）
     */
    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllFlow()
    }
    
    /**
     * 获取所有交易记录
     */
    suspend fun getAllTransactionsSnapshot(): List<Transaction> {
        return transactionDao.getAll()
    }
    
    /**
     * 分页获取交易记录
     */
    suspend fun getTransactionsPaged(limit: Int, offset: Int): List<Transaction> {
        return transactionDao.getTransactions(limit, offset)
    }
    
    // ==================== 按类型查询 ====================
    
    /**
     * 获取收入记录
     */
    fun getIncomeTransactions(): Flow<List<Transaction>> {
        return transactionDao.getIncomeFlow()
    }
    
    /**
     * 获取支出记录
     */
    fun getExpenseTransactions(): Flow<List<Transaction>> {
        return transactionDao.getExpenseFlow()
    }
    
    /**
     * 根据类型获取交易记录
     */
    suspend fun getTransactionsByType(type: Transaction.TransactionType): List<Transaction> {
        return transactionDao.getByType(type)
    }
    
    // ==================== 按分类查询 ====================
    
    /**
     * 根据分类获取交易记录
     */
    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> {
        return transactionDao.getByCategoryFlow(categoryId)
    }
    
    /**
     * 根据分类获取交易记录（快照）
     */
    suspend fun getTransactionsByCategorySnapshot(categoryId: String): List<Transaction> {
        return transactionDao.getByCategory(categoryId)
    }
    
    // ==================== 按时间查询 ====================
    
    /**
     * 获取指定日期范围的交易记录
     */
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getByDateRangeFlow(startDate, endDate)
    }
    
    /**
     * 获取指定日期范围的交易记录（快照）
     */
    suspend fun getTransactionsByDateRangeSnapshot(startDate: Long, endDate: Long): List<Transaction> {
        return transactionDao.getByDateRange(startDate, endDate)
    }
    
    /**
     * 获取今天的交易记录
     */
    fun getTodayTransactions(): Flow<List<Transaction>> {
        val calendar = java.util.Calendar.getInstance()
        val startOfDay = calendar.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
        
        return transactionDao.getTodayTransactionsFlow(startOfDay, endOfDay)
    }
    
    /**
     * 获取本月的交易记录
     */
    fun getMonthTransactions(): Flow<List<Transaction>> {
        val calendar = java.util.Calendar.getInstance()
        val startOfMonth = calendar.apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endOfMonth = calendar.apply {
            add(java.util.Calendar.MONTH, 1)
            add(java.util.Calendar.MILLISECOND, -1)
        }.timeInMillis
        
        return transactionDao.getMonthTransactionsFlow(startOfMonth, endOfMonth)
    }
    
    // ==================== 统计查询 ====================
    
    /**
     * 获取总收入
     */
    suspend fun getTotalIncome(): Double {
        return transactionDao.getTotalIncome()
    }
    
    /**
     * 获取总支出
     */
    suspend fun getTotalExpense(): Double {
        return transactionDao.getTotalExpense()
    }
    
    /**
     * 获取净资产（收入-支出）
     */
    suspend fun getNetWorth(): Double {
        return getTotalIncome() - getTotalExpense()
    }
    
    /**
     * 获取指定日期范围的收入
     */
    suspend fun getIncomeByDateRange(startDate: Long, endDate: Long): Double {
        return transactionDao.getIncomeByDateRange(startDate, endDate)
    }
    
    /**
     * 获取指定日期范围的支出
     */
    suspend fun getExpenseByDateRange(startDate: Long, endDate: Long): Double {
        return transactionDao.getExpenseByDateRange(startDate, endDate)
    }
    
    /**
     * 获取指定分类的支出总额
     */
    suspend fun getExpenseByCategory(categoryId: String): Double {
        return transactionDao.getExpenseByCategory(categoryId)
    }
    
    /**
     * 获取指定分类和日期范围的支出总额
     */
    suspend fun getExpenseByCategoryAndDateRange(
        categoryId: String, 
        startDate: Long, 
        endDate: Long
    ): Double {
        return transactionDao.getExpenseByCategoryAndDateRange(categoryId, startDate, endDate)
    }
    
    /**
     * 获取交易记录总数
     */
    suspend fun getTransactionCount(): Int {
        return transactionDao.getCount()
    }
    
    /**
     * 获取指定类型的交易记录总数
     */
    suspend fun getTransactionCountByType(type: Transaction.TransactionType): Int {
        return transactionDao.getCountByType(type)
    }
    
    // ==================== 搜索操作 ====================
    
    /**
     * 搜索交易记录
     */
    fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactionsFlow(query)
    }
    
    /**
     * 搜索交易记录（快照）
     */
    suspend fun searchTransactionsSnapshot(query: String): List<Transaction> {
        return transactionDao.searchTransactions(query)
    }
    
    // ==================== 复杂查询 ====================
    
    /**
     * 获取最近的交易记录
     */
    suspend fun getRecentTransactions(limit: Int = 10): List<Transaction> {
        return transactionDao.getRecentTransactions(limit)
    }
    
    /**
     * 获取最大金额的交易记录
     */
    suspend fun getMaxAmountTransaction(): Transaction? {
        return transactionDao.getMaxAmountTransaction()
    }
    
    /**
     * 获取定期交易记录
     */
    fun getRecurringTransactions(): Flow<List<Transaction>> {
        return transactionDao.getRecurringTransactionsFlow()
    }
    
    /**
     * 根据支付方式查询
     */
    suspend fun getTransactionsByPaymentMethod(paymentMethod: Transaction.PaymentMethod): List<Transaction> {
        return transactionDao.getByPaymentMethod(paymentMethod)
    }
    
    // ==================== 业务逻辑方法 ====================
    
    /**
     * 获取今日收支统计
     */
    suspend fun getTodayStats(): DailyStats {
        val calendar = java.util.Calendar.getInstance()
        val startOfDay = calendar.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
        
        val income = getIncomeByDateRange(startOfDay, endOfDay)
        val expense = getExpenseByDateRange(startOfDay, endOfDay)
        
        return DailyStats(
            date = startOfDay,
            income = income,
            expense = expense,
            balance = income - expense
        )
    }
    
    /**
     * 获取本月收支统计
     */
    suspend fun getMonthStats(): MonthlyStatsDetail {
        val calendar = java.util.Calendar.getInstance()
        val startOfMonth = calendar.apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endOfMonth = calendar.apply {
            add(java.util.Calendar.MONTH, 1)
            add(java.util.Calendar.MILLISECOND, -1)
        }.timeInMillis
        
        val income = getIncomeByDateRange(startOfMonth, endOfMonth)
        val expense = getExpenseByDateRange(startOfMonth, endOfMonth)
        val transactions = getTransactionsByDateRangeSnapshot(startOfMonth, endOfMonth)
        
        return MonthlyStatsDetail(
            startDate = startOfMonth,
            endDate = endOfMonth,
            income = income,
            expense = expense,
            balance = income - expense,
            transactionCount = transactions.size,
            avgDailyExpense = if (getDaysInMonth() > 0) expense / getDaysInMonth() else 0.0
        )
    }
    
    /**
     * 获取当月天数
     */
    private fun getDaysInMonth(): Int {
        val calendar = java.util.Calendar.getInstance()
        return calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    }
    
    /**
     * 检查是否有交易记录
     */
    suspend fun hasAnyTransactions(): Boolean {
        return getTransactionCount() > 0
    }
    
    /**
     * 获取交易记录摘要
     */
    suspend fun getTransactionSummary(): TransactionSummary {
        val totalIncome = getTotalIncome()
        val totalExpense = getTotalExpense()
        val totalCount = getTransactionCount()
        val incomeCount = getTransactionCountByType(Transaction.TransactionType.INCOME)
        val expenseCount = getTransactionCountByType(Transaction.TransactionType.EXPENSE)
        
        return TransactionSummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netWorth = totalIncome - totalExpense,
            totalCount = totalCount,
            incomeCount = incomeCount,
            expenseCount = expenseCount,
            avgTransactionAmount = if (totalCount > 0) (totalIncome + totalExpense) / totalCount else 0.0
        )
    }
    
    /**
     * 按分类获取支出分类列表
     */
    suspend fun getCategoriesWithExpense(startDate: Long, endDate: Long): List<String> {
        return transactionDao.getCategoriesWithExpense(startDate, endDate)
    }
    
    /**
     * 按月份统计
     */
    suspend fun getMonthlyStats(): List<MonthlyStats> {
        return transactionDao.getMonthlyStats()
    }
    
    /**
     * 更新相关预算的花费金额
     */
    private suspend fun updateRelatedBudgets(transaction: Transaction) {
        try {
            // 更新分类预算
            transaction.categoryId?.let { categoryId ->
                val categoryBudget = budgetDao.getCurrentBudgetByCategory(categoryId, System.currentTimeMillis())
                categoryBudget?.let { budget ->
                    val newSpent = budget.spent + transaction.amount
                    budgetDao.updateSpent(budget.id, newSpent, System.currentTimeMillis())
                }
            }
            
            // 更新总预算（无分类限制）
            val totalBudget = budgetDao.getCurrentTotalBudget(System.currentTimeMillis())
            totalBudget?.let { budget ->
                val newSpent = budget.spent + transaction.amount
                budgetDao.updateSpent(budget.id, newSpent, System.currentTimeMillis())
            }
        } catch (e: Exception) {
            // 记录错误但不影响主要流程
            android.util.Log.e("TransactionRepository", "更新预算失败", e)
        }
    }
    
    /**
     * 重新计算相关预算的花费金额
     */
    private suspend fun recalculateRelatedBudgets(categoryId: String?) {
        try {
            // 重新计算分类预算
            categoryId?.let { catId ->
                val categoryBudget = budgetDao.getCurrentBudgetByCategory(catId, System.currentTimeMillis())
                categoryBudget?.let { budget ->
                    val totalSpent = transactionDao.getExpenseByCategoryAndDateRange(
                        catId, budget.startDate, budget.endDate
                    )
                    budgetDao.updateSpent(budget.id, totalSpent, System.currentTimeMillis())
                }
            }
            
            // 重新计算总预算
            val totalBudget = budgetDao.getCurrentTotalBudget(System.currentTimeMillis())
            totalBudget?.let { budget ->
                val totalSpent = transactionDao.getExpenseByDateRange(budget.startDate, budget.endDate)
                budgetDao.updateSpent(budget.id, totalSpent, System.currentTimeMillis())
            }
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepository", "重新计算预算失败", e)
        }
    }
}

/**
 * 日统计数据类
 */
data class DailyStats(
    val date: Long,
    val income: Double,
    val expense: Double,
    val balance: Double
)

/**
 * 月统计详细数据类
 */
data class MonthlyStatsDetail(
    val startDate: Long,
    val endDate: Long,
    val income: Double,
    val expense: Double,
    val balance: Double,
    val transactionCount: Int,
    val avgDailyExpense: Double
)

/**
 * 交易记录摘要数据类
 */
data class TransactionSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val netWorth: Double,
    val totalCount: Int,
    val incomeCount: Int,
    val expenseCount: Int,
    val avgTransactionAmount: Double
) 