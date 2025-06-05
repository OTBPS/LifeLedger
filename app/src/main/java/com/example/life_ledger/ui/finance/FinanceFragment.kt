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
 * Finance Management Main Page
 * Displays financial overview, transaction record list and quick actions
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
     * Initialize ViewModel
     */
    private fun setupViewModel() {
        try {
            val database = AppDatabase.getDatabase(requireContext())
            val repository = TransactionRepository(database.transactionDao(), database.budgetDao())
            val factory = FinanceViewModelFactory(repository)
            viewModel = ViewModelProvider(this, factory)[FinanceViewModel::class.java]
        } catch (e: Exception) {
            // If ViewModel initialization fails, show error message
            Snackbar.make(
                binding.root,
                "Initialization failed: ${e.message}",
                Snackbar.LENGTH_LONG
            ).show()
            
            // Log error
            android.util.Log.e("FinanceFragment", "ViewModel initialization failed", e)
        }
    }

    /**
     * Setup RecyclerView
     */
    private fun setupRecyclerView() {
        transactionAdapter = TransactionListAdapter(
            onItemClick = { transaction ->
                // Navigate to edit page with lifecycle check
                if (isAdded && !isDetached && activity != null && activity?.isFinishing != true) {
                    try {
                        val action = FinanceFragmentDirections
                            .actionFinanceFragmentToAddEditTransactionFragment(transaction.id)
                        findNavController().navigate(action)
                    } catch (e: Exception) {
                        android.util.Log.e("FinanceFragment", "Error navigating to edit transaction", e)
                    }
                }
            },
            onItemLongClick = { transaction ->
                // Show delete confirmation dialog with lifecycle check
                if (isAdded && !isDetached && activity != null && activity?.isFinishing != true) {
                    showDeleteConfirmDialog(transaction)
                }
            }
        )
        
        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        binding.apply {
            // Add income quick button - use quick record dialog
            buttonQuickIncome.setOnClickListener {
                if (isAdded && !isDetached && activity != null && activity?.isFinishing != true) {
                    showQuickTransactionDialog(Transaction.TransactionType.INCOME)
                }
            }

            // Add expense quick button - use quick record dialog
            buttonQuickExpense.setOnClickListener {
                if (isAdded && !isDetached && activity != null && activity?.isFinishing != true) {
                    showQuickTransactionDialog(Transaction.TransactionType.EXPENSE)
                }
            }

            // Floating action button - navigate to complete add page
            fabAdd.setOnClickListener {
                if (isAdded && !isDetached && activity != null && activity?.isFinishing != true) {
                    try {
                        val action = FinanceFragmentDirections
                            .actionFinanceFragmentToAddEditTransactionFragment("")
                        findNavController().navigate(action)
                    } catch (e: Exception) {
                        android.util.Log.e("FinanceFragment", "Error navigating to add transaction", e)
                    }
                }
            }

            // Pull to refresh
            swipeRefreshLayout.setOnRefreshListener {
                if (isAdded && !isDetached && ::viewModel.isInitialized && activity != null && activity?.isFinishing != true) {
                    try {
                        viewModel.refresh()
                    } catch (e: Exception) {
                        android.util.Log.e("FinanceFragment", "Error refreshing data", e)
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }

            // Date range selection
            textViewDateRange.setOnClickListener {
                if (isAdded && !isDetached && activity != null && activity?.isFinishing != true) {
                    showDateRangeDialog()
                }
            }
        }
    }

    /**
     * Setup search functionality
     */
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (isAdded && !isDetached && ::viewModel.isInitialized && activity != null && activity?.isFinishing != true) {
                    try {
                        viewModel.searchTransactions(newText ?: "")
                    } catch (e: Exception) {
                        android.util.Log.e("FinanceFragment", "Error searching transactions", e)
                    }
                }
                return true
            }
        })

        // Search hint
        binding.searchView.queryHint = getString(R.string.search_transactions_hint)
    }

    /**
     * Setup filter chips
     */
    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (isAdded && !isDetached && ::viewModel.isInitialized && activity != null && activity?.isFinishing != true) {
                try {
                    val filter = when (checkedIds.firstOrNull()) {
                        R.id.chipAll -> FilterOption.ALL
                        R.id.chipIncome -> FilterOption.INCOME
                        R.id.chipExpense -> FilterOption.EXPENSE
                        else -> FilterOption.ALL
                    }
                    viewModel.setFilter(filter)
                } catch (e: Exception) {
                    android.util.Log.e("FinanceFragment", "Error setting filter", e)
                }
            }
        }
    }

    /**
     * Setup data observers
     */
    private fun setupObservers() {
        // Observe transaction record list
        viewModel.filteredTransactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions)
            updateEmptyState(transactions.isEmpty())
        }

        // Observe financial statistics data
        viewModel.financialSummary.observe(viewLifecycleOwner) { summary ->
            updateFinancialSummary(summary)
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        // Observe operation results
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                val message = if (it.isSuccess) it.message else getString(R.string.operation_failed) + ": ${it.message}"
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                viewModel.clearOperationResult()
            }
        }
    }

    /**
     * Update financial statistics display
     */
    private fun updateFinancialSummary(summary: FinancialSummary) {
        binding.apply {
            textViewBalance.text = numberFormat.format(summary.balance)
            textViewIncome.text = numberFormat.format(summary.totalIncome)
            textViewExpense.text = numberFormat.format(summary.totalExpense)
            
            // Set color based on balance
            val balanceColor = if (summary.balance >= 0) {
                requireContext().getColor(R.color.md_theme_success)
            } else {
                requireContext().getColor(R.color.md_theme_error)
            }
            textViewBalance.setTextColor(balanceColor)
            
            // Update transaction count
            textViewTransactionCount.text = getString(R.string.no_transactions_count).format(summary.transactionCount)
        }
    }

    /**
     * Update empty state display
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
     * Show delete confirmation dialog
     */
    private fun showDeleteConfirmDialog(transaction: Transaction) {
        // 检查Fragment生命周期状态
        if (!isAdded || isDetached || activity == null || activity?.isFinishing == true) {
            android.util.Log.w("FinanceFragment", "Fragment not active, cannot show delete confirmation dialog")
            return
        }

        try {
            val messageRes = if (transaction.type == Transaction.TransactionType.INCOME) {
                R.string.delete_income_record
            } else {
                R.string.delete_expense_record
            }
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete)
                .setMessage(messageRes)
                .setPositiveButton(R.string.delete) { _, _ ->
                    // 检查Fragment状态后再删除
                    if (isAdded && !isDetached && ::viewModel.isInitialized && activity != null && activity?.isFinishing != true) {
                        try {
                            viewModel.deleteTransaction(transaction)
                        } catch (e: Exception) {
                            android.util.Log.e("FinanceFragment", "Error deleting transaction", e)
                            if (isAdded && view != null) {
                                Snackbar.make(binding.root, "Delete failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } catch (e: Exception) {
            android.util.Log.e("FinanceFragment", "Error showing delete confirmation dialog", e)
        }
    }

    /**
     * Show quick transaction dialog
     */
    private fun showQuickTransactionDialog(type: Transaction.TransactionType) {
        // 检查Fragment生命周期状态，确保安全显示Dialog
        if (!isAdded || isDetached || activity == null || activity?.isFinishing == true) {
            android.util.Log.w("FinanceFragment", "Fragment not active, cannot show QuickTransactionDialog")
            return
        }

        try {
            val dialog = QuickTransactionDialog.newInstance(type)
            dialog.setOnTransactionAddedListener { transaction ->
                // 再次检查Fragment状态，确保安全调用ViewModel
                if (isAdded && !isDetached && ::viewModel.isInitialized && activity != null && activity?.isFinishing != true) {
                    try {
                        viewModel.addTransaction(transaction)
                        android.util.Log.d("FinanceFragment", "Transaction added successfully via QuickRecord")
                    } catch (e: Exception) {
                        android.util.Log.e("FinanceFragment", "Error adding transaction", e)
                        // 如果添加失败，显示错误信息
                        if (isAdded && view != null) {
                            Snackbar.make(binding.root, getString(R.string.save_failed_format, e.message), Snackbar.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    android.util.Log.w("FinanceFragment", "Fragment not active when transaction callback invoked")
                }
            }
            
            // 使用parentFragmentManager显示Dialog，并检查FragmentManager状态
            if (!parentFragmentManager.isStateSaved && !parentFragmentManager.isDestroyed) {
                dialog.show(parentFragmentManager, "QuickTransactionDialog")
                android.util.Log.d("FinanceFragment", "QuickTransactionDialog shown successfully")
            } else {
                android.util.Log.w("FinanceFragment", "FragmentManager not ready, cannot show dialog")
            }
        } catch (e: Exception) {
            android.util.Log.e("FinanceFragment", "Error showing QuickTransactionDialog", e)
            // 显示友好的错误信息
            if (isAdded && view != null) {
                Snackbar.make(binding.root, "Unable to open quick record dialog", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Show date range selection dialog
     */
    private fun showDateRangeDialog() {
        // 检查Fragment生命周期状态
        if (!isAdded || isDetached || activity == null || activity?.isFinishing == true || !::viewModel.isInitialized) {
            android.util.Log.w("FinanceFragment", "Fragment not active, cannot show date range dialog")
            return
        }

        try {
            val options = arrayOf(
                getString(R.string.today), 
                getString(R.string.this_week), 
                getString(R.string.this_month), 
                getString(R.string.this_year), 
                getString(R.string.all)
            )
            val currentSelection = when (viewModel.getCurrentDateRange()) {
                DateRange.TODAY -> 0
                DateRange.THIS_WEEK -> 1
                DateRange.THIS_MONTH -> 2
                DateRange.THIS_YEAR -> 3
                DateRange.ALL -> 4
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.select_date_range)
                .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                    // 检查Fragment状态后再设置日期范围
                    if (isAdded && !isDetached && ::viewModel.isInitialized && activity != null && activity?.isFinishing != true) {
                        try {
                            val dateRange = when (which) {
                                0 -> DateRange.TODAY
                                1 -> DateRange.THIS_WEEK
                                2 -> DateRange.THIS_MONTH
                                3 -> DateRange.THIS_YEAR
                                else -> DateRange.ALL
                            }
                            viewModel.setDateRange(dateRange)
                            if (view != null) {
                                binding.textViewDateRange.text = options[which]
                            }
                            dialog.dismiss()
                        } catch (e: Exception) {
                            android.util.Log.e("FinanceFragment", "Error setting date range", e)
                            dialog.dismiss()
                        }
                    } else {
                        dialog.dismiss()
                    }
                }
                .show()
        } catch (e: Exception) {
            android.util.Log.e("FinanceFragment", "Error showing date range dialog", e)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when Fragment becomes visible, ensuring latest data is shown when returning from AddEditTransactionFragment
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
         * Create FinanceFragment instance
         */
        @JvmStatic
        fun newInstance() = FinanceFragment()
    }
}

/**
 * FinanceViewModel Factory Class
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
 * Sort option enumeration
 */
enum class SortOrder {
    AMOUNT_ASC,    // Amount ascending
    AMOUNT_DESC,   // Amount descending
    DATE_ASC,      // Date ascending
    DATE_DESC      // Date descending
} 