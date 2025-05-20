package com.example.life_ledger.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.life_ledger.data.model.TodoItem
import com.example.life_ledger.data.model.Category
import com.example.life_ledger.data.repository.LifeLedgerRepository
import com.example.life_ledger.ui.finance.OperationResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 待办事项ViewModel
 * 管理待办事项的状态和业务逻辑
 */
class TodoViewModel(private val repository: LifeLedgerRepository) : ViewModel() {

    // 待办事项列表
    private val _allTodos = MutableStateFlow<List<TodoItem>>(emptyList())
    val allTodos: StateFlow<List<TodoItem>> = _allTodos.asStateFlow()

    // 筛选后的待办事项
    private val _filteredTodos = MutableStateFlow<List<TodoItem>>(emptyList())
    val filteredTodos: StateFlow<List<TodoItem>> = _filteredTodos.asStateFlow()

    // 当前筛选类型
    private val _currentFilter = MutableStateFlow(TodoFilter.ALL)
    val currentFilter: StateFlow<TodoFilter> = _currentFilter.asStateFlow()

    // 搜索查询
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 排序方式
    private val _sortOrder = MutableStateFlow(SortOrder.DUE_DATE_ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // 分类列表
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    // 统计信息
    private val _todoStats = MutableStateFlow(TodoStats())
    val todoStats: StateFlow<TodoStats> = _todoStats.asStateFlow()

    // 操作结果
    private val _operationResult = MutableSharedFlow<OperationResult>()
    val operationResult: SharedFlow<OperationResult> = _operationResult.asSharedFlow()

    init {
        loadTodos()
        loadCategories()
        loadStats()
    }

    /**
     * 加载所有待办事项
     */
    private fun loadTodos() {
        viewModelScope.launch {
            repository.getAllTodos().collect { todos ->
                _allTodos.value = todos
                applyFilterAndSort()
                loadStats()
            }
        }
    }

    /**
     * 加载分类
     */
    private fun loadCategories() {
        viewModelScope.launch {
            repository.getTodoCategories().collect { categories ->
                _categories.value = categories
            }
        }
    }

    /**
     * 加载统计信息
     */
    private fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = repository.getTodoStats()
                _todoStats.value = TodoStats(
                    totalCount = stats.totalCount,
                    pendingCount = stats.pendingCount,
                    completedCount = stats.completedCount,
                    overdueCount = stats.overdueCount,
                    todayDueCount = getTodayDueCount(),
                    completionRate = stats.completionRate
                )
            } catch (e: Exception) {
                _operationResult.emit(OperationResult(false, "加载统计信息失败: ${e.message}"))
            }
        }
    }

    /**
     * 获取今天到期的任务数量
     */
    private suspend fun getTodayDueCount(): Int {
        val todayTodos = repository.getTodayDueTodos().first()
        return todayTodos.size
    }

    /**
     * 添加待办事项
     */
    fun addTodo(todo: TodoItem) {
        viewModelScope.launch {
            try {
                repository.insertTodo(todo)
                _operationResult.emit(OperationResult(true, "任务添加成功"))
            } catch (e: Exception) {
                _operationResult.emit(OperationResult(false, "添加任务失败: ${e.message}"))
            }
        }
    }

    /**
     * 更新待办事项
     */
    fun updateTodo(todo: TodoItem) {
        viewModelScope.launch {
            try {
                repository.updateTodo(todo)
                _operationResult.emit(OperationResult(true, "任务更新成功"))
            } catch (e: Exception) {
                _operationResult.emit(OperationResult(false, "更新任务失败: ${e.message}"))
            }
        }
    }

    /**
     * 删除待办事项
     */
    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            try {
                repository.deleteTodo(todo)
                _operationResult.emit(OperationResult(true, "任务删除成功"))
            } catch (e: Exception) {
                _operationResult.emit(OperationResult(false, "删除任务失败: ${e.message}"))
            }
        }
    }

    /**
     * 根据ID获取待办事项
     */
    suspend fun getTodoById(id: String): TodoItem? {
        return try {
            repository.getTodoById(id)
        } catch (e: Exception) {
            _operationResult.emit(OperationResult(false, "获取任务失败: ${e.message}"))
            null
        }
    }

    /**
     * 切换任务完成状态
     */
    fun toggleTodoCompletion(todo: TodoItem) {
        viewModelScope.launch {
            try {
                val updatedTodo = if (todo.isCompleted) {
                    todo.markAsIncomplete()
                } else {
                    todo.markAsCompleted()
                }
                repository.updateTodo(updatedTodo)
                _operationResult.emit(
                    OperationResult(
                        true,
                        if (updatedTodo.isCompleted) "任务已完成" else "任务已恢复"
                    )
                )
            } catch (e: Exception) {
                _operationResult.emit(OperationResult(false, "更新任务状态失败: ${e.message}"))
            }
        }
    }

    /**
     * 更新任务进度
     */
    fun updateTodoProgress(todo: TodoItem, progress: Int) {
        viewModelScope.launch {
            try {
                val updatedTodo = todo.updateProgress(progress)
                repository.updateTodo(updatedTodo)
                _operationResult.emit(OperationResult(true, "进度已更新"))
            } catch (e: Exception) {
                _operationResult.emit(OperationResult(false, "更新进度失败: ${e.message}"))
            }
        }
    }

    /**
     * 设置筛选器
     */
    fun setFilter(filter: TodoFilter) {
        _currentFilter.value = filter
        applyFilterAndSort()
    }

    /**
     * 设置搜索查询
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilterAndSort()
    }

    /**
     * 设置排序方式
     */
    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        applyFilterAndSort()
    }

    /**
     * 应用筛选和排序
     */
    private fun applyFilterAndSort() {
        val todos = _allTodos.value
        val filtered = filterTodos(todos)
        val sorted = sortTodos(filtered)
        _filteredTodos.value = sorted
    }

    /**
     * 筛选待办事项
     */
    private fun filterTodos(todos: List<TodoItem>): List<TodoItem> {
        val query = _searchQuery.value
        val filter = _currentFilter.value

        var filtered = todos

        // 应用搜索筛选
        if (query.isNotBlank()) {
            filtered = filtered.filter { todo ->
                todo.title.contains(query, ignoreCase = true) ||
                (todo.description?.contains(query, ignoreCase = true) == true) ||
                todo.getTagsList().any { it.contains(query, ignoreCase = true) }
            }
        }

        // 应用状态筛选
        filtered = when (filter) {
            TodoFilter.ALL -> filtered
            TodoFilter.PENDING -> filtered.filter { !it.isCompleted }
            TodoFilter.COMPLETED -> filtered.filter { it.isCompleted }
            TodoFilter.HIGH_PRIORITY -> filtered.filter { 
                it.priority in listOf(TodoItem.Priority.HIGH, TodoItem.Priority.URGENT) && !it.isCompleted
            }
            TodoFilter.DUE_TODAY -> filtered.filter { it.isDueToday() && !it.isCompleted }
            TodoFilter.OVERDUE -> filtered.filter { it.isOverdue() }
            TodoFilter.IN_PROGRESS -> filtered.filter { it.progress > 0 && !it.isCompleted }
        }

        return filtered
    }

    /**
     * 排序待办事项
     */
    private fun sortTodos(todos: List<TodoItem>): List<TodoItem> {
        return when (_sortOrder.value) {
            SortOrder.DUE_DATE_ASC -> todos.sortedBy { it.dueDate ?: Long.MAX_VALUE }
            SortOrder.DUE_DATE_DESC -> todos.sortedByDescending { it.dueDate ?: 0 }
            SortOrder.PRIORITY_DESC -> todos.sortedByDescending { it.priority.value }
            SortOrder.PRIORITY_ASC -> todos.sortedBy { it.priority.value }
            SortOrder.CREATED_DATE_DESC -> todos.sortedByDescending { it.createdAt }
            SortOrder.CREATED_DATE_ASC -> todos.sortedBy { it.createdAt }
            SortOrder.TITLE_ASC -> todos.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_DESC -> todos.sortedByDescending { it.title.lowercase() }
            SortOrder.PROGRESS_DESC -> todos.sortedByDescending { it.progress }
            SortOrder.PROGRESS_ASC -> todos.sortedBy { it.progress }
        }
    }

    /**
     * 批量标记为完成
     */
    fun markTodosAsCompleted(todoIds: List<String>) {
        viewModelScope.launch {
            try {
                repository.markTodosAsCompleted(todoIds)
                _operationResult.emit(OperationResult(true, "已标记 ${todoIds.size} 个任务为完成"))
            } catch (e: Exception) {
                _operationResult.emit(OperationResult(false, "批量更新失败: ${e.message}"))
            }
        }
    }

    /**
     * 删除已完成的任务
     */
    fun deleteCompletedTodos() {
        viewModelScope.launch {
            try {
                val completedTodos = _allTodos.value.filter { it.isCompleted }
                completedTodos.forEach { todo ->
                    repository.deleteTodo(todo)
                }
                _operationResult.emit(OperationResult(true, "已删除 ${completedTodos.size} 个已完成任务"))
            } catch (e: Exception) {
                _operationResult.emit(OperationResult(false, "删除任务失败: ${e.message}"))
            }
        }
    }

    /**
     * 获取分类名称
     */
    fun getCategoryName(categoryId: String?): String {
        if (categoryId == null) return "未分类"
        return _categories.value.find { it.id == categoryId }?.name ?: "未知分类"
    }

    /**
     * 创建示例数据用于测试
     */
    fun createSampleData() {
        viewModelScope.launch {
            try {
                val sampleTodos = listOf(
                    TodoItem(
                        title = "完成项目报告",
                        description = "需要整理这个月的工作总结和下个月的计划",
                        categoryId = null,
                        priority = TodoItem.Priority.HIGH,
                        progress = 60,
                        tags = "工作, 报告, 月度总结",
                        dueDate = System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000, // 2天后
                        reminderTime = System.currentTimeMillis() + 1 * 24 * 60 * 60 * 1000 // 1天后提醒
                    ),
                    TodoItem(
                        title = "去超市买菜",
                        description = "购买本周的蔬菜和水果",
                        categoryId = null,
                        priority = TodoItem.Priority.MEDIUM,
                        progress = 0,
                        tags = "购物, 食材",
                        dueDate = System.currentTimeMillis() + 1 * 24 * 60 * 60 * 1000 // 明天
                    ),
                    TodoItem(
                        title = "学习新技术",
                        description = "学习Kotlin协程的高级用法",
                        categoryId = null,
                        priority = TodoItem.Priority.LOW,
                        progress = 30,
                        tags = "编程, Kotlin, 协程",
                        dueDate = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000 // 7天后
                    ),
                    TodoItem(
                        title = "健身锻炼",
                        description = "每周至少3次有氧运动",
                        categoryId = null,
                        priority = TodoItem.Priority.MEDIUM,
                        progress = 0,
                        tags = "健康, 运动",
                        dueDate = System.currentTimeMillis() + 3 * 60 * 60 * 1000, // 3小时后
                        isRecurring = true,
                        recurringPattern = "WEEKLY"
                    ),
                    TodoItem(
                        title = "已完成的任务",
                        description = "这是一个已经完成的示例任务",
                        categoryId = null,
                        priority = TodoItem.Priority.HIGH,
                        progress = 100,
                        tags = "完成, 示例",
                        isCompleted = true,
                        completedAt = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000, // 昨天完成
                        dueDate = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000 // 前天到期
                    )
                )
                
                sampleTodos.forEach { todo ->
                    repository.insertTodo(todo)
                }
                
                _operationResult.emit(OperationResult(true, "示例数据创建成功"))
            } catch (e: Exception) {
                _operationResult.emit(OperationResult(false, "创建示例数据失败: ${e.message}"))
            }
        }
    }

    /**
     * 筛选枚举
     */
    enum class TodoFilter {
        ALL,           // 全部
        PENDING,       // 待完成
        COMPLETED,     // 已完成
        HIGH_PRIORITY, // 高优先级
        DUE_TODAY,     // 今天到期
        OVERDUE,       // 已过期
        IN_PROGRESS    // 进行中
    }

    /**
     * 排序枚举
     */
    enum class SortOrder {
        DUE_DATE_ASC,      // 截止日期升序
        DUE_DATE_DESC,     // 截止日期降序
        PRIORITY_DESC,     // 优先级降序
        PRIORITY_ASC,      // 优先级升序
        CREATED_DATE_DESC, // 创建日期降序
        CREATED_DATE_ASC,  // 创建日期升序
        TITLE_ASC,         // 标题升序
        TITLE_DESC,        // 标题降序
        PROGRESS_DESC,     // 进度降序
        PROGRESS_ASC       // 进度升序
    }

    /**
     * 统计信息数据类
     */
    data class TodoStats(
        val totalCount: Int = 0,
        val pendingCount: Int = 0,
        val completedCount: Int = 0,
        val overdueCount: Int = 0,
        val todayDueCount: Int = 0,
        val completionRate: Double = 0.0
    )
}

/**
 * ViewModel工厂
 */
class TodoViewModelFactory(private val repository: LifeLedgerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 