package com.example.life_ledger.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Fragment基类
 * 提供通用的Fragment功能和MVVM支持
 */
abstract class BaseFragment<DB : ViewDataBinding, VM : ViewModel> : Fragment() {
    
    private var _binding: DB? = null
    protected val binding get() = _binding!!
    
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
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DataBindingUtil.inflate(inflater, getLayoutId(), container, false)
        binding.lifecycleOwner = this
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
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
    protected open fun showLoading() {
        // 子类可以重写此方法实现自定义加载界面
        (activity as? BaseActivity<*, *>)?.showLoading()
    }
    
    /**
     * 隐藏加载中
     */
    protected open fun hideLoading() {
        // 子类可以重写此方法
        (activity as? BaseActivity<*, *>)?.hideLoading()
    }
    
    /**
     * 显示错误信息
     */
    protected open fun showError(message: String) {
        // 子类可以重写此方法实现自定义错误显示
        (activity as? BaseActivity<*, *>)?.showError(message)
    }
    
    /**
     * 显示成功信息
     */
    protected open fun showSuccess(message: String) {
        // 子类可以重写此方法实现自定义成功提示
        (activity as? BaseActivity<*, *>)?.showSuccess(message)
    }
    
    /**
     * 显示提示信息
     */
    protected open fun showMessage(message: String) {
        // 子类可以重写此方法实现自定义消息提示
        (activity as? BaseActivity<*, *>)?.showMessage(message)
    }
    
    /**
     * 检查权限
     */
    protected fun hasPermission(permission: String): Boolean {
        return requireActivity().checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
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
    
    /**
     * 是否正在加载
     */
    protected open fun isLoading(): Boolean {
        return false
    }
    
    /**
     * 刷新数据
     */
    protected open fun refreshData() {
        // 子类可以重写此方法实现数据刷新
    }
    
    /**
     * 获取安全的Context（避免Fragment已经detach的情况）
     */
    protected fun getSafeContext() = context
    
    /**
     * 获取安全的Activity（避免Fragment已经detach的情况）
     */
    protected fun getSafeActivity() = activity
    
    /**
     * 安全执行代码块（确保Fragment已attached）
     */
    protected inline fun safeExecute(action: () -> Unit) {
        if (isAdded && context != null) {
            action()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 