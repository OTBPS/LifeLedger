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
    val isRecurring: Boolean = true, // 是否为定期预算（自动续期）
    
    @ColumnInfo(name = "lastAlertDate")
    val lastAlertDate: Long? = null, // 最后一次警告日期
    
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 预算周期枚举
     */
    enum class BudgetPeriod(val displayName: String, val days: Int) {
        DAILY("每日", 1),
        WEEKLY("每周", 7),
        MONTHLY("每月", 30),
        QUARTERLY("每季度", 90),
        YEARLY("每年", 365)
    }
    
    /**
     * 预算状态枚举
     */
    enum class BudgetStatus {
        SAFE,       // 安全
        WARNING,    // 警告
        EXCEEDED,   // 超支
        EXPIRED     // 已过期
    }
    
    /**
     * 计算剩余金额
     */
    fun getRemainingAmount(): Double {
        return amount - spent
    }
    
    /**
     * 计算花费百分比
     */
    fun getSpentPercentage(): Double {
        return if (amount <= 0) 0.0 else (spent / amount * 100).coerceAtMost(100.0)
    }
    
    /**
     * 获取预算状态
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
     * 检查是否过期
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > endDate
    }
    
    /**
     * 检查是否需要警告
     */
    fun needsAlert(): Boolean {
        if (!isAlertEnabled || !isActive) return false
        
        val status = getBudgetStatus()
        if (status != BudgetStatus.WARNING && status != BudgetStatus.EXCEEDED) return false
        
        // 检查是否已经在今天发送过警告
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
     * 检查是否即将到期（3天内）
     */
    fun isExpiringSoon(): Boolean {
        val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() + threeDaysInMillis >= endDate
    }
    
    /**
     * 获取剩余天数
     */
    fun getRemainingDays(): Long {
        val remaining = endDate - System.currentTimeMillis()
        return if (remaining > 0) remaining / (24 * 60 * 60 * 1000) else 0
    }
    
    /**
     * 获取状态颜色（用于UI显示）
     */
    fun getStatusColor(): String {
        return when (getBudgetStatus()) {
            BudgetStatus.SAFE -> "#4CAF50"      // 绿色
            BudgetStatus.WARNING -> "#FF9800"   // 橙色
            BudgetStatus.EXCEEDED -> "#F44336"  // 红色
            BudgetStatus.EXPIRED -> "#9E9E9E"   // 灰色
        }
    }
    
    /**
     * 获取状态文本
     */
    fun getStatusText(): String {
        return when (getBudgetStatus()) {
            BudgetStatus.SAFE -> "预算充足"
            BudgetStatus.WARNING -> "接近限额"
            BudgetStatus.EXCEEDED -> "预算超支"
            BudgetStatus.EXPIRED -> "已过期"
        }
    }
    
    /**
     * 更新花费金额
     */
    fun updateSpent(newSpent: Double): Budget {
        return this.copy(
            spent = newSpent.coerceAtLeast(0.0),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 增加花费
     */
    fun addSpent(additionalSpent: Double): Budget {
        return updateSpent(spent + additionalSpent)
    }
    
    /**
     * 减少花费
     */
    fun subtractSpent(amountToSubtract: Double): Budget {
        return updateSpent(spent - amountToSubtract)
    }
    
    /**
     * 重置预算（新周期）
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
     * 计算每日平均可花费金额
     */
    fun getDailyAllowance(): Double {
        val remainingDays = getRemainingDays()
        return if (remainingDays > 0) getRemainingAmount() / remainingDays else 0.0
    }
} 