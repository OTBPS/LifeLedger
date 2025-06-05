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
                
                android.util.Log.d("AIAnalysisViewModel", "Starting expense analysis...")
                
                // 获取最近3个月的交易数据
                val recentTransactions = repository.getAllTransactions().first()
                val categories = repository.getAllCategories().first()
                
                android.util.Log.d("AIAnalysisViewModel", "Loaded ${recentTransactions.size} transactions and ${categories.size} categories")
                
                if (recentTransactions.isEmpty()) {
                    _errorMessage.value = "No transaction data available for analysis. Please add some transactions first."
                    android.util.Log.w("AIAnalysisViewModel", "No transactions found for analysis")
                    return@launch
                }
                
                android.util.Log.d("AIAnalysisViewModel", "Calling AI analysis service...")
                
                val result = aiAnalysisService.analyzeExpenses(recentTransactions, categories)
                
                result.onSuccess { analysis ->
                    _expenseAnalysis.value = analysis
                    android.util.Log.d("AIAnalysisViewModel", "Expense analysis completed successfully")
                }.onFailure { exception ->
                    val errorMsg = when {
                        exception.message?.contains("timeout") == true -> 
                            "Analysis is taking longer than expected. Please check your network connection and try again."
                        exception.message?.contains("network") == true -> 
                            "Network connection issue. Please check your internet connection and try again."
                        exception.message?.contains("failed") == true -> 
                            "AI service is temporarily unavailable. Please try again in a few minutes."
                        else -> 
                            "Analysis failed: ${exception.message}. Please try again."
                    }
                    _errorMessage.value = errorMsg
                    android.util.Log.e("AIAnalysisViewModel", "Expense analysis failed", exception)
                }
                
            } catch (e: Exception) {
                val errorMsg = "An unexpected error occurred during analysis. Please try again."
                _errorMessage.value = errorMsg
                android.util.Log.e("AIAnalysisViewModel", "Unexpected error during expense analysis", e)
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
                
                android.util.Log.d("AIAnalysisViewModel", "Starting monthly report generation for $targetYear-$targetMonth...")
                
                val allTransactions = repository.getAllTransactions().first()
                val categories = repository.getAllCategories().first()
                
                android.util.Log.d("AIAnalysisViewModel", "Loaded ${allTransactions.size} transactions and ${categories.size} categories for report")
                
                if (allTransactions.isEmpty()) {
                    _errorMessage.value = "No transaction data available to generate report. Please add some transactions first."
                    android.util.Log.w("AIAnalysisViewModel", "No transactions found for monthly report")
                    return@launch
                }
                
                android.util.Log.d("AIAnalysisViewModel", "Calling AI monthly report service...")
                
                val result = aiAnalysisService.generateMonthlyReport(
                    allTransactions, 
                    categories, 
                    targetYear, 
                    targetMonth
                )
                
                result.onSuccess { report ->
                    _monthlyReport.value = report
                    android.util.Log.d("AIAnalysisViewModel", "Monthly report generated successfully")
                }.onFailure { exception ->
                    val errorMsg = when {
                        exception.message?.contains("timeout") == true -> 
                            "Report generation is taking longer than expected. Please check your network connection and try again."
                        exception.message?.contains("network") == true -> 
                            "Network connection issue. Please check your internet connection and try again."
                        exception.message?.contains("failed") == true -> 
                            "AI service is temporarily unavailable. Please try again in a few minutes."
                        else -> 
                            "Report generation failed: ${exception.message}. Please try again."
                    }
                    _errorMessage.value = errorMsg
                    android.util.Log.e("AIAnalysisViewModel", "Monthly report generation failed", exception)
                }
                
            } catch (e: Exception) {
                val errorMsg = "An unexpected error occurred during report generation. Please try again."
                _errorMessage.value = errorMsg
                android.util.Log.e("AIAnalysisViewModel", "Unexpected error during monthly report generation", e)
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
                
                android.util.Log.d("AIAnalysisViewModel", "Starting personalized advice generation...")
                
                val allTransactions = repository.getAllTransactions().first()
                val categories = repository.getAllCategories().first()
                
                android.util.Log.d("AIAnalysisViewModel", "Loaded ${allTransactions.size} transactions and ${categories.size} categories for advice")
                
                if (allTransactions.isEmpty()) {
                    _errorMessage.value = "No transaction data available to provide advice. Please add some transactions first."
                    android.util.Log.w("AIAnalysisViewModel", "No transactions found for personalized advice")
                    return@launch
                }
                
                android.util.Log.d("AIAnalysisViewModel", "Calling AI personalized advice service...")
                
                val result = aiAnalysisService.getPersonalizedAdvice(
                    allTransactions,
                    categories,
                    _userProfile.value
                )
                
                result.onSuccess { advice ->
                    _consumptionAdvice.value = advice
                    android.util.Log.d("AIAnalysisViewModel", "Personalized advice generated successfully, count: ${advice.size}")
                }.onFailure { exception ->
                    val errorMsg = when {
                        exception.message?.contains("timeout") == true -> 
                            "Advice generation is taking longer than expected. Please check your network connection and try again."
                        exception.message?.contains("network") == true -> 
                            "Network connection issue. Please check your internet connection and try again."
                        exception.message?.contains("failed") == true -> 
                            "AI service is temporarily unavailable. Please try again in a few minutes."
                        else -> 
                            "Advice generation failed: ${exception.message}. Please try again."
                    }
                    _errorMessage.value = errorMsg
                    android.util.Log.e("AIAnalysisViewModel", "Personalized advice generation failed", exception)
                }
                
            } catch (e: Exception) {
                val errorMsg = "An unexpected error occurred during advice generation. Please try again."
                _errorMessage.value = errorMsg
                android.util.Log.e("AIAnalysisViewModel", "Unexpected error during personalized advice generation", e)
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
            try {
                android.util.Log.d("AIAnalysisViewModel", "Starting full analysis...")
                
                // 检查数据是否可用
                val allTransactions = repository.getAllTransactions().first()
                if (allTransactions.isEmpty()) {
                    _errorMessage.value = "No transaction data available for analysis. Please add some transactions first."
                    android.util.Log.w("AIAnalysisViewModel", "No transactions found for full analysis")
                    return@launch
                }
                
                android.util.Log.d("AIAnalysisViewModel", "Running all analysis components...")
                
                // 并行执行所有分析，但错误处理独立
                launch { 
                    try {
                        analyzeRecentExpenses() 
                    } catch (e: Exception) {
                        android.util.Log.e("AIAnalysisViewModel", "Error in expense analysis", e)
                    }
                }
                launch { 
                    try {
                        generateMonthlyReport() 
                    } catch (e: Exception) {
                        android.util.Log.e("AIAnalysisViewModel", "Error in monthly report", e)
                    }
                }
                launch { 
                    try {
                        getPersonalizedAdvice() 
                    } catch (e: Exception) {
                        android.util.Log.e("AIAnalysisViewModel", "Error in personalized advice", e)
                    }
                }
                
                android.util.Log.d("AIAnalysisViewModel", "Full analysis components launched")
                
            } catch (e: Exception) {
                android.util.Log.e("AIAnalysisViewModel", "Error during full analysis setup", e)
                _errorMessage.value = "Failed to start comprehensive analysis. Please try again."
            }
        }
    }
} 