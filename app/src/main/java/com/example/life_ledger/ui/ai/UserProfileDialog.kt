package com.example.life_ledger.ui.ai

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.example.life_ledger.R
import com.example.life_ledger.data.service.UserProfile
import com.example.life_ledger.databinding.DialogUserProfileBinding
import com.google.android.material.chip.Chip

/**
 * 用户配置对话框
 */
class UserProfileDialog : DialogFragment() {

    private var _binding: DialogUserProfileBinding? = null
    private val binding get() = _binding!!
    
    private var onProfileUpdated: ((UserProfile) -> Unit)? = null
    private var currentProfile: UserProfile = UserProfile()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.dialog_user_profile,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupClickListeners()
        loadCurrentProfile()
    }

    private fun setupViews() {
        // 设置收入水平下拉框
        val incomeLevels = arrayOf("较低", "中等", "较高", "很高")
        val incomeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            incomeLevels
        )
        incomeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerIncomeLevel.adapter = incomeAdapter
        
        // 设置理财目标选项
        val financialGoals = arrayOf("储蓄", "理财", "投资", "还贷", "购房", "购车", "教育", "养老")
        
        financialGoals.forEach { goal ->
            val chip = Chip(requireContext())
            chip.text = goal
            chip.isCheckable = true
            binding.chipGroupFinancialGoals.addView(chip)
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            buttonSave.setOnClickListener {
                saveProfile()
            }
            
            buttonCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun loadCurrentProfile() {
        binding.apply {
            editTextAge.setText(currentProfile.age.toString())
            
            // 设置收入水平
            val incomeLevels = arrayOf("较低", "中等", "较高", "很高")
            val incomeIndex = incomeLevels.indexOf(currentProfile.incomeLevel)
            if (incomeIndex >= 0) {
                spinnerIncomeLevel.setSelection(incomeIndex)
            }
            
            // 设置理财目标
            currentProfile.financialGoals.forEach { goal ->
                for (i in 0 until chipGroupFinancialGoals.childCount) {
                    val chip = chipGroupFinancialGoals.getChildAt(i) as Chip
                    if (chip.text == goal) {
                        chip.isChecked = true
                        break
                    }
                }
            }
        }
    }

    private fun saveProfile() {
        try {
            val age = binding.editTextAge.text.toString().toIntOrNull() ?: 25
            val incomeLevel = binding.spinnerIncomeLevel.selectedItem.toString()
            
            val selectedGoals = mutableListOf<String>()
            for (i in 0 until binding.chipGroupFinancialGoals.childCount) {
                val chip = binding.chipGroupFinancialGoals.getChildAt(i) as Chip
                if (chip.isChecked) {
                    selectedGoals.add(chip.text.toString())
                }
            }
            
            val updatedProfile = UserProfile(
                age = age.coerceIn(18, 100),
                incomeLevel = incomeLevel,
                financialGoals = selectedGoals.ifEmpty { listOf("储蓄") }
            )
            
            onProfileUpdated?.invoke(updatedProfile)
            dismiss()
            
        } catch (e: Exception) {
            // 处理异常，显示错误信息
            binding.editTextAge.error = "请输入有效的年龄"
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            currentProfile: UserProfile,
            onProfileUpdated: (UserProfile) -> Unit
        ): UserProfileDialog {
            return UserProfileDialog().apply {
                this.currentProfile = currentProfile
                this.onProfileUpdated = onProfileUpdated
            }
        }
    }
} 