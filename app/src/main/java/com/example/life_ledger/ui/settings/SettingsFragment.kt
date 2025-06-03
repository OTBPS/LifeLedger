package com.example.life_ledger.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.life_ledger.R
import com.example.life_ledger.constants.AppConstants
import com.example.life_ledger.databinding.FragmentSettingsBinding
import com.example.life_ledger.ui.theme.ThemeManager
import com.example.life_ledger.ui.theme.ThemeSelectionAdapter
import com.example.life_ledger.utils.PreferenceUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import kotlinx.coroutines.launch

/**
 * 设置页面
 * 提供主题设置、通知偏好、数据管理等功能
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_settings,
            container,
            false
        )
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
    }

    /**
     * 设置UI组件
     */
    private fun setupUI() {
        // 初始化主题模式开关状态
        val currentThemeMode = ThemeManager.getCurrentThemeMode()
        binding.switchDarkMode.isChecked = when (currentThemeMode) {
            AppConstants.Theme.MODE_DARK -> true
            AppConstants.Theme.MODE_LIGHT -> false
            else -> ThemeManager.isDarkMode(requireContext())
        }
        
        // 初始化其他设置项状态
        binding.switchNotifications.isChecked = PreferenceUtils.isNotificationEnabled()
        binding.switchFinanceReminder.isChecked = PreferenceUtils.getBoolean("finance_reminder", true)
    }

    /**
     * 设置点击监听器
     */
    private fun setupClickListeners() {
        binding.apply {
            // 深色模式开关
            switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
                toggleDarkMode(isChecked)
            }

            // 通知设置
            switchNotifications.setOnCheckedChangeListener { _, isChecked ->
                toggleNotifications(isChecked)
            }

            // 财务提醒
            switchFinanceReminder.setOnCheckedChangeListener { _, isChecked ->
                toggleFinanceReminder(isChecked)
            }

            // 主题选择
            layoutTheme.setOnClickListener { showThemeDialog() }
            
            // 数据管理
            layoutBackup.setOnClickListener { performBackup() }
            layoutRestore.setOnClickListener { performRestore() }
            layoutExport.setOnClickListener { exportData() }
            
            // 其他
            layoutAbout.setOnClickListener { showAboutDialog() }
            layoutPrivacy.setOnClickListener { showPrivacyPolicy() }
        }
    }

    /**
     * 切换深色模式
     */
    private fun toggleDarkMode(enabled: Boolean) {
        val newMode = if (enabled) {
            AppConstants.Theme.MODE_DARK
        } else {
            AppConstants.Theme.MODE_LIGHT
        }
        
        ThemeManager.setThemeMode(newMode)
        showThemeAppliedMessage()
    }

    /**
     * 切换通知
     */
    private fun toggleNotifications(enabled: Boolean) {
        PreferenceUtils.setNotificationEnabled(enabled)
        Toast.makeText(
            requireContext(),
            if (enabled) "通知已开启" else "通知已关闭",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 切换财务提醒
     */
    private fun toggleFinanceReminder(enabled: Boolean) {
        PreferenceUtils.putBoolean("finance_reminder", enabled)
        Toast.makeText(
            requireContext(),
            if (enabled) "财务提醒已开启" else "财务提醒已关闭",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 显示主题选择对话框
     */
    private fun showThemeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_theme_selection, null)
        
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewThemes)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnApply = dialogView.findViewById<MaterialButton>(R.id.btnApply)
        
        // 设置RecyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        
        val currentTheme = ThemeManager.getCurrentCustomTheme()
        var selectedTheme = currentTheme
        
        val adapter = ThemeSelectionAdapter(
            requireContext(),
            AppConstants.Theme.AVAILABLE_THEMES,
            selectedTheme
        ) { theme ->
            selectedTheme = theme
        }
        recyclerView.adapter = adapter
        
        // 创建对话框
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
        
        // 设置按钮点击事件
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnApply.setOnClickListener {
            if (selectedTheme != currentTheme) {
                ThemeManager.setCustomTheme(selectedTheme)
                showThemeAppliedMessage()
                
                // 重新创建Activity以应用新主题
                requireActivity().recreate()
            }
            dialog.dismiss()
        }
        
        dialog.show()
    }

    /**
     * 显示主题模式选择对话框
     */
    private fun showThemeModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_theme_mode_selection, null)
        
        val radioSystem = dialogView.findViewById<MaterialRadioButton>(R.id.radioSystem)
        val radioLight = dialogView.findViewById<MaterialRadioButton>(R.id.radioLight)
        val radioDark = dialogView.findViewById<MaterialRadioButton>(R.id.radioDark)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnApply = dialogView.findViewById<MaterialButton>(R.id.btnApply)
        
        // 设置当前选中状态
        val currentMode = ThemeManager.getCurrentThemeMode()
        when (currentMode) {
            AppConstants.Theme.MODE_SYSTEM -> radioSystem.isChecked = true
            AppConstants.Theme.MODE_LIGHT -> radioLight.isChecked = true
            AppConstants.Theme.MODE_DARK -> radioDark.isChecked = true
        }
        
        // 创建对话框
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
        
        // 设置按钮点击事件
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnApply.setOnClickListener {
            val newMode = when {
                radioSystem.isChecked -> AppConstants.Theme.MODE_SYSTEM
                radioLight.isChecked -> AppConstants.Theme.MODE_LIGHT
                radioDark.isChecked -> AppConstants.Theme.MODE_DARK
                else -> AppConstants.Theme.MODE_SYSTEM
            }
            
            if (newMode != currentMode) {
                ThemeManager.setThemeMode(newMode)
                showThemeAppliedMessage()
                
                // 更新开关状态
                binding.switchDarkMode.isChecked = when (newMode) {
                    AppConstants.Theme.MODE_DARK -> true
                    AppConstants.Theme.MODE_LIGHT -> false
                    else -> ThemeManager.isDarkMode(requireContext())
                }
            }
            dialog.dismiss()
        }
        
        dialog.show()
    }

    /**
     * 显示主题应用成功消息
     */
    private fun showThemeAppliedMessage() {
        Toast.makeText(
            requireContext(),
            getString(R.string.theme_applied),
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 执行数据备份
     */
    private fun performBackup() {
        lifecycleScope.launch {
            // TODO: 实现数据备份功能
            Toast.makeText(requireContext(), "备份功能开发中", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 执行数据恢复
     */
    private fun performRestore() {
        lifecycleScope.launch {
            // TODO: 实现数据恢复功能
            Toast.makeText(requireContext(), "恢复功能开发中", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 导出数据
     */
    private fun exportData() {
        lifecycleScope.launch {
            // TODO: 实现数据导出功能
            Toast.makeText(requireContext(), "导出功能开发中", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 显示关于对话框
     */
    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("关于 Life Ledger")
            .setMessage("Life Ledger v1.0\n\n一款智能的生活记录应用，帮助您管理财务和待办事项。")
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 显示隐私政策
     */
    private fun showPrivacyPolicy() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("隐私政策")
            .setMessage("我们重视您的隐私。所有数据都存储在本地设备上，不会上传到服务器。")
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
} 