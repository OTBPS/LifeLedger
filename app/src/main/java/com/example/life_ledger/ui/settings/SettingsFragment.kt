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
 * Settings page
 * Provides theme settings, notification preferences, data management and other functions
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
     * Setup UI components
     */
    private fun setupUI() {
        // Initialize theme mode switch status
        val currentThemeMode = ThemeManager.getCurrentThemeMode()
        binding.switchDarkMode.isChecked = when (currentThemeMode) {
            AppConstants.Theme.MODE_DARK -> true
            AppConstants.Theme.MODE_LIGHT -> false
            else -> ThemeManager.isDarkMode(requireContext())
        }
        
        // Initialize other settings status
        binding.switchNotifications.isChecked = PreferenceUtils.isNotificationEnabled()
        binding.switchFinanceReminder.isChecked = PreferenceUtils.getBoolean("finance_reminder", true)
    }

    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        binding.apply {
            // Dark mode switch
            switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
                toggleDarkMode(isChecked)
            }

            // Notification settings
            switchNotifications.setOnCheckedChangeListener { _, isChecked ->
                toggleNotifications(isChecked)
            }

            // Financial reminder
            switchFinanceReminder.setOnCheckedChangeListener { _, isChecked ->
                toggleFinanceReminder(isChecked)
            }

            // Theme selection
            layoutTheme.setOnClickListener { showThemeDialog() }
            
            // Data management
            layoutBackup.setOnClickListener { performBackup() }
            layoutRestore.setOnClickListener { performRestore() }
            layoutExport.setOnClickListener { exportData() }
            
            // Other
            layoutAbout.setOnClickListener { showAboutDialog() }
            layoutPrivacy.setOnClickListener { showPrivacyPolicy() }
        }
    }

    /**
     * Toggle dark mode
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
     * Toggle notifications
     */
    private fun toggleNotifications(enabled: Boolean) {
        PreferenceUtils.setNotificationEnabled(enabled)
        Toast.makeText(
            requireContext(),
            if (enabled) getString(R.string.notifications_enabled) else getString(R.string.notifications_disabled),
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Toggle financial reminder
     */
    private fun toggleFinanceReminder(enabled: Boolean) {
        PreferenceUtils.putBoolean("finance_reminder", enabled)
        Toast.makeText(
            requireContext(),
            if (enabled) getString(R.string.finance_reminder_enabled) else getString(R.string.finance_reminder_disabled),
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Show theme selection dialog
     */
    private fun showThemeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_theme_selection, null)
        
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewThemes)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnApply = dialogView.findViewById<MaterialButton>(R.id.btnApply)
        
        // Setup RecyclerView
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
        
        // Create dialog
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
        
        // Setup button click events
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnApply.setOnClickListener {
            if (selectedTheme != currentTheme) {
                ThemeManager.setCustomTheme(selectedTheme)
                showThemeAppliedMessage()
                
                // Recreate Activity to apply new theme
                requireActivity().recreate()
            }
            dialog.dismiss()
        }
        
        dialog.show()
    }

    /**
     * Show theme mode selection dialog
     */
    private fun showThemeModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_theme_mode_selection, null)
        
        val radioSystem = dialogView.findViewById<MaterialRadioButton>(R.id.radioSystem)
        val radioLight = dialogView.findViewById<MaterialRadioButton>(R.id.radioLight)
        val radioDark = dialogView.findViewById<MaterialRadioButton>(R.id.radioDark)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnApply = dialogView.findViewById<MaterialButton>(R.id.btnApply)
        
        // Set current selection status
        val currentMode = ThemeManager.getCurrentThemeMode()
        when (currentMode) {
            AppConstants.Theme.MODE_SYSTEM -> radioSystem.isChecked = true
            AppConstants.Theme.MODE_LIGHT -> radioLight.isChecked = true
            AppConstants.Theme.MODE_DARK -> radioDark.isChecked = true
        }
        
        // Create dialog
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
        
        // Setup button click events
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
                
                // Update switch status
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
     * Show theme applied success message
     */
    private fun showThemeAppliedMessage() {
        Toast.makeText(
            requireContext(),
            getString(R.string.theme_applied),
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Perform data backup
     */
    private fun performBackup() {
        lifecycleScope.launch {
            // TODO: Implement data backup functionality
            Toast.makeText(requireContext(), getString(R.string.backup_in_development), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Perform data recovery
     */
    private fun performRestore() {
        lifecycleScope.launch {
            // TODO: Implement data recovery functionality
            Toast.makeText(requireContext(), getString(R.string.restore_in_development), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Export data
     */
    private fun exportData() {
        lifecycleScope.launch {
            // TODO: Implement data export functionality
            Toast.makeText(requireContext(), getString(R.string.export_in_development), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Show about dialog
     */
    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.about_life_ledger))
            .setMessage(getString(R.string.about_app_description))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Show privacy policy
     */
    private fun showPrivacyPolicy() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.privacy_policy))
            .setMessage(getString(R.string.privacy_policy_description))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
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