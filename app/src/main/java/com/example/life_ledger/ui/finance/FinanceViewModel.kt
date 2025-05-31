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
    private var currentDateRange = DateRange.THIS_MONTH

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
                transactionRepository.insertTransaction(transaction)
                _operationResult.value = OperationResult(
                    isSuccess = true,
                    message = "记录添加成功"
                )
                loadTransactions() // 重新加载数据
            } catch (e: Exception) {
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
        
        var filtered = when (currentFilter) {
            FilterOption.ALL -> allTransactions
            FilterOption.INCOME -> allTransactions.filter { it.type == Transaction.TransactionType.INCOME }
            FilterOption.EXPENSE -> allTransactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        }

        // 应用日期范围筛选
        val (startDate, endDate) = getDateRangeMillis(currentDateRange)
        filtered = filtered.filter { transaction ->
            transaction.date >= startDate && transaction.date <= endDate
        }

        // 按日期降序排列
        filtered = filtered.sortedByDescending { it.date }

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
        val endDate = calendar.timeInMillis

        when (dateRange) {
            DateRange.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return Pair(calendar.timeInMillis, endDate)
            }
            DateRange.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return Pair(calendar.timeInMillis, endDate)
            }
            DateRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return Pair(calendar.timeInMillis, endDate)
            }
            DateRange.THIS_YEAR -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return Pair(calendar.timeInMillis, endDate)
            }
            DateRange.ALL -> {
                return Pair(0L, endDate)
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