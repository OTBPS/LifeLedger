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
        
        // 第一个预算按钮（当没有预算时显示）
        binding.btnAddFirstBudget?.setOnClickListener {
            showAddBudgetDialog()
        }
        
        // 预算分析按钮
        binding.btnBudgetAnalysis?.setOnClickListener {
            navigateToBudgetAnalysis()
        }
        
        // 预算设置按钮
        binding.btnBudgetSettings?.setOnClickListener {
            navigateToBudgetSettings()
        }
        
        // 下拉刷新
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadBudgets()
        }
    }
    
    private fun updateOverviewCards(overview: com.example.life_ledger.data.dao.BudgetOverview) {
        // 更新预算概览卡片的数据
        try {
            // 更新总预算数量
            binding.tvTotalBudgets?.text = overview.totalCount.toString()
            
            // 更新活跃预算数量 - 这个字段在DAO版本中不存在，暂时使用总数
            binding.tvActiveBudgets?.text = overview.totalCount.toString()
            
            // 更新超支预算数量
            binding.tvOverspentCount?.text = overview.overspentCount.toString()
            
            // 更新使用率
            val usageRate = if (overview.totalAmount > 0) {
                (overview.totalSpent / overview.totalAmount * 100).toInt()
            } else 0
            binding.tvUsageRate?.text = "${usageRate}%"
            
            // 更新进度条
            binding.progressUsage?.progress = usageRate
            
            // 更新总预算金额
            binding.tvTotalBudgetAmount?.text = String.format("¥%.2f", overview.totalAmount)
            
            // 更新已支出金额
            binding.tvTotalSpentAmount?.text = String.format("¥%.2f", overview.totalSpent)
            
            // 更新剩余金额
            binding.tvRemainingAmount?.text = String.format("¥%.2f", overview.remainingAmount)
            
        } catch (e: Exception) {
            // 忽略视图更新错误，可能某些视图在布局中不存在
        }
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
        // 暂时显示预算信息
        showBudgetInfo(budget)
    }
    
    private fun navigateToBudgetAnalysis() {
        try {
            findNavController().navigate(R.id.action_budgetFragment_to_budgetAnalysisFragment)
        } catch (e: Exception) {
            showError("导航到预算分析失败")
        }
    }
    
    private fun navigateToBudgetSettings() {
        try {
            // 导航到预算设置页面
            findNavController().navigate(R.id.action_budgetFragment_to_budgetSettingsFragment)
        } catch (e: Exception) {
            showError("跳转到预算设置失败")
        }
    }
    
    private fun showBudgetInfo(budget: Budget) {
        val message = buildString {
            appendLine("预算名称: ${budget.name}")
            appendLine("预算金额: ¥${String.format("%.2f", budget.amount)}")
            appendLine("已花费: ¥${String.format("%.2f", budget.spent)}")
            appendLine("剩余金额: ¥${String.format("%.2f", budget.getRemainingAmount())}")
            appendLine("使用率: ${String.format("%.1f", budget.getSpentPercentage())}%")
            appendLine("预算周期: ${budget.period.displayName}")
            appendLine("状态: ${budget.getStatusText()}")
            if (!budget.description.isNullOrBlank()) {
                appendLine("描述: ${budget.description}")
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("预算详情")
            .setMessage(message)
            .setPositiveButton("编辑") { _, _ ->
                showEditBudgetDialog(budget)
            }
            .setNegativeButton("关闭", null)
            .show()
    }
    
    private fun showBudgetSettingsDialog() {
        val options = arrayOf("筛选预算", "预算提醒设置", "预算统计偏好")
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("预算设置")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showFilterOptions()
                    1 -> showNotificationSettings()
                    2 -> showStatisticsPreferences()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showFilterOptions() {
        val periods = Budget.BudgetPeriod.values()
        val periodNames = periods.map { it.displayName }.toTypedArray()
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("按周期筛选")
            .setItems(periodNames) { _, which ->
                viewModel.setFilterPeriod(periods[which])
            }
            .setNeutralButton("清除筛选") { _, _ ->
                viewModel.clearFilters()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showNotificationSettings() {
        showError("预算提醒设置功能开发中")
    }
    
    private fun showStatisticsPreferences() {
        showError("统计偏好设置功能开发中")
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