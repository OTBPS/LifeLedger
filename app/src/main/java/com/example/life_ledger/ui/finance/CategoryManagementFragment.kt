package com.example.life_ledger.ui.finance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.life_ledger.R
import com.example.life_ledger.data.database.AppDatabase
import com.example.life_ledger.data.model.Category
import com.example.life_ledger.data.repository.LifeLedgerRepository
import com.example.life_ledger.databinding.FragmentCategoryManagementBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * 分类管理界面
 * 用于管理收入和支出的分类
 */
class CategoryManagementFragment : Fragment() {

    private var _binding: FragmentCategoryManagementBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CategoryManagementViewModel
    private lateinit var categoryAdapter: CategoryManagementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_category_management,
            container,
            false
        )
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
        setupObservers()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val repository = LifeLedgerRepository(
            database.transactionDao(),
            database.todoDao(),
            database.categoryDao(),
            database.budgetDao(),
            database.userSettingsDao()
        )
        val factory = CategoryManagementViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CategoryManagementViewModel::class.java]
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryManagementAdapter(
            onItemClick = { category ->
                showEditCategoryDialog(category)
            },
            onDeleteClick = { category ->
                showDeleteConfirmDialog(category)
            },
            onToggleActive = { category ->
                viewModel.toggleCategoryActive(category)
            }
        )
        
        binding.recyclerViewCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            // 返回按钮
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            // 添加分类按钮
            fabAddCategory.setOnClickListener {
                showAddCategoryDialog()
            }

            // 筛选标签
            chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
                val filter = when (checkedIds.firstOrNull()) {
                    R.id.chipAllCategories -> CategoryFilter.ALL
                    R.id.chipIncomeCategories -> CategoryFilter.INCOME
                    R.id.chipExpenseCategories -> CategoryFilter.EXPENSE
                    else -> CategoryFilter.ALL
                }
                viewModel.setFilter(filter)
            }
        }
    }

    private fun setupObservers() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.submitList(categories)
            updateEmptyState(categories.isEmpty())
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                Snackbar.make(
                    binding.root,
                    if (it.isSuccess) it.message else "操作失败: ${it.message}",
                    Snackbar.LENGTH_SHORT
                ).show()
                viewModel.clearOperationResult()
            }
        }
    }

    private fun showAddCategoryDialog() {
        val dialog = AddEditCategoryDialog()
        dialog.setOnCategoryAddedListener { category ->
            viewModel.addCategory(category)
        }
        dialog.show(parentFragmentManager, "AddCategoryDialog")
    }

    private fun showEditCategoryDialog(category: Category) {
        val dialog = AddEditCategoryDialog.newInstance(category)
        dialog.setOnCategoryEditedListener { editedCategory ->
            viewModel.updateCategory(editedCategory)
        }
        dialog.show(parentFragmentManager, "EditCategoryDialog")
    }

    private fun showDeleteConfirmDialog(category: Category) {
        if (!category.canBeDeleted()) {
            Snackbar.make(
                binding.root,
                "系统默认分类不能删除",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("确认删除")
            .setMessage("删除分类「${category.name}」后，相关记录将不再关联该分类。确定要删除吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteCategory(category)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            if (isEmpty) {
                recyclerViewCategories.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
            } else {
                recyclerViewCategories.visibility = View.VISIBLE
                layoutEmpty.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * 分类筛选枚举
 */
enum class CategoryFilter {
    ALL,
    INCOME,
    EXPENSE
} 