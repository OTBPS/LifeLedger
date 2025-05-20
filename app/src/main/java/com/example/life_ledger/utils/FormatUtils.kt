package com.example.life_ledger.utils

import com.example.life_ledger.constants.AppConstants
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

/**
 * 格式化工具类
 * 提供货币、数字格式化等工具方法
 */
object FormatUtils {
    
    private val decimalFormat = DecimalFormat("#0.00")
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    
    /**
     * 格式化金额显示
     */
    fun formatAmount(amount: Double, currency: String = AppConstants.Finance.DEFAULT_CURRENCY): String {
        return when (currency) {
            "CNY" -> "¥${decimalFormat.format(amount)}"
            "USD" -> "$${decimalFormat.format(amount)}"
            "EUR" -> "€${decimalFormat.format(amount)}"
            "JPY" -> "¥${DecimalFormat("#0").format(amount)}"
            "GBP" -> "£${decimalFormat.format(amount)}"
            else -> "${currency} ${decimalFormat.format(amount)}"
        }
    }
    
    /**
     * 格式化简洁金额显示（大数值使用K、M等单位）
     */
    fun formatCompactAmount(amount: Double, currency: String = AppConstants.Finance.DEFAULT_CURRENCY): String {
        val formattedNumber = when {
            amount >= 1_000_000 -> "${decimalFormat.format(amount / 1_000_000)}M"
            amount >= 1_000 -> "${decimalFormat.format(amount / 1_000)}K"
            else -> decimalFormat.format(amount)
        }
        
        return when (currency) {
            "CNY" -> "¥$formattedNumber"
            "USD" -> "$$formattedNumber"
            "EUR" -> "€$formattedNumber"
            "JPY" -> "¥${DecimalFormat("#0").format(amount)}"
            "GBP" -> "£$formattedNumber"
            else -> "$currency $formattedNumber"
        }
    }
    
    /**
     * 格式化百分比
     */
    fun formatPercentage(value: Double, decimalPlaces: Int = 1): String {
        val format = DecimalFormat("#0.${"0".repeat(decimalPlaces)}%")
        return format.format(value / 100)
    }
    
    /**
     * 格式化数字（添加千位分隔符）
     */
    fun formatNumber(number: Long): String {
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(number)
    }
    
    /**
     * 格式化数字（添加千位分隔符）
     */
    fun formatNumber(number: Double, decimalPlaces: Int = 2): String {
        val pattern = "#,##0.${"0".repeat(decimalPlaces)}"
        val format = DecimalFormat(pattern)
        return format.format(number)
    }
    
    /**
     * 格式化文件大小
     */
    fun formatFileSize(bytes: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024
        
        return when {
            bytes >= gb -> "${decimalFormat.format(bytes.toDouble() / gb)} GB"
            bytes >= mb -> "${decimalFormat.format(bytes.toDouble() / mb)} MB"
            bytes >= kb -> "${decimalFormat.format(bytes.toDouble() / kb)} KB"
            else -> "$bytes B"
        }
    }
    
    /**
     * 验证金额格式
     */
    fun isValidAmount(amountString: String): Boolean {
        return try {
            val amount = amountString.toDouble()
            amount >= AppConstants.Finance.MIN_AMOUNT && amount <= AppConstants.Finance.MAX_AMOUNT
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    /**
     * 解析金额字符串
     */
    fun parseAmount(amountString: String): Double? {
        return try {
            // 移除货币符号和空格
            val cleaned = amountString.replace(Regex("[¥$€£,\\s]"), "")
            val amount = cleaned.toDouble()
            if (amount >= AppConstants.Finance.MIN_AMOUNT && amount <= AppConstants.Finance.MAX_AMOUNT) {
                amount
            } else {
                null
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * 格式化银行卡号显示（隐藏中间部分）
     */
    fun formatBankCardNumber(cardNumber: String): String {
        return if (cardNumber.length >= 8) {
            "${cardNumber.substring(0, 4)} **** **** ${cardNumber.substring(cardNumber.length - 4)}"
        } else {
            cardNumber
        }
    }
    
    /**
     * 格式化手机号显示（隐藏中间部分）
     */
    fun formatPhoneNumber(phoneNumber: String): String {
        return if (phoneNumber.length == 11) {
            "${phoneNumber.substring(0, 3)}****${phoneNumber.substring(7)}"
        } else {
            phoneNumber
        }
    }
    
    /**
     * 限制文本长度并添加省略号
     */
    fun ellipsize(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            "${text.substring(0, maxLength - 3)}..."
        } else {
            text
        }
    }
} 