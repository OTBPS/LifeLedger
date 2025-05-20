package com.example.life_ledger.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.life_ledger.data.repository.LifeLedgerRepository
import com.example.life_ledger.data.service.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

/**
 * AI分析ViewModel
 */
class AIAnalysisViewModel(
    private val repository: LifeLedgerRepository
) : ViewModel() {
    
    private val aiAnalysisService = AIAnalysisService()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // 支出分析结果
    private val _expenseAnalysis = MutableStateFlow<ExpenseAnalysis?>(null)
    val expenseAnalysis: StateFlow<ExpenseAnalysis?> = _expenseAnalysis.asStateFlow()
    
    // 月度报告
    private val _monthlyReport = MutableStateFlow<MonthlyReport?>(null)
    val monthlyReport: StateFlow<MonthlyReport?> = _monthlyReport.asStateFlow()
    
    // 个性化建议
    private val _consumptionAdvice = MutableStateFlow<List<ConsumptionAdvice>>(emptyList())
    val consumptionAdvice: StateFlow<List<ConsumptionAdvice>> = _consumptionAdvice.asStateFlow()
    
    // 用户配置
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()
    
    /**
     * 分析最近支出
     */
    fun analyzeRecentExpenses() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // 获取最近3个月的交易数据
                val recentTransactions = repository.getAllTransactions().first()
                val categories = repository.getAllCategories().first()
                
                if (recentTransactions.isEmpty()) {
                    _errorMessage.value = "暂无交易数据，无法进行分析"
                    return@launch
                }
                
                val result = aiAnalysisService.analyzeExpenses(recentTransactions, categories)
                
                result.onSuccess { analysis ->
                    _expenseAnalysis.value = analysis
                }.onFailure { exception ->
                    _errorMessage.value = "分析失败: ${exception.message}"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "分析过程中发生错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 生成月度报告
     */
    fun generateMonthlyReport(year: Int? = null, month: Int? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val calendar = Calendar.getInstance()
                val targetYear = year ?: calendar.get(Calendar.YEAR)
                val targetMonth = month ?: (calendar.get(Calendar.MONTH) + 1)
                
                val allTransactions = repository.getAllTransactions().first()
                val categories = repository.getAllCategories().first()
                
                if (allTransactions.isEmpty()) {
                    _errorMessage.value = "暂无交易数据，无法生成报告"
                    return@launch
                }
                
                val result = aiAnalysisService.generateMonthlyReport(
                    allTransactions, 
                    categories, 
                    targetYear, 
                    targetMonth
                )
                
                result.onSuccess { report ->
                    _monthlyReport.value = report
                }.onFailure { exception ->
                    _errorMessage.value = "报告生成失败: ${exception.message}"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "报告生成过程中发生错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 获取个性化建议
     */
    fun getPersonalizedAdvice() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val allTransactions = repository.getAllTransactions().first()
                val categories = repository.getAllCategories().first()
                
                if (allTransactions.isEmpty()) {
                    _errorMessage.value = "暂无交易数据，无法提供建议"
                    return@launch
                }
                
                val result = aiAnalysisService.getPersonalizedAdvice(
                    allTransactions,
                    categories,
                    _userProfile.value
                )
                
                result.onSuccess { advice ->
                    _consumptionAdvice.value = advice
                }.onFailure { exception ->
                    _errorMessage.value = "建议生成失败: ${exception.message}"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "建议生成过程中发生错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 更新用户配置
     */
    fun updateUserProfile(profile: UserProfile) {
        _userProfile.value = profile
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * 一键分析（执行所有分析）
     */
    fun performFullAnalysis() {
        viewModelScope.launch {
            analyzeRecentExpenses()
            generateMonthlyReport()
            getPersonalizedAdvice()
        }
    }
} 