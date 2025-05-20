package com.example.life_ledger.ui.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * 基础RecyclerView适配器
 * 使用DiffUtil优化列表更新性能
 */
abstract class BaseAdapter<T : Any, VB : ViewDataBinding>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BaseAdapter.BaseViewHolder<VB>>(diffCallback) {
    
    /**
     * 获取布局资源ID
     */
    abstract fun getLayoutId(): Int
    
    /**
     * 绑定数据到ViewHolder
     */
    abstract fun bind(binding: VB, item: T, position: Int)
    
    /**
     * 可选：处理点击事件
     */
    open fun onItemClick(item: T, position: Int) {}
    
    /**
     * 可选：处理长按事件
     */
    open fun onItemLongClick(item: T, position: Int): Boolean = false
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        val binding = DataBindingUtil.inflate<VB>(
            LayoutInflater.from(parent.context),
            getLayoutId(),
            parent,
            false
        )
        return BaseViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
        val item = getItem(position)
        bind(holder.binding, item, position)
        
        // 设置点击事件
        holder.itemView.setOnClickListener {
            onItemClick(item, position)
        }
        
        // 设置长按事件
        holder.itemView.setOnLongClickListener {
            onItemLongClick(item, position)
        }
        
        holder.binding.executePendingBindings()
    }
    
    /**
     * 基础ViewHolder
     */
    class BaseViewHolder<VB : ViewDataBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)
}

/**
 * 通用的DiffUtil.ItemCallback
 */
class GenericDiffCallback<T : Any>(
    private val areItemsTheSame: (oldItem: T, newItem: T) -> Boolean,
    private val areContentsTheSame: (oldItem: T, newItem: T) -> Boolean
) : DiffUtil.ItemCallback<T>() {
    
    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return areItemsTheSame.invoke(oldItem, newItem)
    }
    
    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return areContentsTheSame.invoke(oldItem, newItem)
    }
} 