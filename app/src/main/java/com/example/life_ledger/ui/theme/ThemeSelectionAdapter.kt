package com.example.life_ledger.ui.theme

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.life_ledger.R
import com.example.life_ledger.constants.AppConstants
import com.google.android.material.card.MaterialCardView

/**
 * 主题选择适配器
 */
class ThemeSelectionAdapter(
    private val context: Context,
    private val themes: List<String>,
    private var selectedTheme: String,
    private val onThemeSelected: (String) -> Unit
) : RecyclerView.Adapter<ThemeSelectionAdapter.ThemeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_theme_option, parent, false)
        return ThemeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val theme = themes[position]
        holder.bind(theme)
    }

    override fun getItemCount(): Int = themes.size

    /**
     * 更新选中的主题
     */
    fun updateSelectedTheme(theme: String) {
        val oldPosition = themes.indexOf(selectedTheme)
        val newPosition = themes.indexOf(theme)
        
        selectedTheme = theme
        
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition)
        }
        if (newPosition != -1) {
            notifyItemChanged(newPosition)
        }
    }

    inner class ThemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val colorPreview: View = itemView.findViewById(R.id.colorPreview)
        private val themeName: TextView = itemView.findViewById(R.id.themeName)
        private val selectedIndicator: ImageView = itemView.findViewById(R.id.selectedIndicator)

        fun bind(theme: String) {
            // 设置主题名称
            themeName.text = ThemeManager.getThemeDisplayName(context, theme)
            
            // 设置颜色预览
            setupColorPreview(theme)
            
            // 设置选中状态
            val isSelected = theme == selectedTheme
            selectedIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
            
            // 设置卡片边框
            cardView.strokeColor = if (isSelected) {
                ContextCompat.getColor(context, R.color.md_theme_primary)
            } else {
                ContextCompat.getColor(context, android.R.color.transparent)
            }
            
            // 设置点击事件
            cardView.setOnClickListener {
                onThemeSelected(theme)
            }
        }

        private fun setupColorPreview(theme: String) {
            val color = ThemeManager.getThemePreviewColor(context, theme)
            
            // 创建圆形背景
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            
            colorPreview.background = drawable
        }
    }
} 