package com.example.life_ledger.data.service

import com.example.life_ledger.data.model.Transaction
import com.example.life_ledger.data.model.Category
import com.example.life_ledger.data.network.NetworkClient
import com.example.life_ledger.data.network.ChatCompletionRequest
import com.example.life_ledger.data.network.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.net.SocketTimeoutException
import java.io.IOException

/**
 * AI Analysis Service
 * Uses DeepSeek API for intelligent financial analysis
 */
class AIAnalysisService {
    
    private val apiService = NetworkClient.deepSeekApiService
    private val dateFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    
    companion object {
        private const val TAG = "AIAnalysisService"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
    }
    
    /**
     * Intelligent expense analysis with retry mechanism
     */
    suspend fun analyzeExpenses(
        transactions: List<Transaction>,
        categories: List<Category>
    ): Result<ExpenseAnalysis> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                android.util.Log.d(TAG, "Starting expense analysis, attempt ${attempt + 1}/$MAX_RETRIES")
                
                val expenseData = prepareExpenseData(transactions, categories)
                val prompt = buildExpenseAnalysisPrompt(expenseData)
                
                android.util.Log.d(TAG, "Prepared data - Total expense: ${expenseData.totalExpense}, Categories: ${expenseData.categoryExpenses.size}")
                android.util.Log.d(TAG, "Sending request to AI API...")
                
                val response = apiService.chatCompletion(
                    ChatCompletionRequest(
                        messages = listOf(
                            Message("system", "You are a professional financial analyst specializing in analyzing personal spending patterns and providing professional advice."),
                            Message("user", prompt)
                        ),
                        max_tokens = 1500,
                        temperature = 0.7
                    )
                )
                
                android.util.Log.d(TAG, "Received response - Success: ${response.isSuccessful}, Code: ${response.code()}")
                
