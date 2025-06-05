package com.example.life_ledger.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.life_ledger.R
import com.example.life_ledger.databinding.FragmentBudgetSettingsBinding
import com.example.life_ledger.ui.budget.viewmodel.BudgetViewModel
import com.example.life_ledger.utils.PreferenceUtils
import com.google.android.material.snackbar.Snackbar

/**
 * 预算设置页面
 * 提供预算相关的配置选项
 */
class BudgetSettingsFragment : Fragment() {
    
    private var _binding: FragmentBudgetSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BudgetViewModel by activityViewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadSettings()
        setupClickListeners()
    }
    
    private fun setupUI() {
        // 设置工具栏
        binding.toolbar.title = "预算设置"
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun loadSettings() {
        // 加载默认警告阈值
        binding.tvDefaultThreshold.text = PreferenceUtils.getString(
            "budget_warning_threshold", "80%"
        )
        
        binding.sliderDefaultThreshold.value = PreferenceUtils.getFloat(
            "budget_warning_threshold_value", 80f
        )
        
        // 加载开关状态
        binding.switchAutoReminder.isChecked = PreferenceUtils.getBoolean(
            "budget_auto_reminder", true
        )
        
        binding.switchOverspendReminder.isChecked = PreferenceUtils.getBoolean(
            "budget_overspending_alert", true
        )
        
        binding.switchExpiryReminder.isChecked = PreferenceUtils.getBoolean(
            "budget_expiration_reminder", true
        )
        
        binding.switchAutoRenewal.isChecked = PreferenceUtils.getBoolean(
            "budget_auto_renewal", false
        )
    }
    
    private fun setupClickListeners() {
        // 默认警告阈值滑动条
        binding.sliderDefaultThreshold.addOnChangeListener { _, value, _ ->
            binding.tvDefaultThreshold.text = "${value.toInt()}%"
            saveFloatSetting("budget_warning_threshold_value", value)
        }
        
        // 自动提醒开关
        binding.switchAutoReminder.setOnCheckedChangeListener { _, isChecked ->
            saveBooleanSetting("budget_auto_reminder", isChecked)
        }
        
        // 超支提醒开关
        binding.switchOverspendReminder.setOnCheckedChangeListener { _, isChecked ->
            saveBooleanSetting("budget_overspending_alert", isChecked)
        }
        
        // 即将到期提醒开关
        binding.switchExpiryReminder.setOnCheckedChangeListener { _, isChecked ->
            saveBooleanSetting("budget_expiration_reminder", isChecked)
        }
        
        // 自动续期开关
        binding.switchAutoRenewal.setOnCheckedChangeListener { _, isChecked ->
            saveBooleanSetting("budget_auto_renewal", isChecked)
        }
        
        // 重置所有预算
        binding.btnResetAllBudgets.setOnClickListener {
            showResetAllBudgetsConfirmation()
        }
        
        // 导出预算数据
        binding.btnExportBudgets.setOnClickListener {
            showMessage("Export function development in progress")
        }
        
        // 导入预算数据
        binding.btnImportBudgets.setOnClickListener {
            showMessage("Importing functionality in development")
        }
        
        // 清理过期预算
        binding.btnCleanExpiredBudgets.setOnClickListener {
            showCleanExpiredBudgetsConfirmation()
        }
    }
    
    private fun saveFloatSetting(key: String, value: Float) {
        PreferenceUtils.putFloat(key, value)
    }
    
    private fun saveBooleanSetting(key: String, value: Boolean) {
        PreferenceUtils.putBoolean(key, value)
    }
    
    private fun showResetAllBudgetsConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Reset all budgets")
            .setMessage("Are you sure you want to reset the spending amounts for all budgets?")
            .setPositiveButton("Reset") { _, _ ->
                resetAllBudgets()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showCleanExpiredBudgetsConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Clean up expired budgets")
            .setMessage("Are you sure you want to delete all expired budgets?。")
            .setPositiveButton("Delete") { _, _ ->
                cleanExpiredBudgets()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun resetAllBudgets() {
        // TODO: 实现重置所有预算功能
        showMessage("Reset function is under development")
    }
    
    private fun cleanExpiredBudgets() {
        // TODO: 实现清理过期预算功能
        showMessage("Cleaning function development in progress")
    }
    
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 