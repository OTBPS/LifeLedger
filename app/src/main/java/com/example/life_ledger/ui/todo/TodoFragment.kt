package com.example.life_ledger.ui.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.life_ledger.R
import com.example.life_ledger.data.database.AppDatabase
import com.example.life_ledger.data.model.TodoItem
import com.example.life_ledger.data.repository.LifeLedgerRepository
import com.example.life_ledger.databinding.FragmentTodoBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * 待办事项页面
 * 提供任务管理、优先级设置、进度跟踪等功能
 */
class TodoFragment : Fragment() {

    private var _binding: FragmentTodoBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: LifeLedgerRepository
    private lateinit var todoAdapter: TodoAdapter
    
    private val viewModel: TodoViewModel by viewModels {
        TodoViewModelFactory(repository)
    }

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
        
        setupRepository()
        setupRecyclerView()
        setupUI()
        setupClickListeners()
        setupObservers()
    }

    /**
     * 初始化Repository
     */
    private fun setupRepository() {
        val database = AppDatabase.getDatabase(requireContext())
        repository = LifeLedgerRepository(
            database.transactionDao(),
            database.todoDao(),
            database.categoryDao(),
            database.budgetDao(),
            database.userSettingsDao()
        )
        
        // 确保默认数据已初始化
        lifecycleScope.launch {
            try {
                repository.initializeDefaultData()
            } catch (e: Exception) {
                android.util.Log.w("TodoFragment", "Failed to initialize default data: ${e.message}")
            }
        }
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        todoAdapter = TodoAdapter(
            onItemClick = { todo -> navigateToEditTodo(todo) },
            onItemLongClick = { todo -> showTodoOptionsMenu(todo) },
            onCheckboxClick = { todo -> viewModel.toggleTodoCompletion(todo) },
            onMoreClick = { todo, view -> showMoreOptionsMenu(todo, view) }
        )
        
        binding.rvTodoItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todoAdapter
        }
    }

    /**
     * 设置UI组件
     */
    private fun setupUI() {
        // 设置下拉刷新
        binding.swipeRefreshLayout.setOnRefreshListener {
            // 刷新数据
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    /**
     * 设置点击监听器
     */
    private fun setupClickListeners() {
        binding.apply {
            // 添加待办事项按钮
            fabAddTodo.setOnClickListener {
                navigateToAddTodo()
            }
            
            // 长按FAB创建示例数据
            fabAddTodo.setOnLongClickListener {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.create_sample_data))
                    .setMessage(getString(R.string.create_sample_data_message))
                    .setPositiveButton(getString(R.string.create)) { _, _ ->
                        viewModel.createSampleData()
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
                true
            }

            // 筛选按钮
            chipAll.setOnClickListener { 
                viewModel.setFilter(TodoViewModel.TodoFilter.ALL)
                updateFilterChips(TodoViewModel.TodoFilter.ALL)
            }
            chipPending.setOnClickListener { 
                viewModel.setFilter(TodoViewModel.TodoFilter.PENDING)
                updateFilterChips(TodoViewModel.TodoFilter.PENDING)
            }
            chipCompleted.setOnClickListener { 
                viewModel.setFilter(TodoViewModel.TodoFilter.COMPLETED)
                updateFilterChips(TodoViewModel.TodoFilter.COMPLETED)
            }
            chipHighPriority.setOnClickListener { 
                viewModel.setFilter(TodoViewModel.TodoFilter.HIGH_PRIORITY)
                updateFilterChips(TodoViewModel.TodoFilter.HIGH_PRIORITY)
            }
        }
    }

    /**
     * 设置数据观察者
     */
    private fun setupObservers() {
        // 观察筛选后的待办事项列表
        lifecycleScope.launch {
            viewModel.filteredTodos.collect { todos ->
                todoAdapter.submitList(todos)
                updateEmptyState(todos.isEmpty())
            }
        }

        // 观察统计信息
        lifecycleScope.launch {
            viewModel.todoStats.collect { stats ->
                updateStatsDisplay(stats)
            }
        }

        // 观察操作结果
        lifecycleScope.launch {
            viewModel.operationResult.collect { result ->
                val message = if (result.isSuccess) {
                    result.message
                } else {
                    "操作失败: ${result.message}"
                }
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            }
        }

        // 观察当前筛选状态
        lifecycleScope.launch {
            viewModel.currentFilter.collect { filter ->
                updateFilterChips(filter)
            }
        }
    }

    /**
     * 更新统计信息显示
     */
    private fun updateStatsDisplay(stats: TodoViewModel.TodoStats) {
        binding.apply {
            tvTodayTasks.text = stats.todayDueCount.toString()
            tvPendingTasks.text = stats.pendingCount.toString()
            tvCompletedTasks.text = stats.completedCount.toString()
        }
    }

    /**
     * 更新筛选按钮状态
     */
    private fun updateFilterChips(currentFilter: TodoViewModel.TodoFilter) {
        binding.apply {
            chipAll.isChecked = currentFilter == TodoViewModel.TodoFilter.ALL
            chipPending.isChecked = currentFilter == TodoViewModel.TodoFilter.PENDING
            chipCompleted.isChecked = currentFilter == TodoViewModel.TodoFilter.COMPLETED
            chipHighPriority.isChecked = currentFilter == TodoViewModel.TodoFilter.HIGH_PRIORITY
        }
    }

    /**
     * 更新空状态显示
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            if (isEmpty) {
                rvTodoItems.visibility = View.GONE
                layoutEmptyState.visibility = View.VISIBLE
            } else {
                rvTodoItems.visibility = View.VISIBLE
                layoutEmptyState.visibility = View.GONE
            }
        }
    }

    /**
     * 导航到添加待办事项页面
     */
    private fun navigateToAddTodo() {
        // 选择导航方式：
        // 方案1（当前）：使用对话框，稳定可靠
        // 方案2（可选）：使用完整界面，取消注释下面的行并注释showAddTodoDialog()
        
        showAddTodoDialog()
        // navigateToAddTodoWithNavigation()  // 取消注释来使用完整界面
    }

    /**
     * 导航到编辑待办事项页面
     */
    private fun navigateToEditTodo(todo: TodoItem) {
        // 选择导航方式：
        // 方案1（当前）：使用对话框
        // 方案2（可选）：使用完整界面，取消注释下面的行并注释showEditTodoDialog()
        
        showEditTodoDialog(todo)
        // navigateToEditTodoWithNavigation(todo)  // 取消注释来使用完整界面
    }

    /**
     * 显示添加任务对话框
     */
    private fun showAddTodoDialog() {
        // 创建自定义对话框视图
        val dialogLayout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        
        // 标题输入框
        val titleEditText = android.widget.EditText(requireContext()).apply {
            hint = getString(R.string.enter_task_title_hint)
            setPadding(16, 16, 16, 16)
        }
        
        // 描述输入框
        val descEditText = android.widget.EditText(requireContext()).apply {
            hint = getString(R.string.task_description_optional_hint)
            setPadding(16, 16, 16, 16)
            maxLines = 3
        }
        
        // 优先级选择
        val priorityLabel = android.widget.TextView(requireContext()).apply {
            text = getString(R.string.priority_label)
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 16, 0, 8)
        }
        
        val prioritySpinner = android.widget.Spinner(requireContext()).apply {
            val priorities = arrayOf(
                getString(R.string.priority_low),
                getString(R.string.priority_medium), 
                getString(R.string.priority_high),
                getString(R.string.priority_urgent)
            )
            val adapter = android.widget.ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                priorities
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter
            setSelection(1) // 默认选择"中"优先级
        }
        
        // 标签输入框
        val tagsEditText = android.widget.EditText(requireContext()).apply {
            hint = getString(R.string.tags_hint_todo_short)
            setPadding(16, 16, 16, 16)
        }
        
        dialogLayout.addView(titleEditText)
        dialogLayout.addView(descEditText)
        dialogLayout.addView(priorityLabel)
        dialogLayout.addView(prioritySpinner)
        dialogLayout.addView(tagsEditText)
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_task))
            .setView(dialogLayout)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val title = titleEditText.text.toString().trim()
                val description = descEditText.text.toString().trim()
                val tags = tagsEditText.text.toString().trim()
                
                if (title.isNotEmpty()) {
                    val priority = when (prioritySpinner.selectedItemPosition) {
                        0 -> TodoItem.Priority.LOW
                        1 -> TodoItem.Priority.MEDIUM
                        2 -> TodoItem.Priority.HIGH
                        3 -> TodoItem.Priority.URGENT
                        else -> TodoItem.Priority.MEDIUM
                    }
                    
                    val todo = TodoItem(
                        title = title,
                        description = description.ifEmpty { null },
                        categoryId = null,
                        priority = priority,
                        progress = 0,
                        tags = tags.ifEmpty { null },
                        isCompleted = false,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    viewModel.addTodo(todo)
                } else {
                    Snackbar.make(binding.root, getString(R.string.please_enter_task_title), Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /**
     * 显示编辑任务对话框
     */
    private fun showEditTodoDialog(todo: TodoItem) {
        // 创建自定义对话框视图
        val dialogLayout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        
        // 标题输入框
        val titleEditText = android.widget.EditText(requireContext()).apply {
            setText(todo.title)
            hint = getString(R.string.enter_task_title_hint)
            setPadding(16, 16, 16, 16)
        }
        
        // 描述输入框
        val descEditText = android.widget.EditText(requireContext()).apply {
            setText(todo.description ?: "")
            hint = getString(R.string.task_description_optional_hint)
            setPadding(16, 16, 16, 16)
            maxLines = 3
        }
        
        // 优先级选择
        val priorityLabel = android.widget.TextView(requireContext()).apply {
            text = getString(R.string.priority_label)
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 16, 0, 8)
        }
        
        val prioritySpinner = android.widget.Spinner(requireContext()).apply {
            val priorities = arrayOf(
                getString(R.string.priority_low),
                getString(R.string.priority_medium), 
                getString(R.string.priority_high),
                getString(R.string.priority_urgent)
            )
            val adapter = android.widget.ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                priorities
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter
            // 设置当前优先级
            setSelection(when (todo.priority) {
                TodoItem.Priority.LOW -> 0
                TodoItem.Priority.MEDIUM -> 1
                TodoItem.Priority.HIGH -> 2
                TodoItem.Priority.URGENT -> 3
            })
        }
        
        // 进度滑块
        val progressLabel = android.widget.TextView(requireContext()).apply {
            text = getString(R.string.progress_colon_format, todo.progress)
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 16, 0, 8)
        }
        
        val progressSeekBar = android.widget.SeekBar(requireContext()).apply {
            max = 100
            progress = todo.progress
            setPadding(0, 8, 0, 8)
            setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    progressLabel.text = getString(R.string.progress_colon_format, progress)
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            })
        }
        
        // 标签输入框
        val tagsEditText = android.widget.EditText(requireContext()).apply {
            setText(todo.tags ?: "")
            hint = getString(R.string.tags_hint_todo_short)
            setPadding(16, 16, 16, 16)
        }
        
        // 完成状态复选框
        val completedCheckBox = android.widget.CheckBox(requireContext()).apply {
            text = getString(R.string.completed)
            isChecked = todo.isCompleted
            setPadding(0, 16, 0, 8)
        }
        
        dialogLayout.addView(titleEditText)
        dialogLayout.addView(descEditText)
        dialogLayout.addView(priorityLabel)
        dialogLayout.addView(prioritySpinner)
        dialogLayout.addView(progressLabel)
        dialogLayout.addView(progressSeekBar)
        dialogLayout.addView(tagsEditText)
        dialogLayout.addView(completedCheckBox)
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.edit_task))
            .setView(dialogLayout)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val title = titleEditText.text.toString().trim()
                val description = descEditText.text.toString().trim()
                val tags = tagsEditText.text.toString().trim()
                
                if (title.isNotEmpty()) {
                    val priority = when (prioritySpinner.selectedItemPosition) {
                        0 -> TodoItem.Priority.LOW
                        1 -> TodoItem.Priority.MEDIUM
                        2 -> TodoItem.Priority.HIGH
                        3 -> TodoItem.Priority.URGENT
                        else -> TodoItem.Priority.MEDIUM
                    }
                    
                    val progress = progressSeekBar.progress
                    val isCompleted = completedCheckBox.isChecked
                    
                    val updatedTodo = todo.copy(
                        title = title,
                        description = description.ifEmpty { null },
                        priority = priority,
                        progress = progress,
                        tags = tags.ifEmpty { null },
                        isCompleted = isCompleted,
                        completedAt = if (isCompleted && !todo.isCompleted) {
                            System.currentTimeMillis()
                        } else if (!isCompleted) {
                            null
                        } else {
                            todo.completedAt
                        },
                        updatedAt = System.currentTimeMillis()
                    )
                    viewModel.updateTodo(updatedTodo)
                } else {
                    Snackbar.make(binding.root, getString(R.string.please_enter_task_title), Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .setNeutralButton(getString(R.string.delete)) { _, _ ->
                showDeleteConfirmDialog(todo)
            }
            .show()
    }

    /**
     * 显示待办事项选项菜单
     */
    private fun showTodoOptionsMenu(todo: TodoItem) {
        // 这里可以实现长按菜单，比如批量选择等
        Snackbar.make(binding.root, getString(R.string.long_press_menu_in_development), Snackbar.LENGTH_SHORT).show()
    }

    /**
     * 显示更多选项菜单
     */
    private fun showMoreOptionsMenu(todo: TodoItem, anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)
        popup.menuInflater.inflate(R.menu.todo_item_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    navigateToEditTodo(todo)
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmDialog(todo)
                    true
                }
                R.id.action_duplicate -> {
                    duplicateTodo(todo)
                    true
                }
                R.id.action_toggle_completion -> {
                    viewModel.toggleTodoCompletion(todo)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(todo: TodoItem) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.confirm_delete))
            .setMessage(getString(R.string.confirm_delete_task, todo.title))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteTodo(todo)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /**
     * 复制待办事项
     */
    private fun duplicateTodo(todo: TodoItem) {
        val duplicatedTodo = todo.copy(
            id = java.util.UUID.randomUUID().toString(),
            title = "${todo.title}${getString(R.string.duplicate_suffix)}",
            isCompleted = false,
            completedAt = null,
            progress = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        viewModel.addTodo(duplicatedTodo)
    }

    /**
     * 导航到添加待办事项页面 - Navigation Component版本
     */
    private fun navigateToAddTodoWithNavigation() {
        try {
            findNavController().navigate(R.id.action_todoFragment_to_addEditTodoFragment)
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.navigation_failed, e.message), Snackbar.LENGTH_SHORT).show()
            // 回退到对话框方式
            showAddTodoDialog()
        }
    }

    /**
     * 导航到编辑待办事项页面 - Navigation Component版本
     */
    private fun navigateToEditTodoWithNavigation(todo: TodoItem) {
        try {
            val bundle = Bundle().apply {
                putString("todoId", todo.id)
            }
            findNavController().navigate(R.id.action_todoFragment_to_addEditTodoFragment, bundle)
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.navigation_failed, e.message), Snackbar.LENGTH_SHORT).show()
            // 回退到对话框方式
            showEditTodoDialog(todo)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = TodoFragment()
    }
} 