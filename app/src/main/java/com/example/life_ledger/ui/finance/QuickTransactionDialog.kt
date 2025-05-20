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
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_quick_transaction, null)

        setupRepository()
        
        val editAmount = view.findViewById<TextInputEditText>(R.id.editAmount)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        radioIncome = view.findViewById(R.id.radioIncome)
        radioExpense = view.findViewById(R.id.radioExpense)

        // 设置快速金额按钮点击事件
        setQuickAmountListeners(view, editAmount)

        // 设置交易类型切换
        radioIncome?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateCategorySpinner(spinnerCategory, Transaction.TransactionType.INCOME)
            }
        }

        radioExpense?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
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
            .setTitle("快速记录")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                saveQuickTransaction(view)
            }
            .setNegativeButton("取消", null)
            .create()
    }

    private fun setupRepository() {
        val database = AppDatabase.getDatabase(requireContext())
        repository = LifeLedgerRepository(
            database.transactionDao(),
            database.todoDao(),
            database.categoryDao(),
            database.budgetDao(),
            database.userSettingsDao()
        )
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                repository.getFinancialCategories().collect { categories ->
                    allCategories = categories
                    
                    // 数据加载完成后，更新当前显示的Spinner
                    val currentType = getCurrentSelectedType()
                    updateCategorySpinner(spinnerCategory, currentType)
                }
            } catch (e: Exception) {
                android.util.Log.e("QuickTransactionDialog", "Failed to load categories", e)
                // 如果加载失败，创建一些默认分类
                allCategories = createDefaultCategories()
                val currentType = getCurrentSelectedType()
                updateCategorySpinner(spinnerCategory, currentType)
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
        return listOf(
            Category(
                id = "default_food",
                name = "餐饮",
                type = Category.CategoryType.FINANCIAL,
                subType = Category.FinancialSubType.EXPENSE,
                isActive = true,
                sortOrder = 1
            ),
            Category(
                id = "default_transport",
                name = "交通",
                type = Category.CategoryType.FINANCIAL,
                subType = Category.FinancialSubType.EXPENSE,
                isActive = true,
                sortOrder = 2
            ),
            Category(
                id = "default_salary",
                name = "工资",
                type = Category.CategoryType.FINANCIAL,
                subType = Category.FinancialSubType.INCOME,
                isActive = true,
                sortOrder = 1
            ),
            Category(
                id = "default_bonus",
                name = "奖金",
                type = Category.CategoryType.FINANCIAL,
                subType = Category.FinancialSubType.INCOME,
                isActive = true,
                sortOrder = 2
            )
        )
    }

    private fun setQuickAmountListeners(view: android.view.View, editAmount: TextInputEditText) {
        val buttonIds = arrayOf(
            R.id.button10, R.id.button20, R.id.button50,
            R.id.button100, R.id.button200, R.id.button500
        )

        buttonIds.forEachIndexed { index, buttonId ->
            view.findViewById<android.widget.Button>(buttonId)?.setOnClickListener {
                editAmount.setText(quickAmounts[index].toInt().toString())
            }
        }
    }

    private fun updateCategorySpinner(spinner: Spinner?, type: Transaction.TransactionType) {
        if (spinner == null) return
        
        val categories = allCategories.filter { category ->
            when (type) {
                Transaction.TransactionType.INCOME -> category.isIncomeCategory()
                Transaction.TransactionType.EXPENSE -> category.isExpenseCategory()
            }
        }.filter { it.isActive }

        val categoryNames = categories.map { it.name }
        
        // 如果没有分类，显示提示信息
        val displayNames = if (categoryNames.isEmpty()) {
            listOf("暂无可用分类")
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
        val editAmount = view.findViewById<TextInputEditText>(R.id.editAmount)
        val radioIncome = view.findViewById<RadioButton>(R.id.radioIncome)

        val amountText = editAmount.text.toString().trim()
        if (amountText.isEmpty()) {
            Snackbar.make(view, "请输入金额", Snackbar.LENGTH_SHORT).show()
            return
        }

        val amount = try {
            amountText.toDouble()
        } catch (e: NumberFormatException) {
            Snackbar.make(view, "请输入有效金额", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (amount <= 0) {
            Snackbar.make(view, "金额必须大于0", Snackbar.LENGTH_SHORT).show()
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
        if (selectedCategory == null || selectedCategory.name == "暂无可用分类") {
            Snackbar.make(view, "请先添加分类或选择有效分类", Snackbar.LENGTH_SHORT).show()
            return
        }

        val transaction = Transaction(
            amount = amount,
            type = type,
            categoryId = selectedCategory.id,
            title = selectedCategory.name,
            description = "快速记录",
            date = System.currentTimeMillis()
        )

        onTransactionAddedListener?.invoke(transaction)

        // 更新分类使用次数
        lifecycleScope.launch {
            try {
                repository.incrementCategoryUsage(selectedCategory.id)
            } catch (e: Exception) {
                android.util.Log.w("QuickTransactionDialog", "Failed to update category usage", e)
            }
        }
    }

    fun setOnTransactionAddedListener(listener: (Transaction) -> Unit) {
        onTransactionAddedListener = listener
    }
} 