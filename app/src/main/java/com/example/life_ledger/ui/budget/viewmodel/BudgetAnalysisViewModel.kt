package com.example.life_ledger.ui.budget.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.life_ledger.data.database.AppDatabase
import com.example.life_ledger.data.model.Budget
import com.example.life_ledger.data.repository.LifeLedgerRepository
import kotlinx.coroutines.launch

/**
 * 预算分析ViewModel
 */
class BudgetAnalysisViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = LifeLedgerRepository.getInstance(AppDatabase.getDatabase(application))
    
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
    
    init {
        loadAnalysisData()
    }
    
    private fun loadAnalysisData() {
        viewModelScope.launch {
            try {
                loadOverviewData()
                loadUsageData()
                loadTrendData()
                loadComparisonData()
                generateRecommendations()
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
    
    private suspend fun generateRecommendations() {
        val budgets = repository.getCurrentBudgetsList().filter { it.isActive }
        val recommendations = mutableListOf<BudgetRecommendation>()
        
        // 检查超支预算
        budgets.filter { it.getBudgetStatus() == Budget.BudgetStatus.EXCEEDED }.forEach { budget ->
            recommendations.add(
                BudgetRecommendation(
                    type = BudgetRecommendation.RecommendationType.OVERSPENDING,
                    title = "预算超支提醒",
                    description = "${budget.name}已超支${String.format("%.2f", budget.spent - budget.amount)}元",
                    priority = BudgetRecommendation.Priority.HIGH,
                    actionText = "调整预算"
                )
            )
        }
        
        _recommendations.value = recommendations
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
} 