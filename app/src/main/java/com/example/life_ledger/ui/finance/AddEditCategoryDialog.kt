package com.example.life_ledger.ui.finance

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.life_ledger.R
import com.example.life_ledger.data.model.Category
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

/**
 * 添加/编辑分类对话框
 * 简化版本，只包含基本的名称输入
 */
class AddEditCategoryDialog : DialogFragment() {

    private var editingCategory: Category? = null
    private var onCategoryAddedListener: ((Category) -> Unit)? = null
    private var onCategoryEditedListener: ((Category) -> Unit)? = null

    companion object {
        fun newInstance(category: Category): AddEditCategoryDialog {
            val dialog = AddEditCategoryDialog()
            dialog.editingCategory = category
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_edit_category, null)

        val editCategoryName = view.findViewById<TextInputEditText>(R.id.editCategoryName)
        val editCategoryDescription = view.findViewById<TextInputEditText>(R.id.editCategoryDescription)

        // 如果是编辑模式，填充现有数据
        editingCategory?.let { category ->
            editCategoryName.setText(category.name)
            editCategoryDescription.setText(category.description)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (editingCategory != null) "编辑分类" else "添加分类")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                val name = editCategoryName.text.toString().trim()
                val description = editCategoryDescription.text.toString().trim()

                if (name.isEmpty()) {
                    Snackbar.make(view, "请输入分类名称", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (editingCategory != null) {
                    // 编辑模式
                    val updatedCategory = editingCategory!!.copy(
                        name = name,
                        description = description.ifEmpty { null },
                        updatedAt = System.currentTimeMillis()
                    )
                    onCategoryEditedListener?.invoke(updatedCategory)
                } else {
                    // 添加模式 - 创建支出分类作为默认
                    val newCategory = Category(
                        name = name,
                        type = Category.CategoryType.FINANCIAL,
                        subType = Category.FinancialSubType.EXPENSE,
                        description = description.ifEmpty { null },
                        icon = "category",
                        color = "#2196F3"
                    )
                    onCategoryAddedListener?.invoke(newCategory)
                }
            }
            .setNegativeButton("取消", null)
            .create()
    }

    fun setOnCategoryAddedListener(listener: (Category) -> Unit) {
        onCategoryAddedListener = listener
    }

    fun setOnCategoryEditedListener(listener: (Category) -> Unit) {
        onCategoryEditedListener = listener
    }
} 