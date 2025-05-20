package com.example.life_ledger.ui.finance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.life_ledger.data.model.Category
import com.example.life_ledger.data.repository.LifeLedgerRepository
import kotlinx.coroutines.launch

/**
 * 分类管理ViewModel
 * 处理分类的CRUD操作和筛选逻辑
 */
class CategoryManagementViewModel(
    private val repository: LifeLedgerRepository
) : ViewModel() {

    // 所有分类
    private val _allCategories = MutableLiveData<List<Category>>()
    
    // 筛选后的分类
    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    // 操作结果状态
    private val _operationResult = MutableLiveData<OperationResult?>()
    val operationResult: LiveData<OperationResult?> = _operationResult

    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 当前筛选条件
    private var currentFilter = CategoryFilter.ALL

    init {
        loadCategories()
    }

    /**
     * 加载所有分类
     */
    private fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllCategories().collect { allCategories ->
                    _allCategories.value = allCategories.filter { it.isFinancialCategory() }
                    applyFilter()
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult(
                    isSuccess = false,
                    message = "加载分类失败: ${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 设置筛选条件
     */
    fun setFilter(filter: CategoryFilter) {
        currentFilter = filter
        applyFilter()
    }

    /**
     * 应用筛选条件
     */
    private fun applyFilter() {
        val allCategories = _allCategories.value ?: return
        
        val filtered = when (currentFilter) {
            CategoryFilter.ALL -> allCategories
            CategoryFilter.INCOME -> allCategories.filter { it.isIncomeCategory() }
            CategoryFilter.EXPENSE -> allCategories.filter { it.isExpenseCategory() }
        }

        // 按排序顺序和名称排序
        val sorted = filtered.sortedWith(compareBy({ it.sortOrder }, { it.name }))
        _categories.value = sorted
    }

    /**
     * 添加分类
     */
    fun addCategory(category: Category) {
        viewModelScope.launch {
            try {
                repository.insertCategory(category)
                _operationResult.value = OperationResult(
                    isSuccess = true,
                    message = "分类添加成功"
                )
            } catch (e: Exception) {
                _operationResult.value = OperationResult(
                    isSuccess = false,
                    message = "添加失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 更新分类
     */
    fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                repository.updateCategory(category)
                _operationResult.value = OperationResult(
                    isSuccess = true,
                    message = "分类更新成功"
                )
            } catch (e: Exception) {
                _operationResult.value = OperationResult(
                    isSuccess = false,
                    message = "更新失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 删除分类
     */
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(category)
                _operationResult.value = OperationResult(
                    isSuccess = true,
                    message = "分类删除成功"
                )
            } catch (e: Exception) {
                _operationResult.value = OperationResult(
                    isSuccess = false,
                    message = "删除失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 切换分类激活状态
     */
    fun toggleCategoryActive(category: Category) {
        viewModelScope.launch {
            try {
                val updatedCategory = category.toggleActive()
                repository.updateCategory(updatedCategory)
                _operationResult.value = OperationResult(
                    isSuccess = true,
                    message = if (updatedCategory.isActive) "分类已启用" else "分类已禁用"
                )
            } catch (e: Exception) {
                _operationResult.value = OperationResult(
                    isSuccess = false,
                    message = "操作失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 清除操作结果
     */
    fun clearOperationResult() {
        _operationResult.value = null
    }
}

/**
 * CategoryManagementViewModel工厂类
 */
class CategoryManagementViewModelFactory(
    private val repository: LifeLedgerRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoryManagementViewModel::class.java)) {
            return CategoryManagementViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 