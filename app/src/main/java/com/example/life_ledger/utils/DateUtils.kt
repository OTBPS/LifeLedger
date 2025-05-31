package com.example.life_ledger.utils

import com.example.life_ledger.constants.AppConstants
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期工具类
 * 提供日期格式化、转换等工具方法
 */
object DateUtils {
    
    private val calendar = Calendar.getInstance()
    
    /**
     * 获取当前日期字符串
     */
    fun getCurrentDateString(format: String = AppConstants.DateFormat.DATE_FORMAT_DISPLAY): String {
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        return sdf.format(Date())
    }
    
    /**
     * 获取当前时间戳
     */
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }
    
    /**
     * 时间戳转日期字符串
     */
    fun timestampToDateString(
        timestamp: Long,
        format: String = AppConstants.DateFormat.DATE_FORMAT_DISPLAY
    ): String {
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * 日期字符串转时间戳
     */
    fun dateStringToTimestamp(
        dateString: String,
        format: String = AppConstants.DateFormat.DATE_FORMAT_DISPLAY
    ): Long? {
        return try {
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            sdf.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取本月第一天
     */
    fun getFirstDayOfMonth(): Date {
        calendar.time = Date()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    /**
     * 获取本月最后一天
     */
    fun getLastDayOfMonth(): Date {
        calendar.time = Date()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }
    
    /**
     * 获取本年第一天
     */
    fun getFirstDayOfYear(): Date {
        calendar.time = Date()
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    /**
     * 获取本年最后一天
     */
    fun getLastDayOfYear(): Date {
        calendar.time = Date()
        calendar.set(Calendar.MONTH, Calendar.DECEMBER)
        calendar.set(Calendar.DAY_OF_MONTH, 31)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }
    
    /**
     * 检查日期是否为今天
     */
    fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val date = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * 检查日期是否为本周
     */
    fun isThisWeek(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val date = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                today.get(Calendar.WEEK_OF_YEAR) == date.get(Calendar.WEEK_OF_YEAR)
    }
    
    /**
     * 检查日期是否为本月
     */
    fun isThisMonth(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val date = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == date.get(Calendar.MONTH)
    }
    
    /**
     * 获取相对日期描述
     */
    fun getRelativeDateString(timestamp: Long): String {
        return when {
            isToday(timestamp) -> "今天"
            isThisWeek(timestamp) -> {
                val daysDiff = ((System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24)).toInt()
                when (daysDiff) {
                    1 -> "昨天"
                    2 -> "前天"
                    else -> "${daysDiff}天前"
                }
            }
            isThisMonth(timestamp) -> {
                val weeksDiff = ((System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24 * 7)).toInt()
                "${weeksDiff}周前"
            }
            else -> timestampToDateString(timestamp)
        }
    }
    
    /**
     * 格式化日期时间
     */
    fun formatDateTime(
        timestamp: Long,
        format: String = "yyyy年MM月dd日 HH:mm"
    ): String {
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * 格式化相对日期（用于待办事项截止时间显示）
     */
    fun formatRelativeDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = timestamp - now
        
        return when {
            diff < 0 -> {
                // 已过期
                val daysPast = (-diff / (1000 * 60 * 60 * 24)).toInt()
                when (daysPast) {
                    0 -> "今天已过期"
                    1 -> "昨天已过期"
                    else -> "${daysPast}天前已过期"
                }
            }
            diff < 60 * 60 * 1000 -> {
                // 1小时内
                val minutes = (diff / (1000 * 60)).toInt()
                "${minutes}分钟后"
            }
            diff < 24 * 60 * 60 * 1000 -> {
                // 24小时内
                val hours = (diff / (1000 * 60 * 60)).toInt()
                "${hours}小时后"
            }
            diff < 7 * 24 * 60 * 60 * 1000 -> {
                // 7天内
                val days = (diff / (1000 * 60 * 60 * 24)).toInt()
                when (days) {
                    0 -> "今天"
                    1 -> "明天"
                    2 -> "后天"
                    else -> "${days}天后"
                }
            }
            else -> {
                // 超过7天，显示具体日期
                timestampToDateString(timestamp, "MM月dd日")
            }
        }
    }
} 