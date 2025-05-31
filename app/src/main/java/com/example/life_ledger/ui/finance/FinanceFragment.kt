package com.example.life_ledger.ui.finance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.life_ledger.R
import com.example.life_ledger.data.database.AppDatabase
import com.example.life_ledger.data.model.Transaction
import com.example.life_ledger.data.repository.TransactionRepository
import com.example.life_ledger.databinding.FragmentFinanceBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.util.*

/**
 * 财务管理主页面
 * 显示财务概览、交易记录列表和快捷操作
 */
class FinanceFragment : Fragment() {

    private var _binding: FragmentFinanceBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FinanceViewModel
    private lateinit var transactionAdapter: TransactionListAdapter
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_finance, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
        setupSearchView()
        setupFilterChips()
        setupObservers()
    }

    /**
     * 初始化ViewModel
     */
    private fun setupViewModel() {
        try {
            val database = AppDatabase.getDatabase(requireContext())
            val repository = TransactionRepository(database.transactionDao())
            val factory = FinanceViewModelFactory(repository)
            viewModel = ViewModelProvider(this, factory)[FinanceViewModel::class.java]
        } catch (e: Exception) {
            // 如果ViewModel初始化失败，显示错误消息
            Snackbar.make(
                binding.root,
                "初始化失败: ${e.message}",
                Snackbar.LENGTH_LONG
            ).show()
            
            // 记录错误日志
            android.util.Log.e("FinanceFragment", "ViewModel initialization failed", e)
        }
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        transactionAdapter = TransactionListAdapter(
            onItemClick = { transaction ->
                // 导航到编辑页面
                val action = FinanceFragmentDirections
                    .actionFinanceFragmentToAddEditTransactionFragment(transaction.id)
                findNavController().navigate(action)
            },
            onItemLongClick = { transaction ->
                // 显示删除确认对话框
                showDeleteConfirmDialog(transaction)
            }
        )
        
        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    /**
     * 设置点击监听器
     */
    private fun setupClickListeners() {
        binding.apply {
            // 添加收入快捷按钮 - 使用快速记录对话框
            buttonQuickIncome.setOnClickListener {
                showQuickTransactionDialog(Transaction.TransactionType.INCOME)
            }

            // 添加支出快捷按钮 - 使用快速记录对话框
            buttonQuickExpense.setOnClickListener {
                showQuickTransactionDialog(Transaction.TransactionType.EXPENSE)
            }

            // 浮动操作按钮 - 导航到完整的添加页面
            fabAdd.setOnClickListener {
                val action = FinanceFragmentDirections
                    .actionFinanceFragmentToAddEditTransactionFragment("")
                findNavController().navigate(action)
            }

            // 下拉刷新
            swipeRefreshLayout.setOnRefreshListener {
                viewModel.refresh()
            }

            // 日期范围选择
            textViewDateRange.setOnClickListener {
                showDateRangeDialog()
            }
        }
    }

    /**
     * 设置搜索功能
     */
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchTransactions(newText ?: "")
                return true
            }
        })

        // 搜索提示
        binding.searchView.queryHint = "搜索标题、备注或标签"
    }

    /**
     * 设置筛选标签
     */
    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipAll -> FilterOption.ALL
                R.id.chipIncome -> FilterOption.INCOME
                R.id.chipExpense -> FilterOption.EXPENSE
                else -> FilterOption.ALL
            }
            viewModel.setFilter(filter)
        }
    }

    /**
     * 设置数据观察者
     */
    private fun setupObservers() {
        // 观察交易记录列表
        viewModel.filteredTransactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions)
            updateEmptyState(transactions.isEmpty())
        }

        // 观察财务统计数据
        viewModel.financialSummary.observe(viewLifecycleOwner) { summary ->
            updateFinancialSummary(summary)
        }

        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        // 观察操作结果
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                val message = if (it.isSuccess) it.message else "操作失败: ${it.message}"
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                viewModel.clearOperationResult()
            }
        }
    }

    /**
     * 更新财务统计显示
     */
    private fun updateFinancialSummary(summary: FinancialSummary) {
        binding.apply {
            textViewBalance.text = numberFormat.format(summary.balance)
            textViewIncome.text = numberFormat.format(summary.totalIncome)
            textViewExpense.text = numberFormat.format(summary.totalExpense)
            
            // 根据余额设置颜色
            val balanceColor = if (summary.balance >= 0) {
                requireContext().getColor(R.color.md_theme_success)
            } else {
                requireContext().getColor(R.color.md_theme_error)
            }
            textViewBalance.setTextColor(balanceColor)
            
            // 更新交易数量
            textViewTransactionCount.text = "共${summary.transactionCount}笔交易"
        }
    }

    /**
     * 更新空状态显示
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            if (isEmpty) {
                recyclerViewTransactions.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
            } else {
                recyclerViewTransactions.visibility = View.VISIBLE
                layoutEmpty.visibility = View.GONE
            }
        }
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("确认删除")
            .setMessage("确定要删除这条${if (transaction.type == Transaction.TransactionType.INCOME) "收入" else "支出"}记录吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteTransaction(transaction)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示快速交易对话框
     */
    private fun showQuickTransactionDialog(type: Transaction.TransactionType) {
        val dialog = QuickTransactionDialog.newInstance(type)
        dialog.setOnTransactionAddedListener { transaction ->
            viewModel.addTransaction(transaction)
        }
        dialog.show(parentFragmentManager, "QuickTransactionDialog")
    }

    /**
     * 显示日期范围选择对话框
     */
    private fun showDateRangeDialog() {
        val options = arrayOf("今天", "本周", "本月", "今年", "全部")
        val currentSelection = when (viewModel.getCurrentDateRange()) {
            DateRange.TODAY -> 0
            DateRange.THIS_WEEK -> 1
            DateRange.THIS_MONTH -> 2
            DateRange.THIS_YEAR -> 3
            DateRange.ALL -> 4
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择日期范围")
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                val dateRange = when (which) {
                    0 -> DateRange.TODAY
                    1 -> DateRange.THIS_WEEK
                    2 -> DateRange.THIS_MONTH
                    3 -> DateRange.THIS_YEAR
                    else -> DateRange.ALL
                }
                viewModel.setDateRange(dateRange)
                binding.textViewDateRange.text = options[which]
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        // 在Fragment可见时刷新数据，确保从AddEditTransactionFragment返回时能看到最新数据
        if (::viewModel.isInitialized) {
            viewModel.refresh()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * 创建FinanceFragment实例
         */
        @JvmStatic
        fun newInstance() = FinanceFragment()
    }
}

/**
 * FinanceViewModel工厂类
 */
class FinanceViewModelFactory(
    private val repository: TransactionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * 排序选项枚举
 */
enum class SortOrder {
    AMOUNT_ASC,    // 金额升序
    AMOUNT_DESC,   // 金额降序
    DATE_ASC,      // 日期升序
    DATE_DESC      // 日期降序
} 