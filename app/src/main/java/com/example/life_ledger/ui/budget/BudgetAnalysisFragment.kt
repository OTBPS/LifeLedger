package com.example.life_ledger.ui.budget

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.life_ledger.R
import com.example.life_ledger.databinding.FragmentBudgetAnalysisBinding
import com.example.life_ledger.ui.budget.adapter.BudgetRecommendationAdapter
import com.example.life_ledger.ui.budget.viewmodel.BudgetAnalysisViewModel
import com.example.life_ledger.ui.budget.viewmodel.*
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.fragment.findNavController
import androidx.appcompat.app.AlertDialog

/**
 * 预算分析页面
 * 显示预算使用情况、趋势分析和智能建议
 */
class BudgetAnalysisFragment : Fragment() {
    
    private var _binding: FragmentBudgetAnalysisBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BudgetAnalysisViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }
    
    private lateinit var recommendationAdapter: BudgetRecommendationAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupViews() {
        // 设置建议列表 - 简化，不需要点击处理
        recommendationAdapter = BudgetRecommendationAdapter()
        
        binding.recyclerViewRecommendations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recommendationAdapter
        }
        
        // 设置图表切换
        setupChartTabs()
    }
    
    private fun setupObservers() {
        // 观察分析概览数据
        viewModel.analysisOverview.observe(viewLifecycleOwner) { overview ->
            overview?.let { updateOverviewUI(it) }
        }
        
        // 观察预算使用数据
        viewModel.budgetUsageData.observe(viewLifecycleOwner) { usageDataList ->
            usageDataList?.let { 
                if (it.isNotEmpty()) {
                    updateUsageChart(it.first()) // 使用第一个预算的数据作为示例
                }
            }
        }
        
        // 观察趋势数据
        viewModel.budgetTrendData.observe(viewLifecycleOwner) { trendDataList ->
            trendDataList?.let { updateTrendChart(it) }
        }
        
        // 观察对比数据
        viewModel.budgetComparisonData.observe(viewLifecycleOwner) { comparisonDataList ->
            comparisonDataList?.let { updateComparisonChart(it) }
        }
        
        // 观察建议数据
        viewModel.recommendations.observe(viewLifecycleOwner) { recommendations ->
            recommendationAdapter.submitList(recommendations)
            updateRecommendationsUI(recommendations)
        }
        
        // 观察加载状态
        viewModel.isLoadingRecommendations.observe(viewLifecycleOwner) { isLoading ->
            updateLoadingState(isLoading)
        }
        
        // 观察错误信息
        viewModel.recommendationError.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
            }
        }
    }
    
    private fun setupClickListeners() {
        // 返回按钮
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        // 刷新按钮
        binding.fabRefresh.setOnClickListener {
            refreshAnalysisData()
        }
        
        // 智能建议按钮
        binding.btnSmartRecommendations?.setOnClickListener {
            viewModel.generateSmartRecommendations()
        }
        
        // 时间范围选择
        binding.chipWeek.setOnClickListener { 
            // 设置周视图
        }
        binding.chipMonth.setOnClickListener { 
            // 设置月视图
        }
        binding.chipQuarter.setOnClickListener { 
            // 设置季度视图
        }
        binding.chipYear.setOnClickListener { 
            // 设置年视图
        }
    }
    
    private fun updateRecommendationsUI(recommendations: List<BudgetRecommendation>) {
        if (recommendations.isEmpty()) {
            binding.layoutEmptyRecommendations.visibility = View.VISIBLE
            binding.recyclerViewRecommendations.visibility = View.GONE
            binding.tvNoRecommendations.text = "暂无建议，点击智能建议获取AI分析"
        } else {
            binding.layoutEmptyRecommendations.visibility = View.GONE
            binding.recyclerViewRecommendations.visibility = View.VISIBLE
        }
    }
    
    private fun updateLoadingState(isLoading: Boolean) {
        binding.btnSmartRecommendations.apply {
            isEnabled = !isLoading
            text = if (isLoading) "分析中..." else "智能建议"
        }
        
        binding.progressRecommendations.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
    
    private fun setupChartTabs() {
        binding.tabLayoutCharts.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showChart(ChartType.PIE)
                    1 -> showChart(ChartType.LINE)
                    2 -> showChart(ChartType.BAR)
                }
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }
    
    private fun showChart(type: ChartType) {
        binding.apply {
            // 隐藏所有图表
            pieChart.visibility = View.GONE
            lineChart.visibility = View.GONE
            barChart.visibility = View.GONE
            layoutEmptyChart.visibility = View.GONE
            
            // 显示选中的图表
            when (type) {
                ChartType.PIE -> {
                    pieChart.visibility = View.VISIBLE
                    // 如果有数据则更新饼图
                    viewModel.budgetUsageData.value?.let { usageDataList ->
                        if (usageDataList.isNotEmpty()) {
                            updateUsageChart(usageDataList.first())
                        } else {
                            layoutEmptyChart.visibility = View.VISIBLE
                        }
                    }
                }
                ChartType.LINE -> {
                    lineChart.visibility = View.VISIBLE
                    // 如果有数据则更新趋势图
                    viewModel.budgetTrendData.value?.let { trendDataList ->
                        if (trendDataList.isNotEmpty()) {
                            updateTrendChart(trendDataList)
                        } else {
                            layoutEmptyChart.visibility = View.VISIBLE
                        }
                    }
                }
                ChartType.BAR -> {
                    barChart.visibility = View.VISIBLE
                    // 如果有数据则更新对比图
                    viewModel.budgetComparisonData.value?.let { comparisonDataList ->
                        if (comparisonDataList.isNotEmpty()) {
                            updateComparisonChart(comparisonDataList)
                        } else {
                            layoutEmptyChart.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }
    
    private fun refreshAnalysisData() {
        // 重新加载所有数据
        viewModel.loadAnalysisData()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    enum class ChartType {
        PIE, LINE, BAR
    }
    
    private fun updateOverviewUI(overview: AnalysisOverview) {
        with(binding) {
            // 健康评分
            tvHealthScore.text = overview.healthScore.toString()
            progressHealth.progress = overview.healthScore
            
            // 设置健康评分颜色
            val scoreColor = when {
                overview.healthScore >= 80 -> R.color.success
                overview.healthScore >= 60 -> R.color.warning
                else -> R.color.error
            }
            tvHealthScore.setTextColor(requireContext().getColor(scoreColor))
            
            // 统计信息
            tvTotalBudgets.text = overview.totalBudgets.toString()
            tvOverspentBudgets.text = "0"  // TODO: 需要从 overview 中添加此字段
            tvAverageUsage.text = "${String.format("%.1f", overview.averageUsage)}%"
        }
    }
    
    private fun updateUsageChart(usageData: BudgetUsageData) {
        val entries = mutableListOf<PieEntry>()
        
        val usedAmount = usageData.spent
        val remainingAmount = usageData.amount - usageData.spent
        
        if (usedAmount > 0) {
            entries.add(PieEntry(usedAmount.toFloat(), "已使用"))
        }
        if (remainingAmount > 0) {
            entries.add(PieEntry(remainingAmount.toFloat(), "剩余"))
        }
        
        if (entries.isEmpty()) {
            binding.pieChart.clear()
            return
        }
        
        val dataSet = PieDataSet(entries, "预算使用情况")
        dataSet.colors = listOf(
            Color.parseColor("#FF6B6B"), // 已使用 - 红色
            Color.parseColor("#4ECDC4")  // 剩余 - 绿色
        )
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE
        
        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(binding.pieChart))
        
        binding.pieChart.data = data
        binding.pieChart.setCenterText("预算使用率\n${String.format("%.1f", usageData.percentage)}%")
        binding.pieChart.invalidate()
    }
    
    private fun updateTrendChart(trendDataList: List<BudgetTrendData>) {
        val budgetEntries = mutableListOf<Entry>()
        val spentEntries = mutableListOf<Entry>()
        
        trendDataList.forEachIndexed { index, point ->
            budgetEntries.add(Entry(index.toFloat(), point.budgetAmount.toFloat()))
            spentEntries.add(Entry(index.toFloat(), point.totalSpent.toFloat()))
        }
        
        val budgetDataSet = LineDataSet(budgetEntries, "预算金额")
        budgetDataSet.color = Color.parseColor("#4ECDC4")
        budgetDataSet.setCircleColor(Color.parseColor("#4ECDC4"))
        budgetDataSet.lineWidth = 2f
        budgetDataSet.circleRadius = 4f
        budgetDataSet.setDrawCircleHole(false)
        budgetDataSet.valueTextSize = 10f
        
        val spentDataSet = LineDataSet(spentEntries, "实际支出")
        spentDataSet.color = Color.parseColor("#FF6B6B")
        spentDataSet.setCircleColor(Color.parseColor("#FF6B6B"))
        spentDataSet.lineWidth = 2f
        spentDataSet.circleRadius = 4f
        spentDataSet.setDrawCircleHole(false)
        spentDataSet.valueTextSize = 10f
        
        val data = LineData(budgetDataSet, spentDataSet)
        binding.lineChart.data = data
        binding.lineChart.invalidate()
    }
    
    private fun updateComparisonChart(comparisonDataList: List<BudgetComparisonData>) {
        val entries = mutableListOf<BarEntry>()
        
        comparisonDataList.forEachIndexed { index, data ->
            val changePercentage = data.changePercentage.toFloat()
            entries.add(BarEntry(index.toFloat(), changePercentage))
        }
        
        val dataSet = BarDataSet(entries, "支出变化率")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 12f
        
        val data = BarData(dataSet)
        data.barWidth = 0.9f
        
        binding.barChart.data = data
        binding.barChart.invalidate()
    }
} 