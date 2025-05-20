package com.example.life_ledger.ui.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.life_ledger.R
import com.example.life_ledger.data.database.AppDatabase
import com.example.life_ledger.data.repository.LifeLedgerRepository
import com.example.life_ledger.databinding.FragmentAiAnalysisBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.*

/**
 * AI分析页面
 * 展示智能支出分析、月度报告和个性化建议
 */
class AIAnalysisFragment : Fragment() {

    private var _binding: FragmentAiAnalysisBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AIAnalysisViewModel
    private lateinit var adviceAdapter: ConsumptionAdviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_ai_analysis,
            container,
            false
        )
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            setupViewModel()
            setupRecyclerView()
            setupClickListeners()
            setupObservers()
            android.util.Log.d("AIAnalysisFragment", "Fragment setup completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("AIAnalysisFragment", "Error during fragment setup", e)
            Snackbar.make(view, "初始化失败：${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setupViewModel() {
        try {
            val database = AppDatabase.getDatabase(requireContext())
            val repository = LifeLedgerRepository.getInstance(database)
            
            viewModel = ViewModelProvider(
                this,
                AIAnalysisViewModelFactory(repository)
            )[AIAnalysisViewModel::class.java]
            
            android.util.Log.d("AIAnalysisFragment", "ViewModel setup completed")
        } catch (e: Exception) {
            android.util.Log.e("AIAnalysisFragment", "Error setting up ViewModel", e)
            throw e
        }
    }

    private fun setupRecyclerView() {
        adviceAdapter = ConsumptionAdviceAdapter()
        binding.recyclerViewAdvice.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adviceAdapter
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            // 支出分析按钮
            buttonAnalyzeExpenses.setOnClickListener {
                viewModel.analyzeRecentExpenses()
            }
            
            // 月度报告按钮
            buttonMonthlyReport.setOnClickListener {
                viewModel.generateMonthlyReport()
            }
            
            // 个性化建议按钮
            buttonPersonalizedAdvice.setOnClickListener {
                viewModel.getPersonalizedAdvice()
            }
            
            // 一键分析按钮
            buttonFullAnalysis.setOnClickListener {
                viewModel.performFullAnalysis()
            }
            
            // 刷新按钮
            swipeRefreshLayout.setOnRefreshListener {
                viewModel.performFullAnalysis()
            }
            
            // 用户配置按钮
            buttonUserProfile.setOnClickListener {
                showUserProfileDialog()
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 观察加载状态
            viewModel.isLoading.collect { isLoading ->
                binding.swipeRefreshLayout.isRefreshing = isLoading
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // 观察错误信息
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // 观察支出分析结果
            viewModel.expenseAnalysis.collect { analysis ->
                analysis?.let {
                    displayExpenseAnalysis(it)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // 观察月度报告
            viewModel.monthlyReport.collect { report ->
                report?.let {
                    displayMonthlyReport(it)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // 观察个性化建议
            viewModel.consumptionAdvice.collect { advice ->
                adviceAdapter.submitList(advice)
                binding.layoutAdvice.visibility = if (advice.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun displayExpenseAnalysis(analysis: com.example.life_ledger.data.service.ExpenseAnalysis) {
        binding.apply {
            layoutExpenseAnalysis.visibility = View.VISIBLE
            
            textExpenseAnalysisSummary.text = analysis.summary
            textStructureAnalysis.text = analysis.structureAnalysis
            textHabitAssessment.text = analysis.habitAssessment
            textProblemIdentification.text = analysis.problemIdentification
            textOptimizationSuggestions.text = analysis.optimizationSuggestions
            
            // 显示分析时间
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(Date(analysis.generatedAt))
            textAnalysisTime.text = "分析时间：$date"
        }
    }

    private fun displayMonthlyReport(report: com.example.life_ledger.data.service.MonthlyReport) {
        binding.apply {
            layoutMonthlyReport.visibility = View.VISIBLE
            
            textReportTitle.text = "${report.year}年${report.month}月财务报告"
            textReportContent.text = report.content
            textReportSummary.text = report.summary
            
            // 显示报告生成时间
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(Date(report.generatedAt))
            textReportTime.text = "生成时间：$date"
        }
    }

    private fun showUserProfileDialog() {
        val currentProfile = viewModel.userProfile.value
        val dialog = UserProfileDialog.newInstance(currentProfile) { newProfile ->
            viewModel.updateUserProfile(newProfile)
        }
        dialog.show(childFragmentManager, "UserProfileDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AIAnalysisFragment()
    }
}

/**
 * AI分析ViewModel工厂
 */
class AIAnalysisViewModelFactory(
    private val repository: LifeLedgerRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AIAnalysisViewModel::class.java)) {
            return AIAnalysisViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 