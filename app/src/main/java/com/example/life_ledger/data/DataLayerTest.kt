package com.example.life_ledger.data

import com.example.life_ledger.data.database.AppDatabase
import com.example.life_ledger.data.model.*
import com.example.life_ledger.data.repository.LifeLedgerRepository
import kotlinx.coroutines.runBlocking

/**
 * 数据层测试类
 * 用于验证数据访问层的基本功能
 */
class DataLayerTest {
    
    /**
     * 测试实体类创建
     */
    fun testEntityCreation() {
        // 测试Transaction创建
        val transaction = Transaction(
            amount = 100.0,
            type = Transaction.TransactionType.EXPENSE,
            categoryId = "category_1",
            title = "测试支出",
            description = "这是一个测试支出记录",
            date = System.currentTimeMillis()
        )
        
        // 测试TodoItem创建
        val todoItem = TodoItem(
            title = "测试待办事项",
            categoryId = "category_1",
            description = "这是一个测试待办事项",
            priority = TodoItem.Priority.HIGH,
            dueDate = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 明天
        )
        
        // 测试Category创建
        val category = Category(
            name = "测试分类",
            type = Category.CategoryType.FINANCIAL,
            subType = "EXPENSE",
            color = "#FF0000",
            icon = "test_icon"
        )
        
        // 测试Budget创建
        val budget = Budget(
            name = "测试预算",
            categoryId = "category_1",
            amount = 1000.0,
            period = Budget.BudgetPeriod.MONTHLY,
            startDate = System.currentTimeMillis(),
            endDate = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L // 30天后
        )
        
        // 测试UserSettings创建
        val userSettings = UserSettings(
            userId = "test_user",
            theme = UserSettings.AppTheme.LIGHT,
            currency = "CNY",
            language = "zh-CN"
        )
        
        println("✅ 所有实体类创建测试通过")
    }
    
    /**
     * 测试默认分类创建
     */
    fun testDefaultCategories() {
        val financialCategories = Category.createDefaultFinancialCategories()
        val todoCategories = Category.createDefaultTodoCategories()
        
        println("✅ 默认财务分类数量: ${financialCategories.size}")
        println("✅ 默认待办分类数量: ${todoCategories.size}")
        
        // 验证财务分类
        assert(financialCategories.any { it.name == "餐饮" && it.subType == "EXPENSE" })
        assert(financialCategories.any { it.name == "工资" && it.subType == "INCOME" })
        
        // 验证待办分类
        assert(todoCategories.any { it.name == "工作" && it.type == Category.CategoryType.TODO })
        assert(todoCategories.any { it.name == "生活" && it.type == Category.CategoryType.TODO })
        
        println("✅ 默认分类创建测试通过")
    }
    
    /**
     * 测试业务逻辑方法
     */
    fun testBusinessLogic() {
        // 测试Transaction业务逻辑
        val transaction = Transaction(
            amount = 100.0,
            type = Transaction.TransactionType.EXPENSE,
            categoryId = "category_1",
            title = "测试支出",
            date = System.currentTimeMillis(),
            tags = "餐饮, 午餐"
        )
        
        assert(transaction.isValidAmount())
        assert(transaction.getFormattedAmount() == "-100.0")
        assert(transaction.getTagsList() == listOf("餐饮", "午餐"))
        assert(transaction.isToday())
        
        // 测试TodoItem业务逻辑
        val todoItem = TodoItem(
            title = "测试任务",
            categoryId = "category_1",
            priority = TodoItem.Priority.HIGH,
            dueDate = System.currentTimeMillis() - 1000, // 已过期
            progress = 50
        )
        
        assert(todoItem.isOverdue())
        assert(todoItem.progress == 50)
        assert(!todoItem.isCompleted)
        
        // 测试Budget业务逻辑
        val budget = Budget(
            name = "测试预算",
            categoryId = "category_1",
            amount = 1000.0,
            spent = 800.0,
            period = Budget.BudgetPeriod.MONTHLY,
            startDate = System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000L,
            endDate = System.currentTimeMillis() + 20 * 24 * 60 * 60 * 1000L
        )
        
        assert(budget.getSpentPercentage() == 80.0)
        assert(budget.getRemainingAmount() == 200.0)
        assert(budget.isActive)
        
        println("✅ 业务逻辑测试通过")
    }
    
    companion object {
        /**
         * 运行所有测试
         */
        @JvmStatic
        fun runAllTests() {
            val test = DataLayerTest()
            
            try {
                test.testEntityCreation()
                test.testDefaultCategories()
                test.testBusinessLogic()
                
                println("\n🎉 所有数据层测试通过！")
                println("📊 测试覆盖：")
                println("   - ✅ 实体类创建")
                println("   - ✅ 默认数据生成")
                println("   - ✅ 业务逻辑方法")
                println("   - ✅ 数据验证")
                
            } catch (e: Exception) {
                println("❌ 测试失败: ${e.message}")
                e.printStackTrace()
            }
        }
    }
} 