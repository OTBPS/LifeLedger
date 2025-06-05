package com.example.life_ledger.ui.finance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.life_ledger.R
import com.example.life_ledger.data.model.Transaction
import com.example.life_ledger.databinding.ItemTransactionBinding
import com.example.life_ledger.databinding.ItemDateHeaderBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * 财务记录列表适配器
 * 使用DiffUtil优化性能，支持点击编辑
 */
class TransactionListAdapter(
    private val onItemClick: (Transaction) -> Unit,
    private val onItemLongClick: (Transaction) -> Unit = {}
) : ListAdapter<Transaction, TransactionListAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MM-dd", Locale.ENGLISH)
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding: ItemTransactionBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_transaction,
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                // 设置金额和颜色
                val amountText = if (transaction.type == Transaction.TransactionType.INCOME) {
                    "+${numberFormat.format(transaction.amount)}"
                } else {
                    "-${numberFormat.format(transaction.amount)}"
                }
                tvAmount.text = amountText
                
                val amountColor = if (transaction.type == Transaction.TransactionType.INCOME) {
                    root.context.getColor(R.color.md_theme_success)
                } else {
                    root.context.getColor(R.color.md_theme_error)
                }
                tvAmount.setTextColor(amountColor)

                // 设置分类（使用title字段）
                tvCategory.text = transaction.title

                // 设置描述
                val description = transaction.description
                if (!description.isNullOrEmpty()) {
                    tvDescription.text = description
                    tvDescription.visibility = android.view.View.VISIBLE
                } else {
                    tvDescription.visibility = android.view.View.GONE
                }

                // 设置日期
                tvDate.text = dateFormat.format(Date(transaction.date))

                // 设置标签
                val tags = transaction.getTagsList()
                if (tags.isNotEmpty()) {
                    tvTags.text = tags.joinToString(" • ")
                    tvTags.visibility = android.view.View.VISIBLE
                } else {
                    tvTags.visibility = android.view.View.GONE
                }

                // 设置类型图标
                val iconRes = if (transaction.type == Transaction.TransactionType.INCOME) {
                    R.drawable.ic_add_circle
                } else {
                    R.drawable.ic_remove_circle
                }
                ivTypeIcon.setImageResource(iconRes)
                
                val iconTint = if (transaction.type == Transaction.TransactionType.INCOME) {
                    root.context.getColor(R.color.md_theme_success)
                } else {
                    root.context.getColor(R.color.md_theme_error)
                }
                ivTypeIcon.setColorFilter(iconTint)

                // 设置点击事件
                root.setOnClickListener {
                    onItemClick(transaction)
                }

                root.setOnLongClickListener {
                    onItemLongClick(transaction)
                    true
                }

                // 设置transaction数据
                this.transaction = transaction
                executePendingBindings()
            }
        }
    }

    /**
     * DiffUtil回调，用于高效更新列表
     */
    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * 交易记录分组数据类
 * 用于按日期分组显示
 */
data class TransactionGroup(
    val date: String,
    val transactions: List<Transaction>,
    val totalIncome: Double,
    val totalExpense: Double
) {
    val netAmount: Double get() = totalIncome - totalExpense
}

/**
 * 分组适配器（可选，用于按日期分组显示）
 */
class GroupedTransactionAdapter(
    private val onItemClick: (Transaction) -> Unit,
    private val onItemLongClick: (Transaction) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DATE_HEADER = 0
        private const val TYPE_TRANSACTION = 1
    }

    private var groups: List<TransactionGroup> = emptyList()
    private var flatItems: List<Any> = emptyList()

    fun submitList(newGroups: List<TransactionGroup>) {
        groups = newGroups
        flatItems = groups.flatMap { group ->
            listOf(group.date) + group.transactions
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (flatItems[position]) {
            is String -> TYPE_DATE_HEADER
            is Transaction -> TYPE_TRANSACTION
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE_HEADER -> {
                val binding: ItemDateHeaderBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_date_header,
                    parent,
                    false
                )
                DateHeaderViewHolder(binding)
            }
            TYPE_TRANSACTION -> {
                val binding: ItemTransactionBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_transaction,
                    parent,
                    false
                )
                TransactionViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DateHeaderViewHolder -> {
                val date = flatItems[position] as String
                val group = groups.find { it.date == date }
                holder.bind(date, group)
            }
            is TransactionViewHolder -> {
                val transaction = flatItems[position] as Transaction
                holder.bind(transaction)
            }
        }
    }

    override fun getItemCount(): Int = flatItems.size

    inner class DateHeaderViewHolder(
        private val binding: ItemDateHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(date: String, group: TransactionGroup?) {
            binding.apply {
                tvDate.text = date
                group?.let {
                    val netAmount = it.netAmount
                    val netText = if (netAmount >= 0) {
                        "+${NumberFormat.getCurrencyInstance(Locale.CHINA).format(netAmount)}"
                    } else {
                        NumberFormat.getCurrencyInstance(Locale.CHINA).format(netAmount)
                    }
                    tvNetAmount.text = netText
                    
                    val netColor = if (netAmount >= 0) {
                        root.context.getColor(R.color.md_theme_success)
                    } else {
                        root.context.getColor(R.color.md_theme_error)
                    }
                    tvNetAmount.setTextColor(netColor)
                }
                executePendingBindings()
            }
        }
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            // 与上面的TransactionViewHolder.bind相同的实现
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val numberFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
            
            binding.apply {
                // 设置金额和颜色
                val amountText = if (transaction.type == Transaction.TransactionType.INCOME) {
                    "+${numberFormat.format(transaction.amount)}"
                } else {
                    "-${numberFormat.format(transaction.amount)}"
                }
                tvAmount.text = amountText
                
                val amountColor = if (transaction.type == Transaction.TransactionType.INCOME) {
                    root.context.getColor(R.color.md_theme_success)
                } else {
                    root.context.getColor(R.color.md_theme_error)
                }
                tvAmount.setTextColor(amountColor)

                // 其他设置与上面相同...
                tvCategory.text = transaction.title
                tvDate.text = dateFormat.format(Date(transaction.date))
                
                // 设置点击事件
                root.setOnClickListener { onItemClick(transaction) }
                root.setOnLongClickListener { 
                    onItemLongClick(transaction)
                    true
                }
                
                executePendingBindings()
            }
        }
    }
} 