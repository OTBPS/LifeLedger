package com.example.life_ledger.ui.budget

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.life_ledger.R
import com.example.life_ledger.databinding.FragmentBudgetBinding
import com.example.life_ledger.ui.budget.adapter.BudgetAdapter
import com.example.life_ledger.ui.budget.dialog.AddEditBudgetDialog
import com.example.life_ledger.ui.budget.viewmodel.BudgetViewModel
import com.example.life_ledger.ui.budget.viewmodel.BudgetOverview
import com.example.life_ledger.data.model.Budget
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * 预算管理主界面
 * 显示所有预算、预算状态概览和预算管理功能
 */
class BudgetFragment : Fragment() {
    
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BudgetViewModel by viewModels()
    private lateinit var budgetAdapter: BudgetAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // 加载数据
        viewModel.loadBudgets()
    }
    
    private fun setupUI() {
        // 设置工具栏
        binding.toolbar.title = "预算管理"
        
        // 设置刷新
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadBudgets()
        }
    }
    
    private fun setupRecyclerView() {
        budgetAdapter = BudgetAdapter(
            onBudgetClick = { budget ->
                navigateToBudgetDetail(budget)
            },
            onEditClick = { budget ->
                showEditBudgetDialog(budget)
            },
            onDeleteClick = { budget ->
                showDeleteConfirmation(budget)
            },
            onToggleClick = { budget, isActive ->
                viewModel.toggleBudgetActive(budget)
            }
        )
        
        binding.recyclerViewBudgets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = budgetAdapter
        }
    }
    
    private fun setupObservers() {
        // 观察预算列表
        viewModel.budgets.observe(viewLifecycleOwner) { budgets ->
            budgetAdapter.submitList(budgets)
            updateEmptyState(budgets.isEmpty())
        }
        
        // 观察预算概览
        viewModel.budgetOverview.observe(viewLifecycleOwner) { overview ->
            overview?.let { updateOverviewCards(it) }
        }
        
        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }
        
        // 观察错误信息
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }
    }
    
    private fun setupClickListeners() {
        // 添加预算按钮
        binding.fabAddBudget.setOnClickListener {
            showAddBudgetDialog()
        }
        
        // 下拉刷新
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadBudgets()
        }
    }
    
    private fun updateOverviewCards(overview: BudgetOverview) {
        // 简化实现，只更新存在的视图
        // TODO: 根据实际布局文件中的视图ID来更新
    }
    
    private fun showAddBudgetDialog() {
        val dialog = AddEditBudgetDialog.newInstance()
        dialog.show(childFragmentManager, "AddBudgetDialog")
    }
    
    private fun showEditBudgetDialog(budget: Budget) {
        val dialog = AddEditBudgetDialog.newInstance(budget)
        dialog.show(childFragmentManager, "EditBudgetDialog")
    }
    
    private fun showDeleteConfirmation(budget: Budget) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("删除预算")
            .setMessage("确定要删除预算 \"${budget.name}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteBudget(budget)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun navigateToBudgetDetail(budget: Budget) {
        // TODO: 实现预算详情导航
    }
    
    private fun navigateToBudgetAnalysis() {
        // TODO: 实现预算分析导航
    }
    
    private fun navigateToBudgetSettings() {
        // TODO: 实现预算设置导航
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerViewBudgets.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.recyclerViewBudgets.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
        }
    }
    
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
    
    private fun showBudgetOptionsDialog(budget: Budget) {
        // Implementation of showBudgetOptionsDialog method
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 