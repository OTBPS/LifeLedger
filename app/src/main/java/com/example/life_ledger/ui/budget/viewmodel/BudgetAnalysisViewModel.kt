package com.example.life_ledger.ui.budget.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.life_ledger.data.database.AppDatabase
import com.example.life_ledger.data.model.Budget
import com.example.life_ledger.data.repository.LifeLedgerRepository
import com.example.life_ledger.data.service.AIAnalysisService
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/**
 * 预算分析ViewModel
 */
class BudgetAnalysisViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = LifeLedgerRepository.getInstance(AppDatabase.getDatabase(application))
    private val aiAnalysisService = AIAnalysisService()
    
    private val _analysisOverview = MutableLiveData<AnalysisOverview>()
    val analysisOverview: LiveData<AnalysisOverview> = _analysisOverview
    
    private val _budgetUsageData = MutableLiveData<List<BudgetUsageData>>()
    val budgetUsageData: LiveData<List<BudgetUsageData>> = _budgetUsageData
    
    private val _budgetTrendData = MutableLiveData<List<BudgetTrendData>>()
    val budgetTrendData: LiveData<List<BudgetTrendData>> = _budgetTrendData
    
    private val _budgetComparisonData = MutableLiveData<List<BudgetComparisonData>>()
    val budgetComparisonData: LiveData<List<BudgetComparisonData>> = _budgetComparisonData
    
    private val _recommendations = MutableLiveData<List<BudgetRecommendation>>()
    val recommendations: LiveData<List<BudgetRecommendation>> = _recommendations
    
    private val _isLoadingRecommendations = MutableLiveData<Boolean>()
    val isLoadingRecommendations: LiveData<Boolean> = _isLoadingRecommendations
    
    private val _recommendationError = MutableLiveData<String?>()
    val recommendationError: LiveData<String?> = _recommendationError
    
    init {
        loadAnalysisData()
    }
    
    /**
     * 重新加载分析数据（公开方法）
     */
    fun loadAnalysisData() {
        viewModelScope.launch {
            try {
                loadOverviewData()
                loadUsageData()
                loadTrendData()
                loadComparisonData()
                generateLocalRecommendations() // 重新生成本地建议
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun loadOverviewData() {
        val budgets = repository.getCurrentBudgetsList()
        val activeBudgets = budgets.filter { it.isActive }
        val totalAmount = activeBudgets.sumOf { it.amount }
        val totalSpent = activeBudgets.sumOf { it.spent }
        val averageUsage = if (totalAmount > 0) (totalSpent / totalAmount) * 100 else 0.0
        val healthScore = calculateHealthScore(activeBudgets)
        
        _analysisOverview.value = AnalysisOverview(
            totalBudgets = budgets.size,
            activeBudgets = activeBudgets.size,
            totalAmount = totalAmount,
            totalSpent = totalSpent,
            averageUsage = averageUsage,
            healthScore = healthScore
        )
    }
    
    private suspend fun loadUsageData() {
        val budgets = repository.getCurrentBudgetsList().filter { it.isActive }
        val usageData = budgets.map { budget ->
            BudgetUsageData(
                budgetName = budget.name,
                amount = budget.amount,
                spent = budget.spent,
                percentage = budget.getSpentPercentage(),
                status = budget.getBudgetStatus()
            )
        }
        _budgetUsageData.value = usageData
    }
    
    private suspend fun loadTrendData() {
        // 简化实现，实际应该从历史数据中获取
        val budgets = repository.getCurrentBudgetsList()
        val trendData = listOf(
            BudgetTrendData(
                date = System.currentTimeMillis(),
                totalSpent = budgets.sumOf { it.spent },
                budgetAmount = budgets.sumOf { it.amount }
            )
        )
        _budgetTrendData.value = trendData
    }
    
    private suspend fun loadComparisonData() {
        // 简化实现
        val comparisonData = listOf<BudgetComparisonData>()
        _budgetComparisonData.value = comparisonData
    }
    
    /**
     * 生成智能建议（调用AI服务）
     */
    fun generateSmartRecommendations() {
        viewModelScope.launch {
            try {
                _isLoadingRecommendations.value = true
                _recommendationError.value = null
                
                // 获取数据
                val budgets = repository.getCurrentBudgetsList()
                val transactions = repository.getAllTransactions().first()
                val categories = repository.getAllCategories().first()
                
                // 调用AI服务
                val result = aiAnalysisService.getBudgetRecommendations(budgets, transactions, categories)
                
                result.fold(
                    onSuccess = { aiRecommendations ->
                        // Merge local recommendations and AI recommendations
                        val localRecommendations = generateLocalRecommendationsSync(budgets)
                        val allRecommendations = (localRecommendations + aiRecommendations).take(8) // Show max 8 recommendations
                        _recommendations.value = allRecommendations
                    },
                    onFailure = { error ->
                        _recommendationError.value = "Smart recommendation generation failed: ${error.message}"
                        // Keep showing local recommendations
                        generateLocalRecommendations()
                    }
                )
            } catch (e: Exception) {
                _recommendationError.value = "Network connection failed, showing local recommendations"
                generateLocalRecommendations()
            } finally {
                _isLoadingRecommendations.value = false
            }
        }
    }
    
    private suspend fun generateLocalRecommendations() {
        val budgets = repository.getCurrentBudgetsList().filter { it.isActive }
        val localRecommendations = generateLocalRecommendationsSync(budgets)
        _recommendations.value = localRecommendations
    }
    
    private fun generateLocalRecommendationsSync(budgets: List<Budget>): List<BudgetRecommendation> {
        val recommendations = mutableListOf<BudgetRecommendation>()
        
        // Check overspending budgets
        budgets.filter { it.getBudgetStatus() == Budget.BudgetStatus.EXCEEDED }.forEach { budget ->
            recommendations.add(
                BudgetRecommendation(
                    type = BudgetRecommendation.RecommendationType.OVERSPENDING,
                    title = "Budget Overspent Alert",
                    description = "${budget.name} has overspent by ¥${String.format("%.2f", budget.spent - budget.amount)}. Consider reviewing recent expenses and adjusting spending plan.",
                    priority = BudgetRecommendation.Priority.HIGH,
                    actionText = "View Details"
                )
            )
        }
        
        // Check budgets about to exceed
        budgets.filter { 
            it.getBudgetStatus() == Budget.BudgetStatus.WARNING && 
            it.getSpentPercentage() > 85 
        }.forEach { budget ->
            recommendations.add(
                BudgetRecommendation(
                    type = BudgetRecommendation.RecommendationType.BUDGET_ADJUSTMENT,
                    title = "Budget Nearly Exhausted",
                    description = "${budget.name} has used ${String.format("%.1f", budget.getSpentPercentage())}%. Please monitor spending carefully.",
                    priority = BudgetRecommendation.Priority.MEDIUM,
                    actionText = "Set Reminder"
                )
            )
        }
        
        // Check long-term unused budgets
        budgets.filter { it.spent < it.amount * 0.1 }.forEach { budget ->
            recommendations.add(
                BudgetRecommendation(
                    type = BudgetRecommendation.RecommendationType.SAVINGS_OPPORTUNITY,
                    title = "Low Budget Utilization",
                    description = "${budget.name} has low usage rate. Consider adjusting budget allocation or adding budget for other categories.",
                    priority = BudgetRecommendation.Priority.LOW,
                    actionText = "Adjust Budget"
                )
            )
        }
        
        return recommendations
    }
    
    private fun calculateHealthScore(budgets: List<Budget>): Int {
        if (budgets.isEmpty()) return 100
        
        val scores = budgets.map { budget ->
            when (budget.getBudgetStatus()) {
                Budget.BudgetStatus.SAFE -> 100
                Budget.BudgetStatus.WARNING -> 70
                Budget.BudgetStatus.EXCEEDED -> 30
                Budget.BudgetStatus.EXPIRED -> 50
            }
        }
        
        return scores.average().toInt()
    }
    
    /**
     * Record recommendation interaction
     */
    fun recordRecommendationInteraction(recommendationId: String, action: String) {
        viewModelScope.launch {
            try {
                // This can be recorded to database or analytics service
                println("Record recommendation interaction: ID=$recommendationId, Action=$action")
                // TODO: Implement specific recording logic
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

/**
 * 分析概览数据
 */
data class AnalysisOverview(
    val totalBudgets: Int,
    val activeBudgets: Int,
    val totalAmount: Double,
    val totalSpent: Double,
    val averageUsage: Double,
    val healthScore: Int
)

/**
 * 预算使用数据
 */
data class BudgetUsageData(
    val budgetName: String,
    val amount: Double,
    val spent: Double,
    val percentage: Double,
    val status: Budget.BudgetStatus
)

/**
 * 预算趋势数据
 */
data class BudgetTrendData(
    val date: Long,
    val totalSpent: Double,
    val budgetAmount: Double
)

/**
 * 预算对比数据
 */
data class BudgetComparisonData(
    val categoryName: String,
    val currentAmount: Double,
    val previousAmount: Double,
    val changePercentage: Double
)

/**
 * 预算建议
 */
data class BudgetRecommendation(
    val id: String = "rec_${System.currentTimeMillis()}",
    val type: RecommendationType,
    val title: String,
    val description: String,
    val priority: Priority,
    val actionText: String? = null,
    val expectedImpact: String? = null,
    val data: Map<String, String>? = null
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
        LOW,
        MEDIUM,
        HIGH
    }
} 