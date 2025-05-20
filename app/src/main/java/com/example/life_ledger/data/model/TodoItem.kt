package com.example.life_ledger.data.model

import androidx.room.*
import com.example.life_ledger.constants.AppConstants
import java.util.*

/**
 * 待办事项实体类
 * 用于存储任务和待办事项
 */
@Entity(
    tableName = AppConstants.Database.TABLE_TODOS,
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["priority"]),
        Index(value = ["dueDate"]),
        Index(value = ["isCompleted"]),
        Index(value = ["createdAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TodoItem(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "categoryId")
    val categoryId: String?,
    
    @ColumnInfo(name = "priority")
    val priority: Priority = Priority.MEDIUM,
    
    @ColumnInfo(name = "dueDate")
    val dueDate: Long? = null, // 截止日期时间戳
    
    @ColumnInfo(name = "isCompleted")
    val isCompleted: Boolean = false,
    
    @ColumnInfo(name = "completedAt")
    val completedAt: Long? = null, // 完成时间
    
    @ColumnInfo(name = "reminderTime")
    val reminderTime: Long? = null, // 提醒时间
    
    @ColumnInfo(name = "isReminderEnabled")
    val isReminderEnabled: Boolean = false,
    
    @ColumnInfo(name = "tags")
    val tags: String? = null, // JSON格式存储标签列表
    
    @ColumnInfo(name = "progress")
    val progress: Int = 0, // 进度百分比 0-100
    
    @ColumnInfo(name = "estimatedDuration")
    val estimatedDuration: Int? = null, // 预估时长（分钟）
    
    @ColumnInfo(name = "actualDuration")
    val actualDuration: Int? = null, // 实际时长（分钟）
    
    @ColumnInfo(name = "isRecurring")
    val isRecurring: Boolean = false, // 是否为重复任务
    
    @ColumnInfo(name = "recurringPattern")
    val recurringPattern: String? = null, // 重复模式 (DAILY, WEEKLY, MONTHLY等)
    
    @ColumnInfo(name = "location")
    val location: String? = null, // 任务地点
    
    @ColumnInfo(name = "attachments")
    val attachments: String? = null, // 附件路径，JSON格式
    
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 优先级枚举
     */
    enum class Priority(val value: Int, val displayName: String) {
        LOW(1, "低"),
        MEDIUM(2, "中"),
        HIGH(3, "高"),
        URGENT(4, "紧急")
    }
    
    /**
     * 任务状态枚举
     */
    enum class Status {
        PENDING,     // 待办
        IN_PROGRESS, // 进行中
        COMPLETED,   // 已完成
        CANCELLED,   // 已取消
        OVERDUE      // 已过期
    }
    
    /**
     * 检查任务是否过期
     */
    fun isOverdue(): Boolean {
        return dueDate != null && dueDate < System.currentTimeMillis() && !isCompleted
    }
    
    /**
     * 检查任务是否即将到期（24小时内）
     */
    fun isDueSoon(): Boolean {
        if (dueDate == null || isCompleted) return false
        val hoursUntilDue = (dueDate - System.currentTimeMillis()) / (1000 * 60 * 60)
        return hoursUntilDue in 0..24
    }
    
    /**
     * 获取任务当前状态
     */
    fun getCurrentStatus(): Status {
        return when {
            isCompleted -> Status.COMPLETED
            isOverdue() -> Status.OVERDUE
            progress > 0 -> Status.IN_PROGRESS
            else -> Status.PENDING
        }
    }
    
    /**
     * 解析标签列表
     */
    fun getTagsList(): List<String> {
        return if (tags.isNullOrEmpty()) {
            emptyList()
        } else {
            tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
    
    /**
     * 设置标签列表
     */
    fun setTagsList(tagsList: List<String>): TodoItem {
        val tagsString = tagsList.joinToString(", ")
        return this.copy(tags = tagsString)
    }
    
    /**
     * 获取剩余时间文本
     */
    fun getRemainingTimeText(): String? {
        if (dueDate == null) return null
        
        val remaining = dueDate - System.currentTimeMillis()
        return when {
            remaining < 0 -> "已过期"
            remaining < 60 * 60 * 1000 -> "1小时内"
            remaining < 24 * 60 * 60 * 1000 -> "${remaining / (60 * 60 * 1000)}小时后"
            remaining < 7 * 24 * 60 * 60 * 1000 -> "${remaining / (24 * 60 * 60 * 1000)}天后"
            else -> "超过1周"
        }
    }
    
    /**
     * 检查是否为今天的任务
     */
    fun isDueToday(): Boolean {
        if (dueDate == null) return false
        
        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1
        return dueDate in todayStart..todayEnd
    }
    
    /**
     * 标记为完成
     */
    fun markAsCompleted(): TodoItem {
        return this.copy(
            isCompleted = true,
            completedAt = System.currentTimeMillis(),
            progress = 100,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 取消完成状态
     */
    fun markAsIncomplete(): TodoItem {
        return this.copy(
            isCompleted = false,
            completedAt = null,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 更新进度
     */
    fun updateProgress(newProgress: Int): TodoItem {
        val validProgress = newProgress.coerceIn(0, 100)
        return this.copy(
            progress = validProgress,
            isCompleted = validProgress == 100,
            completedAt = if (validProgress == 100) System.currentTimeMillis() else completedAt,
            updatedAt = System.currentTimeMillis()
        )
    }
} 