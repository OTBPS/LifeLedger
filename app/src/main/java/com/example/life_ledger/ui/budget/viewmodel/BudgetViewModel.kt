package com.example.life_ledger.ui.budget.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.life_ledger.data.database.AppDatabase
import com.example.life_ledger.data.repository.LifeLedgerRepository
import com.example.life_ledger.data.model.Budget
import com.example.life_ledger.data.model.Category
import com.example.life_ledger.data.dao.BudgetOverview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * 预算管理ViewModel
 * 处理预算相关的业务逻辑和状态管理
 */
class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = LifeLedgerRepository.getInstance(AppDatabase.getDatabase(application))
    
    // 预算列表
    private val _budgets = MutableLiveData<List<Budget>>()
    val budgets: LiveData<List<Budget>> = _budgets
    
    // 预算总览
    private val _budgetOverview = MutableLiveData<com.example.life_ledger.data.dao.BudgetOverview>()
    val budgetOverview: LiveData<com.example.life_ledger.data.dao.BudgetOverview> = _budgetOverview
    
    // 当前预算（按类别分组）
    private val _currentBudgets = MutableStateFlow<List<Budget>>(emptyList())
    val currentBudgets: StateFlow<List<Budget>> = _currentBudgets.asStateFlow()
    
    // 超支预算
    private val _overspentBudgets = MutableStateFlow<List<Budget>>(emptyList())
    val overspentBudgets: StateFlow<List<Budget>> = _overspentBudgets.asStateFlow()
    
    // 需要警告的预算
    private val _warningBudgets = MutableStateFlow<List<Budget>>(emptyList())
    val warningBudgets: StateFlow<List<Budget>> = _warningBudgets.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // 错误消息
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // 成功消息
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    // 分类列表（用于创建预算时选择）
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    
    // 预算筛选状态
    private val _filterPeriod = MutableStateFlow<Budget.BudgetPeriod?>(null)
    private val _filterStatus = MutableStateFlow<BudgetFilterStatus?>(null)
    private val _searchQuery = MutableStateFlow("")
    
    enum class BudgetFilterStatus {
        ACTIVE, EXPIRED, OVERSPENT, WARNING, ALL
    }
    
    init {
        loadBudgets()
        loadCategories()
        observeBudgetChanges()
    }
    
    /**
     * 观察预算变化
     */
    private fun observeBudgetChanges() {
        viewModelScope.launch {
            // 监听当前预算变化
            repository.getCurrentBudgets().collect { budgets ->
                _currentBudgets.value = budgets
                updateBudgetStats(budgets)
            }
        }
        
        viewModelScope.launch {
            // 监听超支预算
            repository.getOverspentBudgets().collect { budgets ->
                _overspentBudgets.value = budgets
            }
        }
        
        viewModelScope.launch {
            // 监听需要警告的预算
            repository.getNearLimitBudgets().collect { budgets ->
                _warningBudgets.value = budgets
            }
        }
    }
    
    /**
     * 加载预算列表
     */
    fun loadBudgets() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val allBudgets = repository.getCurrentBudgetsList()
                _budgets.value = allBudgets
                
                updateBudgetOverview(allBudgets)
                
            } catch (e: Exception) {
                _error.value = "加载预算失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 加载分类列表
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                repository.getExpenseCategories().collect { categories ->
                    _categories.value = categories
                }
            } catch (e: Exception) {
                _error.value = "加载分类失败: ${e.message}"
            }
        }
    }
    
    /**
     * 创建预算
     */
    fun createBudget(
        name: String,
        categoryId: String?,
        amount: Double,
        period: Budget.BudgetPeriod,
        description: String?,
        alertThreshold: Double = 0.8,
        isRecurring: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val startDate = System.currentTimeMillis()
                val endDate = calculateEndDate(startDate, period)
                
                val budget = Budget(
                    name = name,
                    categoryId = categoryId,
                    amount = amount,
                    period = period,
                    startDate = startDate,
                    endDate = endDate,
                    description = description,
                    alertThreshold = alertThreshold,
                    isRecurring = isRecurring,
                    isActive = true
                )
                
                repository.insertBudget(budget)
                _successMessage.value = "预算创建成功"
                
                // 重新加载预算列表
                loadBudgets()
                
            } catch (e: Exception) {
                _error.value = "创建预算失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 更新预算
     */
    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                repository.updateBudget(budget)
                loadBudgets() // 重新加载数据
            } catch (e: Exception) {
                _error.value = "更新预算失败: ${e.message}"
            }
        }
    }
    
    /**
     * 删除预算
     */
    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                repository.deleteBudget(budget)
                loadBudgets() // 重新加载数据
            } catch (e: Exception) {
                _error.value = "删除预算失败: ${e.message}"
            }
        }
    }
    
    /**
     * 切换预算激活状态
     */
    fun toggleBudgetActive(budget: Budget) {
        viewModelScope.launch {
            try {
                val updatedBudget = budget.copy(
                    isActive = !budget.isActive,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateBudget(updatedBudget)
                loadBudgets()
            } catch (e: Exception) {
                _error.value = "更新预算状态失败: ${e.message}"
            }
        }
    }
    
    /**
     * 重置预算（新周期）
     */
    fun resetBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val resetBudget = budget.resetForNewPeriod()
                repository.updateBudget(resetBudget)
                _successMessage.value = "预算已重置为新周期"
                
            } catch (e: Exception) {
                _error.value = "重置预算失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 更新预算花费
     */
    fun updateBudgetSpent(budgetId: String, spentAmount: Double) {
        viewModelScope.launch {
            try {
                repository.updateBudgetSpent(budgetId, spentAmount)
                
            } catch (e: Exception) {
                _error.value = "Update budget spending failed: ${e.message}"
            }
        }
    }
    
    /**
     * Set search query
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadBudgets()
    }
    
    /**
     * Set period filter
     */
    fun setFilterPeriod(period: Budget.BudgetPeriod?) {
        _filterPeriod.value = period
        loadBudgets()
    }
    
    /**
     * Set status filter
     */
    fun setFilterStatus(status: BudgetFilterStatus?) {
        _filterStatus.value = status
        loadBudgets()
    }
    
    /**
     * Clear all filters
     */
    fun clearFilters() {
        _filterPeriod.value = null
        _filterStatus.value = null
        _searchQuery.value = ""
        loadBudgets()
    }
    
    /**
     * Apply filter conditions
     */
    private fun applyFilters(budgets: List<Budget>): List<Budget> {
        var filtered = budgets
        
        // Apply search query
        if (_searchQuery.value.isNotBlank()) {
            filtered = filtered.filter { budget ->
                budget.name.contains(_searchQuery.value, ignoreCase = true) ||
                budget.description?.contains(_searchQuery.value, ignoreCase = true) == true
            }
        }
        
        // Apply period filter
        _filterPeriod.value?.let { period ->
            filtered = filtered.filter { it.period == period }
        }
        
        // Apply status filter
        _filterStatus.value?.let { status ->
            filtered = when (status) {
                BudgetFilterStatus.ACTIVE -> filtered.filter { it.isActive && !it.isExpired() }
                BudgetFilterStatus.EXPIRED -> filtered.filter { it.isExpired() }
                BudgetFilterStatus.OVERSPENT -> filtered.filter { it.getBudgetStatus() == Budget.BudgetStatus.EXCEEDED }
                BudgetFilterStatus.WARNING -> filtered.filter { it.getBudgetStatus() == Budget.BudgetStatus.WARNING }
                BudgetFilterStatus.ALL -> filtered
            }
        }
        
        return filtered.sortedByDescending { it.createdAt }
    }
    
    /**
     * 计算预算结束日期
     */
    private fun calculateEndDate(startDate: Long, period: Budget.BudgetPeriod): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startDate
        }
        
        return when (period) {
            Budget.BudgetPeriod.DAILY -> {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                calendar.timeInMillis - 1
            }
            Budget.BudgetPeriod.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.timeInMillis - 1
            }
            Budget.BudgetPeriod.MONTHLY -> {
                calendar.add(Calendar.MONTH, 1)
                calendar.timeInMillis - 1
            }
            Budget.BudgetPeriod.QUARTERLY -> {
                calendar.add(Calendar.MONTH, 3)
                calendar.timeInMillis - 1
            }
            Budget.BudgetPeriod.YEARLY -> {
                calendar.add(Calendar.YEAR, 1)
                calendar.timeInMillis - 1
            }
        }
    }
    
    /**
     * 更新预算统计
     */
    private fun updateBudgetStats(budgets: List<Budget>) {
        viewModelScope.launch {
            try {
                val totalBudgets = budgets.size
                val activeBudgets = budgets.count { it.isActive }
                val overspentBudgets = budgets.count { it.getBudgetStatus() == Budget.BudgetStatus.EXCEEDED }
                val warningBudgets = budgets.count { it.getBudgetStatus() == Budget.BudgetStatus.WARNING }
                val safeBudgets = budgets.count { it.getBudgetStatus() == Budget.BudgetStatus.SAFE }
                
                val totalAmount = budgets.sumOf { it.amount }
                val totalSpent = budgets.sumOf { it.spent }
                val remainingAmount = totalAmount - totalSpent
                
                val overview = com.example.life_ledger.data.dao.BudgetOverview(
                    totalCount = totalBudgets,
                    totalAmount = totalAmount,
                    totalSpent = totalSpent,
                    avgUsageRate = if (totalAmount > 0) (totalSpent / totalAmount * 100) else 0.0,
                    overspentCount = overspentBudgets
                )
                
                _budgetOverview.value = overview
                
            } catch (e: Exception) {
                _error.value = "Update budget statistics failed: ${e.message}"
            }
        }
    }
    
    /**
     * Get budget recommendations
     */
    fun getBudgetRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val overview = _budgetOverview.value
        
        overview?.let {
            val usageRate = if (it.totalAmount > 0) (it.totalSpent / it.totalAmount) * 100 else 0.0
            
            when {
                it.overspentCount > 0 -> {
                    recommendations.add("You have ${it.overspentCount} overspent budgets, consider adjusting spending plan")
                }
                usageRate > 80 -> {
                    recommendations.add("Budget usage rate is high, please spend carefully")
                }
                usageRate < 50 -> {
                    recommendations.add("Budget usage rate is low, consider increasing spending or adjusting budget")
                }
            }
            
            if (it.totalCount == 0) {
                recommendations.add("Consider creating budgets to manage your expenses")
            }
        }
        
        return recommendations
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * 清除成功消息
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    /**
     * 更新预算概览
     */
    private fun updateBudgetOverview(budgets: List<Budget>) {
        val activeBudgets = budgets.filter { it.isActive }
        val totalAmount = activeBudgets.sumOf { it.amount }
        val totalSpent = activeBudgets.sumOf { it.spent }
        val remainingAmount = totalAmount - totalSpent
        val overBudgetCount = activeBudgets.count { it.getBudgetStatus() == Budget.BudgetStatus.EXCEEDED }
        val warningCount = activeBudgets.count { it.getBudgetStatus() == Budget.BudgetStatus.WARNING }
        
        _budgetOverview.value = com.example.life_ledger.data.dao.BudgetOverview(
            totalCount = budgets.size,
            totalAmount = totalAmount,
            totalSpent = totalSpent,
            avgUsageRate = if (totalAmount > 0) (totalSpent / totalAmount * 100) else 0.0,
            overspentCount = overBudgetCount
        )
    }
} 