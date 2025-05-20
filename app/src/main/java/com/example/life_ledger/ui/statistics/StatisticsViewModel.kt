package com.example.life_ledger.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.life_ledger.data.model.Transaction
import com.example.life_ledger.data.model.Category
import com.example.life_ledger.data.model.Budget
import com.example.life_ledger.data.repository.LifeLedgerRepository
import com.example.life_ledger.ui.finance.OperationResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 统计页面ViewModel
 * 负责处理统计数据的获取和计算
 */
class StatisticsViewModel(private val repository: LifeLedgerRepository) : ViewModel() {

    // 时间范围枚举
    enum class TimeRange {
        THIS_WEEK,    // 本周
        THIS_MONTH,   // 本月  
        THIS_YEAR     // 今年
    }

    // 月度统计时间范围枚举
    enum class MonthlyTimeRange {
        LAST_7_DAYS,       // 近7天
        THIS_YEAR,         // 今年
        LAST_YEAR,         // 去年
        LAST_24_MONTHS     // 最近24个月
    }

    // 当前选择的时间范围
    private val _currentTimeRange = MutableStateFlow(TimeRange.THIS_MONTH)
    val currentTimeRange: StateFlow<TimeRange> = _currentTimeRange.asStateFlow()

    // 当前选择的月度统计时间范围
    private var currentMonthlyTimeRange = MonthlyTimeRange.LAST_7_DAYS

    // 财务统计数据
    private val _financialSummary = MutableStateFlow(FinancialSummaryData())
    val financialSummary: StateFlow<FinancialSummaryData> = _financialSummary.asStateFlow()

    // 支出趋势数据（日期 -> 金额）
    private val _expenseTrendData = MutableStateFlow<List<DailyExpenseData>>(emptyList())
    val expenseTrendData: StateFlow<List<DailyExpenseData>> = _expenseTrendData.asStateFlow()

    // 收入趋势数据（日期 -> 金额）
    private val _incomeTrendData = MutableStateFlow<List<DailyExpenseData>>(emptyList())
    val incomeTrendData: StateFlow<List<DailyExpenseData>> = _incomeTrendData.asStateFlow()

    // 分类统计数据
    private val _categoryData = MutableStateFlow<List<CategoryExpenseData>>(emptyList())
    val categoryData: StateFlow<List<CategoryExpenseData>> = _categoryData.asStateFlow()

    // 月度收支数据
    private val _monthlyData = MutableStateFlow<List<MonthlyStatistic>>(emptyList())
    val monthlyData: StateFlow<List<MonthlyStatistic>> = _monthlyData.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 操作结果
    private val _operationResult = MutableSharedFlow<OperationResult>()
    val operationResult: SharedFlow<OperationResult> = _operationResult.asSharedFlow()

    // 是否为空状态
    private val _isEmpty = MutableStateFlow(false)
    val isEmpty: StateFlow<Boolean> = _isEmpty.asStateFlow()

    // ==================== 数据分析相关数据流 ====================

    // 月度收支统计
    private val _monthlyFinancialSummary = MutableStateFlow<List<MonthlyFinancialSummary>>(emptyList())
    val monthlyFinancialSummary: StateFlow<List<MonthlyFinancialSummary>> = _monthlyFinancialSummary.asStateFlow()

    // 年度收支统计
    private val _yearlyFinancialSummary = MutableStateFlow<List<YearlyFinancialSummary>>(emptyList())
    val yearlyFinancialSummary: StateFlow<List<YearlyFinancialSummary>> = _yearlyFinancialSummary.asStateFlow()

    // 支出模式分析
    private val _expensePatternAnalysis = MutableStateFlow<ExpensePatternAnalysis?>(null)
    val expensePatternAnalysis: StateFlow<ExpensePatternAnalysis?> = _expensePatternAnalysis.asStateFlow()

    // 预算跟踪状态
    private val _budgetTrackingStatus = MutableStateFlow<BudgetTrackingStatus?>(null)
    val budgetTrackingStatus: StateFlow<BudgetTrackingStatus?> = _budgetTrackingStatus.asStateFlow()

    // 财务健康度评估
    private val _financialHealthAssessment = MutableStateFlow<FinancialHealthAssessment?>(null)
    val financialHealthAssessment: StateFlow<FinancialHealthAssessment?> = _financialHealthAssessment.asStateFlow()

    // 趋势类型
    enum class TrendType {
        EXPENSE, INCOME
    }
    
    private var currentTrendType = TrendType.EXPENSE

    init {
        loadStatistics()
        calculateMonthlyStatistics()
        // 启动数据分析
        startDataAnalysis()
    }

    /**
     * 设置时间范围
     */
    fun setTimeRange(timeRange: TimeRange) {
        _currentTimeRange.value = timeRange
        loadStatistics()
    }

    /**
     * 设置月度统计时间范围
     */
    fun setMonthlyTimeRange(timeRange: MonthlyTimeRange) {
        currentMonthlyTimeRange = timeRange
        calculateMonthlyStatistics()
    }

    /**
     * 刷新所有统计数据
     */
    fun refresh() {
        android.util.Log.d("StatisticsViewModel", "手动刷新统计数据")
        loadStatistics()
    }

    /**
     * 加载统计数据
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val (startTime, endTime) = getTimeRangeMillis(_currentTimeRange.value)
                
                // 获取时间范围内的所有交易
                val transactions = repository.getTransactionsByDateRange(startTime, endTime).first()
                
                android.util.Log.d("StatisticsViewModel", "加载统计数据：时间范围内获取到 ${transactions.size} 笔交易")
                
                if (transactions.isEmpty()) {
                    _isEmpty.value = true
                    _financialSummary.value = FinancialSummaryData()
                    _expenseTrendData.value = emptyList()
                    _incomeTrendData.value = emptyList()
                    _categoryData.value = emptyList()
                    // 月度统计仍然计算，因为它使用固定的12个月范围
                    calculateMonthlyStatistics()
                } else {
                    _isEmpty.value = false
                    
                    // 计算财务概览
                    calculateFinancialSummary(transactions)
                    
                    // 计算支出趋势
                    calculateExpenseTrend(transactions, startTime, endTime)
                    
                    // 计算收入趋势
                    calculateIncomeTrend(transactions, startTime, endTime)
                    
                    // 计算分类统计
                    calculateCategoryStatistics(transactions)
                    
                    // 计算月度统计（独立的时间范围）
                    calculateMonthlyStatistics()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("StatisticsViewModel", "加载统计数据失败", e)
                _operationResult.emit(OperationResult(false, "加载统计数据失败: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 计算财务概览
     */
    private fun calculateFinancialSummary(transactions: List<Transaction>) {
        val income = transactions
            .filter { it.type == Transaction.TransactionType.INCOME }
            .sumOf { it.amount }
        
        val expense = transactions
            .filter { it.type == Transaction.TransactionType.EXPENSE }
            .sumOf { it.amount }
        
        _financialSummary.value = FinancialSummaryData(
            totalIncome = income,
            totalExpense = expense,
            netBalance = income - expense,
            transactionCount = transactions.size
        )
    }

