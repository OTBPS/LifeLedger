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
 * Budget Analysis Page
 * Displays budget usage, trend analysis and smart recommendations
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
        // Setup recommendation list - simplified, no click handling needed
        recommendationAdapter = BudgetRecommendationAdapter()
        
        binding.recyclerViewRecommendations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recommendationAdapter
        }
        
        // Setup chart switching
        setupChartTabs()
    }
    
    private fun setupObservers() {
        // Observe analysis overview data
        viewModel.analysisOverview.observe(viewLifecycleOwner) { overview ->
            overview?.let { updateOverviewUI(it) }
        }
        
        // Observe budget usage data
        viewModel.budgetUsageData.observe(viewLifecycleOwner) { usageDataList ->
            usageDataList?.let { 
                if (it.isNotEmpty()) {
                    updateUsageChart(it.first()) // Use first budget data as example
                }
            }
        }
        
        // Observe trend data
        viewModel.budgetTrendData.observe(viewLifecycleOwner) { trendDataList ->
            trendDataList?.let { updateTrendChart(it) }
        }
        
        // Observe comparison data
        viewModel.budgetComparisonData.observe(viewLifecycleOwner) { comparisonDataList ->
            comparisonDataList?.let { updateComparisonChart(it) }
        }
        
        // Observe recommendation data
        viewModel.recommendations.observe(viewLifecycleOwner) { recommendations ->
            recommendationAdapter.submitList(recommendations)
            updateRecommendationsUI(recommendations)
        }
        
        // Observe loading state
        viewModel.isLoadingRecommendations.observe(viewLifecycleOwner) { isLoading ->
            updateLoadingState(isLoading)
        }
        
        // Observe error messages
        viewModel.recommendationError.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
            }
        }
    }
    
    private fun setupClickListeners() {
        // Back button
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        // Refresh button
        binding.fabRefresh.setOnClickListener {
            refreshAnalysisData()
        }
        
        // Smart recommendations button
        binding.btnSmartRecommendations?.setOnClickListener {
            viewModel.generateSmartRecommendations()
        }
        
        // Time range selection
        binding.chipWeek.setOnClickListener { 
            // Set week view
        }
        binding.chipMonth.setOnClickListener { 
            // Set month view
        }
        binding.chipQuarter.setOnClickListener { 
            // Set quarter view
        }
        binding.chipYear.setOnClickListener { 
            // Set year view
        }
    }
    
    private fun updateRecommendationsUI(recommendations: List<BudgetRecommendation>) {
        if (recommendations.isEmpty()) {
            binding.layoutEmptyRecommendations.visibility = View.VISIBLE
            binding.recyclerViewRecommendations.visibility = View.GONE
            binding.tvNoRecommendations.text = getString(R.string.no_recommendations_message)
        } else {
            binding.layoutEmptyRecommendations.visibility = View.GONE
            binding.recyclerViewRecommendations.visibility = View.VISIBLE
        }
    }
    
    private fun updateLoadingState(isLoading: Boolean) {
        binding.btnSmartRecommendations.apply {
            isEnabled = !isLoading
            text = if (isLoading) getString(R.string.analyzing) else getString(R.string.smart_recommendations)
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
            // Hide all charts
            pieChart.visibility = View.GONE
            lineChart.visibility = View.GONE
            barChart.visibility = View.GONE
            layoutEmptyChart.visibility = View.GONE
            
            // Show selected chart
            when (type) {
                ChartType.PIE -> {
                    pieChart.visibility = View.VISIBLE
                    // Update pie chart if data is available
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
                    // Update trend chart if data is available
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
                    // Update comparison chart if data is available
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
        // Reload all data
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
            // Health score
            tvHealthScore.text = overview.healthScore.toString()
            progressHealth.progress = overview.healthScore
            
            // Set health score color
            val scoreColor = when {
                overview.healthScore >= 80 -> R.color.success
                overview.healthScore >= 60 -> R.color.warning
                else -> R.color.error
            }
            tvHealthScore.setTextColor(requireContext().getColor(scoreColor))
            
            // Statistics information
            tvTotalBudgets.text = overview.totalBudgets.toString()
            tvOverspentBudgets.text = "0"  // TODO: Need to add this field from overview
            tvAverageUsage.text = "${String.format("%.1f", overview.averageUsage)}%"
        }
    }
    
    private fun updateUsageChart(usageData: BudgetUsageData) {
        val entries = mutableListOf<PieEntry>()
        
        val usedAmount = usageData.spent
        val remainingAmount = usageData.amount - usageData.spent
        
        if (usedAmount > 0) {
            entries.add(PieEntry(usedAmount.toFloat(), getString(R.string.used)))
        }
        if (remainingAmount > 0) {
            entries.add(PieEntry(remainingAmount.toFloat(), getString(R.string.remaining)))
        }
        
        if (entries.isEmpty()) {
            binding.pieChart.clear()
            return
        }
        
        val dataSet = PieDataSet(entries, getString(R.string.budget_usage_status))
        dataSet.colors = listOf(
            Color.parseColor("#FF6B6B"), // 已使用 - 红色
            Color.parseColor("#4ECDC4")  // 剩余 - 绿色
        )
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE
        
        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(binding.pieChart))
        
        binding.pieChart.data = data
        binding.pieChart.setCenterText("${getString(R.string.budget_usage_rate)}\n${String.format("%.1f", usageData.percentage)}%")
        binding.pieChart.invalidate()
    }
    
    private fun updateTrendChart(trendDataList: List<BudgetTrendData>) {
        val budgetEntries = mutableListOf<Entry>()
        val spentEntries = mutableListOf<Entry>()
        
        trendDataList.forEachIndexed { index, point ->
            budgetEntries.add(Entry(index.toFloat(), point.budgetAmount.toFloat()))
            spentEntries.add(Entry(index.toFloat(), point.totalSpent.toFloat()))
        }
        
        val budgetDataSet = LineDataSet(budgetEntries, getString(R.string.budget_amount))
        budgetDataSet.color = Color.parseColor("#4ECDC4")
        budgetDataSet.setCircleColor(Color.parseColor("#4ECDC4"))
        budgetDataSet.lineWidth = 2f
        budgetDataSet.circleRadius = 4f
        budgetDataSet.setDrawCircleHole(false)
        budgetDataSet.valueTextSize = 10f
        
        val spentDataSet = LineDataSet(spentEntries, getString(R.string.actual_spending))
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
        
        val dataSet = BarDataSet(entries, getString(R.string.spending_change_rate))
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 12f
        
        val data = BarData(dataSet)
        data.barWidth = 0.9f
        
        binding.barChart.data = data
        binding.barChart.invalidate()
    }
} 