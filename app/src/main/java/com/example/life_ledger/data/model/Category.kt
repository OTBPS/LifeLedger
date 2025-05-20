package com.example.life_ledger.data.model

import androidx.room.*
import com.example.life_ledger.constants.AppConstants
import java.util.*

/**
 * 分类实体类
 * 用于财务记录和待办事项的分类管理
 */
@Entity(
    tableName = AppConstants.Database.TABLE_CATEGORIES,
    indices = [
        Index(value = ["type"]),
        Index(value = ["parentId"]),
        Index(value = ["isActive"]),
        Index(value = ["sortOrder"])
    ]
)
data class Category(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "type")
    val type: CategoryType, // 分类类型：财务或待办
    
    @ColumnInfo(name = "subType")
    val subType: String? = null, // 子类型：INCOME, EXPENSE, TODO等
    
    @ColumnInfo(name = "icon")
    val icon: String = "default", // 图标名称或ID
    
    @ColumnInfo(name = "color")
    val color: String = "#2196F3", // 分类颜色
    
    @ColumnInfo(name = "description")
    val description: String? = null, // 分类描述
    
    @ColumnInfo(name = "parentId")
    val parentId: String? = null, // 父分类ID（支持层级分类）
    
    @ColumnInfo(name = "isActive")
    val isActive: Boolean = true, // 是否启用
    
    @ColumnInfo(name = "isDefault")
    val isDefault: Boolean = false, // 是否为默认分类
    
    @ColumnInfo(name = "isSystemCategory")
    val isSystemCategory: Boolean = false, // 是否为系统预设分类
    
    @ColumnInfo(name = "sortOrder")
    val sortOrder: Int = 0, // 排序顺序
    
    @ColumnInfo(name = "budgetLimit")
    val budgetLimit: Double? = null, // 预算限额（仅财务分类）
    
    @ColumnInfo(name = "monthlyBudget")
    val monthlyBudget: Double? = null, // 月度预算
    
    @ColumnInfo(name = "tags")
    val tags: String? = null, // 标签，JSON格式
    
    @ColumnInfo(name = "usageCount")
    val usageCount: Long = 0, // 使用次数
    
    @ColumnInfo(name = "lastUsedAt")
    val lastUsedAt: Long? = null, // 最后使用时间
    
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 分类类型枚举
     */
    enum class CategoryType {
        FINANCIAL,  // 财务分类
        TODO        // 待办分类
    }
    
    /**
     * 财务分类子类型
     */
    object FinancialSubType {
        const val INCOME = "INCOME"     // 收入
        const val EXPENSE = "EXPENSE"   // 支出
        const val TRANSFER = "TRANSFER" // 转账
    }
    
    /**
     * 待办分类子类型
     */
    object TodoSubType {
        const val WORK = "WORK"         // 工作
        const val PERSONAL = "PERSONAL" // 个人
        const val FAMILY = "FAMILY"     // 家庭
        const val HEALTH = "HEALTH"     // 健康
        const val LEARNING = "LEARNING" // 学习
        const val ENTERTAINMENT = "ENTERTAINMENT" // 娱乐
    }
    
    /**
     * 检查是否为财务分类
     */
    fun isFinancialCategory(): Boolean {
        return type == CategoryType.FINANCIAL
    }
    
    /**
     * 检查是否为待办分类
     */
    fun isTodoCategory(): Boolean {
        return type == CategoryType.TODO
    }
    
    /**
     * 检查是否为收入分类
     */
    fun isIncomeCategory(): Boolean {
        return isFinancialCategory() && subType == FinancialSubType.INCOME
    }
    
    /**
     * 检查是否为支出分类
     */
    fun isExpenseCategory(): Boolean {
        return isFinancialCategory() && subType == FinancialSubType.EXPENSE
    }
    
    /**
     * 检查是否为父分类
     */
    fun isParentCategory(): Boolean {
        return parentId == null
    }
    
    /**
     * 检查是否为子分类
     */
    fun isSubCategory(): Boolean {
        return parentId != null
    }
    
    /**
     * 检查是否可以删除
     */
    fun canBeDeleted(): Boolean {
        return !isSystemCategory && !isDefault
    }
    
    /**
     * 增加使用次数
     */
    fun incrementUsage(): Category {
        return this.copy(
            usageCount = usageCount + 1,
            lastUsedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 更新预算限额
     */
    fun updateBudgetLimit(limit: Double?): Category {
        return this.copy(
            budgetLimit = limit,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 启用/禁用分类
     */
    fun toggleActive(): Category {
        return this.copy(
            isActive = !isActive,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 更新排序
     */
    fun updateSortOrder(newOrder: Int): Category {
        return this.copy(
            sortOrder = newOrder,
            updatedAt = System.currentTimeMillis()
        )
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
    fun setTagsList(tagsList: List<String>): Category {
        val tagsString = tagsList.joinToString(", ")
        return this.copy(
            tags = tagsString,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 获取显示名称（包含父分类）
     */
    fun getDisplayName(parentName: String? = null): String {
        return if (parentName != null && isSubCategory()) {
            "$parentName > $name"
        } else {
            name
        }
    }
    
    companion object {
        /**
         * 创建默认财务分类
         */
        fun createDefaultFinancialCategories(): List<Category> {
            return listOf(
                // 收入分类
                Category(
                    name = "工资收入",
                    type = CategoryType.FINANCIAL,
                    subType = FinancialSubType.INCOME,
                    icon = "work",
                    color = "#4CAF50",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 1
                ),
                Category(
                    name = "投资收益",
                    type = CategoryType.FINANCIAL,
                    subType = FinancialSubType.INCOME,
                    icon = "trending_up",
                    color = "#2196F3",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 2
                ),
                Category(
                    name = "其他收入",
                    type = CategoryType.FINANCIAL,
                    subType = FinancialSubType.INCOME,
                    icon = "attach_money",
                    color = "#FF9800",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 3
                ),
                
                // 支出分类
                Category(
                    name = "餐饮",
                    type = CategoryType.FINANCIAL,
                    subType = FinancialSubType.EXPENSE,
                    icon = "restaurant",
                    color = "#E91E63",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 10
                ),
                Category(
                    name = "交通",
                    type = CategoryType.FINANCIAL,
                    subType = FinancialSubType.EXPENSE,
                    icon = "directions_car",
                    color = "#9C27B0",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 11
                ),
                Category(
                    name = "购物",
                    type = CategoryType.FINANCIAL,
                    subType = FinancialSubType.EXPENSE,
                    icon = "shopping_cart",
                    color = "#F44336",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 12
                ),
                Category(
                    name = "娱乐",
                    type = CategoryType.FINANCIAL,
                    subType = FinancialSubType.EXPENSE,
                    icon = "movie",
                    color = "#673AB7",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 13
                ),
                Category(
                    name = "医疗",
                    type = CategoryType.FINANCIAL,
                    subType = FinancialSubType.EXPENSE,
                    icon = "local_hospital",
                    color = "#009688",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 14
                ),
                Category(
                    name = "教育",
                    type = CategoryType.FINANCIAL,
                    subType = FinancialSubType.EXPENSE,
                    icon = "school",
                    color = "#795548",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 15
                ),
                Category(
                    name = "其他支出",
                    type = CategoryType.FINANCIAL,
                    subType = FinancialSubType.EXPENSE,
                    icon = "more_horiz",
                    color = "#607D8B",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 16
                )
            )
        }
        
        /**
         * 创建默认待办分类
         */
        fun createDefaultTodoCategories(): List<Category> {
            return listOf(
                Category(
                    name = "工作",
                    type = CategoryType.TODO,
                    subType = TodoSubType.WORK,
                    icon = "work",
                    color = "#2196F3",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 1
                ),
                Category(
                    name = "个人",
                    type = CategoryType.TODO,
                    subType = TodoSubType.PERSONAL,
                    icon = "person",
                    color = "#4CAF50",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 2
                ),
                Category(
                    name = "家庭",
                    type = CategoryType.TODO,
                    subType = TodoSubType.FAMILY,
                    icon = "home",
                    color = "#FF9800",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 3
                ),
                Category(
                    name = "学习",
                    type = CategoryType.TODO,
                    subType = TodoSubType.LEARNING,
                    icon = "school",
                    color = "#9C27B0",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 4
                ),
                Category(
                    name = "健康",
                    type = CategoryType.TODO,
                    subType = TodoSubType.HEALTH,
                    icon = "favorite",
                    color = "#E91E63",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 5
                ),
                Category(
                    name = "娱乐",
                    type = CategoryType.TODO,
                    subType = TodoSubType.ENTERTAINMENT,
                    icon = "sports_esports",
                    color = "#673AB7",
                    isDefault = true,
                    isSystemCategory = true,
                    sortOrder = 6
                )
            )
        }
    }
} 