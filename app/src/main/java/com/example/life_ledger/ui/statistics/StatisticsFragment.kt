package com.example.life_ledger.ui.statistics

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.life_ledger.R
import com.example.life_ledger.data.database.AppDatabase
import com.example.life_ledger.data.repository.LifeLedgerRepository
import com.example.life_ledger.databinding.FragmentStatisticsBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * 统计页面Fragment
 * 展示财务数据的各种图表和统计信息
 */
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: LifeLedgerRepository
    private lateinit var viewModel: StatisticsViewModel

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    private var currentPieChartType = PieChartType.EXPENSE
    private var currentTrendType = TrendType.EXPENSE

    enum class PieChartType {
        EXPENSE, INCOME
    }

    enum class TrendType {
        EXPENSE, INCOME
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_statistics,
            container,
            false
        )
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRepository()
        setupCharts()
        setupClickListeners()
        setupTimeRangeChips()
        setupTrendTypeChips()
        setupPieChartTypeChips()
        setupMonthlyRangeChips()
        setupObservers()
    }

    /**
     * 初始化Repository
     */
    private fun setupRepository() {
        val database = AppDatabase.getDatabase(requireContext())
        repository = LifeLedgerRepository(
            database.transactionDao(),
            database.todoDao(),
            database.categoryDao(),
            database.budgetDao(),
            database.userSettingsDao()
        )
        
        // 初始化ViewModel
        viewModel = StatisticsViewModel(repository)
        
        // 确保默认数据已初始化
        lifecycleScope.launch {
            try {
                repository.initializeDefaultData()
                
                // 一次性清理：删除所有现有的交易数据（包括之前的模拟数据）
                val sharedPrefs = requireContext().getSharedPreferences("life_ledger_prefs", android.content.Context.MODE_PRIVATE)
                val hasCleanedData = sharedPrefs.getBoolean("has_cleaned_mock_data", false)
                
                if (!hasCleanedData) {
                    android.util.Log.d("StatisticsFragment", "清理所有模拟交易数据...")
                    repository.deleteAllTransactions()
                    
                    // 标记已清理，避免下次再次清理用户的真实数据
                    sharedPrefs.edit().putBoolean("has_cleaned_mock_data", true).apply()
                    android.util.Log.d("StatisticsFragment", "模拟数据清理完成")
                    
                    // 刷新统计数据
                    viewModel.refresh()
                }
            } catch (e: Exception) {
                android.util.Log.w("StatisticsFragment", "初始化或数据清理失败: ${e.message}")
            }
        }
    }

    /**
     * 设置图表基本配置
     */
    private fun setupCharts() {
        setupLineChart()
        setupPieChart()
        setupBarChart()
    }

    /**
     * 设置折线图
     */
    private fun setupLineChart() {
        binding.lineChartExpenseTrend.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            
            // X轴设置
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.GRAY
                textSize = 10f
            }
            
            // 左Y轴设置
            axisLeft.apply {
                setDrawGridLines(true)
                granularity = 1f
                setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                textColor = Color.GRAY
                textSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "¥${value.toInt()}"
                    }
                }
            }
            
            // 右Y轴设置
            axisRight.isEnabled = false
            
            // 图例设置
            legend.apply {
                isEnabled = true
                textColor = Color.GRAY
                textSize = 12f
            }
        }
    }

    /**
     * 设置饼图
     */
    private fun setupPieChart() {
        binding.pieChartCategories.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            
            dragDecelerationFrictionCoef = 0.95f
            
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 58f
            transparentCircleRadius = 61f
            
            setDrawCenterText(true)
            setCenterTextTypeface(Typeface.DEFAULT_BOLD)
            setCenterTextSize(18f)
            setCenterTextColor(Color.BLACK)
            
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            
            // 图例设置
            legend.apply {
                isEnabled = true
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
                textColor = Color.GRAY
                textSize = 12f
            }
            
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
        }
    }

    /**
     * 设置条形图
     */
    private fun setupBarChart() {
        binding.barChartMonthly.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            
            // X轴设置
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.GRAY
                textSize = 10f
            }
            
            // 左Y轴设置
            axisLeft.apply {
                setDrawGridLines(true)
                granularity = 1f
                setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                textColor = Color.GRAY
                textSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "¥${value.toInt()}"
                    }
                }
            }
            
            // 右Y轴设置
            axisRight.isEnabled = false
            
            // 图例设置
            legend.apply {
                isEnabled = true
                textColor = Color.GRAY
                textSize = 12f
            }
        }
    }

    /**
     * 设置点击监听器
     */
    private fun setupClickListeners() {
        binding.apply {
            // 时间范围选择
            chipGroupTimeRange.setOnCheckedStateChangeListener { _, checkedIds ->
                val timeRange = when (checkedIds.firstOrNull()) {
                    R.id.chipThisWeek -> StatisticsViewModel.TimeRange.THIS_WEEK
                    R.id.chipThisMonth -> StatisticsViewModel.TimeRange.THIS_MONTH
                    R.id.chipThisYear -> StatisticsViewModel.TimeRange.THIS_YEAR
                    else -> StatisticsViewModel.TimeRange.THIS_MONTH
                }
                viewModel.setTimeRange(timeRange)
            }

            // 趋势类型选择
            chipGroupTrendType.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    currentTrendType = when (checkedIds.first()) {
                        R.id.chipExpenseTrend -> TrendType.EXPENSE
                        R.id.chipIncomeTrend -> TrendType.INCOME
                        else -> TrendType.EXPENSE
                    }
                    
                    // 立即更新趋势图显示
                    updateTrendChartDisplay()
                }
            }
            
            // 饼图类型选择
            chipGroupPieType.setOnCheckedStateChangeListener { _, checkedIds ->
                currentPieChartType = when (checkedIds.firstOrNull()) {
                    R.id.chipExpenseCategory -> PieChartType.EXPENSE
                    R.id.chipIncomeCategory -> PieChartType.INCOME
                    else -> PieChartType.EXPENSE
                }
                updatePieChart()
            }
            
            // 下拉刷新
            swipeRefreshLayout.setOnRefreshListener {
                viewModel.refresh()
            }

            // 数据分析按钮点击处理
            btnViewHealthDetails.setOnClickListener {
                showFinancialHealthDetails()
            }

            btnViewBudgetDetails.setOnClickListener {
                showBudgetTrackingDetails()
            }

            btnViewPatternDetails.setOnClickListener {
                showExpensePatternDetails()
            }

            // AI分析按钮
            buttonAiAnalysis.setOnClickListener {
                navigateToAIAnalysis()
            }
        }
    }

    /**
     * 设置数据观察者
     */
    private fun setupObservers() {
        // 观察财务概览数据
        lifecycleScope.launch {
            viewModel.financialSummary.collect { summary ->
                updateFinancialSummary(summary)
            }
        }

        // 观察支出趋势数据
        lifecycleScope.launch {
            viewModel.expenseTrendData.collect { trendData ->
                if (currentTrendType == TrendType.EXPENSE) {
                    updateTrendChart(trendData, "Expense")
                }
            }
        }

        // 观察收入趋势数据
        lifecycleScope.launch {
            viewModel.incomeTrendData.collect { trendData ->
                if (currentTrendType == TrendType.INCOME) {
                    updateTrendChart(trendData, "Income")
                }
            }
        }

        // 观察分类统计数据
        lifecycleScope.launch {
            viewModel.categoryData.collect { categoryData ->
                if (currentPieChartType == PieChartType.EXPENSE) {
                    updatePieChartData(categoryData)
                }
            }
        }

        // 观察月度统计数据
        lifecycleScope.launch {
            viewModel.monthlyData.collect { monthlyData ->
                updateMonthlyChart(monthlyData)
            }
        }

        // 观察加载状态
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.swipeRefreshLayout.isRefreshing = isLoading
            }
        }

        // 观察空状态
        lifecycleScope.launch {
            viewModel.isEmpty.collect { isEmpty ->
                updateEmptyState(isEmpty)
            }
        }

        // 观察操作结果
        lifecycleScope.launch {
            viewModel.operationResult.collect { result ->
                val message = if (result.isSuccess) result.message else "操作失败: ${result.message}"
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            }
        }

        // 观察数据分析结果
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.financialHealthAssessment.collect { assessment ->
                updateFinancialHealthDisplay(assessment)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.budgetTrackingStatus.collect { budgetStatus ->
                updateBudgetTrackingDisplay(budgetStatus)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.expensePatternAnalysis.collect { patternAnalysis ->
                updateExpensePatternDisplay(patternAnalysis)
            }
        }
    }

    /**
     * 更新财务概览显示
     */
    private fun updateFinancialSummary(summary: FinancialSummaryData) {
        binding.apply {
            tvTotalIncome.text = numberFormat.format(summary.totalIncome)
            tvTotalExpense.text = numberFormat.format(summary.totalExpense)
            tvNetBalance.text = numberFormat.format(summary.netBalance)
            
            // 根据净收支设置颜色
            val balanceColor = if (summary.netBalance >= 0) {
                requireContext().getColor(R.color.md_theme_success)
            } else {
                requireContext().getColor(R.color.md_theme_error)
            }
            tvNetBalance.setTextColor(balanceColor)
        }
    }

    /**
     * 更新趋势图表
     */
    private fun updateTrendChart(trendData: List<DailyExpenseData>, type: String) {
        if (trendData.isEmpty()) {
            binding.lineChartExpenseTrend.clear()
            binding.lineChartExpenseTrend.invalidate()
            return
        }

        val entries = trendData.mapIndexed { index, data ->
            Entry(index.toFloat(), data.amount.toFloat())
        }

        val color = if (type == "Income") Color.GREEN else Color.BLUE
        val dataSet = LineDataSet(entries, "Daily ${type} Amount").apply {
            this.color = color
            setCircleColor(color)
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 9f
            valueTextColor = Color.BLACK
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
            
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0) "¥${value.toInt()}" else ""
                }
            }
        }

        val lineData = LineData(dataSet)
        
        binding.lineChartExpenseTrend.apply {
            data = lineData
            
            // 设置X轴标签
            val dateLabels = trendData.map { data ->
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                    val outputFormat = SimpleDateFormat("MM/dd", Locale.ENGLISH)
                    val date = inputFormat.parse(data.date)
                    outputFormat.format(date!!)
                } catch (e: Exception) {
                    data.date.substring(5) // 取月-日部分
                }
            }
            xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)
            
            animateX(1000, Easing.EaseInOutQuart)
            invalidate()
        }
    }

    /**
     * 切换趋势图类型
     */
    private fun updateTrendChart() {
        when (currentTrendType) {
            TrendType.EXPENSE -> {
                lifecycleScope.launch {
                    viewModel.expenseTrendData.value.let { trendData ->
                        updateTrendChart(trendData, "Expense")
                    }
                }
            }
            TrendType.INCOME -> {
                lifecycleScope.launch {
                    viewModel.incomeTrendData.value.let { trendData ->
                        updateTrendChart(trendData, "Income")
                    }
                }
            }
        }
    }

    /**
     * 立即更新趋势图显示
     */
    private fun updateTrendChartDisplay() {
        when (currentTrendType) {
            TrendType.EXPENSE -> {
                val trendData = viewModel.expenseTrendData.value
                updateTrendChart(trendData, "Expense")
            }
            TrendType.INCOME -> {
                val trendData = viewModel.incomeTrendData.value
                updateTrendChart(trendData, "Income")
            }
        }
    }

    /**
     * 更新饼图
     */
    private fun updatePieChart() {
        when (currentPieChartType) {
            PieChartType.EXPENSE -> {
                lifecycleScope.launch {
                    viewModel.categoryData.collect { categoryData ->
                        updatePieChartData(categoryData)
                    }
                }
            }
            PieChartType.INCOME -> {
                lifecycleScope.launch {
                    viewModel.getCategoryIncomeData().collect { incomeData ->
                        updatePieChartData(incomeData)
                    }
                }
            }
        }
    }

    /**
     * 更新饼图数据
     */
    private fun updatePieChartData(categoryData: List<CategoryExpenseData>) {
        if (categoryData.isEmpty()) {
            binding.pieChartCategories.clear()
            binding.pieChartCategories.invalidate()
            return
        }

        val entries = categoryData.map { data ->
            PieEntry(data.amount.toFloat(), data.categoryName)
        }

        val dataSet = PieDataSet(entries, "").apply {
            setDrawIcons(false)
            sliceSpace = 3f
            iconsOffset = com.github.mikephil.charting.utils.MPPointF(0f, 40f)
            selectionShift = 5f
            
            // 设置颜色
            colors = categoryData.map { data ->
                try {
                    Color.parseColor(data.color)
                } catch (e: Exception) {
                    ColorTemplate.MATERIAL_COLORS[categoryData.indexOf(data) % ColorTemplate.MATERIAL_COLORS.size]
                }
            }
            
            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.2f
            valueLinePart2Length = 0.4f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        }

        val pieData = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${String.format("%.1f", value)}%"
                }
            })
            setValueTextSize(11f)
            setValueTextColor(Color.WHITE)
        }

        binding.pieChartCategories.data = pieData
        binding.pieChartCategories.animateY(1000)
        binding.pieChartCategories.invalidate()
    }

    /**
     * 更新月度图表
     */
    private fun updateMonthlyChart(monthlyData: List<MonthlyStatistic>) {
        if (monthlyData.isEmpty()) {
            binding.barChartMonthly.visibility = View.GONE
            android.util.Log.d("StatisticsFragment", "月度数据为空，隐藏图表")
            return
        }

        binding.barChartMonthly.visibility = View.VISIBLE
        android.util.Log.d("StatisticsFragment", "更新月度图表：${monthlyData.size} 个月的数据")

        val expenseEntries = ArrayList<BarEntry>()
        monthlyData.forEachIndexed { index: Int, data: MonthlyStatistic ->
            expenseEntries.add(BarEntry(index.toFloat(), data.expense.toFloat()))
        }

        val incomeEntries = ArrayList<BarEntry>()
        monthlyData.forEachIndexed { index: Int, data: MonthlyStatistic ->
            incomeEntries.add(BarEntry(index.toFloat(), data.income.toFloat()))
        }

        val expenseDataSet = BarDataSet(expenseEntries, "Expense").apply {
            color = Color.parseColor("#E91E63")
            valueTextColor = Color.WHITE
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0) {
                        String.format("¥%.0f", value)
                    } else {
                        ""
                    }
                }
            }
        }

        val incomeDataSet = BarDataSet(incomeEntries, "Income").apply {
            color = Color.parseColor("#4CAF50")
            valueTextColor = Color.WHITE
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0) {
                        String.format("¥%.0f", value)
                    } else {
                        ""
                    }
                }
            }
        }

        val monthLabels = ArrayList<String>()
        monthlyData.forEach { data: MonthlyStatistic ->
            monthLabels.add(data.period)
        }

        val barData = BarData(expenseDataSet, incomeDataSet).apply {
            val groupSpace = 0.1f
            val barSpace = 0.05f
            val barWidth = 0.4f
            
            setValueTextColor(Color.WHITE)
            setValueTextSize(10f)
            
            this.barWidth = barWidth
            groupBars(0f, groupSpace, barSpace)
        }

        binding.barChartMonthly.apply {
            data = barData
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            if (monthlyData.size > 1) {
                setVisibleXRangeMaximum(6f)
                moveViewToX(monthlyData.size.toFloat() - 3f)
            }

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelCount = monthLabels.size
                valueFormatter = IndexAxisValueFormatter(monthLabels.toTypedArray())
                textColor = Color.GRAY
                textSize = 10f
                labelRotationAngle = -45f
                setAvoidFirstLastClipping(true)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                textColor = Color.GRAY
                setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value >= 1000) {
                            String.format("%.1fk", value / 1000)
                        } else {
                            String.format("%.0f", value)
                        }
                    }
                }
            }

            axisRight.isEnabled = false
            legend.isEnabled = true
            legend.textColor = Color.GRAY

            invalidate()
        }
    }

    /**
     * 更新空状态显示
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 设置时间范围选择
     */
    private fun setupTimeRangeChips() {
        binding.chipGroupTimeRange.setOnCheckedStateChangeListener { _, checkedIds ->
            val timeRange = when (checkedIds.firstOrNull()) {
                R.id.chipThisWeek -> StatisticsViewModel.TimeRange.THIS_WEEK
                R.id.chipThisMonth -> StatisticsViewModel.TimeRange.THIS_MONTH
                R.id.chipThisYear -> StatisticsViewModel.TimeRange.THIS_YEAR
                else -> StatisticsViewModel.TimeRange.THIS_MONTH
            }
            viewModel.setTimeRange(timeRange)
        }
    }

    /**
     * 设置趋势图类型选择
     */
    private fun setupTrendTypeChips() {
        binding.chipGroupTrendType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentTrendType = when (checkedIds.first()) {
                    R.id.chipExpenseTrend -> TrendType.EXPENSE
                    R.id.chipIncomeTrend -> TrendType.INCOME
                    else -> TrendType.EXPENSE
                }
                
                // 立即更新趋势图显示
                updateTrendChartDisplay()
            }
        }
    }

    /**
     * 设置饼图类型选择
     */
    private fun setupPieChartTypeChips() {
        binding.chipGroupPieType.setOnCheckedStateChangeListener { _, checkedIds ->
            currentPieChartType = when (checkedIds.firstOrNull()) {
                R.id.chipExpenseCategory -> PieChartType.EXPENSE
                R.id.chipIncomeCategory -> PieChartType.INCOME
                else -> PieChartType.EXPENSE
            }
            updatePieChart()
        }
    }

    /**
     * 设置月度时间范围选择
     */
    private fun setupMonthlyRangeChips() {
        binding.chipGroupMonthlyRange.setOnCheckedStateChangeListener { _, checkedIds ->
            val monthlyTimeRange = when (checkedIds.firstOrNull()) {
                R.id.chipLast7Days -> StatisticsViewModel.MonthlyTimeRange.LAST_7_DAYS
                R.id.chipMonthlyThisYear -> StatisticsViewModel.MonthlyTimeRange.THIS_YEAR
                R.id.chipMonthlyLastYear -> StatisticsViewModel.MonthlyTimeRange.LAST_YEAR
                R.id.chipLast24Months -> StatisticsViewModel.MonthlyTimeRange.LAST_24_MONTHS
                else -> StatisticsViewModel.MonthlyTimeRange.LAST_7_DAYS
            }
            
            android.util.Log.d("StatisticsFragment", "切换月度时间范围：$monthlyTimeRange")
            viewModel.setMonthlyTimeRange(monthlyTimeRange)
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("StatisticsFragment", "onResume - 开始刷新统计数据")
        
        // 刷新统计数据
        viewModel.refresh()
        
        // 延迟执行调试信息，确保数据加载完成
        lifecycleScope.launch {
            kotlinx.coroutines.delay(1000) // 等待1秒让数据加载完成
            
            android.util.Log.d("StatisticsFragment", "输出详细调试信息")
            viewModel.debugInfo()
            
            // 强制刷新月度统计
            kotlinx.coroutines.delay(500)
            android.util.Log.d("StatisticsFragment", "强制刷新月度统计")
            viewModel.forceRefreshMonthlyData()
        }
    }

    /**
     * 更新财务健康度显示
     */
    private fun updateFinancialHealthDisplay(assessment: FinancialHealthAssessment?) {
        binding.apply {
            if (assessment != null) {
                tvHealthScore.text = "${assessment.overallScore} points"
                tvHealthLevel.text = assessment.level.displayName
                
                // 设置健康度颜色
                val color = Color.parseColor(assessment.level.color)
                tvHealthScore.setTextColor(color)
            } else {
                tvHealthScore.text = getString(R.string.score_placeholder)
                tvHealthLevel.text = getString(R.string.evaluating)
            }
        }
    }

    /**
     * 更新预算跟踪显示
     */
    private fun updateBudgetTrackingDisplay(budgetStatus: BudgetTrackingStatus?) {
        binding.apply {
            if (budgetStatus != null) {
                tvSafeBudgets.text = budgetStatus.safeBudgets.toString()
                tvWarningBudgets.text = budgetStatus.warningBudgets.toString()
                tvOverBudgets.text = budgetStatus.overBudgets.toString()
            } else {
                tvSafeBudgets.text = "0"
                tvWarningBudgets.text = "0"
                tvOverBudgets.text = "0"
            }
        }
    }

    /**
     * 更新支出模式显示
     */
    private fun updateExpensePatternDisplay(patternAnalysis: ExpensePatternAnalysis?) {
        binding.apply {
            if (patternAnalysis != null) {
                val topCategory = patternAnalysis.topCategories.firstOrNull()
                if (topCategory != null) {
                    tvTopSpendingCategory.text = getString(R.string.main_expense_category_format, topCategory.categoryName, topCategory.percentage)
                } else {
                    tvTopSpendingCategory.text = getString(R.string.main_expense_category_no_data)
                }
                
                val preference = patternAnalysis.weekdayVsWeekendSpending.preference
                val recentTrend = patternAnalysis.spendingTrends.lastOrNull()
                val trendText = if (recentTrend != null && recentTrend.changePercentage != 0.0) {
                    val direction = if (recentTrend.changePercentage > 0) getString(R.string.trend_up) else getString(R.string.trend_down)
                    "$direction ${String.format("%.1f", kotlin.math.abs(recentTrend.changePercentage))}%"
                } else {
                    getString(R.string.expense_trend_stable)
                }
                
                tvSpendingTrend.text = getString(R.string.expense_trend_format, trendText, preference)
            } else {
                tvTopSpendingCategory.text = getString(R.string.main_expense_category_calculating)
                tvSpendingTrend.text = getString(R.string.expense_trend_calculating)
            }
        }
    }

    /**
     * 显示财务健康度详情
     */
    private fun showFinancialHealthDetails() {
        val assessment = viewModel.financialHealthAssessment.value ?: return
        
        val message = buildString {
            appendLine(getString(R.string.financial_health_assessment_format, assessment.overallScore, assessment.level.displayName))
            appendLine()
            
            if (assessment.strengths.isNotEmpty()) {
                appendLine(getString(R.string.financial_strengths))
                assessment.strengths.forEach { appendLine("• $it") }
                appendLine()
            }
            
            if (assessment.concerns.isNotEmpty()) {
                appendLine(getString(R.string.areas_of_concern))
                assessment.concerns.forEach { appendLine("• $it") }
                appendLine()
            }
            
            if (assessment.recommendations.isNotEmpty()) {
                appendLine(getString(R.string.improvement_recommendations))
                assessment.recommendations.forEach { appendLine("• $it") }
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.financial_health_details_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }

    /**
     * 显示预算跟踪详情
     */
    private fun showBudgetTrackingDetails() {
        val budgetStatus = viewModel.budgetTrackingStatus.value ?: return
        
        val message = buildString {
            appendLine(getString(R.string.budget_overview_title))
            appendLine("• ${getString(R.string.total_budgets_count, budgetStatus.totalBudgets)}")
            appendLine("• ${getString(R.string.active_budgets_count, budgetStatus.activeBudgets)}")
            appendLine("• ${getString(R.string.total_budget_amount, budgetStatus.totalBudgetAmount)}")
            appendLine("• ${getString(R.string.total_spent_amount, budgetStatus.totalSpentAmount)}")
            appendLine("• ${getString(R.string.overall_progress, budgetStatus.overallProgress)}")
            appendLine()
            
            if (budgetStatus.budgetDetails.isNotEmpty()) {
                appendLine(getString(R.string.budget_details_header))
                budgetStatus.budgetDetails.take(5).forEach { budget ->
                    appendLine("• ${budget.budgetName}: ${String.format("%.1f", budget.spentPercentage)}% (${budget.status})")
                }
                
                if (budgetStatus.budgetDetails.size > 5) {
                    appendLine(getString(R.string.more_budgets_remaining, budgetStatus.budgetDetails.size - 5))
                }
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.budget_tracking_details_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.manage_budgets)) { _, _ ->
                try {
                    // 导航到预算管理页面
                    findNavController().navigate(R.id.budgetFragment)
                } catch (e: Exception) {
                    Snackbar.make(binding.root, getString(R.string.navigation_to_budget_failed), Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.close), null)
            .show()
    }

    /**
     * 显示支出模式详情
     */
    private fun showExpensePatternDetails() {
        val patternAnalysis = viewModel.expensePatternAnalysis.value ?: return
        
        val message = buildString {
            appendLine(getString(R.string.expense_pattern_analysis_header))
            appendLine()
            
            // 前3大支出类别
            appendLine(getString(R.string.main_spending_categories))
            patternAnalysis.topCategories.take(3).forEach { category ->
                val trendText = when (category.trend) {
                    SpendingTrendType.INCREASING -> getString(R.string.trend_increasing)
                    SpendingTrendType.DECREASING -> getString(R.string.trend_decreasing) 
                    SpendingTrendType.STABLE -> getString(R.string.trend_stable_icon)
                    SpendingTrendType.VOLATILE -> getString(R.string.trend_volatile)
                }
                appendLine("• ${category.categoryName}: ${String.format("%.1f", category.percentage)}% $trendText")
            }
            appendLine()
            
            // 消费习惯
            val weekdayWeekend = patternAnalysis.weekdayVsWeekendSpending
            appendLine(getString(R.string.spending_habits))
            appendLine("• ${getString(R.string.weekday_daily_avg, weekdayWeekend.weekdayAvgDaily)}")
            appendLine("• ${getString(R.string.weekend_daily_avg, weekdayWeekend.weekendAvgDaily)}")
            val preferenceText = when (weekdayWeekend.preference) {
                "weekday" -> getString(R.string.weekday_spending)
                "weekend" -> getString(R.string.weekend_spending)
                else -> weekdayWeekend.preference
            }
            appendLine("• ${getString(R.string.preference_format, preferenceText)}")
            appendLine()
            
            // 异常交易
            if (patternAnalysis.unusualTransactions.isNotEmpty()) {
                appendLine(getString(R.string.unusual_transactions_detected_header))
                patternAnalysis.unusualTransactions.take(3).forEach { unusual ->
                    val date = java.text.SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(unusual.date))
                    appendLine("• $date ${unusual.categoryName}: ¥${String.format("%.2f", unusual.amount)} (${unusual.reason})")
                }
                if (patternAnalysis.unusualTransactions.size > 3) {
                    appendLine(getString(R.string.more_unusual_transactions, patternAnalysis.unusualTransactions.size - 3))
                }
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.expense_pattern_details_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }

    /**
     * 导航到AI分析页面
     */
    private fun navigateToAIAnalysis() {
        try {
            findNavController().navigate(R.id.action_statisticsFragment_to_aiAnalysisFragment)
        } catch (e: Exception) {
            android.util.Log.e("StatisticsFragment", "Navigation to AI analysis failed", e)
            Snackbar.make(binding.root, getString(R.string.ai_analysis_temporarily_unavailable), Snackbar.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance() = StatisticsFragment()
    }
} 