                if (response.isSuccessful && response.body()?.choices?.isNotEmpty() == true) {
                    val content = response.body()!!.choices[0].message.content
                    android.util.Log.d(TAG, "AI response received, content length: ${content.length}")
                    
                    val analysis = parseExpenseAnalysis(content)
                    android.util.Log.d(TAG, "Analysis parsed successfully")
                    return@withContext Result.success(analysis)
                } else {
                    val errorMsg = "AI analysis request failed: ${response.code()} - ${response.message()}"
                    android.util.Log.e(TAG, errorMsg)
                    lastException = Exception(errorMsg)
                }
            } catch (e: SocketTimeoutException) {
                val timeoutMsg = "Request timeout on attempt ${attempt + 1}. This may be due to network issues or AI service being busy."
                android.util.Log.w(TAG, timeoutMsg, e)
                lastException = Exception(timeoutMsg, e)
                
                if (attempt < MAX_RETRIES - 1) {
                    android.util.Log.d(TAG, "Retrying in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            } catch (e: IOException) {
                val networkMsg = "Network error on attempt ${attempt + 1}: ${e.message}"
                android.util.Log.w(TAG, networkMsg, e)
                lastException = Exception(networkMsg, e)
                
                if (attempt < MAX_RETRIES - 1) {
                    android.util.Log.d(TAG, "Retrying in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            } catch (e: Exception) {
                val errorMsg = "Unexpected error on attempt ${attempt + 1}: ${e.message}"
                android.util.Log.e(TAG, errorMsg, e)
                lastException = e
                
                if (attempt < MAX_RETRIES - 1) {
                    android.util.Log.d(TAG, "Retrying in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        
        android.util.Log.e(TAG, "All retry attempts failed")
        Result.failure(lastException ?: Exception("Analysis failed after $MAX_RETRIES attempts"))
    }
    
    /**
     * Generate monthly financial report with retry mechanism
     */
    suspend fun generateMonthlyReport(
        transactions: List<Transaction>,
        categories: List<Category>,
        year: Int,
        month: Int
    ): Result<MonthlyReport> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                android.util.Log.d(TAG, "Starting monthly report generation, attempt ${attempt + 1}/$MAX_RETRIES")
                
                val reportData = prepareMonthlyReportData(transactions, categories, year, month)
                val prompt = buildMonthlyReportPrompt(reportData, year, month)
                
                android.util.Log.d(TAG, "Prepared monthly data for $year-$month - Income: ${reportData.totalIncome}, Expense: ${reportData.totalExpense}")
                android.util.Log.d(TAG, "Sending monthly report request to AI API...")
                
                val response = apiService.chatCompletion(
                    ChatCompletionRequest(
                        messages = listOf(
                            Message("system", "You are a financial advisor specializing in generating detailed monthly financial reports for users. Reports should be professional, accurate, and easy to understand."),
                            Message("user", prompt)
                        ),
                        max_tokens = 2000,
                        temperature = 0.6
                    )
                )
                
                android.util.Log.d(TAG, "Received monthly report response - Success: ${response.isSuccessful}, Code: ${response.code()}")
                
                if (response.isSuccessful && response.body()?.choices?.isNotEmpty() == true) {
                    val content = response.body()!!.choices[0].message.content
                    android.util.Log.d(TAG, "Monthly report AI response received, content length: ${content.length}")
                    
                    val report = parseMonthlyReport(content, year, month)
                    android.util.Log.d(TAG, "Monthly report parsed successfully")
                    return@withContext Result.success(report)
                } else {
                    val errorMsg = "Monthly report generation failed: ${response.code()} - ${response.message()}"
                    android.util.Log.e(TAG, errorMsg)
                    lastException = Exception(errorMsg)
                }
            } catch (e: SocketTimeoutException) {
                val timeoutMsg = "Monthly report request timeout on attempt ${attempt + 1}. The AI service might be processing your request."
                android.util.Log.w(TAG, timeoutMsg, e)
                lastException = Exception(timeoutMsg, e)
                
                if (attempt < MAX_RETRIES - 1) {
                    android.util.Log.d(TAG, "Retrying monthly report in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            } catch (e: IOException) {
                val networkMsg = "Network error during monthly report on attempt ${attempt + 1}: ${e.message}"
                android.util.Log.w(TAG, networkMsg, e)
                lastException = Exception(networkMsg, e)
                
                if (attempt < MAX_RETRIES - 1) {
                    android.util.Log.d(TAG, "Retrying monthly report in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            } catch (e: Exception) {
                val errorMsg = "Unexpected error during monthly report on attempt ${attempt + 1}: ${e.message}"
                android.util.Log.e(TAG, errorMsg, e)
                lastException = e
                
                if (attempt < MAX_RETRIES - 1) {
                    android.util.Log.d(TAG, "Retrying monthly report in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        
        android.util.Log.e(TAG, "Monthly report generation failed after all retry attempts")
        Result.failure(lastException ?: Exception("Monthly report generation failed after $MAX_RETRIES attempts"))
    }
    
    /**
     * Get personalized consumption advice with retry mechanism
     */
    suspend fun getPersonalizedAdvice(
        transactions: List<Transaction>,
        categories: List<Category>,
        userProfile: UserProfile
    ): Result<List<ConsumptionAdvice>> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                android.util.Log.d(TAG, "Starting personalized advice generation, attempt ${attempt + 1}/$MAX_RETRIES")
                
                val adviceData = prepareAdviceData(transactions, categories, userProfile)
                val prompt = buildAdvicePrompt(adviceData, userProfile)
                
                android.util.Log.d(TAG, "Prepared advice data - Avg expense: ${adviceData.monthlyAvgExpense}, Patterns: ${adviceData.spendingPatterns.size}")
                android.util.Log.d(TAG, "Sending personalized advice request to AI API...")
                
                val response = apiService.chatCompletion(
                    ChatCompletionRequest(
                        messages = listOf(
                            Message("system", "You are a financial expert who provides personalized financial advice based on users' spending habits and financial situation."),
                            Message("user", prompt)
                        ),
                        max_tokens = 1500,
                        temperature = 0.8
                    )
                )
                
                android.util.Log.d(TAG, "Received advice response - Success: ${response.isSuccessful}, Code: ${response.code()}")
                
                if (response.isSuccessful && response.body()?.choices?.isNotEmpty() == true) {
                    val content = response.body()!!.choices[0].message.content
                    android.util.Log.d(TAG, "Personalized advice AI response received, content length: ${content.length}")
                    
                    val advice = parseConsumptionAdvice(content)
                    android.util.Log.d(TAG, "Personalized advice parsed successfully, count: ${advice.size}")
                    return@withContext Result.success(advice)
                } else {
                    val errorMsg = "Personalized advice generation failed: ${response.code()} - ${response.message()}"
                    android.util.Log.e(TAG, errorMsg)
                    lastException = Exception(errorMsg)
                }
            } catch (e: SocketTimeoutException) {
                val timeoutMsg = "Personalized advice request timeout on attempt ${attempt + 1}. Please wait while we process your data."
                android.util.Log.w(TAG, timeoutMsg, e)
                lastException = Exception(timeoutMsg, e)
                
                if (attempt < MAX_RETRIES - 1) {
                    android.util.Log.d(TAG, "Retrying personalized advice in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            } catch (e: IOException) {
                val networkMsg = "Network error during personalized advice on attempt ${attempt + 1}: ${e.message}"
                android.util.Log.w(TAG, networkMsg, e)
                lastException = Exception(networkMsg, e)
                
                if (attempt < MAX_RETRIES - 1) {
                    android.util.Log.d(TAG, "Retrying personalized advice in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            } catch (e: Exception) {
                val errorMsg = "Unexpected error during personalized advice on attempt ${attempt + 1}: ${e.message}"
                android.util.Log.e(TAG, errorMsg, e)
                lastException = e
                
                if (attempt < MAX_RETRIES - 1) {
                    android.util.Log.d(TAG, "Retrying personalized advice in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        
        android.util.Log.e(TAG, "Personalized advice generation failed after all retry attempts")
        Result.failure(lastException ?: Exception("Personalized advice generation failed after $MAX_RETRIES attempts"))
    }
    
    /**
     * Get intelligent budget recommendations with retry mechanism
     */
    suspend fun getBudgetRecommendations(
        budgets: List<com.example.life_ledger.data.model.Budget>,
        transactions: List<Transaction>,
        categories: List<Category>
    ): Result<List<com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation>> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                android.util.Log.d(TAG, "Starting budget recommendations generation, attempt ${attempt + 1}/$MAX_RETRIES")
                
                val budgetData = prepareBudgetAnalysisData(budgets, transactions, categories)
                val prompt = buildBudgetRecommendationPrompt(budgetData)
                
                android.util.Log.d(TAG, "Prepared budget data - Total budgets: ${budgetData.totalBudgets}, Over budget: ${budgetData.overBudgetCount}")
                android.util.Log.d(TAG, "Sending budget recommendations request to AI API...")
                
                val response = apiService.chatCompletion(
                    ChatCompletionRequest(
                        messages = listOf(
                            Message("system", "You are a professional budget management consultant specializing in analyzing users' budget execution and providing practical optimization recommendations."),
                            Message("user", prompt)
                        ),
                        max_tokens = 1500,
                        temperature = 0.7
                    )
                )
                
                android.util.Log.d(TAG, "Received budget recommendations response - Success: ${response.isSuccessful}, Code: ${response.code()}")
                
                if (response.isSuccessful && response.body()?.choices?.isNotEmpty() == true) {
                    val content = response.body()!!.choices[0].message.content
                    android.util.Log.d(TAG, "Budget recommendations AI response received, content length: ${content.length}")
                    
                    val recommendations = parseBudgetRecommendations(content)
                    android.util.Log.d(TAG, "Budget recommendations parsed successfully, count: ${recommendations.size}")
                    return@withContext Result.success(recommendations)
                } else {
                    val errorMsg = "Budget recommendation generation failed: ${response.code()} - ${response.message()}"
                    android.util.Log.e(TAG, errorMsg)
                    lastException = Exception(errorMsg)
                }
            } catch (e: SocketTimeoutException) {
                val timeoutMsg = "Budget recommendations request timeout on attempt ${attempt + 1}. Please wait while we analyze your budget data."
                android.util.Log.w(TAG, timeoutMsg, e)
                lastException = Exception(timeoutMsg, e)
                
                if (attempt < MAX_RETRIES - 1) {
                    android.util.Log.d(TAG, "Retrying budget recommendations in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            } catch (e: IOException) {
                val networkMsg = "Network error during budget recommendations on attempt ${attempt + 1}: ${e.message}"
                android.util.Log.w(TAG, networkMsg, e)
                lastException = Exception(networkMsg, e)
                
                if (attempt < MAX_RETRIES - 1) {
                    android.util.Log.d(TAG, "Retrying budget recommendations in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            } catch (e: Exception) {
                val errorMsg = "Unexpected error during budget recommendations on attempt ${attempt + 1}: ${e.message}"
                android.util.Log.e(TAG, errorMsg, e)
                lastException = e
                
                if (attempt < MAX_RETRIES - 1) {
                    android.util.Log.d(TAG, "Retrying budget recommendations in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        
        android.util.Log.e(TAG, "Budget recommendations generation failed after all retry attempts")
        Result.failure(lastException ?: Exception("Budget recommendation generation failed after $MAX_RETRIES attempts"))
    }
    
    // Data preparation methods
    
    private fun prepareExpenseData(transactions: List<Transaction>, categories: List<Category>): ExpenseData {
        val expenseTransactions = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }
        val categoryMap = categories.associateBy { it.id }
        
        val categoryExpenses = expenseTransactions
            .groupBy { it.categoryId }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .mapKeys { (categoryId, _) -> categoryMap[categoryId]?.name ?: "Other" }
        
        val totalExpense = expenseTransactions.sumOf { it.amount }
        val avgDailyExpense = if (expenseTransactions.isNotEmpty()) {
            totalExpense / 30 // Assume 30 days
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
            .mapKeys { (categoryId, _) -> categoryMap[categoryId]?.name ?: "Other" }
        
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
            .mapKeys { (categoryId, _) -> categoryMap[categoryId]?.name ?: "Other" }
        
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
            val categoryName = budget.categoryId?.let { categoryMap[it]?.name } ?: "Total Budget"
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
            .mapKeys { (categoryId, _) -> categoryMap[categoryId]?.name ?: "Other" }
        
        return BudgetAnalysisData(
            budgetPerformance = budgetPerformance,
            categorySpending = categorySpending,
            totalBudgets = budgets.size,
            overBudgetCount = budgetPerformance.count { it.isOverBudget },
            averageUsageRate = budgetPerformance.map { it.usageRate }.average()
        )
    }
    
    // Prompt building methods
    
    private fun buildExpenseAnalysisPrompt(data: ExpenseData): String {
        return """
        Please analyze the following expense data and provide professional financial analysis:
        
        Total expenses: ¥${String.format("%.2f", data.totalExpense)}
        Daily expenses: ¥${String.format("%.2f", data.avgDailyExpense)}
        Transaction count: ${data.transactionCount}
        
        Expense breakdown:
        ${data.categoryExpenses.map { "${it.key}: ¥${String.format("%.2f", it.value)}" }.joinToString("\n")}
        
        Please analyze from the following perspectives:
        1. Expense structure analysis (whether the proportion of each category is reasonable)
        2. Consumption habit assessment (frequency and amount characteristics)
        3. Potential problem identification (categories of excessive consumption)
        4. Optimization suggestions (specific improvement measures)
        
        Please answer in Chinese, with clear structure and specific, executable suggestions.
        """.trimIndent()
    }
    
    private fun buildMonthlyReportPrompt(data: MonthlyReportData, year: Int, month: Int): String {
        return """
        Please generate a detailed financial report for ${year}-${month}:
        
        Basic data:
        - Total income: ¥${String.format("%.2f", data.totalIncome)}
        - Total expenses: ¥${String.format("%.2f", data.totalExpense)}
        - Net income: ¥${String.format("%.2f", data.netIncome)}
        - Transaction count: ${data.transactionCount}
        
        Expense breakdown:
        ${data.categoryBreakdown.map { "${it.key}: ${it.value.count} transactions, ¥${String.format("%.2f", it.value.amount)}" }.joinToString("\n")}
        
        Please include the following:
        1. Financial situation summary
        2. Income and expense analysis
        3. Consumption structure analysis
        4. Comparison with ideal financial situation
        5. Next month improvement suggestions
        
        The report should be professional, detailed, and easy to understand.
        """.trimIndent()
    }
    
    private fun buildAdvicePrompt(data: AdviceData, userProfile: UserProfile): String {
        return """
        Based on the following user information and consumption data, please provide personalized financial advice:
        
        User information:
        - Age: ${userProfile.age} years old
        - Income level: ${userProfile.incomeLevel}
        - Financial goals: ${userProfile.financialGoals.joinToString(", ")}
        
        Consumption data:
        - Monthly average expenses: ¥${String.format("%.2f", data.monthlyAvgExpense)}
        - Recent transactions: ${data.recentTransactionCount} transactions
        
        Expense distribution:
        ${data.spendingPatterns.map { "${it.key}: ¥${String.format("%.2f", it.value)}" }.joinToString("\n")}
        
        Please provide 5-8 personalized advice, each including:
        1. Advice title
        2. Detailed description
        3. Expected effect
        4. Execution difficulty (simple/medium/difficult)
        
        The advice should be practical, specific, and in line with user characteristics.
        """.trimIndent()
    }
    
    private fun buildBudgetRecommendationPrompt(data: BudgetAnalysisData): String {
        return buildString {
            appendLine("As a professional budget analyst, please analyze the following budget usage situation and provide 2-4 concise and practical suggestions:")
            appendLine()
            
            appendLine("【Budget usage analysis】")
            data.budgetPerformance.forEach { budget ->
                val remaining = budget.budgetAmount - budget.spentAmount
                appendLine("• ${budget.name}:")
                appendLine("  - Total budget: ¥${String.format("%.0f", budget.budgetAmount)}")
                appendLine("  - Spent: ¥${String.format("%.0f", budget.spentAmount)}")
                appendLine("  - Remaining amount: ¥${String.format("%.0f", remaining)}")
                appendLine("  - Usage rate: ${String.format("%.1f", budget.usageRate)}%")
                appendLine("  - Remaining time: ${budget.remainingDays} days")
                appendLine()
            }
            
            appendLine("【Overall situation】")
            appendLine("- Total budgets: ${data.totalBudgets} budgets")
            appendLine("- Over budget budgets: ${data.overBudgetCount} budgets")
            appendLine("- Average usage rate: ${String.format("%.1f", data.averageUsageRate)}%")
            appendLine()
            
            appendLine("Please provide suggestions based on the above data, each suggestion format as follows:")
            appendLine("Advice title|Detailed analysis and specific suggestions (including data analysis and improvement measures)|Priority (High/Medium/Low)")
            appendLine()
            appendLine("Requirements:")
            appendLine("1. Each suggestion must be complete and detailed without clicking to view more")
            appendLine("2. Analysis based on specific data")
            appendLine("3. Provide executable improvement measures")
            appendLine("4. Prioritize analysis of budgets with abnormal usage rate")
            appendLine("5. Suggestions should be控制在150字以内但要包含关键信息")
        }
    }
    
    // Parsing methods
    
    private fun parseExpenseAnalysis(content: String): ExpenseAnalysis {
        return ExpenseAnalysis(
            summary = content.substringBefore("\n\n").ifEmpty { content },
            structureAnalysis = extractSection(content, "Expense structure analysis", "Consumption habit assessment") ?: "No analysis available",
            habitAssessment = extractSection(content, "Consumption habit assessment", "Potential problem identification") ?: "No assessment available", 
            problemIdentification = extractSection(content, "Potential problem identification", "Optimization suggestions") ?: "No problem identified",
            optimizationSuggestions = extractSection(content, "Optimization suggestions", null) ?: "No suggestions generated",
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
        var currentDifficulty = "Medium"
        
        for (line in lines) {
            when {
                line.contains("Advice") && line.contains("：") -> {
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
                    currentDifficulty = "Medium"
                }
                line.contains("Description") -> currentDescription = line.substringAfter("：").trim()
                line.contains("Effect") -> currentEffect = line.substringAfter("：").trim()
                line.contains("Difficulty") -> currentDifficulty = when {
                    line.contains("Simple") -> "Simple"
                    line.contains("Difficult") -> "Difficult"
                    else -> "Medium"
                }
            }
        }
        
        // Add the last advice
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
                title = "Continue to maintain",
                description = "Your consumption habits are generally good, it is recommended to continue maintaining the current financial management method.",
                expectedEffect = "Maintain financial stability",
                difficulty = "Simple",
                priority = 1
            ))
        }
    }
    
    private fun parseBudgetRecommendations(content: String): List<com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation> {
        val recommendations = mutableListOf<com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation>()
        
        // Split by line and process new format: Advice title|Detailed description|Priority
        val lines = content.split("\n").filter { it.trim().isNotEmpty() }
        
        for (line in lines) {
            val trimmedLine = line.trim()
            
            // Check if it's an advice format (contains | separator)
            if (trimmedLine.contains("|") && !trimmedLine.startsWith("Advice title")) {
                val parts = trimmedLine.split("|")
                if (parts.size >= 2) {
                    val title = parts[0].trim()
                    val description = parts[1].trim()
                    val priorityText = if (parts.size >= 3) parts[2].trim() else "Medium"
                    
                    // Determine priority
                    val priority = when {
                        priorityText.contains("High") -> com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.Priority.HIGH
                        priorityText.contains("Low") -> com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.Priority.LOW
                        else -> com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.Priority.MEDIUM
                    }
                    
                    // Determine type based on advice content
                    val type = when {
                        title.contains("Over") || description.contains("Over") -> 
                            com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.RecommendationType.OVERSPENDING
                        title.contains("Adjust") || description.contains("Adjust") -> 
                            com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.RecommendationType.BUDGET_ADJUSTMENT
                        title.contains("Save") || description.contains("Save") -> 
                            com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.RecommendationType.SAVINGS_OPPORTUNITY
                        title.contains("Category") || description.contains("Category") -> 
                            com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.RecommendationType.CATEGORY_OPTIMIZATION
                        title.contains("Habit") || description.contains("Habit") -> 
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
                            actionText = null // No need for action button
                        )
                    )
                }
            }
        }
        
        // If parsing fails or no suggestions, return data-based default suggestions
        if (recommendations.isEmpty()) {
            recommendations.addAll(generateDataBasedRecommendations())
        }
        
        return recommendations.take(4) // Maximum 4 suggestions
    }
    
    private fun generateDataBasedRecommendations(): List<com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation> {
        return listOf(
            com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation(
                id = "data_based_1",
                type = com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.RecommendationType.BUDGET_ADJUSTMENT,
                title = "Budget execution analysis",
                description = "Based on the current budget usage situation, it is recommended to regularly check and adjust the budget allocation to ensure that the budget setting is in line with actual consumption needs. The amount of low usage rate budgets can be transferred to high usage rate categories.",
                priority = com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.Priority.MEDIUM
            ),
            com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation(
                id = "data_based_2",
                type = com.example.life_ledger.ui.budget.viewmodel.BudgetRecommendation.RecommendationType.SPENDING_PATTERN,
                title = "Spending habit optimization",
                description = "It is recommended to record the detailed information of each transaction, establish a bookkeeping habit. Regularly analyze spending patterns, identify unnecessary expenses, and gradually cultivate rational consumption habits.",
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

// Data class definitions

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
    val incomeLevel: String = "Medium",
    val financialGoals: List<String> = listOf("Savings", "Financing")
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
    val difficulty: String, // "Simple", "Medium", "Difficult"
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