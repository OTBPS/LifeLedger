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
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.graphics.Color

/**
 * Statistics page ViewModel
 * Handles statistical data retrieval and calculation
 */
class StatisticsViewModel(private val repository: LifeLedgerRepository) : ViewModel() {

    // Time range enumeration
    enum class TimeRange {
        THIS_WEEK,    // This week
        THIS_MONTH,   // This month  
        THIS_YEAR     // This year
    }

    // Monthly statistics time range enumeration
    enum class MonthlyTimeRange {
        LAST_7_DAYS,       // Last 7 days
        THIS_YEAR,         // This year
        LAST_YEAR,         // Last year
        LAST_24_MONTHS     // Last 24 months
    }

    // Currently selected time range
    private val _currentTimeRange = MutableStateFlow(TimeRange.THIS_MONTH)
    val currentTimeRange: StateFlow<TimeRange> = _currentTimeRange.asStateFlow()

    // Currently selected monthly statistics time range
    private var currentMonthlyTimeRange = MonthlyTimeRange.LAST_7_DAYS

    // Financial statistics data
    private val _financialSummary = MutableStateFlow(FinancialSummaryData())
    val financialSummary: StateFlow<FinancialSummaryData> = _financialSummary.asStateFlow()

    // Expense trend data (date -> amount)
    private val _expenseTrendData = MutableStateFlow<List<DailyExpenseData>>(emptyList())
    val expenseTrendData: StateFlow<List<DailyExpenseData>> = _expenseTrendData.asStateFlow()

    // Income trend data (date -> amount)
    private val _incomeTrendData = MutableStateFlow<List<DailyExpenseData>>(emptyList())
    val incomeTrendData: StateFlow<List<DailyExpenseData>> = _incomeTrendData.asStateFlow()

    // Category statistics data
    private val _categoryData = MutableStateFlow<List<CategoryExpenseData>>(emptyList())
    val categoryData: StateFlow<List<CategoryExpenseData>> = _categoryData.asStateFlow()

    // Monthly income and expense data
    private val _monthlyData = MutableStateFlow<List<MonthlyStatistic>>(emptyList())
    val monthlyData: StateFlow<List<MonthlyStatistic>> = _monthlyData.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Operation result
    private val _operationResult = MutableSharedFlow<OperationResult>()
    val operationResult: SharedFlow<OperationResult> = _operationResult.asSharedFlow()

    // Whether in empty state
    private val _isEmpty = MutableStateFlow(false)
    val isEmpty: StateFlow<Boolean> = _isEmpty.asStateFlow()

    // ==================== Data analysis related data flows ====================

    // Monthly income and expense statistics
    private val _monthlyFinancialSummary = MutableStateFlow<List<MonthlyFinancialSummary>>(emptyList())
    val monthlyFinancialSummary: StateFlow<List<MonthlyFinancialSummary>> = _monthlyFinancialSummary.asStateFlow()

    // Annual income and expense statistics
    private val _yearlyFinancialSummary = MutableStateFlow<List<YearlyFinancialSummary>>(emptyList())
    val yearlyFinancialSummary: StateFlow<List<YearlyFinancialSummary>> = _yearlyFinancialSummary.asStateFlow()

    // Expense pattern analysis
    private val _expensePatternAnalysis = MutableStateFlow<ExpensePatternAnalysis?>(null)
    val expensePatternAnalysis: StateFlow<ExpensePatternAnalysis?> = _expensePatternAnalysis.asStateFlow()

    // Budget tracking status
    private val _budgetTrackingStatus = MutableStateFlow<BudgetTrackingStatus?>(null)
    val budgetTrackingStatus: StateFlow<BudgetTrackingStatus?> = _budgetTrackingStatus.asStateFlow()

    // Financial health assessment
    private val _financialHealthAssessment = MutableStateFlow<FinancialHealthAssessment?>(null)
    val financialHealthAssessment: StateFlow<FinancialHealthAssessment?> = _financialHealthAssessment.asStateFlow()

    // Trend type
    enum class TrendType {
        EXPENSE, INCOME
    }
    
    private var currentTrendType = TrendType.EXPENSE

    init {
        loadStatistics()
        calculateMonthlyStatistics()
        // Start data analysis
        startDataAnalysis()
    }

    /**
     * Set time range
     */
    fun setTimeRange(timeRange: TimeRange) {
        _currentTimeRange.value = timeRange
        loadStatistics()
    }

    /**
     * Set monthly statistics time range
     */
    fun setMonthlyTimeRange(timeRange: MonthlyTimeRange) {
        currentMonthlyTimeRange = timeRange
        calculateMonthlyStatistics()
    }

    /**
     * Refresh all statistical data
     */
    fun refresh() {
        android.util.Log.d("StatisticsViewModel", "Manual refresh of statistical data")
        loadStatistics()
    }

