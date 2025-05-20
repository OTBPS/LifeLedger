package com.example.life_ledger.data.dao

import androidx.room.*
import com.example.life_ledger.data.model.TodoItem
import kotlinx.coroutines.flow.Flow

/**
 * 待办事项数据访问对象
 * 提供对TodoItem表的所有数据库操作
 */
@Dao
interface TodoDao {
    
    // ==================== 基础CRUD操作 ====================
    
    /**
     * 插入单个待办事项
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoItem): Long
    
    /**
     * 插入多个待办事项
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(todos: List<TodoItem>): List<Long>
    
    /**
     * 更新待办事项
     */
    @Update
    suspend fun update(todo: TodoItem): Int
    
    /**
     * 删除待办事项
     */
    @Delete
    suspend fun delete(todo: TodoItem): Int
    
    /**
     * 根据ID删除待办事项
     */
    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteById(id: String): Int
    
    /**
     * 删除所有待办事项
     */
    @Query("DELETE FROM todo_items")
    suspend fun deleteAll(): Int
    
    /**
     * 删除已完成的待办事项
     */
    @Query("DELETE FROM todo_items WHERE isCompleted = 1")
    suspend fun deleteCompleted(): Int
    
    // ==================== 查询操作 ====================
    
    /**
     * 根据ID获取待办事项
     */
    @Query("SELECT * FROM todo_items WHERE id = :id")
    suspend fun getById(id: String): TodoItem?
    
