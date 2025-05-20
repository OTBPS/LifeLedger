package com.example.life_ledger.ui.finance

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.life_ledger.R
import com.example.life_ledger.data.model.Category
import com.example.life_ledger.data.model.Transaction
import com.example.life_ledger.data.database.AppDatabase
import com.example.life_ledger.data.repository.LifeLedgerRepository
import com.example.life_ledger.databinding.FragmentAddEditTransactionBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 添加/编辑财务记录页面
 * 提供完整的表单界面和数据验证
 */
class AddEditTransactionFragment : Fragment() {

    private var _binding: FragmentAddEditTransactionBinding? = null
    private val binding get() = _binding!!

    private val args: AddEditTransactionFragmentArgs by navArgs()
    private lateinit var viewModel: FinanceViewModel
    private lateinit var repository: LifeLedgerRepository

    private var selectedDate = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // 分类数据
    private var allCategories = listOf<Category>()
    private var currentCategories = listOf<Category>()

    private var isEditMode = false
    private var editingTransaction: Transaction? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_add_edit_transaction,
            container,
            false
        )
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRepository()
        setupViewModel()
        setupUI()
        setupClickListeners()
        setupObservers()
        loadCategories()
        
        // 检查是否为编辑模式
        if (args.transactionId.isNotEmpty()) {
            isEditMode = true
            loadTransactionForEdit(args.transactionId)
        } else {
            // 新增模式，设置默认值
            updateCategorySpinner(Transaction.TransactionType.EXPENSE)
        }
    }

    /**
     * 初始化Repository
     */
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

    /**
     * 初始化ViewModel
     */
    private fun setupViewModel() {
        try {
            val database = AppDatabase.getDatabase(requireContext())
            val transactionRepository = com.example.life_ledger.data.repository.TransactionRepository(database.transactionDao())
            val factory = FinanceViewModelFactory(transactionRepository)
            viewModel = ViewModelProvider(this, factory)[FinanceViewModel::class.java]
        } catch (e: Exception) {
            // 如果ViewModel初始化失败，显示错误并返回
            Snackbar.make(
                binding.root, 
                "初始化失败: ${e.message}", 
                Snackbar.LENGTH_LONG
            ).show()
            
            // 记录错误日志
            android.util.Log.e("AddEditTransactionFragment", "ViewModel initialization failed", e)
            
            // 返回上一页
            findNavController().navigateUp()
        }
    }

    /**
     * 加载分类数据
     */
    private fun loadCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.getFinancialCategories().collect { categories ->
                    // 检查Fragment是否还活跃
                    if (!isAdded || _binding == null) {
                        return@collect
                    }
                    
                    allCategories = categories
                    // 根据当前选择的交易类型更新分类列表
                    val currentType = if (binding.radioGroupType.checkedRadioButtonId == R.id.radioIncome) {
                        Transaction.TransactionType.INCOME
                    } else {
                        Transaction.TransactionType.EXPENSE
                    }
                    updateCategorySpinner(currentType)
                }
            } catch (e: Exception) {
                // 检查Fragment是否还活跃
                if (isAdded && _binding != null) {
                    Snackbar.make(binding.root, "加载分类失败: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 设置UI组件
     */
    private fun setupUI() {
        binding.apply {
            // 设置日期显示
            updateDateDisplay()
            
            // 设置交易类型单选按钮默认选择
            radioGroupType.check(R.id.radioExpense)
            
            // 设置标题
            toolbarAddEdit.title = if (isEditMode) "编辑记录" else "添加记录"
        }
    }

    /**
     * 设置点击监听器
     */
    private fun setupClickListeners() {
        binding.apply {
            // 返回按钮
            toolbarAddEdit.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            // 日期选择
            layoutDate.setOnClickListener {
                showDatePicker()
            }

            // 交易类型切换
            radioGroupType.setOnCheckedChangeListener { _, checkedId ->
                val type = if (checkedId == R.id.radioIncome) {
                    Transaction.TransactionType.INCOME
                } else {
                    Transaction.TransactionType.EXPENSE
                }
                updateCategorySpinner(type)
            }

            // 保存按钮
            buttonSave.setOnClickListener {
                saveTransaction()
            }

            // 删除按钮（仅编辑模式显示）
            buttonDelete.setOnClickListener {
                deleteTransaction()
            }

            // 分类管理按钮
            buttonManageCategories.setOnClickListener {
                // 导航到分类管理页面
                findNavController().navigate(
                    AddEditTransactionFragmentDirections.actionAddEditTransactionFragmentToCategoryManagementFragment()
                )
            }
        }
    }

    /**
     * 设置数据观察者
     */
    private fun setupObservers() {
        if (!::viewModel.isInitialized) {
            android.util.Log.e("AddEditTransactionFragment", "ViewModel not initialized, skipping observers setup")
            return
        }
        
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    Snackbar.make(binding.root, it.message, Snackbar.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Snackbar.make(binding.root, it.message, Snackbar.LENGTH_LONG).show()
                }
                viewModel.clearOperationResult()
            }
        }
    }

    /**
     * 显示日期选择器
     */
    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                updateDateDisplay()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * 更新日期显示
     */
    private fun updateDateDisplay() {
        if (_binding == null) return
        binding.tvSelectedDate.text = dateFormat.format(selectedDate.time)
    }

    /**
     * 根据交易类型更新分类下拉菜单
     */
    private fun updateCategorySpinner(type: Transaction.TransactionType) {
        currentCategories = allCategories.filter { category ->
            when (type) {
                Transaction.TransactionType.INCOME -> category.isIncomeCategory()
                Transaction.TransactionType.EXPENSE -> category.isExpenseCategory()
            }
        }.filter { it.isActive }

        val categoryNames = currentCategories.map { it.name }
        
        // 检查binding是否为null
        if (_binding == null) return
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categoryNames
        )
        binding.spinnerCategory.adapter = adapter
    }

    /**
     * 获取当前选中的分类
     */
    private fun getSelectedCategory(): Category? {
        if (_binding == null) return null
        val selectedPosition = binding.spinnerCategory.selectedItemPosition
        return if (selectedPosition >= 0 && selectedPosition < currentCategories.size) {
            currentCategories[selectedPosition]
        } else {
            null
        }
    }

    /**
     * 加载要编辑的交易记录
     */
    private fun loadTransactionForEdit(transactionId: String) {
        if (!::viewModel.isInitialized) {
            android.util.Log.e("AddEditTransactionFragment", "ViewModel not initialized, cannot load transaction")
            findNavController().navigateUp()
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                editingTransaction = viewModel.getTransactionById(transactionId)
                
                // 检查Fragment是否还活跃
                if (!isAdded || _binding == null) {
                    return@launch
                }
                
                editingTransaction?.let { transaction ->
                    binding.apply {
                        // 设置金额
                        editAmount.setText(transaction.amount.toString())
                        
                        // 设置交易类型
                        val typeRadioId = if (transaction.type == Transaction.TransactionType.INCOME) {
                            R.id.radioIncome
                        } else {
                            R.id.radioExpense
                        }
                        radioGroupType.check(typeRadioId)
                        
                        // 更新分类下拉菜单
                        updateCategorySpinner(transaction.type)
                        
                        // 设置选中的分类
                        if (!transaction.categoryId.isNullOrEmpty()) {
                            val categoryIndex = currentCategories.indexOfFirst { it.id == transaction.categoryId }
                            if (categoryIndex >= 0) {
                                spinnerCategory.setSelection(categoryIndex)
                            }
                        }
                        
                        // 设置日期
                        selectedDate.time = Date(transaction.date)
                        updateDateDisplay()
                        
                        // 设置标签和备注
                        editTags.setText(transaction.getTagsList().joinToString(", "))
                        editDescription.setText(transaction.description ?: "")
                        
                        // 显示删除按钮
                        buttonDelete.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                // 检查Fragment是否还活跃
                if (isAdded && _binding != null) {
                    Snackbar.make(binding.root, "加载记录失败", Snackbar.LENGTH_LONG).show()
                }
                android.util.Log.e("AddEditTransactionFragment", "Failed to load transaction", e)
            }
        }
    }

    /**
     * 保存交易记录
     */
    private fun saveTransaction() {
        if (!::viewModel.isInitialized) {
            android.util.Log.e("AddEditTransactionFragment", "ViewModel not initialized, cannot save transaction")
            Snackbar.make(binding.root, "系统错误，无法保存", Snackbar.LENGTH_LONG).show()
            return
        }
        
        android.util.Log.d("AddEditTransactionFragment", "开始保存交易记录")
        android.util.Log.d("AddEditTransactionFragment", "选择的日期: ${dateFormat.format(selectedDate.time)} (${selectedDate.timeInMillis})")
        android.util.Log.d("AddEditTransactionFragment", "是否编辑模式: $isEditMode")
        
        if (!validateInput()) {
            android.util.Log.e("AddEditTransactionFragment", "输入验证失败")
            return
        }

        val amount = binding.editAmount.text.toString().toDouble()
        val type = if (binding.radioGroupType.checkedRadioButtonId == R.id.radioIncome) {
            Transaction.TransactionType.INCOME
        } else {
            Transaction.TransactionType.EXPENSE
        }
        
        val selectedCategory = getSelectedCategory()
        android.util.Log.d("AddEditTransactionFragment", "交易信息: 金额=$amount, 类型=$type, 分类=${selectedCategory?.name}")
        
        val description = binding.editDescription.text.toString().trim()
        val tagsText = binding.editTags.text.toString().trim()
        val tagsList = if (tagsText.isNotEmpty()) {
            tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        val transaction = if (isEditMode && editingTransaction != null) {
            // 更新现有记录
            editingTransaction!!.copy(
                amount = amount,
                type = type,
                categoryId = selectedCategory?.id,
                title = selectedCategory?.name ?: "未分类",
                description = description.ifEmpty { null },
                date = selectedDate.timeInMillis,
                updatedAt = System.currentTimeMillis()
            ).setTagsList(tagsList)
        } else {
            // 创建新记录
            Transaction(
                amount = amount,
                type = type,
                categoryId = selectedCategory?.id,
                title = selectedCategory?.name ?: "未分类",
                description = description.ifEmpty { null },
                date = selectedDate.timeInMillis
            ).setTagsList(tagsList)
        }

        // 更新分类使用次数
        selectedCategory?.let { category ->
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    repository.incrementCategoryUsage(category.id)
                } catch (e: Exception) {
                    android.util.Log.w("AddEditTransactionFragment", "Failed to update category usage", e)
                }
            }
        }

        if (isEditMode) {
            viewModel.updateTransaction(transaction)
        } else {
            viewModel.addTransaction(transaction)
        }
    }

    /**
     * 删除交易记录
     */
    private fun deleteTransaction() {
        if (!::viewModel.isInitialized) {
            android.util.Log.e("AddEditTransactionFragment", "ViewModel not initialized, cannot delete transaction")
            Snackbar.make(binding.root, "系统错误，无法删除", Snackbar.LENGTH_LONG).show()
            return
        }
        
        editingTransaction?.let { transaction ->
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除这条记录吗？")
                .setPositiveButton("删除") { _, _ ->
                    viewModel.deleteTransaction(transaction)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    /**
     * 验证输入数据
     */
    private fun validateInput(): Boolean {
        binding.apply {
            // 验证金额
            val amountText = editAmount.text.toString().trim()
            if (amountText.isEmpty()) {
                editAmount.error = "请输入金额"
                editAmount.requestFocus()
                return false
            }

            val amount = try {
                amountText.toDouble()
            } catch (e: NumberFormatException) {
                editAmount.error = "请输入有效的金额"
                editAmount.requestFocus()
                return false
            }

            if (amount <= 0) {
                editAmount.error = "金额必须大于0"
                editAmount.requestFocus()
                return false
            }

            if (amount > 999999999) {
                editAmount.error = "金额过大"
                editAmount.requestFocus()
                return false
            }

            // 验证日期
            if (selectedDate.timeInMillis <= 0) {
                Snackbar.make(root, "请选择有效日期", Snackbar.LENGTH_SHORT).show()
                return false
            }

            val now = System.currentTimeMillis()
            val oneYearAgo = now - 365L * 24 * 60 * 60 * 1000
            val oneYearFromNow = now + 365L * 24 * 60 * 60 * 1000

            if (selectedDate.timeInMillis < oneYearAgo || selectedDate.timeInMillis > oneYearFromNow) {
                Snackbar.make(root, "日期必须在一年范围内", Snackbar.LENGTH_SHORT).show()
                return false
            }

            // 验证分类选择
            if (getSelectedCategory() == null) {
                Snackbar.make(root, "请选择分类", Snackbar.LENGTH_SHORT).show()
                return false
            }

            // 清除之前的错误提示
            editAmount.error = null
        }

        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 