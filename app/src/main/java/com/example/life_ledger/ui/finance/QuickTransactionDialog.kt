package com.example.life_ledger.ui.finance

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.life_ledger.R
import com.example.life_ledger.data.database.AppDatabase
import com.example.life_ledger.data.model.Category
import com.example.life_ledger.data.model.Transaction
import com.example.life_ledger.data.repository.LifeLedgerRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import android.widget.Spinner
import android.widget.RadioButton

/**
 * 快速记录对话框
 * 提供常用金额和分类的快速选择
 */
class QuickTransactionDialog : DialogFragment() {

    private var onTransactionAddedListener: ((Transaction) -> Unit)? = null
    private lateinit var repository: LifeLedgerRepository
    private var allCategories = listOf<Category>()
    private var presetType: Transaction.TransactionType? = null
    
    // UI控件引用
    private var spinnerCategory: Spinner? = null
    private var radioIncome: RadioButton? = null
    private var radioExpense: RadioButton? = null

    // 预设金额选项
    private val quickAmounts = listOf(
        10.0, 20.0, 50.0, 100.0, 200.0, 500.0
    )

    companion object {
        private const val ARG_TRANSACTION_TYPE = "transaction_type"
        
        /**
         * 创建带有预设类型的快速交易对话框
         */
        fun newInstance(type: Transaction.TransactionType): QuickTransactionDialog {
            val dialog = QuickTransactionDialog()
            val args = Bundle()
            args.putString(ARG_TRANSACTION_TYPE, type.name)
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val typeName = it.getString(ARG_TRANSACTION_TYPE)
            presetType = typeName?.let { name ->
                Transaction.TransactionType.valueOf(name)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // 在创建Dialog前检查Fragment状态
        if (!isAdded || isDetached || activity == null) {
            return super.onCreateDialog(savedInstanceState)
        }

        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_quick_transaction, null)

        setupRepository()
        
        val editAmount = view.findViewById<TextInputEditText>(R.id.editAmount)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        radioIncome = view.findViewById(R.id.radioIncome)
        radioExpense = view.findViewById(R.id.radioExpense)

        // 设置快速金额按钮点击事件
        setQuickAmountListeners(view, editAmount)

        // 设置交易类型切换 - 添加Fragment生命周期检查
        radioIncome?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && isAdded && !isDetached && context != null) {
                updateCategorySpinner(spinnerCategory, Transaction.TransactionType.INCOME)
            }
        }

