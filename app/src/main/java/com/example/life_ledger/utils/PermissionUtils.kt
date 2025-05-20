package com.example.life_ledger.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * 权限管理工具类
 * 提供权限检查和请求功能
 */
object PermissionUtils {
    
    // 权限请求码
    const val REQUEST_CODE_STORAGE = 1001
    const val REQUEST_CODE_NOTIFICATION = 1002
    const val REQUEST_CODE_CAMERA = 1003
    const val REQUEST_CODE_LOCATION = 1004
    
    // 常用权限定义
    val STORAGE_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    
    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    val CAMERA_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )
    
    val NOTIFICATION_PERMISSIONS = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }
    
    /**
     * 检查单个权限
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查多个权限
     */
    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { hasPermission(context, it) }
    }
    
    /**
     * 检查存储权限
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11+ 不需要存储权限
            true
        } else {
            hasPermissions(context, STORAGE_PERMISSIONS)
        }
    }
    
    /**
     * 检查通知权限
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true // Android 13以下不需要通知权限
        }
    }
    
    /**
     * 检查相机权限
     */
    fun hasCameraPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.CAMERA)
    }
    
    /**
     * 检查位置权限
     */
    fun hasLocationPermission(context: Context): Boolean {
        return hasPermissions(context, LOCATION_PERMISSIONS)
    }
    
    /**
     * 从Activity请求权限
     */
    fun requestPermissions(
        activity: Activity,
        permissions: Array<String>,
        requestCode: Int
    ) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }
    
    /**
     * 从Fragment请求权限
     */
    fun requestPermissions(
        fragment: Fragment,
        permissions: Array<String>,
        requestCode: Int
    ) {
        fragment.requestPermissions(permissions, requestCode)
    }
    
    /**
     * 请求存储权限
     */
    fun requestStoragePermission(activity: Activity) {
        if (!hasStoragePermission(activity)) {
            requestPermissions(activity, STORAGE_PERMISSIONS, REQUEST_CODE_STORAGE)
        }
    }
    
    /**
     * 请求通知权限
     */
    fun requestNotificationPermission(activity: Activity) {
        if (!hasNotificationPermission(activity) && NOTIFICATION_PERMISSIONS.isNotEmpty()) {
            requestPermissions(activity, NOTIFICATION_PERMISSIONS, REQUEST_CODE_NOTIFICATION)
        }
    }
    
    /**
     * 请求相机权限
     */
    fun requestCameraPermission(activity: Activity) {
        if (!hasCameraPermission(activity)) {
            requestPermissions(activity, CAMERA_PERMISSIONS, REQUEST_CODE_CAMERA)
        }
    }
    
    /**
     * 请求位置权限
     */
    fun requestLocationPermission(activity: Activity) {
        if (!hasLocationPermission(activity)) {
            requestPermissions(activity, LOCATION_PERMISSIONS, REQUEST_CODE_LOCATION)
        }
    }
    
    /**
     * 检查权限请求结果
     */
    fun isPermissionGranted(grantResults: IntArray): Boolean {
        return grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }
    
    /**
     * 处理权限请求结果
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        when (requestCode) {
            REQUEST_CODE_STORAGE,
            REQUEST_CODE_NOTIFICATION,
            REQUEST_CODE_CAMERA,
            REQUEST_CODE_LOCATION -> {
                if (isPermissionGranted(grantResults)) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
        }
    }
    
    /**
     * 是否应该显示权限说明
     */
    fun shouldShowRequestPermissionRationale(
        activity: Activity,
        permission: String
    ): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
} 