package com.example.life_ledger

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.life_ledger.databinding.ActivityMainBinding
import com.example.life_ledger.ui.theme.ThemeManager

/**
 * LifeLedger 主Activity
 * 使用 MVVM 架构、Navigation Component 和底部导航
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // 在super.onCreate之前应用主题
        applyTheme()
        
        super.onCreate(savedInstanceState)
        
        // 初始化DataBinding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        
        // 设置边到边显示
        setupEdgeToEdge()
        
        // 初始化UI
        initializeUI()
        
        // 设置导航
        setupNavigation()
    }
    
    /**
     * 应用主题
     */
    private fun applyTheme() {
        val customTheme = ThemeManager.getCurrentCustomTheme()
        ThemeManager.applyCustomTheme(this, customTheme)
    }
    
    /**
     * 设置边到边显示效果
     */
    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // 底部导航栏处理padding
            insets
        }
    }
    
    /**
     * 初始化UI组件
     */
    private fun initializeUI() {
        // 设置支持ActionBar
        setSupportActionBar(binding.toolbar)
        
        // 设置标题
        supportActionBar?.title = "LifeLedger"
    }
    
    /**
     * 设置Navigation Component导航
     */
    private fun setupNavigation() {
        // 获取NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // 配置顶级目的地（不显示返回按钮的页面）
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.financeFragment,
                R.id.todoFragment, 
                R.id.statisticsFragment,
                R.id.settingsFragment
            )
        )
        
        // 设置ActionBar与NavController的关联
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // 设置底部导航栏与NavController的关联
        binding.bottomNavigation.setupWithNavController(navController)
    }
    
    /**
     * 处理导航返回按钮点击
     */
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}