package com.example.life_ledger.ui.budget.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.life_ledger.R
import com.example.life_ledger.databinding.ItemBudgetRecommendationBinding
import com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation

/**
 * 预算建议列表适配器
 */
class BudgetRecommendationAdapter : ListAdapter<BudgetRecommendation, BudgetRecommendationAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBudgetRecommendationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemBudgetRecommendationBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(recommendation: BudgetRecommendation) {
            with(binding) {
                tvTitle.text = recommendation.title
                tvDescription.text = recommendation.description
                
                // 设置图标
                val iconRes = when (recommendation.type) {
                    BudgetRecommendation.RecommendationType.OVERSPENDING -> R.drawable.ic_warning
                    BudgetRecommendation.RecommendationType.BUDGET_ADJUSTMENT -> R.drawable.ic_analytics
                    BudgetRecommendation.RecommendationType.SAVINGS_OPPORTUNITY -> R.drawable.ic_lightbulb
                    BudgetRecommendation.RecommendationType.CATEGORY_OPTIMIZATION -> R.drawable.ic_analytics
                    BudgetRecommendation.RecommendationType.SPENDING_PATTERN -> R.drawable.ic_analytics
                    BudgetRecommendation.RecommendationType.GOAL_SETTING -> R.drawable.ic_lightbulb
                }
                ivIcon.setImageResource(iconRes)
                
                // 设置优先级指示器
                val priorityColor = when (recommendation.priority) {
                    BudgetRecommendation.Priority.HIGH -> R.color.error
                    BudgetRecommendation.Priority.MEDIUM -> R.color.warning
                    BudgetRecommendation.Priority.LOW -> R.color.info
                }
                ivPriority.setColorFilter(itemView.context.getColor(priorityColor))
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<BudgetRecommendation>() {
        override fun areItemsTheSame(oldItem: BudgetRecommendation, newItem: BudgetRecommendation): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: BudgetRecommendation, newItem: BudgetRecommendation): Boolean {
            return oldItem == newItem
        }
    }
} 