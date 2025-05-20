package com.example.life_ledger.ui.base

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Activity基类
 * 提供通用的Activity功能和MVVM支持
 */
abstract class BaseActivity<DB : ViewDataBinding, VM : ViewModel> : AppCompatActivity() {
    
    protected lateinit var binding: DB
    protected lateinit var viewModel: VM
    
    /**
     * 获取布局资源ID
     */
    abstract fun getLayoutId(): Int
    
    /**
     * 获取ViewModel类
     */
    abstract fun getViewModelClass(): Class<VM>
    
    /**
     * 初始化视图
     */
    abstract fun initView()
    
    /**
     * 初始化数据
     */
    abstract fun initData()
    
    /**
     * 初始化观察者
     */
    abstract fun initObservers()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化DataBinding
        binding = DataBindingUtil.setContentView(this, getLayoutId())
        binding.lifecycleOwner = this
        
        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[getViewModelClass()]
        
        // 初始化方法
        initView()
        initData()
        initObservers()
    }
    
    /**
     * 显示加载中
     */
    open fun showLoading() {
        // 子类可以重写此方法实现自定义加载界面
    }
    
    /**
     * 隐藏加载中
     */
    open fun hideLoading() {
        // 子类可以重写此方法
    }
    
    /**
     * 显示错误信息
     */
    open fun showError(message: String) {
        // 子类可以重写此方法实现自定义错误显示
    }
    
    /**
     * 显示成功信息
     */
    open fun showSuccess(message: String) {
        // 子类可以重写此方法实现自定义成功提示
    }
    
    /**
     * 显示提示信息
     */
    open fun showMessage(message: String) {
        // 子类可以重写此方法实现自定义消息提示
    }
    
    /**
     * 设置Toolbar
     */
    protected fun setupToolbar(title: String? = null, showBackButton: Boolean = false) {
        supportActionBar?.apply {
            if (title != null) {
                this.title = title
            }
            setDisplayHomeAsUpEnabled(showBackButton)
            setDisplayShowHomeEnabled(showBackButton)
        }
    }
    
    /**
     * 处理Toolbar返回按钮点击
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    /**
     * 检查权限
     */
    protected fun hasPermission(permission: String): Boolean {
        return checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 请求权限
     */
    protected fun requestAppPermission(permission: String, requestCode: Int) {
        requestPermissions(arrayOf(permission), requestCode)
    }
    
    /**
     * 请求多个权限
     */
    protected fun requestAppPermissions(permissions: Array<String>, requestCode: Int) {
        requestPermissions(permissions, requestCode)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        if (::binding.isInitialized) {
            binding.unbind()
        }
    }
} 