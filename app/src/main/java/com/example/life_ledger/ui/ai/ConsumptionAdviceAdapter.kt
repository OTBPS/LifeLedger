package com.example.life_ledger.ui.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.life_ledger.R
import com.example.life_ledger.data.service.ConsumptionAdvice
import com.example.life_ledger.databinding.ItemConsumptionAdviceBinding

/**
 * 消费建议适配器
 */
class ConsumptionAdviceAdapter : ListAdapter<ConsumptionAdvice, ConsumptionAdviceAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemConsumptionAdviceBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_consumption_advice,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemConsumptionAdviceBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(advice: ConsumptionAdvice) {
            binding.apply {
                textAdviceTitle.text = advice.title
                textAdviceDescription.text = advice.description
                textAdviceEffect.text = "预期效果：${advice.expectedEffect}"
                textAdviceDifficulty.text = advice.difficulty
                textAdvicePriority.text = "优先级：${advice.priority}"
                
                // 根据难度设置不同的颜色
                val difficultyColor = when (advice.difficulty) {
                    "简单" -> ContextCompat.getColor(root.context, R.color.md_theme_success)
                    "中等" -> ContextCompat.getColor(root.context, R.color.md_theme_warning)
                    "困难" -> ContextCompat.getColor(root.context, R.color.md_theme_error)
                    else -> ContextCompat.getColor(root.context, R.color.md_theme_on_surface_variant)
                }
                textAdviceDifficulty.setTextColor(difficultyColor)
                
                // 根据优先级设置背景颜色
                val priorityColor = when {
                    advice.priority <= 2 -> ContextCompat.getColor(root.context, R.color.priority_high_background)
                    advice.priority <= 4 -> ContextCompat.getColor(root.context, R.color.priority_medium_background)
                    else -> ContextCompat.getColor(root.context, R.color.priority_low_background)
                }
                cardAdvice.setCardBackgroundColor(priorityColor)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ConsumptionAdvice>() {
        override fun areItemsTheSame(oldItem: ConsumptionAdvice, newItem: ConsumptionAdvice): Boolean {
            return oldItem.title == newItem.title && oldItem.priority == newItem.priority
        }

        override fun areContentsTheSame(oldItem: ConsumptionAdvice, newItem: ConsumptionAdvice): Boolean {
            return oldItem == newItem
        }
    }
} 