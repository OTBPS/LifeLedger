package com.example.life_ledger.data.dao

import androidx.room.*
import com.example.life_ledger.data.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * 分类数据访问对象
 * 提供对Category表的所有数据库操作
 */
@Dao
interface CategoryDao {
    
    // ==================== 基础CRUD操作 ====================
    
    /**
     * 插入单个分类
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long
    
    /**
     * 插入多个分类
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>): List<Long>
    
    /**
     * 更新分类
     */
    @Update
    suspend fun update(category: Category): Int
    
    /**
     * 删除分类
     */
    @Delete
    suspend fun delete(category: Category): Int
    
    /**
     * 根据ID删除分类
     */
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String): Int
    
    /**
     * 删除所有分类
     */
    @Query("DELETE FROM categories")
    suspend fun deleteAll(): Int
    
    // ==================== 查询操作 ====================
    
    /**
     * 根据ID获取分类
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): Category?
    
    /**
     * 获取所有分类（Flow响应式）
     */
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAllFlow(): Flow<List<Category>>
    
    /**
     * 获取所有分类
     */
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getAll(): List<Category>
    
    /**
     * 获取启用的分类
     */
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getActiveFlow(): Flow<List<Category>>
    
    /**
     * 获取启用的分类
     */
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    suspend fun getActive(): List<Category>
    
    // ==================== 按类型查询 ====================
    
    /**
     * 获取财务分类
     */
    @Query("SELECT * FROM categories WHERE type = 'FINANCIAL' AND isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getFinancialCategoriesFlow(): Flow<List<Category>>
    
    /**
     * 获取待办分类
     */
    @Query("SELECT * FROM categories WHERE type = 'TODO' AND isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getTodoCategoriesFlow(): Flow<List<Category>>
    
    /**
     * 根据类型获取分类
     */
    @Query("SELECT * FROM categories WHERE type = :type AND isActive = 1 ORDER BY sortOrder ASC, name ASC")
    suspend fun getByType(type: Category.CategoryType): List<Category>
    
    /**
     * 根据类型获取分类（Flow）
     */
    @Query("SELECT * FROM categories WHERE type = :type AND isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getByTypeFlow(type: Category.CategoryType): Flow<List<Category>>
    
    // ==================== 按子类型查询 ====================
    
    /**
     * 获取收入分类
     */
    @Query("""
        SELECT * FROM categories 
        WHERE type = 'FINANCIAL' AND subType = 'INCOME' AND isActive = 1 
        ORDER BY sortOrder ASC, name ASC
    """)
    fun getIncomeCategoriesFlow(): Flow<List<Category>>
    
    /**
     * 获取支出分类
     */
    @Query("""
        SELECT * FROM categories 
        WHERE type = 'FINANCIAL' AND subType = 'EXPENSE' AND isActive = 1 
        ORDER BY sortOrder ASC, name ASC
    """)
    fun getExpenseCategoriesFlow(): Flow<List<Category>>
    
    /**
     * 根据子类型获取分类
     */
    @Query("SELECT * FROM categories WHERE subType = :subType AND isActive = 1 ORDER BY sortOrder ASC, name ASC")
    suspend fun getBySubType(subType: String): List<Category>
    
    // ==================== 层级分类查询 ====================
    
    /**
     * 获取父分类（顶级分类）
     */
    @Query("SELECT * FROM categories WHERE parentId IS NULL AND isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getParentCategoriesFlow(): Flow<List<Category>>
    
    /**
     * 获取子分类
     */
    @Query("SELECT * FROM categories WHERE parentId = :parentId AND isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getSubCategoriesFlow(parentId: String): Flow<List<Category>>
    
    /**
     * 获取父分类
     */
    @Query("SELECT * FROM categories WHERE parentId IS NULL AND isActive = 1 ORDER BY sortOrder ASC, name ASC")
    suspend fun getParentCategories(): List<Category>
    
    /**
     * 获取子分类
     */
    @Query("SELECT * FROM categories WHERE parentId = :parentId AND isActive = 1 ORDER BY sortOrder ASC, name ASC")
    suspend fun getSubCategories(parentId: String): List<Category>
    
    // ==================== 特殊分类查询 ====================
    
    /**
     * 获取默认分类
     */
    @Query("SELECT * FROM categories WHERE isDefault = 1 ORDER BY type ASC, sortOrder ASC")
    suspend fun getDefaultCategories(): List<Category>
    
    /**
     * 获取系统分类
     */
    @Query("SELECT * FROM categories WHERE isSystemCategory = 1 ORDER BY type ASC, sortOrder ASC")
    suspend fun getSystemCategories(): List<Category>
    
    /**
     * 获取用户自定义分类
     */
    @Query("SELECT * FROM categories WHERE isSystemCategory = 0 ORDER BY sortOrder ASC, name ASC")
    fun getUserCategoriesFlow(): Flow<List<Category>>
    
    /**
     * 获取默认财务分类
     */
    @Query("""
        SELECT * FROM categories 
        WHERE type = 'FINANCIAL' AND subType = :subType AND isDefault = 1 
        ORDER BY sortOrder ASC
    """)
    suspend fun getDefaultFinancialCategory(subType: String): Category?
    
    /**
     * 获取默认待办分类
     */
    @Query("""
        SELECT * FROM categories 
        WHERE type = 'TODO' AND isDefault = 1 
        ORDER BY sortOrder ASC 
        LIMIT 1
    """)
    suspend fun getDefaultTodoCategory(): Category?
    
    // ==================== 统计查询 ====================
    
    /**
     * 获取分类总数
     */
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int
    
    /**
     * 获取启用分类数量
     */
    @Query("SELECT COUNT(*) FROM categories WHERE isActive = 1")
    suspend fun getActiveCount(): Int
    
    /**
     * 按类型统计分类数量 - 改为简单计数
     */
    @Query("SELECT COUNT(*) FROM categories WHERE isActive = 1 AND type = :type")
    suspend fun getCountByType(type: Category.CategoryType): Int
    
    /**
     * 获取使用统计
     */
    @Query("SELECT * FROM categories WHERE usageCount > 0 ORDER BY usageCount DESC, lastUsedAt DESC")
    suspend fun getMostUsedCategories(): List<Category>
    
    /**
     * 获取最近使用的分类
     */
    @Query("""
        SELECT * FROM categories 
        WHERE lastUsedAt IS NOT NULL AND isActive = 1
        ORDER BY lastUsedAt DESC 
        LIMIT :limit
    """)
    suspend fun getRecentlyUsedCategories(limit: Int = 10): List<Category>
    
    // ==================== 搜索操作 ====================
    
    /**
     * 搜索分类
     */
    @Query("""
        SELECT * FROM categories 
        WHERE name LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
           AND isActive = 1
        ORDER BY sortOrder ASC, name ASC
    """)
    fun searchCategoriesFlow(query: String): Flow<List<Category>>
    
    /**
     * 搜索分类
     */
    @Query("""
        SELECT * FROM categories 
        WHERE name LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
           AND isActive = 1
        ORDER BY sortOrder ASC, name ASC
    """)
    suspend fun searchCategories(query: String): List<Category>
    
    // ==================== 业务逻辑查询 ====================
    
    /**
     * 检查分类名称是否存在
     */
    @Query("SELECT COUNT(*) FROM categories WHERE name = :name AND type = :type AND id != :excludeId")
    suspend fun isNameExists(name: String, type: Category.CategoryType, excludeId: String = ""): Int
    
    /**
     * 获取下一个排序序号
     */
    @Query("SELECT COALESCE(MAX(sortOrder), 0) + 1 FROM categories WHERE type = :type")
    suspend fun getNextSortOrder(type: Category.CategoryType): Int
    
    /**
     * 获取有预算限额的分类
     */
    @Query("""
        SELECT * FROM categories 
        WHERE budgetLimit IS NOT NULL AND budgetLimit > 0 AND type = 'FINANCIAL'
        ORDER BY budgetLimit DESC
    """)
    suspend fun getCategoriesWithBudget(): List<Category>
    
    /**
     * 更新使用次数
     */
    @Query("""
        UPDATE categories 
        SET usageCount = usageCount + 1, lastUsedAt = :lastUsedAt, updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun incrementUsage(id: String, lastUsedAt: Long, updatedAt: Long): Int
    
    /**
     * 批量更新排序
     */
    @Query("UPDATE categories SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int, updatedAt: Long): Int
    
    /**
     * 批量启用/禁用分类
     */
    @Query("UPDATE categories SET isActive = :isActive, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun updateActiveStatus(ids: List<String>, isActive: Boolean, updatedAt: Long): Int
    
    /**
     * 更新预算限额
     */
    @Query("UPDATE categories SET budgetLimit = :budgetLimit, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBudgetLimit(id: String, budgetLimit: Double?, updatedAt: Long): Int
    
    // ==================== 数据清理操作 ====================
    
    /**
     * 删除未使用的分类（非系统分类且使用次数为0）
     */
    @Query("DELETE FROM categories WHERE isSystemCategory = 0 AND usageCount = 0")
    suspend fun deleteUnusedCategories(): Int
    
    /**
     * 重置使用统计
     */
    @Query("UPDATE categories SET usageCount = 0, lastUsedAt = NULL, updatedAt = :updatedAt")
    suspend fun resetUsageStats(updatedAt: Long): Int
    
    /**
     * 获取孤立的子分类（父分类不存在）
     */
    @Query("""
        SELECT c.* FROM categories c
        WHERE c.parentId IS NOT NULL 
        AND NOT EXISTS (SELECT 1 FROM categories p WHERE p.id = c.parentId)
    """)
    suspend fun getOrphanedSubCategories(): List<Category>
    
    /**
     * 修复孤立的子分类（设置为顶级分类）
     */
    @Query("UPDATE categories SET parentId = NULL, updatedAt = :updatedAt WHERE parentId NOT IN (SELECT id FROM categories)")
    suspend fun fixOrphanedSubCategories(updatedAt: Long): Int
} 