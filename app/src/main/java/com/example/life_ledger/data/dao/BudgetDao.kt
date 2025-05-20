package com.example.life_ledger.data.dao

import androidx.room.*
import com.example.life_ledger.data.model.Budget
import kotlinx.coroutines.flow.Flow

/**
 * 预算数据访问对象
 * 提供对Budget表的所有数据库操作
 */
@Dao
interface BudgetDao {
    
    // ==================== 基础CRUD操作 ====================
    
    /**
     * 插入单个预算
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget): Long
    
    /**
     * 插入多个预算
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<Budget>): List<Long>
    
    /**
     * 更新预算
     */
    @Update
    suspend fun update(budget: Budget): Int
    
    /**
     * 删除预算
     */
    @Delete
    suspend fun delete(budget: Budget): Int
    
    /**
     * 根据ID删除预算
     */
    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: String): Int
    
    /**
     * 删除所有预算
     */
    @Query("DELETE FROM budgets")
    suspend fun deleteAll(): Int
    
    // ==================== 查询操作 ====================
    
    /**
     * 根据ID获取预算
     */
    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: String): Budget?
    
    /**
     * 获取所有预算（Flow响应式）
     */
    @Query("SELECT * FROM budgets ORDER BY startDate DESC")
    fun getAllFlow(): Flow<List<Budget>>
    
    /**
     * 获取所有预算
     */
    @Query("SELECT * FROM budgets ORDER BY startDate DESC")
    suspend fun getAll(): List<Budget>
    
    /**
     * 获取激活的预算
     */
    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY startDate DESC")
    fun getActiveFlow(): Flow<List<Budget>>
    
    /**
     * 获取激活的预算
     */
    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY startDate DESC")
    suspend fun getActive(): List<Budget>
    
    // ==================== 按状态查询 ====================
    
    /**
     * 获取当前有效的预算
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE isActive = 1 AND startDate <= :currentTime AND endDate >= :currentTime
        ORDER BY startDate DESC
    """)
    fun getCurrentBudgetsFlow(currentTime: Long): Flow<List<Budget>>
    
    /**
     * 获取当前有效的预算
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE isActive = 1 AND startDate <= :currentTime AND endDate >= :currentTime
        ORDER BY startDate DESC
    """)
    suspend fun getCurrentBudgets(currentTime: Long): List<Budget>
    
    /**
     * 获取过期的预算
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE endDate < :currentTime
        ORDER BY endDate DESC
    """)
    fun getExpiredBudgetsFlow(currentTime: Long): Flow<List<Budget>>
    
    /**
     * 获取即将到期的预算（3天内）
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE isActive = 1 AND endDate BETWEEN :currentTime AND :threeDaysLater
        ORDER BY endDate ASC
    """)
    suspend fun getExpiringSoon(currentTime: Long, threeDaysLater: Long): List<Budget>
    
    // ==================== 按分类查询 ====================
    
    /**
     * 根据分类获取预算
     */
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId ORDER BY startDate DESC")
    fun getByCategoryFlow(categoryId: String): Flow<List<Budget>>
    
    /**
     * 根据分类获取当前预算
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE categoryId = :categoryId AND isActive = 1 
        AND startDate <= :currentTime AND endDate >= :currentTime
        ORDER BY startDate DESC
        LIMIT 1
    """)
    suspend fun getCurrentBudgetByCategory(categoryId: String, currentTime: Long): Budget?
    
    /**
     * 获取总预算（无分类限制）
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE categoryId IS NULL AND isActive = 1
        AND startDate <= :currentTime AND endDate >= :currentTime
        ORDER BY startDate DESC
        LIMIT 1
    """)
    suspend fun getCurrentTotalBudget(currentTime: Long): Budget?
    
    // ==================== 按周期查询 ====================
    
    /**
     * 根据周期获取预算
     */
    @Query("SELECT * FROM budgets WHERE period = :period ORDER BY startDate DESC")
    fun getByPeriodFlow(period: Budget.BudgetPeriod): Flow<List<Budget>>
    
    /**
     * 获取月度预算
     */
    @Query("SELECT * FROM budgets WHERE period = 'MONTHLY' AND isActive = 1 ORDER BY startDate DESC")
    fun getMonthlyBudgetsFlow(): Flow<List<Budget>>
    
    /**
     * 获取年度预算
     */
    @Query("SELECT * FROM budgets WHERE period = 'YEARLY' AND isActive = 1 ORDER BY startDate DESC")
    fun getYearlyBudgetsFlow(): Flow<List<Budget>>
    
    // ==================== 按日期范围查询 ====================
    
    /**
     * 获取指定日期范围的预算
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE (startDate <= :endDate AND endDate >= :startDate)
        ORDER BY startDate DESC
    """)
    suspend fun getByDateRange(startDate: Long, endDate: Long): List<Budget>
    
    /**
     * 获取指定月份的预算
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE (startDate <= :monthEnd AND endDate >= :monthStart)
        ORDER BY startDate DESC
    """)
    fun getMonthBudgetsFlow(monthStart: Long, monthEnd: Long): Flow<List<Budget>>
    
    // ==================== 预算状态查询 ====================
    
    /**
     * 获取超支的预算
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE spent > amount AND isActive = 1
        ORDER BY (spent - amount) DESC
    """)
    fun getOverspentBudgetsFlow(): Flow<List<Budget>>
    
    /**
     * 获取接近限额的预算
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE (spent / amount) >= alertThreshold AND spent <= amount AND isActive = 1
        ORDER BY (spent / amount) DESC
    """)
    fun getNearLimitBudgetsFlow(): Flow<List<Budget>>
    
    /**
     * 获取需要警告的预算
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE isAlertEnabled = 1 AND isActive = 1
        AND ((spent / amount) >= alertThreshold OR spent > amount)
        AND (lastAlertDate IS NULL OR lastAlertDate < :todayStart)
        ORDER BY (spent / amount) DESC
    """)
    suspend fun getBudgetsNeedingAlert(todayStart: Long): List<Budget>
    
    // ==================== 统计查询 ====================
    
    /**
     * 获取预算总数
     */
    @Query("SELECT COUNT(*) FROM budgets")
    suspend fun getCount(): Int
    
    /**
     * 获取激活预算数量
     */
    @Query("SELECT COUNT(*) FROM budgets WHERE isActive = 1")
    suspend fun getActiveCount(): Int
    
    /**
     * 获取当前有效预算数量
     */
    @Query("""
        SELECT COUNT(*) FROM budgets 
        WHERE isActive = 1 AND startDate <= :currentTime AND endDate >= :currentTime
    """)
    suspend fun getCurrentCount(currentTime: Long): Int
    
    /**
     * 按周期统计预算数量 - 改为简单计数
     */
    @Query("SELECT COUNT(*) FROM budgets WHERE isActive = 1 AND period = :period")
    suspend fun getCountByPeriod(period: Budget.BudgetPeriod): Int
    
    /**
     * 获取预算总金额
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM budgets WHERE isActive = 1")
    suspend fun getTotalBudgetAmount(): Double
    
    /**
     * 获取已花费总金额
     */
    @Query("SELECT COALESCE(SUM(spent), 0) FROM budgets WHERE isActive = 1")
    suspend fun getTotalSpentAmount(): Double
    
    /**
     * 获取当前预算总览
     */
    @Query("""
        SELECT 
            COUNT(*) as totalCount,
            SUM(amount) as totalAmount,
            SUM(spent) as totalSpent,
            AVG(spent / amount * 100) as avgUsageRate,
            SUM(CASE WHEN spent > amount THEN 1 ELSE 0 END) as overspentCount
        FROM budgets 
        WHERE isActive = 1 AND startDate <= :currentTime AND endDate >= :currentTime
    """)
    suspend fun getCurrentBudgetOverview(currentTime: Long): BudgetOverview
    
    // ==================== 搜索操作 ====================
    
    /**
     * 搜索预算
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE name LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
        ORDER BY startDate DESC
    """)
    fun searchBudgetsFlow(query: String): Flow<List<Budget>>
    
    /**
     * 搜索预算
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE name LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
        ORDER BY startDate DESC
    """)
    suspend fun searchBudgets(query: String): List<Budget>
    
    // ==================== 业务逻辑操作 ====================
    
    /**
     * 更新预算花费
     */
    @Query("UPDATE budgets SET spent = :spent, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSpent(id: String, spent: Double, updatedAt: Long): Int
    
    /**
     * 增加预算花费
     */
    @Query("UPDATE budgets SET spent = spent + :amount, updatedAt = :updatedAt WHERE id = :id")
    suspend fun addSpent(id: String, amount: Double, updatedAt: Long): Int
    
    /**
     * 减少预算花费
     */
    @Query("UPDATE budgets SET spent = CASE WHEN spent - :amount < 0 THEN 0 ELSE spent - :amount END, updatedAt = :updatedAt WHERE id = :id")
    suspend fun subtractSpent(id: String, amount: Double, updatedAt: Long): Int
    
    /**
     * 重置预算花费
     */
    @Query("UPDATE budgets SET spent = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun resetSpent(id: String, updatedAt: Long): Int
    
    /**
     * 更新警告日期
     */
    @Query("UPDATE budgets SET lastAlertDate = :alertDate, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateAlertDate(id: String, alertDate: Long, updatedAt: Long): Int
    
    /**
     * 批量启用/禁用预算
     */
    @Query("UPDATE budgets SET isActive = :isActive, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun updateActiveStatus(ids: List<String>, isActive: Boolean, updatedAt: Long): Int
    
    /**
     * 批量更新预算周期
     */
    @Query("UPDATE budgets SET period = :period, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun updatePeriod(ids: List<String>, period: Budget.BudgetPeriod, updatedAt: Long): Int
    
    // ==================== 定期任务相关 ====================
    
    /**
     * 获取需要自动续期的预算
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE isRecurring = 1 AND endDate < :currentTime
        ORDER BY endDate ASC
    """)
    suspend fun getBudgetsForRenewal(currentTime: Long): List<Budget>
    
    /**
     * 获取过期的非重复预算
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE isRecurring = 0 AND endDate < :currentTime AND isActive = 1
        ORDER BY endDate ASC
    """)
    suspend fun getExpiredNonRecurringBudgets(currentTime: Long): List<Budget>
    
    // ==================== 数据清理操作 ====================
    
    /**
     * 删除过期的预算（保留最近90天）
     */
    @Query("DELETE FROM budgets WHERE endDate < :cutoffDate AND isRecurring = 0")
    suspend fun deleteOldBudgets(cutoffDate: Long): Int
    
    /**
     * 归档过期预算（设为非激活状态）
     */
    @Query("UPDATE budgets SET isActive = 0, updatedAt = :updatedAt WHERE endDate < :currentTime")
    suspend fun archiveExpiredBudgets(currentTime: Long, updatedAt: Long): Int
}

/**
 * 预算总览数据类
 */
data class BudgetOverview(
    val totalCount: Int,
    val totalAmount: Double,
    val totalSpent: Double,
    val avgUsageRate: Double,
    val overspentCount: Int
) {
    val remainingAmount: Double get() = totalAmount - totalSpent
    val totalUsageRate: Double get() = if (totalAmount > 0) (totalSpent / totalAmount * 100) else 0.0
} 