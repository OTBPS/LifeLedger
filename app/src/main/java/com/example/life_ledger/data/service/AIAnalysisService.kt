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