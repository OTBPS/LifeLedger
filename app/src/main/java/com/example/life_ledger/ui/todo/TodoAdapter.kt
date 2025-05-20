package com.example.life_ledger.ui.todo

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.life_ledger.R
import com.example.life_ledger.data.model.TodoItem
import com.example.life_ledger.databinding.ItemTodoBinding
import com.example.life_ledger.utils.DateUtils

/**
 * 待办事项列表适配器
 * 使用DiffUtil优化性能，支持数据绑定
 */
class TodoAdapter(
    private val onItemClick: (TodoItem) -> Unit,
    private val onItemLongClick: (TodoItem) -> Unit,
    private val onCheckboxClick: (TodoItem) -> Unit,
    private val onMoreClick: (TodoItem, View) -> Unit
) : ListAdapter<TodoItem, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding: ItemTodoBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_todo,
            parent,
            false
        )
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TodoViewHolder(
        private val binding: ItemTodoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(todo: TodoItem) {
            binding.todo = todo
            
            setupViews(todo)
            setupClickListeners(todo)
            
            binding.executePendingBindings()
        }

        private fun setupViews(todo: TodoItem) {
            binding.apply {
                // 设置完成状态样式
                updateCompletionStyle(todo.isCompleted)
                
                // 设置优先级指示器颜色
                updatePriorityIndicator(todo.priority)
                
                // 设置优先级标签
                updatePriorityTag(todo.priority)
                
                // 设置描述可见性
                updateDescriptionVisibility(todo.description)
                
                // 设置进度条
                updateProgressBar(todo.progress)
                
                // 设置截止时间
                updateDueDate(todo)
                
                // 设置提醒指示器
                updateReminderIndicator(todo.isReminderEnabled)
                
                // 设置分类标签
                updateCategoryTag(todo.categoryId)
            }
        }

        private fun updateCompletionStyle(isCompleted: Boolean) {
            binding.apply {
                if (isCompleted) {
                    tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    tvTitle.alpha = 0.6f
                    tvDescription.alpha = 0.6f
                } else {
                    tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    tvTitle.alpha = 1.0f
                    tvDescription.alpha = 1.0f
                }
            }
        }

        private fun updatePriorityIndicator(priority: TodoItem.Priority) {
            val color = when (priority) {
                TodoItem.Priority.LOW -> R.color.md_theme_success
                TodoItem.Priority.MEDIUM -> R.color.md_theme_primary
                TodoItem.Priority.HIGH -> R.color.md_theme_warning
                TodoItem.Priority.URGENT -> R.color.md_theme_error
            }
            binding.viewPriorityIndicator.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, color)
            )
        }

        private fun updatePriorityTag(priority: TodoItem.Priority) {
            binding.apply {
                tvPriority.text = priority.displayName
                val backgroundColor = when (priority) {
                    TodoItem.Priority.LOW -> R.color.md_theme_success_container
                    TodoItem.Priority.MEDIUM -> R.color.md_theme_primary_container
                    TodoItem.Priority.HIGH -> R.color.md_theme_warning_container
                    TodoItem.Priority.URGENT -> R.color.md_theme_error_container
                }
                val textColor = when (priority) {
                    TodoItem.Priority.LOW -> R.color.md_theme_on_success_container
                    TodoItem.Priority.MEDIUM -> R.color.md_theme_on_primary_container
                    TodoItem.Priority.HIGH -> R.color.md_theme_on_warning_container
                    TodoItem.Priority.URGENT -> R.color.md_theme_on_error_container
                }
                tvPriority.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, backgroundColor)
                )
                tvPriority.setTextColor(
                    ContextCompat.getColor(binding.root.context, textColor)
                )
            }
        }

        private fun updateDescriptionVisibility(description: String?) {
            binding.tvDescription.visibility = if (description.isNullOrBlank()) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        private fun updateProgressBar(progress: Int) {
            binding.apply {
                if (progress > 0) {
                    layoutProgress.visibility = View.VISIBLE
                    progressBar.progress = progress
                    tvProgress.text = "${progress}%"
                } else {
                    layoutProgress.visibility = View.GONE
                }
            }
        }

        private fun updateDueDate(todo: TodoItem) {
            binding.apply {
                if (todo.dueDate != null) {
                    layoutDueDate.visibility = View.VISIBLE
                    tvDueDate.text = DateUtils.formatRelativeDate(todo.dueDate)
                    
                    // 设置过期或即将到期的颜色
                    val textColor = when {
                        todo.isOverdue() -> R.color.md_theme_error
                        todo.isDueSoon() -> R.color.md_theme_warning
                        else -> R.color.md_theme_on_surface_variant
                    }
                    tvDueDate.setTextColor(
                        ContextCompat.getColor(binding.root.context, textColor)
                    )
                } else {
                    layoutDueDate.visibility = View.GONE
                }
            }
        }

        private fun updateReminderIndicator(isReminderEnabled: Boolean) {
            binding.iconReminder.visibility = if (isReminderEnabled) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        private fun updateCategoryTag(categoryId: String?) {
            binding.apply {
                if (categoryId != null) {
                    tvCategory.visibility = View.VISIBLE
                    // 这里可以通过回调获取分类名称
                    // 暂时显示分类ID，后续可以优化
                    tvCategory.text = "分类"
                } else {
                    tvCategory.visibility = View.GONE
                }
            }
        }

        private fun setupClickListeners(todo: TodoItem) {
            binding.apply {
                // 整个项目点击
                root.setOnClickListener {
                    onItemClick(todo)
                }
                
                // 长按
                root.setOnLongClickListener {
                    onItemLongClick(todo)
                    true
                }
                
                // 复选框点击
                checkboxCompleted.setOnClickListener {
                    onCheckboxClick(todo)
                }
                
                // 更多操作按钮点击
                btnMore.setOnClickListener {
                    onMoreClick(todo, it)
                }
            }
        }
    }

    /**
     * DiffUtil回调，用于高效更新列表
     */
    private class TodoDiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem == newItem
        }
    }
} 