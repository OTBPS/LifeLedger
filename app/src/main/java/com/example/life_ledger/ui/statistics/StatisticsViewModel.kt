package com.example.life_ledger.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.life_ledger.data.model.Transaction
import com.example.life_ledger.data.model.Category
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
        LAST_12_MONTHS,    // 最近12个月
        THIS_YEAR,         // 今年
        LAST_YEAR,         // 去年
        LAST_24_MONTHS     // 最近24个月
    }

    // 当前选择的时间范围
    private val _currentTimeRange = MutableStateFlow(TimeRange.THIS_MONTH)
    val currentTimeRange: StateFlow<TimeRange> = _currentTimeRange.asStateFlow()

    // 当前选择的月度统计时间范围
    private val _currentMonthlyTimeRange = MutableStateFlow(MonthlyTimeRange.LAST_12_MONTHS)
    val currentMonthlyTimeRange: StateFlow<MonthlyTimeRange> = _currentMonthlyTimeRange.asStateFlow()

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

    // 月度统计数据
    private val _monthlyData = MutableStateFlow<List<MonthlyData>>(emptyList())
    val monthlyData: StateFlow<List<MonthlyData>> = _monthlyData.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 操作结果
    private val _operationResult = MutableSharedFlow<OperationResult>()
    val operationResult: SharedFlow<OperationResult> = _operationResult.asSharedFlow()

    // 是否为空状态
    private val _isEmpty = MutableStateFlow(false)
    val isEmpty: StateFlow<Boolean> = _isEmpty.asStateFlow()

    init {
        loadStatistics()
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
        _currentMonthlyTimeRange.value = timeRange
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
                val timeRange = _currentMonthlyTimeRange.value
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
                
                // 按月分组统计
                val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val monthlyExpenses = mutableMapOf<String, Double>()
                val monthlyIncomes = mutableMapOf<String, Double>()
                
                // 初始化所有月份数据为0
                monthsList.forEach { monthKey ->
                    monthlyExpenses[monthKey] = 0.0
                    monthlyIncomes[monthKey] = 0.0
                }
                
                // 填入实际数据
                transactions.forEach { transaction ->
                    val monthKey = monthFormat.format(Date(transaction.date))
                    
                    // 检查月份是否在我们的列表中
                    if (monthsList.contains(monthKey)) {
                        android.util.Log.d("StatisticsViewModel", "处理交易：月份 $monthKey, 金额 ${transaction.amount}, 类型 ${transaction.type}")
                        
                        when (transaction.type) {
                            Transaction.TransactionType.EXPENSE -> {
                                monthlyExpenses[monthKey] = monthlyExpenses.getOrDefault(monthKey, 0.0) + transaction.amount
                            }
                            Transaction.TransactionType.INCOME -> {
                                monthlyIncomes[monthKey] = monthlyIncomes.getOrDefault(monthKey, 0.0) + transaction.amount
                            }
                        }
                    } else {
                        android.util.Log.d("StatisticsViewModel", "跳过交易（不在时间范围内）：月份 $monthKey, 金额 ${transaction.amount}")
                    }
                }
                
                // 转换为图表数据，按照时间顺序排列
                val monthData = monthsList.map { monthKey ->
                    val expense = monthlyExpenses[monthKey] ?: 0.0
                    val income = monthlyIncomes[monthKey] ?: 0.0
                    
                    android.util.Log.d("StatisticsViewModel", "月份 $monthKey: 支出 $expense, 收入 $income")
                    
                    MonthlyData(
                        month = monthKey,
                        expense = expense,
                        income = income
                    )
                }
                
                val dataMonthsCount = monthData.count { it.expense > 0 || it.income > 0 }
                android.util.Log.d("StatisticsViewModel", "月度统计完成：生成 ${monthData.size} 个月的数据，有数据的月份：$dataMonthsCount")
                
                _monthlyData.value = monthData
                
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
            MonthlyTimeRange.LAST_12_MONTHS -> {
                // 最近12个月
                val endTime = System.currentTimeMillis()
                val startCalendar = Calendar.getInstance()
                startCalendar.set(Calendar.YEAR, currentYear)
                startCalendar.set(Calendar.MONTH, currentMonth)
                startCalendar.add(Calendar.MONTH, -11)
                startCalendar.set(Calendar.DAY_OF_MONTH, 1)
                startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startCalendar.set(Calendar.MINUTE, 0)
                startCalendar.set(Calendar.SECOND, 0)
                startCalendar.set(Calendar.MILLISECOND, 0)
                val startTime = startCalendar.timeInMillis
                
                val monthsList = generateMonthsList(startCalendar, 12)
                MonthlyTimeRangeData(startTime, endTime, 12, monthsList)
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
                    android.util.Log.d("StatisticsViewModel", "  ${monthData.month}: 支出=${monthData.expense}, 收入=${monthData.income}")
                }
            }
            
            // 查看当前分类数据状态
            android.util.Log.d("StatisticsViewModel", "当前分类数据：${_categoryData.value.size} 个分类")
            _categoryData.value.forEach { categoryData ->
                android.util.Log.d("StatisticsViewModel", "  ${categoryData.categoryName}: ${categoryData.amount}元 (${categoryData.transactionCount}笔)")
            }
        }
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
 * 月度数据
 */
data class MonthlyData(
    val month: String,
    val expense: Double,
    val income: Double
)

/**
 * 月度时间范围数据
 */
data class MonthlyTimeRangeData(
    val startTime: Long,
    val endTime: Long,
    val monthCount: Int,
    val monthsList: List<String>
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