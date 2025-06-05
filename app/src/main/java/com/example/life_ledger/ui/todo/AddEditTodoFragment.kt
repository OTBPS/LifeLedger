package com.example.life_ledger.ui.todo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.life_ledger.R
import com.example.life_ledger.data.database.AppDatabase
import com.example.life_ledger.data.model.TodoItem
import com.example.life_ledger.data.repository.LifeLedgerRepository
import com.example.life_ledger.databinding.FragmentAddEditTodoBinding
import com.example.life_ledger.utils.DateUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.*

/**
 * 添加/编辑待办事项页面
 */
class AddEditTodoFragment : Fragment() {

    private var _binding: FragmentAddEditTodoBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: LifeLedgerRepository
    private lateinit var viewModel: TodoViewModel

    // 当前编辑的待办事项（null表示新建）
    private var currentTodo: TodoItem? = null
    private var todoId: String = ""

    // 临时存储的日期时间
    private var selectedDueDate: Calendar? = null
    private var selectedReminderDate: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_add_edit_todo,
            container,
            false
        )
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            setupRepository()
            setupViewModel()
            extractArguments()
            setupUI()
            setupClickListeners()
            setupObservers()
            loadTodoIfEditing()
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.initialization_failed, e.message), Snackbar.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
        }
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
    }

    /**
     * 初始化ViewModel
     */
    private fun setupViewModel() {
        val factory = TodoViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TodoViewModel::class.java]
    }

    /**
     * 提取传入的参数
     */
    private fun extractArguments() {
        arguments?.let { args ->
            todoId = args.getString("todoId", "")
        }
    }

    /**
     * 设置UI组件
     */
    private fun setupUI() {
        try {
            // 设置标题
            binding.toolbar.title = if (todoId.isEmpty()) getString(R.string.add_task) else getString(R.string.edit_task)
            
            // 设置分类下拉列表
            setupCategorySpinner()
            
            // 设置优先级 - 使用ChipGroup，设置默认选中中等优先级
            setupPrioritySpinner()
            
            // 初始化进度滑块
            binding.sliderProgress.value = 0f
            updateProgressText(0)
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.ui_initialization_failed, e.message), Snackbar.LENGTH_LONG).show()
        }
    }

    /**
     * 设置分类下拉列表
     */
    private fun setupCategorySpinner() {
        try {
            val categories = listOf(
                getString(R.string.category_work), 
                getString(R.string.category_study), 
                getString(R.string.category_life), 
                getString(R.string.category_entertainment_todo), 
                getString(R.string.category_sports), 
                getString(R.string.category_shopping_todo), 
                getString(R.string.category_other_todo)
            )
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategory.adapter = adapter
        } catch (e: Exception) {
            // 忽略Spinner设置错误，使用默认值
        }
    }

    /**
     * 设置优先级下拉列表
     */
    private fun setupPrioritySpinner() {
        try {
            // 布局文件使用的是ChipGroup，不是Spinner
            // 设置默认选中中等优先级
            binding.chipMedium.isChecked = true
        } catch (e: Exception) {
            // 忽略ChipGroup设置错误
        }
    }

    /**
     * 设置点击监听器
     */
    private fun setupClickListeners() {
        try {
            binding.apply {
                // 返回按钮
                toolbar.setNavigationOnClickListener {
                    parentFragmentManager.popBackStack()
                }

                // 截止日期选择
                layoutDueDate.setOnClickListener {
                    showDateTimePicker { date ->
                        selectedDueDate = date
                        updateDueDateDisplay()
                    }
                }

                // 提醒时间选择
                layoutReminderTime.setOnClickListener {
                    showDateTimePicker { date ->
                        selectedReminderDate = date
                        updateReminderDisplay()
                    }
                }

                // 进度滑块变化
                sliderProgress.addOnChangeListener { _, value, _ ->
                    updateProgressText(value.toInt())
                }

                // 保存按钮
                buttonSave.setOnClickListener {
                    // 检查Fragment是否还活跃
                    if (!isAdded || isDetached || activity == null || activity?.isFinishing == true || _binding == null) {
                        android.util.Log.w("AddEditTodoFragment", "Fragment not active, cannot save")
                        return@setOnClickListener
                    }
                    saveTodo()
                }
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.setup_listeners_failed, e.message), Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * 设置观察者
     */
    private fun setupObservers() {
        try {
            // 观察操作结果
            lifecycleScope.launch {
                viewModel.operationResult.collect { result ->
                    if (!isAdded || _binding == null) {
                        return@collect
                    }
                    
                    if (result.isSuccess) {
                        Snackbar.make(binding.root, result.message, Snackbar.LENGTH_SHORT).show()
                        // 安全返回
                        try {
                            if (isAdded && !isDetached) {
                                parentFragmentManager.popBackStack()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AddEditTodoFragment", "Navigation failed", e)
                            activity?.finish()
                        }
                    } else {
                        Snackbar.make(binding.root, "Operation failed: ${result.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            if (isAdded && _binding != null) {
                Snackbar.make(binding.root, "Setup observers failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 如果是编辑模式，加载待办事项数据
     */
    private fun loadTodoIfEditing() {
        if (todoId.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    currentTodo = repository.getTodoById(todoId)
                    currentTodo?.let { todo ->
                        populateFields(todo)
                    }
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "加载数据失败: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    /**
     * 填充表单字段
     */
    private fun populateFields(todo: TodoItem) {
        binding.apply {
            editTitle.setText(todo.title)
            editDescription.setText(todo.description)
            
            // 设置分类 - 在Spinner中选择
            try {
                val categoryAdapter = spinnerCategory.adapter as? ArrayAdapter<String>
                if (categoryAdapter != null) {
                    val categoryPosition = categoryAdapter.getPosition(todo.categoryId ?: getString(R.string.category_other_todo))
                    if (categoryPosition >= 0) {
                        spinnerCategory.setSelection(categoryPosition)
                    }
                }
            } catch (e: Exception) {
                // 如果Spinner还未初始化，忽略错误
            }
            
            // 设置优先级 - 使用ChipGroup
            when(todo.priority) {
                TodoItem.Priority.LOW -> chipLow.isChecked = true
                TodoItem.Priority.MEDIUM -> chipMedium.isChecked = true
                TodoItem.Priority.HIGH -> chipHigh.isChecked = true
                TodoItem.Priority.URGENT -> chipUrgent.isChecked = true
            }
            
            // 设置进度
            sliderProgress.value = todo.progress.toFloat()
            updateProgressText(todo.progress)
            
            // 设置截止日期
            todo.dueDate?.let { dueDate ->
                selectedDueDate = Calendar.getInstance().apply { timeInMillis = dueDate }
                updateDueDateDisplay()
            }
            
            // 设置提醒时间
            todo.reminderTime?.let { reminderTime ->
                selectedReminderDate = Calendar.getInstance().apply { timeInMillis = reminderTime }
                updateReminderDisplay()
            }
            
            // 设置标签
            editTags.setText(todo.getTagsList().joinToString(", "))
        }
    }

    /**
     * 显示日期时间选择器
     */
    private fun showDateTimePicker(onDateTimeSelected: (Calendar) -> Unit) {
        // 检查Fragment是否还活跃
        if (!isAdded || isDetached || activity == null || activity?.isFinishing == true) {
            android.util.Log.w("AddEditTodoFragment", "Fragment not active, cannot show date picker")
            return
        }

        try {
            val calendar = Calendar.getInstance()
            val context = requireContext()
            
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    // 再次检查Fragment状态
                    if (!isAdded || isDetached || activity == null || activity?.isFinishing == true) {
                        return@DatePickerDialog
                    }
                    
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val selectedDate = Calendar.getInstance().apply {
                                set(year, month, dayOfMonth, hourOfDay, minute, 0)
                            }
                            onDateTimeSelected(selectedDate)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            
            // 再次检查Fragment状态，然后显示
            if (isAdded && !isDetached && activity != null && activity?.isFinishing != true) {
                datePickerDialog.show()
            }
        } catch (e: Exception) {
            android.util.Log.e("AddEditTodoFragment", "Error showing date picker", e)
            if (isAdded && _binding != null) {
                Snackbar.make(binding.root, "Unable to show date picker", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 更新截止日期显示
     */
    private fun updateDueDateDisplay() {
        try {
            selectedDueDate?.let { date ->
                binding.tvDueDate.text = DateUtils.formatDateTime(date.timeInMillis)
                binding.tvDueDate.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            // 忽略显示更新错误
        }
    }

    /**
     * 更新提醒时间显示
     */
    private fun updateReminderDisplay() {
        try {
            selectedReminderDate?.let { date ->
                binding.tvReminderTime.text = DateUtils.formatDateTime(date.timeInMillis)
                binding.tvReminderTime.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            // 忽略显示更新错误
        }
    }

    /**
     * 更新进度文本
     */
    private fun updateProgressText(progress: Int) {
        try {
            binding.tvProgressPercent.text = "$progress%"
        } catch (e: Exception) {
            // 忽略进度文本更新错误
        }
    }

    /**
     * 保存待办事项
     */
    private fun saveTodo() {
        // 检查Fragment是否还活跃
        if (!isAdded || isDetached || activity == null || activity?.isFinishing == true || _binding == null) {
            android.util.Log.w("AddEditTodoFragment", "Fragment not active, cannot save")
            return
        }
        
        val title = binding.editTitle.text.toString().trim()
        if (title.isEmpty()) {
            Snackbar.make(binding.root, getString(R.string.please_enter_task_title), Snackbar.LENGTH_SHORT).show()
            return
        }

        val description = binding.editDescription.text.toString().trim()
        
        // 从Spinner获取分类，如果失败则使用默认值
        val category = if (binding.spinnerCategory.adapter != null) {
            binding.spinnerCategory.selectedItem?.toString() ?: getString(R.string.category_other_todo)
        } else {
            getString(R.string.category_other_todo)
        }
        
        // 从ChipGroup获取优先级
        val priority = when {
            binding.chipLow.isChecked -> TodoItem.Priority.LOW
            binding.chipMedium.isChecked -> TodoItem.Priority.MEDIUM
            binding.chipHigh.isChecked -> TodoItem.Priority.HIGH
            binding.chipUrgent.isChecked -> TodoItem.Priority.URGENT
            else -> TodoItem.Priority.MEDIUM // 默认值
        }
        
        val progress = binding.sliderProgress.value.toInt()
        val tagsList = binding.editTags.text.toString().split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val tagsString = tagsList.joinToString(", ")

        val todo = if (currentTodo != null) {
            // 编辑现有待办事项
            currentTodo!!.copy(
                title = title,
                description = description,
                categoryId = category,
                priority = priority,
                progress = progress,
                tags = tagsString,
                dueDate = selectedDueDate?.timeInMillis,
                reminderTime = selectedReminderDate?.timeInMillis,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            // 创建新待办事项
            TodoItem(
                title = title,
                description = description,
                categoryId = category,
                priority = priority,
                progress = progress,
                tags = tagsString,
                dueDate = selectedDueDate?.timeInMillis,
                reminderTime = selectedReminderDate?.timeInMillis,
                isCompleted = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }

        // 保存到数据库
        try {
            if (currentTodo != null) {
                viewModel.updateTodo(todo)
            } else {
                viewModel.addTodo(todo)
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.operation_failed_with_message, e.message), Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 