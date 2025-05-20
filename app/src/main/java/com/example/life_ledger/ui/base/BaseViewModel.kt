package com.example.life_ledger.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * ViewModel基类
 * 提供通用的ViewModel功能和错误处理
 */
abstract class BaseViewModel : ViewModel() {
    
    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // 错误信息
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    // 成功信息
    private val _success = MutableLiveData<String>()
    val success: LiveData<String> = _success
    
    // 提示信息
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message
    
    /**
     * 协程异常处理器
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _isLoading.value = false
        handleException(exception)
    }
    
    /**
     * 显示加载状态
     */
    protected fun showLoading() {
        _isLoading.value = true
    }
    
    /**
     * 隐藏加载状态
     */
    protected fun hideLoading() {
        _isLoading.value = false
    }
    
    /**
     * 显示错误信息
     */
    protected fun showError(message: String) {
        _error.value = message
        hideLoading()
    }
    
    /**
     * 显示成功信息
     */
    protected fun showSuccess(message: String) {
        _success.value = message
        hideLoading()
    }
    
    /**
     * 显示提示信息
     */
    protected fun showMessage(message: String) {
        _message.value = message
    }
    
    /**
     * 安全执行协程
     */
    protected fun launchSafely(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            block()
        }
    }
    
    /**
     * 处理异常
     */
    private fun handleException(exception: Throwable) {
        val errorMessage = when (exception) {
            is java.net.UnknownHostException -> "网络连接失败，请检查网络设置"
            is java.net.SocketTimeoutException -> "网络连接超时，请重试"
            is java.net.ConnectException -> "无法连接到服务器，请稍后重试"
            is kotlinx.coroutines.CancellationException -> return // 协程被取消，不处理
            else -> exception.message ?: "未知错误"
        }
        showError(errorMessage)
    }
    
    /**
     * 清除错误状态
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * 清除成功状态
     */
    fun clearSuccess() {
        _success.value = null
    }
    
    /**
     * 清除消息状态
     */
    fun clearMessage() {
        _message.value = null
    }
    
    /**
     * 清除所有状态
     */
    fun clearAllStates() {
        clearError()
        clearSuccess()
        clearMessage()
        hideLoading()
    }
} 