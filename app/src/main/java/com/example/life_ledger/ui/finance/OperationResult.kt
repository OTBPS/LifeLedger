package com.example.life_ledger.ui.finance

/**
 * 操作结果数据类
 * 用于封装操作的成功或失败状态和消息
 */
data class OperationResult(
    val isSuccess: Boolean,
    val message: String
) 