    /**
     * 计算支出趋势
     */
    private fun calculateExpenseTrend(transactions: List<Transaction>, startTime: Long, endTime: Long) {
        val expenses = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        
        // 按日期分组计算每日支出
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dailyExpenses = mutableMapOf<String, Double>()
        
        // 生成日期范围内的所有日期
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startTime
        
        while (calendar.timeInMillis <= endTime) {
            val dateKey = dateFormat.format(calendar.time)
            dailyExpenses[dateKey] = 0.0
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // 填入实际支出数据
        expenses.forEach { transaction ->
            val dateKey = dateFormat.format(Date(transaction.date))
            dailyExpenses[dateKey] = dailyExpenses.getOrDefault(dateKey, 0.0) + transaction.amount
        }
        
        // 转换为图表数据
        val trendData = dailyExpenses.entries
            .sortedBy { it.key }
            .map { (date, amount) ->
                DailyExpenseData(date, amount)
            }
        
        _expenseTrendData.value = trendData
    }

    /**
     * 计算收入趋势
     */
    private fun calculateIncomeTrend(transactions: List<Transaction>, startTime: Long, endTime: Long) {
        val incomes = transactions.filter { it.type == Transaction.TransactionType.INCOME }
        
        // 按日期分组计算每日收入
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dailyIncomes = mutableMapOf<String, Double>()
        
        // 生成日期范围内的所有日期
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startTime
        
        while (calendar.timeInMillis <= endTime) {
            val dateKey = dateFormat.format(calendar.time)
            dailyIncomes[dateKey] = 0.0
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // 填入实际收入数据
        incomes.forEach { transaction ->
            val dateKey = dateFormat.format(Date(transaction.date))
            dailyIncomes[dateKey] = dailyIncomes.getOrDefault(dateKey, 0.0) + transaction.amount
        }
        
        // 转换为图表数据
        val trendData = dailyIncomes.entries
            .sortedBy { it.key }
            .map { (date, amount) ->
                DailyExpenseData(date, amount)
            }
        
        _incomeTrendData.value = trendData
    }

    /**
     * 计算分类统计
     */
    private fun calculateCategoryStatistics(transactions: List<Transaction>) {
        viewModelScope.launch {
            try {
                // 获取所有分类
                val categories = repository.getFinancialCategories().first()
                val categoryMap = categories.associateBy { it.id }
                
                // 按分类分组统计支出
                val expenseByCategory = transactions
                    .filter { it.type == Transaction.TransactionType.EXPENSE }
                    .groupBy { it.categoryId }
                    .mapNotNull { (categoryId, transactions) ->
                        val category = categoryMap[categoryId]
                        if (category != null) {
                            CategoryExpenseData(
                                categoryName = category.name,
                                amount = transactions.sumOf { it.amount },
                                color = category.color,
                                transactionCount = transactions.size
                            )
                        } else {
                            CategoryExpenseData(
                                categoryName = "未分类",
                                amount = transactions.sumOf { it.amount },
                                color = "#607D8B",
                                transactionCount = transactions.size
                            )
                        }
                    }
                    .sortedByDescending { it.amount }
                
                _categoryData.value = expenseByCategory
                
            } catch (e: Exception) {
                _operationResult.emit(OperationResult(false, "计算分类统计失败: ${e.message}"))
            }
        }
    }

    /**
     * 计算月度统计
     */
    private fun calculateMonthlyStatistics() {
        viewModelScope.launch {
            try {
                val timeRange = currentMonthlyTimeRange
                val (startTime, endTime, monthCount, monthsList) = getMonthlyTimeRangeData(timeRange)
                
                android.util.Log.d("StatisticsViewModel", "月度统计：时间范围 $timeRange")
                android.util.Log.d("StatisticsViewModel", "月度统计：查询时间范围 ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(startTime))} 到 ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(endTime))}")
                android.util.Log.d("StatisticsViewModel", "月度统计：月份列表 $monthsList")
                
                val transactions = repository.getTransactionsByDateRange(startTime, endTime).first()
                android.util.Log.d("StatisticsViewModel", "月度统计：获取到 ${transactions.size} 笔交易")
                
                // 如果有交易数据，打印一些示例
                if (transactions.isNotEmpty()) {
                    android.util.Log.d("StatisticsViewModel", "交易示例：")
                    transactions.take(5).forEach { transaction ->
                        android.util.Log.d("StatisticsViewModel", "  日期: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(transaction.date))}, 金额: ${transaction.amount}, 类型: ${transaction.type}")
                    }
                }
                
                // 重构后的月度统计逻辑（支持日期和月份两种模式）
                val isDaily = timeRange == MonthlyTimeRange.LAST_7_DAYS
                val keyFormat = if (isDaily) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) 
                               else SimpleDateFormat("yyyy-MM", Locale.getDefault())
                
                val keyToLabelMap = mutableMapOf<String, String>()
                val monthlyMap = mutableMapOf<String, Pair<Double, Double>>()
                
                // 初始化时间段数据
                for (key in monthsList) {
                    monthlyMap[key] = Pair(0.0, 0.0)
                    
                    // 为日期生成友好显示标签
                    if (isDaily) {
                        try {
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(key)
                            if (date != null) {
                                val dayFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
                                keyToLabelMap[key] = dayFormat.format(date)
                            }
                        } catch (e: Exception) {
                            keyToLabelMap[key] = key
                        }
                    } else {
                        keyToLabelMap[key] = key
                    }
                }
                
                // 统计交易数据
                for (transaction in transactions) {
                    val transactionKey = keyFormat.format(Date(transaction.date))
                    
                    if (monthlyMap.containsKey(transactionKey)) {
                        val (currentIncome, currentExpense) = monthlyMap[transactionKey]!!
                        
                        when (transaction.type) {
                            Transaction.TransactionType.INCOME -> {
                                monthlyMap[transactionKey] = Pair(currentIncome + transaction.amount, currentExpense)
                            }
                            Transaction.TransactionType.EXPENSE -> {
                                monthlyMap[transactionKey] = Pair(currentIncome, currentExpense + transaction.amount)
                            }
                        }
                    }
                }
                
                // 构建结果列表
                val result = monthsList.map { key ->
                    val (income, expense) = monthlyMap[key] ?: Pair(0.0, 0.0)
                    val label = keyToLabelMap[key] ?: key
                    MonthlyStatistic(label, income, expense)
                }
                
                val dataMonthsCount = result.count { it.expense > 0 || it.income > 0 }
                android.util.Log.d("StatisticsViewModel", "月度统计完成：生成 ${result.size} 个月的数据，有数据的月份：$dataMonthsCount")
                
                _monthlyData.value = result
                
            } catch (e: Exception) {
                android.util.Log.e("StatisticsViewModel", "计算月度统计失败", e)
                _operationResult.emit(OperationResult(false, "计算月度统计失败: ${e.message}"))
            }
        }
    }

    /**
     * 获取月度统计时间范围数据
     */
    private fun getMonthlyTimeRangeData(timeRange: MonthlyTimeRange): MonthlyTimeRangeData {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)
        
        return when (timeRange) {
            MonthlyTimeRange.LAST_7_DAYS -> {
                // 近7天
                val endCalendar = Calendar.getInstance()
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                endCalendar.set(Calendar.MILLISECOND, 999)
                val endTime = endCalendar.timeInMillis
                
                val startCalendar = Calendar.getInstance()
                startCalendar.add(Calendar.DAY_OF_MONTH, -6)
                startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startCalendar.set(Calendar.MINUTE, 0)
                startCalendar.set(Calendar.SECOND, 0)
                startCalendar.set(Calendar.MILLISECOND, 0)
                val startTime = startCalendar.timeInMillis
                
                val daysList = generateDaysList(startCalendar, 7)
                MonthlyTimeRangeData(startTime, endTime, 7, daysList)
            }
            
            MonthlyTimeRange.THIS_YEAR -> {
                // 今年1月到当前月
                val startCalendar = Calendar.getInstance()
                startCalendar.set(Calendar.YEAR, currentYear)
                startCalendar.set(Calendar.MONTH, Calendar.JANUARY)
                startCalendar.set(Calendar.DAY_OF_MONTH, 1)
                startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startCalendar.set(Calendar.MINUTE, 0)
                startCalendar.set(Calendar.SECOND, 0)
                startCalendar.set(Calendar.MILLISECOND, 0)
                val startTime = startCalendar.timeInMillis
                
                val endTime = System.currentTimeMillis()
                val monthCount = currentMonth + 1 // +1因为Calendar.MONTH是0-based
                val monthsList = generateMonthsList(startCalendar, monthCount)
                MonthlyTimeRangeData(startTime, endTime, monthCount, monthsList)
            }
            
            MonthlyTimeRange.LAST_YEAR -> {
                // 去年全年
                val startCalendar = Calendar.getInstance()
                startCalendar.set(Calendar.YEAR, currentYear - 1)
                startCalendar.set(Calendar.MONTH, Calendar.JANUARY)
                startCalendar.set(Calendar.DAY_OF_MONTH, 1)
                startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startCalendar.set(Calendar.MINUTE, 0)
                startCalendar.set(Calendar.SECOND, 0)
                startCalendar.set(Calendar.MILLISECOND, 0)
                val startTime = startCalendar.timeInMillis
                
                val endCalendar = Calendar.getInstance()
                endCalendar.set(Calendar.YEAR, currentYear - 1)
                endCalendar.set(Calendar.MONTH, Calendar.DECEMBER)
                endCalendar.set(Calendar.DAY_OF_MONTH, 31)
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                endCalendar.set(Calendar.MILLISECOND, 999)
                val endTime = endCalendar.timeInMillis
                
                val monthsList = generateMonthsList(startCalendar, 12)
                MonthlyTimeRangeData(startTime, endTime, 12, monthsList)
            }
            
            MonthlyTimeRange.LAST_24_MONTHS -> {
                // 最近24个月
                val endTime = System.currentTimeMillis()
                val startCalendar = Calendar.getInstance()
                startCalendar.set(Calendar.YEAR, currentYear)
                startCalendar.set(Calendar.MONTH, currentMonth)
                startCalendar.add(Calendar.MONTH, -23)
                startCalendar.set(Calendar.DAY_OF_MONTH, 1)
                startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startCalendar.set(Calendar.MINUTE, 0)
                startCalendar.set(Calendar.SECOND, 0)
                startCalendar.set(Calendar.MILLISECOND, 0)
                val startTime = startCalendar.timeInMillis
                
                val monthsList = generateMonthsList(startCalendar, 24)
                MonthlyTimeRangeData(startTime, endTime, 24, monthsList)
            }
        }
    }

