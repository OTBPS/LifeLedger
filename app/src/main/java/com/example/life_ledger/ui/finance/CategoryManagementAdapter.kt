package com.example.life_ledger.ui.finance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.life_ledger.R
import com.example.life_ledger.data.model.Category
import com.example.life_ledger.databinding.ItemCategoryManagementBinding

/**
 * 分类管理列表适配器
 * 显示分类列表，支持编辑、删除、启用/禁用操作
 */
class CategoryManagementAdapter(
    private val onItemClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit,
    private val onToggleActive: (Category) -> Unit
) : ListAdapter<Category, CategoryManagementAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding: ItemCategoryManagementBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_category_management,
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryManagementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.apply {
                // 设置分类名称
                tvCategoryName.text = category.name

                // 设置分类类型显示
                val typeText = when {
                    category.isIncomeCategory() -> "收入"
                    category.isExpenseCategory() -> "支出"
                    else -> "其他"
                }
                tvCategoryType.text = typeText

                // 设置分类颜色
                try {
                    val color = Color.parseColor(category.color)
                    viewColorIndicator.setBackgroundColor(color)
                    ivCategoryIcon.setColorFilter(color)
                } catch (e: Exception) {
                    // 如果颜色解析失败，使用默认颜色
                    val defaultColor = root.context.getColor(R.color.md_theme_primary)
                    viewColorIndicator.setBackgroundColor(defaultColor)
                    ivCategoryIcon.setColorFilter(defaultColor)
                }

                // 设置图标
                val iconRes = getCategoryIconResource(category.icon)
                ivCategoryIcon.setImageResource(iconRes)

                // 设置描述
                if (!category.description.isNullOrEmpty()) {
                    tvCategoryDescription.text = category.description
                    tvCategoryDescription.visibility = android.view.View.VISIBLE
                } else {
                    tvCategoryDescription.visibility = android.view.View.GONE
                }

                // 设置使用次数
                tvUsageCount.text = "使用 ${category.usageCount} 次"

                // 设置系统分类标识
                if (category.isSystemCategory) {
                    tvSystemCategory.visibility = android.view.View.VISIBLE
                } else {
                    tvSystemCategory.visibility = android.view.View.GONE
                }

                // 设置启用状态
                switchActive.isChecked = category.isActive
                
                // 根据启用状态调整透明度
                root.alpha = if (category.isActive) 1.0f else 0.6f

                // 设置预算信息
                if (category.budgetLimit != null && category.budgetLimit > 0) {
                    tvBudgetLimit.text = "预算: ¥${String.format("%.2f", category.budgetLimit)}"
                    tvBudgetLimit.visibility = android.view.View.VISIBLE
                } else {
                    tvBudgetLimit.visibility = android.view.View.GONE
                }

                // 设置点击事件
                root.setOnClickListener {
                    onItemClick(category)
                }

                // 设置删除按钮
                buttonDelete.setOnClickListener {
                    onDeleteClick(category)
                }

                // 设置启用开关
                switchActive.setOnCheckedChangeListener { _, _ ->
                    onToggleActive(category)
                }

                // 如果是系统分类，隐藏删除按钮
                if (category.isSystemCategory) {
                    buttonDelete.visibility = android.view.View.GONE
                } else {
                    buttonDelete.visibility = android.view.View.VISIBLE
                }

                // 设置category数据绑定
                this.category = category
                executePendingBindings()
            }
        }

        /**
         * 根据图标名称获取资源ID
         */
        private fun getCategoryIconResource(iconName: String): Int {
            return when (iconName) {
                "trending_up" -> R.drawable.ic_trending_up
                "attach_money" -> R.drawable.ic_money
                "restaurant" -> R.drawable.ic_restaurant
                "directions_car" -> R.drawable.ic_car
                else -> R.drawable.ic_category
            }
        }
    }

    /**
     * DiffUtil回调，用于高效更新列表
     */
    private class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
} 