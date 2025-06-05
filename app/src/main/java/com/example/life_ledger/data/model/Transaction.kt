package com.example.life_ledger.data.model

import androidx.room.*
import com.example.life_ledger.constants.AppConstants
import java.util.*

/**
 * 财务记录实体类
 * 用于存储收入和支出记录
 */
@Entity(
    tableName = AppConstants.Database.TABLE_TRANSACTIONS,
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["date"]),
        Index(value = ["type"]),
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
data class Transaction(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "amount")
    val amount: Double,
    
    @ColumnInfo(name = "type")
    val type: TransactionType,
    
    @ColumnInfo(name = "categoryId")
    val categoryId: String?,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "tags")
    val tags: String? = null,
    
    @ColumnInfo(name = "date")
    val date: Long,
    
    @ColumnInfo(name = "location")
    val location: String? = null,
    
    @ColumnInfo(name = "paymentMethod")
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    
    @ColumnInfo(name = "isRecurring")
    val isRecurring: Boolean = false,
    
    @ColumnInfo(name = "recurringInterval")
    val recurringInterval: String? = null,
    
    @ColumnInfo(name = "attachments")
    val attachments: String? = null,
    
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 交易类型
     */
    enum class TransactionType {
        INCOME,   // 收入
        EXPENSE   // 支出
    }
    
    /**
     * 支付方式
     */
    enum class PaymentMethod {
        CASH,           // 现金

    }
    
    /**
     * 验证金额是否有效
     */
    fun isValidAmount(): Boolean {
        return amount > 0
    }
    
    /**
     * 获取格式化的金额字符串
     */
    fun getFormattedAmount(): String {
        val prefix = if (type == TransactionType.INCOME) "+" else "-"
        return "$prefix$amount"
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
    fun setTagsList(tagsList: List<String>): Transaction {
        val tagsString = tagsList.joinToString(", ")
        return this.copy(tags = tagsString)
    }
    
    /**
     * 检查是否为今天的交易
     */
    fun isToday(): Boolean {
        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1
        return date in todayStart..todayEnd
    }
    
    /**
     * 检查是否为本月的交易
     */
    fun isThisMonth(): Boolean {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        calendar.timeInMillis = date
        val transactionMonth = calendar.get(Calendar.MONTH)
        val transactionYear = calendar.get(Calendar.YEAR)
        
        return currentMonth == transactionMonth && currentYear == transactionYear
    }
} 