package com.example.life_ledger.ui.budget.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.life_ledger.R
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
    
    // 使用activityViewModels确保与父Fragment共享ViewModel
    private val budgetViewModel: BudgetViewModel by activityViewModels()
    
    private var selectedPeriod: Budget.BudgetPeriod = Budget.BudgetPeriod.MONTHLY
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    
    companion object {
        private const val ARG_BUDGET_ID = "budget_id"
        private const val ARG_BUDGET_NAME = "budget_name"
        private const val ARG_BUDGET_AMOUNT = "budget_amount"
        private const val ARG_BUDGET_CATEGORY_ID = "budget_category_id"
        private const val ARG_BUDGET_PERIOD = "budget_period"
        private const val ARG_BUDGET_DESCRIPTION = "budget_description"
        private const val ARG_BUDGET_ALERT_THRESHOLD = "budget_alert_threshold"
        private const val ARG_BUDGET_IS_RECURRING = "budget_is_recurring"
        private const val ARG_BUDGET_IS_ALERT_ENABLED = "budget_is_alert_enabled"
        
        fun newInstance(budget: Budget? = null): AddEditBudgetDialog {
            val dialog = AddEditBudgetDialog()
            val args = Bundle()
            
            budget?.let {
                args.putString(ARG_BUDGET_ID, it.id)
                args.putString(ARG_BUDGET_NAME, it.name)
                args.putDouble(ARG_BUDGET_AMOUNT, it.amount)
                args.putString(ARG_BUDGET_CATEGORY_ID, it.categoryId)
                args.putString(ARG_BUDGET_PERIOD, it.period.name)
                args.putString(ARG_BUDGET_DESCRIPTION, it.description)
                args.putDouble(ARG_BUDGET_ALERT_THRESHOLD, it.alertThreshold)
                args.putBoolean(ARG_BUDGET_IS_RECURRING, it.isRecurring)
                args.putBoolean(ARG_BUDGET_IS_ALERT_ENABLED, it.isAlertEnabled)
            }
            
            dialog.arguments = args
            return dialog
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // 设置对话框样式 - 使用Material主题而不是透明背景
        dialog.window?.apply {
            setBackgroundDrawableResource(R.drawable.dialog_background)
            // 设置进入和退出动画
            attributes?.windowAnimations = R.style.DialogAnimation
        }
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditBudgetBinding.inflate(inflater, container, false)
        
        // 获取传入的预算数据
        arguments?.let { args ->
            if (args.containsKey(ARG_BUDGET_ID)) {
                currentBudget = Budget(
                    id = args.getString(ARG_BUDGET_ID, ""),
                    name = args.getString(ARG_BUDGET_NAME, ""),
                    amount = args.getDouble(ARG_BUDGET_AMOUNT, 0.0),
                    categoryId = args.getString(ARG_BUDGET_CATEGORY_ID),
                    period = Budget.BudgetPeriod.valueOf(args.getString(ARG_BUDGET_PERIOD, "MONTHLY")),
                    startDate = System.currentTimeMillis(),
                    endDate = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L,
                    description = args.getString(ARG_BUDGET_DESCRIPTION),
                    alertThreshold = args.getDouble(ARG_BUDGET_ALERT_THRESHOLD, 0.8),
                    isRecurring = args.getBoolean(ARG_BUDGET_IS_RECURRING, true),
                    isAlertEnabled = args.getBoolean(ARG_BUDGET_IS_ALERT_ENABLED, true)
                )
            }
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
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    
    private fun setupViews() {
        // 设置标题
        binding.tvTitle.text = if (currentBudget != null) getString(R.string.edit_budget_title) else getString(R.string.create_budget)
        
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
                    // Reset button state
                    resetButtonState()
                    dismiss()
                }
            }
        }
        
        budgetViewModel.error.observe(viewLifecycleOwner) { message ->
            message?.let {
                // Reset button state
                resetButtonState()
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    private fun resetButtonState() {
        binding.btnSave.apply {
            isEnabled = true
            text = if (currentBudget != null) getString(R.string.update) else getString(R.string.save)
        }
        binding.btnCancel.isEnabled = true
    }
    
    private fun setupClickListeners() {
        // Cancel button - add prevent double tap
        binding.btnCancel.setOnClickListener {
            it.isEnabled = false  // Temporarily disable button
            dismiss()
        }
        
        // Save button - add prevent double tap and loading state
        binding.btnSave.setOnClickListener {
            if (it.isEnabled && validateInput()) {
                it.isEnabled = false  // Prevent double tap
                binding.btnSave.text = getString(R.string.saving)
                saveBudget()
            }
        }
        
        // Alert threshold slider
        binding.sliderAlertThreshold.addOnChangeListener { _, value, _ ->
            binding.tvAlertThreshold.text = "${value.toInt()}%"
        }
        
        // Period selection listener
        binding.spinnerPeriod.setOnItemClickListener { _, _, position, _ ->
            selectedPeriod = Budget.BudgetPeriod.values()[position]
        }
        
        // Category selection listener
        binding.spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = if (position == 0) null else categories[position - 1].id
        }
    }
    
    private fun setupCategorySpinner() {
        val categoryNames = mutableListOf(getString(R.string.total_budget_unlimited_category))
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
                val categoryName = if (categoryIndex == 0) getString(R.string.total_budget_unlimited_category) else categories[categoryIndex - 1].name
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
            // Budget name validation
            val name = etBudgetName.text.toString().trim()
            if (name.isEmpty()) {
                tilBudgetName.error = getString(R.string.please_enter_budget_name)
                return false
            } else {
                tilBudgetName.error = null
            }
            
            // Budget amount validation
            val amountText = etBudgetAmount.text.toString().trim()
            if (amountText.isEmpty()) {
                tilBudgetAmount.error = getString(R.string.please_enter_budget_amount)
                return false
            }
            
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                tilBudgetAmount.error = getString(R.string.please_enter_valid_budget_amount)
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
                // Update existing budget
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
                // Create new budget
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