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
 * Add/Edit Financial Record Page
 * Provides complete form interface and data validation
 */
class AddEditTransactionFragment : Fragment() {

    private var _binding: FragmentAddEditTransactionBinding? = null
    private val binding get() = _binding!!

    private val args: AddEditTransactionFragmentArgs by navArgs()
    private lateinit var viewModel: FinanceViewModel
    private lateinit var repository: LifeLedgerRepository

    private var selectedDate: Long = System.currentTimeMillis()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    // Category data
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
        
        // Check if it's edit mode
        if (!args.transactionId.isNullOrEmpty()) {
            isEditMode = true
            loadTransactionForEdit(args.transactionId!!)
        } else {
            // Add mode, set default values
            updateCategorySpinner(Transaction.TransactionType.EXPENSE)
        }
    }

    /**
     * Initialize Repository
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
     * Initialize ViewModel
     */
    private fun setupViewModel() {
        try {
            val database = AppDatabase.getDatabase(requireContext())
            val transactionRepository = com.example.life_ledger.data.repository.TransactionRepository(database.transactionDao(), database.budgetDao())
            val factory = FinanceViewModelFactory(transactionRepository)
            viewModel = ViewModelProvider(this, factory)[FinanceViewModel::class.java]
        } catch (e: Exception) {
            // If ViewModel initialization fails, show error and return
            Snackbar.make(
                binding.root, 
                getString(R.string.initialization_failed, e.message), 
                Snackbar.LENGTH_LONG
            ).show()
            
            // Log error
            android.util.Log.e("AddEditTransactionFragment", "ViewModel initialization failed", e)
            
            // Return to previous page
            findNavController().navigateUp()
        }
    }

    /**
     * Load category data
     */
    private fun loadCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.getFinancialCategories().collect { categories ->
                    // Check if Fragment is still active
                    if (!isAdded || _binding == null) {
                        return@collect
                    }
                    
                    allCategories = categories
                    // Update category list based on currently selected transaction type
                    val currentType = if (binding.radioGroupType.checkedRadioButtonId == R.id.radioIncome) {
                        Transaction.TransactionType.INCOME
                    } else {
                        Transaction.TransactionType.EXPENSE
                    }
                    updateCategorySpinner(currentType)
                }
            } catch (e: Exception) {
                // Check if Fragment is still active
                if (isAdded && _binding != null) {
                    Snackbar.make(binding.root, getString(R.string.loading_categories_failed, e.message), Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Setup UI components
     */
    private fun setupUI() {
        binding.apply {
            // Set date display
            updateDateDisplay()
            
            // Set transaction type radio button default selection
            radioGroupType.check(R.id.radioExpense)
            
            // Set title
            toolbarAddEdit.title = if (isEditMode) getString(R.string.edit_transaction) else getString(R.string.add_record)
        }
    }

    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        binding.apply {
            // Back button
            toolbarAddEdit.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            // Date selection
            layoutDate.setOnClickListener {
                showDatePicker()
            }

            // Transaction type toggle
            radioGroupType.setOnCheckedChangeListener { _, checkedId ->
                val type = if (checkedId == R.id.radioIncome) {
                    Transaction.TransactionType.INCOME
                } else {
                    Transaction.TransactionType.EXPENSE
                }
                updateCategorySpinner(type)
            }

            // Save button
            buttonSave.setOnClickListener {
                // Check if Fragment is still active
                if (!isAdded || isDetached || activity == null || activity?.isFinishing == true) {
                    android.util.Log.w("AddEditTransactionFragment", "Fragment not active, cannot save")
                    return@setOnClickListener
                }
                saveTransaction()
            }

            // Delete button (only shown in edit mode)
            buttonDelete.setOnClickListener {
                // Check if Fragment is still active
                if (!isAdded || isDetached || activity == null || activity?.isFinishing == true) {
                    android.util.Log.w("AddEditTransactionFragment", "Fragment not active, cannot delete")
                    return@setOnClickListener
                }
                deleteTransaction()
            }

            // Category management button
            buttonManageCategories.setOnClickListener {
                // Navigate to category management page
                findNavController().navigate(R.id.categoryManagerFragment)
            }
        }
    }

    /**
     * Setup data observers
     */
    private fun setupObservers() {
        if (!::viewModel.isInitialized) {
            android.util.Log.e("AddEditTransactionFragment", "ViewModel not initialized, skipping observers setup")
            return
        }
        
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    if (isAdded && _binding != null) {
                        Snackbar.make(binding.root, it.message, Snackbar.LENGTH_SHORT).show()
                    }
                    // Safe navigation back
                    try {
                        if (isAdded && !isDetached && findNavController().currentDestination != null) {
                            findNavController().navigateUp()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AddEditTransactionFragment", "Navigation failed", e)
                        // If navigation fails, try to close Activity directly
                        activity?.finish()
                    }
                } else {
                    if (isAdded && _binding != null) {
                        Snackbar.make(binding.root, it.message, Snackbar.LENGTH_LONG).show()
                    }
                }
                viewModel.clearOperationResult()
            }
        }
    }

    /**
     * Display date picker
     */
    private fun showDatePicker() {
        // Check if Fragment is still active
        if (!isAdded || isDetached || activity == null || activity?.isFinishing == true) {
            android.util.Log.w("AddEditTransactionFragment", "Fragment not active, cannot show date picker")
            return
        }

        try {
            val context = requireContext()
            // Use original context instead of configured context to avoid token problem
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    selectedDate = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }.timeInMillis
                    android.util.Log.d("AddEditTransactionFragment", "User selected date: ${dateFormat.format(Date(selectedDate))}")
                    updateDateDisplay()
                },
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            )
            
            // Check Fragment state again before showing
            if (isAdded && !isDetached && activity != null && activity?.isFinishing != true) {
                datePickerDialog.show()
            }
        } catch (e: Exception) {
            android.util.Log.e("AddEditTransactionFragment", "Error showing date picker", e)
            Snackbar.make(binding.root, "Unable to show date picker", Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * Update date display
     */
    private fun updateDateDisplay() {
        if (_binding == null) return
        binding.tvSelectedDate.text = dateFormat.format(Date(selectedDate))
    }

    /**
     * Update category dropdown menu based on transaction type
     */
    private fun updateCategorySpinner(type: Transaction.TransactionType) {
        currentCategories = allCategories.filter { category ->
            when (type) {
                Transaction.TransactionType.INCOME -> category.isIncomeCategory()
                Transaction.TransactionType.EXPENSE -> category.isExpenseCategory()
            }
        }.filter { it.isActive }

        val categoryNames = currentCategories.map { it.name }
        
        // Check if binding is null
        if (_binding == null) return
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categoryNames
        )
        binding.spinnerCategory.adapter = adapter
    }

    /**
     * Get currently selected category
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
     * Load transaction record to edit
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
                
                // Check if Fragment is still active
                if (!isAdded || _binding == null) {
                    return@launch
                }
                
                editingTransaction?.let { transaction ->
                    binding.apply {
                        // Set amount
                        editAmount.setText(transaction.amount.toString())
                        
                        // Set transaction type
                        val typeRadioId = if (transaction.type == Transaction.TransactionType.INCOME) {
                            R.id.radioIncome
                        } else {
                            R.id.radioExpense
                        }
                        radioGroupType.check(typeRadioId)
                        
                        // Update category dropdown menu
                        updateCategorySpinner(transaction.type)
                        
                        // Set selected category
                        if (!transaction.categoryId.isNullOrEmpty()) {
                            val categoryIndex = currentCategories.indexOfFirst { it.id == transaction.categoryId }
                            if (categoryIndex >= 0) {
                                spinnerCategory.setSelection(categoryIndex)
                            }
                        }
                        
                        // Set date
                        selectedDate = transaction.date
                        updateDateDisplay()
                        
                        // Set tags and description
                        editTags.setText(transaction.getTagsList().joinToString(", "))
                        editDescription.setText(transaction.description ?: "")
                        
                        // Show delete button
                        buttonDelete.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                // Check if Fragment is still active
                if (isAdded && _binding != null) {
                    Snackbar.make(binding.root, getString(R.string.loading_record_failed), Snackbar.LENGTH_LONG).show()
                }
                android.util.Log.e("AddEditTransactionFragment", "Failed to load transaction", e)
            }
        }
    }

    /**
     * Save transaction record
     */
    private fun saveTransaction() {
        if (!::viewModel.isInitialized) {
            android.util.Log.e("AddEditTransactionFragment", "ViewModel not initialized, cannot save transaction")
            Snackbar.make(binding.root, getString(R.string.system_error_cannot_save), Snackbar.LENGTH_LONG).show()
            return
        }
        
        android.util.Log.d("AddEditTransactionFragment", "开始保存交易记录")
        android.util.Log.d("AddEditTransactionFragment", "选择的日期: ${dateFormat.format(Date(selectedDate))}")
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
            // Update existing record
            editingTransaction!!.copy(
                amount = amount,
                type = type,
                categoryId = selectedCategory?.id,
                title = selectedCategory?.name ?: getString(R.string.uncategorized),
                description = description.ifEmpty { null },
                date = selectedDate,
                updatedAt = System.currentTimeMillis()
            ).setTagsList(tagsList)
        } else {
            // Create new record
            Transaction(
                amount = amount,
                type = type,
                categoryId = selectedCategory?.id,
                title = selectedCategory?.name ?: getString(R.string.uncategorized),
                description = description.ifEmpty { null },
                date = selectedDate
            ).setTagsList(tagsList)
        }

        // Update category usage count
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
     * Delete transaction record
     */
    private fun deleteTransaction() {
        if (!::viewModel.isInitialized) {
            android.util.Log.e("AddEditTransactionFragment", "ViewModel not initialized, cannot delete transaction")
            if (isAdded && _binding != null) {
                Snackbar.make(binding.root, "System error, unable to delete", Snackbar.LENGTH_LONG).show()
            }
            return
        }
        
        editingTransaction?.let { transaction ->
            if (!isAdded || isDetached || activity == null || activity?.isFinishing == true) {
                return
            }
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this record?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteTransaction(transaction)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    /**
     * Validate input data
     */
    private fun validateInput(): Boolean {
        binding.apply {
            // Validate amount
            val amountText = editAmount.text.toString().trim()
            if (amountText.isEmpty()) {
                editAmount.error = getString(R.string.please_enter_amount)
                return false
            }

            try {
                val amount = amountText.toDouble()
                if (amount <= 0) {
                    editAmount.error = getString(R.string.please_enter_valid_amount)
                    return false
                }
            } catch (e: NumberFormatException) {
                editAmount.error = getString(R.string.please_enter_valid_amount)
                return false
            }

            // Validate date
            if (selectedDate <= 0) {
                Snackbar.make(root, getString(R.string.please_select_valid_date), Snackbar.LENGTH_SHORT).show()
                return false
            }

            val now = System.currentTimeMillis()
            val oneYearAgo = now - 365L * 24 * 60 * 60 * 1000
            val oneWeekFromNow = now + 7L * 24 * 60 * 60 * 1000 // Allow dates within one week from now

            if (selectedDate < oneYearAgo || selectedDate > oneWeekFromNow) {
                Snackbar.make(root, "The date must be within the range of the past year to the next week", Snackbar.LENGTH_SHORT).show()
                return false
            }

            // Validate category selection
            if (getSelectedCategory() == null) {
                Snackbar.make(root, getString(R.string.please_select_category), Snackbar.LENGTH_SHORT).show()
                return false
            }

            // Clear previous error prompts
            editAmount.error = null
        }

        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 