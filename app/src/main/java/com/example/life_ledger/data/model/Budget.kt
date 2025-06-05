package com.example.life_ledger.data.model

import android.os.Parcelable
import androidx.room.*
import com.example.life_ledger.constants.AppConstants
import java.util.*

/**
 * 预算设置实体类
 * 用于存储预算限额和周期设置
 */
@Entity(
    tableName = AppConstants.Database.TABLE_BUDGETS,
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["period"]),
        Index(value = ["startDate"]),
        Index(value = ["endDate"]),
        Index(value = ["isActive"])
    ]
)
data class Budget(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "name")
    val name: String, // 预算名称
    
    @ColumnInfo(name = "categoryId")
    val categoryId: String?, // 关联分类ID，null表示总预算
    
    @ColumnInfo(name = "amount")
    val amount: Double, // 预算金额
    
    @ColumnInfo(name = "spent")
    val spent: Double = 0.0, // 已花费金额
    
    @ColumnInfo(name = "period")
    val period: BudgetPeriod, // 预算周期
    
    @ColumnInfo(name = "startDate")
    val startDate: Long, // 预算开始日期
    
    @ColumnInfo(name = "endDate")
    val endDate: Long, // 预算结束日期
    
    @ColumnInfo(name = "isActive")
    val isActive: Boolean = true, // 是否激活
    
    @ColumnInfo(name = "alertThreshold")
    val alertThreshold: Double = 0.8, // 警告阈值（百分比，0.8 = 80%）
    
    @ColumnInfo(name = "isAlertEnabled")
    val isAlertEnabled: Boolean = true, // 是否启用警告
    
    @ColumnInfo(name = "description")
    val description: String? = null, // 预算描述
    
    @ColumnInfo(name = "currency")
    val currency: String = "CNY", // 货币类型
    
    @ColumnInfo(name = "isRecurring")
    val isRecurring: Boolean = true, // Whether it's a recurring budget (auto-renewal)
    
    @ColumnInfo(name = "lastAlertDate")
    val lastAlertDate: Long? = null, // Last alert date
    
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Budget period enum
     */
    enum class BudgetPeriod(val displayName: String, val days: Int) {
        DAILY("Daily", 1),
        WEEKLY("Weekly", 7),
        MONTHLY("Monthly", 30),
        QUARTERLY("Quarterly", 90),
        YEARLY("Yearly", 365)
    }
    
    /**
     * Budget status enum
     */
    enum class BudgetStatus {
        SAFE,       // Safe
        WARNING,    // Warning
        EXCEEDED,   // Exceeded
        EXPIRED     // Expired
    }
    
    /**
     * Calculate remaining amount
     */
    fun getRemainingAmount(): Double {
        return amount - spent
    }
    
    /**
     * Calculate spent percentage
     */
    fun getSpentPercentage(): Double {
        return if (amount <= 0) 0.0 else (spent / amount * 100).coerceAtMost(100.0)
    }
    
    /**
     * Get budget status
     */
    fun getBudgetStatus(): BudgetStatus {
        return when {
            isExpired() -> BudgetStatus.EXPIRED
            spent > amount -> BudgetStatus.EXCEEDED
            getSpentPercentage() >= alertThreshold * 100 -> BudgetStatus.WARNING
            else -> BudgetStatus.SAFE
        }
    }
    
    /**
     * Check if expired
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > endDate
    }
    
    /**
     * Check if alert is needed
     */
    fun needsAlert(): Boolean {
        if (!isAlertEnabled || !isActive) return false
        
        val status = getBudgetStatus()
        if (status != BudgetStatus.WARNING && status != BudgetStatus.EXCEEDED) return false
        
        // Check if alert was already sent today
        lastAlertDate?.let { lastAlert ->
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            if (lastAlert >= today) return false
        }
        
        return true
    }
    
    /**
     * Check if expiring soon (within 3 days)
     */
    fun isExpiringSoon(): Boolean {
        val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() + threeDaysInMillis >= endDate
    }
    
    /**
     * Get remaining days
     */
    fun getRemainingDays(): Long {
        val remaining = endDate - System.currentTimeMillis()
        return if (remaining > 0) remaining / (24 * 60 * 60 * 1000) else 0
    }
    
    /**
     * Get status color (for UI display)
     */
    fun getStatusColor(): String {
        return when (getBudgetStatus()) {
            BudgetStatus.SAFE -> "#4CAF50"      // Green
            BudgetStatus.WARNING -> "#FF9800"   // Orange
            BudgetStatus.EXCEEDED -> "#F44336"  // Red
            BudgetStatus.EXPIRED -> "#9E9E9E"   // Gray
        }
    }
    
    /**
     * Get status text
     */
    fun getStatusText(): String {
        return when (getBudgetStatus()) {
            BudgetStatus.SAFE -> "Budget Sufficient"
            BudgetStatus.WARNING -> "Near Limit"
            BudgetStatus.EXCEEDED -> "Over Budget"
            BudgetStatus.EXPIRED -> "Expired"
        }
    }
    
    /**
     * Update spent amount
     */
    fun updateSpent(newSpent: Double): Budget {
        return this.copy(
            spent = newSpent.coerceAtLeast(0.0),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Add spent amount
     */
    fun addSpent(additionalSpent: Double): Budget {
        return updateSpent(spent + additionalSpent)
    }
    
    /**
     * Subtract spent amount
     */
    fun subtractSpent(amountToSubtract: Double): Budget {
        return updateSpent(spent - amountToSubtract)
    }
    
    /**
     * Reset budget for new period
     */
    fun resetForNewPeriod(): Budget {
        val newStartDate = endDate + 1
        val newEndDate = when (period) {
            BudgetPeriod.DAILY -> newStartDate + 24 * 60 * 60 * 1000 - 1
            BudgetPeriod.WEEKLY -> newStartDate + 7 * 24 * 60 * 60 * 1000 - 1
            BudgetPeriod.MONTHLY -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = newStartDate
                calendar.add(Calendar.MONTH, 1)
                calendar.timeInMillis - 1
            }
            BudgetPeriod.QUARTERLY -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = newStartDate
                calendar.add(Calendar.MONTH, 3)
                calendar.timeInMillis - 1
            }
            BudgetPeriod.YEARLY -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = newStartDate
                calendar.add(Calendar.YEAR, 1)
                calendar.timeInMillis - 1
            }
        }
        
        return this.copy(
            spent = 0.0,
            startDate = newStartDate,
            endDate = newEndDate,
            lastAlertDate = null,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Calculate daily allowance
     */
    fun getDailyAllowance(): Double {
        val remainingDays = getRemainingDays()
        return if (remainingDays > 0) getRemainingAmount() / remainingDays else 0.0
    }
} 