    /**
     * 获取所有待办事项（Flow响应式）
     */
    @Query("SELECT * FROM todo_items ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<TodoItem>>
    
    /**
     * 获取所有待办事项
     */
    @Query("SELECT * FROM todo_items ORDER BY createdAt DESC")
    suspend fun getAll(): List<TodoItem>
    
    /**
     * 分页获取待办事项
     */
    @Query("SELECT * FROM todo_items ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getTodos(limit: Int, offset: Int): List<TodoItem>
    
    // ==================== 按状态查询 ====================
    
    /**
     * 获取未完成的待办事项
     */
    @Query("SELECT * FROM todo_items WHERE isCompleted = 0 ORDER BY dueDate ASC, priority DESC")
    fun getPendingFlow(): Flow<List<TodoItem>>
    
    /**
     * 获取已完成的待办事项
     */
    @Query("SELECT * FROM todo_items WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedFlow(): Flow<List<TodoItem>>
    
    /**
     * 获取未完成的待办事项
     */
    @Query("SELECT * FROM todo_items WHERE isCompleted = 0 ORDER BY dueDate ASC, priority DESC")
    suspend fun getPending(): List<TodoItem>
    
    /**
     * 获取已完成的待办事项
     */
    @Query("SELECT * FROM todo_items WHERE isCompleted = 1 ORDER BY completedAt DESC")
    suspend fun getCompleted(): List<TodoItem>
    
    // ==================== 按优先级查询 ====================
    
    /**
     * 按优先级获取待办事项
     */
    @Query("SELECT * FROM todo_items WHERE priority = :priority ORDER BY dueDate ASC")
    fun getByPriorityFlow(priority: TodoItem.Priority): Flow<List<TodoItem>>
    
    /**
     * 获取高优先级待办事项
     */
    @Query("""
        SELECT * FROM todo_items 
        WHERE priority IN ('HIGH', 'URGENT') AND isCompleted = 0 
        ORDER BY priority DESC, dueDate ASC
    """)
    fun getHighPriorityFlow(): Flow<List<TodoItem>>
    
    /**
     * 按优先级获取待办事项
     */
    @Query("SELECT * FROM todo_items WHERE priority = :priority ORDER BY dueDate ASC")
    suspend fun getByPriority(priority: TodoItem.Priority): List<TodoItem>
    
    // ==================== 按分类查询 ====================
    
    /**
     * 根据分类获取待办事项
     */
    @Query("SELECT * FROM todo_items WHERE categoryId = :categoryId ORDER BY dueDate ASC, priority DESC")
    fun getByCategoryFlow(categoryId: String): Flow<List<TodoItem>>
    
    /**
     * 根据分类获取待办事项
     */
    @Query("SELECT * FROM todo_items WHERE categoryId = :categoryId ORDER BY dueDate ASC, priority DESC")
    suspend fun getByCategory(categoryId: String): List<TodoItem>
    
    // ==================== 按时间查询 ====================
    
    /**
     * 获取今天到期的待办事项
     */
    @Query("""
        SELECT * FROM todo_items 
        WHERE dueDate >= :startOfDay AND dueDate <= :endOfDay AND isCompleted = 0
        ORDER BY dueDate ASC, priority DESC
    """)
    fun getTodayDueFlow(startOfDay: Long, endOfDay: Long): Flow<List<TodoItem>>
    
    /**
     * 获取过期的待办事项
     */
    @Query("""
        SELECT * FROM todo_items 
        WHERE dueDate < :currentTime AND isCompleted = 0
        ORDER BY dueDate ASC, priority DESC
    """)
    fun getOverdueFlow(currentTime: Long): Flow<List<TodoItem>>
    
    /**
     * 获取即将到期的待办事项（24小时内）
     */
    @Query("""
        SELECT * FROM todo_items 
        WHERE dueDate BETWEEN :currentTime AND :tomorrow AND isCompleted = 0
        ORDER BY dueDate ASC, priority DESC
    """)
    fun getUpcomingFlow(currentTime: Long, tomorrow: Long): Flow<List<TodoItem>>
    
    /**
     * 获取指定日期范围的待办事项
     */
    @Query("""
        SELECT * FROM todo_items 
        WHERE dueDate BETWEEN :startDate AND :endDate 
        ORDER BY dueDate ASC, priority DESC
    """)
    fun getByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<TodoItem>>
    
    /**
     * 获取指定日期范围的待办事项
     */
    @Query("""
        SELECT * FROM todo_items 
        WHERE dueDate BETWEEN :startDate AND :endDate 
        ORDER BY dueDate ASC, priority DESC
    """)
    suspend fun getByDateRange(startDate: Long, endDate: Long): List<TodoItem>
    
    // ==================== 提醒相关查询 ====================
    
    /**
     * 获取启用提醒的待办事项
     */
    @Query("""
        SELECT * FROM todo_items 
        WHERE isReminderEnabled = 1 AND isCompleted = 0
        ORDER BY reminderTime ASC
    """)
    fun getReminderEnabledFlow(): Flow<List<TodoItem>>
    
    /**
     * 获取需要提醒的待办事项
     */
    @Query("""
        SELECT * FROM todo_items 
        WHERE isReminderEnabled = 1 AND reminderTime <= :currentTime AND isCompleted = 0
        ORDER BY reminderTime ASC
    """)
    suspend fun getRemindersToSend(currentTime: Long): List<TodoItem>
    
    // ==================== 统计查询 ====================
    
    /**
     * 获取总数统计
     */
    @Query("SELECT COUNT(*) FROM todo_items")
    suspend fun getTotalCount(): Int
    
    /**
     * 获取未完成数量
     */
    @Query("SELECT COUNT(*) FROM todo_items WHERE isCompleted = 0")
    suspend fun getPendingCount(): Int
    
    /**
     * 获取已完成数量
     */
    @Query("SELECT COUNT(*) FROM todo_items WHERE isCompleted = 1")
    suspend fun getCompletedCount(): Int
    
    /**
     * 获取过期数量
     */
    @Query("""
        SELECT COUNT(*) FROM todo_items 
        WHERE dueDate < :currentTime AND isCompleted = 0
    """)
    suspend fun getOverdueCount(currentTime: Long): Int
    
    /**
     * 获取今天到期数量
     */
    @Query("""
        SELECT COUNT(*) FROM todo_items 
        WHERE dueDate >= :startOfDay AND dueDate <= :endOfDay AND isCompleted = 0
    """)
    suspend fun getTodayDueCount(startOfDay: Long, endOfDay: Long): Int
    
    /**
     * 按优先级统计数量 - 改为简单计数
     */
    @Query("""
        SELECT COUNT(*) 
        FROM todo_items 
        WHERE isCompleted = 0 AND priority = :priority
    """)
    suspend fun getCountByPriority(priority: TodoItem.Priority): Int
    
    /**
     * 按分类统计数量 - 改为简单计数
     */
    @Query("""
        SELECT COUNT(*)
        FROM todo_items 
        WHERE isCompleted = 0 AND categoryId = :categoryId
    """)
    suspend fun getCountByCategory(categoryId: String): Int
    
    /**
     * 获取完成率统计
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as completed,
            (SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as completionRate
        FROM todo_items
    """)
    suspend fun getCompletionStats(): CompletionStats
    
    // ==================== 搜索操作 ====================
    
    /**
     * 搜索待办事项（按标题或描述）
     */
    @Query("""
        SELECT * FROM todo_items 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
        ORDER BY dueDate ASC, priority DESC
    """)
    fun searchTodosFlow(query: String): Flow<List<TodoItem>>
    
    /**
     * 搜索待办事项
     */
    @Query("""
        SELECT * FROM todo_items 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
        ORDER BY dueDate ASC, priority DESC
    """)
    suspend fun searchTodos(query: String): List<TodoItem>
    
    // ==================== 复杂查询 ====================
    
    /**
     * 获取最近创建的待办事项
     */
    @Query("SELECT * FROM todo_items ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentTodos(limit: Int = 10): List<TodoItem>
    
    /**
     * 获取重复任务
     */
    @Query("SELECT * FROM todo_items WHERE isRecurring = 1 ORDER BY createdAt DESC")
    fun getRecurringTodosFlow(): Flow<List<TodoItem>>
    
    /**
     * 获取有附件的待办事项
     */
    @Query("SELECT * FROM todo_items WHERE attachments IS NOT NULL ORDER BY createdAt DESC")
    suspend fun getTodosWithAttachments(): List<TodoItem>
    
    /**
     * 获取有地点的待办事项
     */
    @Query("SELECT * FROM todo_items WHERE location IS NOT NULL ORDER BY createdAt DESC")
    suspend fun getTodosWithLocation(): List<TodoItem>
    
    /**
     * 获取进行中的任务（进度 > 0 且未完成）
     */
    @Query("""
        SELECT * FROM todo_items 
        WHERE progress > 0 AND isCompleted = 0
        ORDER BY progress DESC, dueDate ASC
    """)
    fun getInProgressFlow(): Flow<List<TodoItem>>
    
    // ==================== 批量操作 ====================
    
    /**
     * 批量标记为完成
     */
    @Query("""
        UPDATE todo_items 
        SET isCompleted = 1, completedAt = :completedAt, progress = 100, updatedAt = :updatedAt
        WHERE id IN (:ids)
    """)
    suspend fun markAsCompleted(ids: List<String>, completedAt: Long, updatedAt: Long): Int
    
    /**
     * 批量标记为未完成
     */
    @Query("""
        UPDATE todo_items 
        SET isCompleted = 0, completedAt = NULL, updatedAt = :updatedAt
        WHERE id IN (:ids)
    """)
    suspend fun markAsIncomplete(ids: List<String>, updatedAt: Long): Int
    
    /**
     * 批量更新优先级
     */
    @Query("""
        UPDATE todo_items 
        SET priority = :priority, updatedAt = :updatedAt
        WHERE id IN (:ids)
    """)
    suspend fun updatePriority(ids: List<String>, priority: TodoItem.Priority, updatedAt: Long): Int
    
    /**
     * 批量更新分类
     */
    @Query("""
        UPDATE todo_items 
        SET categoryId = :categoryId, updatedAt = :updatedAt
        WHERE id IN (:ids)
    """)
    suspend fun updateCategory(ids: List<String>, categoryId: String, updatedAt: Long): Int
}

/**
 * 完成率统计数据类
 */
data class CompletionStats(
    val total: Int,
    val completed: Int,
    val completionRate: Double
) 