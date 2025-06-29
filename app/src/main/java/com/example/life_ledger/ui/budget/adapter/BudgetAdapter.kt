package com.example.life_ledger.ui.budget.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.life_ledger.R
import com.example.life_ledger.data.model.Budget
import com.example.life_ledger.databinding.ItemBudgetBinding
import java.text.SimpleDateFormat
import java.util.*
import java.text.NumberFormat

/**
 * 预算列表适配器
 * 用于显示预算项目和提供操作功能
 */
class BudgetAdapter(
    private val onBudgetClick: (Budget) -> Unit,
    private val onEditClick: (Budget) -> Unit,
    private val onDeleteClick: (Budget) -> Unit,
    private val onToggleClick: (Budget, Boolean) -> Unit
) : ListAdapter<Budget, BudgetAdapter.BudgetViewHolder>(BudgetDiffCallback()) {
    
    private val dateFormat = SimpleDateFormat("MM/dd", Locale.ENGLISH)
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemBudgetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BudgetViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class BudgetViewHolder(
        private val binding: ItemBudgetBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(budget: Budget) {
            with(binding) {
                // 基本信息
                tvBudgetName.text = budget.name
                tvBudgetDescription.text = budget.description ?: "No description"
                
                // 预算金额和已花费
                tvBudgetAmount.text = numberFormat.format(budget.amount)
                tvSpentAmount.text = numberFormat.format(budget.spent)
                tvRemainingAmount.text = numberFormat.format(budget.getRemainingAmount())
                
                // 花费百分比
                val spentPercentage = budget.getSpentPercentage()
                tvSpentPercentage.text = "${String.format("%.1f", spentPercentage)}%"
                progressBudget.progress = spentPercentage.toInt()
                
                // 预算周期
                tvBudgetPeriod.text = budget.period.displayName
                
                // 日期范围
                val startDate = dateFormat.format(Date(budget.startDate))
                val endDate = dateFormat.format(Date(budget.endDate))
                tvDateRange.text = "$startDate - $endDate"
                
                // Remaining days
                val remainingDays = budget.getRemainingDays()
                tvRemainingDays.text = if (remainingDays > 0) {
                    "$remainingDays days"
                } else {
                    "Expired"
                }
                
                // 预算状态
                val status = budget.getBudgetStatus()
                val statusText = budget.getStatusText()
                tvBudgetStatus.text = statusText
                
                // 根据状态设置颜色
                val context = binding.root.context
                when (status) {
                    Budget.BudgetStatus.SAFE -> {
                        val color = ContextCompat.getColor(context, R.color.success)
                        tvBudgetStatus.setTextColor(color)
                        progressBudget.progressTintList = android.content.res.ColorStateList.valueOf(color)
                        cardBudget.strokeColor = color
                    }
                    Budget.BudgetStatus.WARNING -> {
                        val color = ContextCompat.getColor(context, R.color.warning)
                        tvBudgetStatus.setTextColor(color)
                        progressBudget.progressTintList = android.content.res.ColorStateList.valueOf(color)
                        cardBudget.strokeColor = color
                    }
                    Budget.BudgetStatus.EXCEEDED -> {
                        val color = ContextCompat.getColor(context, R.color.error)
                        tvBudgetStatus.setTextColor(color)
                        progressBudget.progressTintList = android.content.res.ColorStateList.valueOf(color)
                        cardBudget.strokeColor = color
                    }
                    Budget.BudgetStatus.EXPIRED -> {
                        val color = ContextCompat.getColor(context, R.color.text_secondary)
                        tvBudgetStatus.setTextColor(color)
                        progressBudget.progressTintList = android.content.res.ColorStateList.valueOf(color)
                        cardBudget.strokeColor = color
                        // 设置整个卡片透明度
                        cardBudget.alpha = 0.6f
                    }
                }
                
                // 如果未过期，恢复正常透明度
                if (status != Budget.BudgetStatus.EXPIRED) {
                    cardBudget.alpha = 1.0f
                }
                
                // 是否激活开关
                switchActive.isChecked = budget.isActive
                switchActive.setOnCheckedChangeListener { _, isChecked ->
                    onToggleClick(budget, isChecked)
                }
                
                // 是否定期预算标识
                if (budget.isRecurring) {
                    ivRecurring.visibility = android.view.View.VISIBLE
                } else {
                    ivRecurring.visibility = android.view.View.GONE
                }
                
                // 警告阈值指示器
                val thresholdPosition = (budget.alertThreshold * 100).toInt()
                progressBudget.secondaryProgress = thresholdPosition
                
                // 点击事件
                cardBudget.setOnClickListener {
                    onBudgetClick(budget)
                }
                
                // 编辑按钮
                btnEdit.setOnClickListener {
                    onEditClick(budget)
                }
                
                // 删除按钮
                btnDelete.setOnClickListener {
                    onDeleteClick(budget)
                }
                
                // 设置菜单按钮
                btnMore.setOnClickListener { view ->
                    showContextMenu(view, budget)
                }
                
                // Display category information based on budget type
                if (budget.categoryId != null) {
                    // Here we can get category name by categoryId
                    // Temporarily display ID
                    tvCategoryInfo.text = "Category Budget"
                    tvCategoryInfo.visibility = android.view.View.VISIBLE
                } else {
                    tvCategoryInfo.text = "Total Budget"
                    tvCategoryInfo.visibility = android.view.View.VISIBLE
                }
            }
        }
        
        private fun showContextMenu(view: android.view.View, budget: Budget) {
            val popup = androidx.appcompat.widget.PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.budget_item_menu, popup.menu)
            
            // Set menu item visibility based on budget status
            popup.menu.findItem(R.id.action_reset_budget)?.isVisible = budget.isExpired() && budget.isRecurring
            popup.menu.findItem(R.id.action_archive_budget)?.isVisible = budget.isExpired()
            
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit_budget -> {
                        onEditClick(budget)
                        true
                    }
                    R.id.action_duplicate_budget -> {
                        // Budget duplication feature can be implemented here
                        true
                    }
                    R.id.action_reset_budget -> {
                        // Budget reset feature can be implemented here
                        true
                    }
                    R.id.action_archive_budget -> {
                        onToggleClick(budget, false)
                        true
                    }
                    R.id.action_delete_budget -> {
                        onDeleteClick(budget)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
    
    /**
     * DiffCallback for efficient list updates
     */
    private class BudgetDiffCallback : DiffUtil.ItemCallback<Budget>() {
        override fun areItemsTheSame(oldItem: Budget, newItem: Budget): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Budget, newItem: Budget): Boolean {
            return oldItem == newItem
        }
    }
} 