package com.example.life_ledger.ui.budget.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.life_ledger.data.model.Budget
import com.example.life_ledger.data.model.Category
import com.example.life_ledger.databinding.DialogAddEditBudgetBinding
import com.example.life_ledger.ui.budget.viewmodel.BudgetViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 添加/编辑预算对话框
 */
class AddEditBudgetDialog : DialogFragment() {
    
    private var _binding: DialogAddEditBudgetBinding? = null
    private val binding get() = _binding!!
    
    private var currentBudget: Budget? = null
    private var selectedCategoryId: String? = null
    private var selectedStartDate: Long = System.currentTimeMillis()
    private var selectedEndDate: Long = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L // 默认30天后
    
    private var categoryAdapter: ArrayAdapter<String>? = null
    private val categories = mutableListOf<Category>()
    
    private val budgetViewModel: BudgetViewModel by viewModels({ requireParentFragment() })
    
    private var selectedPeriod: Budget.BudgetPeriod = Budget.BudgetPeriod.MONTHLY
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    companion object {
        private const val ARG_BUDGET = "budget"
        
        fun newInstance(budget: Budget? = null): AddEditBudgetDialog {
            val dialog = AddEditBudgetDialog()
            val args = Bundle()
            budget?.let {
                // 使用JSON序列化代替Parcelable
                val gson = com.google.gson.Gson()
                args.putString(ARG_BUDGET, gson.toJson(it))
            }
            dialog.arguments = args
            return dialog
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditBudgetBinding.inflate(inflater, container, false)
        
        // 获取传入的预算数据
        val budgetJson = arguments?.getString(ARG_BUDGET)
        currentBudget = budgetJson?.let {
            val gson = com.google.gson.Gson()
            gson.fromJson(it, Budget::class.java)
        }
        
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupObservers()
        setupClickListeners()
        currentBudget?.let { fillFormWithBudget(it) }
    }
    
    override fun onStart() {
        super.onStart()
        // 设置对话框大小
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    
    private fun setupViews() {
        // 设置标题
        binding.tvTitle.text = if (currentBudget != null) "编辑预算" else "创建预算"
        
        // 设置周期选择器
        setupPeriodSpinner()
        
        // 设置默认值
        binding.sliderAlertThreshold.value = 80f
        binding.tvAlertThreshold.text = "80%"
        binding.switchRecurring.isChecked = true
        binding.switchAlertEnabled.isChecked = true
    }
    
    private fun setupPeriodSpinner() {
        val periodAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            Budget.BudgetPeriod.values().map { it.displayName }
        )
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPeriod.setAdapter(periodAdapter)
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            budgetViewModel.categories.collect { categoryList ->
                categories.clear()
                categories.addAll(categoryList)
                setupCategorySpinner()
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            budgetViewModel.successMessage.collect { message ->
                message?.let {
                    dismiss()
                }
            }
        }
        
        budgetViewModel.error.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setupClickListeners() {
        // 取消按钮
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        // 保存按钮
        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                saveBudget()
            }
        }
        
        // 警告阈值滑动条
        binding.sliderAlertThreshold.addOnChangeListener { _, value, _ ->
            binding.tvAlertThreshold.text = "${value.toInt()}%"
        }
        
        // 周期选择监听
        binding.spinnerPeriod.setOnItemClickListener { _, _, position, _ ->
            selectedPeriod = Budget.BudgetPeriod.values()[position]
        }
        
        // 分类选择监听
        binding.spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = if (position == 0) null else categories[position - 1].id
        }
    }
    
    private fun setupCategorySpinner() {
        val categoryNames = mutableListOf("总预算(不限分类)")
        categoryNames.addAll(categories.map { it.name })
        
        categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoryNames
        )
        categoryAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.setAdapter(categoryAdapter)
    }
    
    private fun fillFormWithBudget(budget: Budget) {
        with(binding) {
            etBudgetName.setText(budget.name)
            etBudgetAmount.setText(budget.amount.toString())
            etDescription.setText(budget.description ?: "")
            
            // 设置周期
            val periodIndex = Budget.BudgetPeriod.values().indexOf(budget.period)
            if (periodIndex >= 0) {
                spinnerPeriod.setText(Budget.BudgetPeriod.values()[periodIndex].displayName, false)
                selectedPeriod = budget.period
            }
            
            // 设置分类
            val categoryIndex = if (budget.categoryId == null) {
                0
            } else {
                val categoryPosition = categories.indexOfFirst { it.id == budget.categoryId }
                if (categoryPosition >= 0) categoryPosition + 1 else 0
            }
            if (categoryIndex < categories.size + 1) {
                val categoryName = if (categoryIndex == 0) "总预算(不限分类)" else categories[categoryIndex - 1].name
                spinnerCategory.setText(categoryName, false)
                selectedCategoryId = budget.categoryId
            }
            
            // 设置警告阈值
            val thresholdPercent = (budget.alertThreshold * 100).toFloat()
            sliderAlertThreshold.value = thresholdPercent
            tvAlertThreshold.text = "${thresholdPercent.toInt()}%"
            
            // 设置开关状态
            switchRecurring.isChecked = budget.isRecurring
            switchAlertEnabled.isChecked = budget.isAlertEnabled
        }
    }
    
    private fun validateInput(): Boolean {
        with(binding) {
            // 预算名称验证
            val name = etBudgetName.text.toString().trim()
            if (name.isEmpty()) {
                tilBudgetName.error = "请输入预算名称"
                return false
            } else {
                tilBudgetName.error = null
            }
            
            // 预算金额验证
            val amountText = etBudgetAmount.text.toString().trim()
            if (amountText.isEmpty()) {
                tilBudgetAmount.error = "请输入预算金额"
                return false
            }
            
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                tilBudgetAmount.error = "请输入有效的预算金额"
                return false
            } else {
                tilBudgetAmount.error = null
            }
            
            return true
        }
    }
    
    private fun saveBudget() {
        with(binding) {
            val name = etBudgetName.text.toString().trim()
            val amount = etBudgetAmount.text.toString().toDouble()
            val description = etDescription.text.toString().trim().ifEmpty { null }
            val alertThreshold = sliderAlertThreshold.value / 100.0
            val isRecurring = switchRecurring.isChecked
            val isAlertEnabled = switchAlertEnabled.isChecked
            
            if (currentBudget != null) {
                // 更新现有预算
                val updatedBudget = currentBudget!!.copy(
                    name = name,
                    categoryId = selectedCategoryId,
                    amount = amount,
                    period = selectedPeriod,
                    description = description,
                    alertThreshold = alertThreshold,
                    isRecurring = isRecurring,
                    isAlertEnabled = isAlertEnabled
                )
                budgetViewModel.updateBudget(updatedBudget)
            } else {
                // 创建新预算
                budgetViewModel.createBudget(
                    name = name,
                    categoryId = selectedCategoryId,
                    amount = amount,
                    period = selectedPeriod,
                    description = description,
                    alertThreshold = alertThreshold,
                    isRecurring = isRecurring
                )
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 