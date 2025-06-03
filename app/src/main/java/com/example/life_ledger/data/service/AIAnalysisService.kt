package com.example.life_ledger.data.service

import com.example.life_ledger.data.model.Transaction
import com.example.life_ledger.data.model.Category
import com.example.life_ledger.data.network.NetworkClient
import com.example.life_ledger.data.network.ChatCompletionRequest
import com.example.life_ledger.data.network.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * AI分析服务
 * 使用DeepSeek API进行智能财务分析
 */
class AIAnalysisService {
    
    private val apiService = NetworkClient.deepSeekApiService
    private val dateFormat = SimpleDateFormat("yyyy年MM月", Locale.CHINA)
    
    /**
     * 智能支出分析
     */
    suspend fun analyzeExpenses(
        transactions: List<Transaction>,
        categories: List<Category>
    ): Result<ExpenseAnalysis> = withContext(Dispatchers.IO) {
        try {
            val expenseData = prepareExpenseData(transactions, categories)
            val prompt = buildExpenseAnalysisPrompt(expenseData)
            
            val response = apiService.chatCompletion(
                ChatCompletionRequest(
                    messages = listOf(
                        Message("system", "你是一个专业的财务分析师，专门分析个人支出模式并提供专业建议。"),
                        Message("user", prompt)
                    ),
                    max_tokens = 1500,
                    temperature = 0.7
                )
            )
            
            if (response.isSuccessful && response.body()?.choices?.isNotEmpty() == true) {
                val analysis = parseExpenseAnalysis(response.body()!!.choices[0].message.content)
                Result.success(analysis)
            } else {
                Result.failure(Exception("AI分析请求失败: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 生成月度财务报告
     */
    suspend fun generateMonthlyReport(
        transactions: List<Transaction>,
        categories: List<Category>,
        year: Int,
        month: Int
    ): Result<MonthlyReport> = withContext(Dispatchers.IO) {
        try {
            val reportData = prepareMonthlyReportData(transactions, categories, year, month)
            val prompt = buildMonthlyReportPrompt(reportData, year, month)
            
            val response = apiService.chatCompletion(
                ChatCompletionRequest(
                    messages = listOf(
                        Message("system", "你是一个财务顾问，专门为用户生成详细的月度财务报告。报告应该专业、准确、易懂。"),
                        Message("user", prompt)
                    ),
                    max_tokens = 2000,
                    temperature = 0.6
                )
            )
            
            if (response.isSuccessful && response.body()?.choices?.isNotEmpty() == true) {
                val report = parseMonthlyReport(response.body()!!.choices[0].message.content, year, month)
                Result.success(report)
            } else {
                Result.failure(Exception("月度报告生成失败: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取个性化消费建议
     */
    suspend fun getPersonalizedAdvice(
        transactions: List<Transaction>,
        categories: List<Category>,
        userProfile: UserProfile
    ): Result<List<ConsumptionAdvice>> = withContext(Dispatchers.IO) {
        try {
            val adviceData = prepareAdviceData(transactions, categories, userProfile)
            val prompt = buildAdvicePrompt(adviceData, userProfile)
            
            val response = apiService.chatCompletion(
                ChatCompletionRequest(
                    messages = listOf(
                        Message("system", "你是一个理财专家，根据用户的消费习惯和财务状况提供个性化的理财建议。"),
                        Message("user", prompt)
                    ),
                    max_tokens = 1500,
                    temperature = 0.8
                )
            )
            
            if (response.isSuccessful && response.body()?.choices?.isNotEmpty() == true) {
                val advice = parseConsumptionAdvice(response.body()!!.choices[0].message.content)
                Result.success(advice)
            } else {
                Result.failure(Exception("个性化建议生成失败: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取预算智能建议
     */
    suspend fun getBudgetRecommendations(
        budgets: List<com.example.life_ledger.data.model.Budget>,
        transactions: List<Transaction>,
        categories: List<Category>
    ): Result<List<com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation>> = withContext(Dispatchers.IO) {
        try {
            val budgetData = prepareBudgetAnalysisData(budgets, transactions, categories)
            val prompt = buildBudgetRecommendationPrompt(budgetData)
            
            val response = apiService.chatCompletion(
                ChatCompletionRequest(
                    messages = listOf(
                        Message("system", "你是一个专业的预算管理顾问，专门分析用户的预算执行情况并提供实用的优化建议。"),
                        Message("user", prompt)
                    ),
                    max_tokens = 1500,
                    temperature = 0.7
                )
            )
            
            if (response.isSuccessful && response.body()?.choices?.isNotEmpty() == true) {
                val recommendations = parseBudgetRecommendations(response.body()!!.choices[0].message.content)
                Result.success(recommendations)
            } else {
                Result.failure(Exception("预算建议生成失败: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 数据准备方法
    
    private fun prepareExpenseData(transactions: List<Transaction>, categories: List<Category>): ExpenseData {
        val expenseTransactions = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        val categoryMap = categories.associateBy { it.id }
        
        val categoryExpenses = expenseTransactions
            .groupBy { it.categoryId }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .mapKeys { (categoryId, _) -> categoryMap[categoryId]?.name ?: "其他" }
        
        val totalExpense = expenseTransactions.sumOf { it.amount }
        val avgDailyExpense = if (expenseTransactions.isNotEmpty()) {
            totalExpense / 30 // 假设30天
        } else 0.0
        
        val topCategories = categoryExpenses.toList().sortedByDescending { it.second }.take(5)
        
        return ExpenseData(
            totalExpense = totalExpense,
            avgDailyExpense = avgDailyExpense,
            categoryExpenses = categoryExpenses,
            topCategories = topCategories,
            transactionCount = expenseTransactions.size
        )
    }
    
    private fun prepareMonthlyReportData(
        transactions: List<Transaction>,
        categories: List<Category>,
        year: Int,
        month: Int
    ): MonthlyReportData {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        val startOfMonth = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = calendar.timeInMillis
        
        val monthlyTransactions = transactions.filter { 
            it.date >= startOfMonth && it.date <= endOfMonth 
        }
        
        val income = monthlyTransactions
            .filter { it.type == Transaction.TransactionType.INCOME }
            .sumOf { it.amount }
        
        val expense = monthlyTransactions
            .filter { it.type == Transaction.TransactionType.EXPENSE }
            .sumOf { it.amount }
        
        val categoryMap = categories.associateBy { it.id }
        val categoryBreakdown = monthlyTransactions
            .groupBy { it.categoryId }
            .mapValues { (_, transactions) -> 
                TransactionSummary(
                    count = transactions.size,
                    amount = transactions.sumOf { it.amount }
                )
            }
            .mapKeys { (categoryId, _) -> categoryMap[categoryId]?.name ?: "其他" }
        
        return MonthlyReportData(
            year = year,
            month = month,
            totalIncome = income,
            totalExpense = expense,
            netIncome = income - expense,
            transactionCount = monthlyTransactions.size,
            categoryBreakdown = categoryBreakdown
        )
    }
    
    private fun prepareAdviceData(
        transactions: List<Transaction>,
        categories: List<Category>,
        userProfile: UserProfile
    ): AdviceData {
        val recentTransactions = transactions.sortedByDescending { it.date }.take(100)
        val expenseTransactions = recentTransactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        
        val monthlyAvgExpense = expenseTransactions.sumOf { it.amount } / 3 // 最近3个月平均
        val categoryMap = categories.associateBy { it.id }
        
        val spendingPatterns = expenseTransactions
            .groupBy { it.categoryId }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .mapKeys { (categoryId, _) -> categoryMap[categoryId]?.name ?: "其他" }
        
        return AdviceData(
            monthlyAvgExpense = monthlyAvgExpense,
            spendingPatterns = spendingPatterns,
            recentTransactionCount = recentTransactions.size,
            userProfile = userProfile
        )
    }
    
    private fun prepareBudgetAnalysisData(
        budgets: List<com.example.life_ledger.data.model.Budget>,
        transactions: List<Transaction>,
        categories: List<Category>
    ): BudgetAnalysisData {
        val categoryMap = categories.associateBy { it.id }
        val currentTime = System.currentTimeMillis()
        
        val budgetPerformance = budgets.map { budget ->
            val categoryName = budget.categoryId?.let { categoryMap[it]?.name } ?: "总预算"
            val usageRate = if (budget.amount > 0) (budget.spent / budget.amount * 100) else 0.0
            val remainingDays = maxOf(0, ((budget.endDate - currentTime) / (24 * 60 * 60 * 1000)).toInt())
            val isOverBudget = budget.spent > budget.amount
            val warningTriggered = usageRate >= budget.alertThreshold * 100
            
            BudgetPerformance(
                name = budget.name,
                category = categoryName,
                budgetAmount = budget.amount,
                spentAmount = budget.spent,
                usageRate = usageRate,
                remainingDays = remainingDays,
                isOverBudget = isOverBudget,
                warningTriggered = warningTriggered,
                period = budget.period.displayName
            )
        }
        
        val recentTransactions = transactions.filter { 
            it.type == Transaction.TransactionType.EXPENSE && 
            it.date >= currentTime - 30 * 24 * 60 * 60 * 1000L // 最近30天
        }
        
        val categorySpending = recentTransactions
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .mapKeys { (categoryId, _) -> categoryMap[categoryId]?.name ?: "其他" }
        
        return BudgetAnalysisData(
            budgetPerformance = budgetPerformance,
            categorySpending = categorySpending,
            totalBudgets = budgets.size,
            overBudgetCount = budgetPerformance.count { it.isOverBudget },
            averageUsageRate = budgetPerformance.map { it.usageRate }.average()
        )
    }
    
    // Prompt构建方法
    
    private fun buildExpenseAnalysisPrompt(data: ExpenseData): String {
        return """
        请分析以下支出数据并提供专业的财务分析：
        
        总支出：¥${String.format("%.2f", data.totalExpense)}
        日均支出：¥${String.format("%.2f", data.avgDailyExpense)}
        交易笔数：${data.transactionCount}
        
        支出分类明细：
        ${data.categoryExpenses.map { "${it.key}: ¥${String.format("%.2f", it.value)}" }.joinToString("\n")}
        
        请从以下角度进行分析：
        1. 支出结构分析（各类别占比是否合理）
        2. 消费习惯评估（频率、金额特点）
        3. 潜在问题识别（过度消费的类别）
        4. 优化建议（具体的改进措施）
        
        请用中文回答，结构清晰，建议具体可执行。
        """.trimIndent()
    }
    
    private fun buildMonthlyReportPrompt(data: MonthlyReportData, year: Int, month: Int): String {
        return """
        请生成${year}年${month}月的详细财务报告：
        
        基本数据：
        - 总收入：¥${String.format("%.2f", data.totalIncome)}
        - 总支出：¥${String.format("%.2f", data.totalExpense)}
        - 净收入：¥${String.format("%.2f", data.netIncome)}
        - 交易笔数：${data.transactionCount}
        
        分类明细：
        ${data.categoryBreakdown.map { "${it.key}: ${it.value.count}笔, ¥${String.format("%.2f", it.value.amount)}" }.joinToString("\n")}
        
        请包含以下内容：
        1. 财务状况总结
        2. 收支分析
        3. 消费结构分析
        4. 与理想财务状况的对比
        5. 下月改进建议
        
        报告要专业、详细、易懂。
        """.trimIndent()
    }
    
    private fun buildAdvicePrompt(data: AdviceData, userProfile: UserProfile): String {
        return """
        基于以下用户信息和消费数据，请提供个性化的理财建议：
        
        用户信息：
        - 年龄：${userProfile.age}岁
        - 收入水平：${userProfile.incomeLevel}
        - 理财目标：${userProfile.financialGoals.joinToString(", ")}
        
        消费数据：
        - 月均支出：¥${String.format("%.2f", data.monthlyAvgExpense)}
        - 最近交易：${data.recentTransactionCount}笔
        
        支出分布：
        ${data.spendingPatterns.map { "${it.key}: ¥${String.format("%.2f", it.value)}" }.joinToString("\n")}
        
        请提供5-8条个性化建议，每条建议包括：
        1. 建议标题
        2. 详细说明
        3. 预期效果
        4. 执行难度（简单/中等/困难）
        
        建议要实用、具体、符合用户特点。
        """.trimIndent()
    }
    
    private fun buildBudgetRecommendationPrompt(data: BudgetAnalysisData): String {
        return buildString {
            appendLine("作为专业的预算分析师，请分析以下预算使用情况，并提供2-4条简洁实用的建议：")
            appendLine()
            
            appendLine("【预算使用情况分析】")
            data.budgetPerformance.forEach { budget ->
                val remaining = budget.budgetAmount - budget.spentAmount
                appendLine("• ${budget.name}：")
                appendLine("  - 预算总额：¥${String.format("%.0f", budget.budgetAmount)}")
                appendLine("  - 已使用：¥${String.format("%.0f", budget.spentAmount)}")
                appendLine("  - 剩余金额：¥${String.format("%.0f", remaining)}")
                appendLine("  - 使用率：${String.format("%.1f", budget.usageRate)}%")
                appendLine("  - 剩余时间：${budget.remainingDays}天")
                appendLine()
            }
            
            appendLine("【总体情况】")
            appendLine("- 预算总数：${data.totalBudgets}个")
            appendLine("- 超支预算：${data.overBudgetCount}个")
            appendLine("- 平均使用率：${String.format("%.1f", data.averageUsageRate)}%")
            appendLine()
            
            appendLine("请基于以上数据提供建议，每条建议格式如下：")
            appendLine("建议标题|详细分析和具体建议（包含数据分析和改进措施）|优先级（高/中/低）")
            appendLine()
            appendLine("要求：")
            appendLine("1. 每条建议要完整详细，无需点击查看更多")
            appendLine("2. 结合具体数据进行分析")
            appendLine("3. 提供可执行的改进措施")
            appendLine("4. 优先分析使用率异常的预算")
            appendLine("5. 建议控制在150字以内但要包含关键信息")
        }
    }
    
    // 解析方法
    
    private fun parseExpenseAnalysis(content: String): ExpenseAnalysis {
        return ExpenseAnalysis(
            summary = content.substringBefore("\n\n").ifEmpty { content },
            structureAnalysis = extractSection(content, "支出结构分析", "消费习惯评估") ?: "暂无分析",
            habitAssessment = extractSection(content, "消费习惯评估", "潜在问题识别") ?: "暂无评估", 
            problemIdentification = extractSection(content, "潜在问题识别", "优化建议") ?: "暂无问题",
            optimizationSuggestions = extractSection(content, "优化建议", null) ?: "暂无建议",
            generatedAt = System.currentTimeMillis()
        )
    }
    
    private fun parseMonthlyReport(content: String, year: Int, month: Int): MonthlyReport {
        return MonthlyReport(
            year = year,
            month = month,
            content = content,
            summary = content.substringBefore("\n\n").ifEmpty { content.take(200) },
            generatedAt = System.currentTimeMillis()
        )
    }
    
    private fun parseConsumptionAdvice(content: String): List<ConsumptionAdvice> {
        val adviceList = mutableListOf<ConsumptionAdvice>()
        val lines = content.split("\n")
        
        var currentTitle = ""
        var currentDescription = ""
        var currentEffect = ""
        var currentDifficulty = "中等"
        
        for (line in lines) {
            when {
                line.contains("建议") && line.contains("：") -> {
                    if (currentTitle.isNotEmpty()) {
                        adviceList.add(ConsumptionAdvice(
                            title = currentTitle,
                            description = currentDescription,
                            expectedEffect = currentEffect,
                            difficulty = currentDifficulty,
                            priority = adviceList.size + 1
                        ))
                    }
                    currentTitle = line.substringAfter("：").trim()
                    currentDescription = ""
                    currentEffect = ""
                    currentDifficulty = "中等"
                }
                line.contains("说明") -> currentDescription = line.substringAfter("：").trim()
                line.contains("效果") -> currentEffect = line.substringAfter("：").trim()
                line.contains("难度") -> currentDifficulty = when {
                    line.contains("简单") -> "简单"
                    line.contains("困难") -> "困难"
                    else -> "中等"
                }
            }
        }
        
        // 添加最后一个建议
        if (currentTitle.isNotEmpty()) {
            adviceList.add(ConsumptionAdvice(
                title = currentTitle,
                description = currentDescription,
                expectedEffect = currentEffect,
                difficulty = currentDifficulty,
                priority = adviceList.size + 1
            ))
        }
        
        return adviceList.ifEmpty { 
            listOf(ConsumptionAdvice(
                title = "继续保持",
                description = "您的消费习惯总体良好，建议继续保持当前的理财方式。",
                expectedEffect = "维持财务稳定",
                difficulty = "简单",
                priority = 1
            ))
        }
    }
    
    private fun parseBudgetRecommendations(content: String): List<com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation> {
        val recommendations = mutableListOf<com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation>()
        
        // 按行分割并处理新格式：建议标题|详细描述|优先级
        val lines = content.split("\n").filter { it.trim().isNotEmpty() }
        
        for (line in lines) {
            val trimmedLine = line.trim()
            
            // 检查是否为建议格式（包含 | 分隔符）
            if (trimmedLine.contains("|") && !trimmedLine.startsWith("建议标题")) {
                val parts = trimmedLine.split("|")
                if (parts.size >= 2) {
                    val title = parts[0].trim()
                    val description = parts[1].trim()
                    val priorityText = if (parts.size >= 3) parts[2].trim() else "中"
                    
                    // 确定优先级
                    val priority = when {
                        priorityText.contains("高") -> com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.Priority.HIGH
                        priorityText.contains("低") -> com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.Priority.LOW
                        else -> com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.Priority.MEDIUM
                    }
                    
                    // 根据建议内容确定类型
                    val type = when {
                        title.contains("超支") || description.contains("超支") -> 
                            com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.RecommendationType.OVERSPENDING
                        title.contains("调整") || description.contains("调整") -> 
                            com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.RecommendationType.BUDGET_ADJUSTMENT
                        title.contains("节省") || description.contains("节省") -> 
                            com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.RecommendationType.SAVINGS_OPPORTUNITY
                        title.contains("分类") || description.contains("分类") -> 
                            com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.RecommendationType.CATEGORY_OPTIMIZATION
                        title.contains("习惯") || description.contains("习惯") -> 
                            com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.RecommendationType.SPENDING_PATTERN
                        else -> com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.RecommendationType.BUDGET_ADJUSTMENT
                    }
                    
                    recommendations.add(
                        com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation(
                            id = "ai_rec_${System.currentTimeMillis()}_${recommendations.size}",
                            type = type,
                            title = title,
                            description = description,
                            priority = priority,
                            actionText = null // 不需要操作按钮
                        )
                    )
                }
            }
        }
        
        // 如果解析失败或没有建议，返回基于数据的默认建议
        if (recommendations.isEmpty()) {
            recommendations.addAll(generateDataBasedRecommendations())
        }
        
        return recommendations.take(4) // 最多4条建议
    }
    
    private fun generateDataBasedRecommendations(): List<com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation> {
        return listOf(
            com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation(
                id = "data_based_1",
                type = com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.RecommendationType.BUDGET_ADJUSTMENT,
                title = "预算执行分析",
                description = "根据当前预算使用情况，建议定期检查和调整预算分配，确保预算设置符合实际消费需求。可以将使用率低的预算金额转移到使用率高的分类中。",
                priority = com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.Priority.MEDIUM
            ),
            com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation(
                id = "data_based_2",
                type = com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.RecommendationType.SPENDING_PATTERN,
                title = "支出习惯优化",
                description = "建议记录每笔支出的详细信息，养成记账习惯。定期分析支出模式，识别不必要的开支，逐步培养理性消费的习惯。",
                priority = com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.Priority.LOW
            )
        )
    }
    
    private fun extractSection(content: String, startMarker: String, endMarker: String?): String? {
        val startIndex = content.indexOf(startMarker)
        if (startIndex == -1) return null
        
        val endIndex = if (endMarker != null) {
            val endIdx = content.indexOf(endMarker, startIndex)
            if (endIdx == -1) content.length else endIdx
        } else {
            content.length
        }
        
        return content.substring(startIndex, endIndex)
            .removePrefix(startMarker)
            .trim()
            .ifEmpty { null }
    }
}

// 数据类定义

data class ExpenseData(
    val totalExpense: Double,
    val avgDailyExpense: Double,
    val categoryExpenses: Map<String, Double>,
    val topCategories: List<Pair<String, Double>>,
    val transactionCount: Int
)

data class MonthlyReportData(
    val year: Int,
    val month: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val netIncome: Double,
    val transactionCount: Int,
    val categoryBreakdown: Map<String, TransactionSummary>
)

data class TransactionSummary(
    val count: Int,
    val amount: Double
)

data class AdviceData(
    val monthlyAvgExpense: Double,
    val spendingPatterns: Map<String, Double>,
    val recentTransactionCount: Int,
    val userProfile: UserProfile
)

data class UserProfile(
    val age: Int = 25,
    val incomeLevel: String = "中等",
    val financialGoals: List<String> = listOf("储蓄", "理财")
)

data class ExpenseAnalysis(
    val summary: String,
    val structureAnalysis: String,
    val habitAssessment: String,
    val problemIdentification: String,
    val optimizationSuggestions: String,
    val generatedAt: Long
)

data class MonthlyReport(
    val year: Int,
    val month: Int,
    val content: String,
    val summary: String,
    val generatedAt: Long
)

data class ConsumptionAdvice(
    val title: String,
    val description: String,
    val expectedEffect: String,
    val difficulty: String, // "简单", "中等", "困难"
    val priority: Int
)

data class BudgetAnalysisData(
    val budgetPerformance: List<BudgetPerformance>,
    val categorySpending: Map<String, Double>,
    val totalBudgets: Int,
    val overBudgetCount: Int,
    val averageUsageRate: Double
)

data class BudgetPerformance(
    val name: String,
    val category: String,
    val budgetAmount: Double,
    val spentAmount: Double,
    val usageRate: Double,
    val remainingDays: Int,
    val isOverBudget: Boolean,
    val warningTriggered: Boolean,
    val period: String
) 