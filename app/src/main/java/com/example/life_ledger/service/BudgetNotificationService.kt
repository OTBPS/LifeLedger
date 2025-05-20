package com.example.life_ledger.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.life_ledger.MainActivity
import com.example.life_ledger.R
import com.example.life_ledger.data.model.Budget
import com.example.life_ledger.data.repository.LifeLedgerRepository
import com.example.life_ledger.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 预算通知后台服务
 * 使用WorkManager定期检查预算状态并发送通知
 */
class BudgetNotificationService(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = LifeLedgerRepository.getInstance(AppDatabase.getDatabase(context))
    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val WORK_NAME = "budget_notification_work"
        private const val NOTIFICATION_CHANNEL_ID = "budget_notifications"
        private const val NOTIFICATION_CHANNEL_NAME = "预算通知"
        
        private const val NOTIFICATION_ID_OVERSPENT = 1001
        private const val NOTIFICATION_ID_WARNING = 1002
        private const val NOTIFICATION_ID_EXPIRING = 1003
        
        /**
         * 启动预算通知服务
         */
        fun startBudgetNotificationWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<BudgetNotificationService>(
                1, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }
        
        /**
         * 停止预算通知服务
         */
        fun stopBudgetNotificationWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            checkBudgetStatus()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * 检查预算状态并发送通知
     */
    private suspend fun checkBudgetStatus() {
        createNotificationChannel()
        
        val budgets = repository.getCurrentBudgetsList()
        val activeBudgets = budgets.filter { it.isActive }
        
        for (budget in activeBudgets) {
            when (budget.getBudgetStatus()) {
                Budget.BudgetStatus.EXCEEDED -> {
                    if (budget.needsAlert()) {
                        sendOverspentNotification(budget)
                        updateLastAlertDate(budget)
                    }
                }
                Budget.BudgetStatus.WARNING -> {
                    if (budget.needsAlert()) {
                        sendWarningNotification(budget)
                        updateLastAlertDate(budget)
                    }
                }
                Budget.BudgetStatus.EXPIRED -> {
                    if (budget.isExpiringSoon()) {
                        sendExpiringNotification(budget)
                    }
                }
                Budget.BudgetStatus.SAFE -> {
                    // 预算状态正常，无需通知
                }
            }
        }
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "预算超支和到期提醒"
                enableVibration(true)
                enableLights(true)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 检查是否有通知权限
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        }
    }

    /**
     * 发送超支通知
     */
    private fun sendOverspentNotification(budget: Budget) {
        if (!hasNotificationPermission()) {
            return
        }
        
        val title = "预算超支提醒"
        val content = "您的「${budget.name}」预算已超支 ¥${String.format("%.2f", budget.spent - budget.amount)}"

        val notification = createNotification(
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_HIGH
        )

        notificationManager.notify(NOTIFICATION_ID_OVERSPENT, notification)
    }

    /**
     * 发送警告通知
     */
    private fun sendWarningNotification(budget: Budget) {
        if (!hasNotificationPermission()) {
            return
        }
        
        val title = "预算警告提醒"
        val content = "您的「${budget.name}」预算已使用 ${String.format("%.1f", budget.getSpentPercentage())}%"

        val notification = createNotification(
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )

        notificationManager.notify(NOTIFICATION_ID_WARNING, notification)
    }

    /**
     * 发送即将到期通知
     */
    private fun sendExpiringNotification(budget: Budget) {
        if (!hasNotificationPermission()) {
            return
        }
        
        val title = "预算即将到期"
        val content = "您的「${budget.name}」预算将在 ${budget.getRemainingDays()} 天后到期"

        val notification = createNotification(
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )

        notificationManager.notify(NOTIFICATION_ID_EXPIRING, notification)
    }

    /**
     * 创建通知
     */
    private fun createNotification(
        title: String,
        content: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ): android.app.Notification {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "budget")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()
    }

    /**
     * 更新最后警告时间
     */
    private suspend fun updateLastAlertDate(budget: Budget) {
        val currentTime = System.currentTimeMillis()
        val updatedBudget = budget.copy(
            lastAlertDate = currentTime,
            updatedAt = currentTime
        )
        repository.updateBudget(updatedBudget)
    }
} 