    /**
     * Load statistical data
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val (startTime, endTime) = getTimeRangeMillis(_currentTimeRange.value)
                
                // Get all transactions within the time range
                val transactions = repository.getTransactionsByDateRange(startTime, endTime).first()
                
                android.util.Log.d("StatisticsViewModel", "Load statistics: obtained ${transactions.size} transactions within time range")
                
                if (transactions.isEmpty()) {
                    _isEmpty.value = true
                    _financialSummary.value = FinancialSummaryData()
                    _expenseTrendData.value = emptyList()
                    _incomeTrendData.value = emptyList()
                    _categoryData.value = emptyList()
                    // Monthly statistics still calculated as it uses a fixed 12-month range
                    calculateMonthlyStatistics()
                } else {
                    _isEmpty.value = false
                    
                    // Calculate financial overview
                    calculateFinancialSummary(transactions)
                    
                    // Calculate expense trend
                    calculateExpenseTrend(transactions, startTime, endTime)
                    
                    // Calculate income trend
                    calculateIncomeTrend(transactions, startTime, endTime)
                    
                    // Calculate category statistics
                    calculateCategoryStatistics(transactions)
                    
                    // Calculate monthly statistics (independent time range)
                    calculateMonthlyStatistics()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("StatisticsViewModel", "Failed to load statistical data", e)
                _operationResult.emit(OperationResult(false, "Failed to load statistical data: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Calculate financial overview
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
     * Calculate expense trend
     */
    private fun calculateExpenseTrend(transactions: List<Transaction>, startTime: Long, endTime: Long) {
        val expenses = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        
        // Group by date to calculate daily expenses
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val dailyExpenses = mutableMapOf<String, Double>()
        
        // Generate all dates within the date range
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startTime
        
        while (calendar.timeInMillis <= endTime) {
            val dateKey = dateFormat.format(calendar.time)
            dailyExpenses[dateKey] = 0.0
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // Fill in actual expense data
        expenses.forEach { transaction ->
            val dateKey = dateFormat.format(Date(transaction.date))
            dailyExpenses[dateKey] = dailyExpenses.getOrDefault(dateKey, 0.0) + transaction.amount
        }
        
        // Convert to chart data
        val trendData = dailyExpenses.entries
            .sortedBy { it.key }
            .map { (date, amount) ->
                DailyExpenseData(date, amount)
            }
        
        _expenseTrendData.value = trendData
    }

    /**
     * Calculate income trend
     */
    private fun calculateIncomeTrend(transactions: List<Transaction>, startTime: Long, endTime: Long) {
        val incomes = transactions.filter { it.type == Transaction.TransactionType.INCOME }
        
        // Group by date to calculate daily income
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val dailyIncomes = mutableMapOf<String, Double>()
        
        // Generate all dates within the date range
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startTime
        
        while (calendar.timeInMillis <= endTime) {
            val dateKey = dateFormat.format(calendar.time)
            dailyIncomes[dateKey] = 0.0
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // Fill in actual income data
        incomes.forEach { transaction ->
            val dateKey = dateFormat.format(Date(transaction.date))
            dailyIncomes[dateKey] = dailyIncomes.getOrDefault(dateKey, 0.0) + transaction.amount
        }
        
        // Convert to chart data
        val trendData = dailyIncomes.entries
            .sortedBy { it.key }
            .map { (date, amount) ->
                DailyExpenseData(date, amount)
            }
        
        _incomeTrendData.value = trendData
    }

    /**
     * Calculate category statistics
     */
    private fun calculateCategoryStatistics(transactions: List<Transaction>) {
        viewModelScope.launch {
            try {
                // Get all categories
                val categories = repository.getFinancialCategories().first()
                val categoryMap = categories.associateBy { it.id }
                
                // Group expenses by category for statistics
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
                                categoryName = "Uncategorized",
                                amount = transactions.sumOf { it.amount },
                                color = "#607D8B",
                                transactionCount = transactions.size
                            )
                        }
                    }
                    .sortedByDescending { it.amount }
                
                _categoryData.value = expenseByCategory
                
            } catch (e: Exception) {
                _operationResult.emit(OperationResult(false, "Failed to calculate category statistics: ${e.message}"))
            }
        }
    }

    /**
     * Calculate monthly statistics
     */
    private fun calculateMonthlyStatistics() {
        viewModelScope.launch {
            try {
                val timeRange = currentMonthlyTimeRange
                val (startTime, endTime, monthCount, monthsList) = getMonthlyTimeRangeData(timeRange)
                
                android.util.Log.d("StatisticsViewModel", "Monthly statistics: time range $timeRange")
                android.util.Log.d("StatisticsViewModel", "Monthly statistics: query time range ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(startTime))} to ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(endTime))}")
                android.util.Log.d("StatisticsViewModel", "Monthly statistics: month list $monthsList")
                
                val transactions = repository.getTransactionsByDateRange(startTime, endTime).first()
                android.util.Log.d("StatisticsViewModel", "Monthly statistics: obtained ${transactions.size} transactions")
                
                // If there's transaction data, print some examples
                if (transactions.isNotEmpty()) {
                    android.util.Log.d("StatisticsViewModel", "Transaction examples:")
                    transactions.take(5).forEach { transaction ->
                        android.util.Log.d("StatisticsViewModel", "  Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(transaction.date))}, Amount: ${transaction.amount}, Type: ${transaction.type}")
                    }
                }
                
                // Refactored monthly statistics logic (supports both date and month modes)
                val isDaily = timeRange == MonthlyTimeRange.LAST_7_DAYS
                val keyFormat = if (isDaily) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) 
                               else SimpleDateFormat("yyyy-MM", Locale.getDefault())
                
                val keyToLabelMap = mutableMapOf<String, String>()
                val monthlyMap = mutableMapOf<String, Pair<Double, Double>>()
                
                // Initialize time period data
                for (key in monthsList) {
                    monthlyMap[key] = Pair(0.0, 0.0)
                    
                    // Generate friendly display labels for dates
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
                
                // Process transaction data
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
                
                // Build result list
                val result = monthsList.map { key ->
                    val (income, expense) = monthlyMap[key] ?: Pair(0.0, 0.0)
                    val label = keyToLabelMap[key] ?: key
                    MonthlyStatistic(label, income, expense)
                }
                
                val dataMonthsCount = result.count { it.expense > 0 || it.income > 0 }
                android.util.Log.d("StatisticsViewModel", "Monthly statistics completed: generated ${result.size} months of data, months with data: $dataMonthsCount")
                
                _monthlyData.value = result
                
            } catch (e: Exception) {
                android.util.Log.e("StatisticsViewModel", "Failed to calculate monthly statistics", e)
                _operationResult.emit(OperationResult(false, "Failed to calculate monthly statistics: ${e.message}"))
            }
        }
    }

    /**
     * Get monthly statistics time range data
     */
    private fun getMonthlyTimeRangeData(timeRange: MonthlyTimeRange): MonthlyTimeRangeData {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)
        
        return when (timeRange) {
            MonthlyTimeRange.LAST_7_DAYS -> {
                // Last 7 days
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
                // Current year from January to current month
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
                val monthCount = currentMonth + 1 // +1 because Calendar.MONTH is 0-based
                val monthsList = generateMonthsList(startCalendar, monthCount)
                MonthlyTimeRangeData(startTime, endTime, monthCount, monthsList)
            }
            
            MonthlyTimeRange.LAST_YEAR -> {
                // Full last year
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
                // Last 24 months
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
     * Generate month list
     */
    private fun generateMonthsList(startCalendar: Calendar, monthCount: Int): List<String> {
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.ENGLISH)
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
     * Generate day list
     */
    private fun generateDaysList(startCalendar: Calendar, dayCount: Int): List<String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
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
     * Get time range in milliseconds
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
     * Get category income data
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
                            categoryName = "Uncategorized",
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
     * Force refresh monthly data
     */
    fun forceRefreshMonthlyData() {
        android.util.Log.d("StatisticsViewModel", "=== Force refresh monthly statistics ===")
        calculateMonthlyStatistics()
    }

    /**
     * Test monthly statistics calculation (for debugging)
     */
    fun testMonthlyStatistics() {
        android.util.Log.d("StatisticsViewModel", "=== Start testing monthly statistics calculation ===")
        calculateMonthlyStatistics()
    }

    // ==================== Data Analysis Functions ====================

    /**
     * Start data analysis
     */
    private fun startDataAnalysis() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Execute various analyses in parallel
                launch { calculateMonthlyFinancialSummary() }
                launch { calculateYearlyFinancialSummary() }
                launch { analyzeExpensePatterns() }
                launch { analyzeBudgetTracking() }
                launch { assessFinancialHealth() }
                
            } catch (e: Exception) {
                android.util.Log.e("StatisticsViewModel", "Failed to start data analysis", e)
                _operationResult.emit(OperationResult(false, "Failed to start data analysis: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Calculate monthly financial summary
     */
    private suspend fun calculateMonthlyFinancialSummary() {
        try {
            val currentTime = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = currentTime
            
            val monthlyList = mutableListOf<MonthlyFinancialSummary>()
            
            // Calculate statistics for the last 12 months
            for (i in 0 until 12) {
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
                
                // Get start and end time for this month
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
                
                // Get transaction data for this month
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
                
                // Move to previous month
                calendar.add(Calendar.MONTH, -1)
            }
            
            _monthlyFinancialSummary.value = monthlyList.reversed() // Sort by time in ascending order
            
        } catch (e: Exception) {
            android.util.Log.e("StatisticsViewModel", "Failed to calculate monthly statistics", e)
        }
    }

    /**
     * Calculate yearly financial summary
     */
    private suspend fun calculateYearlyFinancialSummary() {
        try {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val yearlyList = mutableListOf<YearlyFinancialSummary>()
            
            // Calculate data for the last 3 years
            for (i in 0 until 3) {
                val year = currentYear - i
                
                // Get start and end time for this year
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
                
                // Get transaction data for this year
                val transactions = repository.getTransactionsByDateRange(yearStart, yearEnd).first()
                
                val income = transactions.filter { it.type == Transaction.TransactionType.INCOME }.sumOf { it.amount }
                val expense = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }.sumOf { it.amount }
                
                // Calculate monthly data to find peak months
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
                
                // Generate monthly summaries for this year
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
            android.util.Log.e("StatisticsViewModel", "Failed to calculate yearly statistics", e)
        }
    }

    /**
     * Analyze expense patterns
     */
    private suspend fun analyzeExpensePatterns() {
        try {
            // Get data from the last 6 months for analysis
            val sixMonthsAgo = Calendar.getInstance().apply {
                add(Calendar.MONTH, -6)
            }.timeInMillis
            val now = System.currentTimeMillis()
            
            val transactions = repository.getTransactionsByDateRange(sixMonthsAgo, now).first()
            val categories = repository.getFinancialCategories().first()
            val categoryMap = categories.associateBy { it.id }
            
            // Analyze expense patterns by category
            val topCategories = analyzeTopCategories(transactions, categoryMap)
            
            // Analyze spending trends
            val spendingTrends = analyzeSpendingTrends(transactions)
            
            // Detect unusual transactions
            val unusualTransactions = detectUnusualTransactions(transactions, categoryMap)
            
            // Analyze regular expenses
            val regularExpenses = analyzeRegularExpenses(transactions, categoryMap)
            
            // Analyze weekday vs weekend spending
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
            android.util.Log.e("StatisticsViewModel", "Failed to analyze expense patterns", e)
        }
    }

    /**
     * Analyze expense patterns by category
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
            
            // Analyze trends for the last 12 months
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
            
            // Calculate trend
            val trend = calculateSpendingTrend(monthlyAmounts.reversed())
            
            CategorySpendingPattern(
                categoryId = categoryId ?: "",
                categoryName = category?.name ?: "Uncategorized",
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
     * Calculate spending trend type
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
        
        // Calculate volatility
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
     * Analyze spending trends
     */
    private fun analyzeSpendingTrends(transactions: List<Transaction>): List<SpendingTrend> {
        val expenseTransactions = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        val trends = mutableListOf<SpendingTrend>()
        
        val currentCalendar = Calendar.getInstance()
        var previousAmount = 0.0
        
        for (i in 0 until 6) { // Last 6 months
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
     * Detect unusual transactions
     */
    private fun detectUnusualTransactions(transactions: List<Transaction>, categoryMap: Map<String, Category>): List<UnusualTransaction> {
        val expenseTransactions = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        val unusualList = mutableListOf<UnusualTransaction>()
        
        // Group transactions by category
        val categoryGroups = expenseTransactions.groupBy { it.categoryId }
        
        categoryGroups.forEach { (categoryId, categoryTransactions) ->
            val amounts = categoryTransactions.map { it.amount }
            if (amounts.size >= 3) {
                val mean = amounts.average()
                val variance = amounts.map { (it - mean) * (it - mean) }.average()
                val stdDev = kotlin.math.sqrt(variance)
                
                // Find transactions that exceed 2 standard deviations
                categoryTransactions.forEach { transaction ->
                    if (kotlin.math.abs(transaction.amount - mean) > 2 * stdDev && transaction.amount > mean * 1.5) {
                        val category = categoryMap[categoryId]
                        unusualList.add(UnusualTransaction(
                            transactionId = transaction.id,
                            amount = transaction.amount,
                            categoryName = category?.name ?: "Uncategorized",
                            date = transaction.date,
                            reason = "Amount unusually high",
                            normalRange = Pair(mean - stdDev, mean + stdDev)
                        ))
                    }
                }
            }
        }
        
        return unusualList.sortedByDescending { it.amount }
    }

    /**
     * Analyze regular expenses
     */
    private fun analyzeRegularExpenses(transactions: List<Transaction>, categoryMap: Map<String, Category>): List<RegularExpense> {
        val expenseTransactions = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        val regularList = mutableListOf<RegularExpense>()
        
        // Group transactions by category and analyze frequency
        val categoryGroups = expenseTransactions.groupBy { it.categoryId }
        
        categoryGroups.forEach { (categoryId, categoryTransactions) ->
            if (categoryTransactions.size >= 3) { // At least 3 transactions are required for regular
                val category = categoryMap[categoryId]
                val amounts = categoryTransactions.map { it.amount }
                val avgAmount = amounts.average()
                val lastAmount = amounts.lastOrNull() ?: 0.0
                
                // Calculate variance to measure regularity
                val variance = amounts.map { (it - avgAmount) * (it - avgAmount) }.average()
                val coefficientOfVariation = if (avgAmount > 0) kotlin.math.sqrt(variance) / avgAmount else 1.0
                
                // Only those with a smaller coefficient of variation are considered regular expenses
                if (coefficientOfVariation < 0.3) {
                    // Calculate monthly frequency
                    val sixMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis
                    val recentTransactions = categoryTransactions.filter { it.date >= sixMonthsAgo }
                    val frequency = (recentTransactions.size * 6.0 / 6).toInt() // Monthly frequency
                    
                    regularList.add(RegularExpense(
                        categoryName = category?.name ?: "Uncategorized",
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
     * Analyze weekday vs weekend spending
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
            weekendAvgDaily > weekdayAvgDaily * 1.2 -> "weekend"
            weekdayAvgDaily > weekendAvgDaily * 1.2 -> "weekday"
            else -> "balanced"
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
     * Analyze budget tracking
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
                // Calculate actual spending for the budget period
                val actualSpent = calculateBudgetSpent(budget)
                val status = when {
                    actualSpent > budget.amount -> {
                        overBudgets++
                        "Over Budget"
                    }
                    actualSpent > budget.amount * budget.alertThreshold -> {
                        warningBudgets++
                        "Warning"
                    }
                    else -> {
                        safeBudgets++
                        "Normal"
                    }
                }
                
                totalBudgetAmount += budget.amount
                totalSpentAmount += actualSpent
                
                val remainingAmount = budget.amount - actualSpent
                val spentPercentage = if (budget.amount > 0) (actualSpent / budget.amount) * 100 else 0.0
                val daysRemaining = budget.getRemainingDays()
                val dailyBudgetRemaining = if (daysRemaining > 0) remainingAmount / daysRemaining else 0.0
                val onTrack = actualSpent <= budget.amount * (1.0 - (daysRemaining.toDouble() / 30.0)) // Assuming 30-day cycle
                
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
            android.util.Log.e("StatisticsViewModel", "Failed to analyze budget tracking", e)
        }
    }

    /**
     * Calculate budget actual spending
     */
    private suspend fun calculateBudgetSpent(budget: Budget): Double {
        val transactions = repository.getTransactionsByDateRange(budget.startDate, budget.endDate).first()
        
        return if (budget.categoryId != null) {
            // Specific category budget
            transactions.filter { 
                it.type == Transaction.TransactionType.EXPENSE && it.categoryId == budget.categoryId 
            }.sumOf { it.amount }
        } else {
            // Total budget
            transactions.filter { 
                it.type == Transaction.TransactionType.EXPENSE 
            }.sumOf { it.amount }
        }
    }

    /**
     * Assess financial health
     */
    private suspend fun assessFinancialHealth() {
        try {
            // Get data from the last 3 months for assessment
            val threeMonthsAgo = Calendar.getInstance().apply {
                add(Calendar.MONTH, -3)
            }.timeInMillis
            val now = System.currentTimeMillis()
            
            val transactions = repository.getTransactionsByDateRange(threeMonthsAgo, now).first()
            val budgets = repository.getBudgetsByDateRange(threeMonthsAgo, now + 30L * 24 * 60 * 60 * 1000)
            
            val factors = mutableListOf<HealthFactor>()
            var totalScore = 0.0
            var totalWeight = 0.0
            
            // 1. Income-expense balance assessment (weight: 30%)
            val incomeExpenseBalance = assessIncomeExpenseBalance(transactions)
            factors.add(incomeExpenseBalance)
            totalScore += incomeExpenseBalance.score * incomeExpenseBalance.weight
            totalWeight += incomeExpenseBalance.weight
            
            // 2. Budget compliance assessment (weight: 25%)
            val budgetCompliance = assessBudgetCompliance(budgets)
            factors.add(budgetCompliance)
            totalScore += budgetCompliance.score * budgetCompliance.weight
            totalWeight += budgetCompliance.weight
            
            // 3. Spending stability assessment (weight: 20%)
            val spendingStability = assessSpendingStability(transactions)
            factors.add(spendingStability)
            totalScore += spendingStability.score * spendingStability.weight
            totalWeight += spendingStability.weight
            
            // 4. Savings rate assessment (weight: 15%)
            val savingsRate = assessSavingsRate(transactions)
            factors.add(savingsRate)
            totalScore += savingsRate.score * savingsRate.weight
            totalWeight += savingsRate.weight
            
            // 5. Expense diversity assessment (weight: 10%)
            val expenseDiversity = assessExpenseDiversity(transactions)
            factors.add(expenseDiversity)
            totalScore += expenseDiversity.score * expenseDiversity.weight
            totalWeight += expenseDiversity.weight
            
            // Calculate total score
            val overallScore = if (totalWeight > 0) (totalScore / totalWeight).toInt().coerceIn(0, 100) else 0
            
            // Determine health level
            val level = when (overallScore) {
                in 90..100 -> FinancialHealthLevel.EXCELLENT
                in 75..89 -> FinancialHealthLevel.GOOD
                in 60..74 -> FinancialHealthLevel.FAIR
                in 40..59 -> FinancialHealthLevel.POOR
                else -> FinancialHealthLevel.CRITICAL
            }
            
            // Generate recommendations and assessments
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
            android.util.Log.e("StatisticsViewModel", "Failed to assess financial health", e)
        }
    }

    /**
     * Assess income-expense balance
     */
    private fun assessIncomeExpenseBalance(transactions: List<Transaction>): HealthFactor {
        val income = transactions.filter { it.type == Transaction.TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }.sumOf { it.amount }
        
        val balanceRatio = if (income > 0) expense / income else 1.0
        
        val score = when {
            balanceRatio <= 0.7 -> 100  // Expenses under 70% of income, excellent
            balanceRatio <= 0.8 -> 85   // Expenses under 80% of income, good
            balanceRatio <= 0.9 -> 70   // Expenses under 90% of income, fair
            balanceRatio < 1.0 -> 55    // Expenses close to income, poor
            else -> 20                  // Expenses exceed income, critical
        }
        
        val impact = if (score >= 70) "Positive" else if (score >= 55) "Neutral" else "Negative"
        val description = when {
            balanceRatio <= 0.7 -> "Good expense control with sufficient surplus"
            balanceRatio <= 0.8 -> "Good expense management with moderate surplus"
            balanceRatio <= 0.9 -> "High expenses with limited surplus"
            balanceRatio < 1.0 -> "Expenses close to income, financial stress"
            else -> "Expenses exceed income, dangerous financial situation"
        }
        
        return HealthFactor(
            name = "Income-Expense Balance",
            score = score,
            weight = 0.3,
            description = description,
            impact = impact
        )
    }

    /**
     * Assess budget compliance
     */
    private suspend fun assessBudgetCompliance(budgets: List<Budget>): HealthFactor {
        if (budgets.isEmpty()) {
            return HealthFactor(
                name = "Budget Execution",
                score = 50,
                weight = 0.25,
                description = "No budget set, suggest creating a budget plan",
                impact = "Neutral"
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
        val impact = if (score >= 75) "Positive" else if (score >= 50) "Neutral" else "Negative"
        val description = when {
            complianceRate >= 0.9 -> "Excellent budget execution, good expense control"
            complianceRate >= 0.7 -> "Good budget execution, most expenses under control"
            complianceRate >= 0.5 -> "Average budget execution, some expenses exceeded"
            else -> "Poor budget execution, multiple budget overspending"
        }
        
        return HealthFactor(
            name = "Budget Execution",
            score = score,
            weight = 0.25,
            description = description,
            impact = impact
        )
    }

    /**
     * Assess spending stability
     */
    private fun assessSpendingStability(transactions: List<Transaction>): HealthFactor {
        val expenseTransactions = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        
        if (expenseTransactions.size < 10) {
            return HealthFactor(
                name = "Spending Stability",
                score = 60,
                weight = 0.2,
                description = "Few transaction data, difficult to assess stability",
                impact = "Neutral"
            )
        }
        
        // Calculate weekly variability by week
        val weeklyExpenses = mutableListOf<Double>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        
        for (i in 0 until 12) { // Last 12 weeks
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
        
        // Calculate coefficient of variation
        val mean = weeklyExpenses.average()
        val variance = weeklyExpenses.map { (it - mean) * (it - mean) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        val coefficientOfVariation = if (mean > 0) stdDev / mean else 0.0
        
        val score = when {
            coefficientOfVariation <= 0.2 -> 100  // Variability below 20%, very stable
            coefficientOfVariation <= 0.3 -> 85   // Variability below 30%, relatively stable
            coefficientOfVariation <= 0.4 -> 70   // Variability below 40%, average stability
            coefficientOfVariation <= 0.5 -> 55   // Variability below 50%, not stable enough
            else -> 30                             // Variability above 50%, very unstable
        }
        
        val impact = if (score >= 70) "Positive" else if (score >= 55) "Neutral" else "Negative"
        val description = when {
            coefficientOfVariation <= 0.2 -> "Spending very stable, good financial management"
            coefficientOfVariation <= 0.3 -> "Spending relatively stable, financial control"
            coefficientOfVariation <= 0.4 -> "Spending stability average, some fluctuation"
            coefficientOfVariation <= 0.5 -> "Spending not stable enough, significant fluctuation"
            else -> "Spending very unstable, suggest strengthening budget management"
        }
        
        return HealthFactor(
            name = "Spending Stability",
            score = score,
            weight = 0.2,
            description = description,
            impact = impact
        )
    }

    /**
     * Assess savings rate
     */
    private fun assessSavingsRate(transactions: List<Transaction>): HealthFactor {
        val income = transactions.filter { it.type == Transaction.TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }.sumOf { it.amount }
        
        val savingsRate = if (income > 0) ((income - expense) / income) else 0.0
        
        val score = when {
            savingsRate >= 0.3 -> 100     // Savings rate above 30%, excellent
            savingsRate >= 0.2 -> 85      // Savings rate above 20%, good
            savingsRate >= 0.1 -> 70      // Savings rate above 10%, average
            savingsRate >= 0.0 -> 50      // Savings rate above 0%, poor
            else -> 20                    // Negative savings, dangerous
        }
        
        val impact = if (score >= 70) "Positive" else if (score >= 50) "Neutral" else "Negative"
        val description = when {
            savingsRate >= 0.3 -> "Excellent savings rate, healthy financial situation"
            savingsRate >= 0.2 -> "Good savings rate, some accumulation"
            savingsRate >= 0.1 -> "Average savings rate, suggest increasing savings"
            savingsRate >= 0.0 -> "Low savings rate, financial stress"
            else -> "Expenses exceed income, unable to save"
        }
        
        return HealthFactor(
            name = "Savings Rate",
            score = score,
            weight = 0.15,
            description = description,
            impact = impact
        )
    }

    /**
     * Assess expense diversity
     */
    private fun assessExpenseDiversity(transactions: List<Transaction>): HealthFactor {
        val expenseTransactions = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        
        if (expenseTransactions.isEmpty()) {
            return HealthFactor(
                name = "Expense Diversity",
                score = 50,
                weight = 0.1,
                description = "No expense data",
                impact = "Neutral"
            )
        }
        
        val categoryGroups = expenseTransactions.groupBy { it.categoryId }
        val categoryCount = categoryGroups.size
        val totalExpense = expenseTransactions.sumOf { it.amount }
        
        // Calculate evenness of expense distribution (simplified version of Gini coefficient)
        val categoryAmounts = categoryGroups.values.map { it.sumOf { transaction -> transaction.amount } }
        val sortedAmounts = categoryAmounts.sortedDescending()
        
        // Calculate proportion of largest category in total expenses
        val maxCategoryRatio = if (totalExpense > 0) sortedAmounts.firstOrNull()?.div(totalExpense) ?: 0.0 else 0.0
        
        val score = when {
            categoryCount >= 8 && maxCategoryRatio <= 0.3 -> 100  // 8 or more categories, max ratio 30% or less
            categoryCount >= 6 && maxCategoryRatio <= 0.4 -> 85   // 6 or more categories, max ratio 40% or less
            categoryCount >= 4 && maxCategoryRatio <= 0.5 -> 70   // 4 or more categories, max ratio 50% or less
            categoryCount >= 3 && maxCategoryRatio <= 0.6 -> 55   // 3 or more categories, max ratio 60% or less
            else -> 30                                            // Fewer categories or uneven distribution
        }
        
        val impact = if (score >= 70) "Positive" else if (score >= 55) "Neutral" else "Negative"
        val description = when {
            score >= 85 -> "Expense categories diverse, evenly distributed"
            score >= 70 -> "Expense categories more, relatively evenly distributed"
            score >= 55 -> "Expense categories average, not evenly distributed"
            else -> "Expense categories less, suggest balancing various expenses"
        }
        
        return HealthFactor(
            name = "Expense Diversity",
            score = score,
            weight = 0.1,
            description = description,
            impact = impact
        )
    }

    /**
     * Generate recommendations
     */
    private fun generateRecommendations(factors: List<HealthFactor>, level: FinancialHealthLevel): List<String> {
        val recommendations = mutableListOf<String>()
        
        factors.forEach { factor ->
            when {
                factor.impact == "Negative" && factor.name == "Income-Expense Balance" -> {
                    recommendations.add("Suggest reducing unnecessary expenses and increasing income sources")
                }
                factor.impact == "Negative" && factor.name == "Budget Execution" -> {
                    recommendations.add("Create a more reasonable budget plan and strictly control expenses")
                }
                factor.impact == "Negative" && factor.name == "Spending Stability" -> {
                    recommendations.add("Establish spending patterns and avoid large unexpected purchases")
                }
                factor.impact == "Negative" && factor.name == "Savings Rate" -> {
                    recommendations.add("Set savings goals and pay yourself first (forced savings)")
                }
                factor.impact == "Negative" && factor.name == "Expense Diversity" -> {
                    recommendations.add("Balance various types of expenses, avoid over-concentration in one category")
                }
            }
        }
        
        // Add general recommendations based on overall health level
        when (level) {
            FinancialHealthLevel.CRITICAL -> {
                recommendations.add("Financial condition needs urgent improvement, suggest seeking professional financial advice")
            }
            FinancialHealthLevel.POOR -> {
                recommendations.add("Create a detailed financial improvement plan to gradually enhance financial health")
            }
            FinancialHealthLevel.FAIR -> {
                recommendations.add("Continue optimizing expense structure and increase savings ratio")
            }
            FinancialHealthLevel.GOOD -> {
                recommendations.add("Maintain current good condition, consider investment opportunities")
            }
            FinancialHealthLevel.EXCELLENT -> {
                recommendations.add("Excellent financial management, explore more investment opportunities")
            }
        }
        
        return recommendations.distinct()
    }

    /**
     * Generate strengths
     */
    private fun generateStrengths(factors: List<HealthFactor>): List<String> {
        return factors.filter { it.impact == "Positive" }
            .map { "${it.name}: ${it.description}" }
    }

    /**
     * Generate concerns
     */
    private fun generateConcerns(factors: List<HealthFactor>): List<String> {
        return factors.filter { it.impact == "Negative" }
            .map { "${it.name}: ${it.description}" }
    }

    /**
     * Refresh all data analysis
     */
    fun refreshDataAnalysis() {
        startDataAnalysis()
    }

    /**
     * Print debugging information
     */
    fun debugInfo() {
        viewModelScope.launch {
            android.util.Log.d("StatisticsViewModel", "=== StatisticsViewModel debugging information ===")
            
            // Check original data state
            val allTransactions = repository.getAllTransactions().first()
            android.util.Log.d("StatisticsViewModel", "Total number of transactions in database: ${allTransactions.size}")
            
            val incomeTransactions = allTransactions.filter { it.type == Transaction.TransactionType.INCOME }
            val expenseTransactions = allTransactions.filter { it.type == Transaction.TransactionType.EXPENSE }
            
            android.util.Log.d("StatisticsViewModel", "Number of income transactions: ${incomeTransactions.size}")
            android.util.Log.d("StatisticsViewModel", "Number of expense transactions: ${expenseTransactions.size}")
            
            // Check recent transactions
            val recentTransactions = allTransactions.sortedByDescending { it.date }.take(5)
            android.util.Log.d("StatisticsViewModel", "Recent 5 transactions:")
            recentTransactions.forEach { transaction ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date(transaction.date))
                android.util.Log.d("StatisticsViewModel", "  ${transaction.type}: ${transaction.amount}元, Date: $dateStr, Category: ${transaction.title}")
            }
            
            // Check current time range selection
            val (startTime, endTime) = getTimeRangeMillis(_currentTimeRange.value)
            android.util.Log.d("StatisticsViewModel", "Current selected time range: ${_currentTimeRange.value}")
            android.util.Log.d("StatisticsViewModel", "  Start time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date(startTime))}")
            android.util.Log.d("StatisticsViewModel", "  End time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date(endTime))}")
            
            // Check current monthly data state
            android.util.Log.d("StatisticsViewModel", "Current monthly data: ${_monthlyData.value.size} months")
            _monthlyData.value.forEach { monthData ->
                if (monthData.expense > 0 || monthData.income > 0) {
                    android.util.Log.d("StatisticsViewModel", "  ${monthData.period}: Expense=${monthData.expense}, Income=${monthData.income}")
                }
            }
            
            // Check current category data state
            android.util.Log.d("StatisticsViewModel", "Current category data: ${_categoryData.value.size} categories")
            _categoryData.value.forEach { categoryData ->
                android.util.Log.d("StatisticsViewModel", "  ${categoryData.categoryName}: ${categoryData.amount}元 (${categoryData.transactionCount} transactions)")
            }
        }
    }
}

/**
 * Financial overview data
 */
data class FinancialSummaryData(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val transactionCount: Int = 0
)

/**
 * Daily expense data
 */
data class DailyExpenseData(
    val date: String,
    val amount: Double
)

/**
 * Category expense data
 */
data class CategoryExpenseData(
    val categoryName: String,
    val amount: Double,
    val color: String,
    val transactionCount: Int
)

/**
 * Monthly statistics data
 */
data class MonthlyStatistic(
    val period: String,
    val income: Double,
    val expense: Double
)

/**
 * Monthly time range data
 */
data class MonthlyTimeRangeData(
    val startTime: Long,
    val endTime: Long,
    val monthCount: Int,
    val labels: List<String>
)

/**
 * ViewModel factory
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

// ==================== Data analysis related data classes ====================

/**
 * Monthly income and expense statistics
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
 * Annual income and expense statistics
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
 * Expense pattern analysis
 */
data class ExpensePatternAnalysis(
    val topCategories: List<CategorySpendingPattern>,
    val spendingTrends: List<SpendingTrend>,
    val unusualTransactions: List<UnusualTransaction>,
    val regularExpenses: List<RegularExpense>,
    val weekdayVsWeekendSpending: WeekdayWeekendSpending
)

/**
 * Category expense pattern
 */
data class CategorySpendingPattern(
    val categoryId: String,
    val categoryName: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val avgAmount: Double,
    val percentage: Double,
    val trend: SpendingTrendType,
    val monthlyAmounts: List<Double> // Expenses for the last 12 months
)

/**
 * Spending trend
 */
data class SpendingTrend(
    val period: String, // "2024-01", "2024-02"
    val amount: Double,
    val changeFromPrevious: Double,
    val changePercentage: Double
)

/**
 * Unusual transaction
 */
data class UnusualTransaction(
    val transactionId: String,
    val amount: Double,
    val categoryName: String,
    val date: Long,
    val reason: String, // "Amount unusually high", "Frequency unusually high"
    val normalRange: Pair<Double, Double> // Normal range
)

/**
 * Regular expense
 */
data class RegularExpense(
    val categoryName: String,
    val avgAmount: Double,
    val frequency: Int, // Monthly frequency
    val lastAmount: Double,
    val variance: Double // Variability
)

/**
 * Weekday vs weekend spending
 */
data class WeekdayWeekendSpending(
    val weekdayAvgDaily: Double,
    val weekendAvgDaily: Double,
    val weekdayTotal: Double,
    val weekendTotal: Double,
    val preference: String // "Weekday", "Weekend", "Average"
)

/**
 * Spending trend type
 */
enum class SpendingTrendType {
    INCREASING,  // Increasing
    DECREASING,  // Decreasing
    STABLE,      // Stable
    VOLATILE     // Volatile
}

/**
 * Budget tracking status
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
 * Budget analysis
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
 * Financial health assessment
 */
data class FinancialHealthAssessment(
    val overallScore: Int, // 0-100 points
    val level: FinancialHealthLevel,
    val factors: List<HealthFactor>,
    val recommendations: List<String>,
    val strengths: List<String>,
    val concerns: List<String>
)

/**
 * Financial health level
 */
enum class FinancialHealthLevel(val displayName: String, val color: String) {
    EXCELLENT("Excellent", "#4CAF50"),
    GOOD("Good", "#8BC34A"),
    FAIR("Fair", "#FF9800"),
    POOR("Poor", "#FF5722"),
    CRITICAL("Critical", "#F44336")
}

/**
 * Health factor
 */
data class HealthFactor(
    val name: String,
    val score: Int, // 0-100
    val weight: Double, // Weight
    val description: String,
    val impact: String // "Positive", "Negative", "Neutral"
) 