        radioExpense?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && isAdded && !isDetached && context != null) {
                updateCategorySpinner(spinnerCategory, Transaction.TransactionType.EXPENSE)
            }
        }

        // 先设置默认的radio button选择
        when (presetType) {
            Transaction.TransactionType.INCOME -> {
                radioIncome?.isChecked = true
            }
            Transaction.TransactionType.EXPENSE -> {
                radioExpense?.isChecked = true
            }
            null -> {
                // 默认选择支出
                radioExpense?.isChecked = true
            }
        }
        
        // 启动分类数据加载（这会在数据加载完成后自动更新Spinner）
        loadCategories()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.quick_record)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                // 检查Dialog和Fragment状态
                if (isAdded && !isDetached && activity != null && activity?.isFinishing != true) {
                    try {
                        saveQuickTransaction(view)
                    } catch (e: Exception) {
                        android.util.Log.e("QuickTransactionDialog", "Error in save button", e)
                        // 如果发生错误，安全关闭Dialog
                        try {
                            if (isAdded && !isDetached) {
                                dismiss()
                            }
                        } catch (dismissError: Exception) {
                            android.util.Log.w("QuickTransactionDialog", "Error dismissing after error", dismissError)
                        }
                    }
                }
                // 注意：不要在这里调用dismiss()，让saveQuickTransaction处理
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                // 安全关闭Dialog
                try {
                    if (isAdded && !isDetached && activity != null && activity?.isFinishing != true) {
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("QuickTransactionDialog", "Error dismissing dialog", e)
                }
            }
            .create()
    }

    private fun setupRepository() {
        try {
            if (isAdded && context != null) {
                val database = AppDatabase.getDatabase(requireContext())
                repository = LifeLedgerRepository(
                    database.transactionDao(),
                    database.todoDao(),
                    database.categoryDao(),
                    database.budgetDao(),
                    database.userSettingsDao()
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("QuickTransactionDialog", "Failed to setup repository", e)
        }
    }

    private fun loadCategories() {
        // 检查Fragment状态
        if (!isAdded || isDetached || !::repository.isInitialized) {
            android.util.Log.w("QuickTransactionDialog", "Fragment not ready for loading categories")
            return
        }

        lifecycleScope.launch {
            try {
                // 再次检查状态，因为协程可能延迟执行
                if (!isAdded || isDetached) {
                    return@launch
                }
                
                repository.getFinancialCategories().collect { categories ->
                    // 检查Fragment是否仍然活跃
                    if (isAdded && !isDetached && context != null) {
                        allCategories = categories
                        
                        // 数据加载完成后，更新当前显示的Spinner
                        val currentType = getCurrentSelectedType()
                        updateCategorySpinner(spinnerCategory, currentType)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("QuickTransactionDialog", "Failed to load categories", e)
                // 如果加载失败且Fragment仍然活跃，创建一些默认分类
                if (isAdded && !isDetached && context != null) {
                    allCategories = createDefaultCategories()
                    val currentType = getCurrentSelectedType()
                    updateCategorySpinner(spinnerCategory, currentType)
                }
            }
        }
    }
    
    /**
     * 获取当前选中的交易类型
     */
    private fun getCurrentSelectedType(): Transaction.TransactionType {
        return when {
            radioIncome?.isChecked == true -> Transaction.TransactionType.INCOME
            radioExpense?.isChecked == true -> Transaction.TransactionType.EXPENSE
            presetType != null -> presetType!!
            else -> Transaction.TransactionType.EXPENSE // 默认支出
        }
    }
    
    /**
     * 创建默认分类（当从数据库加载失败时使用）
     */
    private fun createDefaultCategories(): List<Category> {
        return if (isAdded && context != null) {
            listOf(
                Category(
                    id = "default_food",
                    name = getString(R.string.category_food),
                    type = Category.CategoryType.FINANCIAL,
                    subType = Category.FinancialSubType.EXPENSE,
                    isActive = true,
                    sortOrder = 1
                ),
                Category(
                    id = "default_transport",
                    name = getString(R.string.category_transport),
                    type = Category.CategoryType.FINANCIAL,
                    subType = Category.FinancialSubType.EXPENSE,
                    isActive = true,
                    sortOrder = 2
                ),
                Category(
                    id = "default_salary",
                    name = getString(R.string.category_salary),
                    type = Category.CategoryType.FINANCIAL,
                    subType = Category.FinancialSubType.INCOME,
                    isActive = true,
                    sortOrder = 1
                ),
                Category(
                    id = "default_bonus",
                    name = getString(R.string.category_bonus),
                    type = Category.CategoryType.FINANCIAL,
                    subType = Category.FinancialSubType.INCOME,
                    isActive = true,
                    sortOrder = 2
                )
            )
        } else {
            emptyList()
        }
    }

    private fun setQuickAmountListeners(view: android.view.View, editAmount: TextInputEditText) {
        val buttonIds = arrayOf(
            R.id.button10, R.id.button20, R.id.button50,
            R.id.button100, R.id.button200, R.id.button500
        )

        buttonIds.forEachIndexed { index, buttonId ->
            view.findViewById<android.widget.Button>(buttonId)?.setOnClickListener {
                // 添加Fragment生命周期检查
                if (isAdded && !isDetached && context != null) {
                    try {
                        editAmount.setText(quickAmounts[index].toInt().toString())
                    } catch (e: Exception) {
                        android.util.Log.w("QuickTransactionDialog", "Error setting amount", e)
                    }
                }
            }
        }
    }

    private fun updateCategorySpinner(spinner: Spinner?, type: Transaction.TransactionType) {
        if (spinner == null || !isAdded || isDetached || context == null) {
            return
        }
        
        val categories = allCategories.filter { category ->
            when (type) {
                Transaction.TransactionType.INCOME -> category.isIncomeCategory()
                Transaction.TransactionType.EXPENSE -> category.isExpenseCategory()
            }
        }.filter { it.isActive }

        val categoryNames = categories.map { it.name }
        
        // 如果没有分类，显示提示信息
        val displayNames = if (categoryNames.isEmpty()) {
            listOf(getString(R.string.no_available_categories))
        } else {
            categoryNames
        }
        
        try {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                displayNames
            )
            spinner.adapter = adapter
            
            // 记录日志以便调试
            android.util.Log.d("QuickTransactionDialog", 
                "Updated spinner with ${categories.size} categories for type ${type.name}")
        } catch (e: Exception) {
            android.util.Log.e("QuickTransactionDialog", "Failed to update spinner", e)
        }
    }

    private fun saveQuickTransaction(view: android.view.View) {
        // 严格检查Dialog和Fragment是否还活跃
        if (!isAdded || isDetached || activity == null || activity?.isFinishing == true || context == null) {
            android.util.Log.w("QuickTransactionDialog", "Dialog not active, cannot save")
            return
        }

        try {
            val editAmount = view.findViewById<TextInputEditText>(R.id.editAmount)
            val radioIncome = view.findViewById<RadioButton>(R.id.radioIncome)

            val amountText = editAmount.text.toString().trim()
            if (amountText.isEmpty()) {
                showSnackbarSafely(view, getString(R.string.please_enter_amount))
                return
            }

            val amount = try {
                amountText.toDouble()
            } catch (e: NumberFormatException) {
                showSnackbarSafely(view, getString(R.string.please_enter_valid_amount))
                return
            }

            if (amount <= 0) {
                showSnackbarSafely(view, getString(R.string.amount_must_be_greater_than_zero))
                return
            }

            val type = if (radioIncome?.isChecked == true) {
                Transaction.TransactionType.INCOME
            } else {
                Transaction.TransactionType.EXPENSE
            }

            // 获取当前类型的分类
            val availableCategories = allCategories.filter { category ->
                when (type) {
                    Transaction.TransactionType.INCOME -> category.isIncomeCategory()
                    Transaction.TransactionType.EXPENSE -> category.isExpenseCategory()
                }
            }.filter { it.isActive }

            val selectedPosition = spinnerCategory?.selectedItemPosition ?: -1
            val selectedCategory = if (selectedPosition >= 0 && selectedPosition < availableCategories.size) {
                availableCategories[selectedPosition]
            } else {
                // 如果没有选择有效分类，尝试使用第一个可用分类
                availableCategories.firstOrNull()
            }

            // 如果选择了"暂无可用分类"或没有分类，提示用户
            if (selectedCategory == null || selectedCategory.name == getString(R.string.no_available_categories)) {
                showSnackbarSafely(view, getString(R.string.please_add_category_first))
                return
            }

            val transaction = Transaction(
                amount = amount,
                type = type,
                categoryId = selectedCategory.id,
                title = selectedCategory.name,
                description = getString(R.string.quick_record_description),
                date = System.currentTimeMillis()
            )

            android.util.Log.d("QuickTransactionDialog", "Transaction created: $transaction")
            
            // 安全调用回调
            try {
                onTransactionAddedListener?.invoke(transaction)
                android.util.Log.d("QuickTransactionDialog", "Transaction callback invoked successfully")
            } catch (e: Exception) {
                android.util.Log.e("QuickTransactionDialog", "Error invoking callback", e)
                showSnackbarSafely(view, getString(R.string.save_failed_format, e.message))
                return
            }

            // 更新分类使用次数 - 在后台线程进行
            if (::repository.isInitialized) {
                lifecycleScope.launch {
                    try {
                        repository.incrementCategoryUsage(selectedCategory.id)
                        android.util.Log.d("QuickTransactionDialog", "Category usage updated successfully")
                    } catch (e: Exception) {
                        android.util.Log.w("QuickTransactionDialog", "Failed to update category usage", e)
                    }
                }
            }
            
            // 在主线程中关闭对话框 - 再次检查状态
            dismissDialogSafely()
            
        } catch (e: Exception) {
            android.util.Log.e("QuickTransactionDialog", "Error saving transaction", e)
            if (isAdded && !isDetached && context != null) {
                showSnackbarSafely(view, getString(R.string.save_failed_format, e.message))
            }
        }
    }

    /**
     * 安全显示Snackbar
     */
    private fun showSnackbarSafely(view: android.view.View, message: String) {
        try {
            if (isAdded && !isDetached && context != null) {
                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.w("QuickTransactionDialog", "Error showing snackbar", e)
        }
    }

    /**
     * 安全关闭Dialog
     */
    private fun dismissDialogSafely() {
        try {
            if (isAdded && !isDetached && activity != null && activity?.isFinishing != true) {
                dismiss()
                android.util.Log.d("QuickTransactionDialog", "Dialog dismissed successfully")
            }
        } catch (e: Exception) {
            android.util.Log.w("QuickTransactionDialog", "Error dismissing dialog", e)
        }
    }

    fun setOnTransactionAddedListener(listener: (Transaction) -> Unit) {
        onTransactionAddedListener = listener
    }
    
    override fun onDetach() {
        super.onDetach()
        // 清理回调监听器
        onTransactionAddedListener = null
    }
} 