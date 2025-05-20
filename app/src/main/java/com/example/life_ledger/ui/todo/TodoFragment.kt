package com.example.life_ledger.ui.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.life_ledger.R
import com.example.life_ledger.databinding.FragmentTodoBinding
import kotlinx.coroutines.launch

/**
 * 待办事项页面
 * 提供任务管理、优先级设置、进度跟踪等功能
 */
class TodoFragment : Fragment() {

    private var _binding: FragmentTodoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_todo,
            container,
            false
        )
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
    }

    /**
     * 设置UI组件
     */
    private fun setupUI() {
        // 初始化基础UI状态
    }

    /**
     * 设置点击监听器
     */
    private fun setupClickListeners() {
        binding.apply {
            // 添加待办事项按钮
            fabAddTodo.setOnClickListener {
                showAddTodoDialog()
            }

            // 筛选按钮
            chipAll.setOnClickListener { filterTodos("all") }
            chipPending.setOnClickListener { filterTodos("pending") }
            chipCompleted.setOnClickListener { filterTodos("completed") }
            chipHighPriority.setOnClickListener { filterTodos("high_priority") }
        }
    }

    /**
     * 显示添加待办事项对话框
     */
    private fun showAddTodoDialog() {
        // TODO: 实现添加待办事项功能
    }

    /**
     * 筛选待办事项
     */
    private fun filterTodos(filter: String) {
        // TODO: 实现筛选功能
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = TodoFragment()
    }
} 