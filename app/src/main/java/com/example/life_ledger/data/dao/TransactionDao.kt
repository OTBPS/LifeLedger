package com.example.life_ledger.data.dao

import androidx.room.*
import com.example.life_ledger.data.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * 财务记录数据访问对象
 * 提供对Transaction表的所有数据库操作
 */
@Dao
interface TransactionDao {
    
    // ==================== 基础CRUD操作 ====================
    
    /**
     * 插入单个交易记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long
    
    /**
     * 插入多个交易记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>): List<Long>
    
    /**
     * 更新交易记录
     */
    @Update
    suspend fun update(transaction: Transaction): Int
    
    /**
     * 删除交易记录
     */
    @Delete
    suspend fun delete(transaction: Transaction): Int
    
    /**
     * 根据ID删除交易记录
     */
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String): Int
    
    /**
     * 删除所有交易记录
     */
    @Query("DELETE FROM transactions")
    suspend fun deleteAll(): Int
    
    // ==================== 查询操作 ====================
    
    /**
     * 根据ID获取交易记录
     */
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): Transaction?
    
    /**
     * 获取所有交易记录（Flow响应式）
     */
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllFlow(): Flow<List<Transaction>>
    
    /**
     * 获取所有交易记录
     */
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAll(): List<Transaction>
    
    /**
     * 分页获取交易记录
     */
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactions(limit: Int, offset: Int): List<Transaction>
    
    // ==================== 按类型查询 ====================
    
    /**
     * 获取收入记录
     */
    @Query("SELECT * FROM transactions WHERE type = 'INCOME' ORDER BY date DESC")
    fun getIncomeFlow(): Flow<List<Transaction>>
    
    /**
     * 获取支出记录
     */
    @Query("SELECT * FROM transactions WHERE type = 'EXPENSE' ORDER BY date DESC")
    fun getExpenseFlow(): Flow<List<Transaction>>
    
    /**
     * 根据类型获取交易记录
     */
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    suspend fun getByType(type: Transaction.TransactionType): List<Transaction>
    
    // ==================== 按分类查询 ====================
    
    /**
     * 根据分类获取交易记录
     */
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getByCategoryFlow(categoryId: String): Flow<List<Transaction>>
    
    /**
     * 根据分类获取交易记录
     */
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    suspend fun getByCategory(categoryId: String): List<Transaction>
    
    // ==================== 按时间查询 ====================
    
    /**
     * 获取指定日期范围的交易记录
     */
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<Transaction>>
    
    /**
     * 获取指定日期范围的交易记录
     */
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getByDateRange(startDate: Long, endDate: Long): List<Transaction>
    
    /**
     * 获取今天的交易记录
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE date >= :startOfDay AND date <= :endOfDay 
        ORDER BY date DESC
    """)
    fun getTodayTransactionsFlow(startOfDay: Long, endOfDay: Long): Flow<List<Transaction>>
    
    /**
     * 获取本月的交易记录
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE date >= :startOfMonth AND date <= :endOfMonth 
        ORDER BY date DESC
    """)
    fun getMonthTransactionsFlow(startOfMonth: Long, endOfMonth: Long): Flow<List<Transaction>>
    
    // ==================== 统计查询 ====================
    
    /**
     * 获取总收入
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME'")
    suspend fun getTotalIncome(): Double
    
    /**
     * 获取总支出
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE'")
    suspend fun getTotalExpense(): Double
    
    /**
     * 获取指定日期范围的收入总额
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE type = 'INCOME' AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getIncomeByDateRange(startDate: Long, endDate: Long): Double
    
    /**
     * 获取指定日期范围的支出总额
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getExpenseByDateRange(startDate: Long, endDate: Long): Double
    
    /**
     * 获取指定分类的支出总额
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE type = 'EXPENSE' AND categoryId = :categoryId
    """)
    suspend fun getExpenseByCategory(categoryId: String): Double
    
    /**
     * 获取指定分类和日期范围的支出总额
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE type = 'EXPENSE' AND categoryId = :categoryId AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getExpenseByCategoryAndDateRange(
        categoryId: String, 
        startDate: Long, 
        endDate: Long
    ): Double
    
    /**
     * 按分类统计支出 - 改为返回List
     */
    @Query("""
        SELECT categoryId
        FROM transactions 
        WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate
        GROUP BY categoryId
    """)
    suspend fun getCategoriesWithExpense(startDate: Long, endDate: Long): List<String>
    
    /**
     * 按月份统计
     */
    @Query("""
        SELECT strftime('%Y-%m', datetime(date/1000, 'unixepoch')) as month,
               SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as income,
               SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as expense
        FROM transactions
        GROUP BY month
        ORDER BY month DESC
    """)
    suspend fun getMonthlyStats(): List<MonthlyStats>
    
    /**
     * 获取交易记录总数
     */
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int
    
    /**
     * 获取指定类型的交易记录总数
     */
    @Query("SELECT COUNT(*) FROM transactions WHERE type = :type")
    suspend fun getCountByType(type: Transaction.TransactionType): Int
    
    // ==================== 搜索操作 ====================
    
    /**
     * 搜索交易记录（按标题或描述）
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
        ORDER BY date DESC
    """)
    fun searchTransactionsFlow(query: String): Flow<List<Transaction>>
    
    /**
     * 搜索交易记录
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
        ORDER BY date DESC
    """)
    suspend fun searchTransactions(query: String): List<Transaction>
    
    // ==================== 复杂查询 ====================
    
    /**
     * 获取最近的交易记录
     */
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentTransactions(limit: Int = 10): List<Transaction>
    
    /**
     * 获取最大金额的交易记录
     */
    @Query("SELECT * FROM transactions WHERE amount = (SELECT MAX(amount) FROM transactions)")
    suspend fun getMaxAmountTransaction(): Transaction?
    
    /**
     * 获取定期交易记录
     */
    @Query("SELECT * FROM transactions WHERE isRecurring = 1 ORDER BY date DESC")
    fun getRecurringTransactionsFlow(): Flow<List<Transaction>>
    
    /**
     * 根据支付方式查询
     */
    @Query("SELECT * FROM transactions WHERE paymentMethod = :paymentMethod ORDER BY date DESC")
    suspend fun getByPaymentMethod(paymentMethod: Transaction.PaymentMethod): List<Transaction>
}

/**
 * 月度统计数据类
 */
data class MonthlyStats(
    val month: String,
    val income: Double,
    val expense: Double
) {
    val balance: Double get() = income - expense
} 