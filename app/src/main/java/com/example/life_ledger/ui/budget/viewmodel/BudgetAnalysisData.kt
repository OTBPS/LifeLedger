package com.example.life_ledger.ui.budget.viewmodel

import com.example.life_ledger.data.model.Budget
import java.util.*

/**
 * 预算分析概览数据类
 */
data class AnalysisOverview(
    val totalBudgets: Int,
    val activeBudgets: Int,
    val totalAmount: Double,
    val totalSpent: Double,
    val averageUsage: Double,
    val healthScore: Int // 0-100分
)

/**
 * 预算使用数据类
 */
data class BudgetUsageData(
    val budgetName: String,
    val amount: Double,
    val spent: Double,
    val percentage: Double,
    val status: Budget.BudgetStatus
)

/**
 * 预算趋势数据类
 */
data class BudgetTrendData(
    val date: Long,
    val totalSpent: Double,
    val budgetAmount: Double
)

/**
 * 预算对比数据类
 */
data class BudgetComparisonData(
    val categoryName: String,
    val currentSpent: Double,
    val previousSpent: Double,
    val change: Double
)

/**
 * 预算建议数据类
 */
data class BudgetRecommendation(
    val id: String = UUID.randomUUID().toString(),
    val type: RecommendationType,
    val title: String,
    val description: String,
    val priority: Priority,
    val actionText: String? = null
) {
    enum class RecommendationType {
        OVERSPENDING,
        BUDGET_ADJUSTMENT,
        SAVINGS_OPPORTUNITY,
        CATEGORY_OPTIMIZATION,
        SPENDING_PATTERN,
        GOAL_SETTING
    }
    
    enum class Priority {
        HIGH, MEDIUM, LOW
    }
}

/**
 * 预算概览数据类
 */
data class BudgetOverview(
    val totalBudgets: Int,
    val activeBudgets: Int,
    val totalAmount: Double,
    val totalSpent: Double,
    val remainingAmount: Double,
    val overBudgetCount: Int,
    val warningCount: Int
) 