    /**
     * 生成月份列表
     */
    private fun generateMonthsList(startCalendar: Calendar, monthCount: Int): List<String> {
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthsList = mutableListOf<String>()
        val tempCalendar = Calendar.getInstance()
        tempCalendar.timeInMillis = startCalendar.timeInMillis
        
        repeat(monthCount) {
            val monthKey = monthFormat.format(tempCalendar.time)
            monthsList.add(monthKey)
            tempCalendar.add(Calendar.MONTH, 1)
        }
        
        return monthsList
    }

    /**
     * 生成日期列表
     */
    private fun generateDaysList(startCalendar: Calendar, dayCount: Int): List<String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val daysList = mutableListOf<String>()
        val tempCalendar = Calendar.getInstance()
        tempCalendar.timeInMillis = startCalendar.timeInMillis
        
        repeat(dayCount) {
            val dayKey = dateFormat.format(tempCalendar.time)
            daysList.add(dayKey)
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return daysList
    }

    /**
     * 获取时间范围的毫秒值
     */
    private fun getTimeRangeMillis(timeRange: TimeRange): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        
        when (timeRange) {
            TimeRange.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return Pair(calendar.timeInMillis, endTime)
            }
            TimeRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return Pair(calendar.timeInMillis, endTime)
            }
            TimeRange.THIS_YEAR -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return Pair(calendar.timeInMillis, endTime)
            }
        }
    }

    /**
     * 获取分类收入数据
     */
    fun getCategoryIncomeData(): Flow<List<CategoryExpenseData>> = flow {
        try {
            val (startTime, endTime) = getTimeRangeMillis(_currentTimeRange.value)
            val transactions = repository.getTransactionsByDateRange(startTime, endTime).first()
            val categories = repository.getFinancialCategories().first()
            val categoryMap = categories.associateBy { it.id }
            
            val incomeByCategory = transactions
                .filter { it.type == Transaction.TransactionType.INCOME }
                .groupBy { it.categoryId }
                .mapNotNull { (categoryId, transactions) ->
                    val category = categoryMap[categoryId]
                    if (category != null) {
                        CategoryExpenseData(
                            categoryName = category.name,
                            amount = transactions.sumOf { it.amount },
                            color = category.color,
                            transactionCount = transactions.size
                        )
                    } else {
                        CategoryExpenseData(
                            categoryName = "未分类",
                            amount = transactions.sumOf { it.amount },
                            color = "#607D8B",
                            transactionCount = transactions.size
                        )
                    }
                }
                .sortedByDescending { it.amount }
            
            emit(incomeByCategory)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    /**
     * 强制刷新月度统计
     */
    fun forceRefreshMonthlyData() {
        android.util.Log.d("StatisticsViewModel", "=== 强制刷新月度统计 ===")
        calculateMonthlyStatistics()
    }

    /**
     * 测试月度统计计算（调试用）
     */
    fun testMonthlyStatistics() {
        android.util.Log.d("StatisticsViewModel", "=== 开始测试月度统计计算 ===")
        calculateMonthlyStatistics()
    }

    // ==================== 数据分析功能 ====================

    /**
     * 启动数据分析
     */
    private fun startDataAnalysis() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // 并行执行各种分析
                launch { calculateMonthlyFinancialSummary() }
                launch { calculateYearlyFinancialSummary() }
                launch { analyzeExpensePatterns() }
                launch { analyzeBudgetTracking() }
                launch { assessFinancialHealth() }
                
            } catch (e: Exception) {
                android.util.Log.e("StatisticsViewModel", "数据分析启动失败", e)
                _operationResult.emit(OperationResult(false, "数据分析启动失败: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 计算月度收支统计
     */
    private suspend fun calculateMonthlyFinancialSummary() {
        try {
            val currentTime = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = currentTime
            
            val monthlyList = mutableListOf<MonthlyFinancialSummary>()
            
            // 计算最近12个月的统计
            for (i in 0 until 12) {
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
                
                // 获取该月的开始和结束时间
                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val monthEnd = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                
                // 获取该月的交易数据
                val transactions = repository.getTransactionsByDateRange(monthStart, monthEnd).first()
                
                val income = transactions.filter { it.type == Transaction.TransactionType.INCOME }.sumOf { it.amount }
                val expense = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }.sumOf { it.amount }
                val daysInMonth = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                }.getActualMaximum(Calendar.DAY_OF_MONTH)
                
                val monthlyFinancial = MonthlyFinancialSummary(
                    year = year,
                    month = month,
                    totalIncome = income,
                    totalExpense = expense,
                    netBalance = income - expense,
                    transactionCount = transactions.size,
                    avgDailyExpense = expense / daysInMonth,
                    biggestExpense = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
                        .maxOfOrNull { it.amount } ?: 0.0,
                    biggestIncome = transactions.filter { it.type == Transaction.TransactionType.INCOME }
                        .maxOfOrNull { it.amount } ?: 0.0
                )
                
                monthlyList.add(monthlyFinancial)
                
                // 移动到上一个月
                calendar.add(Calendar.MONTH, -1)
            }
            
            _monthlyFinancialSummary.value = monthlyList.reversed() // 按时间正序
            
        } catch (e: Exception) {
            android.util.Log.e("StatisticsViewModel", "计算月度统计失败", e)
        }
    }

    /**
     * 计算年度收支统计
     */
    private suspend fun calculateYearlyFinancialSummary() {
        try {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val yearlyList = mutableListOf<YearlyFinancialSummary>()
            
            // 计算最近3年的数据
            for (i in 0 until 3) {
                val year = currentYear - i
                
                // 获取该年的开始和结束时间
                val yearStart = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val yearEnd = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, Calendar.DECEMBER)
                    set(Calendar.DAY_OF_MONTH, 31)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                
                // 获取该年的交易数据
                val transactions = repository.getTransactionsByDateRange(yearStart, yearEnd).first()
                
                val income = transactions.filter { it.type == Transaction.TransactionType.INCOME }.sumOf { it.amount }
                val expense = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }.sumOf { it.amount }
                
                // 计算各月份数据，找出峰值月份
                val monthlyData = mutableMapOf<Int, Pair<Double, Double>>() // month -> (income, expense)
                for (month in 1..12) {
                    val monthStart = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    val monthEnd = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    
                    val monthTransactions = transactions.filter { it.date >= monthStart && it.date <= monthEnd }
                    val monthIncome = monthTransactions.filter { it.type == Transaction.TransactionType.INCOME }.sumOf { it.amount }
                    val monthExpense = monthTransactions.filter { it.type == Transaction.TransactionType.EXPENSE }.sumOf { it.amount }
                    
                    monthlyData[month] = Pair(monthIncome, monthExpense)
                }
                
                val peakExpenseMonth = monthlyData.maxByOrNull { it.value.second }?.key ?: 1
                val peakIncomeMonth = monthlyData.maxByOrNull { it.value.first }?.key ?: 1
                
                // 生成该年的月度统计
                val monthlySummaries = monthlyData.map { (month, data) ->
                    val daysInMonth = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                    }.getActualMaximum(Calendar.DAY_OF_MONTH)
                    
                    MonthlyFinancialSummary(
                        year = year,
                        month = month,
                        totalIncome = data.first,
                        totalExpense = data.second,
                        netBalance = data.first - data.second,
                        transactionCount = transactions.filter { 
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = it.date
                            cal.get(Calendar.MONTH) + 1 == month
                        }.size,
                        avgDailyExpense = data.second / daysInMonth,
                        biggestExpense = transactions.filter { 
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = it.date
                            cal.get(Calendar.MONTH) + 1 == month && it.type == Transaction.TransactionType.EXPENSE
                        }.maxOfOrNull { it.amount } ?: 0.0,
                        biggestIncome = transactions.filter { 
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = it.date
                            cal.get(Calendar.MONTH) + 1 == month && it.type == Transaction.TransactionType.INCOME
                        }.maxOfOrNull { it.amount } ?: 0.0
                    )
                }.sortedBy { it.month }
                
                val yearlyFinancial = YearlyFinancialSummary(
                    year = year,
                    totalIncome = income,
                    totalExpense = expense,
                    netBalance = income - expense,
                    transactionCount = transactions.size,
                    avgMonthlyExpense = expense / 12,
                    avgMonthlyIncome = income / 12,
                    peakExpenseMonth = peakExpenseMonth,
                    peakIncomeMonth = peakIncomeMonth,
                    monthlySummaries = monthlySummaries
                )
                
                yearlyList.add(yearlyFinancial)
            }
            
            _yearlyFinancialSummary.value = yearlyList.sortedByDescending { it.year }
            
        } catch (e: Exception) {
            android.util.Log.e("StatisticsViewModel", "计算年度统计失败", e)
        }
    }

    /**
     * 分析支出模式
     */
    private suspend fun analyzeExpensePatterns() {
        try {
            // 获取最近6个月的数据进行分析
            val sixMonthsAgo = Calendar.getInstance().apply {
                add(Calendar.MONTH, -6)
            }.timeInMillis
            val now = System.currentTimeMillis()
            
            val transactions = repository.getTransactionsByDateRange(sixMonthsAgo, now).first()
            val categories = repository.getFinancialCategories().first()
            val categoryMap = categories.associateBy { it.id }
            
            // 分析各分类支出模式
            val topCategories = analyzeTopCategories(transactions, categoryMap)
            
            // 分析支出趋势
            val spendingTrends = analyzeSpendingTrends(transactions)
            
            // 检测异常交易
            val unusualTransactions = detectUnusualTransactions(transactions, categoryMap)
            
            // 分析定期支出
            val regularExpenses = analyzeRegularExpenses(transactions, categoryMap)
            
            // 分析工作日vs周末支出
            val weekdayWeekendSpending = analyzeWeekdayWeekendSpending(transactions)
            
            val patternAnalysis = ExpensePatternAnalysis(
                topCategories = topCategories,
                spendingTrends = spendingTrends,
                unusualTransactions = unusualTransactions,
                regularExpenses = regularExpenses,
                weekdayVsWeekendSpending = weekdayWeekendSpending
            )
            
            _expensePatternAnalysis.value = patternAnalysis
            
        } catch (e: Exception) {
            android.util.Log.e("StatisticsViewModel", "支出模式分析失败", e)
        }
    }

    /**
     * 分析各分类支出模式
     */
    private fun analyzeTopCategories(transactions: List<Transaction>, categoryMap: Map<String, Category>): List<CategorySpendingPattern> {
        val expenseTransactions = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        val totalExpense = expenseTransactions.sumOf { it.amount }
        
        val categoryGroups = expenseTransactions.groupBy { it.categoryId }
        
        return categoryGroups.map { (categoryId, categoryTransactions) ->
            val category = categoryMap[categoryId]
            val totalAmount = categoryTransactions.sumOf { it.amount }
            val avgAmount = totalAmount / categoryTransactions.size
            val percentage = if (totalExpense > 0) (totalAmount / totalExpense) * 100 else 0.0
            
            // 分析最近12个月的趋势
            val monthlyAmounts = mutableListOf<Double>()
            val currentCalendar = Calendar.getInstance()
            
            for (i in 0 until 12) {
                val monthStart = Calendar.getInstance().apply {
                    timeInMillis = currentCalendar.timeInMillis
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val monthEnd = Calendar.getInstance().apply {
                    timeInMillis = currentCalendar.timeInMillis
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                
                val monthAmount = categoryTransactions
                    .filter { it.date >= monthStart && it.date <= monthEnd }
                    .sumOf { it.amount }
                
                monthlyAmounts.add(monthAmount)
                currentCalendar.add(Calendar.MONTH, -1)
            }
            
            // 计算趋势
            val trend = calculateSpendingTrend(monthlyAmounts.reversed())
            
            CategorySpendingPattern(
                categoryId = categoryId ?: "",
                categoryName = category?.name ?: "未分类",
                totalAmount = totalAmount,
                transactionCount = categoryTransactions.size,
                avgAmount = avgAmount,
                percentage = percentage,
                trend = trend,
                monthlyAmounts = monthlyAmounts.reversed()
            )
        }.sortedByDescending { it.totalAmount }
    }

    /**
     * 计算支出趋势类型
     */
    private fun calculateSpendingTrend(monthlyAmounts: List<Double>): SpendingTrendType {
        if (monthlyAmounts.size < 3) return SpendingTrendType.STABLE
        
        val recentThree = monthlyAmounts.takeLast(3)
        val previousThree = monthlyAmounts.dropLast(3).takeLast(3)
        
        if (previousThree.isEmpty()) return SpendingTrendType.STABLE
        
        val recentAvg = recentThree.average()
        val previousAvg = previousThree.average()
        
        val changePercentage = if (previousAvg > 0) {
            ((recentAvg - previousAvg) / previousAvg) * 100
        } else 0.0
        
        // 计算波动性
        val variance = monthlyAmounts.map { (it - monthlyAmounts.average()).let { diff -> diff * diff } }.average()
        val stdDev = kotlin.math.sqrt(variance)
        val coefficientOfVariation = if (monthlyAmounts.average() > 0) stdDev / monthlyAmounts.average() else 0.0
        
        return when {
            coefficientOfVariation > 0.5 -> SpendingTrendType.VOLATILE
            changePercentage > 10 -> SpendingTrendType.INCREASING
            changePercentage < -10 -> SpendingTrendType.DECREASING
            else -> SpendingTrendType.STABLE
        }
    }

    /**
     * 分析支出趋势
     */
    private fun analyzeSpendingTrends(transactions: List<Transaction>): List<SpendingTrend> {
        val expenseTransactions = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        val trends = mutableListOf<SpendingTrend>()
        
        val currentCalendar = Calendar.getInstance()
        var previousAmount = 0.0
        
        for (i in 0 until 6) { // 最近6个月
            val monthStart = Calendar.getInstance().apply {
                timeInMillis = currentCalendar.timeInMillis
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val monthEnd = Calendar.getInstance().apply {
                timeInMillis = currentCalendar.timeInMillis
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            
            val monthAmount = expenseTransactions
                .filter { it.date >= monthStart && it.date <= monthEnd }
                .sumOf { it.amount }
            
            val changeFromPrevious = monthAmount - previousAmount
            val changePercentage = if (previousAmount > 0) (changeFromPrevious / previousAmount) * 100 else 0.0
            
            val period = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(monthStart))
            
            trends.add(SpendingTrend(
                period = period,
                amount = monthAmount,
                changeFromPrevious = changeFromPrevious,
                changePercentage = changePercentage
            ))
            
            previousAmount = monthAmount
            currentCalendar.add(Calendar.MONTH, -1)
        }
        
        return trends.reversed()
    }

    /**
     * 检测异常交易
     */
    private fun detectUnusualTransactions(transactions: List<Transaction>, categoryMap: Map<String, Category>): List<UnusualTransaction> {
        val expenseTransactions = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        val unusualList = mutableListOf<UnusualTransaction>()
        
        // 按分类分组
        val categoryGroups = expenseTransactions.groupBy { it.categoryId }
        
        categoryGroups.forEach { (categoryId, categoryTransactions) ->
            val amounts = categoryTransactions.map { it.amount }
            if (amounts.size >= 3) {
                val mean = amounts.average()
                val variance = amounts.map { (it - mean) * (it - mean) }.average()
                val stdDev = kotlin.math.sqrt(variance)
                
                // 找出超过2个标准差的异常交易
                categoryTransactions.forEach { transaction ->
                    if (kotlin.math.abs(transaction.amount - mean) > 2 * stdDev && transaction.amount > mean * 1.5) {
                        val category = categoryMap[categoryId]
                        unusualList.add(UnusualTransaction(
                            transactionId = transaction.id,
                            amount = transaction.amount,
                            categoryName = category?.name ?: "未分类",
                            date = transaction.date,
                            reason = "金额异常大",
                            normalRange = Pair(mean - stdDev, mean + stdDev)
                        ))
                    }
                }
            }
        }
        
        return unusualList.sortedByDescending { it.amount }
    }

    /**
     * 分析定期支出
     */
    private fun analyzeRegularExpenses(transactions: List<Transaction>, categoryMap: Map<String, Category>): List<RegularExpense> {
        val expenseTransactions = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        val regularList = mutableListOf<RegularExpense>()
        
        // 按分类分组并分析频率
        val categoryGroups = expenseTransactions.groupBy { it.categoryId }
        
        categoryGroups.forEach { (categoryId, categoryTransactions) ->
            if (categoryTransactions.size >= 3) { // 至少3笔交易才算定期
                val category = categoryMap[categoryId]
                val amounts = categoryTransactions.map { it.amount }
                val avgAmount = amounts.average()
                val lastAmount = amounts.lastOrNull() ?: 0.0
                
                // 计算方差来衡量规律性
                val variance = amounts.map { (it - avgAmount) * (it - avgAmount) }.average()
                val coefficientOfVariation = if (avgAmount > 0) kotlin.math.sqrt(variance) / avgAmount else 1.0
                
                // 只有变异系数较小的才算定期支出
                if (coefficientOfVariation < 0.3) {
                    // 计算月频率
                    val sixMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis
                    val recentTransactions = categoryTransactions.filter { it.date >= sixMonthsAgo }
                    val frequency = (recentTransactions.size * 6.0 / 6).toInt() // 每月频率
                    
                    regularList.add(RegularExpense(
                        categoryName = category?.name ?: "未分类",
                        avgAmount = avgAmount,
                        frequency = frequency,
                        lastAmount = lastAmount,
                        variance = coefficientOfVariation
                    ))
                }
            }
        }
        
        return regularList.sortedByDescending { it.avgAmount }
    }

    /**
     * 分析工作日vs周末支出
     */
    private fun analyzeWeekdayWeekendSpending(transactions: List<Transaction>): WeekdayWeekendSpending {
        val expenseTransactions = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        
        var weekdayTotal = 0.0
        var weekendTotal = 0.0
        var weekdayCount = 0
        var weekendCount = 0
        
        expenseTransactions.forEach { transaction ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = transaction.date
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                weekendTotal += transaction.amount
                weekendCount++
            } else {
                weekdayTotal += transaction.amount
                weekdayCount++
            }
        }
        
        val weekdayAvgDaily = if (weekdayCount > 0) weekdayTotal / weekdayCount else 0.0
        val weekendAvgDaily = if (weekendCount > 0) weekendTotal / weekendCount else 0.0
        
        val preference = when {
            weekendAvgDaily > weekdayAvgDaily * 1.2 -> "周末"
            weekdayAvgDaily > weekendAvgDaily * 1.2 -> "工作日"
            else -> "平均"
        }
        
        return WeekdayWeekendSpending(
            weekdayAvgDaily = weekdayAvgDaily,
            weekendAvgDaily = weekendAvgDaily,
            weekdayTotal = weekdayTotal,
            weekendTotal = weekendTotal,
            preference = preference
        )
    }

    /**
     * 分析预算跟踪
     */
    private suspend fun analyzeBudgetTracking() {
        try {
            val currentTime = System.currentTimeMillis()
            val budgets = repository.getBudgetsByDateRange(currentTime - 30L * 24 * 60 * 60 * 1000, currentTime + 30L * 24 * 60 * 60 * 1000)
            val categories = repository.getFinancialCategories().first()
            val categoryMap = categories.associateBy { it.id }
            
            var safeBudgets = 0
            var warningBudgets = 0
            var overBudgets = 0
            var totalBudgetAmount = 0.0
            var totalSpentAmount = 0.0
            
            val budgetDetailsList = mutableListOf<BudgetAnalysis>()
            
            budgets.forEach { budget ->
                // 计算该预算期间的实际支出
                val actualSpent = calculateBudgetSpent(budget)
                val status = when {
                    actualSpent > budget.amount -> {
                        overBudgets++
                        "超支"
                    }
                    actualSpent > budget.amount * budget.alertThreshold -> {
                        warningBudgets++
                        "警告"
                    }
                    else -> {
                        safeBudgets++
                        "安全"
                    }
                }
                
                totalBudgetAmount += budget.amount
                totalSpentAmount += actualSpent
                
                val remainingAmount = budget.amount - actualSpent
                val spentPercentage = if (budget.amount > 0) (actualSpent / budget.amount) * 100 else 0.0
                val daysRemaining = budget.getRemainingDays()
                val dailyBudgetRemaining = if (daysRemaining > 0) remainingAmount / daysRemaining else 0.0
                val onTrack = actualSpent <= budget.amount * (1.0 - (daysRemaining.toDouble() / 30.0)) // 假设30天周期
                
                budgetDetailsList.add(BudgetAnalysis(
                    budgetId = budget.id,
                    budgetName = budget.name,
                    categoryName = categoryMap[budget.categoryId]?.name,
                    budgetAmount = budget.amount,
                    spentAmount = actualSpent,
                    remainingAmount = remainingAmount,
                    spentPercentage = spentPercentage,
                    status = status,
                    daysRemaining = daysRemaining,
                    dailyBudgetRemaining = dailyBudgetRemaining,
                    onTrack = onTrack
                ))
            }
            
            val overallProgress = if (totalBudgetAmount > 0) (totalSpentAmount / totalBudgetAmount) * 100 else 0.0
            
            val budgetTracking = BudgetTrackingStatus(
                totalBudgets = budgets.size,
                activeBudgets = budgets.count { it.isActive },
                overBudgets = overBudgets,
                warningBudgets = warningBudgets,
                safeBudgets = safeBudgets,
                totalBudgetAmount = totalBudgetAmount,
                totalSpentAmount = totalSpentAmount,
                overallProgress = overallProgress,
                budgetDetails = budgetDetailsList
            )
            
            _budgetTrackingStatus.value = budgetTracking
            
        } catch (e: Exception) {
            android.util.Log.e("StatisticsViewModel", "预算跟踪分析失败", e)
        }
    }

    /**
     * 计算预算实际支出
     */
    private suspend fun calculateBudgetSpent(budget: Budget): Double {
        val transactions = repository.getTransactionsByDateRange(budget.startDate, budget.endDate).first()
        
        return if (budget.categoryId != null) {
            // 特定分类预算
            transactions.filter { 
                it.type == Transaction.TransactionType.EXPENSE && it.categoryId == budget.categoryId 
            }.sumOf { it.amount }
        } else {
            // 总预算
            transactions.filter { 
                it.type == Transaction.TransactionType.EXPENSE 
            }.sumOf { it.amount }
        }
    }

    /**
     * 评估财务健康度
     */
    private suspend fun assessFinancialHealth() {
        try {
            // 获取最近3个月的数据进行评估
            val threeMonthsAgo = Calendar.getInstance().apply {
                add(Calendar.MONTH, -3)
            }.timeInMillis
            val now = System.currentTimeMillis()
            
            val transactions = repository.getTransactionsByDateRange(threeMonthsAgo, now).first()
            val budgets = repository.getBudgetsByDateRange(threeMonthsAgo, now + 30L * 24 * 60 * 60 * 1000)
            
            val factors = mutableListOf<HealthFactor>()
            var totalScore = 0.0
            var totalWeight = 0.0
            
            // 1. 收支平衡评估 (权重: 30%)
            val incomeExpenseBalance = assessIncomeExpenseBalance(transactions)
            factors.add(incomeExpenseBalance)
            totalScore += incomeExpenseBalance.score * incomeExpenseBalance.weight
            totalWeight += incomeExpenseBalance.weight
            
            // 2. 预算执行评估 (权重: 25%)
            val budgetCompliance = assessBudgetCompliance(budgets)
            factors.add(budgetCompliance)
            totalScore += budgetCompliance.score * budgetCompliance.weight
            totalWeight += budgetCompliance.weight
            
            // 3. 支出稳定性评估 (权重: 20%)
            val spendingStability = assessSpendingStability(transactions)
            factors.add(spendingStability)
            totalScore += spendingStability.score * spendingStability.weight
            totalWeight += spendingStability.weight
            
            // 4. 储蓄率评估 (权重: 15%)
            val savingsRate = assessSavingsRate(transactions)
            factors.add(savingsRate)
            totalScore += savingsRate.score * savingsRate.weight
            totalWeight += savingsRate.weight
            
            // 5. 支出多样性评估 (权重: 10%)
            val expenseDiversity = assessExpenseDiversity(transactions)
            factors.add(expenseDiversity)
            totalScore += expenseDiversity.score * expenseDiversity.weight
            totalWeight += expenseDiversity.weight
            
            // 计算总分
            val overallScore = if (totalWeight > 0) (totalScore / totalWeight).toInt().coerceIn(0, 100) else 0
            
            // 确定健康度等级
            val level = when (overallScore) {
                in 90..100 -> FinancialHealthLevel.EXCELLENT
                in 75..89 -> FinancialHealthLevel.GOOD
                in 60..74 -> FinancialHealthLevel.FAIR
                in 40..59 -> FinancialHealthLevel.POOR
                else -> FinancialHealthLevel.CRITICAL
            }
            
            // 生成建议和评价
            val recommendations = generateRecommendations(factors, level)
            val strengths = generateStrengths(factors)
            val concerns = generateConcerns(factors)
            
            val assessment = FinancialHealthAssessment(
                overallScore = overallScore,
                level = level,
                factors = factors,
                recommendations = recommendations,
                strengths = strengths,
                concerns = concerns
            )
            
            _financialHealthAssessment.value = assessment
            
        } catch (e: Exception) {
            android.util.Log.e("StatisticsViewModel", "财务健康度评估失败", e)
        }
    }

    /**
     * 评估收支平衡
     */
    private fun assessIncomeExpenseBalance(transactions: List<Transaction>): HealthFactor {
        val income = transactions.filter { it.type == Transaction.TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }.sumOf { it.amount }
        
        val balanceRatio = if (income > 0) expense / income else 1.0
        
        val score = when {
            balanceRatio <= 0.7 -> 100  // 支出占收入70%以下，优秀
            balanceRatio <= 0.8 -> 85   // 支出占收入80%以下，良好
            balanceRatio <= 0.9 -> 70   // 支出占收入90%以下，一般
            balanceRatio < 1.0 -> 55    // 支出接近收入，较差
            else -> 20                  // 支出超过收入，危险
        }
        
        val impact = if (score >= 70) "正面" else if (score >= 55) "中性" else "负面"
        val description = when {
            balanceRatio <= 0.7 -> "支出控制良好，有充足结余"
            balanceRatio <= 0.8 -> "支出管理较好，有适度结余"
            balanceRatio <= 0.9 -> "支出偏高，结余较少"
            balanceRatio < 1.0 -> "支出接近收入，财务紧张"
            else -> "支出超过收入，财务状况危险"
        }
        
        return HealthFactor(
            name = "收支平衡",
            score = score,
            weight = 0.3,
            description = description,
            impact = impact
        )
    }

    /**
     * 评估预算执行
     */
    private suspend fun assessBudgetCompliance(budgets: List<Budget>): HealthFactor {
        if (budgets.isEmpty()) {
            return HealthFactor(
                name = "预算执行",
                score = 50,
                weight = 0.25,
                description = "未设置预算，建议制定预算计划",
                impact = "中性"
            )
        }
        
        var compliantCount = 0
        var totalBudgets = 0
        
        for (budget in budgets) {
            if (budget.isActive && !budget.isExpired()) {
                totalBudgets++
                val actualSpent = calculateBudgetSpent(budget)
                if (actualSpent <= budget.amount) {
                    compliantCount++
                }
            }
        }
        
        val complianceRate = if (totalBudgets > 0) compliantCount.toDouble() / totalBudgets else 0.0
        
        val score = (complianceRate * 100).toInt()
        val impact = if (score >= 75) "正面" else if (score >= 50) "中性" else "负面"
        val description = when {
            complianceRate >= 0.9 -> "预算执行优秀，支出控制良好"
            complianceRate >= 0.7 -> "预算执行良好，大部分支出在控制范围内"
            complianceRate >= 0.5 -> "预算执行一般，部分预算超支"
            else -> "预算执行较差，多项预算超支"
        }
        
        return HealthFactor(
            name = "预算执行",
            score = score,
            weight = 0.25,
            description = description,
            impact = impact
        )
    }

    /**
     * 评估支出稳定性
     */
    private fun assessSpendingStability(transactions: List<Transaction>): HealthFactor {
        val expenseTransactions = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        
        if (expenseTransactions.size < 10) {
            return HealthFactor(
                name = "支出稳定性",
                score = 60,
                weight = 0.2,
                description = "交易数据较少，难以评估稳定性",
                impact = "中性"
            )
        }
        
        // 按周分组计算支出变异性
        val weeklyExpenses = mutableListOf<Double>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        
        for (i in 0 until 12) { // 最近12周
            val weekStart = Calendar.getInstance().apply {
                timeInMillis = calendar.timeInMillis
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val weekEnd = weekStart + 7 * 24 * 60 * 60 * 1000L - 1
            
            val weekExpense = expenseTransactions
                .filter { it.date >= weekStart && it.date <= weekEnd }
                .sumOf { it.amount }
            
            weeklyExpenses.add(weekExpense)
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
        }
        
        // 计算变异系数
        val mean = weeklyExpenses.average()
        val variance = weeklyExpenses.map { (it - mean) * (it - mean) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        val coefficientOfVariation = if (mean > 0) stdDev / mean else 0.0
        
        val score = when {
            coefficientOfVariation <= 0.2 -> 100  // 变异系数20%以下，非常稳定
            coefficientOfVariation <= 0.3 -> 85   // 变异系数30%以下，比较稳定
            coefficientOfVariation <= 0.4 -> 70   // 变异系数40%以下，一般稳定
            coefficientOfVariation <= 0.5 -> 55   // 变异系数50%以下，不够稳定
            else -> 30                             // 变异系数50%以上，很不稳定
        }
        
        val impact = if (score >= 70) "正面" else if (score >= 55) "中性" else "负面"
        val description = when {
            coefficientOfVariation <= 0.2 -> "支出非常稳定，财务管理良好"
            coefficientOfVariation <= 0.3 -> "支出比较稳定，财务可控"
            coefficientOfVariation <= 0.4 -> "支出稳定性一般，存在波动"
            coefficientOfVariation <= 0.5 -> "支出不够稳定，波动较大"
            else -> "支出很不稳定，建议加强预算管理"
        }
        
        return HealthFactor(
            name = "支出稳定性",
            score = score,
            weight = 0.2,
            description = description,
            impact = impact
        )
    }

    /**
     * 评估储蓄率
     */
    private fun assessSavingsRate(transactions: List<Transaction>): HealthFactor {
        val income = transactions.filter { it.type == Transaction.TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }.sumOf { it.amount }
        
        val savingsRate = if (income > 0) ((income - expense) / income) else 0.0
        
        val score = when {
            savingsRate >= 0.3 -> 100     // 储蓄率30%以上，优秀
            savingsRate >= 0.2 -> 85      // 储蓄率20%以上，良好
            savingsRate >= 0.1 -> 70      // 储蓄率10%以上，一般
            savingsRate >= 0.0 -> 50      // 储蓄率0%以上，较差
            else -> 20                    // 负储蓄，危险
        }
        
        val impact = if (score >= 70) "正面" else if (score >= 50) "中性" else "负面"
        val description = when {
            savingsRate >= 0.3 -> "储蓄率优秀，财务状况健康"
            savingsRate >= 0.2 -> "储蓄率良好，有一定积累"
            savingsRate >= 0.1 -> "储蓄率一般，建议增加储蓄"
            savingsRate >= 0.0 -> "储蓄率较低，财务紧张"
            else -> "支出超过收入，无法储蓄"
        }
        
        return HealthFactor(
            name = "储蓄率",
            score = score,
            weight = 0.15,
            description = description,
            impact = impact
        )
    }

    /**
     * 评估支出多样性
     */
    private fun assessExpenseDiversity(transactions: List<Transaction>): HealthFactor {
        val expenseTransactions = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        
        if (expenseTransactions.isEmpty()) {
            return HealthFactor(
                name = "支出多样性",
                score = 50,
                weight = 0.1,
                description = "无支出数据",
                impact = "中性"
            )
        }
        
        val categoryGroups = expenseTransactions.groupBy { it.categoryId }
        val categoryCount = categoryGroups.size
        val totalExpense = expenseTransactions.sumOf { it.amount }
        
        // 计算支出分布的均匀性（基尼系数的简化版本）
        val categoryAmounts = categoryGroups.values.map { it.sumOf { transaction -> transaction.amount } }
        val sortedAmounts = categoryAmounts.sortedDescending()
        
        // 计算最大分类占总支出的比例
        val maxCategoryRatio = if (totalExpense > 0) sortedAmounts.firstOrNull()?.div(totalExpense) ?: 0.0 else 0.0
        
        val score = when {
            categoryCount >= 8 && maxCategoryRatio <= 0.3 -> 100  // 8个以上分类，最大占比30%以下
            categoryCount >= 6 && maxCategoryRatio <= 0.4 -> 85   // 6个以上分类，最大占比40%以下
            categoryCount >= 4 && maxCategoryRatio <= 0.5 -> 70   // 4个以上分类，最大占比50%以下
            categoryCount >= 3 && maxCategoryRatio <= 0.6 -> 55   // 3个以上分类，最大占比60%以下
            else -> 30                                            // 分类较少或分布不均
        }
        
        val impact = if (score >= 70) "正面" else if (score >= 55) "中性" else "负面"
        val description = when {
            score >= 85 -> "支出分类多样，分布均匀"
            score >= 70 -> "支出分类较多，分布相对均匀"
            score >= 55 -> "支出分类一般，分布不够均匀"
            else -> "支出分类较少，建议平衡各类支出"
        }
        
        return HealthFactor(
            name = "支出多样性",
            score = score,
            weight = 0.1,
            description = description,
            impact = impact
        )
    }

    /**
     * 生成建议
     */
    private fun generateRecommendations(factors: List<HealthFactor>, level: FinancialHealthLevel): List<String> {
        val recommendations = mutableListOf<String>()
        
        factors.forEach { factor ->
            when {
                factor.impact == "负面" && factor.name == "收支平衡" -> {
                    recommendations.add("建议减少不必要的支出，增加收入来源")
                }
                factor.impact == "负面" && factor.name == "预算执行" -> {
                    recommendations.add("制定更合理的预算计划，严格控制支出")
                }
                factor.impact == "负面" && factor.name == "支出稳定性" -> {
                    recommendations.add("建立支出规律，避免大额突发消费")
                }
                factor.impact == "负面" && factor.name == "储蓄率" -> {
                    recommendations.add("设定储蓄目标，优先支付自己（强制储蓄）")
                }
                factor.impact == "负面" && factor.name == "支出多样性" -> {
                    recommendations.add("平衡各类支出，避免过度集中在某一类别")
                }
            }
        }
        
        // 根据整体健康度等级添加通用建议
        when (level) {
            FinancialHealthLevel.CRITICAL -> {
                recommendations.add("财务状况需要紧急改善，建议寻求专业理财建议")
            }
            FinancialHealthLevel.POOR -> {
                recommendations.add("制定详细的财务改善计划，逐步提升财务健康度")
            }
            FinancialHealthLevel.FAIR -> {
                recommendations.add("继续优化支出结构，提高储蓄比例")
            }
            FinancialHealthLevel.GOOD -> {
                recommendations.add("保持当前良好状态，可考虑投资增值")
            }
            FinancialHealthLevel.EXCELLENT -> {
                recommendations.add("财务管理优秀，可探索更多投资机会")
            }
        }
        
        return recommendations.distinct()
    }

    /**
     * 生成优势
     */
    private fun generateStrengths(factors: List<HealthFactor>): List<String> {
        return factors.filter { it.impact == "正面" }
            .map { "${it.name}：${it.description}" }
    }

    /**
     * 生成关注点
     */
    private fun generateConcerns(factors: List<HealthFactor>): List<String> {
        return factors.filter { it.impact == "负面" }
            .map { "${it.name}：${it.description}" }
    }

    /**
     * 刷新所有数据分析
     */
    fun refreshDataAnalysis() {
        startDataAnalysis()
    }

    /**
     * 获取调试信息
     */
    fun getDebugInfo() {
        viewModelScope.launch {
            android.util.Log.d("StatisticsViewModel", "=== 统计调试信息 ===")
            
            // 获取所有交易
            val allTransactions = repository.getTransactionsByDateRange(0, System.currentTimeMillis()).first()
            android.util.Log.d("StatisticsViewModel", "数据库中总交易数：${allTransactions.size}")
            
            if (allTransactions.isNotEmpty()) {
                val earliestDate = allTransactions.minByOrNull { it.date }?.date ?: 0
                val latestDate = allTransactions.maxByOrNull { it.date }?.date ?: 0
                android.util.Log.d("StatisticsViewModel", "最早交易：${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(earliestDate))}")
                android.util.Log.d("StatisticsViewModel", "最晚交易：${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(latestDate))}")
                
                // 按月份统计所有交易
                val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val transactionsByMonth = allTransactions.groupBy { transaction ->
                    monthFormat.format(Date(transaction.date))
                }
                
                android.util.Log.d("StatisticsViewModel", "按月份分布的交易数：")
                transactionsByMonth.forEach { (month, transactions) ->
                    val income = transactions.filter { it.type == Transaction.TransactionType.INCOME }.sumOf { it.amount }
                    val expense = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }.sumOf { it.amount }
                    android.util.Log.d("StatisticsViewModel", "  $month: ${transactions.size}笔交易, 收入=$income, 支出=$expense")
                }
            }
            
            // 查看当前选择的时间范围
            val (startTime, endTime) = getTimeRangeMillis(_currentTimeRange.value)
            android.util.Log.d("StatisticsViewModel", "当前选择的时间范围: ${_currentTimeRange.value}")
            android.util.Log.d("StatisticsViewModel", "  开始时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(startTime))}")
            android.util.Log.d("StatisticsViewModel", "  结束时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(endTime))}")
            
            // 查看当前月度数据状态
            android.util.Log.d("StatisticsViewModel", "当前月度数据：${_monthlyData.value.size} 个月")
            _monthlyData.value.forEach { monthData ->
                if (monthData.expense > 0 || monthData.income > 0) {
                    android.util.Log.d("StatisticsViewModel", "  ${monthData.period}: 支出=${monthData.expense}, 收入=${monthData.income}")
                }
            }
            
            // 查看当前分类数据状态
            android.util.Log.d("StatisticsViewModel", "当前分类数据：${_categoryData.value.size} 个分类")
            _categoryData.value.forEach { categoryData ->
                android.util.Log.d("StatisticsViewModel", "  ${categoryData.categoryName}: ${categoryData.amount}元 (${categoryData.transactionCount}笔)")
            }
        }
    }

    /**
     * 设置趋势类型
     */
    fun setTrendType(trendType: TrendType) {
        currentTrendType = trendType
        // 触发趋势数据重新计算
        loadStatistics()
    }
}

/**
 * 财务概览数据
 */
data class FinancialSummaryData(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val transactionCount: Int = 0
)

/**
 * 每日支出数据
 */
data class DailyExpenseData(
    val date: String,
    val amount: Double
)

/**
 * 分类支出数据
 */
data class CategoryExpenseData(
    val categoryName: String,
    val amount: Double,
    val color: String,
    val transactionCount: Int
)

/**
 * 月度统计数据
 */
data class MonthlyStatistic(
    val period: String,
    val income: Double,
    val expense: Double
)

/**
 * 月度时间范围数据
 */
data class MonthlyTimeRangeData(
    val startTime: Long,
    val endTime: Long,
    val monthCount: Int,
    val labels: List<String>
)

/**
 * ViewModel工厂
 */
class StatisticsViewModelFactory(private val repository: LifeLedgerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ==================== 数据分析相关数据类 ====================

/**
 * 月度收支统计
 */
data class MonthlyFinancialSummary(
    val year: Int,
    val month: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val transactionCount: Int,
    val avgDailyExpense: Double,
    val biggestExpense: Double,
    val biggestIncome: Double
)

/**
 * 年度收支统计
 */
data class YearlyFinancialSummary(
    val year: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val transactionCount: Int,
    val avgMonthlyExpense: Double,
    val avgMonthlyIncome: Double,
    val peakExpenseMonth: Int,
    val peakIncomeMonth: Int,
    val monthlySummaries: List<MonthlyFinancialSummary>
)

/**
 * 支出模式分析
 */
data class ExpensePatternAnalysis(
    val topCategories: List<CategorySpendingPattern>,
    val spendingTrends: List<SpendingTrend>,
    val unusualTransactions: List<UnusualTransaction>,
    val regularExpenses: List<RegularExpense>,
    val weekdayVsWeekendSpending: WeekdayWeekendSpending
)

/**
 * 分类支出模式
 */
data class CategorySpendingPattern(
    val categoryId: String,
    val categoryName: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val avgAmount: Double,
    val percentage: Double,
    val trend: SpendingTrendType,
    val monthlyAmounts: List<Double> // 最近12个月的支出
)

/**
 * 支出趋势
 */
data class SpendingTrend(
    val period: String, // "2024-01", "2024-02"
    val amount: Double,
    val changeFromPrevious: Double,
    val changePercentage: Double
)

/**
 * 异常交易
 */
data class UnusualTransaction(
    val transactionId: String,
    val amount: Double,
    val categoryName: String,
    val date: Long,
    val reason: String, // "金额异常大", "频率异常高"
    val normalRange: Pair<Double, Double> // 正常范围
)

/**
 * 定期支出
 */
data class RegularExpense(
    val categoryName: String,
    val avgAmount: Double,
    val frequency: Int, // 每月频率
    val lastAmount: Double,
    val variance: Double // 变异度
)

/**
 * 工作日vs周末支出
 */
data class WeekdayWeekendSpending(
    val weekdayAvgDaily: Double,
    val weekendAvgDaily: Double,
    val weekdayTotal: Double,
    val weekendTotal: Double,
    val preference: String // "工作日", "周末", "平均"
)

/**
 * 支出趋势类型
 */
enum class SpendingTrendType {
    INCREASING,  // 递增
    DECREASING,  // 递减
    STABLE,      // 稳定
    VOLATILE     // 波动
}

/**
 * 预算跟踪状态
 */
data class BudgetTrackingStatus(
    val totalBudgets: Int,
    val activeBudgets: Int,
    val overBudgets: Int,
    val warningBudgets: Int,
    val safeBudgets: Int,
    val totalBudgetAmount: Double,
    val totalSpentAmount: Double,
    val overallProgress: Double,
    val budgetDetails: List<BudgetAnalysis>
)

/**
 * 预算分析
 */
data class BudgetAnalysis(
    val budgetId: String,
    val budgetName: String,
    val categoryName: String?,
    val budgetAmount: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val spentPercentage: Double,
    val status: String,
    val daysRemaining: Long,
    val dailyBudgetRemaining: Double,
    val onTrack: Boolean
)

/**
 * 财务健康度评估
 */
data class FinancialHealthAssessment(
    val overallScore: Int, // 0-100分
    val level: FinancialHealthLevel,
    val factors: List<HealthFactor>,
    val recommendations: List<String>,
    val strengths: List<String>,
    val concerns: List<String>
)

/**
 * 财务健康度等级
 */
enum class FinancialHealthLevel(val displayName: String, val color: String) {
    EXCELLENT("优秀", "#4CAF50"),
    GOOD("良好", "#8BC34A"),
    FAIR("一般", "#FF9800"),
    POOR("较差", "#FF5722"),
    CRITICAL("危险", "#F44336")
}

/**
 * 健康度因素
 */
data class HealthFactor(
    val name: String,
    val score: Int, // 0-100
    val weight: Double, // 权重
    val description: String,
    val impact: String // "正面", "负面", "中性"
) 