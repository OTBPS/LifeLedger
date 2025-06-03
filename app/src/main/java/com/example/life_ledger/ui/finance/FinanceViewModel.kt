package com.example.life_ledger.ui.finance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.example.life_ledger.data.model.Transaction
import com.example.life_ledger.data.repository.TransactionRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * 财务模块ViewModel
 * 处理财务记录的CRUD操作和数据展示逻辑
 */
class FinanceViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // 所有交易记录
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    // 筛选后的交易记录
    private val _filteredTransactions = MutableLiveData<List<Transaction>>()
    val filteredTransactions: LiveData<List<Transaction>> = _filteredTransactions

    // 财务统计数据
    private val _financialSummary = MutableLiveData<FinancialSummary>()
    val financialSummary: LiveData<FinancialSummary> = _financialSummary

    // 操作结果状态
    private val _operationResult = MutableLiveData<OperationResult?>()
    val operationResult: LiveData<OperationResult?> = _operationResult

    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 当前筛选条件
    private var currentFilter = FilterOption.ALL
    private var currentDateRange = DateRange.ALL

    /**
     * 获取当前日期范围
     */
    fun getCurrentDateRange(): DateRange = currentDateRange

    init {
        loadTransactions()
    }

    /**
     * 加载所有交易记录
     */
    fun loadTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allTransactions = transactionRepository.getAllTransactions().first()
                _transactions.value = allTransactions
                applyFilter()
                calculateSummary(allTransactions)
            } catch (e: Exception) {
                _operationResult.value = OperationResult(
                    isSuccess = false,
                    message = "加载数据失败: ${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 添加新的交易记录
     */
    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                android.util.Log.d("FinanceViewModel", "开始添加交易记录：类型=${transaction.type}, 金额=${transaction.amount}, 分类ID=${transaction.categoryId}")
                
                val result = transactionRepository.insertTransaction(transaction)
                android.util.Log.d("FinanceViewModel", "交易记录插入成功，ID=$result")
                
                _operationResult.value = OperationResult(
                    isSuccess = true,
                    message = "记录添加成功"
                )
                
                loadTransactions() // 重新加载数据
                android.util.Log.d("FinanceViewModel", "数据重新加载完成")
                
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "添加交易记录失败", e)
                _operationResult.value = OperationResult(
                    isSuccess = false,
                    message = "添加失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 更新交易记录
     */
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                transactionRepository.updateTransaction(transaction)
                _operationResult.value = OperationResult(
                    isSuccess = true,
                    message = "记录更新成功"
                )
                loadTransactions() // 重新加载数据
            } catch (e: Exception) {
                _operationResult.value = OperationResult(
                    isSuccess = false,
                    message = "更新失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 删除交易记录
     */
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransaction(transaction)
                _operationResult.value = OperationResult(
                    isSuccess = true,
                    message = "记录删除成功"
                )
                loadTransactions() // 重新加载数据
            } catch (e: Exception) {
                _operationResult.value = OperationResult(
                    isSuccess = false,
                    message = "删除失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 根据ID获取交易记录
     */
    suspend fun getTransactionById(id: String): Transaction? {
        return try {
            transactionRepository.getTransactionById(id)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 设置筛选条件
     */
    fun setFilter(filter: FilterOption) {
        currentFilter = filter
        applyFilter()
    }

    /**
     * 设置日期范围
     */
    fun setDateRange(dateRange: DateRange) {
        currentDateRange = dateRange
        applyFilter()
    }

    /**
     * 应用当前筛选条件
     */
    private fun applyFilter() {
        val allTransactions = _transactions.value ?: return
        
        android.util.Log.d("FinanceViewModel", "开始应用筛选：总交易数=${allTransactions.size}, 筛选器=$currentFilter, 日期范围=$currentDateRange")
        
        var filtered = when (currentFilter) {
            FilterOption.ALL -> allTransactions
            FilterOption.INCOME -> allTransactions.filter { it.type == Transaction.TransactionType.INCOME }
            FilterOption.EXPENSE -> allTransactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        }
        
        android.util.Log.d("FinanceViewModel", "类型筛选后：交易数=${filtered.size}")

        // 应用日期范围筛选
        val (startDate, endDate) = getDateRangeMillis(currentDateRange)
        android.util.Log.d("FinanceViewModel", "时间范围：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(startDate))} 到 ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(endDate))}")
        
        filtered = filtered.filter { transaction ->
            val inRange = transaction.date >= startDate && transaction.date <= endDate
            if (!inRange) {
                android.util.Log.d("FinanceViewModel", "过滤掉交易：日期=${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(transaction.date))}, 类型=${transaction.type}, 金额=${transaction.amount}")
            }
            inRange
        }
        
        android.util.Log.d("FinanceViewModel", "日期筛选后：交易数=${filtered.size}")

        // 按日期降序排列
        filtered = filtered.sortedByDescending { it.date }

        // 统计收入和支出数量
        val incomeCount = filtered.count { it.type == Transaction.TransactionType.INCOME }
        val expenseCount = filtered.count { it.type == Transaction.TransactionType.EXPENSE }
        android.util.Log.d("FinanceViewModel", "最终结果：总数=${filtered.size}, 收入=${incomeCount}笔, 支出=${expenseCount}笔")

        _filteredTransactions.value = filtered
        calculateSummary(filtered)
    }

    /**
     * 计算财务统计数据
     */
    private fun calculateSummary(transactions: List<Transaction>) {
        val income = transactions
            .filter { it.type == Transaction.TransactionType.INCOME }
            .sumOf { it.amount }
        
        val expense = transactions
            .filter { it.type == Transaction.TransactionType.EXPENSE }
            .sumOf { it.amount }

        val balance = income - expense

        _financialSummary.value = FinancialSummary(
            totalIncome = income,
            totalExpense = expense,
            balance = balance,
            transactionCount = transactions.size
        )
    }

    /**
     * 根据日期范围获取时间戳范围
     */
    private fun getDateRangeMillis(dateRange: DateRange): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        when (dateRange) {
            DateRange.TODAY -> {
                // 今天的开始时间
                val startOfDay = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                // 今天的结束时间
                val endOfDay = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                
                return Pair(startOfDay, endOfDay)
            }
            DateRange.THIS_WEEK -> {
                // 本周开始时间
                val startOfWeek = calendar.apply {
                    set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                // 本周结束时间
                val endOfWeek = calendar.apply {
                    set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    add(Calendar.WEEK_OF_YEAR, 1)
                    add(Calendar.MILLISECOND, -1)
                }.timeInMillis
                
                return Pair(startOfWeek, endOfWeek)
            }
            DateRange.THIS_MONTH -> {
                // 本月开始时间
                val startOfMonth = calendar.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                // 本月结束时间
                val endOfMonth = calendar.apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                
                return Pair(startOfMonth, endOfMonth)
            }
            DateRange.THIS_YEAR -> {
                // 今年开始时间
                val startOfYear = calendar.apply {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                // 今年结束时间
                val endOfYear = calendar.apply {
                    set(Calendar.MONTH, Calendar.DECEMBER)
                    set(Calendar.DAY_OF_MONTH, 31)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                
                return Pair(startOfYear, endOfYear)
            }
            DateRange.ALL -> {
                // 所有时间：从最早到最晚
                return Pair(0L, Long.MAX_VALUE)
            }
        }
    }

    /**
     * 获取分组的交易记录（按日期分组）
     */
    fun getGroupedTransactions(): LiveData<List<TransactionGroup>> {
        val groupedLiveData = MutableLiveData<List<TransactionGroup>>()
        
        filteredTransactions.observeForever { transactions ->
            if (transactions != null) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val grouped = transactions
                    .groupBy { dateFormat.format(Date(it.date)) }
                    .map { (date, dayTransactions) ->
                        val income = dayTransactions
                            .filter { it.type == Transaction.TransactionType.INCOME }
                            .sumOf { it.amount }
                        val expense = dayTransactions
                            .filter { it.type == Transaction.TransactionType.EXPENSE }
                            .sumOf { it.amount }
                        
                        TransactionGroup(
                            date = date,
                            transactions = dayTransactions.sortedByDescending { it.date },
                            totalIncome = income,
                            totalExpense = expense
                        )
                    }
                    .sortedByDescending { it.date }
                
                groupedLiveData.value = grouped
            }
        }
        
        return groupedLiveData
    }

    /**
     * 搜索交易记录
     */
    fun searchTransactions(query: String) {
        val allTransactions = _transactions.value ?: return
        
        if (query.isBlank()) {
            applyFilter()
            return
        }

        val searchResults = allTransactions.filter { transaction ->
            transaction.title.contains(query, ignoreCase = true) ||
            (transaction.description?.contains(query, ignoreCase = true) == true) ||
            transaction.getTagsList().any { it.contains(query, ignoreCase = true) }
        }.sortedByDescending { it.date }

        _filteredTransactions.value = searchResults
        calculateSummary(searchResults)
    }

    /**
     * 清除操作结果
     */
    fun clearOperationResult() {
        _operationResult.value = null
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadTransactions()
    }
}

/**
 * 财务统计数据
 */
data class FinancialSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val transactionCount: Int
)

/**
 * 筛选选项
 */
enum class FilterOption {
    ALL,     // 全部
    INCOME,  // 收入
    EXPENSE  // 支出
}

/**
 * 日期范围选项
 */
enum class DateRange {
    TODAY,      // 今天
    THIS_WEEK,  // 本周
    THIS_MONTH, // 本月
    THIS_YEAR,  // 今年
    ALL         // 全部
} 