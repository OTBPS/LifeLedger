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
                currentTrendType = when (checkedIds.firstOrNull()) {
                    R.id.chipExpenseTrend -> TrendType.EXPENSE
                    R.id.chipIncomeTrend -> TrendType.INCOME
                    else -> TrendType.EXPENSE
                }
                updateTrendChart()
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
                    updateTrendChart(trendData, "支出")
                }
            }
        }

        // 观察收入趋势数据
        lifecycleScope.launch {
            viewModel.incomeTrendData.collect { trendData ->
                if (currentTrendType == TrendType.INCOME) {
                    updateTrendChart(trendData, "收入")
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

        val color = if (type == "收入") Color.GREEN else Color.BLUE
        val dataSet = LineDataSet(entries, "日${type}金额").apply {
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
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
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
                        updateTrendChart(trendData, "支出")
                    }
                }
            }
            TrendType.INCOME -> {
                lifecycleScope.launch {
                    viewModel.incomeTrendData.value.let { trendData ->
                        updateTrendChart(trendData, "收入")
                    }
                }
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

        binding.pieChartCategories.apply {
            data = pieData
            centerText = when (currentPieChartType) {
                PieChartType.EXPENSE -> "支出分类"
                PieChartType.INCOME -> "收入分类"
            }
            animateY(1000, Easing.EaseInOutQuad)
            highlightValues(null)
            invalidate()
        }
    }

    /**
     * 更新月度图表
     */
    private fun updateMonthlyChart(monthlyData: List<MonthlyData>) {
        if (monthlyData.isEmpty()) {
            android.util.Log.d("StatisticsFragment", "月度数据为空，清空图表")
            binding.barChartMonthly.clear()
            binding.barChartMonthly.invalidate()
            return
        }

        android.util.Log.d("StatisticsFragment", "更新月度图表：${monthlyData.size} 个月的数据")

        val expenseEntries = monthlyData.mapIndexed { index, data ->
            BarEntry(index.toFloat(), data.expense.toFloat())
        }
        
        val incomeEntries = monthlyData.mapIndexed { index, data ->
            BarEntry(index.toFloat(), data.income.toFloat())
        }

        val expenseDataSet = BarDataSet(expenseEntries, "支出").apply {
            color = Color.parseColor("#F44336") // 红色
            valueTextSize = 9f
            valueTextColor = Color.BLACK
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0) "¥${value.toInt()}" else ""
                }
            }
        }

        val incomeDataSet = BarDataSet(incomeEntries, "收入").apply {
            color = Color.parseColor("#4CAF50") // 绿色
            valueTextSize = 9f
            valueTextColor = Color.BLACK
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0) "¥${value.toInt()}" else ""
                }
            }
        }

        val barData = BarData(expenseDataSet, incomeDataSet).apply {
            barWidth = 0.35f
        }

        binding.barChartMonthly.apply {
            data = barData
            
            // 设置X轴标签
            val monthLabels = monthlyData.map { data ->
                try {
                    val parts = data.month.split("-")
                    if (parts.size >= 2) {
                        "${parts[1]}月"
                    } else {
                        data.month
                    }
                } catch (e: Exception) {
                    data.month
                }
            }
            
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(monthLabels)
                granularity = 1f
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
            }
            
            // 分组显示
            if (monthlyData.size > 1) {
                groupBars(0f, 0.3f, 0.05f)
            }
            
            // 设置图表边距
            setFitBars(true)
            
            animateY(1000, Easing.EaseInOutQuart)
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
            currentTrendType = when (checkedIds.firstOrNull()) {
                R.id.chipExpenseTrend -> TrendType.EXPENSE
                R.id.chipIncomeTrend -> TrendType.INCOME
                else -> TrendType.EXPENSE
            }
            updateTrendChart()
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
                R.id.chipLast12Months -> StatisticsViewModel.MonthlyTimeRange.LAST_12_MONTHS
                R.id.chipMonthlyThisYear -> StatisticsViewModel.MonthlyTimeRange.THIS_YEAR
                R.id.chipMonthlyLastYear -> StatisticsViewModel.MonthlyTimeRange.LAST_YEAR
                R.id.chipLast24Months -> StatisticsViewModel.MonthlyTimeRange.LAST_24_MONTHS
                else -> StatisticsViewModel.MonthlyTimeRange.LAST_12_MONTHS
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
            viewModel.getDebugInfo()
            
            // 强制刷新月度统计
            kotlinx.coroutines.delay(500)
            android.util.Log.d("StatisticsFragment", "强制刷新月度统计")
            viewModel.forceRefreshMonthlyData()
        }
    }

    companion object {
        fun newInstance() = StatisticsFragment()
    }
} 